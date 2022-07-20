package ru.hse.jigsaw.model.stopwatch;

import javafx.animation.AnimationTimer;
import javafx.scene.control.Label;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Класс, описывающий секундомер.
 */
public class Stopwatch extends AnimationTimer {
    /**
     * Контейнер, в котором необходимо обновлять информацию о прошедшем с момента старта времени.
     */
    private final Label timeLabel;

    /**
     * Время старта секундомера.
     */
    private LocalDateTime startTimestamp;

    /**
     * Формат вывода.
     */
    private final DateTimeFormatter formatter;

    public Stopwatch(Label timeLabel, String timePattern) {
        this.timeLabel = timeLabel;
        formatter = DateTimeFormatter.ofPattern(timePattern);
    }

    public void reset() {
        startTimestamp = LocalDateTime.now();
        timeLabel.setText(formatter.format(LocalTime.of(0, 0, 0)));
    }

    public Duration getDuration() {
        return Duration.between(startTimestamp, LocalDateTime.now());
    }

    @Override
    public void start() {
        startTimestamp = LocalDateTime.now();
        super.start();
    }

    @Override
    public void handle(long now) {
        long secondsDuration = getDuration().toSeconds();
        timeLabel.setText(formatter.format(LocalTime.of(0, 0, 0).plusSeconds(secondsDuration)));
    }
}
