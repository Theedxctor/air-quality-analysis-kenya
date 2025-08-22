package com.myproject.kenyaair.data.net

import com.squareup.moshi.Json
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/* ---------- Common ---------- */
data class Meta(val page: Int? = null, val limit: Int? = null, val found: Int? = null)

data class Coordinates(val latitude: Double? = null, val longitude: Double? = null)

data class Country(val code: String? = null, val name: String? = null)

/* ---------- Locations list ---------- */
data class Location(
    val id: Int,
    val name: String? = null,
    val locality: String? = null,
    val country: Country? = null,
    val coordinates: Coordinates? = null
)
data class LocationsResponse(val meta: Meta? = null, val results: List<Location> = emptyList())

/* ---------- Latest for a location ---------- */

data class DateTime(val utc: String? = null, val local: String? = null)

/** One reading */
data class LatestResult(
    val parameter: String? = null,
    val unit: String? = null,
    val value: Double? = null,
    val quality: String? = null,
    val displayName: String? = null,
    val entity: String? = null,
    val sensorType: String? = null,

    // time variants
    val datetime: DateTime? = null,
    val date: DateTime? = null,
    @Json(name = "lastUpdated") val lastUpdated: String? = null
)

/** Container returned by /v3/latest or /v3/locations/{id}/latest */
data class LatestContainer(
    val id: Int? = null,
    val name: String? = null,
    val country: Country? = null,
    val coordinates: Coordinates? = null,

    // Some responses use "measurements", others "parameters"
    @Json(name = "measurements") val measurements: List<LatestResult>? = null,
    @Json(name = "parameters")   val parameters:   List<LatestResult>? = null
)
data class LatestResponse(val meta: Meta? = null, val results: List<LatestContainer> = emptyList())

/* ---------- Sensors for a location ---------- */
data class SensorsResponse(
    val meta: Meta? = null,
    val results: List<Sensor> = emptyList()
)

data class Sensor(
    val id: Int? = null,                 // sensorsId (this is what latest references)
    val parameter: ParameterRef? = null, // may include code/name/units
    val unit: String? = null,            // sometimes present if not in parameter
    val parametersId: Int? = null        // numeric param id (fallback)
)

data class ParameterRef(
    val id: Int? = null,
    val code: String? = null,            // e.g., "pm25"
    val name: String? = null,            // e.g., "PM2.5"
    val units: String? = null            // e.g., "µg/m³"
)

/* ---------- Fallback “raw” latest shape (value + sensorsId) ---------- */
data class DateTimePair(val utc: String? = null, val local: String? = null)

data class LatestRawItem(
    val value: Double? = null,
    val datetime: DateTimePair? = null,
    @Json(name = "date") val date2: DateTimePair? = null,
    @Json(name = "sensorsId") val sensorsId: Int? = null,
    @Json(name = "locationsId") val locationsId: Int? = null
)

data class LatestRawResponse(
    val meta: Meta? = null,
    val results: List<LatestRawItem> = emptyList()
)

/* ---------- Global parameters catalog ---------- */
data class ParameterDef(
    val id: Int? = null,
    val code: String? = null,     // e.g., "pm25"
    val name: String? = null,     // e.g., "PM2.5"
    val units: String? = null     // e.g., "µg/m³"
)

data class ParametersResponse(
    val meta: Meta? = null,
    val results: List<ParameterDef> = emptyList()
)

interface OpenAQApi {
    @GET("v3/locations")
    suspend fun getLocations(
        @Query("iso") iso: String = "KE",
        @Query("limit") limit: Int = 100,
        @Query("page") page: Int = 1
    ): LocationsResponse

    // Path style (some servers support this)
    @GET("v3/locations/{id}/latest")
    suspend fun getLatestForLocationPath(
        @Path("id") id: Int,
        @Query("limit") limit: Int = 100
    ): LatestResponse

    // Query style (works everywhere)
    @GET("v3/latest")
    suspend fun getLatestForLocationQuery(
        @Query("location_id") id: Int,
        @Query("limit") limit: Int = 100
    ): LatestResponse

    // Get sensors for a location (maps sensorsId -> parameter/unit)
    @GET("v3/locations/{id}/sensors")
    suspend fun getSensorsForLocation(
        @Path("id") id: Int
    ): SensorsResponse

    // Same path as getLatestForLocationPath, but parsed as the "raw" shape
    @GET("v3/locations/{id}/latest")
    suspend fun getLatestForLocationRaw(
        @Path("id") id: Int,
        @Query("limit") limit: Int = 100
    ): LatestRawResponse

    @GET("v3/parameters")
    suspend fun getParameters(): ParametersResponse

}

