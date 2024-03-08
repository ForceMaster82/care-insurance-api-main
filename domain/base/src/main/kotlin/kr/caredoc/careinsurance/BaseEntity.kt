package kr.caredoc.careinsurance

import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.PostLoad
import jakarta.persistence.PostPersist
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.domain.Persistable
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseEntity(
    @Id
    private val id: String
) : Persistable<String> {
    @CreatedDate
    @Column(updatable = false)
    open var createdAt: LocalDateTime? = null

    @LastModifiedDate
    open var updatedAt: LocalDateTime? = null

    @Transient
    private var persisted: Boolean = false

    override fun getId(): String {
        return id
    }

    override fun isNew(): Boolean {
        return !persisted
    }

    @PostLoad
    protected open fun postLoad() {
        markAsPersisted()
    }

    private fun markAsPersisted() {
        persisted = true
    }

    @PostPersist
    protected open fun postPersist() {
        markAsPersisted()
    }
}
