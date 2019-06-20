import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.replykeyboard.ForceReplyKeyboard;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import org.telegram.telegrambots.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.logging.BotLogger;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InvalidObjectException;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

public class Bot extends TelegramLongPollingBot {
    private static final String LOGTAG = "CHANNELHANDLERS";

    private static final int WAITINGCHANNEL = 1;

    public Timer timer;
    private ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();

    private static final String HELP_TEXT = "Send me the channel username where you added me as admin.";
    private static final String CANCEL_COMMAND = "/stop";
    private static final String AFTER_CHANNEL_TEXT = "A message to provided channel will be sent if the bot was added to it as admin.";
    private static final String WRONG_CHANNEL_TEXT = "Wrong username, please remember to add *@* before the username and send only the username.";
    private static final String CHANNEL_MESSAGE_TEXT = "This message was sent by *@updateschannelbot*. Enjoy!";
    private static final String ERROR_MESSAGE_TEXT = "There was an error sending the message to channel *%s*, the error was: ```%s```";

    private final ConcurrentHashMap<Integer, Integer> userState = new ConcurrentHashMap<Integer, Integer>();
    private final ArrayList<String> phrases = new ArrayList<String>();

    /**
     * Метод для приема сообщений.
     *
     * @param update Содержит сообщение от пользователя.
     */
    @Override
    public void onUpdateReceived(Update update) {
        try {
            Message message = update.getMessage();
            if (message != null && message.hasText()) {
                try {
                    handleIncomingMessage(message);
                } catch (InvalidObjectException e) {
                    BotLogger.severe(LOGTAG, e);
                }
            }
        } catch (Exception e) {
            BotLogger.error(LOGTAG, e);
        }
    }

    public Bot() {
        initPhrases();
        timer = new Timer();

        Connection conn = null;
            Statement stmt = null;

        String CREATE_TABLE_SQL="create table test (id int, value varchar(100));";

            try {

              conn = getConnection();
              stmt = conn.createStatement();

              //stmt.executeUpdate(CREATE_TABLE_SQL);

              System.out.println("Table created");

            } catch (Exception e) {
              e.printStackTrace();
            } finally {
              try {
                // Close connection
                if (stmt != null) {
                  stmt.close();
                }
                if (conn != null) {
                  conn.close();
                }
              } catch (Exception e) {
                e.printStackTrace();
              }
            }

    }

    private static Connection getConnection() throws URISyntaxException, SQLException {
        String dbUrl = System.getenv("JDBC_DATABASE_URL");
        return DriverManager.getConnection(dbUrl);
    }

    private void handleIncomingMessage(Message message) throws InvalidObjectException {
        sendMessage(message);


        //if(message.getText().contains("@Vuster_bot")) {



            //timer = new Timer();
            //timer.schedule(new CrunchifyReminder(message.getChatId()), 0, 1 * 1000);





        //}
        //int state = userState.getOrDefault(message.getFrom().getId(), 0);
        //        switch (state) {
        //            case WAITINGCHANNEL:
        //                onWaitingChannelMessage(message);
        //                break;
        //            default:
        //                sendHelpMessage(message.getChatId(), message.getMessageId(), null);
        //                userState.put(message.getFrom().getId(), WAITINGCHANNEL);
        //                break;
        //        }
    }

    private void sendMessage(Message message) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(message.getChatId());

        String text = message.getText();
        sendMessage.setReplyMarkup(replyKeyboardMarkup);

