/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.dekd.shared.util

import org.readium.dekd.shared.publication.asset.FileAsset

@Deprecated("Renamed into `FileAsset`", ReplaceWith("FileAsset"), level = DeprecationLevel.ERROR)
typealias File = FileAsset
