package com.dsa.thebigtrip.data.post

data class PostDto(
    val id: String = "",
    val userId: String = "",
    val imageUrl: String = "",
    val caption: String = "",
    val timestamp: Long = 0,
    val likes: Int = 0
)
