/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readiumd.dekd.navigator.media.extensions

import android.support.v4.media.session.MediaControllerCompat
import org.readiumd.dekd.navigator.ExperimentalAudiobook
import org.readiumd.dekd.navigator.media.MediaService
import org.readiumd.dekd.shared.publication.PublicationId

@ExperimentalAudiobook
internal val MediaControllerCompat.publicationId: PublicationId?
    get() = extras?.getString(MediaService.EXTRA_PUBLICATION_ID)
