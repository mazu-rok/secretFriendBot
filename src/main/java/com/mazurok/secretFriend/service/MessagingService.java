package com.mazurok.secretFriend.service;

import com.mazurok.secretFriend.repository.UserRepository;
import com.mazurok.secretFriend.repository.entity.Commands;
import com.mazurok.secretFriend.repository.entity.UserEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendSticker;
import org.telegram.telegrambots.meta.api.methods.send.SendVoice;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import static com.mazurok.secretFriend.repository.entity.Stage.*;
import static com.mazurok.secretFriend.repository.entity.Stage.MESSAGING;
import static com.mazurok.secretFriend.repository.entity.StagePart.*;

@Slf4j
@Service
public class MessagingService {

    private final UserRepository userRepository;

    @Autowired
    public MessagingService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<Object> handleConfigCommand(Commands command, UserEntity user, Update update) {
        List<Object> messages = new ArrayList<>();

        switch (command) {
            case GET_RANDOM_FRIEND -> {
                user.setStage(CHOOSE_FRIEND);
                userRepository.save(user);
                messages.addAll(getRandomUser(user));
            }
            case START_AUTOMATIC_SEARCH -> {
//                messages.addAll()
            }
        }
        return messages;
    }

    public List<Object> handleConfigStage(UserEntity user, Update update) {
        List<Object> messages = new ArrayList<>();

        if (user.getStage().equals(CHOOSE_FRIEND)) {
            if (update.hasCallbackQuery() && update.getCallbackQuery().getData().contains("Apply")) {
                messages = sendMessagingRequest(user);
            }
            if (!update.hasCallbackQuery() || update.getCallbackQuery().getData().contains("Next")) {
                messages = getRandomUser(user);
            }
        } else if (user.getStage().equals(MESSAGE_REQUEST)) {
            messages = switch (user.getStagePart()) {
                case MESSAGE_REQUEST_CONFIRMATION -> {
                    if (update.hasCallbackQuery() && update.getCallbackQuery().getData().contains("Accept")) {
                        yield applyMessagingRequest(user);
                    } else if (update.hasCallbackQuery() && update.getCallbackQuery().getData().contains("Decline")) {
                        yield declineMessagingRequest(user);
                    } else {
                        yield null;
                    }
                }
                case MESSAGE_REQUEST_SENT -> {
                    if (update.hasCallbackQuery() && update.getCallbackQuery().getData().contains("Cancel")) {
                        yield cancelMessagingRequest(user);
                    } else {
                        yield null;
                    }
                }
                default -> null;
            };
        } else if (user.getStage().equals(MESSAGING)) {
            if (update.getMessage().hasText()) {
                messages = List.of(SendMessage.builder()
                        .text(update.getMessage().getText())
                        .chatId(String.valueOf(user.getSecretFriend().getChatId()))
                        .build());
            } else if (update.getMessage().hasSticker()) {
                messages = List.of(SendSticker.builder()
                        .chatId(String.valueOf(user.getChatId()))
                        .sticker(new InputFile(update.getMessage().getSticker().getFileId()))
                        .build());
            } else if (update.getMessage().hasVoice()) {
                messages = List.of(SendVoice.builder()
                        .chatId(String.valueOf(user.getChatId()))
                        .voice(new InputFile(update.getMessage().getVoice().getFileId()))
                        .build());
            }
        }
        return messages;
    }

    public List<Object> getRandomUser(UserEntity user) {
        UserEntity randomUser = userRepository.findRandomUserByIdNot(user.getId());
        if (randomUser == null) {
            throw new NoSuchElementException("Random user not found");
        }
        user.setSecretFriend(randomUser);
        userRepository.save(user);

        return List.of(createFoundUserRepresentationMessage(randomUser, user.getChatId()));
    }

    public List<Object> sendMessagingRequest(UserEntity user) {
        user.setStage(MESSAGE_REQUEST);
        user.setStagePart(MESSAGE_REQUEST_SENT);

        user.getSecretFriend().setStage(MESSAGE_REQUEST);
        user.getSecretFriend().setStagePart(MESSAGE_REQUEST_CONFIRMATION);
        user.getSecretFriend().setSecretFriend(user);

        userRepository.save(user);
        userRepository.save(user.getSecretFriend());
        return List.of(createMessagingRequest(user.getSecretFriend()), createMessagingRequestSent(user));
    }

