package ru.hse.jigsaw.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.DragEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.Duration;

import org.json.JSONArray;
import org.json.JSONObject;
import ru.hse.jigsaw.App;
import ru.hse.jigsaw.entities.net.Client;
import ru.hse.jigsaw.entities.net.Server;
import ru.hse.jigsaw.entities.shape.Shape;
import ru.hse.jigsaw.entities.shape.Tile;
import ru.hse.jigsaw.entities.stopwatch.Stopwatch;
import ru.hse.jigsaw.utils.LayoutDesigner;
import ru.hse.jigsaw.utils.LayoutInfo;
import ru.hse.jigsaw.utils.PlayerInfo;
import ru.hse.jigsaw.view.LobbyLayout;
import ru.hse.jigsaw.view.MultiplayerLayout;
import ru.hse.jigsaw.view.PrimaryLayout;

import java.net.URL;
import java.text.ParseException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Controller-класс основного окна приложения.
 */
public class PrimaryLayoutController implements Initializable {
    @FXML
    private GridPane layout;

    /**
     * Игровая зона, место генерации фигур.
     */
    @FXML
    private GridPane nestGrid;

    /**
     * Основная игровая зона, в которую перетаскиваются фигуры.
     */
    @FXML
    private GridPane mainGrid;

    /**
     * Место отображения информации о количестве успешных ходов.
     */
    @FXML
    private Label shapesCountLabel;
    private int shapesCount;

    /**
     * Место отображения информации о врошедшем с начала игры времени.
     */
    @FXML
    private Label timeLabel;
    private Stopwatch stopwatch;

    @FXML
    private Button startSingleplayerGameButton;
    @FXML
    private Button startMultiplayerGameButton;
    @FXML
    private Button endGameButton;
    @FXML
    private Button closeAppButton;

    private MediaPlayer player;
    private AudioClip buttonSelectionSound;
    private AudioClip buttonPressSound;
    private AudioClip shapeDropSound;

    /**
     * Фигура, хранящаяся на момент обращения в зоне генерации.
     */
    private Shape nestShape = null;

    private Server server;
    private Client client;

    private int numberOfPlayers;
    private Duration sessionDuration;

    Stage lobby;
    LayoutInfo<LobbyLayoutController> lobbyInfo;

    private PlayerInfo[] playersInfo;

    @FXML
    private void onGameStarted() {
        if (lobby != null && lobby.isShowing()) {
            lobby.close();
        }

        updateGameBoard();

        startSingleplayerGameButton.setDisable(true);
        startMultiplayerGameButton.setDisable(true);
        endGameButton.setDisable(false);

        stopwatch.start();
        startBackgroundMusic();

        Shape shape = LayoutDesigner.generateShape();
        placeNewShape(shape);
    }

    @FXML
    private void onMultiplayerButtonPressed() {
        if (lobby != null && lobby.isShowing()) {
            lobby.close();
        }

        LayoutInfo<MultiplayerLayoutController> pair =
                LayoutDesigner.loadNewScene(LayoutDesigner.GameStyle.DARK_STYLE, "view/multiplayer_layout.fxml");
        MultiplayerLayout multiplayerLayout = new MultiplayerLayout(pair.scene(), pair.controller());

        Stage stage = new Stage();
        stage.initOwner(layout.getScene().getWindow());
        stage.initModality(Modality.WINDOW_MODAL);

        stage.setTitle("Игра по сети");
        LayoutDesigner.setIcon(stage, MultiplayerLayout.ICON_PATH);
        stage.setWidth(MultiplayerLayout.STAGE_WIDTH);
        stage.setHeight(MultiplayerLayout.STAGE_HEIGHT);
        stage.setResizable(false);
        stage.setScene(multiplayerLayout.scene());

        stage.setOnHiding(windowEvent -> {
            if ((client = multiplayerLayout.controller().getClient()) != null) {
                server = multiplayerLayout.controller().getServer();
                if (server != null) {
                    numberOfPlayers = multiplayerLayout.controller().getNumberOfPlayers();
                    sessionDuration = multiplayerLayout.controller().getSessionDuration();
                }

                showLobby();
            }
        });
        stage.show();
    }

