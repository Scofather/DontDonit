package com.prismgrade.ui.capture

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.prismgrade.data.image.PreparedImage
import com.prismgrade.ui.components.FinePrint
import com.prismgrade.ui.components.SectionLabel
import com.prismgrade.ui.theme.PrismColors

/**
 * The inspection bay: two photo slots and the run button, or the description
 * form when the user would rather type than shoot.
 */
@Composable
fun CaptureScreen(
    state: CaptureUiState,
    onPickImage: (CardSide, Uri) -> Unit,
    onClearImage: (CardSide) -> Unit,
    onModeChange: (InspectionMode) -> Unit,
    onCardDescriptionChange: (String) -> Unit,
    onConditionDescriptionChange: (String) -> Unit,
    onRunInspection: () -> Unit,
    onOpenSettings: () -> Unit,
    newCaptureFile: () -> java.io.File,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var pendingCaptureUri by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingSide by rememberSaveable { mutableStateOf(CardSide.FRONT.name) }

    val takePicture = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { success ->
        val uri = pendingCaptureUri
        if (success && uri != null) {
            onPickImage(CardSide.valueOf(pendingSide), Uri.parse(uri))
        }
        pendingCaptureUri = null
    }

    val pickFromGallery = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) onPickImage(CardSide.valueOf(pendingSide), uri)
    }

    fun launchCamera(side: CardSide) {
        pendingSide = side.name
        val file = newCaptureFile()
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        pendingCaptureUri = uri.toString()
        takePicture.launch(uri)
    }

    fun launchGallery(side: CardSide) {
        pendingSide = side.name
        pickFromGallery.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Every card gets a look under three lights before it gets a number.",
                style = MaterialTheme.typography.headlineMedium,
                color = PrismColors.Ink,
            )
            Text(
                text = "Photograph the front and, ideally, the back. Claude reads centering, " +
                    "corners, edges and surface the way a PSA or BGS grader would, then commits " +
                    "to an estimate.",
                style = MaterialTheme.typography.bodyMedium,
                color = PrismColors.InkDim,
            )
        }

        if (!state.hasApiKey) {
            ApiKeyNotice(onOpenSettings)
        }

        ModeSwitch(mode = state.mode, onModeChange = onModeChange)

        when (state.mode) {
            InspectionMode.PHOTOS -> PhotoBay(
                state = state,
                onCapture = ::launchCamera,
                onPickFromGallery = ::launchGallery,
                onClearImage = onClearImage,
            )

            InspectionMode.DESCRIPTION -> DescriptionForm(
                state = state,
                onCardDescriptionChange = onCardDescriptionChange,
                onConditionDescriptionChange = onConditionDescriptionChange,
            )
        }

        state.errorMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = PrismColors.Bad,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(PrismColors.PanelRaised)
                    .border(1.dp, PrismColors.Bad, RoundedCornerShape(4.dp))
                    .padding(14.dp),
            )
        }

        RunButton(state = state, onRunInspection = onRunInspection)

        FinePrint(
            "Prism Grade is an AI estimate, not a certified grade from PSA, BGS, CGC or SGC. " +
                "Lighting and camera quality shift the reading — for cards worth submitting, get " +
                "a physical opinion first.",
        )
    }
}

@Composable
private fun ApiKeyNotice(onOpenSettings: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(PrismColors.PanelRaised)
            .border(1.dp, PrismColors.Warn, RoundedCornerShape(4.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SectionLabel("Setup needed", color = PrismColors.Warn)
        Text(
            text = "Add your Anthropic API key before running an inspection.",
            style = MaterialTheme.typography.bodyMedium,
            color = PrismColors.InkDim,
        )
        TextButton(onClick = onOpenSettings) { Text("Open settings") }
    }
}

@Composable
private fun ModeSwitch(mode: InspectionMode, onModeChange: (InspectionMode) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ModeTab("Photos", mode == InspectionMode.PHOTOS) {
            onModeChange(InspectionMode.PHOTOS)
        }
        ModeTab("Describe", mode == InspectionMode.DESCRIPTION) {
            onModeChange(InspectionMode.DESCRIPTION)
        }
    }
}