    public List<Object> applyMessagingRequest(UserEntity user) {
        user.setStage(MESSAGING);
        user.setStagePart(NO_ACTION);

        user.getSecretFriend().setStage(MESSAGING);
        user.getSecretFriend().setStagePart(NO_ACTION);

        userRepository.save(user);
        userRepository.save(user.getSecretFriend());

        return List.of(createStartMessagingMessage(user.getChatId()), createStartMessagingMessage(user.getSecretFriend().getChatId()));
    }

    public List<Object> declineMessagingRequest(UserEntity user) {
        user.getSecretFriend().setStage(CHOOSE_FRIEND);
        user.getSecretFriend().setStagePart(NO_ACTION);
        userRepository.save(user.getSecretFriend());

        return List.of(createDeclineRequestMessage(user.getSecretFriend().getChatId()));
    }

    public List<Object> cancelMessagingRequest(UserEntity user) {
        user.setStage(CHOOSE_FRIEND);
        user.setStagePart(NO_ACTION);

        //FIXME: Change logic for message request, we shouldn't change secret friend entity
        user.getSecretFriend().setStage(NO_STAGE);
        user.getSecretFriend().setStagePart(NO_ACTION);
        userRepository.save(user);
        userRepository.save(user.getSecretFriend());

        return getRandomUser(user);
    }

    /**
     * create messages
     */

    private final static String userRepresentationMessage = """
            Gender: %s
            Age: %s
            City: %s
            """;


    private SendMessage createFoundUserRepresentationMessage(UserEntity user, Long chatId) {
        List<List<InlineKeyboardButton>> actionButtons = new ArrayList<>();
        List<InlineKeyboardButton> actionButtonsLine1 = new ArrayList<>();
        actionButtonsLine1.add(InlineKeyboardButton.builder().text("Start messaging").callbackData("Apply:%s" .formatted(user.getId())).build());
        actionButtonsLine1.add(InlineKeyboardButton.builder().text("Next").callbackData("Next:%s" .formatted(user.getId())).build());
        actionButtons.add(actionButtonsLine1);
        InlineKeyboardMarkup markupKeyboard = new InlineKeyboardMarkup();
        markupKeyboard.setKeyboard(actionButtons);

        return SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text("This is a random user from our bot:\n" + userRepresentationMessage.formatted(user.getGender(), user.getAge(), user.getCity()))
                .replyMarkup(markupKeyboard)
                .build();
    }

    public SendMessage createStartMessagingMessage(Long chatId) {
        return SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text("Now you can start messaging with your friend!")
                .build();
    }

    public SendMessage createMessagingRequestSent(UserEntity user) {
        List<List<InlineKeyboardButton>> actionButtons = new ArrayList<>();
        List<InlineKeyboardButton> actionButtonsLine1 = new ArrayList<>();
        actionButtonsLine1.add(InlineKeyboardButton.builder().text("Cancel").callbackData("Cancel").build());
        actionButtons.add(actionButtonsLine1);
        InlineKeyboardMarkup markupKeyboard = new InlineKeyboardMarkup();
        markupKeyboard.setKeyboard(actionButtons);

        return SendMessage.builder()
                .chatId(String.valueOf(user.getChatId()))
                .text("Messaging request has been sent. But you can cancel it")
                .replyMarkup(markupKeyboard)
                .build();
    }

    public SendMessage createMessagingRequest(UserEntity secretFriend) {
        List<List<InlineKeyboardButton>> actionButtons = new ArrayList<>();
        List<InlineKeyboardButton> actionButtonsLine1 = new ArrayList<>();
        actionButtonsLine1.add(InlineKeyboardButton.builder().text("Accept").callbackData("Accept").build());
        actionButtonsLine1.add(InlineKeyboardButton.builder().text("Decline").callbackData("Decline").build());
        actionButtons.add(actionButtonsLine1);
        InlineKeyboardMarkup markupKeyboard = new InlineKeyboardMarkup();
        markupKeyboard.setKeyboard(actionButtons);

        return SendMessage.builder()
                .chatId(String.valueOf(secretFriend.getChatId()))
                .text("This user wants to send you a message\n" +
                        userRepresentationMessage.formatted(
                                secretFriend.getSecretFriend().getGender(),
                                secretFriend.getSecretFriend().getAge(),
                                secretFriend.getSecretFriend().getCity()))
                .replyMarkup(markupKeyboard)
                .build();
    }

    public SendMessage createDeclineRequestMessage(Long chatId) {
        return SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text("This user didn't accept your request!")
                .build();
    }
}
