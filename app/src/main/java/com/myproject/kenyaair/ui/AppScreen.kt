package com.myproject.kenyaair.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.myproject.kenyaair.data.net.LatestResult
import com.myproject.kenyaair.data.net.Location
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun AppScreen(vm: AirViewModel) {
    var showDetail by remember { mutableStateOf(false) }

    val uiState by vm.state.collectAsState()
    val stations by vm.stations.collectAsState()
    val latest by vm.latest.collectAsState()

    LaunchedEffect(Unit) { vm.loadStations() }

    when (val st = uiState) {
        UiState.Loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
        is UiState.Error -> Text("Error: ${st.message}", color = MaterialTheme.colorScheme.error)
        UiState.Idle -> {}
    }

    if (!showDetail) {
        StationList(
            stations = stations,
            onClick = { loc ->
                vm.openStation(loc)
                showDetail = true
            }
        )
    } else {
        val sel = vm.selected
        StationDetail(
            title = sel?.name ?: "Station ${sel?.id ?: ""}",
            locality = sel?.locality ?: (sel?.country?.name ?: ""),
            readings = latest,
            onBack = { showDetail = false }
        )
    }
}

@Composable
fun StationList(stations: List<Location>, onClick: (Location) -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Kenya Air Quality Stations", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        LazyColumn {
            items(stations) { s ->
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable { onClick(s) }
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(s.name ?: "Station ${s.id}", style = MaterialTheme.typography.titleMedium)
                        val sub = buildString {
                            s.locality?.let { append(it) }
                            if (!s.locality.isNullOrBlank()) append(" • ")
                            append(s.country?.name ?: s.country?.code ?: "KE")
                        }
                        Text(sub, style = MaterialTheme.typography.bodyMedium)
                        s.coordinates?.let { c ->
                            Text("(${c.latitude}, ${c.longitude})", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StationDetail(title: String?, locality: String, readings: List<LatestResult>, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title ?: "Station", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onBack) { Text("Back") }
        }
        Text(if (locality.isBlank()) "N/A" else locality, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(12.dp))

        if (readings.isEmpty()) {
            Text("No recent readings found.")
        } else {
            LazyColumn {
                items(readings) { r ->
                    val prettyName = pollutantPrettyName(r.parameter)
                    val valueText = formatValue(r.value, r.unit)
                    val timeText = formatIsoToLocal(
                        r.datetime?.local
                            ?: r.datetime?.utc
                            ?: r.date?.local
                            ?: r.date?.utc
                            ?: r.lastUpdated
                    )
                    val category = aqiCategory(r.parameter, r.value)

                    ElevatedCard(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            Text(prettyName, style = MaterialTheme.typography.titleMedium)
                            Text(valueText, style = MaterialTheme.typography.bodyLarge)
                            if (category != null) {
                                Text("Air quality: $category", style = MaterialTheme.typography.labelMedium)
                            }
                            Text("Time: $timeText", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

/* ---------- Helpers ---------- */

private fun pollutantPrettyName(code: String?): String = when (code?.lowercase()) {
    "pm25", "pm2_5" -> "PM2.5 (Fine Particles)"
    "pm10"          -> "PM10 (Coarse Particles)"
    "no2"           -> "Nitrogen Dioxide (NO₂)"
    "so2"           -> "Sulfur Dioxide (SO₂)"
    "o3"            -> "Ozone (O₃)"
    "co"            -> "Carbon Monoxide (CO)"
    "bc"            -> "Black Carbon (BC)"
    else            -> code?.uppercase() ?: "Unknown"
}

private fun formatValue(value: Double?, unit: String?): String {
    if (value == null) return "-"
    val u = unit?.ifBlank { null } ?: "µg/m³"
    val num = if (value % 1.0 == 0.0) value.toInt().toString() else String.format("%.1f", value)
    return "$num $u"
}

private fun formatIsoToLocal(iso: String?): String {
    if (iso.isNullOrBlank()) return "-"
    val outFmt = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
    return try {
        OffsetDateTime.parse(iso).atZoneSameInstant(ZoneId.systemDefault()).format(outFmt)
    } catch (_: Exception) {
        try {
            Instant.parse(iso).atZone(ZoneId.systemDefault()).format(outFmt)
        } catch (_: Exception) {
            iso
        }
    }
}

/** Simple EPA-style categories for PM2.5/PM10 using µg/m³ thresholds. */
private fun aqiCategory(parameter: String?, value: Double?): String? {
    val v = value ?: return null
    return when (parameter?.lowercase()) {
        "pm25", "pm2_5" -> when {
            v <= 12.0   -> "Good"
            v <= 35.4   -> "Moderate"
            v <= 55.4   -> "Unhealthy (Sensitive)"
            v <= 150.4  -> "Unhealthy"
            v <= 250.4  -> "Very Unhealthy"
            else        -> "Hazardous"
        }
        "pm10" -> when {
            v <= 54     -> "Good"
            v <= 154    -> "Moderate"
            v <= 254    -> "Unhealthy (Sensitive)"
            v <= 354    -> "Unhealthy"
            v <= 424    -> "Very Unhealthy"
            else        -> "Hazardous"
        }
        else -> null
    }
}
