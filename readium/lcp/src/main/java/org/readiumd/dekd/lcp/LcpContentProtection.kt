/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readiumd.dekd.lcp

import org.readiumd.dekd.lcp.auth.LcpPassphraseAuthentication
import org.readiumd.dekd.shared.fetcher.Fetcher
import org.readiumd.dekd.shared.fetcher.TransformingFetcher
import org.readiumd.dekd.shared.publication.ContentProtection
import org.readiumd.dekd.shared.publication.Publication
import org.readiumd.dekd.shared.publication.asset.FileAsset
import org.readiumd.dekd.shared.publication.asset.PublicationAsset
import org.readiumd.dekd.shared.publication.services.contentProtectionServiceFactory
import org.readiumd.dekd.shared.util.Try

internal class LcpContentProtection(
    private val lcpService: LcpService,
    private val authentication: LcpAuthenticating
) : ContentProtection {

    override suspend fun open(
        asset: PublicationAsset,
        fetcher: Fetcher,
        credentials: String?,
        allowUserInteraction: Boolean,
        sender: Any?
    ): Try<ContentProtection.ProtectedAsset, Publication.OpeningException>? {
        if (asset !is FileAsset) {
            return null
        }

        if (!lcpService.isLcpProtected(asset.file)) {
            return null
        }

        val authentication = credentials?.let { LcpPassphraseAuthentication(it, fallback = this.authentication) }
            ?: this.authentication

        val license = lcpService
            .retrieveLicense(asset.file,  authentication, allowUserInteraction, sender)

        val serviceFactory = LcpContentProtectionService
            .createFactory(license?.getOrNull(), license?.exceptionOrNull())

        val protectedFile = ContentProtection.ProtectedAsset(
            asset = asset,
            fetcher = TransformingFetcher(fetcher, LcpDecryptor(license?.getOrNull())::transform),
            onCreatePublication = {
                servicesBuilder.contentProtectionServiceFactory = serviceFactory
            }
        )

        return Try.success(protectedFile)
    }

}
