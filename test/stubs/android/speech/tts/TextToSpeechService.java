package android.speech.tts;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.TestPrefs;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

public abstract class TextToSpeechService extends Context {
    private SharedPreferences prefs = new TestPrefs();

    public void setSharedPreferences(SharedPreferences prefs) {
        this.prefs = prefs;
    }

    @Override
    public SharedPreferences getSharedPreferences(String name, int mode) {
        return prefs;
    }

    public void onCreate() {
        // AOSP TextToSpeechService.onCreate() simulation:
        // TextToSpeechService initializes SynthThread and calls onLoadLanguage with system default locale!
        onLoadLanguage("eng", "USA", "");
    }

    public void onDestroy() {
    }

    public abstract int onIsLanguageAvailable(String lang, String country, String variant);
    public abstract String[] onGetLanguage();
    public abstract int onLoadLanguage(String lang, String country, String variant);
    public abstract void onStop();
    public abstract void onSynthesizeText(SynthesisRequest req, SynthesisCallback cb);
    public abstract List<Voice> onGetVoices();
    public abstract String onGetDefaultVoiceNameFor(String lang, String country, String variant);
    public abstract int onLoadVoice(String voiceName);
    public abstract int onIsValidVoiceName(String voiceName);
    public Set<String> onGetFeaturesForLanguage(String lang, String country, String variant) {
        return new HashSet<String>();
    }
}
