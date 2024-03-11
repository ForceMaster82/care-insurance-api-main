package kr.caredoc.careinsurance.caregiving.certificate

import com.lowagie.text.Document
import com.lowagie.text.Element
import com.lowagie.text.Font
import com.lowagie.text.FontFactory
import com.lowagie.text.Image
import com.lowagie.text.Paragraph
import com.lowagie.text.Rectangle
import com.lowagie.text.pdf.BaseFont
import com.lowagie.text.pdf.PdfPCell
import com.lowagie.text.pdf.PdfPTable
import com.lowagie.text.pdf.PdfWriter
import kr.caredoc.careinsurance.Clock
import kr.caredoc.careinsurance.patient.Sex
import org.springframework.core.io.ClassPathResource
import java.awt.Color
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object CertificateTemplate {
    init {
        val fontResource = ClassPathResource("/static/font/NanumGothic-Regular.ttf")
        val fontPath = fontResource.url.toString()
        FontFactory.register(fontPath)
    }

    private val phoneNumberPattern = Regex("""^(\d{3})([\d*]{3,4})(\d{4})$""")

    private data class Style(
        val documentTitleFont: Font,
        val tableTitleFont: Font,
        val contentLabelFont: Font,
        val contentFont: Font,
        val plainTextFont: Font,
        val cautionFont: Font,
        val criticalCautionFont: Font,
        val disclaimerFont: Font,
        val signingInfoFont: Font,
        val signerTypeFont: Font,
        val signingPlaceHolderFont: Font,
        val receiverFont: Font,
        val caregivingPeriodFont: Font,
        val redLineColor: Color,
    )

    private val style: Style by lazy {
        Style(
            documentTitleFont = FontFactory.getFont(
                "nanumgothic",
                BaseFont.IDENTITY_H,
                true,
                12f,
                1,
                Color(32, 32, 32)
            ),
            tableTitleFont = FontFactory.getFont(
                "nanumgothic",
                BaseFont.IDENTITY_H,
                true,
                9f,
                1,
                Color(204, 9, 46),
            ),
            contentLabelFont = FontFactory.getFont(
                "nanumgothic",
                BaseFont.IDENTITY_H,
                true,
                7f,
                0,
                Color(30, 30, 30)
            ),
            contentFont = FontFactory.getFont(
                "nanumgothic",
                BaseFont.IDENTITY_H,
                true,
                9f,
                1,
                Color.black
            ),
            plainTextFont = FontFactory.getFont(
                "nanumgothic",
                BaseFont.IDENTITY_H,
                true,
                9f,
                0,
                Color(87, 87, 87)
            ),
            cautionFont = FontFactory.getFont(
                "nanumgothic",
                BaseFont.IDENTITY_H,
                true,
                7.5f,
                1,
                Color(87, 87, 87)
            ),
            criticalCautionFont = FontFactory.getFont(
                "nanumgothic",
                BaseFont.IDENTITY_H,
                true,
                7.5f,
                0,
                Color.RED
            ),
            disclaimerFont = FontFactory.getFont(
                "nanumgothic",
                BaseFont.IDENTITY_H,
                true,
                8f,
                0,
                Color(87, 87, 87)
            ),
            signingInfoFont = FontFactory.getFont(
                "nanumgothic",
                BaseFont.IDENTITY_H,
                true,
                9f,
                1,
                Color(87, 87, 87)
            ),
            signerTypeFont = FontFactory.getFont(
                "nanumgothic",
                BaseFont.IDENTITY_H,
                true,
                8f,
                0,
                Color(87, 87, 87)
            ),
            signingPlaceHolderFont = FontFactory.getFont(
                "nanumgothic",
                BaseFont.IDENTITY_H,
                true,
                8f,
                0,
                Color(128, 128, 128)
            ),
            receiverFont = FontFactory.getFont(
                "nanumgothic",
                BaseFont.IDENTITY_H,
                true,
                14f,
                1,
                Color.black
            ),
            caregivingPeriodFont = FontFactory.getFont(
                "nanumgothic",
                BaseFont.IDENTITY_H,
                true,
                7f,
                0,
                Color.black
            ),
            redLineColor = Color(231, 0, 51),
        )
    }

    private fun PdfPTable.nestedTable(numColumns: Int, block: PdfPTable.() -> Unit = {}) {
        this.addCell(
            PdfPCell(
                PdfPTable(numColumns).apply {
                    block(this)
                }
            ).apply { noBorder() }
        )
    }

    private fun PdfPTable.contentLabelCell(label: String, block: PdfPCell.() -> Unit = {}): PdfPCell {
        val cell = PdfPCell(Paragraph(label, style.contentLabelFont))
        cell.borderColor = Color(228, 228, 228)
        cell.borderWidth = 0.75f
        cell.backgroundColor = Color(248, 241, 239)
        cell.verticalAlignment = Element.ALIGN_MIDDLE
        cell.paddingTop = 5f
        cell.paddingBottom = 5f
        block(cell)

        this.addCell(cell)

        return cell
    }

    private fun PdfPTable.contentCell(content: String, block: PdfPCell.() -> Unit = {}): PdfPCell {
        val cell = PdfPCell(Paragraph(content, style.contentFont))
        cell.borderColor = Color(228, 228, 228)
        cell.borderWidth = 0.75f
        cell.verticalAlignment = Element.ALIGN_MIDDLE
        cell.paddingTop = 5f
        cell.paddingBottom = 5f
        block(cell)

        this.addCell(cell)

        return cell
    }

    private fun PdfPTable.checkBoxCell(checked: Boolean, block: PdfPCell.() -> Unit = {}): PdfPCell {
        val checkBoxPath = if (checked) {
            ClassPathResource("/static/image/checked.png").url.toString()
        } else {
            ClassPathResource("/static/image/unchecked.png").url.toString()
        }

        val checkBox = Image.getInstance(checkBoxPath).apply {
            scalePercent(40f)
        }

        val cell = PdfPCell(checkBox)
        cell.borderColor = Color(228, 228, 228)
        cell.borderWidth = 0.75f
        cell.verticalAlignment = Element.ALIGN_MIDDLE
        cell.horizontalAlignment = Element.ALIGN_CENTER

        block(cell)

        this.addCell(cell)

        return cell
    }

    private fun contentTitleCell(content: String) = PdfPCell(Paragraph(content, style.tableTitleFont)).apply {
        this.border = PdfPCell.TOP + PdfPCell.BOTTOM
        this.borderColor = style.redLineColor
        this.borderWidth = 0.75f
    }

    private fun PdfPTable.summaryCell(summary: String, block: PdfPCell.() -> Unit = {}): PdfPCell {
        val cell = PdfPCell(Paragraph(summary, style.plainTextFont))
        cell.borderColor = Color(228, 228, 228)
        cell.borderWidth = 0.75f
        block(cell)

        this.addCell(cell)

        return cell
    }

    private fun PdfPTable.imageCell(image: Image, block: PdfPCell.() -> Unit = {}): PdfPCell {
        val cell = PdfPCell(image)
        cell.borderColor = Color(228, 228, 228)
        cell.borderWidth = 0.75f
        block(cell)

        this.addCell(cell)

        return cell
    }

    private fun PdfPTable.cautionCell(
        caution: String,
        critical: Boolean = false,
        block: PdfPCell.() -> Unit = {}
    ): PdfPCell {
        val font = if (critical) {
            style.criticalCautionFont
        } else {
            style.cautionFont
        }

        val cell = PdfPCell(Paragraph(caution, font))
        cell.borderColor = Color(228, 228, 228)
        cell.borderWidth = 0.75f
        cell.paddingTop = 4f
        cell.paddingBottom = 4f
        cell.verticalAlignment = Element.ALIGN_MIDDLE
        block(cell)

        this.addCell(cell)

        return cell
    }

    private fun PdfPTable.disclaimerCell(summary: String, block: PdfPCell.() -> Unit = {}): PdfPCell {
        val cell = PdfPCell(Paragraph(summary, style.disclaimerFont))
        cell.borderColor = Color(228, 228, 228)
        cell.borderWidth = 0.75f
        cell.paddingTop = 5f
        cell.paddingBottom = 5f
        cell.verticalAlignment = Element.ALIGN_MIDDLE
        block(cell)

        this.addCell(cell)

        return cell
    }

    private fun PdfPTable.signingInfoCell(signingInfo: String, block: PdfPCell.() -> Unit = {}): PdfPCell {
        val cell = PdfPCell(Paragraph(signingInfo, style.signingInfoFont))
        cell.borderColor = Color(228, 228, 228)
        cell.borderWidth = 0.75f
        block(cell)

        this.addCell(cell)

        return cell
    }

    private fun PdfPTable.signerTypeCell(signerType: String, block: PdfPCell.() -> Unit = {}): PdfPCell {
        val cell = PdfPCell(Paragraph(signerType, style.signerTypeFont))
        cell.borderColor = Color(228, 228, 228)
        cell.borderWidth = 0.75f
        block(cell)

        this.addCell(cell)

        return cell
    }

    private fun PdfPTable.signingPlaceHolderCell(placeHolder: String, block: PdfPCell.() -> Unit = {}): PdfPCell {
        val cell = PdfPCell(Paragraph(placeHolder, style.signerTypeFont))
        cell.borderColor = Color(228, 228, 228)
        cell.borderWidth = 0.75f
        block(cell)

        this.addCell(cell)

        return cell
    }

    private fun PdfPTable.caregivingPeriodCell(periodData: String, block: PdfPCell.() -> Unit = {}): PdfPCell {
        val cell = PdfPCell(Paragraph(periodData, style.caregivingPeriodFont))
        cell.borderColor = Color(228, 228, 228)
        cell.borderWidth = 0.75f
        block(cell)

        this.addCell(cell)

        return cell
    }

    data class CertificateData(
        val accidentNumber: String,
        val subscriptionDate: LocalDate,
        val patientName: String,
        val patientAge: Int,
        val patientSex: Sex,
        val patientPhoneNumber: String,
        val relationshipBetweenPatientAndPhoneOwner: String,
        val accidentDate: LocalDate,
        val patientDescription: String,
        val hospitalState: String?,
        val hospitalCity: String?,
        val hospitalAndRoom: String,
        val caregiverName: String,
        val caregiverBirthDate : String,
        val caregiverSex: Sex,
        val caregiverPhoneNumber: String,
        val caregivingOrganizationName: String,
        val caregivingOrganizationPhoneNumber: String,
        val caregivingStartDateTime: LocalDateTime,
        val caregivingEndDateTime: LocalDateTime,
        val remarks: String,
        val continueWithSameCaregiver: Boolean,
    )

    fun generate(certificateData: CertificateData): ByteArrayOutputStream {
        val document = Document()
        document.documentLanguage = "ko-KR"
        val pdfArray = ByteArrayOutputStream()

        val writer = PdfWriter.getInstance(document, pdfArray)

        val mertizCallCenterQRResource = ClassPathResource("/static/image/mertiz_call_center_qr.png")
        val mertizCallCenterQRPath = mertizCallCenterQRResource.url.toString()
        val mertizCallCenterQR = Image.getInstance(mertizCallCenterQRPath).apply {
            scalePercent(60f)
        }

        val caredocStampResource = ClassPathResource("/static/image/caredoc_stamp.png")
        val caredocStampPath = caredocStampResource.url.toString()
        val caredocStamp = Image.getInstance(caredocStampPath).apply {
            scalePercent(60f)
        }

        document.apply {
            open()

            add(generateSummaryTable())
            add(
                generateContentTable(
                    "기본사항",
                    generateBasicInfoContentTable(certificateData),
                )
            )
            add(
                generateContentTable(
                    "간병인 사용내역",
                    generateCaregiverUsageContentTable(certificateData),
                )
            )
            add(
                PdfPTable(1).apply {
                    widthPercentage = 93f
                    cautionCell("※ 사용기간 : 최초 사용일로부터 현재까지, 간병인사용 공백 기간 존재시 공백 기간 제외한") {
                        border = 0
                    }
                    cautionCell("                     실제 사용 기간 날짜, 시간까지 정확히 기재", critical = true) {
                        border = 0
                    }
                    contentCell("[중대사유로 인한 해지]") {
                        border = Rectangle.TOP + Rectangle.RIGHT + Rectangle.LEFT
                    }
                    disclaimerCell("회사는 아래와 같은 사실이 있는 경우에는 안 날부터 1개월 이내에 계약을 해지할 수 있습니다.") {
                        border = Rectangle.RIGHT + Rectangle.LEFT
                    }
                    nestedTable(2) {
                        setWidths(intArrayOf(6, 94))
                        disclaimerCell("①") {
                            border = Rectangle.LEFT
                        }
                        disclaimerCell("계약자, 피보험자 또는 보험수익자가 고의로 보험금 지급사유를 발생시킨 경우") {
                            border = Rectangle.RIGHT
                        }
                        disclaimerCell("②") {
                            border = Rectangle.LEFT + Rectangle.BOTTOM
                        }
                        disclaimerCell("계약자, 피보험자 또는 보험수익자가 보험금 청구에 관한 서류에 고의로 사실과 다른 것을 기재하였거나 그 서류 또는 증거를 위조 또는 변조한 경우 -- 이하 생략 --") {
                            border = Rectangle.RIGHT + Rectangle.BOTTOM
                        }
                    }
                }
            )
            add(
                PdfPTable(2).apply {
                    widthPercentage = 93f
                    setWidths(floatArrayOf(32.35f, 67.65f))
                    contentCell("") {
                        noBorder()
                    }
                    val todayString = Clock.today().format(DateTimeFormatter.ofPattern("yyyy년 M월 d일"))
                    nestedTable(4) {
                        setWidths(intArrayOf(41, 29, 18, 12))
                        signingInfoCell(todayString) {
                            border = Rectangle.BOTTOM
                        }
                        signerTypeCell("피보험자(보호자)") {
                            noBorder()
                        }
                        signingInfoCell(certificateData.patientName) {
                            noBorder()
                        }
                        signingPlaceHolderCell(certificateData.patientName) {
                            noBorder()
                        }
                        signingInfoCell(todayString) {
                            border = Rectangle.BOTTOM
                        }
                        signerTypeCell("간병인") {
                            noBorder()
                        }
                        signingInfoCell(certificateData.caregiverName) {
                            noBorder()
                        }
                        signingPlaceHolderCell(certificateData.caregiverName) {
                            noBorder()
                        }
                        signingInfoCell(certificateData.caregiverBirthDate) {
                            noBorder()
                        }
                        signingPlaceHolderCell(certificateData.caregiverBirthDate) {
                            noBorder()
                        }
                        signingInfoCell(todayString) {
                            border = Rectangle.BOTTOM
                        }
                        signerTypeCell("간병업체") {
                            noBorder()
                        }
                        signingInfoCell("㈜ 케어닥") {
                            noBorder()
                        }
                        signingPlaceHolderCell("(인)") {
                            noBorder()
                        }
                        contentCell("") {
                            noBorder()
                        }
                        contentCell("") {
                            noBorder()
                        }
                        disclaimerCell("(708-81-00933)") {
                            colspan = 2
                            noBorder()
                        }
                    }
                }
            )
            add(
                PdfPTable(2).apply {
                    widthPercentage = 93f
                    setWidths(intArrayOf(17, 83))
                    addCell(
                        PdfPCell(Paragraph("메리츠화재해상보험주식회사 귀중", style.receiverFont)).colspan(2).noBorder()
                            .apply {
                                paddingTop = 10f
                                paddingBottom = 10f
                                verticalAlignment = Element.ALIGN_MIDDLE
                            }
                    )
                    addCell(
                        PdfPCell(Paragraph("고객콜센터 1566-7711\nwww.meritzfire.com", style.cautionFont))
                            .noBorder()
                            .apply {
                                verticalAlignment = Element.ALIGN_BOTTOM
                            }
                    )
                    addCell(
                        PdfPCell(mertizCallCenterQR).apply {
                            border = Rectangle.BOTTOM
                            paddingTop = 10f
                            paddingBottom = 5f
                            verticalAlignment = Element.ALIGN_MIDDLE
                        }
                    )
                }
            )
            add(
                caredocStamp.apply {
                    scalePercent(50f)
                    setAbsolutePosition(486f, 245f)
                }
            )

            val directContent = writer.directContent

            directContent.saveState()
            directContent.setFontAndSize(style.contentFont.baseFont, 10f)

            directContent.beginText()
            directContent.moveText(510f, 285f)
            directContent.showText("인")
            directContent.endText()

            directContent.beginText()
            directContent.moveText(510f, 297f)
            directContent.showText("인")
            directContent.endText()

            directContent.restoreState()

            close()
        }

        return pdfArray
    }

    private fun generateSummaryTable(): PdfPTable {
        val meritzLogoResource = ClassPathResource("/static/image/meritz_insurance_logo.png")
        val meritzLogoPath = meritzLogoResource.url.toString()
        val meritzLogo = Image.getInstance(meritzLogoPath).apply {
            scalePercent(37f)
        }

        return PdfPTable(2).apply {
            widthPercentage = 93f
            setWidths(intArrayOf(78, 22))
            addCell(
                PdfPCell(
                    Paragraph("간병인사용 확인서", style.documentTitleFont)
                ).colspan(2).noBorder().apply {
                    verticalAlignment = Element.ALIGN_MIDDLE
                    fixedHeight = 50f
                }
            )
            summaryCell("메리츠화재의 간병인 사용비용 청구와 관련하여 아래와 같이 간병인 사용을 확인합니다.") {
                noBorder()
                verticalAlignment = Element.ALIGN_MIDDLE
                fixedHeight = 20f
            }
            imageCell(meritzLogo) {
                noBorder()
                verticalAlignment = Element.ALIGN_MIDDLE
            }
        }
    }

    private fun generateContentTable(name: String, content: PdfPTable): PdfPTable {
        val tableTitleMarkResource = ClassPathResource("/static/image/table_title_mark.png")
        val tableTitleMarkPath = tableTitleMarkResource.url.toString()
        val tableTitleMark = Image.getInstance(tableTitleMarkPath).apply {
            scalePercent(22f, 25f)
        }

        return PdfPTable(2).apply {
            widthPercentage = 93f
            setWidths(floatArrayOf(2f, 98f))
            imageCell(tableTitleMark) {
                border = PdfPCell.TOP + PdfPCell.BOTTOM
                borderColor = style.redLineColor
                borderWidth = 0.75f
                paddingTop = 2f
                paddingLeft = 4f
                fixedHeight = 20f
            }
            addCell(
                contentTitleCell(name).apply {
                    fixedHeight = 20f
                    verticalAlignment = Element.ALIGN_MIDDLE
                }
            )
            addCell(
                PdfPCell(content).colspan(2).noBorder()
            )
        }
    }

    private fun generateBasicInfoContentTable(certificateData: CertificateData): PdfPTable {
        return PdfPTable(1).apply {
            nestedTable(4) {
                setWidths(intArrayOf(13, 36, 13, 38))
                contentLabelCell("사고번호") { border = Rectangle.BOTTOM + Rectangle.RIGHT }
                contentCell(certificateData.accidentNumber) { noTopBorder() }
                contentLabelCell("청약일자") { noTopBorder() }
                contentCell(certificateData.subscriptionDate.format(DateTimeFormatter.ISO_DATE)) {
                    border = Rectangle.BOTTOM + Rectangle.LEFT
                }
            }
            nestedTable(5) {
                setWidths(intArrayOf(13, 36, 13, 27, 11))
                contentLabelCell("피보험자명") { border = Rectangle.BOTTOM + Rectangle.RIGHT + Rectangle.TOP }
                contentCell(certificateData.patientName)
                contentLabelCell("생년월일")
                contentCell(certificateData.patientAge.toString())
                contentCell(certificateData.patientSex.toKoreanString()) {
                    border = Rectangle.BOTTOM + Rectangle.LEFT + Rectangle.TOP
                }
            }
            nestedTable(3) {
                setWidths(intArrayOf(13, 36, 51))
                contentLabelCell("연락처") { border = Rectangle.BOTTOM + Rectangle.RIGHT + Rectangle.TOP }
                contentCell(certificateData.relationshipBetweenPatientAndPhoneOwner)
                contentCell(formatPhoneNumber(certificateData.patientPhoneNumber)) {
                    border = Rectangle.BOTTOM + Rectangle.LEFT + Rectangle.TOP
                }
            }
            nestedTable(2) {
                setWidths(intArrayOf(13, 87))
                contentLabelCell("주소") { border = Rectangle.BOTTOM + Rectangle.RIGHT + Rectangle.TOP }
                contentCell("") { border = Rectangle.BOTTOM + Rectangle.LEFT + Rectangle.TOP }
            }

            nestedTable(3) {
                setWidths(intArrayOf(13, 36, 51))
                contentLabelCell("사고(발병)일시") { border = Rectangle.BOTTOM + Rectangle.RIGHT + Rectangle.TOP }
                contentCell(certificateData.accidentDate.format(DateTimeFormatter.ISO_DATE))
                contentCell("상해 □      질병 □      교통사고 □      산재 □") {
                    border = Rectangle.BOTTOM + Rectangle.LEFT + Rectangle.TOP
                }
            }

            nestedTable(2) {
                setWidths(intArrayOf(13, 87))
                contentLabelCell("사고내용\n(필수기재)") { border = Rectangle.RIGHT + Rectangle.TOP }
                contentCell(certificateData.patientDescription) { border = Rectangle.LEFT + Rectangle.TOP }
            }
        }
    }

    private fun generateCaregiverUsageContentTable(certificateData: CertificateData): PdfPTable {
        return PdfPTable(1).apply {
            nestedTable(2) {
                setWidths(intArrayOf(49, 51))
                nestedTable(3) {
                    setWidths(floatArrayOf(26.53f, 39.5f, 33.97f))
                    contentLabelCell("병원명") { border = Rectangle.BOTTOM + Rectangle.RIGHT }
                    contentCell("${certificateData.hospitalState ?: ""} ${certificateData.hospitalCity ?: ""}") { noTopBorder() }
                    contentCell(certificateData.hospitalAndRoom) { noTopBorder() }
                }
                nestedTable(2) {
                    setWidths(floatArrayOf(25.49f, 74.51f))
                    contentLabelCell("간병인성명") { noTopBorder() }
                    contentCell(certificateData.caregiverName) { border = Rectangle.BOTTOM + Rectangle.LEFT }
                    contentLabelCell("간병인 생년월일")
                    /*contentCell("") {
                        border = Rectangle.BOTTOM + Rectangle.LEFT + Rectangle.TOP
                    }*/
                    contentCell(certificateData.caregiverBirthDate) { border = Rectangle.BOTTOM + Rectangle.LEFT + Rectangle.TOP }
                }
            }
            nestedTable(2) {
                setWidths(intArrayOf(49, 51))
                nestedTable(2) {
                    setWidths(floatArrayOf(26.53f, 73.47f))
                    contentLabelCell("추가근무시간") { border = Rectangle.BOTTOM + Rectangle.RIGHT + Rectangle.TOP }
                    contentCell("")
                }
                nestedTable(2) {
                    setWidths(floatArrayOf(25.49f, 74.51f))
                    contentLabelCell("간병인 연락처")
                    contentCell(formatPhoneNumber(certificateData.caregiverPhoneNumber)) {
                        border = Rectangle.BOTTOM + Rectangle.LEFT + Rectangle.TOP
                    }
                    contentLabelCell("간병 협회")
                    contentCell(certificateData.caregivingOrganizationName) {
                        border = Rectangle.BOTTOM + Rectangle.LEFT + Rectangle.TOP
                    }
                    contentLabelCell("간병 협회 전화번호")
                    contentCell(certificateData.caregivingOrganizationPhoneNumber) {
                        border = Rectangle.BOTTOM + Rectangle.LEFT + Rectangle.TOP
                    }
                }
            }
            nestedTable(2) {
                setWidths(intArrayOf(13, 87))
                nestedTable(1) {
                    contentLabelCell("사용기간") { border = Rectangle.BOTTOM + Rectangle.RIGHT + Rectangle.TOP }
                }
                nestedTable(6) {
                    setWidths(floatArrayOf(17.15f, 11f, 17.15f, 11f, 21.7f, 22f))
                    caregivingPeriodCell(
                        certificateData.caregivingStartDateTime.toLocalDate().format(DateTimeFormatter.ISO_DATE)
                    ) { border = Rectangle.BOTTOM + Rectangle.TOP + Rectangle.LEFT }
                    caregivingPeriodCell(
                        certificateData.caregivingStartDateTime.toLocalTime()
                            .format(DateTimeFormatter.ofPattern("HH:mm"))
                    ) { border = Rectangle.BOTTOM + Rectangle.TOP }
                    caregivingPeriodCell(
                        certificateData.caregivingEndDateTime.toLocalDate().format(DateTimeFormatter.ISO_DATE)
                    ) { border = Rectangle.BOTTOM + Rectangle.TOP }
                    caregivingPeriodCell(
                        certificateData.caregivingEndDateTime.toLocalTime()
                            .format(DateTimeFormatter.ofPattern("HH:mm"))
                    ) { border = Rectangle.BOTTOM + Rectangle.TOP }
                    caregivingPeriodCell("(    차)") { border = Rectangle.BOTTOM + Rectangle.TOP + Rectangle.RIGHT }
                    caregivingPeriodCell("") { border = Rectangle.BOTTOM + Rectangle.LEFT + Rectangle.TOP }
                    caregivingPeriodCell("") { border = Rectangle.BOTTOM + Rectangle.TOP + Rectangle.LEFT }
                    caregivingPeriodCell("") { border = Rectangle.BOTTOM + Rectangle.TOP }
                    caregivingPeriodCell("") { border = Rectangle.BOTTOM + Rectangle.TOP }
                    caregivingPeriodCell("") { border = Rectangle.BOTTOM + Rectangle.TOP }
                    caregivingPeriodCell("(    차)") { border = Rectangle.BOTTOM + Rectangle.TOP + Rectangle.RIGHT }
                    caregivingPeriodCell("") { border = Rectangle.BOTTOM + Rectangle.LEFT + Rectangle.TOP }
                    caregivingPeriodCell("") { border = Rectangle.BOTTOM + Rectangle.TOP + Rectangle.LEFT }
                    caregivingPeriodCell("") { border = Rectangle.BOTTOM + Rectangle.TOP }
                    caregivingPeriodCell("") { border = Rectangle.BOTTOM + Rectangle.TOP }
                    caregivingPeriodCell("") { border = Rectangle.BOTTOM + Rectangle.TOP }
                    caregivingPeriodCell("(    차)") { border = Rectangle.BOTTOM + Rectangle.TOP + Rectangle.RIGHT }
                    caregivingPeriodCell("") { border = Rectangle.BOTTOM + Rectangle.LEFT + Rectangle.TOP }
                }
            }
            nestedTable(4) {
                setWidths(intArrayOf(13, 58, 19, 10))
                contentLabelCell("특이사항") { border = Rectangle.BOTTOM + Rectangle.RIGHT + Rectangle.TOP }
                caregivingPeriodCell(certificateData.remarks)
                contentLabelCell("간병인 계속사용\n(계속 사용시 체크)\n")
                checkBoxCell(certificateData.continueWithSameCaregiver) { border = Rectangle.BOTTOM + Rectangle.LEFT + Rectangle.TOP }
            }
        }
    }

    private fun PdfPCell.colspan(colSpan: Int) = this.apply { colspan = colSpan }

    private fun PdfPCell.noBorder() = this.apply { border = 0 }

    private fun PdfPCell.noTopBorder() = this.apply { border = border.and(Rectangle.TOP.inv()) }

    private fun Sex.toKoreanString() = when (this) {
        Sex.MALE -> "남"
        Sex.FEMALE -> "여"
    }

    private fun formatPhoneNumber(rawPhoneNumber: String): String =
        phoneNumberPattern.matchEntire(rawPhoneNumber)?.groups?.let {
            val first = it[1]?.value ?: return@let null
            val second = it[2]?.value ?: return@let null
            val third = it[3]?.value ?: return@let null

            "$first-$second-$third"
        } ?: rawPhoneNumber
}
