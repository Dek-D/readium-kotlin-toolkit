package org.readiumd.dekd.testapp.utils.extensions

import org.readiumd.dekd.shared.publication.Link

val Link.outlineTitle: String
    get() = title ?: href
