package nl.bldn.housedata

import mu.KLogger
import mu.KLogging
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.nio.file.Paths
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.zip.ZipInputStream

fun main() {
    PhotosFixer().run()
}

class PhotosFixer {

    fun run() {
        File(destDir).mkdir()

        val srcDir = File(sourceDir)

        for (srcFile in srcDir.listFiles()!!) {
            if (srcFile.name.startsWith(".")) continue

            logger.warn { "Processing src file: $srcFile" }

            val dateStringParts = srcFile.name.split(" - ")[0].split("-")
            val date = LocalDate.of(dateStringParts[0].toInt(), dateStringParts[1].toInt(), dateStringParts[2].toInt())
            ZipInputStream(srcFile.inputStream().buffered()).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val newFileName = "${date.format(DateTimeFormatter.ofPattern("yyyyMMdd"))}-${entry.name}"

                    val newFile = File(destDir, newFileName).canonicalFile

                    // Prevent Zip Slip: Ensure the newFile is within the intended directory
                    if (!newFile.toPath().startsWith(File(destDir).toPath())) {
                        throw SecurityException("Blocked Zip Slip attack attempt: ${entry.name}")
                    }

                    if (entry.isDirectory) {
                        newFile.mkdirs()
                    } else {
                        newFile.parentFile.mkdirs()
                        FileOutputStream(newFile).use { fos -> zis.copyTo(fos) }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        }
    }

    companion object : KLogging() {
        private val sourceDir = "/Users/nielskuiper/Downloads/SocialSchools"
        private val destDir = "/Users/nielskuiper/Downloads/SocialSchools2"
    }
}