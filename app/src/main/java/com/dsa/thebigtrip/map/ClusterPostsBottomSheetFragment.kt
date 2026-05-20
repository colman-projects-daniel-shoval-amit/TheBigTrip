package com.dsa.thebigtrip.map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.dsa.thebigtrip.R
import com.dsa.thebigtrip.databinding.FragmentClusterBottomSheetBinding
import com.dsa.thebigtrip.model.Post
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.util.UUID

class ClusterPostsBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentClusterBottomSheetBinding? = null
    private val binding get() = _binding!!

    companion object {
        // Posts stored by key because Post contains LatLng which is not Parcelable.
        private val pendingPosts = mutableMapOf<String, List<Post>>()

        fun newInstance(posts: List<Post>): ClusterPostsBottomSheetFragment {
            val key = UUID.randomUUID().toString()
            pendingPosts[key] = posts
            return ClusterPostsBottomSheetFragment().apply {
                arguments = bundleOf("postsKey" to key)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentClusterBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val key = requireArguments().getString("postsKey") ?: return
        val posts = pendingPosts.remove(key) ?: emptyList()

        binding.tvTitle.text = "Trips at this location (${posts.size})"
        binding.rvPosts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPosts.adapter = ClusterPostsAdapter(posts) { postId ->
            // Navigate via MapFragment's NavController.
            // setFragmentResult can't be used here: this dialog is attached to
            // MapFragment.childFragmentManager, so its parentFragmentManager differs
            // from the one MapFragment.setFragmentResultListener() subscribes to.
            requireParentFragment().findNavController().navigate(
                R.id.action_mapFragment_to_postDetailsFragment,
                Bundle().apply { putString("postId", postId) }
            )
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
