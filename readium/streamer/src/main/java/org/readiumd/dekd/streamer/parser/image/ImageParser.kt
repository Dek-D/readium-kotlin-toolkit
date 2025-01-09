/*
 * Module: r2-streamer-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readiumd.dekd.streamer.parser.image

import org.readiumd.dekd.shared.fetcher.Fetcher
import org.readiumd.dekd.shared.publication.*
import org.readiumd.dekd.shared.publication.asset.PublicationAsset
import org.readiumd.dekd.shared.publication.services.PerResourcePositionsService
import org.readiumd.dekd.shared.util.logging.WarningLogger
import org.readiumd.dekd.shared.util.mediatype.MediaType
import org.readiumd.dekd.streamer.PublicationParser
import org.readiumd.dekd.streamer.extensions.guessTitle
import org.readiumd.dekd.streamer.extensions.isHiddenOrThumbs
import org.readiumd.dekd.streamer.extensions.lowercasedExtension
import java.io.File

/**
 * Parses an image–based Publication from an unstructured archive format containing bitmap files,
 * such as CBZ or a simple ZIP.
 *
 * It can also work for a standalone bitmap file.
 */
class ImageParser : PublicationParser {

    override suspend fun parse(
        asset: PublicationAsset,
        fetcher: Fetcher,
        warnings: WarningLogger?
    ): Publication.Builder? {

        if (!accepts(asset, fetcher))
            return null

        val readingOrder = fetcher.links()
            .filter { !File(it.href).isHiddenOrThumbs && it.mediaType.isBitmap }
            .sortedBy(Link::href)
            .toMutableList()

        if (readingOrder.isEmpty())
            throw Exception("No bitmap found in the publication.")

        val title = fetcher.guessTitle() ?: asset.name

        // First valid resource is the cover.
        readingOrder[0] = readingOrder[0].copy(rels = setOf("cover"))

        val manifest = Manifest(
            metadata = Metadata(
                conformsTo = setOf(Publication.Profile.DIVINA),
                localizedTitle = LocalizedString(title)
            ),
            readingOrder = readingOrder
        )

        return Publication.Builder(
            manifest = manifest,
            fetcher = fetcher,
            servicesBuilder = Publication.ServicesBuilder(
                positions = PerResourcePositionsService.createFactory(fallbackMediaType = "image/*")
            )
        )
    }

    private suspend fun accepts(asset: PublicationAsset, fetcher: Fetcher): Boolean {
        if (asset.mediaType() == MediaType.CBZ)
            return true

        val allowedExtensions = listOf("acbf", "txt", "xml")

        if (fetcher.links()
                .filterNot { File(it.href).isHiddenOrThumbs }
                .all { it.mediaType.isBitmap || File(it.href).lowercasedExtension in allowedExtensions })
            return true

        return false
    }
}
