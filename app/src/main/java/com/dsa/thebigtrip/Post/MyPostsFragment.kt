package com.dsa.thebigtrip.posts

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dsa.thebigtrip.R
import com.dsa.thebigtrip.data.post.Post
import com.dsa.thebigtrip.data.user.User
import com.dsa.thebigtrip.viewmodel.PostsViewModel
import com.google.firebase.auth.FirebaseAuth

class MyPostsFragment : Fragment(R.layout.fragment_my_posts) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private val viewModel: PostsViewModel by viewModels()
    private val auth = FirebaseAuth.getInstance()
    private var allUsers: List<User> = emptyList()
    private var postAdapter: PostAdapter? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerView = view.findViewById(R.id.postsRecyclerView)
        progressBar = view.findViewById(R.id.postsProgressBar)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        setLoading(true)
        loadAllUsers()
        observeWeather()
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

        viewModel.getPostsByUserId(uid).observe(viewLifecycleOwner) { posts ->
            setLoading(false)
            postAdapter = PostAdapter(
                posts = posts,
                initialWeatherSummaries = viewModel.weatherSummaries.value.orEmpty(),
                showActions = true,
                onEditClick = { post -> openEditPost(post) },
                onDeleteClick = { post -> confirmDeletePost(post) },
                onManagePermissionsClick = { post -> showManagePermissionsDialog(post) }
            )
            recyclerView.adapter = postAdapter
            viewModel.loadWeatherForPosts(posts)
        }
    }

    private fun observeWeather() {
        viewModel.weatherSummaries.observe(viewLifecycleOwner) { summaries ->
            postAdapter?.updateWeatherSummaries(summaries)
        }
    }

    private fun loadAllUsers() {
        viewModel.allUsers.observe(viewLifecycleOwner) { users ->
            allUsers = users
        }
        viewModel.loadAllUsers()
    }

    private fun openEditPost(post: Post) {
        val action = MyPostsFragmentDirections
            .actionMyPostsFragmentToCreatePostFragment(post.id)
        findNavController().navigate(action)
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
        viewModel.deletePost(
            postId = post.id,
            onSuccess = {
                Toast.makeText(requireContext(), "Post deleted", Toast.LENGTH_SHORT).show()
            },
            onError = {
                Toast.makeText(requireContext(), "Failed to delete post", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun showManagePermissionsDialog(post: Post) {
        val candidates = allUsers.filter { it.uid != post.userId }

        if (candidates.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "No other users available to share with",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val labels = candidates.map { user ->
            user.fullName?.takeIf { it.isNotBlank() }
                ?: user.email?.takeIf { it.isNotBlank() }
                ?: user.uid
        }.toTypedArray()

        val selectedIds = post.visibleTo.toMutableSet()
        val checked = BooleanArray(candidates.size) { index ->
            selectedIds.contains(candidates[index].uid)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Who can view this post")
            .setMultiChoiceItems(labels, checked) { _, which, isChecked ->
                val uid = candidates[which].uid
                if (isChecked) {
                    selectedIds.add(uid)
                } else {
                    selectedIds.remove(uid)
                }
            }
            .setPositiveButton("Save") { _, _ ->
                savePermissions(post, selectedIds.toList())
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun savePermissions(post: Post, visibleTo: List<String>) {
        val updated = post.copy(visibleTo = visibleTo.filter { it != post.userId })
        viewModel.updatePost(
            post = updated,
            onSuccess = {
                Toast.makeText(requireContext(), "Permissions updated", Toast.LENGTH_SHORT).show()
            },
            onError = {
                Toast.makeText(requireContext(), "Failed to update permissions", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun refreshPosts() {
        setLoading(true)
        viewModel.refreshPosts(
            onError = {
                Toast.makeText(requireContext(), "Failed to refresh posts", Toast.LENGTH_SHORT).show()
            },
            onDone = {
                setLoading(false)
            }
        )
    }

    private fun setLoading(isLoading: Boolean) {
        if (!::progressBar.isInitialized) return
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
}
