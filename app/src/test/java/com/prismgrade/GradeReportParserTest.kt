package com.prismgrade

import com.prismgrade.data.remote.GradeReportParser
import com.prismgrade.domain.model.AuthenticityFlag
import com.prismgrade.domain.model.Confidence
import com.prismgrade.domain.model.InspectionSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The parser is the one place a model's sloppiness turns into a crash, so the
 * cases below are the shapes Claude actually returns in practice — clean JSON,
 * fenced JSON, prose around the object, and missing fields.
 */
class GradeReportParserTest {

    private val complete = """
        {"identification":{"name":"Charizard","setName":"Base Set","year":"1999",
        "cardNumber":"4/102","confidence":"high"},
        "valueEstimate":{"low":180,"high":260,"currency":"USD","basis":"Raw unlimited print."},
        "subgrades":{"centering":{"score":8,"note":"60/40"},"corners":{"score":7,"note":"whitening"},
        "edges":{"score":8,"note":"clean"},"surface":{"score":7,"note":"print lines"}},
        "overallGrade":7,"gradeLabel":"Near Mint",
        "authenticity":{"flag":"no concerns","note":"Stock matches."},
        "findings":["Centering near 60/40.","Corner whitening bottom right."]}
    """.trimIndent()

    @Test
    fun `parses a complete reply`() {
        val report = GradeReportParser.parse(complete, InspectionSource.PHOTOS)

        assertEquals("Charizard", report.identification.name)
        assertEquals(Confidence.HIGH, report.identification.confidence)
        assertEquals("Base Set · 1999 · #4/102", report.identification.subtitle)
        assertEquals(180, report.valueEstimate.low)
        assertEquals(7, report.overallGrade)
        assertEquals("Near Mint", report.resolvedGradeLabel)
        assertEquals(8, report.subgrades.centering.score)
        assertEquals(AuthenticityFlag.NO_CONCERNS, report.authenticity.flag)
        assertEquals(2, report.findings.size)
        assertEquals(InspectionSource.PHOTOS, report.source)
    }

    @Test
    fun `parses JSON wrapped in a code fence`() {
        val report = GradeReportParser.parse("```json\n$complete\n```", InspectionSource.PHOTOS)
        assertEquals("Charizard", report.identification.name)
    }

    @Test
    fun `parses JSON with a sentence around it`() {
        val reply = "Here is the grade:\n$complete\nHope that helps."
        val report = GradeReportParser.parse(reply, InspectionSource.PHOTOS)
        assertEquals(7, report.overallGrade)
    }

    @Test
    fun `fills in a grade label when the model omits one`() {
        val reply = """{"overallGrade":9,"gradeLabel":""}"""
        val report = GradeReportParser.parse(reply, InspectionSource.DESCRIPTION)
        assertEquals("Mint", report.resolvedGradeLabel)
    }

    @Test
    fun `degrades gracefully when fields are missing`() {
        val report = GradeReportParser.parse("{}", InspectionSource.DESCRIPTION)

        assertEquals("Unidentified card", report.identification.name)
        assertEquals(Confidence.LOW, report.identification.confidence)
        assertEquals(0, report.valueEstimate.low)
        assertEquals("USD", report.valueEstimate.currency)
        assertTrue(report.findings.isEmpty())
        // An absent grade must still be inside the 1-10 scale the UI draws.
        assertTrue(report.overallGrade in 1..10)
    }

    @Test
    fun `clamps out-of-range scores`() {
        val reply = """{"overallGrade":47,"subgrades":{"centering":{"score":-3,"note":""}}}"""
        val report = GradeReportParser.parse(reply, InspectionSource.PHOTOS)

        assertEquals(10, report.overallGrade)
        assertEquals(1, report.subgrades.centering.score)
    }

    @Test
    fun `reads scores the model wrote as text`() {
        val reply = """{"overallGrade":"8","subgrades":{"corners":{"score":"7.5","note":""}}}"""
        val report = GradeReportParser.parse(reply, InspectionSource.PHOTOS)

        assertEquals(8, report.overallGrade)
        assertEquals(7, report.subgrades.corners.score)
    }

    @Test(expected = GradeReportParser.MalformedReplyException::class)
    fun `rejects a reply with no JSON at all`() {
        GradeReportParser.parse("I can't grade this card.", InspectionSource.PHOTOS)
    }
}
