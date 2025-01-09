package org.readium.dekd.testapp.utils.extensions

import org.readium.dekd.shared.publication.Link

val Link.outlineTitle: String
    get() = title ?: href
