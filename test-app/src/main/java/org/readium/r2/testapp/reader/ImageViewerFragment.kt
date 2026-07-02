/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.reader

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.DialogFragment
import org.readium.r2.testapp.databinding.FragmentImageViewerBinding

internal class ImageViewerFragment : DialogFragment() {

    private var binding: FragmentImageViewerBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentImageViewerBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val b = binding ?: return

        b.btnClose.setOnClickListener { dismiss() }

        ViewCompat.setOnApplyWindowInsetsListener(b.btnClose) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = bars.top + 8
                rightMargin = bars.right + 8
            }
            insets
        }

        @Suppress("DEPRECATION")
        val bitmap = arguments?.getParcelable<Bitmap>(ARG_BITMAP)
        if (bitmap != null) {
            b.imageViewer.setImageBitmap(bitmap)
        } else {
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    companion object {
        private const val ARG_BITMAP = "bitmap"

        fun newInstance(bitmap: Bitmap): ImageViewerFragment =
            ImageViewerFragment().apply {
                arguments = bundleOf(ARG_BITMAP to bitmap)
            }
    }
}
