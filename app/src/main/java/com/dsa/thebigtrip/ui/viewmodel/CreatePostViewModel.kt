package com.dsa.thebigtrip.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.dsa.thebigtrip.data.WeatherUtil
import com.dsa.thebigtrip.data.post.Post
import com.dsa.thebigtrip.data.post.PostRepository
import com.dsa.thebigtrip.data.user.User
import com.dsa.thebigtrip.data.user.UserRepository
import com.dsa.thebigtrip.utils.ImageUtil
import kotlinx.coroutines.launch

class CreatePostViewModel(application: Application) : AndroidViewModel(application) {

    private val postRepository = PostRepository.shared
    private val userRepository = UserRepository.shared

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _postForEdit = MutableLiveData<Post?>()
    val postForEdit: LiveData<Post?> = _postForEdit

    private val _allUsers = MutableLiveData<List<User>>(emptyList())
    val allUsers: LiveData<List<User>> = _allUsers

    fun loadAllUsers() {
        viewModelScope.launch {
            _allUsers.value = try {
                userRepository.getAllUsers()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    fun loadPostForEdit(postId: String) {
        _isLoading.value = true

        viewModelScope.launch {
            try {
                _postForEdit.value = postRepository.getPostById(postId)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun publishPost(
        uid: String,
        title: String,
        description: String,
        imageUri: Uri?,
        locationName: String,
        latitude: Double,
        longitude: Double,
        visibleTo: List<String>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val postId = System.currentTimeMillis().toString()
                val imageUrl = imageUri?.let {
                    ImageUtil.uploadPostImage(getApplication(), it, postId)
                        ?: throw IllegalStateException("Failed to upload image")
                }

                val weather = WeatherUtil.fetchWeather(latitude, longitude)

                val post = Post(
                    id = postId,
                    userId = uid,
                    caption = title,
                    description = description.ifBlank { null },
                    imageUrl = imageUrl,
                    createdAt = System.currentTimeMillis(),
                    locationName = locationName,
                    latitude = latitude,
                    longitude = longitude,
                    weather = weather,
                    visibleTo = visibleTo.filter { it != uid }
                )

                postRepository.addPost(post)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Error")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updatePost(
        existingPost: Post,
        uid: String,
        title: String,
        description: String,
        imageUri: Uri?,
        locationName: String,
        latitude: Double,
        longitude: Double,
        visibleTo: List<String>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        _isLoading.value = true

        viewModelScope.launch {
            try {
                if (existingPost.userId != uid) {
                    throw IllegalStateException("You can edit only your own posts")
                }

                val imageUrl = imageUri?.let {
                    ImageUtil.uploadPostImage(getApplication(), it, existingPost.id)
                        ?: throw IllegalStateException("Failed to upload image")
                } ?: existingPost.imageUrl

                val weather = if (
                    existingPost.weather != null &&
                    existingPost.latitude == latitude &&
                    existingPost.longitude == longitude
                ) {
                    existingPost.weather
                } else {
                    WeatherUtil.fetchWeather(latitude, longitude)
                }

                val updatedPost = existingPost.copy(
                    caption = title,
                    description = description.ifBlank { null },
                    imageUrl = imageUrl,
                    locationName = locationName,
                    latitude = latitude,
                    longitude = longitude,
                    weather = weather,
                    visibleTo = visibleTo.filter { it != existingPost.userId }
                )

                postRepository.updatePost(updatedPost)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Error")
            } finally {
                _isLoading.value = false
            }
        }
    }
}
