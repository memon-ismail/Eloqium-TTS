package org.eloqium.tts.pipeline

import org.eloqium.tts.service.SettingsDefaults
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Preprocessor that transforms numbers into Digits, Pairs, Triplets, or Smart mode before synthesis.
 */
object NumberProcessor {

    private val NUMBER_REGEX = Regex("""\d+""")
    private val PHONETIC_TAG_REGEX = Regex("""`\[[^\]\r\n\t]+\]""")

    // Contexts protected from digit conversion in Smart mode
    private val PHONETIC_TAG_PATTERN = Pattern.compile("`\\[[^\\]\\r\\n\\t]+\\]")
    private val CURRENCY_PATTERN = Pattern.compile("([$€£¥₹¢]|\\b(?:USD|EUR|GBP|INR|Rs\\.?)\\s*)\\d+(?:[.,]\\d+)?")
    private val PERCENT_PATTERN = Pattern.compile("\\d+(?:[.,]\\d+)?\\s*%")
    private val DECIMAL_PATTERN = Pattern.compile("\\b\\d+\\.\\d+\\b")
    private val COMMA_NUM_PATTERN = Pattern.compile("\\b\\d{1,3}(?:,\\d{3})+\\b")
    private val DATE_PATTERN = Pattern.compile("\\b\\d{1,4}[/-]\\d{1,2}[/-]\\d{1,4}\\b")
    private val TIME_PATTERN = Pattern.compile("\\b\\d{1,2}:\\d{2}(?::\\d{2})?\\b")
    private val VERSION_PATTERN = Pattern.compile("\\b(?:version|v\\.|ver\\.|build)\\s+\\d+\\b", Pattern.CASE_INSENSITIVE)
    private val QUANTITY_PATTERN = Pattern.compile(
        "\\b\\d+\\s+(?:apples|oranges|items|people|users|dollars|euros|cents|miles|km|meters|seconds|minutes|hours|days|weeks|months|years|percent|times|books|cars|houses|points|steps|bytes|kb|mb|gb)\\b",
        Pattern.CASE_INSENSITIVE
    )
    private val ALPHANUMERIC_PATTERN = Pattern.compile("\\b(?=[A-Za-z0-9]*[A-Za-z])(?=[A-Za-z0-9]*\\d)[A-Za-z0-9]+\\b")
    private val OTP_CONTEXT_PATTERN = Pattern.compile(
        "\\b(?:otp|code|pin|passcode|password|verification|security code|verify)\\b",
        Pattern.CASE_INSENSITIVE
    )
    private val DIGIT_RUN_PATTERN = Pattern.compile("\\b\\d+\\b")

    // Formatted phone numbers: (987) 654-3210, 987-654-3210, 987 654 3210, (987)654-3210,
    // optionally with international country prefix +91, +1, etc.
    private val FORMATTED_PHONE_PATTERN = Pattern.compile(
        """(?:\+(\d{1,3})\s*)?(?:(?:\((\d{3})\)\s*|(\d{3})[-. ])(\d{3})[-. ](\d{4})|(\d{5})[-. ](\d{5}))\b"""
    )

    fun process(
        text: String,
        enabled: Boolean = false,
        mode: Int = SettingsDefaults.NUMBER_DIGITS
    ): String {
        if (!enabled || text.isEmpty()) return text

        if (mode == SettingsDefaults.NUMBER_SMART) {
            return processSmart(text)
        }

        if (!text.contains("`[")) {
            return NUMBER_REGEX.replace(text) { match ->
                val num = match.value
                formatNumber(num, mode)
            }
        }

        val sb = StringBuilder(text.length + 16)
        var cursor = 0
        for (match in PHONETIC_TAG_REGEX.findAll(text)) {
            val start = match.range.first
            val end = match.range.last + 1
            if (start > cursor) {
                sb.append(NUMBER_REGEX.replace(text.substring(cursor, start)) { m ->
                    formatNumber(m.value, mode)
                })
            }
            sb.append(match.value)
            cursor = end
        }
        if (cursor < text.length) {
            sb.append(NUMBER_REGEX.replace(text.substring(cursor)) { m ->
                formatNumber(m.value, mode)
            })
        }
        return sb.toString()
    }

