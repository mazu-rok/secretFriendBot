package com.mazurok.secretFriend.service;

import com.mazurok.secretFriend.exceptions.NotFoundException;
import com.mazurok.secretFriend.repository.UserRepository;
import com.mazurok.secretFriend.repository.entity.Commands;
import com.mazurok.secretFriend.repository.entity.Language;
import com.mazurok.secretFriend.repository.entity.StagePart;
import com.mazurok.secretFriend.repository.entity.UserEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendSticker;
import org.telegram.telegrambots.meta.api.methods.send.SendVoice;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.*;

import static com.mazurok.secretFriend.repository.entity.Stage.*;
import static com.mazurok.secretFriend.repository.entity.Stage.MESSAGING;
import static com.mazurok.secretFriend.repository.entity.StagePart.*;

@Slf4j
@Service
public class MessagingService {
    private final UserRepository userRepository;
    private final MessageSource messageSource;
    private final ButtonsService buttonsService;

    @Autowired
    public MessagingService(UserRepository userRepository,
                            MessageSource messageSource,
                            ButtonsService buttonsService) {
        this.userRepository = userRepository;
        this.messageSource = messageSource;
        this.buttonsService = buttonsService;
    }

    public List<Object> handleConfigCommand(Commands command, UserEntity user) {
        List<Object> messages = new ArrayList<>();

        switch (command) {
            case GET_RANDOM_FRIEND -> {
                try {
                    messages.addAll(getRandomUser(user));
                    user.getStages().add(Pair.of(CHOOSE_FRIEND, NO_ACTION));
                    userRepository.save(user);
                } catch (NotFoundException e) {
                    log.error("Random user not found", e);
                    messages.add(randomUserNotFoundMessage(String.valueOf(user.getChatId()), user.getLanguage()));
                }
            }
            case START_AUTOMATIC_SEARCH -> {
//                messages.addAll()
            }
            case STOP_MESSAGING -> {
                messages.addAll(stopMessaging(user));
            }
            case BLOCK_USER -> {

            }
        }
        return messages;
    }

