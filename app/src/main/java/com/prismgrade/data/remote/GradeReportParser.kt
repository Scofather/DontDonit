package com.prismgrade.data.remote

import com.prismgrade.domain.model.Authenticity
import com.prismgrade.domain.model.AuthenticityFlag
import com.prismgrade.domain.model.CardIdentification
import com.prismgrade.domain.model.Confidence
import com.prismgrade.domain.model.GradeReport
import com.prismgrade.domain.model.GradeScale
import com.prismgrade.domain.model.InspectionSource
import com.prismgrade.domain.model.Subgrade
import com.prismgrade.domain.model.Subgrades
import com.prismgrade.domain.model.ValueEstimate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Turns Claude's reply into a [GradeReport].
 *
 * Read tolerantly on purpose: the model is asked for a bare JSON object, but a
 * stray sentence or a code fence around it shouldn't lose the whole inspection.
 * Missing fields degrade to sensible values rather than throwing.
 */
object GradeReportParser {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    class MalformedReplyException(val reply: String) :
        IllegalStateException("No JSON object found in the reply")

    fun parse(reply: String, source: InspectionSource): GradeReport {
        val obj = extractObject(reply) ?: throw MalformedReplyException(reply)

        val identification = obj.child("identification")
        val value = obj.child("valueEstimate")
        val subgrades = obj.child("subgrades")
        val authenticity = obj.child("authenticity")

        return GradeReport(
            identification = CardIdentification(
                name = identification.string("name").ifBlank { "Unidentified card" },
                setName = identification.string("setName"),
                year = identification.string("year"),
                cardNumber = identification.string("cardNumber"),
                confidence = Confidence.parse(identification.string("confidence")),
            ),
            valueEstimate = ValueEstimate(
                low = value.int("low", 0),
                high = value.int("high", 0),
                currency = value.string("currency").ifBlank { "USD" },
                basis = value.string("basis"),
            ),
            subgrades = Subgrades(
                centering = subgrades.subgrade("centering"),
                corners = subgrades.subgrade("corners"),
                edges = subgrades.subgrade("edges"),
                surface = subgrades.subgrade("surface"),
            ),
            overallGrade = GradeScale.clamp(obj.int("overallGrade", 1)),
            gradeLabel = obj.string("gradeLabel"),
            authenticity = Authenticity(
                flag = AuthenticityFlag.parse(authenticity.string("flag")),
                note = authenticity.string("note"),
            ),
            findings = obj["findings"]?.asStringList().orEmpty(),
            source = source,
        )
    }

    /**
     * Pull the JSON object out of a reply: the whole string, the body of a
     * fenced block, or the span from the first `{` to the last `}`.
     */
    private fun extractObject(reply: String): JsonObject? {
        val candidates = buildList {
            add(reply.trim())
            Regex("```(?:json)?\\s*(\\{.*?})\\s*```", RegexOption.DOT_MATCHES_ALL)
                .find(reply)?.groupValues?.getOrNull(1)?.let { add(it) }
            val first = reply.indexOf('{')
            val last = reply.lastIndexOf('}')
            if (first in 0..<last) add(reply.substring(first, last + 1))
        }
        for (candidate in candidates) {
            runCatching { json.parseToJsonElement(candidate).jsonObject }
                .getOrNull()
                ?.let { return it }
        }
        return null
    }

    // --- lenient accessors -------------------------------------------------

    private fun JsonObject?.child(key: String): JsonObject? =
        this?.get(key)?.let { runCatching { it.jsonObject }.getOrNull() }

    private fun JsonObject?.string(key: String): String =
        this?.get(key)?.let { runCatching { it.jsonPrimitive.contentOrNull }.getOrNull() }
            ?.takeIf { it != "null" }
            .orEmpty()
            .trim()

    private fun JsonObject?.int(key: String, fallback: Int): Int {
        val primitive = this?.get(key)?.let { runCatching { it.jsonPrimitive }.getOrNull() }
            ?: return fallback
        primitive.intOrNull?.let { return it }
        // Models sometimes answer "8" or "8.5" as text.
        return primitive.contentOrNull?.trim()?.toDoubleOrNull()?.toInt() ?: fallback
    }

    private fun JsonObject?.subgrade(key: String): Subgrade {
        val node = child(key)
        return Subgrade(
            score = GradeScale.clamp(node.int("score", 1)),
            note = node.string("note"),
        )
    }

    private fun JsonElement.asStringList(): List<String> =
        runCatching {
            jsonArray.mapNotNull { it.jsonPrimitive.contentOrNull?.trim() }
                .filter { it.isNotEmpty() }
        }.getOrDefault(emptyList())
}
