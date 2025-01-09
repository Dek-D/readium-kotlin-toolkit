/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */
package org.readiumd.dekd.shared.publication.presentation

import org.json.JSONObject
import org.readiumd.dekd.shared.publication.Metadata

// Presentation extensions for [Metadata]

val Metadata.presentation: Presentation
    get() = Presentation.fromJSON(
        (this["presentation"] as? Map<*, *>)
            ?.let { JSONObject(it) }
    )
