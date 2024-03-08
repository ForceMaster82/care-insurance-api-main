package kr.caredoc.careinsurance.web.paging

data class PagedResponse<T>(
    val currentPageNumber: Int,
    val lastPageNumber: Int,
    val totalItemCount: Long,
    val items: Collection<T>,
) {
    companion object {
        fun <T> empty(pageNumber: Int) = PagedResponse<T>(
            pageNumber,
            1,
            0,
            listOf(),
        )
    }
}
