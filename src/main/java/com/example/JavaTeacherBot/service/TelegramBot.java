package com.example.JavaTeacherBot.service;


import com.example.JavaTeacherBot.config.BotConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;



class Algorithm {
    @Value("${name}")
    String name;
    @Value("${description}")
    String description;
    @Value("${imageUrl}")
    String imageUrl;

    public Algorithm(String name, String description, String imageUrl) {
        this.name = name;
        this.description = description;
        this.imageUrl = imageUrl;
    }
}
@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot{

    final BotConfig config;
    static final String HELP_TEXT = "This bot is created to teach you java.\n\n" +
            "You can execute commands from the main menu on the left or by typing the command\n\n" +
            "/start - get a welcome massage\n\n" +
            "/algorithm - algorithms menu\n\n" +
            "/help - info how to use this bot\n\n" +
            "/settings - set your preferences";
    List<Algorithm> algorithms = new ArrayList<>();


    public TelegramBot(BotConfig config) {
        this.config = config;

        algorithms.add(new Algorithm(
                "Selection Sort",
                "Описание алгоритма...",
                "V:/Downloads/JavaTeacherBot/src/main/resources/Algoritms&StructuresTelegramTeacherBot/Sort/SelectionSort.jpg"
        ));

        ArrayList<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "get a welcome massage"));
        listOfCommands.add(new BotCommand("/algorithm", "algorithms menu"));
        listOfCommands.add(new BotCommand("/help", "info how to use this bot"));
        listOfCommands.add(new BotCommand("/settings", "set your preferences"));

        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
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
                    // Пока ничего нет
                    break;
                default:
                    // Проверить, является ли сообщение названием алгоритма
                    for (Algorithm algorithm : algorithms) {
                        if (algorithm.name.equals(messageText)) {
                            try {
                                sendAlgorithmInfo(chatId, messageText);
                                return; // Алгоритм найден и информация отправлена
                            } catch (IOException e) {
                                log.error("Ошибка при отправке информации об алгоритме", e);
                                sendMessage(chatId, "Произошла ошибка. Попробуйте еще раз.");
                            }
                        }
                    }
                    sendMessage(chatId, "Sorry, command was not recognized, try to use command /help");
            }
        }
        // ...
    }
    private void startCommandReceived(long chatId, String name){

        String answer = "Hi, " + name + ", nice to meet you!";
        log.info("Replied to user: " + name);

        sendMessage(chatId,answer);
    }


    private void sendAlgorithmInfo(long chatId, String algorithmName) throws IOException {
        // Найти алгоритм по имени
        for (Algorithm algorithm : algorithms) {
            if (algorithm.name.equals(algorithmName)) {
                String description = algorithm.description;
                String imageURL = algorithm.imageUrl;
                switch (algorithmName) {
                    case "Selection Sort":
                        sendSelectionSortInfo(chatId,description,imageURL);
                    break;
                default:sendInvalidAlgorithmMessage(chatId);
                }
                return; // Алгоритм найден и информация отправлена
            }
        }
        // Алгоритм не найден
        sendInvalidAlgorithmMessage(chatId);
    }

    private void sendInvalidAlgorithmMessage(long chatId) {
        String message = "Invalid algorithm name. Please try again.";
        sendMessage(chatId, message);
    }

    private void sendAlgorithmList(long chatId) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        for (Algorithm algorithm : algorithms) {
            KeyboardRow row = new KeyboardRow();
            row.add(new KeyboardButton(algorithm.name));
            keyboard.add(row);
        }
        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setOneTimeKeyboard(true); // Hide keyboard after selection

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Choose an algorithm:"); // Add a message to prompt the user
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }


    private void sendSelectionSortInfo(long chatId, String description,String imageURL) throws IOException {

        if (!imageURL.isEmpty()) {
            InputStream stream = new URL(imageURL).openStream();
            SendPhoto photoMessage = new SendPhoto();
            photoMessage.setChatId(chatId);
            photoMessage.setPhoto(new InputFile(stream, imageURL));
            photoMessage.setCaption(description);
            try {
                execute(photoMessage);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }

        // Отправка описания как отдельное сообщение
        SendMessage message = new SendMessage(imageURL, description);
        message.setChatId(chatId);
        message.setParseMode("Markdown"); // Markdown для форматирования ссыль
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

    @Override
    public String getBotToken() { return config.getBotToken(); }
    @Override
    public String getBotUsername() { return config.getBotName(); }
}
