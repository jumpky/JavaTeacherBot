package com.example.JavaTeacherBot.service;

import com.example.JavaTeacherBot.config.BotConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot{
    final BotConfig config;
    final DatabaseManager databaseManager;
    static final String HELP_TEXT = "Этот бот создан для обучения вас java.\n\n" +
            " \"Вы можете выполнять команды из главного меню слева или набрав команду вручную\n\n" +
            "/start - Получите приветственное сообщение\n\n" +
            "/algorithm - Меню алгоритмов\n\n" +
            "/help - Информация о том, как использовать этого бота\n\n" +
            "/settings - Установите свои параметры";

    public TelegramBot(BotConfig config, DatabaseManager databaseManager) {
        this.config = config;
        this.databaseManager = databaseManager;

        ArrayList<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "Получите приветственное сообщение"));
        listOfCommands.add(new BotCommand("/algorithm", "Меню алгоритмов"));
        listOfCommands.add(new BotCommand("/help", "Информация о том, как использовать этого бота"));
        listOfCommands.add(new BotCommand("/settings", "Установите свои параметры"));

        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        }
        catch (TelegramApiException e) {
            log.error("Error setting bot's command list: " + e.getMessage());
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            switch (messageText) {
                case "/start":
                    startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                    break;
                case "/algorithm":
                    sendAlgorithmList(chatId);
                    break;
                case "/help":
                    sendMessage(chatId, HELP_TEXT);
                    break;
                case "/settings":
                    sendSettingsMenu(chatId);
                    break;
                // Команды
            }
        }
        else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            if (callbackData.startsWith("set_language_")) {
                String language = callbackData.split("_")[2];
                databaseManager.updateUserLanguage(chatId, language);
                sendMessage(chatId, "Теперь ваш предпочтительный язык сменён на: " + language);
            }
            else {
                String algorithmName = callbackData; // callbackData должен быть именем алгоритма
                String language = databaseManager.getUserLanguage(chatId);
                Algorithm algorithm = databaseManager.getAlgorithm(algorithmName, language);

                if (algorithm != null) {
                    try {
                        sendAlgorithmInfo(chatId, algorithm);
                    }
                    catch (IOException e) {
                        log.error("Error sending algorithm info: " + e.getMessage());
                        sendInvalidAlgorithmMessage(chatId);
                    }
                }
                else {
                    sendInvalidAlgorithmMessage(chatId);
                }
            }
        }
    }
    private void startCommandReceived(long chatId, String name){

        String answer = "Привет, " + name + ", приятно познакомиться с вами!";
        log.info("Replied to user: " + name);

        sendMessage(chatId,answer);
    }

    private void sendAlgorithmInfo(long chatId, Algorithm algorithm) throws IOException {
        if (algorithm == null) {
            throw new IllegalArgumentException("Algorithm is null");
        }

        String description = algorithm.getDescription();
        String imageURL = algorithm.getImageUrl();
        String descriptionCode = algorithm.getDescriptionCode();

        // Отправка фото с описанием работы алгоритма
        if (imageURL != null && !imageURL.isEmpty()) {

            SendPhoto photoMessage = new SendPhoto();
            photoMessage.setChatId(chatId);
            photoMessage.setPhoto(new InputFile(imageURL));
            photoMessage.setCaption(description);

            try {
                execute(photoMessage);
            }
            catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }

        // Отправка кода алгоритма в отдельном сообщении
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(descriptionCode);
        message.setParseMode("Markdown");
        try {
            execute(message);
        }
        catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendInvalidAlgorithmMessage(long chatId) {
        String message = "Недопустимое название алгоритма. Пожалуйста, попробуйте снова.";
        sendMessage(chatId, message);
    }

    private void sendAlgorithmList(long chatId) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();

        List<String> algorithmNames = databaseManager.getAlgorithmNames(); // Получать имена из базы данных

        // Создайте кнопки для каждого алгоритма
        for (String algorithmName : algorithmNames) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(algorithmName);
            button.setCallbackData(algorithmName);
            row.add(button);
            rowList.add(row);
        }

        inlineKeyboardMarkup.setKeyboard(rowList);
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Выберите алгоритм:");
        message.setReplyMarkup(inlineKeyboardMarkup);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

     private void sendMessage(long chatId, String textToSend){
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);

        try {
            execute(message);
        }
        catch (TelegramApiException e) {
            log.error("Error occurred: " + e.getMessage());
        }
    }

    private void sendSettingsMenu(long chatId) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();

        // Кнопка для выбора Java
        List<InlineKeyboardButton> Row = new ArrayList<>();
        InlineKeyboardButton javaButton = new InlineKeyboardButton();
        javaButton.setText("Java");
        javaButton.setCallbackData("set_language_java");
        Row.add(javaButton);
        rowList.add(Row);

        // Кнопка для выбора Python
        InlineKeyboardButton pythonButton = new InlineKeyboardButton();
        pythonButton.setText("Python");
        pythonButton.setCallbackData("set_language_python");
        Row.add(pythonButton);

        inlineKeyboardMarkup.setKeyboard(rowList);
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Выберите язык программирования:");
        message.setReplyMarkup(inlineKeyboardMarkup);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getBotToken() { return config.getBotToken(); }
    @Override
    public String getBotUsername() { return config.getBotName(); }
}
