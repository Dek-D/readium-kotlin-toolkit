package org.readium.navigator.image

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import org.readium.navigator.internal.util.FitBox
import org.readium.navigator.internal.util.ZoomableBox
import org.readium.navigator.internal.util.rememberZoomableBoxState
import org.readium.r2.shared.fetcher.Resource
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication

@Composable
fun ImageSpread(
    publication: Publication,
    link: Link,
    scaleState: MutableState<Float>,
    isPaginated: Boolean
) {
    val resource = publication.get(link)

    val itemSize = Size(link.width!!.toFloat(), link.height!!.toFloat())

    if (isPaginated) {
        ScrollableImage(resource, scaleState, itemSize)
    } else {
        ImageOrPlaceholder(resource)
    }
}

@Composable
private fun ScrollableImage(
    resource: Resource,
    scaleState: MutableState<Float>,
    itemSize: Size,

) {
    val state = rememberZoomableBoxState(scaleState)

    BoxWithConstraints {

        val parentSize =
            Size(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())

         ZoomableBox(
             modifier = Modifier.fillMaxSize(),
             state = state
         ) {
             FitBox(
                 parentSize = parentSize,
                 contentScale = ContentScale.Fit,
                 scaleSetting = scaleState.value,
                 itemSize = itemSize,
                 content = { ImageOrPlaceholder(resource) }
             )
         }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@Composable
private fun ImageOrPlaceholder(resource: Resource) {
    val loadedState = remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val bitmap = remember {
        coroutineScope.async {
            val bytes = resource.read().getOrThrow()
            val bitmap = BitmapFactory.decodeByteArray(bytes)!!.asImageBitmap()
            loadedState.value = true
            bitmap
        }
    }

    if (loadedState.value) {
        Image(bitmap.getCompleted())
    } else {
        Placeholder()
    }
}

@Composable
private fun Placeholder() {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Gray)
    )
}