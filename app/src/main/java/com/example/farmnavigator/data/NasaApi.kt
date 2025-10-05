package com.example.farmnavigator.data

import retrofit2.http.GET
import retrofit2.http.Query


data class NasaDataResponse(
    val vegetationIndex: Double,
    val soilMoisture: Double,
    val precipitation: Double
)


interface NasaApiService {
    @GET("events")
    suspend fun getAgricultureData(
        @Query("region") region: String,
        @Query("season") season: String
    ): NasaDataResponse
}
