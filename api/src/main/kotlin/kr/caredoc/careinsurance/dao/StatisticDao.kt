package kr.caredoc.careinsurance.dao

import kr.caredoc.careinsurance.security.accesscontrol.Object
import org.apache.ibatis.session.SqlSession
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository

@Repository
class StatisticDao(
    private val sqlSession: SqlSession
) {

    fun listSettlementExcel(svcMap: Map<String, Any>): List<Map<String, Any>> {
        return sqlSession.selectList("caredoc.statistic.listSettlementExcel", svcMap)
    }

    fun listBillingExcel(svcMap: Map<String, Any>): List<Map<String, Any>> {
        return sqlSession.selectList("caredoc.statistic.listBillingExcel", svcMap)
    }

}