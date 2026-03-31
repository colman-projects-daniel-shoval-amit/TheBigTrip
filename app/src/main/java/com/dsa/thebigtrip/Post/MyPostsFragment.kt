package com.dsa.thebigtrip.posts

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dsa.thebigtrip.R
import com.dsa.thebigtrip.data.post.PostRepository

class MyPostsFragment : Fragment(R.layout.fragment_my_posts) {

    private lateinit var recyclerView: RecyclerView
    private val repository = PostRepository.shared

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerView = view.findViewById(R.id.postsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        loadPosts()
    }

    private fun loadPosts() {
        val postId = arguments?.getString("postId")

        repository.getAllPosts().observe(viewLifecycleOwner) { posts ->
            val filtered = if (postId != null) {
                posts.filter { it.id == postId }
            } else {
                posts
            }
            recyclerView.adapter = PostAdapter(filtered)
        }
    }
}