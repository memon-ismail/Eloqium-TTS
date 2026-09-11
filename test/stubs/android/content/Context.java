package android.content;

import android.content.res.AssetManager;

public abstract class Context {
    public static final int MODE_PRIVATE = 0;
    public abstract SharedPreferences getSharedPreferences(String name, int mode);
    public void startActivity(Intent intent) {}
    public AssetManager getAssets() { return new AssetManager(); }
}
