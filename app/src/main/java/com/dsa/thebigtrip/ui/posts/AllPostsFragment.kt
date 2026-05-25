package com.dsa.thebigtrip.ui.posts

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dsa.thebigtrip.R
import com.dsa.thebigtrip.data.post.Post
import com.dsa.thebigtrip.ui.viewmodel.PostsViewModel
import com.google.firebase.auth.FirebaseAuth

class AllPostsFragment : Fragment(R.layout.fragment_my_posts) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private val viewModel: PostsViewModel by viewModels()
    private var postAdapter: PostAdapter? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerView = view.findViewById(R.id.postsRecyclerView)
        progressBar = view.findViewById(R.id.postsProgressBar)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        setLoading(true)
        observeWeather()
        loadPosts()
        refreshPosts()
    }

    private fun loadPosts() {
        viewModel.allPosts.observe(viewLifecycleOwner) { posts ->
            val currentUid = FirebaseAuth.getInstance().currentUser?.uid
            val visiblePosts = if (currentUid == null) {
                emptyList()
            } else {
                posts.filter { it.isVisibleTo(currentUid) }
            }
            showPosts(visiblePosts)
        }
    }

    private fun showPosts(posts: List<Post>) {
        setLoading(true)
        viewModel.loadUploaderNames(posts) { userNames ->
            postAdapter = PostAdapter(
                posts = posts,
                userNames = userNames,
                initialWeatherSummaries = viewModel.weatherSummaries.value.orEmpty(),
                showUploader = true
            )
            recyclerView.adapter = postAdapter
            viewModel.loadWeatherForPosts(posts)
            setLoading(false)
        }
    }

    private fun observeWeather() {
        viewModel.weatherSummaries.observe(viewLifecycleOwner) { summaries ->
            postAdapter?.updateWeatherSummaries(summaries)
        }
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
