package kr.caredoc.careinsurance.ctrl


import kr.caredoc.careinsurance.security.hash.PepperedHasher
import kr.caredoc.careinsurance.svc.SettlementSvc
import kr.caredoc.careinsurance.toHex
import kr.caredoc.careinsurance.util.Converter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v2/settlement")
class SettlementCtrl(
    private val settlementSvc: SettlementSvc,
    private val patientNameHasher: PepperedHasher,
) {
    private val logger: Logger = LoggerFactory.getLogger(SettlementCtrl::class.java)

    @ResponseBody
    @GetMapping("/logTransaction")
    @Throws(Exception::class)
    fun logTransaction(
        @RequestParam params: MutableMap<String, Any>,
    ): Any {
        val query = Converter.toStr(params.get("query"))
        if (!"".equals(query)) {
            val queryArr = query.split(":")
            if ("patientName".equals(queryArr[0])) {
                params.put(queryArr[0], patientNameHasher.hash(queryArr[1].toByteArray()).toHex())
            } else {
                params.put(queryArr[0], queryArr[1])
            }
        }

        val resMap = settlementSvc.pageTransaction(params)
        return resMap
    }
}
