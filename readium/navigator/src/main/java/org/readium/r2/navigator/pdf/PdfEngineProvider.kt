/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.pdf

import android.graphics.PointF
import androidx.fragment.app.Fragment
import org.readium.r2.navigator.VisualNavigator
import org.readium.r2.navigator.settings.Configurable
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.fetcher.Resource
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.Publication

/**
 * To be implemented by adapters for third-party PDF engines which can be used with [PdfNavigatorFragment].
 */
@ExperimentalReadiumApi
interface PdfEngineProvider<S: Configurable.Settings, P: Configurable.Preferences> {

    /**
     * Creates a [PdfDocumentFragment] for [input].
     */
    suspend fun createDocumentFragment(input: PdfDocumentFragmentInput<S>): PdfDocumentFragment<S>

    /**
     * Creates [Configurable.Settings] for [metadata] and [preferences].
     */
    fun createSettings(metadata: Metadata, preferences: P): S


    fun createPreferenceEditor(metadata)

    /**
     * Infers a [VisualNavigator.Presentation] from settings.
     */
    fun createPresentation(settings: S): VisualNavigator.Presentation
}

@ExperimentalReadiumApi
typealias PdfDocumentFragmentFactory<S> = suspend (PdfDocumentFragmentInput<S>) -> PdfDocumentFragment<S>

/**
 * A [PdfDocumentFragment] renders a single PDF resource.
 */
@ExperimentalReadiumApi
abstract class PdfDocumentFragment<S: Configurable.Settings> : Fragment() {

    interface Listener {
        /**
         * Called when the fragment navigates to a different page.
         */
        fun onPageChanged(pageIndex: Int)

        /**
         * Called when the user triggers a tap on the document.
         */
        fun onTap(point: PointF): Boolean

        /**
         * Called when the PDF engine fails to load the PDF document.
         */
        fun onResourceLoadFailed(link: Link, error: Resource.Exception)
    }

    /**
     * Returns the current page index in the document, from 0.
     */
    abstract val pageIndex: Int

    /**
     * Jumps to the given page [index].
     *
     * @param animated Indicates if the transition should be animated.
     * @return Whether the jump is valid.
     */
    abstract fun goToPageIndex(index: Int, animated: Boolean): Boolean

    /**
     * Current settings for the PDF document.
     */
    abstract var settings: S
}

@ExperimentalReadiumApi
data class PdfDocumentFragmentInput<S: Configurable.Settings>(
    val publication: Publication,
    val link: Link,
    val initialPageIndex: Int,
    val settings: S,
    val listener: PdfDocumentFragment.Listener?
)

@ExperimentalReadiumApi
interface PdfSettingsValues : Configurable.Settings