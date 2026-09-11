package android.speech.tts;

import java.util.Locale;

public class TextToSpeech {
    public static final int SUCCESS = 0;
    public static final int ERROR = -1;
    public static final int ERROR_SYNTHESIS = -3;
    public static final int ERROR_SERVICE = -4;
    public static final int ERROR_NOT_INSTALLED_YET = -6;

    public static final int LANG_AVAILABLE = 0;
    public static final int LANG_COUNTRY_AVAILABLE = 1;
    public static final int LANG_COUNTRY_VAR_AVAILABLE = 2;
    public static final int LANG_NOT_SUPPORTED = -2;

    public static class Engine {
        public static final int CHECK_VOICE_DATA_PASS = 1;
        public static final int CHECK_VOICE_DATA_FAIL = 0;
        public static final String EXTRA_AVAILABLE_VOICES = "availableVoices";
        public static final String EXTRA_UNAVAILABLE_VOICES = "unavailableVoices";
        public static final String EXTRA_SAMPLE_TEXT = "sampleText";
    }
}
