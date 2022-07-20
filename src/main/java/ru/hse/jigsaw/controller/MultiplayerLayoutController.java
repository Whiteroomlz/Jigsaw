package ru.hse.jigsaw.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.media.AudioClip;
import javafx.stage.Stage;
import ru.hse.jigsaw.App;
import ru.hse.jigsaw.model.net.Client;
import ru.hse.jigsaw.model.net.Server;
import ru.hse.jigsaw.utils.LayoutDesigner;
import ru.hse.jigsaw.view.MultiplayerLayout;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.time.Duration;
import java.util.Objects;
import java.util.ResourceBundle;

public class MultiplayerLayoutController implements Initializable {
    @FXML
    private Button confirmButton;
    @FXML
    private TextField ipField;
    @FXML
    private TextField portField;
    @FXML
    private TextField nicknameField;
    @FXML
    private ChoiceBox<String> connectionTypeField;
    @FXML
    private Spinner<Integer> playersCountSpinner;
    @FXML
    private TextField sessionDurationField;
    @FXML
    private Slider sessionDurationSlider;

    private AudioClip buttonSelectionSound;
    private AudioClip buttonPressSound;

    private int numberOfPlayers;
    private Duration sessionDuration;

    private Server server;
    private Client client;

    Server getServer() {
        return server;
    }

    public Client getClient() {
        return client;
    }

    public int getNumberOfPlayers() {
        return numberOfPlayers;
    }

    public Duration getSessionDuration() {
        return sessionDuration;
    }

    @FXML
    private void onConnectionTypeChanged() {
        if ("Создать сервер".equals(connectionTypeField.getValue())) {
            playersCountSpinner.setDisable(false);
            sessionDurationField.setDisable(false);
            sessionDurationSlider.setDisable(false);
        } else {
            playersCountSpinner.setDisable(true);
            sessionDurationField.setDisable(true);
            sessionDurationSlider.setDisable(true);
        }
    }

    @FXML
    private void onConfirmButtonClicked() {
        if (ipField.getText().isEmpty() || portField.getText().isEmpty() || nicknameField.getText().isEmpty()) {
            showAlertError("Для подключения к серверу необходимо указать IP-адрес, порт и никнейм.",
                    "Не все обязательные поля заполнены!");
            return;
        }

        String ipAddress = ipField.getText();
        int port;
        try {
            port = Integer.parseInt(portField.getText());
        } catch (NumberFormatException exception) {
            showAlertError("Порт должен быть целочисленным значением.", "Невозможно считать порт!");
            return;
        }
        String nickname = nicknameField.getText();

        if ("Создать сервер".equals(connectionTypeField.getValue())) {
            try {
                sessionDuration = Duration.ofSeconds(Math.round(sessionDurationSlider.getValue()));
                numberOfPlayers = playersCountSpinner.getValue();

                ServerSocket serverSocket = new ServerSocket(port);
                server = new Server(serverSocket);
                server.run();
            } catch (IOException exception) {
                throw new UncheckedIOException(exception);
            }
        }

        try {
            Socket clientSocket = new Socket(ipAddress, port);
            client = new Client(clientSocket, nickname);
            Stage stage = (Stage) confirmButton.getScene().getWindow();
            stage.close();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    @FXML
    private void onSliderValueChanged() {
        sessionDurationField.setText(String.valueOf(Math.round(sessionDurationSlider.getValue())));
    }

    @FXML
    private void onMenuButtonSelected() {
        buttonSelectionSound.play();
    }

    @FXML
    private void onMenuButtonPressed() {
        buttonPressSound.play();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        buttonSelectionSound = new AudioClip(Objects.requireNonNull(
                App.class.getResource(MultiplayerLayout.BUTTON_SELECTION_SOUND_PATH)).toExternalForm()
        );
        buttonSelectionSound.setVolume(0.5);

        buttonPressSound = new AudioClip(Objects.requireNonNull(
                App.class.getResource(MultiplayerLayout.BUTTON_PRESS_SOUND_PATH)).toExternalForm()
        );
        buttonPressSound.setVolume(0.05);
    }

    private void showAlertError(String message, String header) {
        Alert error = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        error.setHeaderText(header);
        LayoutDesigner.setIcon((Stage) error.getDialogPane().getScene().getWindow(), MultiplayerLayout.ICON_PATH);
        error.show();
    }
}
