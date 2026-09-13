package org.eloqium.tts.service

import android.content.SharedPreferences
import org.eloqium.tts.pipeline.UserDictionaryProcessor
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Result of a dictionary import operation.
 */
data class ImportResult(
    val dictionary: UserDictionary,
    val languageTag: String,
    val entryCount: Int
)

/**
 * Thread-safe repository managing persistence and caching of user dictionaries.
 * Persisted in SharedPreferences as robust JSON representation.
 *
 * Implements cross-instance cache synchronization:
 * - Shared in-memory caches across all repository instances in the process.
 * - Automatic cache invalidation on any dictionary mutation (add/edit/delete/toggle/import).
 * - SharedPreferences change listener for inter-component reactive synchronization.
 * - Zero-disk-I/O hot audio synthesis path lookup.
 */
class UserDictionaryRepository(private val prefs: SharedPreferences) {

    companion object {
        const val KEY_USER_DICTIONARIES_DATA = "user_dictionaries_data"
        const val KEY_USER_DICTIONARY_ENABLED = "user_dictionary_enabled"

        private val globalLock = Any()

        @Volatile
        private var sharedSystem: UserDictionarySystem? = null

        private val sharedRulesCache = ConcurrentHashMap<String, List<UserDictionaryEntry>>()

        /**
         * Global notification when persistent dictionary data has changed.
         * Invalidates all in-memory caches across all repositories and the processor.
         */
        fun notifyDataChanged() {
            synchronized(globalLock) {
                sharedSystem = null
                sharedRulesCache.clear()
            }
            UserDictionaryProcessor.clearCache()
        }
    }

