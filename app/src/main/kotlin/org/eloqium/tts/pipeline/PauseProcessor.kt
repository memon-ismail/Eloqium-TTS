package org.eloqium.tts.pipeline

/**
 * Manages clause pause durations, trailing utterance gaps, and phrase prediction tags.
 */
object PauseProcessor {

    const val PAUSE_KEEP = 0
    const val PAUSE_END_ONLY = 1
    const val PAUSE_ALL = 2

    private const val PHRASE_PREDICTION_ON = "`pp1 "
    private const val PHRASE_PREDICTION_OFF = "`pp0 "

    private const val MARKS = "-,.:;?!–—"
    // Punctuation following a word or digit, followed by space or end
    private val AT_A_MARK = Regex("""([A-Za-z0-9])([$MARKS])(\2*)(?=\s|${'$'})""")
    private const val BRIEF_PAUSE = "`p1"
    private const val END_PAUSE = "`p100"

    fun process(
        text: String,
        pauseMode: Int = PAUSE_KEEP,
        phrasePrediction: Boolean = true,
        isLastChunk: Boolean = true
    ): String {
        if (text.isEmpty()) return text
        var out = text

        if (pauseMode == PAUSE_ALL) {
            // Shorten pauses at punctuation while keeping mark bound to word
            out = AT_A_MARK.replace(out, "$1 $BRIEF_PAUSE$2$3")
        }

        if (isLastChunk && pauseMode != PAUSE_KEEP) {
            val trimmed = out.trimEnd()
            if (trimmed.isNotEmpty() && trimmed.last() !in MARKS) {
                out = "$trimmed $END_PAUSE"
            }
        }

        val prefix = if (phrasePrediction) PHRASE_PREDICTION_ON else PHRASE_PREDICTION_OFF
        return prefix + out
    }
}
