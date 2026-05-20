package com.dsa.thebigtrip.posts

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dsa.thebigtrip.R
import com.dsa.thebigtrip.data.post.Post
import com.dsa.thebigtrip.data.post.PostRepository
import com.dsa.thebigtrip.data.user.UserRepository
import kotlinx.coroutines.launch

class AllPostsFragment : Fragment(R.layout.fragment_my_posts) {

    private lateinit var recyclerView: RecyclerView
    private val postRepository = PostRepository.shared
    private val userRepository = UserRepository.shared

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerView = view.findViewById(R.id.postsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        loadPosts()
        refreshPosts()
    }

    private fun loadPosts() {
        postRepository.getAllPosts().observe(viewLifecycleOwner) { posts ->
            showPosts(posts)
        }
    }

    private fun showPosts(posts: List<Post>) {
        lifecycleScope.launch {
            val userNames = posts
                .map { it.userId }
                .distinct()
                .associateWith { userId ->
                    val user = userRepository.getUserById(userId)
                    user?.fullName ?: user?.email ?: userId
                }

            recyclerView.adapter = PostAdapter(posts, userNames, showUploader = true)
        }
    }

    private fun refreshPosts() {
        lifecycleScope.launch {
            try {
                postRepository.refreshPosts()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to refresh posts", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
