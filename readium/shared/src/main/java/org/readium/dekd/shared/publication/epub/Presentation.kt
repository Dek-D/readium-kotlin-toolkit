/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.dekd.shared.publication.epub

import org.readium.dekd.shared.publication.Link
import org.readium.dekd.shared.publication.presentation.Presentation

/**
 * Get the layout of the given resource in this publication.
 * Falls back on REFLOWABLE.
 */
fun Presentation.layoutOf(link: Link): EpubLayout {
    return link.properties.layout
        ?: layout
        ?: EpubLayout.REFLOWABLE
}
