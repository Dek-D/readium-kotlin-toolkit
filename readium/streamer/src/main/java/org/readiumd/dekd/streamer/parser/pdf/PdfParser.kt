/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readiumd.dekd.streamer.parser.pdf

import android.content.Context
import kotlinx.coroutines.runBlocking
import org.readiumd.dekd.shared.PdfSupport
import org.readiumd.dekd.shared.fetcher.Fetcher
import org.readiumd.dekd.shared.fetcher.FileFetcher
import org.readiumd.dekd.shared.publication.*
import org.readiumd.dekd.shared.publication.asset.FileAsset
import org.readiumd.dekd.shared.publication.asset.PublicationAsset
import org.readiumd.dekd.shared.publication.services.InMemoryCoverService
import org.readiumd.dekd.shared.util.logging.WarningLogger
import org.readiumd.dekd.shared.util.mediatype.MediaType
import org.readiumd.dekd.shared.util.pdf.PdfDocumentFactory
import org.readiumd.dekd.shared.util.pdf.toLinks
import org.readiumd.dekd.streamer.DefaultPdfDocumentFactory
import org.readiumd.dekd.streamer.PublicationParser
import org.readiumd.dekd.streamer.container.PublicationContainer
import org.readiumd.dekd.streamer.parser.PubBox
import java.io.File

/**
 * Parses a PDF file into a Readium [Publication].
 */
@PdfSupport
class PdfParser(
    context: Context,
    private val pdfFactory: PdfDocumentFactory = DefaultPdfDocumentFactory(context)
) : PublicationParser, org.readiumd.dekd.streamer.parser.PublicationParser {

    override suspend fun parse(asset: PublicationAsset, fetcher: Fetcher, warnings: WarningLogger?): Publication.Builder? =
        _parse(asset, fetcher, asset.name)

    suspend fun _parse(asset: PublicationAsset, fetcher: Fetcher, fallbackTitle: String): Publication.Builder? {
        if (asset.mediaType() != MediaType.PDF)
            return null

        val fileHref = fetcher.links().firstOrNull { it.mediaType == MediaType.PDF }?.href
            ?: throw Exception("Unable to find PDF file.")
        val document = pdfFactory.open(fetcher.get(fileHref), password = null)
        val tableOfContents = document.outline.toLinks(fileHref)

        val manifest = Manifest(
            metadata = Metadata(
                identifier = document.identifier,
                conformsTo = setOf(Publication.Profile.PDF),
                localizedTitle = LocalizedString(document.title?.ifBlank { null } ?: fallbackTitle),
                authors = listOfNotNull(document.author).map { Contributor(name = it) },
                numberOfPages = document.pageCount
            ),
            readingOrder = listOf(Link(href = fileHref, type = MediaType.PDF.toString())),
            tableOfContents = tableOfContents
        )

        val servicesBuilder = Publication.ServicesBuilder(
            positions = PdfPositionsService.Companion::create,
            cover = document.cover?.let { InMemoryCoverService.createFactory(it) }
        )

        return Publication.Builder(manifest, fetcher, servicesBuilder)
    }

    override fun parse(fileAtPath: String, fallbackTitle: String): PubBox? = runBlocking {

        val file = File(fileAtPath)
        val asset = FileAsset(file)
        val baseFetcher = FileFetcher(href = "/${file.name}", file = file)
        val builder = try {
            _parse(asset, baseFetcher, fallbackTitle)
        } catch (e: Exception) {
            return@runBlocking null
        } ?: return@runBlocking null

        val publication = builder.build()
        val container = PublicationContainer(
            publication = publication,
            path = file.canonicalPath,
            mediaType = MediaType.PDF
        )

        PubBox(publication, container)
    }

}
