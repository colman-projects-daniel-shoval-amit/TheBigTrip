package com.dsa.thebigtrip.map

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dsa.thebigtrip.R
import com.dsa.thebigtrip.databinding.ItemClusterPostBinding
import com.dsa.thebigtrip.model.Post
import com.dsa.thebigtrip.utils.ImageUtil

class ClusterPostsAdapter(
    private val posts: List<Post>,
    private val onPostClick: (postId: String) -> Unit
) : RecyclerView.Adapter<ClusterPostsAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemClusterPostBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(post: Post) {
            binding.tvPostTitle.text = post.title
            binding.tvPostExcerpt.text = post.description
            ImageUtil.loadImage(binding.ivThumbnail, post.imageUri, R.drawable.ic_camera)
            binding.root.setOnClickListener { onPostClick(post.id) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemClusterPostBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(posts[position])

    override fun getItemCount(): Int = posts.size
}
