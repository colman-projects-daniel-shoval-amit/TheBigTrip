package com.dsa.thebigtrip.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.android.gms.maps.model.LatLng

@Entity
data class Post(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val location: LatLng,
    val imageUri: String?,
) {
    companion object {
        const val ID_KEY = "id"
        const val TITLE_KEY = "title"
        const val DESCRIPTION_KEY = "description"
        const val LOCATION_KEY = "location"
        const val IMAGE_URI_KEY = "imageUri"

        fun fromJson(json: Map<String, Any?>): Post {
            val id = json[ID_KEY] as String
            val title = json[TITLE_KEY] as String
            val description = json[DESCRIPTION_KEY] as String
            val locationStr = json[LOCATION_KEY] as String
            val parts = locationStr.split(",")
            val location = LatLng(parts[0].toDouble(), parts[1].toDouble())
            val imageUri = json[IMAGE_URI_KEY] as? String
            return Post(id = id, title = title, description = description, location = location, imageUri = imageUri)
        }
    }

    val toJson: Map<String, Any?>
        get() = hashMapOf(
            ID_KEY to id,
            TITLE_KEY to title,
            DESCRIPTION_KEY to description,
            LOCATION_KEY to "${location.latitude},${location.longitude}",
            IMAGE_URI_KEY to imageUri,
        )
}
