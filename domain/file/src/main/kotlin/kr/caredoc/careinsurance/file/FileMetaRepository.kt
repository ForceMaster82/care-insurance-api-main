package kr.caredoc.careinsurance.file

import org.springframework.data.jpa.repository.JpaRepository

interface FileMetaRepository : JpaRepository<FileMeta, String> {
    fun findByUrlAndStatus(url: String, status: FileMeta.Status): FileMeta?
}
