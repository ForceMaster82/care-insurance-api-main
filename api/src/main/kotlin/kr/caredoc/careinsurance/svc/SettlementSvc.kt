package kr.caredoc.careinsurance.svc

import kr.caredoc.careinsurance.dao.SettlementDao
import kr.caredoc.careinsurance.util.Pager
import org.springframework.stereotype.Service


@Service
class SettlementSvc(
    private val settlementDao: SettlementDao
) {

    fun pageTransaction(svcMap:MutableMap<String, Any>): Map<String, Any> {
        val resMap: MutableMap<String, Any> = mutableMapOf()

        val cnt = settlementDao.cntTransaction(svcMap)
        val pgMap = Pager().setInfo(cnt, svcMap)

        svcMap.putAll(pgMap)
        val list = settlementDao.listTransaction(svcMap)

        resMap.put("items", list)
        resMap.put("currentPageNumber", svcMap.get("page-number")!!)
        resMap.put("lastPageNumber", pgMap.get("totpage")!!)
        resMap.put("totalItemCount", cnt)
        return resMap
    }
}