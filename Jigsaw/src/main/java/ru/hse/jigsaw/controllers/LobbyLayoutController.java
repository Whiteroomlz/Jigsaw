package ru.hse.jigsaw.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import ru.hse.jigsaw.utils.PlayerInfo;
import ru.hse.jigsaw.view.LobbyLayout;

import java.net.URL;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class LobbyLayoutController implements Initializable {
    @FXML
    private TableView<PlayerInfo> listOfPlayers;
    @FXML
    private TableColumn<PlayerInfo, String> nicknameColumn;
    @FXML
    private TableColumn<PlayerInfo, String> idColumn;
    @FXML
    private TableColumn<PlayerInfo, String> statusColumn;
    @FXML
    private Label sessionDurationLabel;

    public TableView<PlayerInfo> getPlayersTable() {
        return listOfPlayers;
    }

    public void setSessionDuration(Duration sessionDuration) {
        String sessionDurationText = LocalTime.of(0, 0, 0).plusSeconds(
                sessionDuration.toSeconds()).format(DateTimeFormatter.ofPattern(LobbyLayout.TIME_PATTERN));
        sessionDurationLabel.setText(String.format("ВРЕМЯ СЕССИИ:\t%s", sessionDurationText));
    }

    public void updateLobbyWhileWaiting(JSONArray activeClients) {
        List<PlayerInfo> items = new ArrayList<>();
        for (int index = 0; index < activeClients.length(); index++) {
            JSONObject activeClientContainer = activeClients.getJSONObject(index);

            String id = activeClientContainer.getString("id");
            String nickname = activeClientContainer.getString("nickname");
            String status = "Ожидает старта";

            items.add(new PlayerInfo(id, nickname, status, Duration.ZERO, 0));
        }

        listOfPlayers.setItems(FXCollections.observableList(items));
    }

    public void updateLobbyAfterGameStarted(JSONArray activeClients) {
        Set<String> activeIds = new HashSet<>();
        for (int activeClientIndex = 0; activeClientIndex < activeClients.length(); activeClientIndex++) {
            activeIds.add(activeClients.getJSONObject(activeClientIndex).getString("id"));
        }

        int registeredPlayersCount = listOfPlayers.getItems().size();
        for (int playerIndex = 0; playerIndex < registeredPlayersCount; playerIndex++) {
            if (!activeIds.contains(listOfPlayers.getItems().get(playerIndex).getId())) {
                String newStatus = "Покинул(-а) лобби";
                PlayerInfo info = listOfPlayers.getItems().get(playerIndex);
                info.setStatus(newStatus);
                listOfPlayers.getItems().set(playerIndex, info);
            }
        }
    }

    public void updateInfo(PlayerInfo[] playersInfo) {
        for (int playerIndex = 0; playerIndex < playersInfo.length; playerIndex++) {
            listOfPlayers.getItems().set(playerIndex, playersInfo[playerIndex]);
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        listOfPlayers.setFocusModel(null);
        nicknameColumn.setCellValueFactory(new PropertyValueFactory<>("nickname"));
        nicknameColumn.setSortable(false);
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        idColumn.setSortable(false);
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusColumn.setSortable(false);
    }
}
