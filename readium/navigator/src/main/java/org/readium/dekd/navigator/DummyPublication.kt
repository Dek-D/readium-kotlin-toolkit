package org.readium.dekd.navigator

import org.readium.dekd.shared.publication.LocalizedString
import org.readium.dekd.shared.publication.Manifest
import org.readium.dekd.shared.publication.Metadata
import org.readium.dekd.shared.publication.Publication

object RestorationNotSupportedException : Exception(
    "Restoration of the navigator fragment after process death is not supported. You must pop it from the back stack or finish the host Activity before `onResume`."
)

internal val dummyPublication = Publication(
    Manifest(
        metadata = Metadata(
            identifier = "readium:dummy",
            localizedTitle = LocalizedString("")
        )
    )
)