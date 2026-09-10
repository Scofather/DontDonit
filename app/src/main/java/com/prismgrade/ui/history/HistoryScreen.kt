package com.prismgrade.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.prismgrade.data.repository.SavedInspection
import com.prismgrade.ui.components.SectionLabel
import com.prismgrade.ui.theme.PrismColors
import java.io.File
import java.text.DateFormat
import java.util.Date

/** Every inspection this device has run, newest first. */
@Composable
fun HistoryScreen(
    inspections: List<SavedInspection>,
    onOpen: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (inspections.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "No inspections yet. Grade a card and it lands here.",
                style = MaterialTheme.typography.bodyMedium,
                color = PrismColors.InkFaint,
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(inspections, key = { it.id }) { inspection ->
            HistoryRow(
                inspection = inspection,
                onOpen = { onOpen(inspection.id) },
                onDelete = { onDelete(inspection.id) },
            )
        }
    }
}

@Composable
private fun HistoryRow(
    inspection: SavedInspection,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    val report = inspection.report

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(PrismColors.Panel)
            .border(1.dp, PrismColors.Line, RoundedCornerShape(4.dp))
            .clickable(onClick = onOpen)
            .padding(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(PrismColors.PanelRaised)
                .border(1.dp, PrismColors.Line, RoundedCornerShape(3.dp)),
            contentAlignment = Alignment.Center,
        ) {
            val thumb = inspection.frontImagePath
            if (thumb != null) {
                AsyncImage(
                    model = File(thumb),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text(
                    text = report.overallGrade.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = PrismColors.Gold,
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = report.identification.name,
                style = MaterialTheme.typography.bodyLarge,
                color = PrismColors.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${report.overallGrade} · ${report.resolvedGradeLabel}",
                style = MaterialTheme.typography.labelMedium,
                color = PrismColors.Gold,
            )
            SectionLabel(
                text = DateFormat.getDateInstance(DateFormat.MEDIUM)
                    .format(Date(inspection.createdAt)),
            )
        }

        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete inspection",
                tint = PrismColors.InkFaint,
            )
        }
    }
}
