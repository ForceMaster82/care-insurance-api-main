package kr.caredoc.careinsurance.file

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import kr.caredoc.careinsurance.BaseEntity

@Entity
class FileMeta(
    id: String,
    val bucket: String,
    val path: String,
) : BaseEntity(id) {
    @Enumerated(EnumType.STRING)
    var status: Status = Status.READY
        protected set
    var url: String? = null
        protected set
    var mime: String? = null
        protected set
    var contentLength: Long? = null
        protected set

    fun markAsUploaded(url: String, mime: String, contentLength: Long) {
        status = Status.UPLOADED
        this.url = url
        this.mime = mime
        this.contentLength = contentLength
    }

    fun scheduleDelete() {
        status = Status.TO_BE_DELETED
    }

    fun markAsDeleted() {
        status = Status.DELETED
    }

    enum class Status {
        READY,
        UPLOADED,
        TO_BE_DELETED,
        DELETED,
    }
}
