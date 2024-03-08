package kr.caredoc.careinsurance

import jakarta.persistence.EntityManager
import jakarta.persistence.criteria.CriteriaQuery
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable

fun <T> EntityManager.getPagedResult(
    contentQuery: CriteriaQuery<T>,
    countQuery: CriteriaQuery<Long>,
    pageable: Pageable,
): Page<T> {
    val content = this.createQuery(contentQuery)
        .setFirstResult(pageable.offset.toInt())
        .setMaxResults(pageable.pageSize)
        .resultList
    val count = this.createQuery(countQuery)
        .resultList.firstOrNull() ?: 0

    return PageImpl(
        content,
        pageable,
        count,
    )
}
