package com.noxcrew.launchy.logic

import com.noxcrew.launchy.logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories

object Downloader {
    val client = okhttp3.OkHttpClient()

    suspend fun download(
        url: String,
        writeTo: Path,
        onProgressUpdate: (progress: Progress) -> Unit = {},
    ) = withContext(Dispatchers.IO) {
        try {
            val startTime = System.currentTimeMillis()
            var lastUpdate = 0L
            onProgressUpdate(Progress(0L, 0L, 0L))
            val request = Request.Builder().url(url).build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("HTTP ${response.code}: ${response.message}")
                }

                // After we get initial data we update the UI
                val body = response.body
                val totalBytes = body.contentLength()
                onProgressUpdate(
                    Progress(
                        0,
                        totalBytes,
                        System.currentTimeMillis() - startTime
                    )
                )

                // Start streaming the data to the output file
                writeTo.parent.createDirectories()
                body.byteStream().use { input ->
                    Files.newOutputStream(writeTo).buffered().use { output ->
                        val buffer = ByteArray(256 * 1024)
                        var downloaded = 0L

                        while (true) {
                            coroutineContext.ensureActive()
                            val read = input.read(buffer)
                            if (read == -1) break

                            output.write(buffer, 0, read)
                            downloaded += read

                            val now = System.currentTimeMillis()
                            if (now - lastUpdate >= 100 || downloaded == totalBytes) {
                                lastUpdate = now
                                onProgressUpdate(
                                    Progress(
                                        downloaded,
                                        totalBytes,
                                        now - startTime
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to download file from `$url`", e)
            throw e
        }
    }
}

data class Progress(val bytesDownloaded: Long, val totalBytes: Long, val timeElapsed: Long)
