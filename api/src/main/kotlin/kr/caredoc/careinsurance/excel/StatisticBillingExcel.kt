package kr.caredoc.careinsurance.excel

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kr.caredoc.careinsurance.util.Converter
import org.apache.poi.ss.usermodel.Workbook
import org.springframework.web.servlet.view.document.AbstractXlsxView
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class StatisticBillingExcel: AbstractXlsxView() {
    override fun buildExcelDocument(
        model: MutableMap<String, Any>,
        workbook: Workbook,
        request: HttpServletRequest,
        response: HttpServletResponse
    ) {
        val filename = URLEncoder.encode("기간별_월마감_조회_청구입출금일자.xlsx", StandardCharsets.UTF_8)
        response.setHeader("Content-Disposition", "attachment; filename=\"$filename\";")

        val list = model.get("data") as List<MutableMap<String, Any>>

        val sheet = workbook.createSheet("Sheet1")

        var r = 0
        var c = 0
        var row = sheet.createRow(r++)
        row.createCell(c++).setCellValue("번호")
        row.createCell(c++).setCellValue("차수별 간병인 소속")
        row.createCell(c++).setCellValue("신청일(접수등록일자)")
        row.createCell(c++).setCellValue("사고번호")
        row.createCell(c++).setCellValue("가입담보의 일일 간병비")
        row.createCell(c++).setCellValue("고객명(마스킹 유지)")
        row.createCell(c++).setCellValue("간병인명")
        row.createCell(c++).setCellValue("간병인 일당")
        row.createCell(c++).setCellValue("간병 시작일시(차수별)")
        row.createCell(c++).setCellValue("간병 종료일시(차수별)")
        row.createCell(c++).setCellValue("간병 차수")
        row.createCell(c++).setCellValue("차수별 청구 금액")
        row.createCell(c++).setCellValue("정산 예정일자")
        row.createCell(c++).setCellValue("산정 금액")
        row.createCell(c++).setCellValue("청구 입출금일자")
        row.createCell(c++).setCellValue("청구 입금금액")
        row.createCell(c++).setCellValue("청구 출금금액")
        row.createCell(c++).setCellValue("코로나(간병비 산정금액 상세)")
        row.createCell(c++).setCellValue("식대(간병비 산정금액 상세)")
        row.createCell(c++).setCellValue("교통비(간병비 산정금액 상세)")
        row.createCell(c++).setCellValue("명절(간병비 산정금액 상세)")
        row.createCell(c++).setCellValue("추가시간(간병비 산정금액 상세)")
        row.createCell(c++).setCellValue("기타(간병비 산정금액 상세)")
        row.createCell(c++).setCellValue("추가 총금액(간병비 산정금액)")
        row.createCell(c++).setCellValue("청구 추가 시간")
        row.createCell(c++).setCellValue("청구 추가 금액")

        var i = 1
        for (map in list) {
            c = 0
            row = sheet.createRow(r++)

            row.createCell(c++).setCellValue(i.toDouble())
            row.createCell(c++).setCellValue(Converter.toStr(map.get("name")))
            row.createCell(c++).setCellValue(Converter.toStr(map.get("received_date_time")))
            row.createCell(c++).setCellValue(Converter.toStr(map.get("accident_number")))
            row.createCell(c++).setCellValue(Converter.toDouble(map.get("billing_caregiving_charge")))
            row.createCell(c++).setCellValue(Converter.toStr(map.get("masked_patient_name")))
            row.createCell(c++).setCellValue(Converter.toStr(map.get("caregiver_name")))
            row.createCell(c++).setCellValue(Converter.toDouble(map.get("daily_caregiving_charge")))
            row.createCell(c++).setCellValue(Converter.toStr(map.get("start_date_time")))
            row.createCell(c++).setCellValue(Converter.toStr(map.get("end_date_time")))
            row.createCell(c++).setCellValue(Converter.toDouble(map.get("caregiving_round_number")))
            row.createCell(c++).setCellValue(Converter.toDouble(map.get("billing_total_amount")))
            row.createCell(c++).setCellValue(Converter.toStr(map.get("expected_settlement_date")))
            row.createCell(c++).setCellValue(Converter.toDouble(map.get("total_amount")))
            row.createCell(c++).setCellValue(Converter.toStr(map.get("date")))
            row.createCell(c++).setCellValue(Converter.toDouble(map.get("total_deposit_amount")))
            row.createCell(c++).setCellValue(Converter.toDouble(map.get("total_withdrawal_amount")))
            row.createCell(c++).setCellValue(Converter.toDouble(map.get("covid19_testing_cost")))
            row.createCell(c++).setCellValue(Converter.toDouble(map.get("meal_cost")))
            row.createCell(c++).setCellValue(Converter.toDouble(map.get("transportation_fee")))
            row.createCell(c++).setCellValue(Converter.toDouble(map.get("holiday_charge")))
            row.createCell(c++).setCellValue(Converter.toDouble(map.get("additional_hours_charge")))
            row.createCell(c++).setCellValue(Converter.toDouble(map.get("amount")))
            row.createCell(c++).setCellValue(Converter.toDouble(map.get("additional_amount")))
            row.createCell(c++).setCellValue(Converter.toDouble(map.get("additional_hours")))
            row.createCell(c++).setCellValue(Converter.toDouble(map.get("billing_additional_amount")))
            i++
        }

    }

}