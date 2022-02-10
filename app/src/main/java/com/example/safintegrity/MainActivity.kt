package com.example.safintegrity

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import java.io.InputStream
import java.security.MessageDigest

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.button_create_document)?.setOnClickListener {
            activityResultLauncherCreation.launch("ImageTest.jpg")
        }

        findViewById<Button>(R.id.button_open_document)?.setOnClickListener {
            activityResultLauncherOpening.launch("*/*")
        }
    }

    /*
     * HASH
     */

    private fun md5(inputStream: InputStream): String {
        val digest = MessageDigest.getInstance("MD5")
        inputStream.buffered(1024).use { it.iterator().forEach(digest::update) }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun InputStream.readAllBytes(bufferSize: Int = DEFAULT_BUFFER_SIZE,
                                         readBytes: (bytesRead: ByteArray) -> Unit) {
        val buffer = ByteArray(bufferSize)
        var read = 0
        while (read != -1) {
            read = this.read(buffer, 0, buffer.size)
            if (read != -1) {
                val optimizedBuffer: ByteArray = if (buffer.size == read) {
                    buffer
                } else {
                    buffer.copyOf(read)
                }
                readBytes.invoke(optimizedBuffer)
            }
        }
    }

    /*
     * CREATION
     */

    private val resultCreationCallback = ActivityResultCallback<Uri> { resultUri ->
        val outputStream = contentResolver.openOutputStream(resultUri, "rwt")
        outputStream?.use {
            val assetString = "test.jpg"
            assets.open(assetString).use { inputStream ->
                inputStream.readAllBytes { buffer ->
                    outputStream.write(buffer)
                }
            }
            findViewById<TextView>(R.id.create_document_hash).text =
                md5(assets.open(assetString))
        }
    }

    private val activityResultLauncherCreation = this.registerForActivityResult(
            CreateDocument(),
            resultCreationCallback
        )

    private class CreateDocument : ActivityResultContracts.CreateDocument() {
        override fun createIntent(context: Context, input: String): Intent {
            return super.createIntent(context, input).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/octet-stream"
            }
        }
    }

    /*
     * OPENING
     */

    private val resultOpeningCallback = ActivityResultCallback<Uri> { resultUri ->
        contentResolver.openInputStream(resultUri)?.let { inputStream ->
            findViewById<TextView>(R.id.open_document_hash).text = md5(inputStream)
        }
    }

    private val activityResultLauncherOpening = this.registerForActivityResult(
        GetContent(),
        resultOpeningCallback
    )

    class GetContent : ActivityResultContracts.GetContent() {
        override fun createIntent(context: Context, input: String): Intent {
            return super.createIntent(context, input).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
        }
    }
}