package kr.caredoc.careinsurance

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable

fun <T> List<T>.page(pageable: Pageable): Page<T> {
    val startIndex = pageable.pageSize * pageable.pageNumber
    if (startIndex >= this.size) {
        return PageImpl(
            listOf(),
            pageable,
            this.size.toLong(),
        )
    }

    val endIndex = Integer.min(startIndex + pageable.pageSize, this.size)

    return PageImpl(
        this.subList(startIndex, endIndex),
        pageable,
        this.size.toLong(),
    )
}
