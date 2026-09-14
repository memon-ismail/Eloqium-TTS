package org.eloqium.tts.pipeline

import org.eloqium.tts.service.DictionaryEntryType
import org.eloqium.tts.service.MatchMode
import org.eloqium.tts.service.UserDictionaryEntry
import org.eloqium.tts.service.UserDictionaryRepository
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

/**
 * Compiled user dictionary entry prepared for fast regex matching and precedence sorting.
 */
data class CompiledEntry(
    val entry: UserDictionaryEntry,
    val regex: Regex,
    val matchModeRank: Int,
    val order: Int,
    val runtimeReplacement: String = UserDictionaryProcessor.formatRuntimeReplacement(entry)
)

/**
 * High-performance text replacement processor executing user-defined pronunciation dictionaries.
 *
 * Designed with:
 * - Deterministic precedence: Longer source phrases > Exact match > Starts/Ends with > Contains > Case-sensitive.
 * - Single-pass interval replacement: Zero cascading mutations or accidental recursive corruption.
 * - In-memory compiled rule caching: Zero disk I/O on the audio synthesis path.
 * - Robust Unicode word boundary awareness.
 * - Support for both TEXT replacements and OpenEVV phonetic PRONUNCIATION entries.
 * - Scalable O(1) indexed word lookup for massive community dictionaries (e.g. 68k+ entries).
 */
object UserDictionaryProcessor {

    private val cache = ConcurrentHashMap<String, CachedRuleSet>()
    private val WORD_TOKEN_REGEX = Regex("""[\p{L}\p{N}_]+""")

    private data class CachedRuleSet(
        val rawEntriesSignature: Int,
        val rules: List<CompiledEntry>
    )

    /**
     * Formats the replacement string for audio synthesis execution.
     * PRONUNCIATION entries are formatted as OpenEVV phonetic tags: `[cleanSpr]
     */
    fun formatRuntimeReplacement(entry: UserDictionaryEntry): String {
        if (entry.type != DictionaryEntryType.PRONUNCIATION) {
            return entry.replacement
        }
        val raw = entry.replacement.trim()
        val clean = raw.removePrefix("`").removePrefix("[").removeSuffix(".").removeSuffix("]").trim()
        return "`[$clean]"
    }

    /**
     * Target-level matching helper verifying single target compliance.
     */
    fun matchesTarget(source: String, target: String, mode: MatchMode, caseSensitive: Boolean): Boolean {
        return when (mode) {
            MatchMode.EXACT -> target.equals(source, ignoreCase = !caseSensitive)
            MatchMode.STARTS_WITH -> target.startsWith(source, ignoreCase = !caseSensitive)
            MatchMode.ENDS_WITH -> target.endsWith(source, ignoreCase = !caseSensitive)
            MatchMode.CONTAINS -> target.contains(source, ignoreCase = !caseSensitive)
        }
    }

