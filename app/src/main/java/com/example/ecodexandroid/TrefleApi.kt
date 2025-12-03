package com.example.ecodexandroid

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TrefleApi {

    // GET /species?page=1&token=...
    @GET("species")
    suspend fun listSpecies(
        @Query("page") page: Int
    ): SpeciesListResponse

    // GET /species/search?q=rosa&page=1&token=...
    @GET("species/search")
    suspend fun searchSpecies(
        @Query("q") query: String,
        @Query("page") page: Int
    ): SpeciesListResponse

    // GET /species/{id}?token=...
    @GET("species/{id}")
    suspend fun getSpecies(
        @Path("id") id: Int
    ): SpeciesDetailResponse
}

data class SpeciesDetailResponse(
    val data: Species
)
