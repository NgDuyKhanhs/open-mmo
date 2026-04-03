package com.openmmo.ai.dto

/**
 * Generic API Response Wrapper
 * Used for all API responses with data
 */
data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Pagination Request DTO
 */
data class PaginationRequest(
    val page: Int = 0,
    val size: Int = 20,
    val sortBy: String = "id",
    val sortOrder: String = "ASC"
) {
    fun validate(): String? {
        return when {
            page < 0 -> "Trang không được âm"
            size <= 0 -> "Kích thước trang phải lớn hơn 0"
            size > 100 -> "Kích thước trang tối đa là 100"
            else -> null
        }
    }
}

/**
 * Pagination Response DTO
 */
data class PaginatedResponse<T>(
    val content: List<T>,
    val totalElements: Long,
    val totalPages: Int,
    val currentPage: Int,
    val pageSize: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean
)
