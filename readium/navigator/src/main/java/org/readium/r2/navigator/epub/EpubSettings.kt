/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.r2.navigator.epub

import android.content.Context
import android.content.SharedPreferences
import org.readium.r2.navigator.epub.css.*
import org.readium.r2.navigator.epub.css.Layout
import org.readium.r2.navigator.epub.css.ReadiumCss
import org.readium.r2.navigator.preferences.*
import org.readium.r2.navigator.preferences.Color
import org.readium.r2.navigator.preferences.TextAlign
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.ReadingProgression
import org.readium.r2.shared.util.Either
import org.readium.r2.shared.util.Language

/**
 * EPUB navigator settings values.
 *
 * @param language Language of the publication content.
 * @param readingProgression Direction of the reading progression across resources.
 * @param spread Indicates if the publication should be rendered with a
 * synthetic spread (dual-page).
 * @param backgroundColor Default page background color.
 * @param columnCount Number of columns to display (one-page view or two-page spread).
 * @param fontFamily Default typeface for the text.
 * @param fontSize Base text font size.
 * @param hyphens Enable hyphenation.
 * @param imageFilter Filter applied to images in dark theme.
 * @param language Language of the publication content.
 * @param letterSpacing Space between letters.
 * @param ligatures Enable ligatures in Arabic.
 * @param lineHeight Leading line height.
 * @param pageMargins Factor applied to horizontal margins.
 * @param paragraphIndent Text indentation for paragraphs.
 * @param paragraphSpacing Vertical margins for paragraphs.
 * @param publisherStyles Indicates whether the original publisher styles should be observed.
 * Many settings require this to be off.
 * @param readingProgression Direction of the reading progression across resources.
 * @param scroll Indicates if the overflow of resources should be handled using scrolling
 * instead of synthetic pagination.
 * @param textAlign Page text alignment.
 * @param textColor Default page text color.
 * @param textNormalization Normalize font style, weight and variants using a specific strategy.
 * @param theme Reader theme.
 * @param typeScale Scale applied to all element font sizes.
 * @param verticalText Indicates whether the text should be laid out vertically. This is used
 * for example with CJK languages. This setting is automatically derived from the language if
 * no preference is given.
 * @param wordSpacing Space between words.
 */
@ExperimentalReadiumApi
data class EpubSettings(
    val language: Language?,
    val readingProgression: ReadingProgression,
    val spread: Spread,
    val backgroundColor: Color,
    val columnCount: ColumnCount,
    val fontFamily: FontFamily?,
    val fontSize: Double,
    val hyphens: Boolean,
    val imageFilter: ImageFilter,
    val letterSpacing: Double,
    val ligatures: Boolean,
    val lineHeight: Double,
    val pageMargins: Double,
    val paragraphIndent: Double,
    val paragraphSpacing: Double,
    val publisherStyles: Boolean,
    val scroll: Boolean,
    val textAlign: TextAlign,
    val textColor: Color,
    val textNormalization: TextNormalization,
    val theme: Theme,
    val typeScale: Double,
    val verticalText: Boolean,
    val wordSpacing: Double
) : Configurable.Settings

internal fun ReadiumCss.update(settings: EpubSettings, fontFamilies: List<FontFamilyDeclaration>): ReadiumCss {
    return with (settings) {
        copy(
            layout = Layout.from(settings),
            userProperties = userProperties.copy(
                view = when (scroll) {
                    false -> View.PAGED
                    true -> View.SCROLL
                },
                colCount = when (columnCount) {
                    ColumnCount.ONE -> ColCount.ONE
                    ColumnCount.TWO -> ColCount.TWO
                    else -> ColCount.AUTO
                },
                pageMargins = pageMargins,
                appearance = when (theme) {
                    Theme.LIGHT -> null
                    Theme.DARK -> Appearance.NIGHT
                    Theme.SEPIA -> Appearance.SEPIA
                },
                darkenImages = imageFilter == ImageFilter.DARKEN,
                invertImages = imageFilter == ImageFilter.INVERT,
                textColor = textColor.toCss(),
                backgroundColor = backgroundColor.toCss(),
                fontOverride = (fontFamily != null || (textNormalization == TextNormalization.ACCESSIBILITY)),
                fontFamily = fontFamily?.toCss(fontFamilies),
                // Font size is handled natively with WebSettings.textZoom.
                // See https://github.com/readium/mobile/issues/1#issuecomment-652431984
//                fontSize = fontSize.value
//                    ?.let { Length.Percent(it) },
                advancedSettings = !publisherStyles,
                typeScale = typeScale,
                textAlign = when (textAlign) {
                    TextAlign.JUSTIFY -> org.readium.r2.navigator.epub.css.TextAlign.JUSTIFY
                    TextAlign.LEFT -> org.readium.r2.navigator.epub.css.TextAlign.LEFT
                    TextAlign.RIGHT -> org.readium.r2.navigator.epub.css.TextAlign.RIGHT
                    TextAlign.START, TextAlign.CENTER, TextAlign.END -> org.readium.r2.navigator.epub.css.TextAlign.START
                },
                lineHeight = Either(lineHeight),
                paraSpacing = Length.Rem(paragraphSpacing),
                paraIndent = Length.Rem(paragraphIndent),
                wordSpacing = Length.Rem(wordSpacing),
                letterSpacing = Length.Rem(letterSpacing / 2),
                bodyHyphens = if (hyphens) Hyphens.AUTO else Hyphens.NONE,
                ligatures = if (ligatures) Ligatures.COMMON else Ligatures.NONE,
                a11yNormalize = textNormalization == TextNormalization.ACCESSIBILITY,
                overrides = mapOf(
                    "font-weight" to if (textNormalization == TextNormalization.BOLD) "bold" else null
                )
            )
        )
    }
}

private fun FontFamily.toCss(declarations: List<FontFamilyDeclaration>): List<String> = buildList {
    val declaration = declarations.firstOrNull { it.fontFamily == this@toCss }
    checkNotNull(declaration) { "Cannot resolve font name."}
    add(name)
    val alternateChain = declaration.alternate?.fontFamily?.toCss(declarations)
    alternateChain?.let {  addAll(it) }
}

private fun Color.toCss(): org.readium.r2.navigator.epub.css.Color =
    org.readium.r2.navigator.epub.css.Color.Int(int)
