package android.util;

public class Log {
    public static int i(String tag, String msg) { System.out.println("[INFO] " + tag + ": " + msg); return 0; }
    public static int e(String tag, String msg) { System.err.println("[ERROR] " + tag + ": " + msg); return 0; }
    public static int e(String tag, String msg, Throwable tr) { System.err.println("[ERROR] " + tag + ": " + msg); if (tr != null) tr.printStackTrace(); return 0; }
    public static int w(String tag, String msg) { System.out.println("[WARN] " + tag + ": " + msg); return 0; }
    public static int w(String tag, String msg, Throwable tr) { System.out.println("[WARN] " + tag + ": " + msg); if (tr != null) tr.printStackTrace(); return 0; }
    public static int w(String tag, Throwable tr) { if (tr != null) tr.printStackTrace(); return 0; }
    public static int d(String tag, String msg) { return 0; }
    public static int v(String tag, String msg) { return 0; }
}
