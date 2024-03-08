package kr.caredoc.careinsurance.reception

import jakarta.persistence.Embeddable

@Embeddable
data class ReceptionApplicationFileInfo(
    val receptionApplicationFileName: String,
    val receptionApplicationFileUrl: String,
)
