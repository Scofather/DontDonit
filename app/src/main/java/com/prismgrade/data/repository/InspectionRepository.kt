package com.prismgrade.data.repository

import android.net.Uri
import com.prismgrade.data.image.ImageProcessor
import com.prismgrade.data.image.PreparedImage
import com.prismgrade.data.local.InspectionDao
import com.prismgrade.data.local.InspectionEntity
import com.prismgrade.data.local.SettingsStore
import com.prismgrade.data.remote.ClaudeGradingService
import com.prismgrade.domain.model.Authenticity
import com.prismgrade.domain.model.AuthenticityFlag
import com.prismgrade.domain.model.CardIdentification
import com.prismgrade.domain.model.Confidence
import com.prismgrade.domain.model.GradeReport
import com.prismgrade.domain.model.InspectionSource
import com.prismgrade.domain.model.Subgrade
import com.prismgrade.domain.model.Subgrades
import com.prismgrade.domain.model.ValueEstimate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** A saved inspection: the report plus where its photos ended up. */
data class SavedInspection(
    val id: Long,
    val createdAt: Long,
    val report: GradeReport,
    val frontImagePath: String?,
    val backImagePath: String?,
)

/**
 * The single entry point the UI uses: run an inspection, then read history.
 * Owns the order of operations (prepare images -> call Claude -> persist) so no
 * ViewModel has to.
 */
class InspectionRepository(
    private val gradingService: ClaudeGradingService,
    private val imageProcessor: ImageProcessor,
    private val dao: InspectionDao,
    private val settingsStore: SettingsStore,
) {

    val history: Flow<List<SavedInspection>> =
        dao.observeAll().map { rows -> rows.map { it.toSaved() } }

    suspend fun hasApiKey(): Boolean = settingsStore.apiKey.first().isNotBlank()

    /** Prepare a picked/captured photo for display and sending. */
    suspend fun prepareImage(uri: Uri, slot: String): PreparedImage =
        imageProcessor.prepare(uri, slot)

    suspend fun inspectPhotos(front: PreparedImage, back: PreparedImage?): SavedInspection {
        val apiKey = settingsStore.apiKey.first()
        val report = gradingService.gradeFromPhotos(apiKey, front, back)
        return persist(report, front.localFile.absolutePath, back?.localFile?.absolutePath)
    }

    suspend fun inspectDescription(card: String, condition: String): SavedInspection {
        val apiKey = settingsStore.apiKey.first()
        val report = gradingService.gradeFromDescription(apiKey, card, condition)
        return persist(report, frontPath = null, backPath = null)
    }

    suspend fun findById(id: Long): SavedInspection? = dao.findById(id)?.toSaved()

    suspend fun delete(id: Long) {
        // Drop the photos with the row so history doesn't leak files.
        dao.findById(id)?.let { row ->
            listOfNotNull(row.frontImagePath, row.backImagePath)
                .forEach { path -> runCatching { java.io.File(path).delete() } }
        }
        dao.deleteById(id)
    }

    private suspend fun persist(
        report: GradeReport,
        frontPath: String?,
        backPath: String?,
    ): SavedInspection {
        val createdAt = System.currentTimeMillis()
        val id = dao.insert(report.toEntity(createdAt, frontPath, backPath))
        return SavedInspection(id, createdAt, report, frontPath, backPath)
    }
}

// --- mapping ---------------------------------------------------------------

private fun GradeReport.toEntity(
    createdAt: Long,
    frontPath: String?,
    backPath: String?,
) = InspectionEntity(
    createdAt = createdAt,
    cardName = identification.name,
    setName = identification.setName,
    year = identification.year,
    cardNumber = identification.cardNumber,
    confidence = identification.confidence.name,
    valueLow = valueEstimate.low,
    valueHigh = valueEstimate.high,
    currency = valueEstimate.currency,
    valueBasis = valueEstimate.basis,
    overallGrade = overallGrade,
    gradeLabel = resolvedGradeLabel,
    centeringScore = subgrades.centering.score,
    centeringNote = subgrades.centering.note,
    cornersScore = subgrades.corners.score,
    cornersNote = subgrades.corners.note,
    edgesScore = subgrades.edges.score,
    edgesNote = subgrades.edges.note,
    surfaceScore = subgrades.surface.score,
    surfaceNote = subgrades.surface.note,
    authenticityFlag = authenticity.flag.name,
    authenticityNote = authenticity.note,
    findings = findings.joinToString("\n"),
    source = source.name,
    frontImagePath = frontPath,
    backImagePath = backPath,
)

private fun InspectionEntity.toSaved() = SavedInspection(
    id = id,
    createdAt = createdAt,
    frontImagePath = frontImagePath,
    backImagePath = backImagePath,
    report = GradeReport(
        identification = CardIdentification(
            name = cardName,
            setName = setName,
            year = year,
            cardNumber = cardNumber,
            confidence = runCatching { Confidence.valueOf(confidence) }.getOrDefault(Confidence.LOW),
        ),
        valueEstimate = ValueEstimate(valueLow, valueHigh, currency, valueBasis),
        subgrades = Subgrades(
            centering = Subgrade(centeringScore, centeringNote),
            corners = Subgrade(cornersScore, cornersNote),
            edges = Subgrade(edgesScore, edgesNote),
            surface = Subgrade(surfaceScore, surfaceNote),
        ),
        overallGrade = overallGrade,
        gradeLabel = gradeLabel,
        authenticity = Authenticity(
            flag = runCatching { AuthenticityFlag.valueOf(authenticityFlag) }
                .getOrDefault(AuthenticityFlag.REVIEW_SUGGESTED),
            note = authenticityNote,
        ),
        findings = findings.split("\n").filter { it.isNotBlank() },
        source = runCatching { InspectionSource.valueOf(source) }
            .getOrDefault(InspectionSource.PHOTOS),
    ),
)
