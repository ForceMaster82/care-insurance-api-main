package kr.caredoc.careinsurance.excel

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.apache.poi.ss.usermodel.Workbook
import org.springframework.web.servlet.view.document.AbstractXlsxView
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class StatisticSettlementExcel: AbstractXlsxView() {
    override fun buildExcelDocument(
        model: MutableMap<String, Any>,
        workbook: Workbook,
        request: HttpServletRequest,
        response: HttpServletResponse
    ) {
        val filename = URLEncoder.encode("관리자 아이디 생성.xlsx", StandardCharsets.UTF_8)
        response.setHeader("Content-Disposition", "attachment; filename=\"$filename\";")

        val list = model.get("data") as List<MutableMap<String, Any>>

        val sheet = workbook.createSheet("Sheet1")

        var r = 0
        var c = 0
        var row = sheet.createRow(r++)
        row.createCell(c++).setCellValue("차수별 간병인 소속")

        var i = 1
        for (map in list) {
            c = 0
            row = sheet.createRow(r++)

            row.createCell(c++).setCellValue(i.toDouble())
/*            row.createCell(c++).setCellValue(Converter.toStr(map.get("MB_NAME")))
            row.createCell(c++).setCellValue(Converter.toStr(map.get("MB_ID")))
            row.createCell(c++).setCellValue(Converter.toStr(map.get("RL_NAME")))
            row.createCell(c++).setCellValue(Converter.toStr(map.get("MB_INID")))
            row.createCell(c++).setCellValue(Converter.toStr(map.get("MB_LOGINDATE")))*/
            i++
        }

    }

}