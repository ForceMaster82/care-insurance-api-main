package kr.caredoc.careinsurance.ctrl

import jakarta.servlet.http.HttpServletRequest
import kr.caredoc.careinsurance.svc.ReceptionSvc
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v2/reception")
class ReceptionCtrl(
    private val receptionSvc: ReceptionSvc,
) {
    private val logger: Logger = LoggerFactory.getLogger(ReceptionCtrl::class.java)

    @ResponseBody
    @GetMapping("/receptionList")
    @Throws(Exception::class)
    fun receptionList(
        req: HttpServletRequest,
        @RequestParam params: Map<String, Any>
    ): Any {
        logger.debug("12354 {}", params)
        return receptionSvc.list(params)
    }
}
