/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readiumd.dekd.navigator.media

import org.readiumd.dekd.navigator.ExperimentalAudiobook
import org.readiumd.dekd.shared.publication.Locator
import org.readiumd.dekd.shared.publication.Publication
import org.readiumd.dekd.shared.publication.PublicationId

/**
 * Holds information about a media-based [publication] waiting to be rendered by a [MediaPlayer].
 */
@ExperimentalAudiobook
data class PendingMedia(
    val publication: Publication,
    val publicationId: PublicationId,
    val locator: Locator
)
