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
    private final static String savedMessage = "Saved";
    private final static String askAgeMessage = "Please set your age";
    private final static String incorrectAgeMessage = "Use only numbers, for example: 21";

    private final static String askCityMessage = """
            Please set your city.
            Use official english name, use wikipedia for example: Kyiv (NOT Kiev!)
            It is important for automatic search""";

    private final static String askGenderMessage = "Please set gender";
    private final static String manGenderAnswerMessage = "Male";
    private final static String womanGenderAnswerMessage = "Female";
    private final static String anyGenderAnswerMessage = "Any";
    private final static String askWhatChangeMessage = "What do you want to change?";

    private final static String userConfigFinished = """
            Your profile configured:
            First Name: %s
            Last Name: %s
            Gender: %s
            Age: %s
            City: %s
            """;

    /** Text for secret friend config **/
    private final static String askSecretFriendAgeMessage = "Please set age for your secret friend in format: minAge-maxAge. Example: 20-25";
    private final static String incorrectSecretFriendAgeMessage = "Use only numbers, for example: 20-25";

    private final static String askSecretFriendCityMessage = """
            Please set your secret friend city.
            Use official english name, you can use wikipedia to check correct name. For example: Kyiv (NOT Kiev!)
            It is important for automatic search""";

    private final static String askSecretFriendGenderMessage = "Please set gender of your friend";

    private final static String secretFriendConfigFinished = """
            Your Secret Friend profile configured:
            Gender: %s
            Age: %s-%s
            City: %s
            """;

    public SendMessage createSavedMessage(Long chatId) {
        return SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(savedMessage)
                .build();
    }

    public SendMessage createAskUserAgeMessage(Long chatId) {
        return SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(askAgeMessage)
                .build();
    }

    public SendMessage createIncorrectUserAgeMessage(Long chatId) {
        return SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(incorrectAgeMessage)
                .build();
    }

    public SendMessage createAskUserCityMessage(Long chatId) {
        return SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(askCityMessage)
                .build();
    }

    public SendMessage createAskUserGenderMessage(Long chatId) {
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
        List<InlineKeyboardButton> buttons1 = new ArrayList<>();
        buttons1.add(InlineKeyboardButton.builder().text(manGenderAnswerMessage).callbackData("MALE").build());
        buttons1.add(InlineKeyboardButton.builder().text(womanGenderAnswerMessage).callbackData("FEMALE").build());
        buttons.add(buttons1);

        InlineKeyboardMarkup markupKeyboard = new InlineKeyboardMarkup();
        markupKeyboard.setKeyboard(buttons);

        return SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(askGenderMessage)
                .replyMarkup(markupKeyboard)
                .build();
    }

    public AnswerCallbackQuery createConfigFinishedMessage(UserEntity user, String callbackId) {
        return AnswerCallbackQuery.builder()
                .callbackQueryId(callbackId)
                .text(String.format(userConfigFinished, user.getFirstName(), user.getLastName(), user.getGender(),
                        user.getAge(), user.getCity()))
                .showAlert(true)
                .build();
    }

    public SendMessage createAskConfigProfileMessage(Long chatId) {
        return SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(askWhatChangeMessage)
                .build();
    }


    /** Messages for secret friend config **/
    public SendMessage createAskSecretFriendAgeMessage(Long chatId) {
        return SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(askSecretFriendAgeMessage)
                .build();
    }

    public SendMessage createIncorrectSecretFriendAgeMessage(Long chatId) {
        return SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(incorrectSecretFriendAgeMessage)
                .build();
    }

    public SendMessage createAskSecretFriendCityMessage(Long chatId) {
        return SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(askSecretFriendCityMessage)
                .build();
    }

    public SendMessage createAskSecretFriendGenderMessage(Long chatId) {
        List<List<InlineKeyboardButton>> genderButtons = new ArrayList<>();
        List<InlineKeyboardButton> genderButtonsLine1 = new ArrayList<>();
        genderButtonsLine1.add(InlineKeyboardButton.builder().text(manGenderAnswerMessage).callbackData("MALE").build());
        genderButtonsLine1.add(InlineKeyboardButton.builder().text(womanGenderAnswerMessage).callbackData("FEMALE").build());
        genderButtonsLine1.add(InlineKeyboardButton.builder().text(anyGenderAnswerMessage).callbackData("ANY").build());
        genderButtons.add(genderButtonsLine1);

        InlineKeyboardMarkup markupKeyboard = new InlineKeyboardMarkup();
        markupKeyboard.setKeyboard(genderButtons);

        return SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(askSecretFriendGenderMessage)
                .replyMarkup(markupKeyboard)
                .build();
    }

    public AnswerCallbackQuery createSecretFriendConfigFinishedMessage(UserEntity user, String callbackId) {
        return AnswerCallbackQuery.builder()
                .callbackQueryId(callbackId)
                .text(String.format(secretFriendConfigFinished, user.getSecretFriendConfig().getGender(),
                        user.getSecretFriendConfig().getMinAge(), user.getSecretFriendConfig().getMaxAge(),
                        user.getSecretFriendConfig().getCity()))
                .showAlert(true)
                .build();
    }
}
