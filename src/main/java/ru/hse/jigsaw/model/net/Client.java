package ru.hse.jigsaw.model.net;

import org.json.JSONObject;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Client {
    private static final Logger logger = Logger.getLogger(Server.class.getName());

    private String id;
    private final Socket socket;
    private final String nickname;
    private PrintWriter requestWriter;
    private BufferedReader responseReader;

    private ExecutorService messageHandlerRunner;
    private final Map<String, Consumer<String>> messageHandler;

    public Client(Socket socket, String nickname) {
        try {
            responseReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            requestWriter = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

            messageHandlerRunner = Executors.newSingleThreadExecutor();
            messageHandlerRunner.submit(this::processMessages);
        } catch (IOException exception) {
            closeConnection();
        }

        this.socket = socket;
        this.nickname = nickname;
        messageHandler = new HashMap<>() {{
            put("registration", body -> {
                if ("Регистрация закрыта".equals(body)) {
                    closeConnection();
                } else {
                    Client.this.id = new JSONObject(body).getString("id");
                }
            });
        }};
    }

    public String getId() {
        return id;
    }

    public String getNickname() {
        return nickname;
    }

    public boolean isConnected() {
        return !socket.isClosed();
    }

    public void addMessageHandler(String key, Consumer<String> handler) {
        messageHandler.put(key, handler);
    }

    public void removeMessageHandler(String key) {
        messageHandler.remove(key);
    }

    public void register() {
        sendRequest("registration", nickname);
    }

    public void sendRequest(String requestKey) {
        sendRequest(requestKey, "");
    }

    public void sendRequest(String requestKey, String requestBody) {
        JSONObject request = new JSONObject();
        request.put("key", requestKey);
        request.put("body", requestBody);
        requestWriter.println(request);
    }

    private void processMessages() {
        try {
            while (!socket.isClosed()) {
                String message = responseReader.readLine();
                JSONObject jsonObject = new JSONObject(message);

                String key = jsonObject.getString("key");
                if (messageHandler.containsKey(key)) {
                    messageHandler.get(key).accept(jsonObject.get("body").toString());
                }
            }
        } catch (SocketException | NullPointerException exception) {
            logger.log(Level.INFO, "Разорвано соединение с сервером.");
        } catch (IOException exception) {
            logger.log(Level.SEVERE, "Возникла исключительная ситуация при обработке запроса:", exception);
        } finally {
            if (messageHandler.containsKey("connection_closed")) {
                messageHandler.get("connection_closed").accept(Instant.now().toString());
            }
            closeConnection();
        }
    }

    public void closeConnection() {
        messageHandlerRunner.shutdownNow();

        try {
            if (socket != null) {
                socket.close();
            }
            if (responseReader != null) {
                responseReader.close();
            }
            if (requestWriter != null) {
                requestWriter.close();
            }
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
