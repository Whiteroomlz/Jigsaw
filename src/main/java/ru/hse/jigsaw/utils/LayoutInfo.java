package ru.hse.jigsaw.utils;

import javafx.scene.Scene;

public record LayoutInfo<T>(Scene scene, T controller) {
}
