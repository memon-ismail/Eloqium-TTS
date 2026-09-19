/* Eloqium TTS - Native JNI Bridge to OpenEVV (libevv)
 * Package: org.eloqium.tts.engine.NativeEngine
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <jni.h>
#include <pthread.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <android/log.h>

#include "eci.h"

#define TAG "EloqiumJNI"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)

#define FRAME_SAMPLES 1024
#define RING_BYTES    (128 * 1024)
#define START_SPINS   200

typedef struct {
	ECIHand         eci;
	pthread_mutex_t lock;
	pthread_cond_t  room;
	pthread_cond_t  filled;
	unsigned char*  ring;
	size_t          head;
	size_t          tail;
	size_t          count;
	int             aborted;
	int             started;
	int             done;
	pthread_t       worker;
	pthread_cond_t  work;
	int             running;
	int             pending;
	int             busy;
	int             quitting;
	unsigned char*  text;
	ECIDictHand     dict;
	short           frame[FRAME_SAMPLES];
} instance;

static instance* unwrap(jlong handle) {
	return (instance*)(intptr_t)handle;
}

static void put(instance* in, const unsigned char* src, size_t n) {
	size_t first = RING_BYTES - in->head;
	if (first > n)
		first = n;
	memcpy(in->ring + in->head, src, first);
	memcpy(in->ring, src + first, n - first);
	in->head = (in->head + n) % RING_BYTES;
	in->count += n;
}

static void take(instance* in, unsigned char* dst, size_t n) {
	size_t first = RING_BYTES - in->tail;
	if (first > n)
		first = n;
	memcpy(dst, in->ring + in->tail, first);
	memcpy(dst + first, in->ring, n - first);
	in->tail = (in->tail + n) % RING_BYTES;
	in->count -= n;
}

static void* worker_loop(void* data) {
	instance* in = (instance*)data;
	struct timespec tick = { 0, 1000L * 1000L };
	for (;;) {
		int spins;
		pthread_mutex_lock(&in->lock);
		while (!in->pending && !in->quitting)
			pthread_cond_wait(&in->work, &in->lock);
		if (in->quitting) {
			pthread_mutex_unlock(&in->lock);
			return NULL;
		}
		in->pending = 0;
		pthread_mutex_unlock(&in->lock);
		for (spins = 0; spins < START_SPINS; spins++) {
			int begun;
			pthread_mutex_lock(&in->lock);
			begun = in->started || in->aborted;
			pthread_mutex_unlock(&in->lock);
			if (begun || eciSpeaking(in->eci))
				break;
			nanosleep(&tick, NULL);
		}
		eciSynchronize(in->eci);
		pthread_mutex_lock(&in->lock);
		in->done = 1;
		in->busy = 0;
		pthread_cond_broadcast(&in->filled);
		pthread_cond_broadcast(&in->work);
		pthread_mutex_unlock(&in->lock);
	}
}

static jboolean finished(instance* in) {
	pthread_mutex_lock(&in->lock);
	in->done = 1;
	in->busy = 0;
	pthread_cond_broadcast(&in->filled);
	pthread_cond_broadcast(&in->work);
	pthread_mutex_unlock(&in->lock);
	return JNI_FALSE;
}

static void wait_until_idle(instance* in) {
	pthread_mutex_lock(&in->lock);
	while (in->busy)
		pthread_cond_wait(&in->work, &in->lock);
	pthread_mutex_unlock(&in->lock);
}

static int ECICALL on_message(ECIHand handle, ECIMessage message, int param, void* data) {
	instance* in = (instance*)data;
	size_t want;
	(void)handle;
	if (message != eciWaveformBuffer)
		return eciDataProcessed;
	want = (size_t)param * sizeof(short);
	if (want == 0)
		return eciDataProcessed;
	pthread_mutex_lock(&in->lock);
	while (!in->aborted && RING_BYTES - in->count < want)
		pthread_cond_wait(&in->room, &in->lock);
	if (in->aborted) {
		pthread_mutex_unlock(&in->lock);
		return eciDataAbort;
	}
	put(in, (const unsigned char*)in->frame, want);
	in->started = 1;
	pthread_cond_signal(&in->filled);
	pthread_mutex_unlock(&in->lock);
	return eciDataProcessed;
}

JNIEXPORT jlong JNICALL Java_org_eloqium_tts_engine_NativeEngine_create(JNIEnv* env, jclass cls, jint language) {
	instance* in;
	(void)env;
	(void)cls;
	in = (instance*)calloc(1, sizeof *in);
	if (in == NULL)
		return 0;
	in->ring = (unsigned char*)malloc(RING_BYTES);
	if (in->ring == NULL) {
		free(in);
		return 0;
	}
	in->eci = eciNewEx(language);
	if (in->eci == NULL_ECI_HAND) {
		LOGE("eciNewEx(%#x) gave no instance", language);
		free(in->ring);
		free(in);
		return 0;
	}
	pthread_mutex_init(&in->lock, NULL);
	pthread_cond_init(&in->room, NULL);
	pthread_cond_init(&in->filled, NULL);
	pthread_cond_init(&in->work, NULL);
	eciRegisterCallback(in->eci, on_message, in);
	if (!eciSetOutputBuffer(in->eci, FRAME_SAMPLES, in->frame)) {
		LOGE("engine refused output buffer");
		eciDelete(in->eci);
		free(in->ring);
		free(in);
		return 0;
	}
	if (pthread_create(&in->worker, NULL, worker_loop, in) != 0) {
		LOGE("failed to start worker thread");
		eciDelete(in->eci);
		free(in->ring);
		free(in);
		return 0;
	}
	in->running = 1;
	return (jlong)(intptr_t)in;
}

JNIEXPORT void JNICALL Java_org_eloqium_tts_engine_NativeEngine_destroy(JNIEnv* env, jclass cls, jlong handle) {
	instance* in = unwrap(handle);
	(void)env;
	(void)cls;
	if (in == NULL)
		return;
	pthread_mutex_lock(&in->lock);
	in->aborted = 1;
	in->quitting = 1;
	pthread_cond_broadcast(&in->room);
	pthread_cond_broadcast(&in->filled);
	pthread_cond_broadcast(&in->work);
	pthread_mutex_unlock(&in->lock);
	eciStop(in->eci);
	if (in->running) {
		pthread_join(in->worker, NULL);
		in->running = 0;
	}
	if (in->dict != NULL_DICT_HAND)
		eciDeleteDict(in->eci, in->dict);
	eciDelete(in->eci);
	pthread_cond_destroy(&in->room);
	pthread_cond_destroy(&in->filled);
	pthread_cond_destroy(&in->work);
	pthread_mutex_destroy(&in->lock);
	free(in->text);
	free(in->ring);
	free(in);
}

JNIEXPORT jboolean JNICALL Java_org_eloqium_tts_engine_NativeEngine_speak(JNIEnv* env, jclass cls, jlong handle, jbyteArray text) {
	instance*      in = unwrap(handle);
	jsize          n;
	unsigned char* buf;
	(void)cls;
	if (in == NULL || text == NULL)
		return JNI_FALSE;
	n = (*env)->GetArrayLength(env, text);
	buf = (unsigned char*)malloc((size_t)n + 1);
	if (buf == NULL)
		return JNI_FALSE;
	(*env)->GetByteArrayRegion(env, text, 0, n, (jbyte*)buf);
	buf[n] = 0;
	wait_until_idle(in);
	pthread_mutex_lock(&in->lock);
	in->aborted = 0;
	in->started = 0;
	in->done = 0;
	in->busy = 1;
	in->head = in->tail = in->count = 0;
	pthread_mutex_unlock(&in->lock);
	free(in->text);
	in->text = buf;
	if (!in->running) {
		LOGE("no worker to run synthesis");
		return finished(in);
	}
	if (!eciAddText(in->eci, buf)) {
		LOGE("eciAddText refused text");
		return finished(in);
	}
	if (!eciSynthesize(in->eci)) {
		LOGE("eciSynthesize refused");
		return finished(in);
	}
	pthread_mutex_lock(&in->lock);
	in->pending = 1;
	pthread_cond_broadcast(&in->work);
	pthread_mutex_unlock(&in->lock);
	return JNI_TRUE;
}

JNIEXPORT jint JNICALL Java_org_eloqium_tts_engine_NativeEngine_read(JNIEnv* env, jclass cls, jlong handle, jbyteArray dst) {
	instance*      in = unwrap(handle);
	size_t         room;
	size_t         got;
	unsigned char* scratch;
	(void)cls;
	if (in == NULL || dst == NULL)
		return -1;
	room = (size_t)(*env)->GetArrayLength(env, dst);
	if (room == 0)
		return -1;
	pthread_mutex_lock(&in->lock);
	while (in->count == 0 && !in->done && !in->aborted)
		pthread_cond_wait(&in->filled, &in->lock);
	if (in->aborted) {
		pthread_mutex_unlock(&in->lock);
		return -1;
	}
	if (in->count == 0) {
		pthread_mutex_unlock(&in->lock);
		return 0;
	}
	got = in->count < room ? in->count : room;
	scratch = (unsigned char*)malloc(got);
	if (scratch == NULL) {
		pthread_mutex_unlock(&in->lock);
		return -1;
	}
	take(in, scratch, got);
	pthread_cond_signal(&in->room);
	pthread_mutex_unlock(&in->lock);
	(*env)->SetByteArrayRegion(env, dst, 0, (jsize)got, (const jbyte*)scratch);
	free(scratch);
	return (jint)got;
}

JNIEXPORT void JNICALL Java_org_eloqium_tts_engine_NativeEngine_stop(JNIEnv* env, jclass cls, jlong handle) {
	instance* in = unwrap(handle);
	(void)env;
	(void)cls;
	if (in == NULL)
		return;
	pthread_mutex_lock(&in->lock);
	in->aborted = 1;
	in->head = in->tail = in->count = 0;
	pthread_cond_broadcast(&in->room);
	pthread_cond_broadcast(&in->filled);
	pthread_mutex_unlock(&in->lock);
	eciStop(in->eci);
}

static int ensure_dict(instance* in) {
	if (in->dict != NULL_DICT_HAND)
		return 1;
	in->dict = eciNewDict(in->eci);
	if (in->dict == NULL_DICT_HAND) {
		LOGE("could not allocate dictionary");
		return 0;
	}
	if (eciSetDict(in->eci, in->dict) != 0) {
		LOGE("could not set dictionary");
		return 0;
	}
	return 1;
}

JNIEXPORT jint JNICALL Java_org_eloqium_tts_engine_NativeEngine_loadDictionary(JNIEnv* env, jclass cls, jlong handle, jint volume, jstring path) {
	instance*   in = unwrap(handle);
	const char* name;
	jint        answer;
	(void)cls;
	if (in == NULL || path == NULL || !ensure_dict(in))
		return -1;
	name = (*env)->GetStringUTFChars(env, path, NULL);
	if (name == NULL)
		return -1;
	answer = eciLoadDict(in->eci, in->dict, volume, name);
	(*env)->ReleaseStringUTFChars(env, path, name);
	return answer;
}

JNIEXPORT jint JNICALL Java_org_eloqium_tts_engine_NativeEngine_teachWord(JNIEnv* env, jclass cls, jlong handle, jint volume, jbyteArray key, jbyteArray say) {
	instance*      in = unwrap(handle);
	jsize          keyLen;
	jsize          sayLen;
	unsigned char* pair;
	jint           answer;
	(void)cls;
	if (in == NULL || key == NULL || say == NULL || !ensure_dict(in))
		return -1;
	keyLen = (*env)->GetArrayLength(env, key);
	sayLen = (*env)->GetArrayLength(env, say);
	pair = (unsigned char*)malloc((size_t)keyLen + (size_t)sayLen + 2);
	if (pair == NULL)
		return -1;
	(*env)->GetByteArrayRegion(env, key, 0, keyLen, (jbyte*)pair);
	pair[keyLen] = 0;
	(*env)->GetByteArrayRegion(env, say, 0, sayLen, (jbyte*)(pair + keyLen + 1));
	pair[keyLen + 1 + sayLen] = 0;
	answer = eciUpdateDict(in->eci, in->dict, volume, pair, pair + keyLen + 1);
	free(pair);
	return answer;
}

JNIEXPORT jstring JNICALL Java_org_eloqium_tts_engine_NativeEngine_lookUpWord(JNIEnv* env, jclass cls, jlong handle, jint volume, jbyteArray key) {
	instance*      in = unwrap(handle);
	jsize          keyLen;
	unsigned char* want;
	const char*    found;
	jstring        answer;
	(void)cls;
	if (in == NULL || key == NULL || in->dict == NULL_DICT_HAND)
		return NULL;
	keyLen = (*env)->GetArrayLength(env, key);
	want = (unsigned char*)malloc((size_t)keyLen + 1);
	if (want == NULL)
		return NULL;
	(*env)->GetByteArrayRegion(env, key, 0, keyLen, (jbyte*)want);
	want[keyLen] = 0;
	found = eciDictLookup(in->eci, in->dict, volume, want);
	answer = found == NULL ? NULL : (*env)->NewStringUTF(env, found);
	free(want);
	return answer;
}

JNIEXPORT void JNICALL Java_org_eloqium_tts_engine_NativeEngine_forgetDictionaries(JNIEnv* env, jclass cls, jlong handle) {
	instance* in = unwrap(handle);
	(void)env;
	(void)cls;
	if (in == NULL || in->dict == NULL_DICT_HAND)
		return;
	eciSetDict(in->eci, NULL_DICT_HAND);
	eciDeleteDict(in->eci, in->dict);
	in->dict = NULL_DICT_HAND;
}

JNIEXPORT jint JNICALL Java_org_eloqium_tts_engine_NativeEngine_setParam(JNIEnv* env, jclass cls, jlong handle, jint param, jint value) {
	instance* in = unwrap(handle);
	(void)env;
	(void)cls;
	if (in == NULL)
		return -1;
	wait_until_idle(in);
	return eciSetParam(in->eci, param, value);
}

JNIEXPORT jint JNICALL Java_org_eloqium_tts_engine_NativeEngine_getParam(JNIEnv* env, jclass cls, jlong handle, jint param) {
	instance* in = unwrap(handle);
	(void)env;
	(void)cls;
	return in == NULL ? -1 : eciGetParam(in->eci, param);
}

JNIEXPORT jint JNICALL Java_org_eloqium_tts_engine_NativeEngine_setVoiceParam(JNIEnv* env, jclass cls, jlong handle, jint voice, jint param, jint value) {
	instance* in = unwrap(handle);
	(void)env;
	(void)cls;
	if (in == NULL)
		return -1;
	wait_until_idle(in);
	return eciSetVoiceParam(in->eci, voice, param, value);
}

JNIEXPORT jint JNICALL Java_org_eloqium_tts_engine_NativeEngine_getVoiceParam(JNIEnv* env, jclass cls, jlong handle, jint voice, jint param) {
	instance* in = unwrap(handle);
	(void)env;
	(void)cls;
	return in == NULL ? -1 : eciGetVoiceParam(in->eci, voice, param);
}

JNIEXPORT jint JNICALL Java_org_eloqium_tts_engine_NativeEngine_copyVoice(JNIEnv* env, jclass cls, jlong handle, jint from, jint to) {
	instance* in = unwrap(handle);
	(void)env;
	(void)cls;
	if (in == NULL)
		return -1;
	wait_until_idle(in);
	return eciCopyVoice(in->eci, from, to);
}

JNIEXPORT jintArray JNICALL Java_org_eloqium_tts_engine_NativeEngine_languages(JNIEnv* env, jclass cls) {
	unsigned int found[32];
	int          n = 0;
	jintArray    out;
	(void)cls;
	if (eciGetAvailableLanguages(NULL, &n) != 0 || n < 1)
		return (*env)->NewIntArray(env, 0);
	if (n > 32)
		n = 32;
	if (eciGetAvailableLanguages(found, &n) != 0)
		return (*env)->NewIntArray(env, 0);
	out = (*env)->NewIntArray(env, n);
	if (out == NULL)
		return NULL;
	(*env)->SetIntArrayRegion(env, out, 0, n, (const jint*)found);
	return out;
}

JNIEXPORT jstring JNICALL Java_org_eloqium_tts_engine_NativeEngine_version(JNIEnv* env, jclass cls) {
	char buffer[ECI_VERSION_LENGTH];
	(void)cls;
	memset(buffer, 0, sizeof buffer);
	eciVersion(buffer);
	buffer[sizeof buffer - 1] = 0;
	return (*env)->NewStringUTF(env, buffer);
}
