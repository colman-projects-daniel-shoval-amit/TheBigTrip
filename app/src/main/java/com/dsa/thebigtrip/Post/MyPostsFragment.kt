package com.dsa.thebigtrip.posts

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dsa.thebigtrip.R
import com.dsa.thebigtrip.data.post.Post
import com.dsa.thebigtrip.data.post.PostRepository
import kotlinx.coroutines.launch

class MyPostsFragment : Fragment(R.layout.fragment_my_posts) {

    private lateinit var recyclerView: RecyclerView
    private val repository = PostRepository.shared

    private fun insertStubPosts() {
        lifecycleScope.launch {

            val posts = listOf(
                Post(
                    "1",
                    "user1",
                    "Sunset by the sea 🌅",
                    "https://picsum.photos/600/400",
                    System.currentTimeMillis(),
                    "Tel Aviv",
                    32.0853,
                    34.7818
                ),
                Post("2","user1","Morning coffee ☕","https://picsum.photos/600/401",System.currentTimeMillis(),"Haifa",32.7940,34.9896),
                Post("3","user1","Hiking day 🏔️","https://picsum.photos/600/402",System.currentTimeMillis(),"Alps",46.8182,8.2275),
                Post("4","user1","City lights at night 🌃","https://picsum.photos/600/403",System.currentTimeMillis(),"Berlin",52.5200,13.4050),
                Post("5","user1","Exploring the streets","https://picsum.photos/600/404",System.currentTimeMillis(),"Barcelona",41.3851,2.1734),
                Post("6","user1","Beautiful mountains","https://picsum.photos/600/405",System.currentTimeMillis(),"Dolomites",46.4333,11.8500),
                Post("7","user1","Beach vibes 🌊","https://picsum.photos/600/406",System.currentTimeMillis(),"Nice",43.7102,7.2620),
                Post("8","user1","Night walk","https://picsum.photos/600/407",System.currentTimeMillis(),"Vienna",48.2082,16.3738)
            )

            posts.forEach {
                PostRepository.shared.addPost(it)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)
        recyclerView = view.findViewById(R.id.postsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        insertStubPosts()
        loadPosts()
    }

    private fun loadPosts() {

        repository.getAllPosts().observe(viewLifecycleOwner) { posts ->
            recyclerView.adapter = PostAdapter(posts)
        }
    }
}