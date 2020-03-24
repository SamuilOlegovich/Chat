package java.client;



import java.io.IOException;
import java.ConsoleHelper;
import java.net.Socket;
import java.Connection;
import java.Message;

import static java.MessageType.*;


public class Client {
    private volatile boolean clientConnected = false;
    protected Connection connection;

    public static void main(String[] args) {
        Client client = new Client();
        client.run();
    }

    public class SocketThread extends Thread {

        protected void processIncomingMessage(String message) { ConsoleHelper.writeMessage(message); }

        protected void informAboutAddingNewUser(String userName) {
            ConsoleHelper.writeMessage(String.format("%s присоединился к чату.", userName));
        }

        protected void informAboutDeletingNewUser(String userName) {
            ConsoleHelper.writeMessage(String.format("%s вышел и чатф.", userName));
        }

        protected void notifyConnectionStatusChanged(boolean clientConnected) {
            Client.this.clientConnected = clientConnected;
            synchronized (Client.this) { Client.this.notify(); }
        }

        protected void clientHandshake() throws IOException, ClassNotFoundException {

            Message sms;

            while (true) {
                sms = connection.receive();

                if (sms != null) {

                    if (sms.getType() == NAME_REQUEST) {
                        String userName = getUserName();
                        connection.send(new Message(USER_NAME, getUserName()));
                    } else if (sms.getType() == NAME_ACCEPTED) {
                        notifyConnectionStatusChanged(true);
                        break;
                    } else throw new IOException("Unexpected MessageType");
                }
            }
        }

        protected void clientMainLoop() throws IOException, ClassNotFoundException {

            Message sms;
            while (true) {
                sms = connection.receive();
                if (sms != null) {

                    if (sms.getType() == TEXT) processIncomingMessage(sms.getData());
                    else if (sms.getType() == USER_ADDED) informAboutAddingNewUser(sms.getData());
                    else if (sms.getType() == USER_REMOVED) informAboutDeletingNewUser(sms.getData());
                    else throw new IOException("Unexpected MessageType");
                }
            }
        }

        @Override
        public void run() {
            String serverAddress = getServerAddress();
            int serverPort = getServerPort();
            Socket socket;
            Connection connection;

            try {
                socket = new Socket(serverAddress, serverPort);
                connection = new Connection(socket);
                Client.this.connection = connection;
                clientHandshake();
                clientMainLoop();
            } catch (ClassNotFoundException e) {
                notifyConnectionStatusChanged(false);
                ConsoleHelper.writeMessage("Что-то пошло не так :( ClassNotFoundException");
            } catch (IOException e) {
                notifyConnectionStatusChanged(false);
                ConsoleHelper.writeMessage("Что-то пошло не так :( IOException");
            }

            while (clientConnected) {
                String message = ConsoleHelper.readString();

                if (message.equals("exit")) break;
                else if (shouldSendTextFromConsole()) sendTextMessage(message);
            }
            clientConnected = false;
        }
    }


    protected boolean shouldSendTextFromConsole() { return true; }

    protected void sendTextMessage(String text) {
        try { connection.send(new Message(TEXT, text)); }
        catch (ClassNotFoundException e) { clientConnected = false; }
        catch (IOException e) { clientConnected = false; }
    }

    protected int getServerPort() {
        ConsoleHelper.writeMessage("Введите номер порта сервера:");
        return ConsoleHelper.readInt();
    }

    protected String getUserName() {
        ConsoleHelper.writeMessage("Введите ваше имя:");
        return ConsoleHelper.readString();
    }

    protected SocketThread getSocketThread() { return new SocketThread(); }

    protected String getServerAddress() {
        ConsoleHelper.writeMessage("Введите адрес сервера:");
        return ConsoleHelper.readString();
    }

    public void run() {
        SocketThread socketThread = getSocketThread();
        socketThread.setDaemon(true);
        socketThread.start();

        try { synchronized (this) { this.wait(); }
        } catch (InterruptedException e) { ConsoleHelper.writeMessage("При работе клиента возникла ошибка"); }

        if (clientConnected) ConsoleHelper.writeMessage("Соединение установлено. Для выхода наберите команду 'exit'.");
        else ConsoleHelper.writeMessage("Произошла ошибка во время работы клиента.");

        while (clientConnected) {
            String message = ConsoleHelper.readString();

            if (message.equals("exit")) break;
            else if (shouldSendTextFromConsole()) sendTextMessage(message);
        }
        clientConnected = false;
    }
}
