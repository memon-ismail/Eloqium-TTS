package android.net;

public class Uri {
    private final String uriString;

    public Uri(String uriString) {
        this.uriString = uriString;
    }

    public static Uri parse(String uriString) {
        return new Uri(uriString);
    }

    @Override
    public String toString() {
        return uriString;
    }
}
