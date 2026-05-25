package com.dsa.thebigtrip.posts

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dsa.thebigtrip.R
import com.dsa.thebigtrip.data.post.Post
import com.dsa.thebigtrip.data.post.PostRepository
import com.dsa.thebigtrip.data.user.UserRepository
import com.dsa.thebigtrip.data.weather.WeatherRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class AllPostsFragment : Fragment(R.layout.fragment_my_posts) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private val postRepository = PostRepository.shared
    private val userRepository = UserRepository.shared
    private val weatherRepository = WeatherRepository.shared
    private val weatherSummaries = mutableMapOf<String, String>()
    private val weatherLoadingPostIds = mutableSetOf<String>()
    private var postAdapter: PostAdapter? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerView = view.findViewById(R.id.postsRecyclerView)
        progressBar = view.findViewById(R.id.postsProgressBar)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        setLoading(true)
        loadPosts()
        refreshPosts()
    }

    private fun loadPosts() {
        postRepository.getAllPosts().observe(viewLifecycleOwner) { posts ->
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
        lifecycleScope.launch {
            setLoading(true)
            val userNames = posts
                .map { it.userId }
                .distinct()
                .associateWith { userId ->
                    val user = userRepository.getUserById(userId)
                    user?.fullName ?: user?.email ?: userId
                }

            postAdapter = PostAdapter(
                posts = posts,
                userNames = userNames,
                initialWeatherSummaries = weatherSummaries,
                showUploader = true
            )
            recyclerView.adapter = postAdapter
            loadWeatherForPosts(posts)
            setLoading(false)
        }
    }

    private fun loadWeatherForPosts(posts: List<Post>) {
        posts
            .filter { post ->
                post.latitude != null &&
                        post.longitude != null &&
                        !weatherSummaries.containsKey(post.id) &&
                        !weatherLoadingPostIds.contains(post.id)
            }
            .forEach { post ->
                weatherLoadingPostIds.add(post.id)
                lifecycleScope.launch {
                    val summary = try {
                        weatherRepository.getWeatherSummary(post)
                    } catch (e: Exception) {
                        "Weather unavailable"
                    }

                    weatherSummaries[post.id] = summary
                    weatherLoadingPostIds.remove(post.id)
                    postAdapter?.updateWeather(post.id, summary)
                }
            }
    }

    private fun refreshPosts() {
        lifecycleScope.launch {
            try {
                setLoading(true)
                postRepository.refreshPosts()
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
