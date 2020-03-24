package java.client;

import java.text.SimpleDateFormat;
import java.io.IOException;
import java.ConsoleHelper;
import java.util.Calendar;

public class BotClient extends Client {
    private final String botSey =
            "Привет чатику. Я бот. Понимаю команды: дата, день, месяц, год, время, час, минуты, секунды.";

    public static void main(String[] args) {
        BotClient botClient = new BotClient();
        botClient.run();
    }

    public class BotSocketThread extends SocketThread {
        @Override
        protected void clientMainLoop() throws IOException, ClassNotFoundException {
            sendTextMessage(botSey);
            super.clientMainLoop();
        }

        @Override
        protected void processIncomingMessage(String message) {
            ConsoleHelper.writeMessage(message);
            String[] arr = message.split(": ");

            if (arr.length != 2) return;

            String name = arr[0];
            String text = arr[1].trim();
            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat dateFormat = null;

            switch (text) {
                case "дата":
                    dateFormat = new SimpleDateFormat("d.MM.YYYY");
                    break;
                case "время":
                    dateFormat = new SimpleDateFormat("H:mm:ss");
                    break;
                case "месяц":
                    dateFormat = new SimpleDateFormat("MMMM");
                    break;
                case "год":
                    dateFormat = new SimpleDateFormat("YYYY");
                    break;
                case "день":
                    dateFormat = new SimpleDateFormat("d");
                    break;
                case "час":
                    dateFormat = new SimpleDateFormat("H");
                    break;
                case "минуты":
                    dateFormat = new SimpleDateFormat("m");
                    break;
                case "секунды":
                    dateFormat = new SimpleDateFormat("s");
                    break;
            }

            if (dateFormat != null) {
                sendTextMessage(String.format("Информация для %s: %s", name,
                        dateFormat.format(calendar.getTime())));
            }
        }
    }

    @Override
    protected boolean shouldSendTextFromConsole() { return false; }

    @Override
    protected SocketThread getSocketThread() { return new BotSocketThread(); }

    @Override
    protected String getUserName() { return String.format("date_bot_%d", (int) ( Math.random() * 100 )); }
}
