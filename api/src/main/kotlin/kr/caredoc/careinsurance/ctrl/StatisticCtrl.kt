package kr.caredoc.careinsurance.ctrl

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kr.caredoc.careinsurance.excel.StatisticBillingExcel
import kr.caredoc.careinsurance.excel.StatisticSettlementExcel
import kr.caredoc.careinsurance.svc.StatisticSvc
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.ModelAndView

@RestController
@RequestMapping("/api/v2/statistic/*")
class StatisticCtrl(
    private val statisticSvc: StatisticSvc,
) {
    private val logger: Logger = LoggerFactory.getLogger(StatisticCtrl::class.java)

    /**
     * 보고 지표 > 정산입출금현황 > 엑셀다운로드
     */
    @GetMapping("settlementExcelDown")
    @Throws(Exception::class)
    fun settlementExcelDown(
        req: HttpServletRequest,
        res: HttpServletResponse,
        @RequestParam params: Map<String, Any>
    ): ModelAndView {
        val resList: List<Map<String, Any>> = statisticSvc.listSettlementExcel(params)
        //logger.debug("resList : {}", resList)

        val resMap: MutableMap<String, Any> = mutableMapOf()
        resMap.put("data", resList)

        val modelAndView = ModelAndView(StatisticSettlementExcel(), resMap)
        return modelAndView
    }

    @GetMapping("billingExcelDown")
    @Throws(Exception::class)
    fun billingExcelDown(
        req: HttpServletRequest,
        res: HttpServletResponse,
        @RequestParam params: Map<String, Any>
    ): ModelAndView {
        val resList: List<Map<String, Any>> = statisticSvc.listBillingExcel(params)
        //logger.debug("resList : {}", resList)

        val resMap: MutableMap<String, Any> = mutableMapOf()
        resMap.put("data", resList)

        val modelAndView = ModelAndView(StatisticBillingExcel(), resMap)
        return modelAndView
    }


}
