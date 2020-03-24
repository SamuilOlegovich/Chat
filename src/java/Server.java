package java;

import java.util.concurrent.ConcurrentHashMap;
import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.net.ServerSocket;
import java.io.IOException;
import java.net.Socket;
import java.util.Map;

import static java.MessageType.*;

public class Server {
            // для отправки сразу всем подключенным сообщения
    private static Map<String, Connection> connectionMap = new ConcurrentHashMap<>();
    private static ServerSocket serverSocket;       // серверсокет
    private static BufferedWriter out;              // поток записи в сокет
    private static BufferedReader in;               // поток чтения из сокета
    private static Socket socket;                   // сокет для общения

    public static void main(String[] args) throws IOException {
        int port = ConsoleHelper.readInt();

        try {
            serverSocket = new ServerSocket(port);
            while (true) {
                socket = serverSocket.accept();
                Handler handler = new Handler(socket);
                handler.start();
            }
        } catch (IOException e) {
            ConsoleHelper.writeMessage (e + "Произошла ошибка.");
            serverSocket.close();
        }
    }

    private static class Handler extends Thread {
        private Socket socket;

        public Handler(Socket socket) { this.socket = socket; }

        private String serverHandshake(Connection connection) throws IOException, ClassNotFoundException {
            String name;
            connection.send(new Message(NAME_REQUEST, "Превед, медвед. Как звать?"));

            while (true) {
                Message reply = connection.receive();
                name = reply.getData();
                if (reply.getType() == USER_NAME) {
                    if (connectionMap.get(name) == null && !name.equals("")) {
                        connectionMap.put(name, connection);
                        connection.send(new Message(NAME_ACCEPTED, name + " ты принят!"));
                        break;
                    }
                } else {
                    connection.send(new Message(NAME_REQUEST, "Что-то пошло не так, пропробуем еще раз.\n"
                            + "Превед, медвед! Как звать?"));
                }
            } return name;
        }

        private void notifyUsers(Connection connection, String userName) throws IOException, ClassNotFoundException {
            for (String name : connectionMap.keySet()) {
                if (!(name.equals(userName))) {
                    connection.send(new Message(MessageType.USER_ADDED, name));
                }
            }
        }

        private void serverMainLoop(Connection connection, String userName) throws IOException, ClassNotFoundException {
            String outMessage;
            while (true) {
                Message reply = connection.receive();
                if (reply.getType() == TEXT) {
                    outMessage = userName + ": " + reply.getData();
                    sendBroadcastMessage(new Message(TEXT, outMessage));
                } else ConsoleHelper.writeMessage("Error!");
            }
        }

        public void run() {
            String userName;
            String address = socket.getRemoteSocketAddress().toString();
            Connection connection;

            ConsoleHelper.writeMessage(String.format("Было установлено соединение с удаленным адресом: %s", address));

            try {
                connection = new Connection(socket);
                    userName = serverHandshake(connection);
                    sendBroadcastMessage(new Message(USER_ADDED, userName));
                    notifyUsers(connection, userName);
                    serverMainLoop(connection, userName);
                if(userName != null) {
                    connectionMap.remove(userName);
                    sendBroadcastMessage(new Message(USER_REMOVED, userName));
                }
                ConsoleHelper.writeMessage(String.format("Соединение с %s закрыто", address));
            } catch (ClassNotFoundException e) {ConsoleHelper.writeMessage("Ошибка - " + e);}
            catch (IOException e) {ConsoleHelper.writeMessage("Ошибка при создании нового соединения.");}
        }
    }

    public static void sendBroadcastMessage(Message message) {
        for (String name : connectionMap.keySet()) {
            try {
                connectionMap.get(name).send(message);
            }
            catch (ClassNotFoundException e) {
                ConsoleHelper.writeMessage(String.format("Не могу отправить сообщение %s", name));
            }
            catch (IOException e) {
                ConsoleHelper.writeMessage(String.format("Не могу отправить сообщение %s", name));
            }
        }
    }
}