    /**
     * Compiles raw dictionary entries into an ordered, precedence-sorted rule list.
     */
    fun compileEntries(entries: List<UserDictionaryEntry>): List<CompiledEntry> {
        val compiled = mutableListOf<CompiledEntry>()

        entries.forEachIndexed { index, entry ->
            val source = entry.source
            if (source.isEmpty()) return@forEachIndexed

            val quoted = Pattern.quote(source)
            val startsWithWord = Character.isLetterOrDigit(source.first()) || source.first() == '_'
            val endsWithWord = Character.isLetterOrDigit(source.last()) || source.last() == '_'

            val leftBoundary = if (startsWithWord) "(?<![\\p{L}\\p{N}_])" else ""
            val rightBoundary = if (endsWithWord) "(?![\\p{L}\\p{N}_])" else ""

            val pattern = when (entry.matchMode) {
                MatchMode.EXACT -> "$leftBoundary$quoted$rightBoundary"
                MatchMode.STARTS_WITH -> "$leftBoundary$quoted"
                MatchMode.ENDS_WITH -> "$quoted$rightBoundary"
                MatchMode.CONTAINS -> quoted
            }

            val options = if (entry.caseSensitive) emptySet() else setOf(RegexOption.IGNORE_CASE)
            val regex = try {
                Regex(pattern, options)
            } catch (t: Throwable) {
                Regex(quoted, options)
            }

            val rank = when (entry.matchMode) {
                MatchMode.EXACT -> 1
                MatchMode.STARTS_WITH, MatchMode.ENDS_WITH -> 2
                MatchMode.CONTAINS -> 3
            }

            val runtimeRepl = formatRuntimeReplacement(entry)
            compiled.add(CompiledEntry(entry, regex, rank, index, runtimeRepl))
        }

        // Precedence Strategy:
        // 1. Longer source text length first (more specific)
        // 2. Specificity rank: EXACT (1) > STARTS_WITH/ENDS_WITH (2) > CONTAINS (3)
        // 3. Case-sensitive before case-insensitive
        // 4. Stable original dictionary order
        compiled.sortWith(Comparator { a, b ->
            val lenCmp = b.entry.source.length.compareTo(a.entry.source.length)
            if (lenCmp != 0) return@Comparator lenCmp

            val rankCmp = a.matchModeRank.compareTo(b.matchModeRank)
            if (rankCmp != 0) return@Comparator rankCmp

            val caseCmp = b.entry.caseSensitive.compareTo(a.entry.caseSensitive)
            if (caseCmp != 0) return@Comparator caseCmp

            a.order.compareTo(b.order)
        })

        return compiled
    }

    /**
     * Executes single-pass, non-cascading replacement over input text.
     */
    fun process(text: String, rules: List<CompiledEntry>): String {
        if (text.isEmpty() || rules.isEmpty()) return text

        // For large rule sets (> 100 entries), use optimized index lookup to prevent audio stutter
        if (rules.size > 100) {
            return processOptimized(text, rules)
        }

        data class MatchInterval(
            val start: Int,
            val end: Int,
            val replacement: String
        )

        val acceptedIntervals = mutableListOf<MatchInterval>()

        // For each rule in strict precedence order, collect non-overlapping match intervals
        for (rule in rules) {
            val matches = rule.regex.findAll(text)
            for (match in matches) {
                val start = match.range.first
                val end = match.range.last + 1

                // Check overlap with higher-precedence accepted intervals
                var overlaps = false
                for (existing in acceptedIntervals) {
                    if (start < existing.end && end > existing.start) {
                        overlaps = true
                        break
                    }
                }

                if (!overlaps) {
                    acceptedIntervals.add(MatchInterval(start, end, rule.runtimeReplacement))
                }
            }
        }

        if (acceptedIntervals.isEmpty()) return text

        // Sort accepted intervals chronologically by text start position
        acceptedIntervals.sortBy { it.start }

        val sb = StringBuilder(text.length + 32)
        var cursor = 0
        for (interval in acceptedIntervals) {
            if (interval.start > cursor) {
                sb.append(text, cursor, interval.start)
            }
            sb.append(interval.replacement)
            cursor = interval.end
        }
        if (cursor < text.length) {
            sb.append(text, cursor, text.length)
        }

        return sb.toString()
    }

