package com.dsa.thebigtrip.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import android.widget.ImageView
import com.dsa.thebigtrip.base.TheBigTrip
import com.google.firebase.Firebase
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.storage
import com.squareup.picasso.Picasso
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream

object ImageUtil {

    private val storage = Firebase.storage
    private const val TAG = "ImageUtil"
    private const val MAX_IMAGE_SIZE = 1024

    /**
     * Uploads a Bitmap to Firebase Storage under the given path.
     * Returns the download URL string.
     */
    suspend fun uploadImage(image: Bitmap, path: String): String? {
        return try {
            val resizedBitmap = scaleBitmap(image, MAX_IMAGE_SIZE)
            val baos = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
            val data = baos.toByteArray()

            val metadata = StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .build()

            val ref = storage.reference.child(path)
            ref.putBytes(data, metadata).await()
            val uri = ref.downloadUrl.await()
            uri.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Upload failure for $path", e)
            null
        }
    }

    /**
     * Uploads an image from a Uri to Firebase Storage.
     */
    suspend fun uploadImage(uri: Uri, path: String): String? {
        val context = TheBigTrip.appContext ?: return null
        val bitmap = uriToBitmap(context, uri) ?: return null
        return uploadImage(bitmap, path)
    }

    suspend fun uploadUserProfileImage(uri: Uri, uid: String): String? {
        return uploadImage(uri, "images/users/$uid/profile.jpg")
    }

    private fun scaleBitmap(source: Bitmap, maxSize: Int): Bitmap {
        val width = source.width
        val height = source.height

        if (width <= maxSize && height <= maxSize) return source

        val scale = maxSize.toFloat() / maxOf(width, height)
        val matrix = Matrix()
        matrix.postScale(scale, scale)

        return Bitmap.createBitmap(source, 0, 0, width, height, matrix, true)
    }

    fun uriToBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode Uri to Bitmap", e)
            null
        }
    }

    fun loadImage(imageView: ImageView, url: String?, placeholder: Int? = null) {
        if (url.isNullOrEmpty()) {
            placeholder?.let { imageView.setImageResource(it) }
            return
        }
        val request = Picasso.get().load(url)
        placeholder?.let {
            request.placeholder(it).error(it)
        }
        request.into(imageView)
    }

    fun loadCircleImage(imageView: ImageView, url: String?, placeholder: Int? = null) {
        if (url.isNullOrEmpty()) {
            placeholder?.let { imageView.setImageResource(it) }
            return
        }
        val request = Picasso.get().load(url).transform(CircleTransform())
        placeholder?.let {
            request.placeholder(it).error(it)
        }
        request.into(imageView)
    }
}
