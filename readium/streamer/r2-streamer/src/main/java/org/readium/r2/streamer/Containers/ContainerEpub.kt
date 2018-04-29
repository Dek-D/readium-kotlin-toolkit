package org.readium.r2.streamer.Containers

import org.readium.r2.shared.drm.Drm
import java.io.File
import java.util.zip.ZipFile
import org.readium.r2.shared.Link
import org.readium.r2.shared.RootFile
import org.readium.r2.shared.XmlParser.XmlParser
import org.readium.r2.streamer.Parser.mimetype

class ContainerEpub : EpubContainer, ZipArchiveContainer {

    override fun xmlDocumentforFile(relativePath: String): XmlParser {
        val containerData = data(relativePath)
        val document = XmlParser()
        document.parseXml(containerData.inputStream())
        return document
    }

    override fun xmlDocumentforResource(link: Link?): XmlParser {
        var pathFile = link?.href ?: throw Exception("Missing Link : ${link?.title}")
        if (pathFile.first() == '/')
            pathFile = pathFile.substring(1)
        return xmlDocumentforFile(pathFile)
    }

    override var rootFile: RootFile
    override var zipFile: ZipFile
    override var drm: Drm? = null
    override var successCreated: Boolean = false

    constructor(path: String) {

        if (File(path).exists()) {
            successCreated = true
        }
        zipFile = ZipFile(path)
        rootFile = RootFile(path, mimetype)
    }

    override fun data(relativePath: String): ByteArray {
        return super.data(relativePath)
    }

}