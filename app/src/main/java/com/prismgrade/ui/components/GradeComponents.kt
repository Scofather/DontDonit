package com.prismgrade.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prismgrade.domain.model.Authenticity
import com.prismgrade.domain.model.AuthenticityFlag
import com.prismgrade.domain.model.Subgrade
import com.prismgrade.ui.theme.PrismColors

/** A section label: uppercase mono, used to head every block in the console. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, color: Color = PrismColors.InkFaint) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = modifier,
    )
}

/** A bordered chip. Used for source, confidence, and status. */
@Composable
fun PrismChip(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = PrismColors.InkDim,
    borderColor: Color = PrismColors.Line,
) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = modifier
            .border(1.dp, borderColor, RoundedCornerShape(2.dp))
            .padding(horizontal = 9.dp, vertical = 4.dp),
    )
}

/**
 * The headline grade, styled like a slab label — the one place gold is spent
 * at size, because the number is what the whole screen exists to deliver.
 */
@Composable
fun GradeBadge(
    grade: Int,
    label: String,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(PrismColors.PanelRaised)
            .border(1.dp, Color(0xFF4D3C1C), RoundedCornerShape(4.dp))
            .padding(horizontal = 22.dp, vertical = 18.dp),
    ) {
        Text(
            text = grade.toString(),
            fontSize = 46.sp,
            fontWeight = FontWeight.Bold,
            color = PrismColors.Gold,
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.titleLarge,
                color = PrismColors.Ink,
            )
            Text(
                text = "overall · 1–10 scale",
                style = MaterialTheme.typography.labelSmall,
                color = PrismColors.InkFaint,
            )
        }
    }
}

/**
 * One subgrade as a labelled meter. The bar makes four scores comparable at a
 * glance in a way four numbers never are.
 */
@Composable
fun SubgradeMeter(
    label: String,
    subgrade: Subgrade,
    modifier: Modifier = Modifier,
) {
    val fraction by animateFloatAsState(
        targetValue = subgrade.score.coerceIn(1, 10) / 10f,
        label = "subgrade-$label",
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            SectionLabel(label, color = PrismColors.InkDim)
            Text(
                text = "${subgrade.score}/10",
                style = MaterialTheme.typography.labelMedium,
                color = PrismColors.Ink,
            )
        }

        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(PrismColors.PanelRaised)
                .border(1.dp, PrismColors.Line, RoundedCornerShape(1.dp)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(8.dp)
                    .background(meterColor(subgrade.score)),
            )
        }

        if (subgrade.note.isNotBlank()) {
            Text(
                text = subgrade.note,
                style = MaterialTheme.typography.bodyMedium,
                color = PrismColors.InkFaint,
                modifier = Modifier.padding(top = 5.dp),
            )
        }
    }
}

/** Weak scores are the ones worth noticing, so the bar carries that meaning. */
private fun meterColor(score: Int): Color = when {
    score <= 4 -> PrismColors.Bad
    score <= 6 -> PrismColors.Warn
    else -> PrismColors.Scan
}

@Composable
fun AuthenticityRow(authenticity: Authenticity, modifier: Modifier = Modifier) {
    val dotColor =
        if (authenticity.flag == AuthenticityFlag.NO_CONCERNS) PrismColors.Good else PrismColors.Warn

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(dotColor),
        )
        Column {
            Text(
                text = authenticity.flag.display,
                style = MaterialTheme.typography.labelMedium,
                color = PrismColors.Ink,
            )
            if (authenticity.note.isNotBlank()) {
                Text(
                    text = authenticity.note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = PrismColors.InkDim,
                )
            }
        }
    }
}

/** A finding, marked with a dot rather than a bullet glyph. */
@Composable
fun FindingRow(text: String, modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .padding(top = 7.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(PrismColors.Scan),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = PrismColors.InkDim,
        )
    }
}

/** Small print — disclaimers and the "not a certified grade" line. */
@Composable
fun FinePrint(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontFamily = FontFamily.SansSerif,
        color = PrismColors.InkFaint,
        modifier = modifier.fillMaxWidth(),
    )
}