    /**
     * Smart number processing: identifies whether a number represents an ordinary number/amount
     * (e.g. 100, 2026, $500, 50%, 16,000, 3.14, 5 apples) or an OTP/phone number sequence.
     * Sequences of 7+ digits, formatted phone numbers, and 4-8 digit OTP codes are grouped with natural comma pauses.
     */
    fun processSmart(text: String): String {
        if (text.isEmpty()) return text

        // Pass 1: Formatted phone numbers (e.g. 987-654-3210, (987) 654-3210, 987 654 3210, +91 987-654-3210)
        val pass1 = processFormattedPhones(text)

        // Pass 2: Raw digit runs (unformatted phone numbers, OTPs, years, ordinary numbers)
        return processDigitRuns(pass1)
    }

    private fun processFormattedPhones(text: String): String {
        val protectedMask = BooleanArray(text.length)
        markPattern(protectedMask, PHONETIC_TAG_PATTERN.matcher(text))
        markPattern(protectedMask, CURRENCY_PATTERN.matcher(text))
        markPattern(protectedMask, PERCENT_PATTERN.matcher(text))
        markPattern(protectedMask, DECIMAL_PATTERN.matcher(text))
        markPattern(protectedMask, COMMA_NUM_PATTERN.matcher(text))
        markPattern(protectedMask, DATE_PATTERN.matcher(text))
        markPattern(protectedMask, TIME_PATTERN.matcher(text))
        markPattern(protectedMask, VERSION_PATTERN.matcher(text))
        markPattern(protectedMask, QUANTITY_PATTERN.matcher(text))
        markPattern(protectedMask, ALPHANUMERIC_PATTERN.matcher(text))

        val m = FORMATTED_PHONE_PATTERN.matcher(text)
        val sb = StringBuilder(text.length + 16)
        var lastIdx = 0

        while (m.find()) {
            val start = m.start()
            val end = m.end()

            var isProtected = false
            for (k in start until end) {
                if (protectedMask[k]) {
                    isProtected = true
                    break
                }
            }

            sb.append(text, lastIdx, start)
            if (isProtected) {
                sb.append(text, start, end)
            } else {
                val country = m.group(1)
                val areaParen = m.group(2)
                val areaPlain = m.group(3)
                val exchange = m.group(4)
                val line = m.group(5)
                val five1 = m.group(6)
                val five2 = m.group(7)

                val phoneDigits = StringBuilder()
                if (country != null) {
                    phoneDigits.append("+").append(country).append(" ")
                }

                if (exchange != null && line != null) {
                    val area = areaParen ?: areaPlain ?: ""
                    phoneDigits.append(toSpacedDigits(area))
                        .append(", ")
                        .append(toSpacedDigits(exchange))
                        .append(", ")
                        .append(toSpacedDigits(line))
                } else if (five1 != null && five2 != null) {
                    phoneDigits.append(toSpacedDigits(five1))
                        .append(", ")
                        .append(toSpacedDigits(five2))
                } else {
                    phoneDigits.append(text, start, end)
                }
                sb.append(phoneDigits)
            }
            lastIdx = end
        }
        sb.append(text.substring(lastIdx))
        return sb.toString()
    }

    private fun processDigitRuns(text: String): String {
        val protectedMask = BooleanArray(text.length)
        markPattern(protectedMask, PHONETIC_TAG_PATTERN.matcher(text))
        markPattern(protectedMask, CURRENCY_PATTERN.matcher(text))
        markPattern(protectedMask, PERCENT_PATTERN.matcher(text))
        markPattern(protectedMask, DECIMAL_PATTERN.matcher(text))
        markPattern(protectedMask, COMMA_NUM_PATTERN.matcher(text))
        markPattern(protectedMask, DATE_PATTERN.matcher(text))
        markPattern(protectedMask, TIME_PATTERN.matcher(text))
        markPattern(protectedMask, VERSION_PATTERN.matcher(text))
        markPattern(protectedMask, QUANTITY_PATTERN.matcher(text))
        markPattern(protectedMask, ALPHANUMERIC_PATTERN.matcher(text))

        val m = DIGIT_RUN_PATTERN.matcher(text)
        val result = StringBuilder(text.length + 16)
        var lastIdx = 0

        while (m.find()) {
            val start = m.start()
            val end = m.end()
            result.append(text, lastIdx, start)

            var isProtected = false
            for (k in start until end) {
                if (protectedMask[k]) {
                    isProtected = true
                    break
                }
            }

            val digits = m.group()
            if (isProtected) {
                result.append(digits)
            } else {
                val len = digits.length
                val windowStart = (start - 30).coerceAtLeast(0)
                val windowEnd = (end + 30).coerceAtMost(text.length)
                val window = text.substring(windowStart, windowEnd)
                val isOtp = OTP_CONTEXT_PATTERN.matcher(window).find()

                if (isOtp && len in 4..8) {
                    result.append(formatOtp(digits))
                } else if (len < 7) {
                    result.append(digits)
                } else {
                    result.append(formatSmartDigits(digits))
                }
            }
            lastIdx = end
        }
        result.append(text.substring(lastIdx))
        return result.toString()
    }

