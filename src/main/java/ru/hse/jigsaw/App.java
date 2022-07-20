package ru.hse.jigsaw;

import javafx.application.Application;
import javafx.stage.Stage;
import ru.hse.jigsaw.controller.PrimaryLayoutController;
import ru.hse.jigsaw.utils.LayoutDesigner;
import ru.hse.jigsaw.utils.LayoutInfo;
import ru.hse.jigsaw.view.PrimaryLayout;

public class App extends Application {
    PrimaryLayout primaryLayout;

    @Override
    public void start(Stage stage) {
        stage.setMinWidth(PrimaryLayout.STAGE_MIN_WIDTH);
        stage.setMinHeight(PrimaryLayout.STAGE_MIN_HEIGHT);
        LayoutDesigner.setIcon(stage, PrimaryLayout.ICON_PATH);

        LayoutInfo<PrimaryLayoutController> pair =
                LayoutDesigner.loadNewScene(LayoutDesigner.GameStyle.DARK_STYLE, "view/primary_layout.fxml");
        primaryLayout = new PrimaryLayout(pair.scene(), pair.controller());

        stage.setTitle("Мозаика");
        stage.setScene(primaryLayout.scene());
        stage.setOnHiding(event -> primaryLayout.controller().onAppFinish());
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