    private val prefChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == KEY_USER_DICTIONARIES_DATA || key == KEY_USER_DICTIONARY_ENABLED) {
            notifyDataChanged()
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(prefChangeListener)
        loadSystem()
    }

    private fun loadSystem(): UserDictionarySystem = synchronized(globalLock) {
        sharedSystem?.let { return it }
        val json = prefs.getString(KEY_USER_DICTIONARIES_DATA, null)
        val loaded = if (!json.isNullOrBlank()) {
            UserDictionaryJson.parseSystem(json)
        } else {
            UserDictionarySystem()
        }
        sharedSystem = loaded
        sharedRulesCache.clear()
        loaded
    }

    private fun saveSystem(system: UserDictionarySystem) = synchronized(globalLock) {
        sharedSystem = system
        sharedRulesCache.clear()
        val json = UserDictionaryJson.exportSystem(system)
        prefs.edit().putString(KEY_USER_DICTIONARIES_DATA, json).apply()
        UserDictionaryProcessor.clearCache()
    }

    fun getSystem(): UserDictionarySystem = synchronized(globalLock) {
        loadSystem()
    }

    // ---- Language Lifecycle -------------------------------------------------

    fun getAddedLanguages(): List<LanguageDictionaries> = synchronized(globalLock) {
        loadSystem().languages.filter { it.dictionaries.isNotEmpty() }
    }

    fun isLanguageAdded(languageTag: String): Boolean = synchronized(globalLock) {
        val cleanTag = languageTag.trim()
        loadSystem().languages.any {
            (it.languageTag.equals(cleanTag, ignoreCase = true) ||
             it.languageTag.replace('_', '-').equals(cleanTag.replace('_', '-'), ignoreCase = true)) &&
            it.dictionaries.isNotEmpty()
        }
    }

    fun addLanguage(languageTag: String): LanguageDictionaries = synchronized(globalLock) {
        val cleanTag = languageTag.trim()
        val current = loadSystem()
        val existing = current.languages.firstOrNull {
            it.languageTag.equals(cleanTag, ignoreCase = true) ||
            it.languageTag.replace('_', '-').equals(cleanTag.replace('_', '-'), ignoreCase = true)
        }
        if (existing != null && existing.dictionaries.isNotEmpty()) {
            return existing
        }

        // Create default initial dictionary: "User Dictionary", enabled, 0 entries
        val defaultDict = UserDictionary(
            id = UUID.randomUUID().toString(),
            name = "User Dictionary",
            enabled = true,
            entries = emptyList()
        )

        val newLang = LanguageDictionaries(
            languageTag = cleanTag,
            dictionaries = listOf(defaultDict)
        )

        val updatedLanguages = current.languages.filterNot {
            it.languageTag.equals(cleanTag, ignoreCase = true) ||
            it.languageTag.replace('_', '-').equals(cleanTag.replace('_', '-'), ignoreCase = true)
        } + newLang
        val newSystem = current.copy(languages = updatedLanguages)
        saveSystem(newSystem)
        newLang
    }

    fun removeLanguage(languageTag: String) = synchronized(globalLock) {
        val cleanTag = languageTag.trim()
        val current = loadSystem()
        val updated = current.languages.filterNot {
            it.languageTag.equals(cleanTag, ignoreCase = true) ||
            it.languageTag.replace('_', '-').equals(cleanTag.replace('_', '-'), ignoreCase = true)
        }
        saveSystem(current.copy(languages = updated))
    }

    // ---- Dictionary Lifecycle -----------------------------------------------

    fun getDictionaries(languageTag: String): List<UserDictionary> = synchronized(globalLock) {
        val cleanTag = languageTag.trim()
        loadSystem().languages.firstOrNull {
            it.languageTag.equals(cleanTag, ignoreCase = true) ||
            it.languageTag.replace('_', '-').equals(cleanTag.replace('_', '-'), ignoreCase = true)
        }?.dictionaries ?: emptyList()
    }

    fun getDictionary(languageTag: String, dictionaryId: String): UserDictionary? = synchronized(globalLock) {
        getDictionaries(languageTag).firstOrNull { it.id == dictionaryId }
    }

    fun addDictionary(languageTag: String, name: String): UserDictionary = synchronized(globalLock) {
        val cleanTag = languageTag.trim()
        val cleanName = name.trim().takeUnless { it.isEmpty() } ?: "User Dictionary"
        val current = loadSystem()

        val newDict = UserDictionary(
            id = UUID.randomUUID().toString(),
            name = cleanName,
            enabled = true,
            entries = emptyList()
        )

        val existingLang = current.languages.firstOrNull {
            it.languageTag.equals(cleanTag, ignoreCase = true) ||
            it.languageTag.replace('_', '-').equals(cleanTag.replace('_', '-'), ignoreCase = true)
        }
        val updatedLanguages = if (existingLang != null) {
            current.languages.map { lang ->
                if (lang.languageTag.equals(cleanTag, ignoreCase = true) ||
                    lang.languageTag.replace('_', '-').equals(cleanTag.replace('_', '-'), ignoreCase = true)) {
                    lang.copy(dictionaries = lang.dictionaries + newDict)
                } else lang
            }
        } else {
            current.languages + LanguageDictionaries(languageTag = cleanTag, dictionaries = listOf(newDict))
        }

        saveSystem(current.copy(languages = updatedLanguages))
        newDict
    }

    fun renameDictionary(languageTag: String, dictionaryId: String, newName: String): Boolean = synchronized(globalLock) {
        val cleanTag = languageTag.trim()
        val cleanName = newName.trim().takeUnless { it.isEmpty() } ?: return false
        val current = loadSystem()

        var modified = false
        val updatedLanguages = current.languages.map { lang ->
            if (lang.languageTag.equals(cleanTag, ignoreCase = true) ||
                lang.languageTag.replace('_', '-').equals(cleanTag.replace('_', '-'), ignoreCase = true)) {
                val updatedDicts = lang.dictionaries.map { dict ->
                    if (dict.id == dictionaryId) {
                        modified = true
                        dict.copy(name = cleanName)
                    } else dict
                }
                lang.copy(dictionaries = updatedDicts)
            } else lang
        }

        if (modified) {
            saveSystem(current.copy(languages = updatedLanguages))
        }
        modified
    }

    fun setDictionaryEnabled(languageTag: String, dictionaryId: String, enabled: Boolean): Boolean = synchronized(globalLock) {
        val cleanTag = languageTag.trim()
        val current = loadSystem()

        var modified = false
        val updatedLanguages = current.languages.map { lang ->
            if (lang.languageTag.equals(cleanTag, ignoreCase = true) ||
                lang.languageTag.replace('_', '-').equals(cleanTag.replace('_', '-'), ignoreCase = true)) {
                val updatedDicts = lang.dictionaries.map { dict ->
                    if (dict.id == dictionaryId) {
                        modified = true
                        dict.copy(enabled = enabled)
                    } else dict
                }
                lang.copy(dictionaries = updatedDicts)
            } else lang
        }

        if (modified) {
            saveSystem(current.copy(languages = updatedLanguages))
        }
        modified
    }

    fun deleteDictionary(languageTag: String, dictionaryId: String): Boolean = synchronized(globalLock) {
        val cleanTag = languageTag.trim()
        val current = loadSystem()

        var modified = false
        val updatedLanguages = mutableListOf<LanguageDictionaries>()

        for (lang in current.languages) {
            if (lang.languageTag.equals(cleanTag, ignoreCase = true) ||
                lang.languageTag.replace('_', '-').equals(cleanTag.replace('_', '-'), ignoreCase = true)) {
                val remainingDicts = lang.dictionaries.filterNot { it.id == dictionaryId }
                if (remainingDicts.size != lang.dictionaries.size) {
                    modified = true
                }
                // If language is reduced to zero dictionaries, remove that language
                if (remainingDicts.isNotEmpty()) {
                    updatedLanguages.add(lang.copy(dictionaries = remainingDicts))
                }
            } else {
                updatedLanguages.add(lang)
            }
        }

        if (modified) {
            saveSystem(current.copy(languages = updatedLanguages))
        }
        modified
    }

    // ---- Entry Lifecycle ----------------------------------------------------

    fun getEntries(languageTag: String, dictionaryId: String): List<UserDictionaryEntry> = synchronized(globalLock) {
        getDictionary(languageTag, dictionaryId)?.entries ?: emptyList()
    }

    fun addEntry(languageTag: String, dictionaryId: String, entry: UserDictionaryEntry): Boolean = synchronized(globalLock) {
        val cleanTag = languageTag.trim()
        if (entry.source.trim().isEmpty()) return false
        val current = loadSystem()

        var modified = false
        val updatedLanguages = current.languages.map { lang ->
            if (lang.languageTag.equals(cleanTag, ignoreCase = true) ||
                lang.languageTag.replace('_', '-').equals(cleanTag.replace('_', '-'), ignoreCase = true)) {
                val updatedDicts = lang.dictionaries.map { dict ->
                    if (dict.id == dictionaryId) {
                        modified = true
                        dict.copy(entries = dict.entries + entry)
                    } else dict
                }
                lang.copy(dictionaries = updatedDicts)
            } else lang
        }

        if (modified) {
            saveSystem(current.copy(languages = updatedLanguages))
        }
        modified
    }

    fun updateEntry(languageTag: String, dictionaryId: String, entry: UserDictionaryEntry): Boolean = synchronized(globalLock) {
        val cleanTag = languageTag.trim()
        if (entry.source.trim().isEmpty()) return false
        val current = loadSystem()

        var modified = false
        val updatedLanguages = current.languages.map { lang ->
            if (lang.languageTag.equals(cleanTag, ignoreCase = true) ||
                lang.languageTag.replace('_', '-').equals(cleanTag.replace('_', '-'), ignoreCase = true)) {
                val updatedDicts = lang.dictionaries.map { dict ->
                    if (dict.id == dictionaryId) {
                        val updatedEntries = dict.entries.map { e ->
                            if (e.id == entry.id) {
                                modified = true
                                entry
                            } else e
                        }
                        dict.copy(entries = updatedEntries)
                    } else dict
                }
                lang.copy(dictionaries = updatedDicts)
            } else lang
        }

        if (modified) {
            saveSystem(current.copy(languages = updatedLanguages))
        }
        modified
    }

    fun deleteEntry(languageTag: String, dictionaryId: String, entryId: String): Boolean = synchronized(globalLock) {
        val cleanTag = languageTag.trim()
        val current = loadSystem()

        var modified = false
        val updatedLanguages = current.languages.map { lang ->
            if (lang.languageTag.equals(cleanTag, ignoreCase = true) ||
                lang.languageTag.replace('_', '-').equals(cleanTag.replace('_', '-'), ignoreCase = true)) {
                val updatedDicts = lang.dictionaries.map { dict ->
                    if (dict.id == dictionaryId) {
                        val updatedEntries = dict.entries.filterNot { it.id == entryId }
                        if (updatedEntries.size != dict.entries.size) {
                            modified = true
                        }
                        dict.copy(entries = updatedEntries)
                    } else dict
                }
                lang.copy(dictionaries = updatedDicts)
            } else lang
        }

        if (modified) {
            saveSystem(current.copy(languages = updatedLanguages))
        }
        modified
    }

    // ---- Active Entries for Synthesis Pipeline -------------------------------

    /**
     * Returns all enabled entries for a language, maintaining deterministic dictionary order.
     * Thread-safe and cached in-memory for zero disk I/O on audio synthesis path.
     */
    fun getActiveEntriesForLanguage(languageTag: String): List<UserDictionaryEntry> {
        val cleanTag = languageTag.trim()
        val cached = sharedRulesCache[cleanTag]
        if (cached != null) return cached

        return synchronized(globalLock) {
            val secondCheck = sharedRulesCache[cleanTag]
            if (secondCheck != null) return secondCheck

            val sys = sharedSystem ?: loadSystem()
            val lang = sys.languages.firstOrNull { it.languageTag.equals(cleanTag, ignoreCase = true) }
                ?: sys.languages.firstOrNull { it.languageTag.replace('_', '-').equals(cleanTag.replace('_', '-'), ignoreCase = true) }

            val entries = if (lang != null) {
                lang.dictionaries
                    .filter { it.enabled }
                    .flatMap { it.entries }
            } else {
                emptyList()
            }

            sharedRulesCache[cleanTag] = entries
            entries
        }
    }

    // ---- Import / Export ----------------------------------------------------

    fun exportDictionary(languageTag: String, dictionaryId: String): Result<String> = synchronized(globalLock) {
        val cleanTag = languageTag.trim()
        val dict = getDictionary(cleanTag, dictionaryId)
            ?: return Result.failure(IllegalArgumentException("Dictionary not found: $dictionaryId in $cleanTag"))

        try {
            val json = UserDictionaryJson.exportDictionary(dict, cleanTag)
            Result.success(json)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    fun importDictionary(targetLanguage: String?, jsonString: String): Result<ImportResult> = synchronized(globalLock) {
        val parsed = UserDictionaryJson.parseDictionary(jsonString).getOrElse {
            return Result.failure(it)
        }

        val effectiveTag = (targetLanguage?.trim().takeUnless { it.isNullOrEmpty() } ?: parsed.languageTag).trim()
        val current = loadSystem()

        val existingDicts = current.languages.firstOrNull {
            it.languageTag.equals(effectiveTag, ignoreCase = true) ||
            it.languageTag.replace('_', '-').equals(effectiveTag.replace('_', '-'), ignoreCase = true)
        }?.dictionaries ?: emptyList()

        // Handle duplicate dictionary names safely (e.g. "User Dictionary (1)")
        var finalName = parsed.dictionary.name
        var counter = 1
        while (existingDicts.any { it.name.equals(finalName, ignoreCase = true) }) {
            finalName = "${parsed.dictionary.name} ($counter)"
            counter++
        }

        val importedDict = parsed.dictionary.copy(
            id = UUID.randomUUID().toString(),
            name = finalName
        )

        val existingLang = current.languages.firstOrNull {
            it.languageTag.equals(effectiveTag, ignoreCase = true) ||
            it.languageTag.replace('_', '-').equals(effectiveTag.replace('_', '-'), ignoreCase = true)
        }
        val updatedLanguages = if (existingLang != null) {
            current.languages.map { lang ->
                if (lang.languageTag.equals(effectiveTag, ignoreCase = true) ||
                    lang.languageTag.replace('_', '-').equals(effectiveTag.replace('_', '-'), ignoreCase = true)) {
                    lang.copy(dictionaries = lang.dictionaries + importedDict)
                } else lang
            }
        } else {
            current.languages + LanguageDictionaries(languageTag = effectiveTag, dictionaries = listOf(importedDict))
        }

        saveSystem(current.copy(languages = updatedLanguages))
        Result.success(ImportResult(importedDict, effectiveTag, importedDict.entries.size))
    }

    fun clearAll() = synchronized(globalLock) {
        saveSystem(UserDictionarySystem())
        notifyDataChanged()
    }
}
