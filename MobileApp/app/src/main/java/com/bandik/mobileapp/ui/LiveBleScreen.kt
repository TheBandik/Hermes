package com.bandik.mobileapp.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun LiveBleScreen(vm: LiveBleViewModel = viewModel()) {

    val devices by vm.devices.collectAsState()

    LaunchedEffect(Unit) {
        vm.start()
    }

    LazyColumn {
        items(devices) { d ->
            Column(Modifier.padding(12.dp)) {
                Text(d.name ?: "Unknown")
                Text(d.address)
                Text("RSSI: ${d.rssi}")
            }
        }
    }
}
