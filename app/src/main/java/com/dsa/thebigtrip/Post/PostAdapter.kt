package com.dsa.thebigtrip.posts

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dsa.thebigtrip.R
import com.dsa.thebigtrip.data.post.Post
import com.bumptech.glide.Glide

class PostAdapter(
    private val posts: List<Post>
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    class PostViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val location: TextView = view.findViewById(R.id.postLocation)
        val caption: TextView = view.findViewById(R.id.postCaption)
        val image: ImageView = view.findViewById(R.id.postImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post, parent, false)

        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {

        val post = posts[position]

        holder.location.text = post.locationName ?: "Unknown location"
        holder.caption.text = post.caption ?: ""

        Glide.with(holder.itemView.context)
            .load(post.imageUrl)
            .into(holder.image)
    }

    override fun getItemCount(): Int = posts.size
}