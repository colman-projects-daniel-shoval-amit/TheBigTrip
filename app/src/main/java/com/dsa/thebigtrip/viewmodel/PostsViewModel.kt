package com.dsa.thebigtrip.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsa.thebigtrip.data.post.Post
import com.dsa.thebigtrip.data.post.PostRepository
import com.dsa.thebigtrip.data.user.User
import com.dsa.thebigtrip.data.user.UserRepository
import com.dsa.thebigtrip.data.weather.WeatherRepository
import kotlinx.coroutines.launch

class PostsViewModel : ViewModel() {

    private val postRepository = PostRepository.shared
    private val userRepository = UserRepository.shared
    private val weatherRepository = WeatherRepository.shared

    val allPosts: LiveData<List<Post>> = postRepository.getAllPosts()

    private val _allUsers = MutableLiveData<List<User>>(emptyList())
    val allUsers: LiveData<List<User>> = _allUsers

    private val _weatherSummaries = MutableLiveData<Map<String, String>>(emptyMap())
    val weatherSummaries: LiveData<Map<String, String>> = _weatherSummaries

    private val weatherLoadingPostIds = mutableSetOf<String>()

    fun getPostsByUserId(userId: String): LiveData<List<Post>> {
        return postRepository.getPostsByUserId(userId)
    }

    fun refreshPosts(onError: (() -> Unit)? = null, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            try {
                postRepository.refreshPosts()
            } catch (e: Exception) {
                onError?.invoke()
            } finally {
                onDone?.invoke()
            }
        }
    }

    fun deletePost(postId: String, onSuccess: () -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
            try {
                postRepository.deletePost(postId)
                onSuccess()
            } catch (e: Exception) {
                onError()
            }
        }
    }

    fun updatePost(post: Post, onSuccess: () -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
            try {
                postRepository.updatePost(post)
                onSuccess()
            } catch (e: Exception) {
                onError()
            }
        }
    }

    fun loadAllUsers() {
        viewModelScope.launch {
            _allUsers.value = try {
                userRepository.getAllUsers()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    fun loadUploaderNames(posts: List<Post>, onLoaded: (Map<String, String>) -> Unit) {
        viewModelScope.launch {
            val userNames = posts
                .map { it.userId }
                .distinct()
                .associateWith { userId ->
                    val user = userRepository.getUserById(userId)
                    user?.fullName ?: user?.email ?: userId
                }

            onLoaded(userNames)
        }
    }

    fun loadWeatherForPosts(posts: List<Post>) {
        posts
            .filter { post ->
                post.latitude != null &&
                        post.longitude != null &&
                        !_weatherSummaries.value.orEmpty().containsKey(post.id) &&
                        !weatherLoadingPostIds.contains(post.id)
            }
            .forEach { post ->
                weatherLoadingPostIds.add(post.id)
                viewModelScope.launch {
                    val summary = try {
                        weatherRepository.getWeatherSummary(post)
                    } catch (e: Exception) {
                        "Weather unavailable"
                    }

                    val updated = _weatherSummaries.value.orEmpty().toMutableMap()
                    updated[post.id] = summary
                    _weatherSummaries.value = updated
                    weatherLoadingPostIds.remove(post.id)
                }
            }
    }
}
