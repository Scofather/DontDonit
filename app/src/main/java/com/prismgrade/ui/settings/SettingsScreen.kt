package com.prismgrade.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.prismgrade.ui.components.FinePrint
import com.prismgrade.ui.components.SectionLabel
import com.prismgrade.ui.theme.PrismColors

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onApiKeyChange: (String) -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var revealed by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        SectionLabel("Anthropic API key", color = PrismColors.Scan)

        Text(
            text = "Inspections run on Claude, billed to your own Anthropic account. Paste a key " +
                "from console.anthropic.com to get started.",
            style = MaterialTheme.typography.bodyMedium,
            color = PrismColors.InkDim,
        )

        OutlinedTextField(
            value = state.apiKeyDraft,
            onValueChange = onApiKeyChange,
            label = { Text("sk-ant-…") },
            singleLine = true,
            visualTransformation = if (revealed) VisualTransformation.None
            else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
        )

        TextButton(onClick = { revealed = !revealed }) {
            Text(if (revealed) "Hide key" else "Show key")
        }

        Button(
            onClick = onSave,
            enabled = state.apiKeyDraft.isNotBlank() && !state.isSaving,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(4.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrismColors.Scan,
                contentColor = PrismColors.Void,
                disabledContainerColor = PrismColors.PanelRaised,
                disabledContentColor = PrismColors.InkFaint,
            ),
        ) {
            Text(if (state.isSaved) "Saved" else "Save key")
        }

        if (state.hasStoredKey) {
            TextButton(onClick = onClear) { Text("Remove stored key") }
        }

        HorizontalDivider(color = PrismColors.Line)

        SectionLabel("Where the key lives")
        FinePrint(
            "The key is kept in this app's private storage and excluded from device backups. " +
                "It never leaves the device except in requests to Anthropic's API. Anyone with " +
                "access to an unlocked, rooted device could still read it — for a published app, " +
                "route requests through a server you control instead of shipping a key per install.",
        )
    }
}
