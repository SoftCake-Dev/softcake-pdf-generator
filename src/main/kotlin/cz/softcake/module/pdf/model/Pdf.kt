package cz.softcake.module.pdf.model

import cz.softcake.module.pdf.extensions.*
import cz.softcake.module.pdf.reader.FileReader
import org.apache.pdfbox.pdmodel.PDDocument
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.net.URISyntaxException

fun JSONObject.toPdf(): Pdf {
    val pageType = this.getOrNull<String>("pageType")
    return Pdf(
            pages = this.getOrNull<JSONArray>("page")?.map { it.cast<JSONObject>().toPage(pageType) }?.toMutableList()
                    ?: this.getOrNull<JSONObject>("page")?.toPage(pageType)?.let { mutableListOf(it) }
                    ?: mutableListOf()
    )
}

class Pdf(
        private val pages: MutableList<Page> = mutableListOf(),
) {

    val document: PDDocument = PDDocument()

    companion object {

        @JvmStatic
        @Throws(IOException::class, URISyntaxException::class)
        fun fromString(xml: String) = this.fromString(xml, true)

        @JvmStatic
        @Throws(IOException::class, URISyntaxException::class)
        fun fromString(xml: String, preCalculate: Boolean): Pdf {
            return xml.replaceXmlTags()
                    .parseJsonFromXml()
                    .getOrThrow<JSONObject>("pdf")
                    .toPdf().also { if(preCalculate) it.preCalculate() }
        }

        @JvmStatic
        @Throws(IOException::class, URISyntaxException::class)
        fun fromResource(path: String) = this.fromResource(path, true)

        @JvmStatic
        @Throws(IOException::class, URISyntaxException::class)
        fun fromResource(path: String, preCalculate: Boolean) = this.fromString(FileReader.readJsonFromXmlResource(path), preCalculate)
    }

    init {
        pages.forEach {
            it.parent = this
        }
    }

    fun preCalculate() {
        pages.forEach { it.preCalculate() }
    }

    fun addPage(absolutePage: AbsolutePage) {
        absolutePage.parent = this
        pages.add(absolutePage)
    }

    fun addAllPages(pages: List<AbsolutePage>) {
        pages.forEach { addPage(it) }
    }

    fun findElementById(id: String): Element? {
        return pages.mapNotNull { it.findElementById(id) }
                .firstOrNull()
    }

    fun copy(): Pdf {
        return Pdf(
                pages = pages.map { it.copy() }.toMutableList()
        )
    }

    @Throws(IOException::class)
    private fun draw() {
        pages.forEach { it.draw() }
    }

    @Throws(IOException::class)
    fun save(path: String) {
        this.draw()
        document.apply {
            save(path)
            close()
        }
    }

    @Throws(IOException::class)
    fun save(outputStream: OutputStream) {
        this.draw()
        document.apply {
            save(outputStream)
            close()
        }
    }

}