    private void onMultiplayerGameStarted() {
        client.removeMessageHandler("clients_list_changed");
        client.addMessageHandler("clients_list_changed", data -> {
            JSONArray activeClients = new JSONObject(data).getJSONArray("clients_list");
            lobbyInfo.controller().updateLobbyAfterGameStarted(activeClients);

            if (server != null) {
                TableView<PlayerInfo> playersTable = lobbyInfo.controller().getPlayersTable();
                if (activeClients.length() < 2 ||
                        playersTable.getItems().stream().noneMatch(info -> "Играет".equals(info.getStatus()))) {
                    playersInfo[0].setStatus("Ожидает результатов");
                    server.sendEvent("game_over");
                }
            }
        });

        client.sendRequest("get_clients_list");
        lobby.setTitle("Лобби (игра началась)");
        endGameButton.setDisable(false);

        updateGameBoard();

        stopwatch.start();
        startBackgroundMusic();

        client.sendRequest("get_shape", Integer.toString(shapesCount));
    }

    private void showLobby() {
        startSingleplayerGameButton.setDisable(true);
        startMultiplayerGameButton.setDisable(true);
        endGameButton.setDisable(false);

        lobbyInfo = LayoutDesigner.loadNewScene(LayoutDesigner.GameStyle.DARK_STYLE, "view/lobby_layout.fxml");
        LobbyLayout lobbyLayout = new LobbyLayout(lobbyInfo.scene(), lobbyInfo.controller());

        if (server != null) {
            prepareServer();
        }
        prepareClient();

        lobby = new Stage();
        lobby.initOwner(layout.getScene().getWindow());
        lobby.setTitle("Лобби (ожидание игроков)");
        LayoutDesigner.setIcon(lobby, LobbyLayout.ICON_PATH);
        lobby.setResizable(false);
        lobby.setScene(lobbyLayout.scene());

        lobby.setOnHiding(windowEvent -> {
            closeConnection();
            onGameOver();
        });
        lobby.show();
    }

    private void prepareClient() {
        client.addMessageHandler("connection_closed", data ->
                Platform.runLater(() -> {
                    if (lobby != null && lobby.isShowing()) {
                        showAlertError("Соединение с сервером разорвано.", "Ошибка подключения!");
                        lobby.close();
                    }
                }));

        client.addMessageHandler("game_started", data ->
                Platform.runLater(() -> {
                    sessionDuration = Duration.parse(new JSONObject(data).getString("session_duration"));
                    onMultiplayerGameStarted();
                }));

        client.addMessageHandler("game_over", data -> {
            Platform.runLater(() -> {
                lobby.setTitle("Лобби (игра окончена)");

                if (!endGameButton.isDisabled()) {
                    if (server != null && Arrays.stream(playersInfo).filter(info ->
                            !"Покинул(-а) лобби".equals(info.getStatus())).count() == 1) {
                        showAlertInfo("Вы - единственный игрок на сервере, игра окончена.", "");
                    } else {
                        showAlertInfo("Время сессии истекло, игра окончена.", "");
                    }
                    onGameOver();
                }
            });
        });

        client.addMessageHandler("get_shape", data -> {
            try {
                Shape shape = Shape.parse(new JSONObject(data).getString("shape"));
                Platform.runLater(() -> placeNewShape(shape));
            } catch (ParseException exception) {
                showAlertError("Полученные с сервера данные повреждены.", "Ошибка представления!");
            }
        });

        client.addMessageHandler("clients_list_changed", data -> {
            lobbyInfo.controller().updateLobbyWhileWaiting(new JSONObject(data).getJSONArray("clients_list"));
            if (server != null) {
                if (lobbyInfo.controller().getPlayersTable().getItems().size() == numberOfPlayers) {
                    server.sendEvent("game_started");
                }
            }
        });

        client.addMessageHandler("get_clients_list", data -> {
            JSONArray jsonArray = new JSONObject(data).getJSONArray("clients_list");

            numberOfPlayers = jsonArray.length();

            playersInfo = new PlayerInfo[numberOfPlayers];
            for (int playerIndex = 0; playerIndex < numberOfPlayers; playerIndex++) {
                String id = jsonArray.getJSONObject(playerIndex).getString("id");
                String nickname = jsonArray.getJSONObject(playerIndex).getString("nickname");
                String status = "Играет";
                Duration gameDuration = Duration.ZERO;
                int placedShapesCount = 0;

                PlayerInfo playerInfo = new PlayerInfo(id, nickname, status, gameDuration, placedShapesCount);
                playersInfo[playerIndex] = playerInfo;
            }

            lobbyInfo.controller().updateInfo(playersInfo);
        });

        client.addMessageHandler("players_info_changed", data -> {
            JSONArray newStatuses = new JSONObject(data).getJSONArray("players_info");
            for (int playerIndex = 0; playerIndex < newStatuses.length(); playerIndex++) {
                playersInfo[playerIndex].setStatus(newStatuses.getJSONObject(playerIndex).getString("status"));
            }

            lobbyInfo.controller().updateInfo(playersInfo);
        });

        client.addMessageHandler("server_shutdown", data -> Platform.runLater(() -> {
            ((Stage) lobbyInfo.controller().getPlayersTable().getScene().getWindow()).close();
            showAlertError("Сервер остановлен.", "Ошибка подключения!");
        }));

        client.register();
        client.addMessageHandler("get_game_duration", data -> Platform.runLater(() -> lobbyInfo.controller()
                .setSessionDuration(Duration.parse(new JSONObject(data).getString("game_duration")))));
        client.sendRequest("get_game_duration");
    }

