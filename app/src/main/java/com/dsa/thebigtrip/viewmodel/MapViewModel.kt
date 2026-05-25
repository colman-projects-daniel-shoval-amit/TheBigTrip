package com.dsa.thebigtrip.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsa.thebigtrip.data.post.Post
import com.dsa.thebigtrip.data.post.PostRepository
import kotlinx.coroutines.launch

class MapViewModel : ViewModel() {

    private val postRepository = PostRepository.shared

    val posts: LiveData<List<Post>> = postRepository.getAllPosts()

    fun refreshPosts(onError: () -> Unit) {
        viewModelScope.launch {
            try {
                postRepository.refreshPosts()
            } catch (e: Exception) {
                onError()
            }
        }
    }
}
