package ru.hse.jigsaw.entities.net;

import javafx.collections.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server {
    private static final Logger logger = Logger.getLogger(Server.class.getName());

    private int connectionsCount = 0;
    private boolean couldRegister = true;

    private final ServerSocket serverSocket;
    private final ObservableMap<String, ClientHandler> activeConnections;

    private final ExecutorService clientsThreadPool;
    private final ExecutorService serverExecutor;

    private final Map<String, Supplier<JSONObject>> events;
    private final Map<String, Function<String, JSONObject>> responses;

    public Server(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;

        activeConnections = FXCollections.observableHashMap();
        activeConnections.addListener((MapChangeListener<? super String, ? super ClientHandler>) change ->
                sendEvent("clients_list_changed"));

        clientsThreadPool = Executors.newCachedThreadPool();
        serverExecutor = Executors.newSingleThreadExecutor();

        events = new HashMap<>() {{
            put("server_shutdown", () -> new JSONObject().put("shutdown_timestamp", Instant.now().toString()));
            put("clients_list_changed", () -> new JSONObject()
                    .put("clients_list", new JSONArray(activeConnections.values())));
        }};
        responses = new HashMap<>() {{
            put("get_clients_list", body -> new JSONObject()
                    .put("clients_list", new JSONArray(activeConnections.values())));
        }};
    }

    public boolean isActive() {
        return !serverSocket.isClosed();
    }

    public void closeRegistration() {
        couldRegister = false;
    }

    public void openRegistration() {
        couldRegister = true;
    }

    public void addEvent(String eventKey, Supplier<JSONObject> bodyGenerator) {
        events.put(eventKey, bodyGenerator);
    }

    public void removeEvent(String eventKey) {
        events.remove(eventKey);
    }

    public void addResponse(String responseKey, Function<String, JSONObject> bodyGenerator) {
        responses.put(responseKey, bodyGenerator);
    }

    public void removeResponse(String responseKey) {
        responses.remove(responseKey);
    }

    public void run() {
        serverExecutor.submit(() -> {
            try {
                logger.log(Level.INFO, "Сервер запущен.");

                while (!serverSocket.isClosed()) {
                    Socket socket = serverSocket.accept();
                    clientsThreadPool.execute(new ClientHandler(socket, this));
                }
            } catch (IOException exception) {
                logger.log(Level.SEVERE, "Возникла исключительная ситуация:", exception);
                throw new UncheckedIOException(exception);
            } finally {
                sendEvent("server_shutdown");
                shutdown();
                logger.log(Level.INFO, "Сервер остановлен.");
            }
        });
    }

    public void shutdown() {
        try {
            serverExecutor.shutdownNow();
            clientsThreadPool.shutdownNow();

            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException exception) {
            logger.log(Level.SEVERE, "Возникла исключительная ситуация при остановке сервера:", exception);
            throw new UncheckedIOException(exception);
        }
    }

    private void registerClient(ClientHandler connection) {
        connection.id = Integer.toString(++connectionsCount);
        activeConnections.put(connection.id, connection);
    }

    public void sendEvent(String eventKey) {
        JSONObject message = new JSONObject();

        message.put("key", eventKey);
        message.put("body", events.get(eventKey).get());

        for (ClientHandler clientHandler : activeConnections.values()) {
            clientHandler.messageWriter.println(message);
        }
    }

    private JSONObject getResponse(JSONObject request) {
        String requestKey = request.getString("key");
        String requestBody = request.getString("body");

        JSONObject message = new JSONObject();

        message.put("key", requestKey);
        message.put("body", responses.get(requestKey).apply(requestBody));

        return message;
    }

    public static class ClientHandler implements Runnable {
        private String id;
        private Server server;
        private String nickname;
        private Socket clientSocket;
        private PrintWriter messageWriter;
        private BufferedReader requestReader;

        ClientHandler(Socket clientSocket, Server server) {
            try {
                this.server = server;
                this.clientSocket = clientSocket;

                requestReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                messageWriter = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()), true);

                JSONObject registrationRequest = new JSONObject(requestReader.readLine());
                if (server.couldRegister) {
                    nickname = registrationRequest.getString("body");
                    server.registerClient(this);
                    JSONObject message = new JSONObject();
                    message.put("key", "registration");
                    message.put("body", new JSONObject().put("id", id));
                    messageWriter.println(message);
                } else  {
                    messageWriter.println(new JSONObject().put("key", "registration")
                            .put("body", "Регистрация закрыта."));
                    closeConnection();
                }
            } catch (IOException exception) {
                logger.log(Level.SEVERE, "Возникла исключительная ситуация:", exception);
                closeConnection();
            }
        }

        public String getId() {
            return id;
        }

        public String getNickname() {
            return nickname;
        }

        @Override
        public void run() {
            processRequests();
        }

        private void processRequests() {
            try {
                while (!clientSocket.isClosed()) {
                    JSONObject request = new JSONObject(requestReader.readLine());
                    messageWriter.println(server.getResponse(request));
                }
            } catch (SocketException | NullPointerException exception) {
                logger.log(Level.INFO, String.format("Пользователь %s#%s потерял соединение с сервером", nickname, id));
            } catch (IOException exception) {
                logger.log(Level.SEVERE, "Возникла исключительная ситуация при обработке запроса:", exception);
            } finally {
                closeConnection();
            }
        }

        private void closeConnection() {
            server.activeConnections.remove(id);

            try {
                if (clientSocket != null) {
                    clientSocket.close();
                }
                if (requestReader != null) {
                    requestReader.close();
                }
                if (messageWriter != null) {
                    messageWriter.close();
                }
            } catch (IOException exception) {
                logger.log(Level.SEVERE, "Возникла исключительная ситуация при закрытии соединения:", exception);
                throw new UncheckedIOException(exception);
            }
        }
    }
}