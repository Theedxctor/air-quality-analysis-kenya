package com.myproject.kenyaair.data

import android.util.Log
import com.myproject.kenyaair.data.net.*

private const val TAG = "OpenAQ"

class AirRepository(private val api: OpenAQApi) {

    suspend fun kenyaStations(page: Int = 1): List<Location> {
        val resp = api.getLocations(page = page)
        Log.d(TAG, "Locations meta=${resp.meta} count=${resp.results.size}")
        return resp.results
    }

    /**
     * Returns a flat list of LatestResult with parameter code/unit resolved.
     * Order of attempts:
     * 1) /v3/locations/{id}/latest parsed as container (measurements/parameters).
     * 2) If empty, /v3/locations/{id}/latest as raw + /v3/locations/{id}/sensors.
     *    Use /v3/parameters to map parametersId to code/name/units if needed.
     */
    suspend fun latestFor(locationId: Int): List<LatestResult> {
        // 1) Try the container shape
        val r1 = api.getLatestForLocationPath(locationId)
        var flat: List<LatestResult> =
            r1.results.flatMap { (it.measurements ?: it.parameters).orEmpty() }

        if (flat.isNotEmpty()) {
            Log.d(TAG, "Latest(container) id=$locationId -> count=${flat.size}")
            debugPreview(flat)
            return flat
        }

        // 2) Fallback: raw + sensors + (maybe) parameters catalog
        val raw = api.getLatestForLocationRaw(locationId).results
        val sensors = api.getSensorsForLocation(locationId).results
        val paramsCatalog = api.getParameters().results.associateBy { it.id } // id -> ParameterDef

        // sensorsId -> ParamInfo(code, pretty, unit)
        data class ParamInfo(val code: String?, val pretty: String?, val unit: String?)
        val sensorMap: Map<Int?, ParamInfo> = sensors.associateBy({ it.id }) { s ->
            val pid = s.parametersId
            val cat = paramsCatalog[pid]
            val code = s.parameter?.code ?: cat?.code
            val pretty = s.parameter?.name ?: cat?.name ?: code?.uppercase()
            val unit = s.parameter?.units ?: s.unit ?: cat?.units
            ParamInfo(code, pretty, unit)
        }

        flat = raw.map { item ->
            val info = sensorMap[item.sensorsId]
            LatestResult(
                parameter = info?.code,      // e.g., "pm25"
                unit = info?.unit,           // e.g., "µg/m³"
                value = item.value,
                quality = null,
                displayName = info?.pretty,  // readable name if you want it
                entity = null,
                sensorType = null,
                datetime = DateTime(
                    utc = item.datetime?.utc ?: item.date2?.utc,
                    local = item.datetime?.local ?: item.date2?.local
                ),
                date = null,
                lastUpdated = null
            )
        }

        Log.d(TAG, "Latest(raw+sensors+catalog) id=$locationId -> count=${flat.size}")
        debugPreview(flat)
        return flat
    }

    private fun debugPreview(list: List<LatestResult>) {
        list.take(5).forEachIndexed { i, m ->
            Log.d(
                TAG,
                "Meas[$i]: param=${m.parameter} value=${m.value} unit=${m.unit} " +
                        "t=${m.datetime?.local ?: m.datetime?.utc ?: m.date?.local ?: m.date?.utc ?: m.lastUpdated}"
            )
        }
    }
}
