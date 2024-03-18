package kr.caredoc.careinsurance.svc

import kr.caredoc.careinsurance.dao.ReceptionDao
import kr.caredoc.careinsurance.security.accesscontrol.Object
import org.springframework.stereotype.Service

@Service
class ReceptionSvc(
    private val receptionDao: ReceptionDao
) {
    fun one(receptionId:String): Map<String, Any> {
        return receptionDao.one(receptionId)
    }

    fun list(svcMap:Map<String, Any>): List<Map<String, Any>> {
        return receptionDao.list(svcMap)
    }
}