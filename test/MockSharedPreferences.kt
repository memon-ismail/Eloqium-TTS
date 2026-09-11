package test

import android.content.SharedPreferences

class MockSharedPreferences : SharedPreferences {
    val map = mutableMapOf<String, Any>()

    override fun getAll(): Map<String, *> = HashMap(map)

    override fun getString(key: String, defValue: String?): String? =
        map[key] as? String ?: defValue

    override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? =
        @Suppress("UNCHECKED_CAST")
        (map[key] as? Set<String>) ?: defValues

    override fun getInt(key: String, defValue: Int): Int =
        map[key] as? Int ?: defValue

    override fun getLong(key: String, defValue: Long): Long =
        map[key] as? Long ?: defValue

    override fun getFloat(key: String, defValue: Float): Float =
        map[key] as? Float ?: defValue

    override fun getBoolean(key: String, defValue: Boolean): Boolean =
        map[key] as? Boolean ?: defValue

    private val listeners = mutableListOf<SharedPreferences.OnSharedPreferenceChangeListener>()

    override fun contains(key: String): Boolean = map.containsKey(key)

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        listeners.add(listener)
    }

    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        listeners.remove(listener)
    }

    override fun edit(): SharedPreferences.Editor = EditorImpl()

    inner class EditorImpl : SharedPreferences.Editor {
        private val staging = mutableMapOf<String, Any?>()
        private var clearRequested = false

        override fun putString(key: String, value: String?): SharedPreferences.Editor {
            staging[key] = value
            return this
        }

        override fun putStringSet(key: String, values: Set<String>?): SharedPreferences.Editor {
            staging[key] = values
            return this
        }

        override fun putInt(key: String, value: Int): SharedPreferences.Editor {
            staging[key] = value
            return this
        }

        override fun putLong(key: String, value: Long): SharedPreferences.Editor {
            staging[key] = value
            return this
        }

        override fun putFloat(key: String, value: Float): SharedPreferences.Editor {
            staging[key] = value
            return this
        }

        override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor {
            staging[key] = value
            return this
        }

        override fun remove(key: String): SharedPreferences.Editor {
            staging[key] = null
            return this
        }

        override fun clear(): SharedPreferences.Editor {
            clearRequested = true
            return this
        }

        override fun commit(): Boolean {
            apply()
            return true
        }

        override fun apply() {
            if (clearRequested) {
                map.clear()
                clearRequested = false
            }
            val modifiedKeys = staging.keys.toList()
            for ((k, v) in staging) {
                if (v == null) {
                    map.remove(k)
                } else {
                    map[k] = v
                }
            }
            staging.clear()
            for (listener in listeners.toList()) {
                for (key in modifiedKeys) {
                    listener.onSharedPreferenceChanged(this@MockSharedPreferences, key)
                }
            }
        }
    }
}
