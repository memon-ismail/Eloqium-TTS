package org.eloqium.tts.pipeline

/**
 * Splits long text into manageable sentence-level chunks for responsive screen reader feedback.
 * Guarantees:
 * - Does not split decimal numbers (3.14), grouped thousands (1,024), or times (2:30).
 * - Does not split dotted abbreviations (e.g., U.S., i.e.) or titles (Mr. Smith, Dr. Brown).
 * - Preserves punctuation attached to the ending word of each chunk.
 * - Never produces empty chunks.
 */
object Chunker {

    const val PIECE_CAP = 500

    private const val CLOSERS = "\"')]}»”’"
    private const val ENDERS = "?!…"

    /**
     * Determines whether [word] terminates a sentence.
     */
    fun endsSentence(word: String): Boolean {
        val tail = word.trimEnd { it in CLOSERS }
        if (tail.isEmpty()) return false
        if (tail.last() in ENDERS) return true
        if (!tail.endsWith('.')) return false
        if (tail.length < 2) return false

        // Decimal numbers: digit before period
        if (tail[tail.length - 2].isDigit()) return false

        val withoutDot = tail.dropLast(1)
        // Multi-dot abbreviations like "e.g." or "U.S."
        if (withoutDot.contains('.')) return false

        // Titles or initials like "Mr.", "Dr.", "J."
        if (withoutDot.length <= 3 && withoutDot.first().isUpperCase()) return false

        return true
    }

    /**
     * Splits [text] into chunks without breaking words, abbreviations, or numbers.
     */
    fun chunk(text: String, cap: Int = PIECE_CAP): List<String> {
        if (text.isEmpty()) return emptyList()
        if (text.length <= cap && !text.any { it.isWhitespace() }) return listOf(text)

        val pieces = ArrayList<String>()
        val current = StringBuilder()
        var gathered = 0
        var sentenceEnded = false

        for (part in runs(text)) {
            current.append(part)
            gathered += part.length
            if (part.isNotBlank()) {
                sentenceEnded = endsSentence(part)
                continue
            }
            // Boundary reached after the whitespace following the ender
            if (sentenceEnded || gathered >= cap) {
                val s = current.toString()
                if (s.isNotBlank()) {
                    pieces.add(s)
                }
                current.setLength(0)
                gathered = 0
            }
            sentenceEnded = false
        }

        if (current.isNotBlank()) {
            pieces.add(current.toString())
        } else if (current.isNotEmpty() && pieces.isNotEmpty()) {
            pieces[pieces.lastIndex] = pieces.last() + current
        }

        return if (pieces.isEmpty()) listOf(text) else pieces
    }

    private fun runs(text: String): List<String> {
        val out = ArrayList<String>()
        var at = 0
        while (at < text.length) {
            val from = at
            val isSpace = text[at].isWhitespace()
            while (at < text.length && text[at].isWhitespace() == isSpace) at++
            out.add(text.substring(from, at))
        }
        return out
    }
}
