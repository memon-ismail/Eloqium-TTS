package org.eloqium.tts.pipeline

/**
 * Built-in dictionary containing common, clean, accessibility-oriented abbreviations.
 * Defined as a structured data model separate from processor execution logic.
 */
object AbbreviationDictionary {

    data class Entry(
        val abbreviation: String,
        val expansion: String,
        val isCaseSensitive: Boolean = false
    )

    val ENTRIES: List<Entry> = listOf(
        Entry("Mr.", "Mister"),
        Entry("Mrs.", "Missus"),
        Entry("Ms.", "Ms"),
        Entry("Dr.", "Doctor"),
        Entry("Prof.", "Professor"),
        Entry("Ave.", "Avenue"),
        Entry("Rd.", "Road"),
        Entry("Blvd.", "Boulevard"),
        Entry("Ln.", "Lane"),
        Entry("Ct.", "Court"),
        Entry("Pl.", "Place"),
        Entry("Apt.", "Apartment"),
        Entry("Ste.", "Suite"),
        Entry("Dept.", "Department"),
        Entry("Gen.", "General"),
        Entry("Col.", "Colonel"),
        Entry("Capt.", "Captain"),
        Entry("Lt.", "Lieutenant"),
        Entry("Sgt.", "Sergeant"),
        Entry("Rev.", "Reverend"),
        Entry("Hon.", "Honorable"),
        Entry("Gov.", "Governor"),
        Entry("Pres.", "President"),
        Entry("Jr.", "Junior"),
        Entry("Sr.", "Senior"),
        Entry("vs.", "versus", isCaseSensitive = false),
        Entry("etc.", "etcetera", isCaseSensitive = false),
        Entry("e.g.", "for example", isCaseSensitive = false),
        Entry("i.e.", "that is", isCaseSensitive = false)
    )
}
