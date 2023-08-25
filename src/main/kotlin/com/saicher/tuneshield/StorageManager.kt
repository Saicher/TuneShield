package com.saicher.tuneshield

import java.io.File
import java.io.IOException
class StorageManager(private val controller: StorageController) {

    fun runStorageScript() {
        val scriptResource = object {}.javaClass.getResourceAsStream("/Tools/storage_manager.ps1")
            ?: throw IOException("Script not found in resources")

        // Check if PowerShell is installed
        if (!isPowerShellInstalled()) {
            println("PowerShell is not installed on this system.")
            return
        }

        // Command to execute PowerShell and run the script
        val tempFile = File.createTempFile("storage_manager", ".ps1")
        tempFile.deleteOnExit()
        tempFile.outputStream().use { output ->
            scriptResource.copyTo(output)
        }
        val command = "powershell -ExecutionPolicy Bypass -File ${tempFile.absolutePath}"

        val processBuilder = ProcessBuilder()
        processBuilder.command("cmd.exe", "/c", command)
        // Redirect error stream to standard output
        processBuilder.redirectErrorStream(true)

        try {
            var progressPercentage = 0.0
            var elapsedTime = ""
            val size = mutableListOf<Double>()
            val folderNames = mutableListOf<String>()


            // Start the process
            val process = processBuilder.start()

            // Read the output line by line and print it
            process.inputStream.bufferedReader().use { reader ->
                var line: String? = reader.readLine()
                while (reader.readLine().also { line = it } != null) {
                    if (line!!.startsWith("PROGRESS")) {
                        progressPercentage = line!!.substringAfter("PROGRESS:").toDouble()
                        controller.updateProgress(progressPercentage / 100)
                    } else if (line!!.startsWith("SIZE")) {
                        size.add(line!!.substringAfter("SIZE:").toDouble())
                    } else if (line!!.startsWith("FOLDER")) {
                        folderNames.add(line!!.substringAfter("FOLDER:C:\\"))
                    } else if (line!!.startsWith("Time taken")) {
                        elapsedTime = line!!.substringAfter("Time taken:")
                    }
                }
            }

            // Show the Results:
            controller.updateElapsedTime("Time Taken: $elapsedTime")
            size.forEachIndexed { index, size ->
                controller.addFolderInfo(folderNames[index], size)
            }



            // Wait for the process to exit
            val exitCode = process.waitFor()
        } catch (e: IOException) {
            println("An error occurred while executing the script: ${e.message}")
        } catch (e: InterruptedException) {
            println("The process was interrupted: ${e.message}")
        }
    }

    fun isPowerShellInstalled(): Boolean {
        return try {
            val process = ProcessBuilder("powershell.exe", "-Command", "echo 'Checking PowerShell'").start()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor() == 0 && output.contains("Checking PowerShell")
        } catch (e: Exception) {
            false
        }
    }
}