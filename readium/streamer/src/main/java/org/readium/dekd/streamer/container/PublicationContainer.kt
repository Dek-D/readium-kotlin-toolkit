/*
 * Module: r2-streamer-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.dekd.streamer.container

import kotlinx.coroutines.runBlocking
import org.readium.dekd.shared.RootFile
import org.readium.dekd.shared.drm.DRM
import org.readium.dekd.shared.extensions.tryOr
import org.readium.dekd.shared.fetcher.ResourceInputStream
import org.readium.dekd.shared.util.mediatype.MediaType
import org.readium.dekd.shared.publication.Link
import org.readium.dekd.shared.publication.Publication
import java.io.InputStream

/**
 * Temporary solution to migrate to [Publication.get] while ensuring backward compatibility with
 * [Container].
 */
internal class PublicationContainer(
    private val publication: Publication,
    path: String,
    mediaType: MediaType,
    override var drm: DRM? = null
) : Container {

    override var rootFile = RootFile(rootPath = path, mimetype = mediaType.toString())

    override fun data(relativePath: String): ByteArray = runBlocking {
        publication.get(relativePath).read().getOrThrow()
    }

    override fun dataLength(relativePath: String): Long = runBlocking {
        tryOr(0) {
            publication.get(relativePath).length().getOrThrow()
        }
    }

    override fun dataInputStream(relativePath: String): InputStream =
        ResourceInputStream(publication.get(relativePath)).buffered()

    private fun Publication.get(href: String) = get(Link(href))
}
