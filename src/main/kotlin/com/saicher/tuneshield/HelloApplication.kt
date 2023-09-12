package com.saicher.tuneshield

import com.saicher.tuneshield.storage.StorageController
import com.saicher.tuneshield.storage.StorageManager
import com.saicher.tuneshield.storage.StorageTask
import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.stage.Stage

class HelloApplication : Application() {
    override fun start(stage: Stage) {
        val loader = FXMLLoader(HelloApplication::class.java.getResource("/GUI/StorageManager.fxml"))
        val root = loader.load<Parent>()
        val controller = loader.getController<StorageController>()
        stage.title = "TuneShield"
        stage.scene = Scene(root)
        stage.show()

        val storageManager = StorageManager(controller)
        val task = StorageTask(storageManager)

        Thread(task).start()
    }
}

fun main() {
    Application.launch(HelloApplication::class.java)
}