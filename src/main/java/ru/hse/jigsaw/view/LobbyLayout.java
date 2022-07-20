package ru.hse.jigsaw.view;

import javafx.scene.Scene;
import ru.hse.jigsaw.controller.LobbyLayoutController;

public record LobbyLayout(Scene scene, LobbyLayoutController controller) {
    public static final String ICON_PATH = "view/images/icon.png";

    public static final String TIME_PATTERN = "HH:mm:ss";
}
