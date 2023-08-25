module com.saicher.tuneshield {
    requires javafx.controls;
    requires javafx.fxml;
                requires kotlin.stdlib;
    
        requires org.controlsfx.controls;
                        requires org.kordamp.bootstrapfx.core;
            
    opens com.saicher.tuneshield to javafx.fxml;
    exports com.saicher.tuneshield;
}