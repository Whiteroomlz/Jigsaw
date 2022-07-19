package ru.hse.jigsaw.utils;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.time.Duration;
import java.util.Objects;

public class PlayerInfo implements Comparable<PlayerInfo> {
    String id;
    String nickname;
    String status;
    Duration gameDuration;
    int placedShapesCount;

    public PlayerInfo(String id, String nickname, String status, Duration gameDuration, int placedShapesCount) {
        this.id = id;
        this.nickname = nickname;
        this.status = status;
        this.gameDuration = gameDuration;
        this.placedShapesCount = placedShapesCount;
    }

    public StringProperty idProperty() {
        return new SimpleStringProperty(id);
    }

    public StringProperty nicknameProperty() {
        return new SimpleStringProperty(nickname);
    }

    public StringProperty statusProperty() {
        return new SimpleStringProperty(status);
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Duration getGameDuration() {
        return gameDuration;
    }

    public void setGameDuration(Duration gameDuration) {
        this.gameDuration = gameDuration;
    }

    public int getPlacedShapesCount() {
        return placedShapesCount;
    }

    public void setPlacedShapesCount(int placedShapesCount) {
        this.placedShapesCount = placedShapesCount;
    }

    @Override
    public int compareTo(PlayerInfo other) {
        if (placedShapesCount > other.placedShapesCount) {
            return -1;
        } else if (placedShapesCount == other.placedShapesCount) {
            return gameDuration.compareTo(other.gameDuration);
        } else {
            return 1;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (!(o instanceof PlayerInfo that))
            return false;

        return getPlacedShapesCount() == that.getPlacedShapesCount() && Objects.equals(getId(), that.getId())
                && Objects.equals(getNickname(), that.getNickname()) && Objects.equals(getStatus(), that.getStatus())
                && Objects.equals(getGameDuration(), that.getGameDuration());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getNickname(), getStatus(), getGameDuration(), getPlacedShapesCount());
    }
}
