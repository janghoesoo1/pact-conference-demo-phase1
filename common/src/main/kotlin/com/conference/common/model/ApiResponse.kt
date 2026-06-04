package com.conference.common.model

data class ApiResponse<T>(
    val data: List<T>,
    val total: Int = data.size
)