    private void prepareServer() {
        Shape[] shapes = new Shape[mainGrid.getRowCount() * mainGrid.getColumnCount()];
        for (int index = 0; index < shapes.length; index++) {
            shapes[index] = LayoutDesigner.generateShape();
        }

        server.addEvent("game_over", () -> {
            for (int playerIndex = 0; playerIndex < numberOfPlayers; playerIndex++) {
                if ("Играет".equals(playersInfo[playerIndex].getStatus())) {
                    return new JSONObject();
                }
            }

            PlayerInfo winnerInfo = Arrays.stream(playersInfo).filter(info ->
                    "Ожидает результатов".equals(info.getStatus())).sorted().toList().get(0);
            for (PlayerInfo playerInfo : playersInfo) {
                if (winnerInfo.equals(playerInfo)) {
                    playerInfo.setStatus("Победитель");
                } else {
                    if ("Ожидает результатов".equals(playerInfo.getStatus())) {
                        playerInfo.setStatus("Участник");
                    }
                }
            }

            server.sendEvent("players_info_changed");

            return new JSONObject();
        });

        server.addEvent("game_started", () -> {
            server.closeRegistration();
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            executorService.submit(() -> {
                try {
                    TimeUnit.SECONDS.sleep(sessionDuration.toSeconds());
                    server.sendEvent("game_over");
                } catch (InterruptedException exception) {
                    exception.printStackTrace();
                }
            });
            return new JSONObject().put("session_duration", sessionDuration);
        });

        server.addEvent("players_info_changed", () ->
                new JSONObject().put("players_info", new JSONArray(playersInfo)));

        server.addResponse("get_shape", data -> {
            int figureIndex = Integer.parseInt(data);
            return new JSONObject().put("shape", shapes[figureIndex].getEncodedShape());
        });

        server.addResponse("get_game_duration", data ->
                new JSONObject().put("game_duration", sessionDuration.toString()));

        server.addResponse("player_finished", data -> {
            JSONObject container = new JSONObject(data);

            String playerId = container.getString("id");
            Duration gameDuration = Duration.parse(container.getString("game_duration"));
            int placedShapesCount = container.getInt("placed_shapes_count");

            int playerIndex = -1;
            for (int index = 0; index < numberOfPlayers; index++) {
                if (Objects.equals(playersInfo[index].getId(), playerId)) {
                    playerIndex = index;
                    break;
                }
            }

            if ("Играет".equals(playersInfo[playerIndex].getStatus())) {
                playersInfo[playerIndex].setStatus("Ожидает результатов");
                playersInfo[playerIndex].setGameDuration(gameDuration);
                playersInfo[playerIndex].setPlacedShapesCount(placedShapesCount);

                if (Arrays.stream(playersInfo).allMatch(info -> "Ожидает результатов".equals(info.getStatus()) ||
                        "Покинул(-а) лобби".equals(info.getStatus()))) {
                    server.sendEvent("game_over");
                } else {
                    server.sendEvent("players_info_changed");
                }
            }

            return new JSONObject();
        });
    }

    @FXML
    private void onGameOver() {
        endGameButton.setDisable(true);

        stopBackgroundMusic();

        stopwatch.stop();

        if (nestShape != null) {
            nestShape.setDraggable(false);
        }

        if (client != null && client.isConnected()) {
            JSONObject container = new JSONObject();
            container.put("id", client.getId());
            container.put("game_duration", stopwatch.getDuration().toString());
            container.put("placed_shapes_count", shapesCount);
            client.sendRequest("player_finished", container.toString());
        }

        startSingleplayerGameButton.setDisable(false);
        startMultiplayerGameButton.setDisable(false);
    }

    @FXML
    public void onAppFinish() {
        closeConnection();
        Stage stage = (Stage) closeAppButton.getScene().getWindow();
        stage.close();

        System.exit(0);
    }

    @FXML
    private void onMenuButtonSelected() {
        buttonSelectionSound.play();
    }

    @FXML
    private void onMenuButtonPressed() {
        buttonPressSound.play();
    }

