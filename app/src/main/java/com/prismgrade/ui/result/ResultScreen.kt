package com.prismgrade.ui.result

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.prismgrade.data.repository.SavedInspection
import com.prismgrade.domain.model.Confidence
import com.prismgrade.domain.model.InspectionSource
import com.prismgrade.ui.components.AuthenticityRow
import com.prismgrade.ui.components.FindingRow
import com.prismgrade.ui.components.FinePrint
import com.prismgrade.ui.components.GradeBadge
import com.prismgrade.ui.components.PrismChip
import com.prismgrade.ui.components.SectionLabel
import com.prismgrade.ui.components.SubgradeMeter
import com.prismgrade.ui.theme.PrismColors
import java.io.File
import java.text.NumberFormat
import java.util.Locale

/** The console readout for one inspection. */
@Composable
fun ResultScreen(
    inspection: SavedInspection?,
    modifier: Modifier = Modifier,
) {
    if (inspection == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PrismColors.Scan)
        }
        return
    }

    val report = inspection.report

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PrismChip(
                text = report.source.display,
                color = if (report.source == InspectionSource.PHOTOS) PrismColors.Good
                else PrismColors.Uv,
                borderColor = if (report.source == InspectionSource.PHOTOS) PrismColors.Good
                else PrismColors.Uv,
            )
            PrismChip(text = "confidence: ${report.identification.confidence.name.lowercase()}")
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = report.identification.name,
                style = MaterialTheme.typography.headlineMedium,
                color = PrismColors.Ink,
            )
            if (report.identification.subtitle.isNotBlank()) {
                Text(
                    text = report.identification.subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = PrismColors.InkDim,
                )
            }
        }

        if (report.identification.confidence == Confidence.LOW) {
            Text(
                text = "Low confidence on the identification — the value below assumes this is " +
                    "the right card, so check the name before trusting the range.",
                style = MaterialTheme.typography.bodyMedium,
                color = PrismColors.Warn,
            )
        }

        InspectionPhotos(inspection)

        GradeBadge(
            grade = report.overallGrade,
            label = report.resolvedGradeLabel,
            modifier = Modifier.fillMaxWidth(),
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SectionLabel("Estimated value")
            Text(
                text = "${money(report.valueEstimate.low)} – ${money(report.valueEstimate.high)}",
                fontSize = 30.sp,
                fontFamily = FontFamily.Monospace,
                color = PrismColors.Gold,
            )
            Text(
                text = report.valueEstimate.basis.ifBlank {
                    "An estimate from general market knowledge — not live sold listings."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = PrismColors.InkFaint,
            )
        }

        HorizontalDivider(color = PrismColors.Line)

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SectionLabel("Subgrades")
            report.subgrades.asList().forEach { (label, subgrade) ->
                SubgradeMeter(label = label, subgrade = subgrade)
            }
        }

        AuthenticityRow(report.authenticity)

        if (report.findings.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("Findings")
                report.findings.forEach { FindingRow(it) }
            }
        }

        HorizontalDivider(color = PrismColors.Line)

        FinePrint(
            if (report.source == InspectionSource.DESCRIPTION) {
                "Graded from your written description, not from photos — it can only reflect what " +
                    "you described. Prism Grade is an AI estimate, never a certified grade."
            } else {
                "Prism Grade is an AI estimate, not a certified grade from any grading company. " +
                    "For cards worth submitting, get a physical opinion first."
            },
        )
    }
}

@Composable
private fun InspectionPhotos(inspection: SavedInspection) {
    val paths = listOfNotNull(inspection.frontImagePath, inspection.backImagePath)
    if (paths.isEmpty()) return

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        paths.forEach { path ->
            AsyncImage(
                model = File(path),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .weight(1f)
                    .height(150.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .border(1.dp, PrismColors.Line, RoundedCornerShape(4.dp))
                    .background(PrismColors.Panel),
            )
        }
    }
}

private fun money(amount: Int): String =
    NumberFormat.getCurrencyInstance(Locale.US).apply { maximumFractionDigits = 0 }.format(amount)
