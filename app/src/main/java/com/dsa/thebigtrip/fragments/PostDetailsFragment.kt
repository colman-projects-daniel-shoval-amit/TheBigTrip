package com.dsa.thebigtrip.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.dsa.thebigtrip.R
import com.dsa.thebigtrip.data.repository.posts.PostRepository
import com.dsa.thebigtrip.databinding.FragmentPostDetailsBinding
import com.dsa.thebigtrip.utils.ImageUtil
import kotlinx.coroutines.launch

class PostDetailsFragment : Fragment() {

    private var _binding: FragmentPostDetailsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPostDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val postId = arguments?.getString("postId")
        if (postId.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Post not found", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        loadPost(postId)
    }

    private fun loadPost(postId: String) {
        lifecycleScope.launch {
            val post = PostRepository.shared.getPostById(postId)

            if (post == null) {
                Toast.makeText(requireContext(), "Post not found", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
                return@launch
            }

            binding.tvTitle.text = post.title
            binding.tvDescription.text = post.description
            binding.tvLocation.text = "%.6f, %.6f".format(post.location.latitude, post.location.longitude)

            if (!post.imageUri.isNullOrEmpty()) {
                ImageUtil.loadImage(binding.ivPostImage, post.imageUri)
            } else {
                binding.ivPostImage.setImageResource(R.drawable.ic_camera)
            }

            binding.progressBar.visibility = View.GONE
            binding.cardContent.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
