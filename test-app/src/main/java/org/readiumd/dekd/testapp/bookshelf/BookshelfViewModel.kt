/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readiumd.dekd.testapp.bookshelf

import android.app.Activity
import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.readiumd.dekd.lcp.LcpService
import org.readiumd.dekd.shared.UserException
import org.readiumd.dekd.shared.extensions.mediaType
import org.readiumd.dekd.shared.extensions.tryOrNull
import org.readiumd.dekd.shared.publication.Publication
import org.readiumd.dekd.shared.publication.asset.FileAsset
import org.readiumd.dekd.shared.publication.services.cover
import org.readiumd.dekd.shared.util.Try
import org.readiumd.dekd.shared.util.flatMap
import org.readiumd.dekd.shared.util.mediatype.MediaType
import org.readiumd.dekd.streamer.Streamer
import org.readiumd.dekd.testapp.BuildConfig
import org.readiumd.dekd.testapp.domain.model.Book
import org.readiumd.dekd.testapp.reader.ReaderActivityContract
import org.readiumd.dekd.testapp.reader.ReaderRepository
import org.readiumd.dekd.testapp.utils.EventChannel
import org.readiumd.dekd.testapp.utils.extensions.copyToTempFile
import org.readiumd.dekd.testapp.utils.extensions.moveTo
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import kotlin.time.ExperimentalTime

class BookshelfViewModel(application: Application) : AndroidViewModel(application) {

    private val r2Application = application as org.readiumd.dekd.testapp.Application
    private val bookRepository = r2Application.bookRepository
    private val preferences =
        application.getSharedPreferences("org.readium.r2.settings", Context.MODE_PRIVATE)
    private var lcpService = LcpService(application)
        ?.let { Try.success(it) }
        ?: Try.failure(Exception("liblcp is missing on the classpath"))
    private var streamer = Streamer(
        application,
        contentProtections = listOfNotNull(
            lcpService.getOrNull()?.contentProtection()
        )
    )
    private var r2Directory: String = r2Application.r2Directory
    val channel = EventChannel(Channel<Event>(Channel.BUFFERED), viewModelScope)

    val books = bookRepository.books()

    init {
        copySamplesFromAssetsToStorage()
    }

    fun deleteBook(book: Book) = viewModelScope.launch {
        book.id?.let { bookRepository.deleteBook(it) }
        tryOrNull { File(book.href).delete() }
        tryOrNull { File("${r2Application.r2Directory}covers/${book.id}.png").delete() }
    }

    private suspend fun addPublicationToDatabase(
        href: String,
        mediaType: MediaType,
        publication: Publication
    ): Long {
        if (mediaType == MediaType.PDF) return -1L
        val id = bookRepository.insertBook(href, mediaType, publication)
        storeCoverImage(publication, id.toString())
        return id
    }

    fun copySamplesFromAssetsToStorage() = viewModelScope.launch(Dispatchers.IO) {
        withContext(Dispatchers.IO) {
            if (!preferences.contains("samples")) {
                val dir = File(r2Directory)
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                val samples = r2Application.assets.list("Samples")?.filterNotNull().orEmpty()
                for (element in samples) {
                    val file =
                        r2Application.assets.open("Samples/$element").copyToTempFile(r2Directory)
                    if (file != null)
                        importPublication(file)
                    else if (BuildConfig.DEBUG)
                        error("Unable to load sample into the library")
                }
                preferences.edit().putBoolean("samples", true).apply()
            }
        }
    }

    fun importPublicationFromUri(
        uri: Uri
    ) = viewModelScope.launch {
        uri.copyToTempFile(r2Application, r2Directory)
            ?.let {
                importPublication(it)
            }
    }

    private suspend fun importPublication(
        sourceFile: File
    ) {
        val sourceMediaType = sourceFile.mediaType()
        val publicationAsset: FileAsset =
            if (sourceMediaType != MediaType.LCP_LICENSE_DOCUMENT)
                FileAsset(sourceFile, sourceMediaType)
            else {
                lcpService
                    .flatMap { it.acquirePublication(sourceFile) }
                    .fold(
                        {
                            val mediaType =
                                MediaType.of(fileExtension = File(it.suggestedFilename).extension)
                            FileAsset(it.localFile, mediaType)
                        },
                        {
                            tryOrNull { sourceFile.delete() }
                            Timber.d(it)
                            channel.send(Event.ImportPublicationFailed(it.message))
                            return
                        }
                    )
            }

        val mediaType = publicationAsset.mediaType()
        val fileName = "${UUID.randomUUID()}.${mediaType.fileExtension}"
        val libraryAsset = FileAsset(File(r2Directory + fileName), mediaType)

        try {
            publicationAsset.file.moveTo(libraryAsset.file)
        } catch (e: Exception) {
            Timber.d(e)
            tryOrNull { publicationAsset.file.delete() }
            channel.send(Event.UnableToMovePublication)
            return
        }

        streamer.open(libraryAsset, allowUserInteraction = false)
            .onSuccess {
                addPublicationToDatabase(libraryAsset.file.path, libraryAsset.mediaType(), it).let { id ->

                    if (id != -1L)
                        channel.send(Event.ImportPublicationSuccess)
                    else
                        channel.send(Event.ImportDatabaseFailed)
                }
            }
            .onFailure {
                tryOrNull { libraryAsset.file.delete() }
                Timber.d(it)
                channel.send(Event.ImportPublicationFailed(it.getUserMessage(r2Application)))
            }
    }

    @OptIn(ExperimentalTime::class)
    fun openBook(
        bookId: Long,
        activity: Activity
    ) = viewModelScope.launch {
        val readerRepository = r2Application.readerRepository.await()
        readerRepository.open(bookId, activity)
            .onFailure { exception ->
                if (exception is ReaderRepository.CancellationException)
                    return@launch

                val message = when (exception) {
                    is UserException -> exception.getUserMessage(r2Application)
                    else -> exception.message
                }
                channel.send(Event.OpenBookError(message))
            }
            .onSuccess {
                val arguments = ReaderActivityContract.Arguments(bookId)
                channel.send(Event.LaunchReader(arguments))
            }
    }

    fun closeBook(bookId: Long) = viewModelScope.launch {
        val readerRepository = r2Application.readerRepository.await()
        readerRepository.close(bookId)
    }

    private fun storeCoverImage(publication: Publication, imageName: String) =
        viewModelScope.launch(Dispatchers.IO) {
            // TODO Figure out where to store these cover images
            val coverImageDir = File("${r2Directory}covers/")
            if (!coverImageDir.exists()) {
                coverImageDir.mkdirs()
            }
            val coverImageFile = File("${r2Directory}covers/${imageName}.png")

            val bitmap: Bitmap? = publication.cover()

            val resized = bitmap?.let { Bitmap.createScaledBitmap(it, 120, 200, true) }
            val fos = FileOutputStream(coverImageFile)
            resized?.compress(Bitmap.CompressFormat.PNG, 80, fos)
            fos.flush()
            fos.close()
        }

    private fun getBitmapFromURL(src: String): Bitmap? {
        return try {
            val url = URL(src)
            val connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()
            val input = connection.inputStream
            BitmapFactory.decodeStream(input)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    sealed class Event {

        class ImportPublicationFailed(val errorMessage: String?) : Event()

        object UnableToMovePublication : Event()

        object ImportPublicationSuccess : Event()

        object ImportDatabaseFailed : Event()

        class OpenBookError(val errorMessage: String?) : Event()

        class LaunchReader(val arguments: ReaderActivityContract.Arguments) : Event()
    }
}