    /**
     * Вызывается в случае успешного завершения операции перемещения фигуры из зоны генерации в основную.
     */
    private void onShapeDragDone(DragEvent event) {
        shapeDropSound.play();

        /* Генерация новой фигуры. */
        if (client != null) {
            client.sendRequest("get_shape", Integer.toString(shapesCount + 1));
        } else {
            Shape shape = LayoutDesigner.generateShape();
            placeNewShape(shape);
        }
    }

    private void placeNewShape(Shape shape) {
        shape.setDraggable(true);

        updateGameGrid(nestGrid, false);

        LayoutDesigner.placeShape(shape, nestGrid);
        setShapesCount(shapesCount + 1);
        nestShape = shape;
    }

    /**
     * Сбрасывает визуальные составляющие во всех зонах приложения к состоянию, ожидаемому на момент запуска приложения.
     */
    private void updateGameBoard() {
        if (stopwatch != null) {
            stopwatch.reset();
        }

        setShapesCount(0);
        timeLabel.setText(DateTimeFormatter.ofPattern(PrimaryLayout.TIME_PATTERN).format(LocalTime.of(0, 0, 0)));
        updateGameGrid(mainGrid, true);
        updateGameGrid(nestGrid, false);
    }

    /**
     * Меняет информацию о количестве успешных совершённых пользователем ходов и обновляет визуальную часть.
     *
     * @param shapesCount количество размещённых в основной игровой зоне фигур;
     */
    private void setShapesCount(int shapesCount) {
        shapesCountLabel.setText(String.valueOf(shapesCount));
        this.shapesCount = shapesCount;
    }

    /**
     * Очищает игровую зону и затем заполняет её пустыми ячейками.
     *
     * @param gameGrid  очищаемая игровая зона;
     * @param allowDrop может ли эта зона являться конечной целью события "Drag and drop";
     */
    private void updateGameGrid(GridPane gameGrid, boolean allowDrop) {
        gameGrid.getChildren().clear();

        for (int row = 0; row < gameGrid.getRowCount(); row++) {
            for (int column = 0; column < gameGrid.getColumnCount(); column++) {
                Tile unfilledTile = new Tile(row, column, null, LayoutDesigner.EMPTY_CELL_STYLE_CLASS);

                if (allowDrop) {
                    unfilledTile.setOnDragOver(unfilledTile::onDragOver);
                    unfilledTile.setOnDragExited(unfilledTile::onDragExited);
                    unfilledTile.setOnDragDropped(unfilledTile::onDragDropped);
                    unfilledTile.setOnDragDone(this::onShapeDragDone);
                }

                gameGrid.add(unfilledTile, column, row);
            }
        }
    }

    private void startBackgroundMusic() {
        player.play();
    }

    private void stopBackgroundMusic() {
        player.stop();
    }

    private void showAlertError(String message, String header) {
        Alert error = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        error.setHeaderText(header);
        LayoutDesigner.setIcon((Stage) error.getDialogPane().getScene().getWindow(), PrimaryLayout.ICON_PATH);
        error.show();
    }

    private void showAlertInfo(String message, String header) {
        Alert info = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        info.setHeaderText(header);
        LayoutDesigner.setIcon((Stage) info.getDialogPane().getScene().getWindow(), PrimaryLayout.ICON_PATH);
        info.show();
    }

    private void closeConnection() {
        if (client != null && client.isConnected()) {
            client.closeConnection();
            client = null;
        }

        if (server != null && server.isActive()) {
            server.shutdown();
            server = null;
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        updateGameBoard();

        stopwatch = new Stopwatch(timeLabel, PrimaryLayout.TIME_PATTERN);

        Media backgroundMusic = new Media(Objects.requireNonNull(
                App.class.getResource(PrimaryLayout.BACKGROUND_MUSIC_PATH)).toExternalForm()
        );
        player = new MediaPlayer(backgroundMusic);
        player.setOnEndOfMedia(() -> player.seek(javafx.util.Duration.ZERO));
        player.setVolume(0.2);

        buttonSelectionSound = new AudioClip(Objects.requireNonNull(
                App.class.getResource(PrimaryLayout.BUTTON_SELECTION_SOUND_PATH)).toExternalForm()
        );
        buttonSelectionSound.setVolume(0.5);

        buttonPressSound = new AudioClip(Objects.requireNonNull(
                App.class.getResource(PrimaryLayout.BUTTON_PRESS_SOUND_PATH)).toExternalForm()
        );
        buttonPressSound.setVolume(0.05);

        shapeDropSound = new AudioClip(Objects.requireNonNull(
                App.class.getResource(PrimaryLayout.SHAPE_DROP_SOUND_PATH)).toExternalForm()
        );
        shapeDropSound.setVolume(0.1);
    }
}
