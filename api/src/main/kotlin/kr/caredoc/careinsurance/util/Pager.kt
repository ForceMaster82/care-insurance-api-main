package kr.caredoc.careinsurance.util

import kr.caredoc.careinsurance.util.Converter.Companion.toInt
import kr.caredoc.careinsurance.web.paging.PagingRequest

class Pager {
    fun setInfo(totalCnt: Int, svcMap: MutableMap<String, Any>): MutableMap<String, Any> {
        var r_page = toInt(svcMap.get("page-number"), 1) // 페이지
        var r_limitrow = toInt(svcMap.get("page-size"), 10) // 한페이지에 보여줄 레코드수

        if (r_page == 0) r_page = 1
        if (r_limitrow == 0) r_limitrow = 10

        val resMap: MutableMap<String, Any> = mutableMapOf()

        if (r_limitrow > 0) {
            val totalPage = if ((totalCnt % r_limitrow == 0)) totalCnt / r_limitrow else totalCnt / r_limitrow + 1 // 전체페이지 수
            if (r_page > totalPage) r_page = 1

            val r_endrow = r_page * r_limitrow // 끝지점
            val r_startrow = r_endrow - r_limitrow + 1 // 시작지점

            // 2014-10-22. CHAE. 일련번호 설정
            val startnumber = totalCnt - (r_limitrow * (r_page - 1))

            // xml에 보낼 파라미터 생성
            resMap.put("r_startrow", r_startrow)
            resMap.put("r_endrow", r_endrow)
            resMap.put("startnumber", startnumber)
            resMap.put("totpage", totalPage)
        }

        return resMap
    }
}