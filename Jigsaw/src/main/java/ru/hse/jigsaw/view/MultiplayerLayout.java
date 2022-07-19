package ru.hse.jigsaw.view;

import javafx.scene.Scene;
import ru.hse.jigsaw.controllers.MultiplayerLayoutController;

public record MultiplayerLayout(Scene scene, MultiplayerLayoutController controller) {
    public final static double STAGE_WIDTH = 620.0;
    public final static double STAGE_HEIGHT = 440.0;

    public static final String BUTTON_SELECTION_SOUND_PATH = "sounds/button_selected.mp3";
    public static final String BUTTON_PRESS_SOUND_PATH = "sounds/button_pressed.mp3";
    public static final String ICON_PATH = "view/images/icon.png";
}
