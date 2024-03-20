package kr.caredoc.careinsurance.ctrl

import jakarta.servlet.http.HttpServletRequest
import kr.caredoc.careinsurance.svc.ReceptionSvc
import kr.caredoc.careinsurance.web.settlement.request.IdentifiedSettlementEditingRequest
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v2/settlement")
class SettlementCtrl(
    private val receptionSvc: ReceptionSvc,
) {
    private val logger: Logger = LoggerFactory.getLogger(SettlementCtrl::class.java)

    @ResponseBody
    @GetMapping("/sendCalculate")
    @Throws(Exception::class)
    fun sendCalculate(
        req: HttpServletRequest,
        @RequestParam params: Map<String, Any>,
    ): Any {
        logger.debug("params : {}", params)
        logger.debug("from : {}", req.getParameter("from"))

        //return receptionSvc.list(params)
        return receptionSvc.one("01HD05RT3N32H81WSHMGHF0PH2")
    }
}
