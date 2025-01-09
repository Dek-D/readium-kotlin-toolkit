/*
 * Module: r2-lcp-kotlin
 * Developers: Aferdita Muriqi
 *
 * Copyright (c) 2019. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readiumd.dekd.lcp.service

import org.readiumd.dekd.lcp.LcpException
import org.readiumd.dekd.lcp.license.model.LicenseDocument
import org.readiumd.dekd.lcp.persistence.LcpDao

internal class DeviceRepository(private val lcpDao: LcpDao) {

    suspend fun isDeviceRegistered(license: LicenseDocument): Boolean {
        if (lcpDao.exists(license.id) == null) {
            throw LcpException.Runtime("The LCP License doesn't exist in the database")
        }
        return lcpDao.isDeviceRegistered(license.id)
    }

    suspend fun registerDevice(license: LicenseDocument) {
        if (lcpDao.exists(license.id) == null) {
            throw LcpException.Runtime("The LCP License doesn't exist in the database")
        }
        lcpDao.registerDevice(license.id)
    }
}
