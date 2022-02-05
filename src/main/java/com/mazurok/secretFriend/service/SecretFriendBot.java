package com.mazurok.secretFriend.service;

import com.mazurok.secretFriend.exceptions.IllegalInputException;
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
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;
import java.util.Locale;

@Slf4j
@Component
public class SecretFriendBot extends TelegramLongPollingBot {
    private final String botUsername;
    private final String token;

    private final UserService userService;
    private final UserConfigService userConfigService;
    private final MessagingService messagingService;

    private final MessageSource messageSource;

    @Autowired
    public SecretFriendBot(@Value("${secret-friend.bot.name}") String botUsername,
                           @Value("${secret-friend.bot.token}") String token,
                           UserService userService,
                           UserConfigService userConfigService,
                           MessagingService messagingService,
                           MessageSource messageSource) {
        this.botUsername = botUsername;
        this.token = token;

        this.userService = userService;
        this.userConfigService = userConfigService;
        this.messagingService = messagingService;
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
        // TODO: bot logic
        UserEntity user;
        List<Object> messages;

        try {
            user = userService.getUserByUpdate(update);
            if (user == null) {
                return;
            }

            if (update.hasMessage() && update.getMessage().hasText() && (update.getMessage().getText().startsWith("/cmd")
                    || update.getMessage().getText().equals("/start"))) {
                messages = handleCommand(user, update);
            } else {
                messages = switch (user.getStage()) {
                    case CONFIGURE_FULL_PROFILE, CONFIGURE_FULL_SECRET_FRIEND_PROFILE, CONFIGURE_PROFILE,
                            CONFIGURE_SECRET_FRIEND_PROFILE -> userConfigService.handleConfigStage(user, update);
                    case AUTO_LOOKING_FOR_A_FRIEND, CHOOSE_FRIEND, MESSAGE_REQUEST, MESSAGING ->
                            messagingService.handleConfigStage(user, update);
                    case NO_STAGE -> List.of(SendMessage.builder()
                            .chatId(String.valueOf(user.getChatId()))
                            .text(messageSource.getMessage("no_stage_msg",
                                    List.of(user.getFirstName()).toArray(), new Locale(user.getLanguage().name())))
                            .replyMarkup(createMainButtons())
                            .build());
                };
            }
        } catch (IllegalInputException e) {
            log.error("Error", e);
            return;
        }
        sendMessage(messages);
    }

    private List<Object> handleCommand(UserEntity user, Update update) throws IllegalInputException {
        Commands command;
        command = Commands.fromString(update.getMessage().getText());
        return switch (command) {
            case START -> List.of(SendMessage.builder()
                    .chatId(String.valueOf(user.getChatId()))
                    .text(messageSource.getMessage("hello_msg",
                            List.of(user.getFirstName()).toArray(), new Locale(user.getLanguage().name())))
                    .replyMarkup(createMainButtons())
                    .build());
            case CONFIGURE_PROFILE, CONFIGURE_SECRET_FRIEND_PROFILE, SHOW_PROFILE ->
                    userConfigService.handleConfigCommand(command, user);
            case GET_RANDOM_FRIEND, START_AUTOMATIC_SEARCH -> messagingService.handleConfigCommand(command, user);
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

    private ReplyKeyboardMarkup createMainButtons() {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(true);

        KeyboardRow keyboardRow1 = new KeyboardRow();
        keyboardRow1.add(new KeyboardButton(Commands.CONFIGURE_PROFILE.command));
        keyboardRow1.add(new KeyboardButton(Commands.CONFIGURE_SECRET_FRIEND_PROFILE.command));

        KeyboardRow keyboardRow2 = new KeyboardRow();
        keyboardRow2.add(new KeyboardButton(Commands.SHOW_PROFILE.command));

        KeyboardRow keyboardRow3 = new KeyboardRow();
        keyboardRow3.add(new KeyboardButton(Commands.GET_RANDOM_FRIEND.command));
//        keyboardRow3.add(new KeyboardButton(Commands.START_AUTOMATIC_SEARCH.command));

        replyKeyboardMarkup.setKeyboard(List.of(keyboardRow1, keyboardRow2, keyboardRow3));
        return replyKeyboardMarkup;
    }
}
