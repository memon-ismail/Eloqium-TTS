package org.eloqium.tts.pipeline

import android.content.Context
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets

/**
 * Loads and provides fast in-memory UTF-16 Trie lookup for Unicode 16.0 / CLDR 48
 * emoji sequences across all 5 Eloqium language families:
 * en (0), es (1), fr (2), de (3), it (4).
 */
object EmojiData {

    const val LANG_EN = 0
    const val LANG_ES = 1
    const val LANG_FR = 2
    const val LANG_DE = 3
    const val LANG_IT = 4

    private val LANG_CODES = arrayOf("en", "es", "fr", "de", "it")

    class TrieNode {
        val children = HashMap<Char, TrieNode>()
        var translations: Array<String?>? = null
    }

    private val root = TrieNode()
    @Volatile var isLoaded = false
        private set

    var totalEmojis = 0
        private set

    /**
     * Maps language code (e.g. "en", "en-US", "eng", "es", "spa", "fr", "fra", "de", "deu", "it", "ita")
     * to internal language index (0..4). Defaults to 0 (English).
     */
    fun getLanguageIndex(lang: String?): Int {
        if (lang.isNullOrBlank()) return LANG_EN
        val clean = lang.trim().lowercase()
        return when {
            clean.startsWith("en") -> LANG_EN
            clean.startsWith("es") || clean.startsWith("spa") -> LANG_ES
            clean.startsWith("fr") || clean.startsWith("fra") -> LANG_FR
            clean.startsWith("de") || clean.startsWith("deu") -> LANG_DE
            clean.startsWith("it") || clean.startsWith("ita") -> LANG_IT
            else -> LANG_EN
        }
    }

    /**
     * Ensures EmojiData is loaded using the application assets.
     */
    fun ensureLoaded(context: Context): Boolean {
        if (isLoaded) return true
        return synchronized(this) {
            if (isLoaded) return true
            try {
                context.assets.open("emoji_data.bin").use { load(it) }
            } catch (e: Exception) {
                android.util.Log.e("EmojiData", "Failed to load emoji_data.bin", e)
                false
            }
        }
    }

    /**
     * Loads EmojiData from any InputStream (used by Android runtime and test suites).
     */
    @Synchronized
    fun load(inputStream: InputStream): Boolean {
        if (isLoaded) return true
        return try {
            val dis = DataInputStream(BufferedInputStream(inputStream, 65536))

            // 1. Magic bytes "ELQE"
            val magic = ByteArray(4)
            dis.readFully(magic)
            val magicStr = String(magic, StandardCharsets.US_ASCII)
            if (magicStr != "ELQE") {
                throw IllegalStateException("Invalid magic header: $magicStr")
            }

            // 2. Format version (uint16)
            val formatVer = dis.readUnsignedShort()
            if (formatVer != 1) {
                throw IllegalStateException("Unsupported format version: $formatVer")
            }

            // 3. Unicode version (uint8 major, uint8 minor)
            val unicodeMajor = dis.readUnsignedByte()
            val unicodeMinor = dis.readUnsignedByte()

            // 4. CLDR version (uint8 major, uint8 minor)
            val cldrMajor = dis.readUnsignedByte()
            val cldrMinor = dis.readUnsignedByte()

            // 5. Language count (uint16)
            val langCount = dis.readUnsignedShort()
            val langs = Array(langCount) {
                val langBytes = ByteArray(2)
                dis.readFully(langBytes)
                String(langBytes, StandardCharsets.US_ASCII)
            }

            // 6. Emoji count (uint32)
            val count = dis.readInt()
            totalEmojis = count

            // 7. Keys
            val keys = Array(count) {
                val kLen = dis.readUnsignedShort()
                val kBytes = ByteArray(kLen)
                dis.readFully(kBytes)
                String(kBytes, StandardCharsets.UTF_8)
            }

            // 8. Per-language string tables
            val tables = Array(langCount) { Array<String?>(count) { null } }
            for (l in 0 until langCount) {
                val poolSize = dis.readInt()
                val offsets = IntArray(count) { dis.readInt() }
                val pool = ByteArray(poolSize)
                dis.readFully(pool)

                for (k in 0 until count) {
                    val off = offsets[k]
                    if (off != -1) { // 0xFFFFFFFF
                        val sLen = ((pool[off].toInt() and 0xFF) shl 8) or (pool[off + 1].toInt() and 0xFF)
                        tables[l][k] = String(pool, off + 2, sLen, StandardCharsets.UTF_8)
                    }
                }
            }

            // 9. Populate Trie
            for (k in 0 until count) {
                val key = keys[k]
                var curr = root
                for (i in 0 until key.length) {
                    val ch = key[i]
                    curr = curr.children.getOrPut(ch) { TrieNode() }
                }
                val trans = Array<String?>(5) { l ->
                    if (l < langCount) tables[l][k] else null
                }
                curr.translations = trans
            }

            isLoaded = true
            true
        } catch (e: Exception) {
            isLoaded = false
            throw e
        }
    }

    /**
     * Searches for the longest matching emoji sequence in [text] starting at index [startOffset].
     * Returns a Pair of (matchedLength, translationsArray) or null if no match.
     */
    fun findLongestMatch(text: String, startOffset: Int): Pair<Int, Array<String?>>? {
        var curr = root
        var bestLen = 0
        var bestTrans: Array<String?>? = null

        var j = startOffset
        while (j < text.length) {
            val ch = text[j]
            val next = curr.children[ch] ?: break
            curr = next
            j++
            if (curr.translations != null) {
                bestLen = j - startOffset
                bestTrans = curr.translations
            }
        }

        return if (bestLen > 0 && bestTrans != null) {
            Pair(bestLen, bestTrans)
        } else null
    }

    /**
     * Resets loaded state (used in tests if needed).
     */
    @Synchronized
    fun resetForTest() {
        root.children.clear()
        root.translations = null
        isLoaded = false
        totalEmojis = 0
    }
}
