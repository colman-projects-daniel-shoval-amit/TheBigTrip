package com.dsa.thebigtrip.utils

import android.graphics.Bitmap
import android.widget.ImageView
import com.google.firebase.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.storage
import com.squareup.picasso.Picasso
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream

object ImageUtil {

    private val storage = Firebase.storage

    /**
     * Uploads a Bitmap to Firebase Storage under the given path.
     * Returns the download URL string.
     *
     * Example path: "images/users/{uid}/profile.jpg"
     */
    suspend fun uploadImage(image: Bitmap, path: String): String? {
        return try {
            val ref = storage.reference.child(path)
            val baos = ByteArrayOutputStream()
            image.compress(Bitmap.CompressFormat.JPEG, 80, baos)
            val data = baos.toByteArray()

            ref.putBytes(data).await()
            val uri = ref.downloadUrl.await()
            uri.toString()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Uploads a user's profile image to Firebase Storage.
     * Path: images/users/{uid}/profile.jpg
     */
    suspend fun uploadUserProfileImage(image: Bitmap, uid: String): String? {
        return uploadImage(image, "images/users/$uid/profile.jpg")
    }

    /**
     * Loads an image from a URL into an ImageView using Picasso.
     */
    fun loadImage(imageView: ImageView, url: String?, placeholder: Int? = null) {
        if (url.isNullOrEmpty()) {
            if (placeholder != null) {
                imageView.setImageResource(placeholder)
            }
            return
        }

        val request = Picasso.get().load(url)

        if (placeholder != null) {
            request.placeholder(placeholder)
            request.error(placeholder)
        }

        request.into(imageView)
    }

    /**
     * Loads an image with a circular crop.
     */
    fun loadCircleImage(imageView: ImageView, url: String?, placeholder: Int? = null) {
        if (url.isNullOrEmpty()) {
            if (placeholder != null) {
                imageView.setImageResource(placeholder)
            }
            return
        }

        val request = Picasso.get()
            .load(url)
            .transform(CircleTransform())

        if (placeholder != null) {
            request.placeholder(placeholder)
            request.error(placeholder)
        }

        request.into(imageView)
    }
}