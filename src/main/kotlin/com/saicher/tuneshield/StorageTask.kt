package com.saicher.tuneshield

import javafx.concurrent.Task

class StorageTask(private val manager: StorageManager) : Task<Void>() {
    override fun call(): Void? {
        manager.runStorageScript()
        return null
    }

}