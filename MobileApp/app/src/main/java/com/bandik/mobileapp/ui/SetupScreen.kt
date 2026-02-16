package com.bandik.mobileapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Text("BLE Logger — Setup", style = MaterialTheme.typography.titleLarge)

        // -------- BASIC --------

        OutlinedTextField(
            value = state.experimentId,
            onValueChange = { state = state.copy(experimentId = it) },
            label = { Text("Experiment ID") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = state.pointId,
            onValueChange = { state = state.copy(pointId = it) },
            label = { Text("Point ID") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // -------- POINT COORDINATES --------

        Text("Point coordinates (meters)", style = MaterialTheme.typography.labelLarge)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

            OutlinedTextField(
                value = state.pointXText,
                onValueChange = { state = state.copy(pointXText = it) },
                label = { Text("X") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )

            OutlinedTextField(
                value = state.pointYText,
                onValueChange = { state = state.copy(pointYText = it) },
                label = { Text("Y") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )

            OutlinedTextField(
                value = state.pointZText,
                onValueChange = { state = state.copy(pointZText = it) },
                label = { Text("Z") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
        }

        // -------- ROOM SIZE --------

        Text("Room dimensions (meters)", style = MaterialTheme.typography.labelLarge)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

            OutlinedTextField(
                value = state.roomWidthText,
                onValueChange = { state = state.copy(roomWidthText = it) },
                label = { Text("Width") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )

            OutlinedTextField(
                value = state.roomLengthText,
                onValueChange = { state = state.copy(roomLengthText = it) },
                label = { Text("Length") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )

            OutlinedTextField(
                value = state.roomHeightText,
                onValueChange = { state = state.copy(roomHeightText = it) },
                label = { Text("Height") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
        }

        // -------- RECORDING --------

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
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Sessions / Export")
        }

        if (!isValid) {
            Text(
                "Fill required fields correctly",
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

    val pointXText: String = "0.0",
    val pointYText: String = "0.0",
    val pointZText: String = "1.0",

    val roomWidthText: String = "2.8",
    val roomLengthText: String = "4.6",
    val roomHeightText: String = "2.6",

    val durationSecText: String = "120",
    val heightMText: String = "1.5",
    val phonePose: String = "hand",
    val notes: String = ""
) {

    fun isValid(): Boolean {
        val dur = durationSecText.toIntOrNull() ?: return false
        val px = pointXText.toDoubleOrNull() ?: return false
        val py = pointYText.toDoubleOrNull() ?: return false
        val pz = pointZText.toDoubleOrNull() ?: return false

        val rw = roomWidthText.toDoubleOrNull() ?: return false
        val rl = roomLengthText.toDoubleOrNull() ?: return false
        val rh = roomHeightText.toDoubleOrNull() ?: return false

        return experimentId.isNotBlank() &&
                pointId.isNotBlank() &&
                dur > 0 &&
                rw > 0 && rl > 0 && rh > 0
    }

    fun normalized(): SetupState {
        return copy(
            experimentId = experimentId.trim(),
            pointId = pointId.trim(),
            notes = notes.trim()
        )
    }
}
