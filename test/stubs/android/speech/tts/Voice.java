package android.speech.tts;

import java.util.Locale;
import java.util.Set;

public class Voice {
    public static final int QUALITY_NORMAL = 300;
    public static final int LATENCY_VERY_LOW = 100;

    private final String name;
    private final Locale locale;
    private final int quality;
    private final int latency;
    private final boolean requiresNetwork;
    private final Set<String> features;

    public Voice(String name, Locale locale, int quality, int latency, boolean requiresNetwork, Set<String> features) {
        this.name = name;
        this.locale = locale;
        this.quality = quality;
        this.latency = latency;
        this.requiresNetwork = requiresNetwork;
        this.features = features;
    }

    public String getName() { return name; }
    public Locale getLocale() { return locale; }
    public int getQuality() { return quality; }
    public int getLatency() { return latency; }
    public boolean isRequiresNetwork() { return requiresNetwork; }
    public Set<String> getFeatures() { return features; }

    @Override
    public String toString() {
        return "Voice[name=" + name + ", locale=" + locale + "]";
    }
}
