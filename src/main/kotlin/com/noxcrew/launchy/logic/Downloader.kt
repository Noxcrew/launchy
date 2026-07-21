package com.noxcrew.launchy.logic

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.contentLength
import io.ktor.utils.io.core.remaining
import io.ktor.utils.io.readAvailable
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.asSink
import kotlinx.io.buffered
import kotlinx.io.readByteArray
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories

object Downloader {
    val httpClient = HttpClient()

    suspend fun download(
        url: String,
        writeTo: Path,
        onProgressUpdate: (progress: Progress) -> Unit = {},
    ) = withContext(Dispatchers.IO) {
        try {
            val startTime = System.currentTimeMillis()
            val response = httpClient.get(url)
            val contentLength = response.contentLength() ?: -1L
            writeTo.parent.createDirectories()
            Files.newOutputStream(writeTo).asSink().buffered().use { sink ->
                val channel = response.bodyAsChannel()
                var downloaded = 0L
                val buffer = ByteArray(8192)
                while (!channel.isClosedForRead) {
                    val count = channel.readAvailable(buffer)
                    if (count <= 0) continue
                    sink.write(buffer, 0, count)
                    downloaded += count
                    onProgressUpdate(
                        Progress(
                            downloaded,
                            contentLength,
                            System.currentTimeMillis() - startTime
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }
}

data class Progress(val bytesDownloaded: Long, val totalBytes: Long, val timeElapsed: Long) {
    val percent: Float
        get() = bytesDownloaded.toFloat() / totalBytes.toFloat()
}
