package org.readiumd.dekd.navigator

import org.readiumd.dekd.shared.publication.LocalizedString
import org.readiumd.dekd.shared.publication.Manifest
import org.readiumd.dekd.shared.publication.Metadata
import org.readiumd.dekd.shared.publication.Publication

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