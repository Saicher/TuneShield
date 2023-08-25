package com.saicher.tuneshield

import javafx.animation.*
import javafx.application.Platform
import javafx.fxml.FXML
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Parent
import javafx.scene.control.Label
import javafx.scene.control.ListView
import javafx.scene.control.ProgressIndicator
import javafx.scene.effect.BlendMode
import javafx.scene.effect.ColorAdjust
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.TilePane
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import javafx.scene.text.TextAlignment
import javafx.util.Duration

class StorageController {
    @FXML
    lateinit var scannedPercentage: ProgressIndicator
    @FXML
    lateinit var elapsedTimeLabel: Label
    @FXML
    lateinit var folderPane: TilePane
    @FXML
    lateinit var root: Parent

    val imageLoc = javaClass.getResource("/Images/folder.png")
    val font = Font.loadFont(javaClass.getResource("/Fonts/Inter-Bold.otf")?.toString(), 20.0)

    private var targetProgress: Double = 0.0
    private val lerpSpeed: Double = 0.02  // Adjust this for faster/slower interpolation

    private val timer = object : AnimationTimer() {
        override fun handle(now: Long) {
            val currentProgress = scannedPercentage.progress
            scannedPercentage.progress = lerp(currentProgress, targetProgress, lerpSpeed)

            // Stop the timer if the progress is close enough to the target
            if (Math.abs(scannedPercentage.progress - targetProgress) < 0.001) {
                scannedPercentage.progress = targetProgress
                stop()
                if (targetProgress >= 1.0) {
                    fadeOutProgressIndicator()
                }
            }
        }
    }

    fun lerp(a: Double, b: Double, t: Double): Double {
        return a + t * (b - a)
    }

    fun updateProgress(progress: Double) {
        targetProgress = progress
        timer.start()
    }

    fun updateElapsedTime(time: String) {
        Platform.runLater {
            elapsedTimeLabel.text = time
        }
    }
    fun addFolderInfo(folderName: String, size: Double) {
        Platform.runLater {

            // Folder Name Label:
            val folderLabel = Label(folderName)
            folderLabel.font = font
            folderLabel.textFill = Color.WHITE
            folderLabel.textAlignment = TextAlignment.CENTER
            folderLabel.alignment = Pos.TOP_CENTER

            // Folder Image:
            val imageView = ImageView(imageLoc.toString())
            imageView.fitWidth = folderPane.prefTileWidth
            imageView.fitHeight = folderPane.prefTileHeight

            // Folder Size Label:
            val sizeLabel = Label(size.toString()+"%")
            val normalized = Math.min(size / 60.0, 1.0)
            val redValue = (normalized * 255).toInt()
            sizeLabel.font = font
            sizeLabel.textFill = Color.rgb(redValue, 0, 0)
            sizeLabel.textAlignment = TextAlignment.CENTER
            sizeLabel.alignment = Pos.CENTER
            sizeLabel.padding = Insets(-220.0, 0.0, 0.0, 0.0)
            sizeLabel.opacity = 0.5

            val folderBox = VBox(imageView, folderLabel, sizeLabel)
            folderBox.alignment = Pos.CENTER
            setupVBoxHoverAnimation(folderBox)
            setupFlashOnVBoxClick(folderBox, imageView)

            folderPane.children.add(folderBox)
            folderLabel.translateY = -30.0
            folderPane.requestLayout()

            setupResizing(folderBox, folderPane, imageView, sizeLabel)
        }
    }

    private fun setupResizing(vbox: VBox, tilePane: TilePane, imageView: ImageView, sizeLabel: Label) {

        // Capture initial sizes
        val initialVBoxWidth = vbox.width
        val initialVBoxHeight = vbox.height
        val initialTilePaneWidth = tilePane.width
        val initialTilePaneHeight = tilePane.height
        val initialTileWidth = tilePane.prefTileWidth
        val initialTileHeight = tilePane.prefTileHeight

        // Add listeners to the TilePane's width and height properties
        tilePane.widthProperty().addListener { _, _, newValue ->
            var scale = newValue.toDouble() / initialTilePaneWidth
            scale = kotlin.math.round(scale)

            vbox.prefWidth = initialVBoxWidth * scale
            imageView.fitWidth = initialTileWidth * scale
            tilePane.prefTileWidth = initialTileWidth * scale
            tilePane.hgap = 60 * scale

            sizeLabel.scaleX = scale
            sizeLabel.scaleY = scale
            sizeLabel.padding = Insets((initialTileHeight / scale) * -scale, 0.0, 0.0, 0.0)

            tilePane.requestLayout()
        }

        tilePane.heightProperty().addListener { _, _, newValue ->
            var scale = newValue.toDouble() / initialTilePaneHeight
            scale = kotlin.math.round(scale)

            vbox.prefHeight = initialVBoxHeight * scale
            imageView.fitHeight = initialTileHeight * scale
            tilePane.prefTileHeight = initialTileHeight * scale
            tilePane.vgap = 60 * scale

            sizeLabel.scaleX = scale
            sizeLabel.scaleY = scale
            sizeLabel.padding = Insets((initialTileHeight / scale) * -scale, 0.0, 0.0, 0.0)

            tilePane.requestLayout()
        }
    }

    private fun fadeOutProgressIndicator() {
        val fadeTransition = FadeTransition(Duration.millis(1000.0), scannedPercentage)
        fadeTransition.fromValue = 1.0
        fadeTransition.toValue = 0.0
        fadeTransition.setOnFinished {
            scannedPercentage.isVisible = false
            elapsedTimeLabel.isVisible = true
            folderPane.isVisible = true
            scannedPercentage.progress = 0.0

            val vBoxes = folderPane.children.filterIsInstance<VBox>()
            animateVBoxesIntoPosition(vBoxes)
        }
        fadeTransition.play()
    }

    fun setupVBoxHoverAnimation(vbox: VBox) {
        val scaleUp = 1.5
        val scaleDown = 1.0
        val duration = Duration.millis(200.0)  // Duration of the animation

        val scaleTransition = ScaleTransition(duration, vbox)
        scaleTransition.cycleCount = 1

        vbox.setOnMouseEntered {
            scaleTransition.stop()  // Stop any ongoing animation
            scaleTransition.toX = scaleUp
            scaleTransition.toY = scaleUp
            scaleTransition.playFromStart()
        }

        vbox.setOnMouseExited {
            scaleTransition.stop()  // Stop any ongoing animation
            scaleTransition.toX = scaleDown
            scaleTransition.toY = scaleDown
            scaleTransition.playFromStart()
        }
    }

    fun setupFlashOnVBoxClick(vBox: VBox, imageView: ImageView) {
        val originalBlend = imageView.blendMode

        // Define the PauseTransition
        val pause = PauseTransition(Duration.millis(200.0))
        pause.setOnFinished {
            imageView.blendMode = originalBlend
        }

        // Add a click event to the VBox
        vBox.setOnMouseClicked {
            imageView.blendMode = BlendMode.BLUE
            pause.playFromStart()
        }
    }

    fun animateVBoxesIntoPosition(vBoxes: List<VBox>) {
        val duration = Duration.millis(1000.0) // Duration of the animation

        vBoxes.forEachIndexed { index, vBox ->
            // Set the initial position outside the top of the window
            vBox.translateY = -vBox.scene.window.height

            // Create the TranslateTransition for the VBox
            val transition = TranslateTransition(duration, vBox)
            transition.toY = 0.0 // Final position

            // Delay the start of the transition for a cascading effect
            transition.delay = Duration.millis(300.0 * index)

            transition.play()
        }
    }
}
