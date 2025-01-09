/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.dekd.shared.publication.asset

import org.readium.dekd.shared.fetcher.Fetcher
import org.readium.dekd.shared.publication.Publication
import org.readium.dekd.shared.util.Try
import org.readium.dekd.shared.util.archive.ArchiveFactory
import org.readium.dekd.shared.util.mediatype.MediaType

/** Represents a digital medium (e.g. a file) offering access to a publication. */
interface PublicationAsset {

    /** Name of the asset, e.g. a filename. */
    val name: String

    /**
     * Media type of the asset.
     *
     * If unknown, fallback on `MediaType.BINARY`.
     */
    suspend fun mediaType(): MediaType

    /**
     * Creates a fetcher used to access the asset's content.
     */
    suspend fun createFetcher(dependencies: Dependencies, credentials: String?): Try<Fetcher, Publication.OpeningException>

    data class Dependencies(val archiveFactory: ArchiveFactory)

}
