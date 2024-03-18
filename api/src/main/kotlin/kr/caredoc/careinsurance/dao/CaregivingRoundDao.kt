package kr.caredoc.careinsurance.dao

import kr.caredoc.careinsurance.security.accesscontrol.Object
import org.apache.ibatis.session.SqlSession
import org.springframework.stereotype.Repository

@Repository
class CaregivingRoundDao(
    private val sqlSession: SqlSession
) {

    fun listByReception(receptionId: String): List<Map<String, Any>> {
        return sqlSession.selectList("caredoc.caregivingRound.listByReception", receptionId)
    }
}