        try {
            sendMessage.setText(getMessage(text));
            execute(sendMessage);
        } catch (TelegramApiException e) {
            BotLogger.error(LOGTAG, e);
        }
    }

    private String getMessage(String msg) {
        ArrayList<KeyboardRow> keyboard = new ArrayList<KeyboardRow>();
        KeyboardRow keyboardRowFirst = new KeyboardRow();
        //KeyboardRow keyboardRowSecond = new KeyboardRow();

        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(false);
        keyboard.clear();

        if(msg.equalsIgnoreCase(("Меню"))) {
            keyboardRowFirst.clear();
            keyboardRowFirst.add("Создать напоминание");
            keyboard.add(keyboardRowFirst);
            replyKeyboardMarkup.setKeyboard(keyboard);
            return "Выбрать";
        }
        return "";
    }

    class CrunchifyReminder extends TimerTask {
        private Long chatId;
        int loop;

        public CrunchifyReminder(Long chatId) {
            this.chatId = chatId;
            loop = 5;
        }

        public void run() {
            if (loop > 0) {
                //System.out.format("Knock Knock..!\n");
                sendRandomMessage(chatId);
                loop--;
            } else {
                System.out.println("That's it.. Done.......!!!!!");
                timer.cancel();
                timer.purge();
            }
        }
    }

    private void onWaitingChannelMessage(Message message) throws InvalidObjectException {
        try {
            if (message.getText().equals(CANCEL_COMMAND)) {
                userState.remove(message.getFrom().getId());
                sendHelpMessage(message.getChatId(), message.getMessageId(), null);
            } else {
                if (message.getText().startsWith("@") && !message.getText().trim().contains(" ")) {
                    execute(getMessageToChannelSent(message));
                    sendMessageToChannel(message.getText(), message);
                    userState.remove(message.getFrom().getId());
                } else {
                    execute(getWrongUsernameMessage(message));
                }
            }
        } catch (TelegramApiException e) {
            BotLogger.error(LOGTAG, e);
        }
    }

    private void sendMessageToChannel(String username, Message message) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(username.trim());

        sendMessage.setText(CHANNEL_MESSAGE_TEXT);
        sendMessage.enableMarkdown(true);

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            sendErrorMessage(message, e.getMessage());
        }
    }

    private void sendErrorMessage(Message message, String errorText) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(message.getChatId());
        sendMessage.setReplyToMessageId(message.getMessageId());

        sendMessage.setText(String.format(ERROR_MESSAGE_TEXT, message.getText().trim(), errorText.replace("\"", "\\\"")));
        sendMessage.enableMarkdown(true);

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            BotLogger.error(LOGTAG, e);
        }
    }

    private static SendMessage getWrongUsernameMessage(Message message) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(message.getChatId());
        sendMessage.setReplyToMessageId(message.getMessageId());

        ForceReplyKeyboard forceReplyKeyboard = new ForceReplyKeyboard();
        forceReplyKeyboard.setSelective(true);
        sendMessage.setReplyMarkup(forceReplyKeyboard);

        sendMessage.setText(WRONG_CHANNEL_TEXT);
        sendMessage.enableMarkdown(true);
        return sendMessage;
    }

    private static SendMessage getMessageToChannelSent(Message message) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(message.getChatId());
        sendMessage.setReplyToMessageId(message.getMessageId());

        sendMessage.setText(AFTER_CHANNEL_TEXT);
        return sendMessage;
    }

    private void sendHelpMessage(Long chatId, Integer messageId, ReplyKeyboardMarkup replyKeyboardMarkup) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId);
        sendMessage.setReplyToMessageId(messageId);
        if (replyKeyboardMarkup != null) {
            sendMessage.setReplyMarkup(replyKeyboardMarkup);
        }

        sendMessage.setText(HELP_TEXT);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            BotLogger.error(LOGTAG, e);
        }
    }

    private void sendRandomMessage(Long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId);

        String msg = phrases.get(new Random().nextInt(phrases.size()));
        //String msg = "AAAaaaaAAAAaAAAA";
        sendMessage.setText(msg);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            BotLogger.error(LOGTAG, e);
        }
    }

    /**
     * Метод для настройки сообщения и его отправки.
     *
     * @param chatId id чата
     * @param s      Строка, которую необходимот отправить в качестве сообщения.
     */
    public synchronized void sendMsg(String chatId, String s) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId);
        sendMessage.setText(s);
        try {
            sendMessage(sendMessage);
        } catch (TelegramApiException e) {
            //log.log(Level.SEVERE, "Exception: ", e.toString());
        }
    }

    /**
     * Метод возвращает имя бота, указанное при регистрации.
     *
     * @return имя бота
     */
    @Override
    public String getBotUsername() {
        return "Vuster_bot";
    }

    /**
     * Метод возвращает token бота для связи с сервером Telegram
     *
     * @return token для бота
     */
    @Override
    public String getBotToken() {
        return "872913315:AAH_yRWw6jb-Th_kW9rdXnzOd3iBb9wkj1M";
    }

    private void initPhrases() {


        try {
            InputStream ruleSet = ClassLoader.getSystemResourceAsStream("a1.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(ruleSet));
            StringBuilder out = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                phrases.add(line);
            }
            //System.out.println(out.toString());   //Prints the string content read from input stream
            reader.close();
        }catch(Exception e) {
            e.printStackTrace();
        }

//        URL resource = getClass().getClassLoader().getResource("a1.txt");
//        if (resource == null) {
//            throw new IllegalArgumentException("file is not found!");
//        } else {
//            return new File(resource.getFile());
//        }

//        URL url = getClass().getClassLoader().getResource("a1.txt");
//        System.out.println("=================== path = " + url.getPath());
//        System.out.println("=================== file = " + url.getFile());
//
//try {
//    InputStream input = new BufferedInputStream(new FileInputStream(url.getFile()));
//    byte[] buffer = new byte[8192];
//
//    try {
//        for (int length = 0; (length = input.read(buffer)) != -1; ) {
//            System.out.write(buffer, 0, length);
//        }
//    } finally {
//        input.close();
//    }
//}catch(Exception e){
//    e.printStackTrace();
//}


        /*File file = new File("src/resources/a1.txt");

//        File file = new File(url.getFile());
        //File file = new File(url.getPath());
        try {
            Scanner s = new Scanner(file);
            while (s.hasNext()) {
                phrases.add(s.next());
            }
            s.close();
        } catch (Exception e) {
            e.printStackTrace();
        }*/

        //System.out.println(phrases.toString());
        System.out.println(phrases.size());
    }

    public static void main(String[] args) {
        System.out.println("=================== START =============");
        //new Bot().initPhrases();


        ApiContextInitializer.init();
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi();
        try {
            telegramBotsApi.registerBot(new Bot());
        } catch (TelegramApiRequestException e) {
            e.printStackTrace();
        }
    }
}