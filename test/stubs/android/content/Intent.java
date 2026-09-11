package android.content;

import android.net.Uri;

public class Intent {
    public static final String ACTION_VIEW = "android.intent.action.VIEW";
    private String action;
    private Uri data;

    public Intent() {}

    public Intent(String action) {
        this.action = action;
    }

    public Intent(String action, Uri data) {
        this.action = action;
        this.data = data;
    }

    public String getAction() {
        return action;
    }

    public Uri getData() {
        return data;
    }
}