    fun formatOtp(digits: String): String {
        return when (digits.length) {
            4 -> toSpacedDigits(digits.substring(0, 2)) + ", " + toSpacedDigits(digits.substring(2))
            5 -> toSpacedDigits(digits.substring(0, 3)) + ", " + toSpacedDigits(digits.substring(3))
            6 -> toSpacedDigits(digits.substring(0, 3)) + ", " + toSpacedDigits(digits.substring(3))
            7 -> toSpacedDigits(digits.substring(0, 3)) + ", " + toSpacedDigits(digits.substring(3))
            8 -> toSpacedDigits(digits.substring(0, 4)) + ", " + toSpacedDigits(digits.substring(4))
            else -> toSpacedDigits(digits)
        }
    }

    private fun markPattern(mask: BooleanArray, m: Matcher) {
        while (m.find()) {
            val start = m.start()
            val end = m.end()
            for (k in start until end) {
                mask[k] = true
            }
        }
    }

    fun formatSmartDigits(digits: String): String {
        val len = digits.length
        return when (len) {
            7 -> toSpacedDigits(digits.substring(0, 3)) + ", " + toSpacedDigits(digits.substring(3))
            8 -> toSpacedDigits(digits.substring(0, 4)) + ", " + toSpacedDigits(digits.substring(4))
            9 -> toSpacedDigits(digits.substring(0, 3)) + ", " + toSpacedDigits(digits.substring(3, 6)) + ", " + toSpacedDigits(digits.substring(6))
            10 -> toSpacedDigits(digits.substring(0, 5)) + ", " + toSpacedDigits(digits.substring(5))
            12 -> toSpacedDigits(digits.substring(0, 4)) + ", " + toSpacedDigits(digits.substring(4, 8)) + ", " + toSpacedDigits(digits.substring(8))
            else -> {
                val sb = StringBuilder()
                val chunkLen = if (len % 3 == 0) 3 else 4
                var idx = 0
                while (idx < len) {
                    val end = (idx + chunkLen).coerceAtMost(len)
                    if (sb.isNotEmpty()) sb.append(", ")
                    sb.append(toSpacedDigits(digits.substring(idx, end)))
                    idx = end
                }
                sb.toString()
            }
        }
    }

    private fun toSpacedDigits(s: String): String {
        val sb = StringBuilder()
        for (i in s.indices) {
            if (i > 0) sb.append(' ')
            sb.append(s[i])
        }
        return sb.toString()
    }

    fun formatNumber(digits: String, mode: Int): String {
        if (digits.length <= 1) return digits

        return when (mode) {
            SettingsDefaults.NUMBER_DIGITS -> {
                digits.toCharArray().joinToString(" ")
            }
            SettingsDefaults.NUMBER_PAIRS -> {
                // Group in pairs: e.g. "1984" -> "19 84", "123" -> "12 3", "123456" -> "12 34 56"
                digits.chunked(2).joinToString(" ")
            }
            SettingsDefaults.NUMBER_TRIPLETS -> {
                // Group in triplets from right (standard number grouping): e.g. "1234567" -> "1 234 567", "1000" -> "1 000"
                val len = digits.length
                val firstGroupLen = if (len % 3 == 0) 3 else len % 3
                val sb = StringBuilder()
                sb.append(digits.substring(0, firstGroupLen))
                var idx = firstGroupLen
                while (idx < len) {
                    sb.append(" ")
                    sb.append(digits.substring(idx, idx + 3))
                    idx += 3
                }
                sb.toString()
            }
            SettingsDefaults.NUMBER_SMART -> {
                formatSmartDigits(digits)
            }
            else -> digits
        }
    }
}
