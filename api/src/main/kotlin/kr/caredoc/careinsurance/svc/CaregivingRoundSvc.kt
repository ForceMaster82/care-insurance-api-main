package kr.caredoc.careinsurance.svc

import kr.caredoc.careinsurance.dao.CaregivingRoundDao
import org.springframework.stereotype.Service

@Service
class CaregivingRoundSvc(
    private val caregivingRoundDao: CaregivingRoundDao
) {
    fun listByReception(receptionId: String): List<Map<String, Any>> {
        return caregivingRoundDao.listByReception(receptionId)
    }
}