package kr.caredoc.careinsurance.dao

import kr.caredoc.careinsurance.security.accesscontrol.Object
import org.apache.ibatis.session.SqlSession
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository

@Repository
class SettlementDao(
    private val sqlSession: SqlSession
) {

    fun cntTransaction(svcMap: Map<String, Any>): Int {
        return sqlSession.selectOne("caredoc.settlement.cntTransaction", svcMap)
    }

    fun listTransaction(svcMap: Map<String, Any>): List<Map<String, Any>> {
        return sqlSession.selectList("caredoc.settlement.listTransaction", svcMap)
    }

}