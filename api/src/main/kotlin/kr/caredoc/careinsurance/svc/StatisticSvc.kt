package kr.caredoc.careinsurance.svc

import kr.caredoc.careinsurance.dao.ReceptionDao
import kr.caredoc.careinsurance.dao.StatisticDao
import kr.caredoc.careinsurance.security.accesscontrol.Object
import org.springframework.stereotype.Service

@Service
class StatisticSvc(
    private val statisticDao: StatisticDao
) {

    fun listSettlementExcel(svcMap:Map<String, Any>): List<Map<String, Any>> {
        return statisticDao.listSettlementExcel(svcMap)
    }
}