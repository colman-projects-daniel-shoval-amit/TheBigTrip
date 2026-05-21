package com.dsa.thebigtrip.posts

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dsa.thebigtrip.R
import com.dsa.thebigtrip.data.post.Post
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PostAdapter(
    private val posts: List<Post>,
    private val userNames: Map<String, String> = emptyMap(),
    private val showUploader: Boolean = false,
    private val showActions: Boolean = false,
    private val onEditClick: ((Post) -> Unit)? = null,
    private val onDeleteClick: ((Post) -> Unit)? = null
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    class PostViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val location: TextView = view.findViewById(R.id.postLocation)
        val uploader: TextView = view.findViewById(R.id.postUploader)
        val date: TextView = view.findViewById(R.id.postDate)
        val caption: TextView = view.findViewById(R.id.postCaption)
        val image: ImageView = view.findViewById(R.id.postImage)
        val actions: View = view.findViewById(R.id.postActions)
        val editButton: ImageButton = view.findViewById(R.id.btnEditPost)
        val deleteButton: ImageButton = view.findViewById(R.id.btnDeletePost)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post, parent, false)

        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {

        val post = posts[position]

        holder.location.text = post.locationName ?: "Unknown location"
        if (!showUploader) {
            holder.uploader.visibility = View.GONE
        } else {
            holder.uploader.visibility = View.VISIBLE
            holder.uploader.text = "Uploaded by ${userNames[post.userId] ?: post.userId}"
        }
        holder.date.text = formatPostDate(post.createdAt)
        holder.caption.text = post.caption ?: ""
        holder.actions.visibility = if (showActions) View.VISIBLE else View.GONE
        holder.editButton.setOnClickListener { onEditClick?.invoke(post) }
        holder.deleteButton.setOnClickListener { onDeleteClick?.invoke(post) }

        Glide.with(holder.itemView.context)
            .load(post.imageUrl)
            .placeholder(R.drawable.bg_image_placeholder)
            .error(R.drawable.bg_image_placeholder)
            .into(holder.image)
    }

    override fun getItemCount(): Int = posts.size

    private fun formatPostDate(createdAt: Long): String {
        if (createdAt <= 0) {
            return "Published date unknown"
        }

        val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return "Published ${formatter.format(Date(createdAt))}"
    }
}
