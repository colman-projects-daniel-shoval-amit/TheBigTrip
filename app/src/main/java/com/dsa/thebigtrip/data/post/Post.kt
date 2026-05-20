package com.dsa.thebigtrip.data.post

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.Timestamp

@Entity
data class Post(

    @PrimaryKey
    val id: String,

    val userId: String,
    val caption: String?,
    val imageUrl: String?,
    val createdAt: Long,

    val locationName: String?,
    val latitude: Double?,
    val longitude: Double?

) {

    constructor() : this("", "", null, null, 0, null, null, null)

    companion object {

        const val ID_KEY = "id"
        const val USER_ID_KEY = "userId"
        const val CAPTION_KEY = "caption"
        const val IMAGE_URL_KEY = "imageUrl"
        const val CREATED_AT_KEY = "createdAt"

        const val LOCATION_NAME_KEY = "locationName"
        const val LATITUDE_KEY = "latitude"
        const val LONGITUDE_KEY = "longitude"

        fun fromJson(json: Map<String, Any?>): Post? {

            val id = json[ID_KEY] as? String ?: return null
            val userId = json[USER_ID_KEY] as? String ?: return null
            val caption = json[CAPTION_KEY] as? String
            val imageUrl = json[IMAGE_URL_KEY] as? String
            val createdAt = parseLong(json[CREATED_AT_KEY])

            val locationName = json[LOCATION_NAME_KEY] as? String
            val latitude = parseDouble(json[LATITUDE_KEY])
            val longitude = parseDouble(json[LONGITUDE_KEY])

            return Post(
                id = id,
                userId = userId,
                caption = caption,
                imageUrl = imageUrl,
                createdAt = createdAt,
                locationName = locationName,
                latitude = latitude,
                longitude = longitude
            )
        }

        private fun parseLong(value: Any?): Long {
            return when (value) {
                is Long -> value
                is Int -> value.toLong()
                is Double -> value.toLong()
                is Float -> value.toLong()
                is Timestamp -> value.toDate().time
                else -> 0L
            }
        }

        private fun parseDouble(value: Any?): Double? {
            return when (value) {
                is Double -> value
                is Float -> value.toDouble()
                is Long -> value.toDouble()
                is Int -> value.toDouble()
                is String -> value.toDoubleOrNull()
                else -> null
            }
        }
    }

    val toJson: Map<String, Any?>
        get() = hashMapOf(
            ID_KEY to id,
            USER_ID_KEY to userId,
            CAPTION_KEY to caption,
            IMAGE_URL_KEY to imageUrl,
            CREATED_AT_KEY to createdAt,
            LOCATION_NAME_KEY to locationName,
            LATITUDE_KEY to latitude,
            LONGITUDE_KEY to longitude
        )
}
