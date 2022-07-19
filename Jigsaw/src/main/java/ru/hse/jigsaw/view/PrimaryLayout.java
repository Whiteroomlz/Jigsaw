package ru.hse.jigsaw.view;

import javafx.scene.Scene;
import ru.hse.jigsaw.controllers.PrimaryLayoutController;

/**
 * Model-класс основного окна приложения.
 */
public record PrimaryLayout(Scene scene, PrimaryLayoutController controller) {
    public final static double STAGE_MIN_WIDTH = 1024.0;
    public final static double STAGE_MIN_HEIGHT = 768.0;

    public static final String BACKGROUND_MUSIC_PATH = "sounds/background.mp3";
    public static final String BUTTON_SELECTION_SOUND_PATH = "sounds/button_selected.mp3";
    public static final String BUTTON_PRESS_SOUND_PATH = "sounds/button_pressed.mp3";
    public static final String SHAPE_DROP_SOUND_PATH = "sounds/shape_dropped.wav";
    public static final String ICON_PATH = "view/images/icon.png";

    /**
     * Паттерн отображения времени в секундомере основ приложения.
     */
    public static final String TIME_PATTERN = "HH:mm:ss";
}