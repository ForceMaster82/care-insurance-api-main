package kr.caredoc.careinsurance.ctrl

import jakarta.servlet.http.HttpServletRequest
import kr.caredoc.careinsurance.svc.ReceptionSvc
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/rec/*")
class ReceptionCtrl(
    private val receptionSvc: ReceptionSvc,
) {
    @ResponseBody
    @PostMapping("receptionList")
    @Throws(Exception::class)
    fun inAjax(
        req: HttpServletRequest,
        @RequestParam params: Map<String, Any>
    ): Any {
        return receptionSvc.list(params)
    }
}
