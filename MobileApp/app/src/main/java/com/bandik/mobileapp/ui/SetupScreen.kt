package com.bandik.mobileapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun SetupScreen(
    onStart: (SetupState) -> Unit,
    onOpenSessions: () -> Unit
) {
    var state by remember { mutableStateOf(SetupState()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("BLE Logger — Setup", style = MaterialTheme.typography.titleLarge)

        OutlinedTextField(
            value = state.experimentId,
            onValueChange = { state = state.copy(experimentId = it) },
            label = { Text("Experiment ID (S0 / S1 / …)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = state.pointId,
            onValueChange = { state = state.copy(pointId = it) },
            label = { Text("Point ID (P12 / 12 / A-3 ...)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = state.durationSecText,
            onValueChange = { state = state.copy(durationSecText = it.filter { ch -> ch.isDigit() }) },
            label = { Text("Duration (seconds)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = state.heightMText,
            onValueChange = { state = state.copy(heightMText = it) },
            label = { Text("Receiver height (meters)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        PoseSelector(
            value = state.phonePose,
            onChange = { state = state.copy(phonePose = it) }
        )

        OutlinedTextField(
            value = state.notes,
            onValueChange = { state = state.copy(notes = it) },
            label = { Text("Notes (optional)") },
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
        )

        val isValid = state.isValid()

        Button(
            onClick = { onStart(state.normalized()) },
            enabled = isValid,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("START")
        }

        OutlinedButton(
            onClick = onOpenSessions,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Sessions / Export")
        }

        if (!isValid) {
            Text(
                "Fill Experiment ID, Point ID, Duration (>0)",
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun PoseSelector(value: String, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Phone pose", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = value == "hand",
                onClick = { onChange("hand") },
                label = { Text("hand") }
            )
            FilterChip(
                selected = value == "tripod",
                onClick = { onChange("tripod") },
                label = { Text("tripod") }
            )
            FilterChip(
                selected = value == "pocket",
                onClick = { onChange("pocket") },
                label = { Text("pocket") }
            )
        }
    }
}

data class SetupState(
    val experimentId: String = "",
    val pointId: String = "",
    val durationSecText: String = "10",
    val heightMText: String = "1.0",
    val phonePose: String = "hand",
    val notes: String = ""
) {
    fun isValid(): Boolean {
        val dur = durationSecText.toIntOrNull() ?: return false
        return experimentId.isNotBlank() && pointId.isNotBlank() && dur > 0
    }

    fun normalized(): SetupState {
        return copy(
            experimentId = experimentId.trim(),
            pointId = pointId.trim(),
            durationSecText = durationSecText.trim(),
            heightMText = heightMText.trim(),
            notes = notes.trim()
        )
    }
}