@Composable
private fun ModeTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = if (selected) PrismColors.Scan else PrismColors.InkFaint,
        modifier = Modifier
            .clip(RoundedCornerShape(2.dp))
            .border(
                1.dp,
                if (selected) PrismColors.ScanDim else PrismColors.Line,
                RoundedCornerShape(2.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

@Composable
private fun PhotoBay(
    state: CaptureUiState,
    onCapture: (CardSide) -> Unit,
    onPickFromGallery: (CardSide) -> Unit,
    onClearImage: (CardSide) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PhotoSlot(
            title = "Front",
            requirement = "required",
            image = state.front,
            error = state.frontError,
            onCapture = { onCapture(CardSide.FRONT) },
            onPickFromGallery = { onPickFromGallery(CardSide.FRONT) },
            onClear = { onClearImage(CardSide.FRONT) },
        )
        PhotoSlot(
            title = "Back",
            requirement = "optional — improves accuracy",
            image = state.back,
            error = state.backError,
            onCapture = { onCapture(CardSide.BACK) },
            onPickFromGallery = { onPickFromGallery(CardSide.BACK) },
            onClear = { onClearImage(CardSide.BACK) },
        )
    }
}

@Composable
private fun PhotoSlot(
    title: String,
    requirement: String,
    image: PreparedImage?,
    error: String?,
    onCapture: () -> Unit,
    onPickFromGallery: () -> Unit,
    onClear: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionLabel("$title · $requirement", color = PrismColors.InkDim)
            if (image != null) {
                IconButton(onClick = onClear, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Remove $title photo",
                        tint = PrismColors.InkDim,
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.6f)
                .clip(RoundedCornerShape(4.dp))
                .background(PrismColors.Panel)
                .border(1.dp, PrismColors.Line, RoundedCornerShape(4.dp))
                .clickable(onClick = onCapture),
            contentAlignment = Alignment.Center,
        ) {
            if (image != null) {
                AsyncImage(
                    model = image.localFile,
                    contentDescription = "$title of the card",
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = PrismColors.InkFaint,
                    )
                    Text(
                        text = "Tap to photograph",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PrismColors.InkDim,
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = onCapture) { Text("Camera") }
            TextButton(onClick = onPickFromGallery) { Text("Choose photo") }
        }

        error?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = PrismColors.Bad)
        }
    }
}

@Composable
private fun DescriptionForm(
    state: CaptureUiState,
    onCardDescriptionChange: (String) -> Unit,
    onConditionDescriptionChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "No photos needed. The grade can only reflect what you describe, so mention " +
                "centering, corners, edges and surface if you can.",
            style = MaterialTheme.typography.bodyMedium,
            color = PrismColors.InkDim,
        )
        OutlinedTextField(
            value = state.cardDescription,
            onValueChange = onCardDescriptionChange,
            label = { Text("What is the card?") },
            placeholder = { Text("1999 Pokémon Base Set Charizard 4/102, holo") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
        )
        OutlinedTextField(
            value = state.conditionDescription,
            onValueChange = onConditionDescriptionChange,
            label = { Text("What does it look like?") },
            placeholder = {
                Text("Sharp corners except slight whitening bottom-right. One faint holo scratch.")
            },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
        )
    }
}

@Composable
private fun RunButton(state: CaptureUiState, onRunInspection: () -> Unit) {
    val enabled = when (state.mode) {
        InspectionMode.PHOTOS -> state.canRunPhotoInspection
        InspectionMode.DESCRIPTION -> state.canRunDescriptionInspection
    }

    Button(
        onClick = onRunInspection,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = PrismColors.Scan,
            contentColor = PrismColors.Void,
            disabledContainerColor = PrismColors.PanelRaised,
            disabledContentColor = PrismColors.InkFaint,
        ),
        shape = RoundedCornerShape(4.dp),
    ) {
        if (state.isInspecting) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = PrismColors.Void,
                )
                Text("Inspecting…", fontWeight = FontWeight.SemiBold)
            }
        } else {
            Text(
                text = if (state.mode == InspectionMode.PHOTOS) "Run inspection"
                else "Estimate from description",
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
