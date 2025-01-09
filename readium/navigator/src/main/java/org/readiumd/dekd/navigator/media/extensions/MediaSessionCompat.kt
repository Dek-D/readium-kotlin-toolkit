/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readiumd.dekd.navigator.media.extensions

import android.os.Bundle
import android.support.v4.media.session.MediaSessionCompat
import org.readiumd.dekd.navigator.ExperimentalAudiobook
import org.readiumd.dekd.navigator.media.MediaService
import org.readiumd.dekd.shared.publication.PublicationId

@ExperimentalAudiobook
internal var MediaSessionCompat.publicationId: PublicationId?
    get() = controller.publicationId
    set(value) {
        val extras = Bundle(controller.extras ?: Bundle())
        setExtras(extras.apply {
            putString(MediaService.EXTRA_PUBLICATION_ID, value)
        })
        sendSessionEvent(MediaService.EVENT_PUBLICATION_CHANGED, Bundle().apply {
            putString(MediaService.EXTRA_PUBLICATION_ID, value)
        })
    }