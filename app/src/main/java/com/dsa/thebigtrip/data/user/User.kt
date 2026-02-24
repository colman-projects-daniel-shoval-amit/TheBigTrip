package com.dsa.thebigtrip.data.user

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue

@Entity
data class User(
    @PrimaryKey
    val uid: String,
    val fullName: String?,
    val email: String?,
    var imageUri: String?,
) {

    constructor() : this("", null, null, null)

    companion object {

        const val UID_KEY = "uid"
        const val FULL_NAME_KEY = "fullName"
        const val EMAIL_KEY = "email"
        const val IMAGE_URI_KEY = "imageUri"

        fun fromJson(json: Map<String, Any?>): User {
            val uid = json[UID_KEY] as String
            val fullName = json[FULL_NAME_KEY] as? String
            val email = json[EMAIL_KEY] as? String
            val imageUri = json[IMAGE_URI_KEY] as? String

            return User(
                uid = uid,
                fullName = fullName,
                email = email,
                imageUri = imageUri,
            )
        }
    }

    val toJson: Map<String, Any?>
        get() = hashMapOf(
            UID_KEY to uid,
            FULL_NAME_KEY to fullName,
            EMAIL_KEY to email,
            IMAGE_URI_KEY to imageUri,
        )
}