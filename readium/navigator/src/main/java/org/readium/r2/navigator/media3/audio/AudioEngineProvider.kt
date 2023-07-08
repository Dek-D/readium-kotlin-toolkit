/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.audio

import org.readium.r2.navigator.preferences.Configurable
import org.readium.r2.navigator.preferences.PreferencesEditor
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.Publication

/**
 * To be implemented by adapters for third-party audio engines which can be used with [AudioNavigator].
 */
@ExperimentalReadiumApi
interface AudioEngineProvider<S : Configurable.Settings, P : Configurable.Preferences<P>,
    E : PreferencesEditor<P>> {

    suspend fun createEngine(
        publication: Publication,
        initialLocator: Locator,
        initialPreferences: P
    ): AudioEngine<S, P>?

    /**
     * Creates settings for [metadata] and [preferences].
     */
    fun computeSettings(metadata: Metadata, preferences: P): S

    /**
     * Creates a preferences editor for [publication] and [initialPreferences].
     */
    fun createPreferenceEditor(publication: Publication, initialPreferences: P): E

    /**
     * Creates an empty set of preferences of this TTS engine provider.
     */
    fun createEmptyPreferences(): P
}
