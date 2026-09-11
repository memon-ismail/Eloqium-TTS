package android.speech.tts;

import android.os.Bundle;

public class SynthesisRequest {
    private final CharSequence text;
    private final Bundle params;
    private String voiceName;
    private String language;
    private String country;
    private String variant;
    private int speechRate = 100;
    private int pitch = 100;

    public SynthesisRequest(CharSequence text, Bundle params) {
        this.text = text;
        this.params = params;
    }

    public CharSequence getCharSequenceText() { return text; }
    public String getText() { return text != null ? text.toString() : null; }
    public Bundle getParams() { return params; }
    public String getVoiceName() { return voiceName; }
    public void setVoiceName(String voiceName) { this.voiceName = voiceName; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getVariant() { return variant; }
    public void setVariant(String variant) { this.variant = variant; }
    public int getSpeechRate() { return speechRate; }
    public void setSpeechRate(int speechRate) { this.speechRate = speechRate; }
    public int getPitch() { return pitch; }
    public void setPitch(int pitch) { this.pitch = pitch; }
}
