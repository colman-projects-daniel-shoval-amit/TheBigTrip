package com.dsa.thebigtrip.posts

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dsa.thebigtrip.R
import com.dsa.thebigtrip.data.post.Post
import com.dsa.thebigtrip.data.post.PostRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class MyPostsFragment : Fragment(R.layout.fragment_my_posts) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private val repository = PostRepository.shared
    private val auth = FirebaseAuth.getInstance()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerView = view.findViewById(R.id.postsRecyclerView)
        progressBar = view.findViewById(R.id.postsProgressBar)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        setLoading(true)
        loadPosts()
        refreshPosts()
    }

    private fun loadPosts() {
        val uid = auth.currentUser?.uid

        if (uid == null) {
            recyclerView.adapter = PostAdapter(emptyList())
            Toast.makeText(requireContext(), "Please log in to see your posts", Toast.LENGTH_SHORT).show()
            return
        }

        repository.getPostsByUserId(uid).observe(viewLifecycleOwner) { posts ->
            setLoading(false)
            recyclerView.adapter = PostAdapter(
                posts = posts,
                showActions = true,
                onEditClick = { post -> openEditPost(post) },
                onDeleteClick = { post -> confirmDeletePost(post) }
            )
        }
    }

    private fun openEditPost(post: Post) {
        val args = Bundle().apply {
            putString("postId", post.id)
        }
        findNavController().navigate(R.id.createPostFragment, args)
    }

    private fun confirmDeletePost(post: Post) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete post?")
            .setMessage("This post will be removed from your profile.")
            .setPositiveButton("Delete") { _, _ ->
                deletePost(post)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deletePost(post: Post) {
        lifecycleScope.launch {
            try {
                repository.deletePost(post.id)
                Toast.makeText(requireContext(), "Post deleted", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to delete post", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun refreshPosts() {
        lifecycleScope.launch {
            try {
                setLoading(true)
                repository.refreshPosts()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to refresh posts", Toast.LENGTH_SHORT).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        if (!::progressBar.isInitialized) return
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
}
