package de.abdocore.sharetohttp

import android.content.ContentResolver
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import java.io.*

class HtmlPage (val contentResolver: ContentResolver, val baseDir: File) {

    private val HTML_HEADER = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8"/>
            <title>ShareToHttp</title>
        </head>
        <body>
    """.trimIndent()

    private val HTML_FOOTER = """
        </body>
        </html>
    """.trimIndent()


    private var quotedText: String = ""
    private var sharedImages: MutableList<Uri> = mutableListOf()




    fun addText(text: String) {
        quotedText += text
    }

    fun clear() {
        quotedText = ""
        sharedImages = mutableListOf()
    }

    fun filenameOf(uri: Uri): String {
        val cursor = contentResolver.query(uri, null, null, null, null)
        if (cursor == null) {
            return "INVALID_NAME.jpg"
        }
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        cursor.moveToFirst()
        return cursor.getString(nameIndex)
    }

    private fun copySharedFile(uri: Uri): Boolean {

        val parcelFileDescriptor : ParcelFileDescriptor? = try {
            contentResolver.openFileDescriptor(uri, "r")
        } catch (e: FileNotFoundException) {
            return false
        }

        val input = FileInputStream(parcelFileDescriptor?.fileDescriptor)
        val outputName = baseDir.absolutePath + File.separator + filenameOf(uri)
        val output = FileOutputStream(outputName)


        //input.channel.transferTo(0, input.channel.size(), output.channel)
        try {
            val bytesCopied = output.channel.transferFrom(input.channel, 0, input.channel.size())
            if (bytesCopied != input.channel.size()) {
                return false
            }
        } catch (e: IOException) {
            return false
        }

        input.channel?.close()
        output.channel?.close()

        input.close()
        output.close()

        return true
    }


    fun addImage(uri: Uri): Boolean {
        val copyWorked = copySharedFile(uri)
        if (copyWorked) {
            sharedImages.add(uri)
        }
        return copyWorked
    }


    fun generateWebpage() {
        val indexWriteStream = FileOutputStream(baseDir.absolutePath + File.separator + "index.html");
        var htmlText = HTML_HEADER

        var somethingShared = false

        if (quotedText != "") {
            somethingShared = true
            htmlText += """
                <h1>Shared Text</h1>
                <textarea readonly="true" title="Shared Text" style="width:90%" rows="20">""".trimIndent()
            htmlText += quotedText
            htmlText += "</textarea>\n"
        }

        if (! sharedImages.isEmpty()) {
            somethingShared = true
            htmlText += """
                <h1>Shared Images</h1>
                <ul>
            """.trimIndent()
            for (uri in sharedImages) {
                htmlText += """
                    <li>
                        <a href="${filenameOf(uri)}"><img src="${filenameOf(uri)}" style="width:30vw;"/></a><br/>
                        <a href="${filenameOf(uri)}">${filenameOf(uri)}</a>
                    </li>
                """.trimIndent()
            }
            htmlText += "</ul>\n"
        }

        if (! somethingShared)
        {
            htmlText += "<p>Nothing shared yet</p>"
        }

        htmlText += HTML_FOOTER

        indexWriteStream.write(htmlText.toByteArray())
        indexWriteStream.close();
    }
}