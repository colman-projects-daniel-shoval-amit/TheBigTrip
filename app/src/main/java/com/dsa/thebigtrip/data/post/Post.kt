package com.dsa.thebigtrip.data.post

import androidx.room.Entity
import androidx.room.PrimaryKey

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

        fun fromJson(json: Map<String, Any?>): Post {

            val id = json[ID_KEY] as String
            val userId = json[USER_ID_KEY] as String
            val caption = json[CAPTION_KEY] as? String
            val imageUrl = json[IMAGE_URL_KEY] as? String
            val createdAt = json[CREATED_AT_KEY] as Long

            val locationName = json[LOCATION_NAME_KEY] as? String
            val latitude = json[LATITUDE_KEY] as? Double
            val longitude = json[LONGITUDE_KEY] as? Double

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