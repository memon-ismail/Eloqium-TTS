package android.content;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TestPrefs implements SharedPreferences {
    private final Map<String, Object> map = new HashMap<String, Object>();

    @Override public Map<String, ?> getAll() { return new HashMap<String, Object>(map); }
    @Override public String getString(String key, String defValue) { Object v = map.get(key); return v instanceof String ? (String) v : defValue; }
    @SuppressWarnings("unchecked")
    @Override public Set<String> getStringSet(String key, Set<String> defValues) { Object v = map.get(key); return v instanceof Set ? (Set<String>) v : defValues; }
    @Override public int getInt(String key, int defValue) { Object v = map.get(key); return v instanceof Integer ? (Integer) v : defValue; }
    @Override public long getLong(String key, long defValue) { Object v = map.get(key); return v instanceof Long ? (Long) v : defValue; }
    @Override public float getFloat(String key, float defValue) { Object v = map.get(key); return v instanceof Float ? (Float) v : defValue; }
    @Override public boolean getBoolean(String key, boolean defValue) { Object v = map.get(key); return v instanceof Boolean ? (Boolean) v : defValue; }
    @Override public boolean contains(String key) { return map.containsKey(key); }
    @Override public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {}
    @Override public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {}

    @Override public Editor edit() {
        return new Editor() {
            private final Map<String, Object> staging = new HashMap<String, Object>();
            private boolean clearRequested = false;

            @Override public Editor putString(String key, String value) { staging.put(key, value); return this; }
            @Override public Editor putStringSet(String key, Set<String> values) { staging.put(key, values); return this; }
            @Override public Editor putInt(String key, int value) { staging.put(key, value); return this; }
            @Override public Editor putLong(String key, long value) { staging.put(key, value); return this; }
            @Override public Editor putFloat(String key, float value) { staging.put(key, value); return this; }
            @Override public Editor putBoolean(String key, boolean value) { staging.put(key, value); return this; }
            @Override public Editor remove(String key) { staging.put(key, null); return this; }
            @Override public Editor clear() { clearRequested = true; return this; }
            @Override public boolean commit() { apply(); return true; }
            @Override public void apply() {
                if (clearRequested) { map.clear(); clearRequested = false; }
                for (Map.Entry<String, Object> entry : staging.entrySet()) {
                    if (entry.getValue() == null) map.remove(entry.getKey());
                    else map.put(entry.getKey(), entry.getValue());
                }
                staging.clear();
            }
        };
    }
}
