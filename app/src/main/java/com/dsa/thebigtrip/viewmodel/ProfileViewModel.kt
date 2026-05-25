package com.dsa.thebigtrip.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsa.thebigtrip.data.user.User
import com.dsa.thebigtrip.data.user.UserRepository
import com.dsa.thebigtrip.utils.ImageUtil
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {

    private val userRepository = UserRepository.shared

    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> = _user

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

    fun loadUser(uid: String) {
        viewModelScope.launch {
            _user.value = userRepository.getUserById(uid)
        }
    }

    fun updateProfile(uid: String, name: String, email: String?, currentImageUrl: String?, selectedImage: Bitmap?) {
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val imageUrl = selectedImage?.let {
                    ImageUtil.uploadUserProfileImage(it, uid)
                } ?: currentImageUrl

                val updatedUser = User(
                    uid = uid,
                    fullName = name,
                    email = email,
                    imageUri = imageUrl
                )

                userRepository.updateUser(updatedUser)
                _user.value = updatedUser
                _message.value = "Profile updated!"
            } catch (e: Exception) {
                _message.value = "Failed to save: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
