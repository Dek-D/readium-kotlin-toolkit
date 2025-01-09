/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readiumd.dekd.shared.publication.opds

import org.junit.Assert.*
import org.junit.Test
import org.readiumd.dekd.shared.publication.LocalizedString
import org.readiumd.dekd.shared.publication.Publication
import org.readiumd.dekd.shared.publication.PublicationCollection
import org.readiumd.dekd.shared.publication.Link
import org.readiumd.dekd.shared.publication.Manifest
import org.readiumd.dekd.shared.publication.Metadata

class PublicationTest {

    private fun createPublication(
        subCollections: Map<String, List<PublicationCollection>> = emptyMap()
    ) = Publication(
            Manifest(
                metadata = Metadata(localizedTitle = LocalizedString("Title")),
                subcollections = subCollections
            )
    )

    @Test fun `get {images}`() {
        val links = listOf(Link(href = "/image.png"))
        assertEquals(
            links,
            createPublication(subCollections = mapOf(
                "images" to listOf(PublicationCollection(links = links))
            )).images
        )
    }

    @Test fun `get {images} when missing`() {
        assertEquals(0, createPublication().images.size)
    }

}
