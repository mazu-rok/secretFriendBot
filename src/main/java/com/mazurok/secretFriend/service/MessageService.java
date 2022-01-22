package com.mazurok.secretFriend.service;

import com.mazurok.secretFriend.repository.entity.UserEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class MessageService {
    private final static String askAgeMessage = "Please set your age";
    private final static String incorrectAgeMessage = "Use only numbers, for example: 21";

    private final static String askCityMessage = """
            Please set your city.
            Use official english name, use wikipedia for example: Kyiv(NOT Kiev!)
            It is important for automatic search""";

    private final static String askGenderMessage = "Please set gender";
    private final static String manGenderAnswerMessage = "Male";
    private final static String womanGenderAnswerMessage = "Female";

    private final static String userConfigFinished = """
            Your profile configured:
            First Name: %s
            Last Name: %s
            Gender: %s
            Age: %s
            City: %s
            """;

    public SendMessage createAskUserAgeMessage(Update update) {
        return SendMessage.builder()
                .chatId(String.valueOf(update.getMessage().getChatId()))
                .text(askAgeMessage)
                .build();
    }

    public SendMessage createIncorrectUserAgeMessage(Update update) {
        return SendMessage.builder()
                .chatId(String.valueOf(update.getMessage().getChatId()))
                .text(incorrectAgeMessage)
                .build();
    }

    public SendMessage createAskUserCityMessage(Update update) {
        return SendMessage.builder()
                .chatId(String.valueOf(update.getMessage().getChatId()))
                .text(askCityMessage)
                .build();
    }

    public SendMessage createAskUserGenderMessage(Update update) {
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
        List<InlineKeyboardButton> buttons1 = new ArrayList<>();
        buttons1.add(InlineKeyboardButton.builder().text(manGenderAnswerMessage).callbackData("MALE").build());
        buttons1.add(InlineKeyboardButton.builder().text(womanGenderAnswerMessage).callbackData("FEMALE").build());
        buttons.add(buttons1);

        InlineKeyboardMarkup markupKeyboard = new InlineKeyboardMarkup();
        markupKeyboard.setKeyboard(buttons);

        return SendMessage.builder()
                .chatId(String.valueOf(update.getMessage().getChatId()))
                .text(askGenderMessage)
                .replyMarkup(markupKeyboard)
                .build();
    }

    public AnswerCallbackQuery createConfigFinishedMessage(UserEntity user, Update update) {
        return AnswerCallbackQuery.builder()
                .callbackQueryId(update.getCallbackQuery().getId())
                .text(String.format(userConfigFinished, user.getFirstName(), user.getLastName(), user.getGender(),
                        user.getAge(), user.getCity()))
                .showAlert(true)
                .build();
    }
}