    public List<Object> handleConfigStage(UserEntity user, Update update) {
        return switch (user.getStages().peek().getFirst()) {
            case CHOOSE_FRIEND -> {
                if (update.hasCallbackQuery() && update.getCallbackQuery().getData().contains("Apply")) {
                    yield sendMessagingRequest(user);
                } else if (!update.hasCallbackQuery() || update.getCallbackQuery().getData().contains("Next")) {
                    try {
                        yield getRandomUser(user);
                    } catch (NotFoundException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    yield null; // send notification to choose a command
                }
            }
            case MESSAGE_REQUEST -> switch (user.getStages().peek().getSecond()) {
                case MESSAGE_REQUEST_CONFIRMATION -> {
                    if (update.hasCallbackQuery() && update.getCallbackQuery().getData().contains("Apply")) {
                        yield applyMessagingRequest(user);
                    } else {
                        yield declineMessagingRequest(user);
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
            case MESSAGING -> {
                if (update.getMessage().hasText()) {
                    yield List.of(SendMessage.builder()
                            .text(update.getMessage().getText())
                            .chatId(String.valueOf(user.getSecretFriend().getChatId()))
                            .build());
                } else if (update.getMessage().hasSticker()) {
                    yield List.of(SendSticker.builder()
                            .chatId(String.valueOf(user.getSecretFriend().getChatId()))
                            .sticker(new InputFile(update.getMessage().getSticker().getFileId()))
                            .build());
                } else if (update.getMessage().hasVoice()) {
                    yield List.of(SendVoice.builder()
                            .chatId(String.valueOf(user.getSecretFriend().getChatId()))
                            .voice(new InputFile(update.getMessage().getVoice().getFileId()))
                            .build());
                } else {
                    yield null;
                }
            }
            default -> null;
        };
    }

    public List<Object> getRandomUser(UserEntity user) throws NotFoundException {
        UserEntity randomUser = userRepository.findRandomUserByIdNot(user.getId());
        if (randomUser == null) {
            throw new NotFoundException("Random user for %s not found".formatted(user.getUserName()));
        }
        user.setSecretFriend(randomUser);
        userRepository.save(user);

        return List.of(SendMessage.builder()
                        .chatId(String.valueOf(user.getChatId()))
                        .text(messageSource.getMessage("found_random_user_msg", null, new Locale(user.getLanguage().name())))
                        .replyMarkup(buttonsService.createCancelButton(user.getLanguage()))
                        .build(),
                createFoundUserRepresentationMessage(randomUser, user.getChatId(), user.getLanguage()));
    }

    public List<Object> sendMessagingRequest(UserEntity user) {
        user.getStages().add(Pair.of(MESSAGE_REQUEST, MESSAGE_REQUEST_SENT));

        user.getSecretFriend().getStages().add(Pair.of(MESSAGE_REQUEST, MESSAGE_REQUEST_CONFIRMATION));
        user.getSecretFriend().setSecretFriend(user);

        userRepository.save(user.getSecretFriend());
        userRepository.save(user);
        return List.of(createMessagingRequest(user.getSecretFriend()), createMessagingRequestSent(user));
    }

    public List<Object> applyMessagingRequest(UserEntity user) {
        user.replaceLastStage(Pair.of(MESSAGING, NO_ACTION));

        user.getSecretFriend().replaceLastStage(Pair.of(MESSAGING, NO_ACTION));

        userRepository.save(user.getSecretFriend());
        userRepository.save(user);

        return List.of(createStartMessagingMessage(user.getChatId(), user.getLanguage()),
                createStartMessagingMessage(user.getSecretFriend().getChatId(), user.getSecretFriend().getLanguage()));
    }

    public List<Object> declineMessagingRequest(UserEntity user) {
        user.getStages().pop();
        user.getSecretFriend().getStages().pop();

        userRepository.save(user.getSecretFriend());
        userRepository.save(user);

        return List.of(createDeclineRequestMessage(user.getSecretFriend().getChatId(), user.getSecretFriend().getLanguage()));
    }

    public List<Object> cancelMessagingRequest(UserEntity user) {
        user.getStages().pop();

        user.getSecretFriend().getStages().pop();

        userRepository.save(user.getSecretFriend());
        userRepository.save(user);

        try {
            return getRandomUser(user);
        } catch (NotFoundException e) {
            user.getStages().pop();
            userRepository.save(user);

            return List.of(randomUserNotFoundMessage(String.valueOf(user.getChatId()), user.getLanguage()));
        }
    }

    public List<Object> stopMessaging(UserEntity user) {
        UserEntity secFriend = user.getSecretFriend();

        SendMessage canceledMessage = SendMessage.builder()
                .text(messageSource.getMessage("messaging_canceled_friend", null,
                        new Locale(secFriend.getLanguage().name())))
                .chatId(String.valueOf(secFriend.getChatId()))
                .replyMarkup(buttonsService.createMainButtons(secFriend.getLanguage()))
                .build();

        SendMessage cancelMessage = SendMessage.builder()
                .text(messageSource.getMessage("messaging_canceled", null, new Locale(user.getLanguage().name())))
                .chatId(String.valueOf(user.getChatId()))
                .replyMarkup(buttonsService.createMainButtons(user.getLanguage()))
                .build();

        secFriend.setSecretFriend(null);
        secFriend.getStages().removeAllElements();
        secFriend.getStages().push(Pair.of(NO_STAGE, StagePart.NO_ACTION));
        userRepository.save(secFriend);

        user.setSecretFriend(null);
        user.getStages().removeAllElements();
        user.getStages().push(Pair.of(NO_STAGE, StagePart.NO_ACTION));
        userRepository.save(user);

        return List.of(canceledMessage, cancelMessage);
    }

    public List<Object> handleCancelCommand(UserEntity user) {
        return List.of(SendMessage.builder()
                .text(messageSource.getMessage("no_stage_msg",
                        List.of(user.getFirstName()).toArray(), new Locale(user.getLanguage().name())))
                .replyMarkup(buttonsService.createMainButtons(user.getLanguage())).build());
    }

    /**
     * create messages
     */

    private SendMessage createFoundUserRepresentationMessage(UserEntity user, Long chatId, Language lang) {
        List<List<InlineKeyboardButton>> actionButtons = new ArrayList<>();
        List<InlineKeyboardButton> actionButtonsLine1 = new ArrayList<>();
        actionButtonsLine1.add(InlineKeyboardButton.builder()
                .text(messageSource.getMessage("start_messaging_btn_msg", null, new Locale(lang.name())))
                .callbackData("Apply").build());
        actionButtonsLine1.add(InlineKeyboardButton.builder()
                .text(messageSource.getMessage("next_btn_msg", null, new Locale(lang.name())))
                .callbackData("Next").build());
        actionButtons.add(actionButtonsLine1);
        InlineKeyboardMarkup markupKeyboard = new InlineKeyboardMarkup();
        markupKeyboard.setKeyboard(actionButtons);

        return SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(messageSource.getMessage("secret_friend_desc",
                        List.of(user.getGender(), user.getAge(), user.getCity()).toArray(), new Locale(lang.name())))
                .replyMarkup(markupKeyboard)
                .build();
    }

    public SendMessage createStartMessagingMessage(Long chatId, Language lang) {
        return SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(messageSource.getMessage("start_messaging_msg", null, new Locale(lang.name())))
                .replyMarkup(buttonsService.createMessagingButtons(lang))
                .build();
    }

    public SendMessage createMessagingRequestSent(UserEntity user) {
        List<List<InlineKeyboardButton>> actionButtons = new ArrayList<>();
        List<InlineKeyboardButton> actionButtonsLine1 = new ArrayList<>();
        actionButtonsLine1.add(InlineKeyboardButton.builder()
                .text(messageSource.getMessage("cancel_btn_msg", null, new Locale(user.getLanguage().name())))
                .callbackData("Cancel").build());
        actionButtons.add(actionButtonsLine1);
        InlineKeyboardMarkup markupKeyboard = new InlineKeyboardMarkup();
        markupKeyboard.setKeyboard(actionButtons);

        return SendMessage.builder()
                .chatId(String.valueOf(user.getChatId()))
                .text(messageSource.getMessage("message_request_sent_msg", null, new Locale(user.getLanguage().name())))
                .replyMarkup(markupKeyboard)
                .build();
    }

    public SendMessage createMessagingRequest(UserEntity secretFriend) {
        List<List<InlineKeyboardButton>> actionButtons = new ArrayList<>();
        List<InlineKeyboardButton> actionButtonsLine1 = new ArrayList<>();
        actionButtonsLine1.add(InlineKeyboardButton.builder()
                .text(messageSource.getMessage("apply_btn_msg", null, new Locale(secretFriend.getLanguage().name())))
                .callbackData("Apply").build());
        actionButtonsLine1.add(InlineKeyboardButton.builder()
                .text(messageSource.getMessage("decline_btn_msg", null, new Locale(secretFriend.getLanguage().name())))
                .callbackData("Decline").build());
        actionButtons.add(actionButtonsLine1);
        InlineKeyboardMarkup markupKeyboard = new InlineKeyboardMarkup();
        markupKeyboard.setKeyboard(actionButtons);

        return SendMessage.builder()
                .chatId(String.valueOf(secretFriend.getChatId()))
                .text(messageSource.getMessage("message_request_msg",
                        List.of(secretFriend.getSecretFriend().getGender(), secretFriend.getSecretFriend().getAge(),
                                secretFriend.getSecretFriend().getCity()).toArray(),
                        new Locale(secretFriend.getLanguage().name())))
                .replyMarkup(markupKeyboard)
                .build();
    }

    public SendMessage createDeclineRequestMessage(Long chatId, Language lang) {
        return SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(messageSource.getMessage("decline_request_msg", null, new Locale(lang.name())))
                .build();
    }

    private SendMessage randomUserNotFoundMessage(String chatId, Language language) {
        return SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(messageSource.getMessage("random_user_not_found_msg", null, new Locale(language.name())))
                .replyMarkup(buttonsService.createMainButtons(language))
                .build();
    }
}
