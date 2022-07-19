module ru.hse {
    requires javafx.controls;
    requires javafx.media;
    requires javafx.fxml;
    requires org.json;
    requires java.logging;

    opens ru.hse.jigsaw to javafx.fxml;
    exports ru.hse.jigsaw;
    exports ru.hse.jigsaw.controllers;
    opens ru.hse.jigsaw.controllers to javafx.fxml;
    exports ru.hse.jigsaw.view;
    opens ru.hse.jigsaw.view to javafx.fxml;
    exports ru.hse.jigsaw.entities.shape;
    opens ru.hse.jigsaw.entities.shape to javafx.fxml;
    exports ru.hse.jigsaw.entities.stopwatch;
    opens ru.hse.jigsaw.entities.stopwatch to javafx.fxml;
    exports ru.hse.jigsaw.utils;
    opens ru.hse.jigsaw.utils to javafx.fxml;
    exports ru.hse.jigsaw.entities.net;
    opens ru.hse.jigsaw.entities.net to javafx.fxml;
}