package com.example.ecodexandroid

import com.google.gson.annotations.SerializedName

data class Species(
    val id: Int,

    @SerializedName("common_name")
    val commonName: String?,

    @SerializedName("scientific_name")
    val scientificName: String?,

    @SerializedName("image_url")
    val imageUrl: String?,

    val family: String?,
    val genus: String?,
    val year: Int?,
    val author: String?,
    val status: String?
)

data class Links(
    val next: String?
)

data class SpeciesListResponse(
    val data: List<Species>,
    val links: Links?
)
