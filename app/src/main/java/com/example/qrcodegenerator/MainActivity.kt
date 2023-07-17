package com.example.qrcodegenerator

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageFormat.JPEG
import android.media.Image
import android.net.Uri
import android.opengl.Visibility
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.MediaStore.Audio.Media
import android.view.ContentInfo
import android.view.View
import android.webkit.URLUtil
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.EncodeHintType.*
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import com.journeyapps.barcodescanner.BarcodeEncoder
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Hashtable


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val textView=findViewById<TextView>(R.id.editText)
        val qr=findViewById<ImageView>(R.id.imageView)
        val btn=findViewById<ImageView>(R.id.sendBtn)
        btn.visibility=View.GONE

        findViewById<Button>(R.id.button).setOnClickListener{
            val string=textView.text.toString()
            if(string.isNotEmpty()) {
                if (URLUtil.isValidUrl("https://$string")) {
                    val bitmap = generateQr(string)
                    qr.setImageBitmap(bitmap)
                    btn.visibility = View.VISIBLE
                    btn.setOnClickListener() {
                        val uri = saveImage(
                            this,
                            bitmap!!,
                            Bitmap.CompressFormat.JPEG,
                            "image/jpeg",
                            "newImage"
                        )
                        sendImage(uri)
                    }
                } else {
                    Toast.makeText(this, "Enter a valid url", Toast.LENGTH_SHORT).show()
                }
            }else {
                    Toast.makeText(this, "Cant create", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun sendImage(uri: Uri) {
        val sharingIntent=Intent(Intent.ACTION_SEND)
        sharingIntent.type = "image/jpeg"
        sharingIntent.putExtra(Intent.EXTRA_STREAM,uri)
        startActivity(Intent.createChooser(sharingIntent,"Share Image"))
    }

    @Throws(IOException::class)
    private fun saveImage(context: Context,bitmap:Bitmap,format: Bitmap.CompressFormat,mimeType:String,displayName:String):Uri {
        val values=ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME,displayName)
            put(MediaStore.MediaColumns.MIME_TYPE,mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH,Environment.DIRECTORY_DCIM)
        }
         var uri:Uri?=null
        return runCatching {
            with(context.contentResolver) {
                insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)?.also {
                    uri = it // Keep uri reference so it can be removed on failure

                    openOutputStream(it)?.use { stream ->
                        if (!bitmap.compress(format, 95, stream))
                            throw IOException("Failed to save bitmap.")
                    } ?: throw IOException("Failed to open output stream.")

                } ?: throw IOException("Failed to create new MediaStore record.")
            }
        }.getOrElse {
            uri?.let { orphanUri ->
                // Don't leave an orphan entry in the MediaStore
                context.contentResolver.delete(orphanUri, null, null)
            }

            throw it
        }
            }

    private fun generateQr(link: String): Bitmap? {
        val hints = Hashtable<EncodeHintType, String>()
        hints[CHARACTER_SET] = "UTF-8"
        val writer = QRCodeWriter()
        try {
            val bitMatrix = writer.encode(link, BarcodeFormat.QR_CODE, 1024, 1024, hints)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)
            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (bitMatrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
                }
            }
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            return bitmap
        } catch (e: WriterException) {
            e.printStackTrace()
            Toast.makeText(this,"Cant create",Toast.LENGTH_SHORT).show()
        }
        return null
    }
}