package kr.caredoc.careinsurance.dao

import kr.caredoc.careinsurance.security.accesscontrol.Object
import org.apache.ibatis.session.SqlSession
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository

@Repository
class ReceptionDao(
    private val sqlSession: SqlSession
) {

    fun one(receptionId: String): Map<String, Any> {
        return sqlSession.selectOne("caredoc.reception.one", receptionId)
    }

    fun list(svcMap: Map<String, Any>): List<Map<String, Any>> {
        return sqlSession.selectList("caredoc.reception.list", svcMap)
    }
}