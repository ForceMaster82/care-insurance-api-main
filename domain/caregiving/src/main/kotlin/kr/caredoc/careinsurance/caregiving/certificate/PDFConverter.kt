package kr.caredoc.careinsurance.caregiving.certificate

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.PDFRenderer
import java.io.ByteArrayOutputStream
import java.io.InputStream
import javax.imageio.ImageIO

object PDFConverter {
    fun convertJPG(
        pdfStream: InputStream,
        pageIndex: Int = 0,
        dpi: Float = 120.0f,
    ): ByteArrayOutputStream {
        return PDDocument.load(pdfStream).use {
            val renderer = PDFRenderer(it)

            ByteArrayOutputStream().apply {
                ImageIO.write(
                    renderer.renderImageWithDPI(pageIndex, dpi),
                    "jpeg",
                    this
                )
            }
        }
    }
}