    /**
     * High-performance processing for large dictionaries (e.g. 68k+ IBM Root dictionaries).
     * Partitions exact single words into hash maps for O(1) lookup while maintaining identical precedence semantics.
     */
    private fun processOptimized(text: String, rules: List<CompiledEntry>): String {
        val exactCS = HashMap<String, CompiledEntry>(rules.size)
        val exactCI = HashMap<String, CompiledEntry>()
        val patternRules = mutableListOf<CompiledEntry>()

        for (rule in rules) {
            val src = rule.entry.source
            val isSimpleWord = rule.entry.matchMode == MatchMode.EXACT &&
                    src.all { Character.isLetterOrDigit(it) || it == '_' }
            if (isSimpleWord) {
                if (rule.entry.caseSensitive) {
                    exactCS.putIfAbsent(src, rule)
                } else {
                    exactCI.putIfAbsent(src.lowercase(), rule)
                }
            } else {
                patternRules.add(rule)
            }
        }

        data class CandidateMatch(
            val start: Int,
            val end: Int,
            val rule: CompiledEntry
        )

        val candidates = mutableListOf<CandidateMatch>()

        // 1. Scan pattern rules
        for (rule in patternRules) {
            for (m in rule.regex.findAll(text)) {
                candidates.add(CandidateMatch(m.range.first, m.range.last + 1, rule))
            }
        }

        // 2. Scan exact word tokens in text
        if (exactCS.isNotEmpty() || exactCI.isNotEmpty()) {
            for (m in WORD_TOKEN_REGEX.findAll(text)) {
                val tok = m.value
                val cs = exactCS[tok]
                if (cs != null) {
                    candidates.add(CandidateMatch(m.range.first, m.range.last + 1, cs))
                } else {
                    val ci = exactCI[tok.lowercase()]
                    if (ci != null) {
                        candidates.add(CandidateMatch(m.range.first, m.range.last + 1, ci))
                    }
                }
            }
        }

        if (candidates.isEmpty()) return text

        candidates.sortWith(Comparator { a, b ->
            val lenCmp = b.rule.entry.source.length.compareTo(a.rule.entry.source.length)
            if (lenCmp != 0) return@Comparator lenCmp
            val rankCmp = a.rule.matchModeRank.compareTo(b.rule.matchModeRank)
            if (rankCmp != 0) return@Comparator rankCmp
            val caseCmp = b.rule.entry.caseSensitive.compareTo(a.rule.entry.caseSensitive)
            if (caseCmp != 0) return@Comparator caseCmp
            a.rule.order.compareTo(b.rule.order)
        })

        data class MatchInterval(val start: Int, val end: Int, val replacement: String)
        val acceptedIntervals = mutableListOf<MatchInterval>()

        for (cand in candidates) {
            var overlaps = false
            for (existing in acceptedIntervals) {
                if (cand.start < existing.end && cand.end > existing.start) {
                    overlaps = true
                    break
                }
            }
            if (!overlaps) {
                acceptedIntervals.add(MatchInterval(cand.start, cand.end, cand.rule.runtimeReplacement))
            }
        }

        if (acceptedIntervals.isEmpty()) return text

        acceptedIntervals.sortBy { it.start }

        val sb = StringBuilder(text.length + 32)
        var cursor = 0
        for (interval in acceptedIntervals) {
            if (interval.start > cursor) {
                sb.append(text, cursor, interval.start)
            }
            sb.append(interval.replacement)
            cursor = interval.end
        }
        if (cursor < text.length) {
            sb.append(text, cursor, text.length)
        }

        return sb.toString()
    }

    /**
     * High-level entry point used by EloqiumTtsService on the synthesis path.
     * Caches compiled rules in-memory for zero disk I/O.
     */
    fun process(
        text: String,
        languageTag: String,
        repository: UserDictionaryRepository,
        enabled: Boolean = true
    ): String {
        if (!enabled || text.isEmpty()) return text

        val cleanTag = languageTag.trim()
        val activeEntries = repository.getActiveEntriesForLanguage(cleanTag)
        if (activeEntries.isEmpty()) return text

        val signature = activeEntries.hashCode()
        val cached = cache[cleanTag]

        val rules = if (cached != null && cached.rawEntriesSignature == signature) {
            cached.rules
        } else {
            val compiled = compileEntries(activeEntries)
            cache[cleanTag] = CachedRuleSet(signature, compiled)
            compiled
        }

        return process(text, rules)
    }

    /**
     * Clears cached compiled rules across all languages.
     */
    fun clearCache() {
        cache.clear()
    }
}
