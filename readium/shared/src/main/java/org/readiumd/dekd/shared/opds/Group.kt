/*
 * Module: r2-shared-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readiumd.dekd.shared.opds

import org.readiumd.dekd.shared.publication.Link
import org.readiumd.dekd.shared.publication.Publication

data class Group(val title: String) {
    var metadata: OpdsMetadata = OpdsMetadata(title = title)
    var links = mutableListOf<Link>()
    var publications = mutableListOf<Publication>()
    var navigation = mutableListOf<Link>()

}
