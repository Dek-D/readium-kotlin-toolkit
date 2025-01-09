/*
 * Module: r2-navigator-kotlin
 * Developers: Taehyun Kim, Seongjin Kim
 *
 * Copyright (c) 2019. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.dekd.navigator.epub

import org.readium.dekd.shared.publication.Locator

interface IR2Selectable {
    fun currentSelection(callback: (Locator?) -> Unit)
}