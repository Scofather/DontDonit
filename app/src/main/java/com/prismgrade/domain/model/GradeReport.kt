package com.prismgrade.domain.model

/**
 * The result of one inspection: what the card is, what it is worth, and how it
 * would likely grade. Mirrors the JSON contract in
 * [com.prismgrade.data.remote.GradingPrompts].
 */
data class GradeReport(
    val identification: CardIdentification,
    val valueEstimate: ValueEstimate,
    val subgrades: Subgrades,
    val overallGrade: Int,
    val gradeLabel: String,
    val authenticity: Authenticity,
    val findings: List<String>,
    val source: InspectionSource,
) {
    /** Label to show when the model didn't supply one. */
    val resolvedGradeLabel: String
        get() = gradeLabel.ifBlank { GradeScale.labelFor(overallGrade) }
}

data class CardIdentification(
    val name: String,
    val setName: String,
    val year: String,
    val cardNumber: String,
    val confidence: Confidence,
) {
    /** "Base Set · 1999 · #4/102", skipping whatever is missing. */
    val subtitle: String
        get() = listOf(
            setName,
            year,
            cardNumber.takeIf { it.isNotBlank() }?.let { "#$it" }.orEmpty(),
        ).filter { it.isNotBlank() }.joinToString(" · ")
}

enum class Confidence {
    HIGH, MEDIUM, LOW;

    companion object {
        fun parse(raw: String?): Confidence = when (raw?.trim()?.lowercase()) {
            "high" -> HIGH
            "medium" -> MEDIUM
            else -> LOW
        }
    }
}

data class ValueEstimate(
    val low: Int,
    val high: Int,
    val currency: String,
    val basis: String,
)

data class Subgrades(
    val centering: Subgrade,
    val corners: Subgrade,
    val edges: Subgrade,
    val surface: Subgrade,
) {
    fun asList(): List<Pair<String, Subgrade>> = listOf(
        "Centering" to centering,
        "Corners" to corners,
        "Edges" to edges,
        "Surface" to surface,
    )
}

data class Subgrade(
    val score: Int,
    val note: String,
)

data class Authenticity(
    val flag: AuthenticityFlag,
    val note: String,
)

enum class AuthenticityFlag {
    NO_CONCERNS, REVIEW_SUGGESTED;

    companion object {
        fun parse(raw: String?): AuthenticityFlag =
            if (raw?.trim()?.lowercase()?.startsWith("no concern") == true) NO_CONCERNS
            else REVIEW_SUGGESTED
    }

    val display: String
        get() = if (this == NO_CONCERNS) "No concerns" else "Review suggested"
}

/** How the grade was reached — photos carry more weight than a description. */
enum class InspectionSource {
    PHOTOS, DESCRIPTION;

    val display: String
        get() = if (this == PHOTOS) "Photo inspection" else "From your description"
}

/** The 1-10 scale grading companies use, and the labels that go with it. */
object GradeScale {
    private val LABELS = mapOf(
        10 to "Gem Mint",
        9 to "Mint",
        8 to "NM-MT",
        7 to "Near Mint",
        6 to "EX-MT",
        5 to "Excellent",
        4 to "VG-EX",
        3 to "Very Good",
        2 to "Good",
        1 to "Poor",
    )

    fun labelFor(grade: Int): String = LABELS[grade.coerceIn(1, 10)] ?: "—"

    fun clamp(grade: Int): Int = grade.coerceIn(1, 10)
}
