/*
 * Module: r2-lcp-kotlin
 * Developers: Aferdita Muriqi
 *
 * Copyright (c) 2019. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.dekd.lcp.license.model.components.lsd

import org.json.JSONObject
import org.readium.dekd.shared.extensions.iso8601ToDate
import org.readium.dekd.shared.extensions.optNullableString
import java.util.*

data class PotentialRights(val json: JSONObject) {
    val end: Date? = json.optNullableString("end")?.iso8601ToDate()
}
