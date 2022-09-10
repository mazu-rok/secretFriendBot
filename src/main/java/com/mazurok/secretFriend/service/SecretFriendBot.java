package com.mazurok.secretFriend.service;

import com.mazurok.secretFriend.exceptions.IllegalInputException;
import com.mazurok.secretFriend.exceptions.NotFoundException;
import com.mazurok.secretFriend.repository.entity.Commands;
import com.mazurok.secretFriend.repository.entity.UserEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendSticker;
import org.telegram.telegrambots.meta.api.methods.send.SendVoice;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Slf4j
@Component
public class SecretFriendBot extends TelegramLongPollingBot {
    private final String botUsername;
    private final String token;

    private final UserService userService;
    private final UserConfigService userConfigService;
    private final MessagingService messagingService;
    private final ButtonsService buttonsService;

    private final MessageSource messageSource;

    @Autowired
    public SecretFriendBot(@Value("${secret-friend.bot.name}") String botUsername,
                           @Value("${secret-friend.bot.token}") String token,
                           UserService userService,
                           UserConfigService userConfigService,
                           MessagingService messagingService,
                           ButtonsService buttonsService,
                           MessageSource messageSource) {
        this.botUsername = botUsername;
        this.token = token;

        this.userService = userService;
        this.userConfigService = userConfigService;
        this.messagingService = messagingService;
        this.buttonsService = buttonsService;
        this.messageSource = messageSource;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return token;
    }

    @Override
    public void onUpdateReceived(Update update) {
        UserEntity user;
        List<Object> messages;

        try {
            user = userService.getUserByUpdate(update);
            if (user == null) {
                return;
            }

            if (update.hasMyChatMember()) {
                return;
            }
            // handle input commands
            if (update.hasMessage() && update.getMessage().hasText()
                    && update.getMessage().getText().startsWith("| ") && update.getMessage().getText().endsWith(" |")) {
                try {
                    messages = handleCommand(user, update);
                    sendMessage(messages);
                    return;
                } catch (NotFoundException e) {
                    log.warn("command {} not found", update.getMessage().getText(), e);
                    // ????
//                    return;
                }
            }
            // handle stage
            messages = handleStage(user, update);
            sendMessage(messages);
        } catch (IllegalInputException e) {
            log.error("Error", e);
        }
    }

    private List<Object> handleCommand(UserEntity user, Update update) throws NotFoundException {
        Commands command;
        command = Commands.fromString(update.getMessage().getText());
        if (!isCommandAllowed(user, command)) {
            // create messages for each stage
            return List.of(SendMessage.builder()
                    .text(messageSource.getMessage("command_forbidden_message", null, new Locale(user.getLanguage().name())))
                    .chatId(String.valueOf(user.getChatId()))
                    .build());
        }

        return switch (command) {
            case START -> {
                List<Object> res = new ArrayList<>();
                res.add(SendMessage.builder()
                        .chatId(String.valueOf(user.getChatId()))
                        .text(messageSource.getMessage("hello_msg",
                                List.of(user.getFirstName()).toArray(), new Locale(user.getLanguage().name())))
                        .build());
                res.addAll(userConfigService.handleConfigStage(user, update));
                yield res;
            }
            case CONFIGURE_PROFILE, CONFIGURE_SECRET_FRIEND_PROFILE, SHOW_PROFILE -> userConfigService.handleConfigCommand(command, user);
            case GET_RANDOM_FRIEND, START_AUTOMATIC_SEARCH, STOP_MESSAGING, BLOCK_USER -> messagingService.handleConfigCommand(command, user);
            case CANCEL -> {
                user.getStages().pop();
                userService.save(user);
                yield handleStage(user, update);
            }
        };
    }

    private List<Object> handleStage(UserEntity user, Update update) {
        return switch (user.getStages().peek().getFirst()) {
            case CONFIGURE_FULL_PROFILE, CONFIGURE_FULL_SECRET_FRIEND_PROFILE, CONFIGURE_PROFILE,
                    CONFIGURE_SECRET_FRIEND_PROFILE -> userConfigService.handleConfigStage(user, update);
            case AUTO_LOOKING_FOR_A_FRIEND, CHOOSE_FRIEND, MESSAGE_REQUEST, MESSAGING -> messagingService.handleConfigStage(user, update);
            case NO_STAGE -> List.of(SendMessage.builder()
                    .chatId(String.valueOf(user.getChatId()))
                    .text(messageSource.getMessage("no_stage_msg",
                            List.of(user.getFirstName()).toArray(), new Locale(user.getLanguage().name())))
                    .replyMarkup(buttonsService.createMainButtons(user.getLanguage()))
                    .build());
        };
    }

    private boolean isCommandAllowed(UserEntity user, Commands command) {
        return switch (user.getStages().peek().getFirst()) {
            case NO_STAGE -> List.of(Commands.CONFIGURE_PROFILE, Commands.CONFIGURE_SECRET_FRIEND_PROFILE,
                    Commands.SHOW_PROFILE, Commands.GET_RANDOM_FRIEND, Commands.START_AUTOMATIC_SEARCH).contains(command);
            case CONFIGURE_PROFILE, CONFIGURE_SECRET_FRIEND_PROFILE, AUTO_LOOKING_FOR_A_FRIEND, CHOOSE_FRIEND -> Objects.equals(Commands.CANCEL, command);
            case MESSAGING -> List.of(Commands.STOP_MESSAGING, Commands.BLOCK_USER).contains(command);
            default -> false;
        };
    }

    private void sendMessage(List<Object> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }

        for (Object message : messages) {
            try {
                if (message instanceof SendMessage text) {
                    execute(text);
                } else if (message instanceof AnswerCallbackQuery answer) {
                    execute(answer);
                } else if (message instanceof SendSticker sticker) {
                    execute(sticker);
                } else if (message instanceof SendVoice voice) {
                    execute(voice);
                }
            } catch (TelegramApiException e) {
                log.error("Exception: {}", e.toString());
            }
        }
    }
}
