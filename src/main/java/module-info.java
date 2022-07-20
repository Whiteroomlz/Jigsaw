module ru.hse {
    requires javafx.controls;
    requires javafx.media;
    requires javafx.fxml;
    requires org.json;
    requires java.logging;

    opens ru.hse.jigsaw to javafx.fxml;
    exports ru.hse.jigsaw;
    exports ru.hse.jigsaw.controller;
    opens ru.hse.jigsaw.controller to javafx.fxml;
    exports ru.hse.jigsaw.view;
    opens ru.hse.jigsaw.view to javafx.fxml;
    exports ru.hse.jigsaw.model.shape;
    opens ru.hse.jigsaw.model.shape to javafx.fxml;
    exports ru.hse.jigsaw.model.stopwatch;
    opens ru.hse.jigsaw.model.stopwatch to javafx.fxml;
    exports ru.hse.jigsaw.utils;
    opens ru.hse.jigsaw.utils to javafx.fxml;
    exports ru.hse.jigsaw.model.net;
    opens ru.hse.jigsaw.model.net to javafx.fxml;
}