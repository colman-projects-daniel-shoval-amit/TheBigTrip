package com.dsa.thebigtrip.map

import com.dsa.thebigtrip.model.Post
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem

class PostClusterItem(val post: Post) : ClusterItem {
    override fun getPosition(): LatLng = post.location
    override fun getTitle(): String = post.title
    override fun getSnippet(): String? = null
    override fun getZIndex(): Float? = 0f
}
