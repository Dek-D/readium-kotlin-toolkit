/*
 * Module: r2-streamer-kotlin
 * Developers: Aferdita Muriqi, Irteza Sheikh
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readiumd.dekd.streamer.parser.audio

import kotlinx.coroutines.runBlocking
import org.readiumd.dekd.shared.fetcher.Fetcher
import org.readiumd.dekd.shared.util.mediatype.MediaType
import org.readiumd.dekd.shared.publication.Manifest
import org.readiumd.dekd.shared.publication.Publication
import org.readiumd.dekd.streamer.container.ContainerError
import org.readiumd.dekd.streamer.container.PublicationContainer
import org.readiumd.dekd.streamer.extensions.fromArchiveOrDirectory
import org.readiumd.dekd.streamer.extensions.readAsJsonOrNull
import org.readiumd.dekd.streamer.parser.PubBox
import org.readiumd.dekd.streamer.parser.PublicationParser


class AudioBookConstant {
    companion object {
        @Deprecated("Use [MediaType.AUDIOBOOK.toString()] instead", replaceWith = ReplaceWith("MediaType.AUDIOBOOK.toString()"))
        val mimetype get() = MediaType.READIUM_AUDIOBOOK.toString()
    }
}

/**
 *      AudiobookParser : Handle any Audiobook Package file. Opening, listing files
 *                  get name of the resource, creating the Publication
 *                  for rendering
 */
class AudioBookParser : PublicationParser {

    /**
     * This functions parse a manifest.json and build PubBox object from it
     */
    override fun parse(fileAtPath: String, fallbackTitle: String): PubBox? = runBlocking {
        _parse(fileAtPath)
    }

    private suspend fun _parse(fileAtPath: String): PubBox? {
        val fetcher = Fetcher.fromArchiveOrDirectory(fileAtPath)
            ?: throw ContainerError.missingFile(fileAtPath)

        val manifest = fetcher.readAsJsonOrNull("manifest.json")
            ?.let { Manifest.fromJSON(it, packaged = true) }
            ?: return null

        val publication = Publication(
            manifest = manifest
        ).apply {
            @Suppress("DEPRECATION")
            type = Publication.TYPE.AUDIO
        }

        val container = PublicationContainer(
            publication =  publication,
            path = fileAtPath,
            mediaType = MediaType.READIUM_AUDIOBOOK
        )

        return PubBox(publication, container)
    }
}
