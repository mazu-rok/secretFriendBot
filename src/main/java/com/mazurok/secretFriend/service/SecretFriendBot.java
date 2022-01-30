package com.mazurok.secretFriend.service;

import com.mazurok.secretFriend.exceptions.IllegalInputException;
import com.mazurok.secretFriend.repository.entity.Commands;
import com.mazurok.secretFriend.repository.entity.StagePart;
import com.mazurok.secretFriend.repository.entity.UserEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

import static com.mazurok.secretFriend.repository.entity.Stage.*;

@Slf4j
@Component
public class SecretFriendBot extends TelegramLongPollingBot {
    private final String botUsername;
    private final String token;

    private final MessageService messageService;
    private final UserService userService;

    @Autowired
    public SecretFriendBot(@Value("${secret-friend.bot.name}") String botUsername,
                           @Value("${secret-friend.bot.token}") String token,
                           MessageService messageService,
                           UserService userService) {
        this.botUsername = botUsername;
        this.token = token;

        this.messageService = messageService;
        this.userService = userService;
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
        UserEntity user = userService.getUserByUpdate(update);

        ReplyKeyboardMarkup buttons = null;

        if (update.hasMessage() && update.getMessage().getText().startsWith("/")) {
            Commands command;
            try {
                command = Commands.fromString(update.getMessage().getText());
                switch (command) {
                    case START -> {
                        buttons = createMainButtons();
                    }
                    case CONFIGURE_PROFILE -> {
                        sendMessage(messageService.createAskConfigProfileMessage(user.getChatId()), createConfigureProfileButtons());
                        return;
                    }
                    case CHANGE_AGE -> {
                        user.setStage(CONFIGURE_PROFILE);
                        user.setStagePart(StagePart.ASK_AGE);
                    }
                    case CHANGE_CITY -> {
                        user.setStage(CONFIGURE_PROFILE);
                        user.setStagePart(StagePart.ASK_CITY);
                    }
                    case CHANGE_GENDER -> {
                        user.setStage(CONFIGURE_PROFILE);
                        user.setStagePart(StagePart.ASK_GENDER);
                    }

                    case CONFIGURE_SECRET_FRIEND_PROFILE -> {
                        sendMessage(messageService.createAskConfigProfileMessage(user.getChatId()), createConfigureSecretFriendProfileButtons());
                        return;
                    }
                    case CHANGE_SECRET_FRIEND_AGE -> {
                        user.setStage(CONFIGURE_SECRET_FRIEND_PROFILE);
                        user.setStagePart(StagePart.ASK_AGE);
                    }
                    case CHANGE_SECRET_FRIEND_CITY -> {
                        user.setStage(CONFIGURE_SECRET_FRIEND_PROFILE);
                        user.setStagePart(StagePart.ASK_CITY);
                    }
                    case CHANGE_SECRET_FRIEND_GENDER -> {
                        user.setStage(CONFIGURE_SECRET_FRIEND_PROFILE);
                        user.setStagePart(StagePart.ASK_GENDER);
                    }
                }
            } catch (IllegalInputException e) {
                log.error("Failed to parse command", e);
            }
        }
        Object message = null;

        if (user.getStage().equals(CONFIGURE_PROFILE)) {
            message = switch (user.getStagePart()) {
                case ASK_AGE -> userService.askUserAge(user);
                case SET_AGE -> {
                    buttons = createMainButtons();
                    yield userService.setSecretFriendAge(user, update);
                }
                case ASK_CITY -> userService.askUserCity(user);
                case SET_CITY -> {
                    buttons = createMainButtons();
                    yield userService.setSecretFriendCity(user, update);
                }
                case ASK_GENDER -> userService.askUserGender(user);
                case SET_GENDER -> {
                    buttons = createMainButtons();
                    yield userService.setUserGender(user, update);
                }
                default -> null;
            };
        }

        if (user.getStage().equals(CONFIGURE_SECRET_FRIEND_PROFILE)) {
            message = switch (user.getStagePart()) {
                case ASK_AGE -> userService.askSecretFriendAge(user);
                case SET_AGE -> {
                    buttons = createMainButtons();
                    yield userService.setSecretFriendAge(user, update);
                }
                case ASK_CITY -> userService.askSecretFriendCity(user);
                case SET_CITY -> {
                    buttons = createMainButtons();
                    yield userService.setSecretFriendCity(user, update);
                }
                case ASK_GENDER -> userService.askSecretFriendGender(user);
                case SET_GENDER -> {
                    buttons = createMainButtons();
                    yield userService.setSecretFriendGender(user, update);
                }
                default -> null;
            };
        }

        if (user.getStage().equals(CONFIGURE_FULL_PROFILE)) {
            message = userService.userConfig(user, update);
        }
        if (user.getStage().equals(CONFIGURE_FULL_SECRET_FRIEND_PROFILE)) {
            message = userService.userSecretFriendConfig(user, update);
        }

        sendMessage(message, buttons);
    }

    private void sendMessage(Object message, ReplyKeyboardMarkup buttons) {
        if (message == null) {
            return;
        }

        try {
            if (message instanceof SendMessage msg) {
                if (msg.getReplyMarkup() == null && buttons != null) {
                    msg.setReplyMarkup(buttons);
                }
                execute(msg);
            } else if (message instanceof AnswerCallbackQuery answer) {
                execute(answer);
            }
        } catch (TelegramApiException e) {
            log.error("Exception: {}", e.toString());
        }
    }

    private ReplyKeyboardMarkup createConfigureProfileButtons() {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(true);

        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow keyboardFirstRow = new KeyboardRow();
        keyboardFirstRow.add(new KeyboardButton(Commands.CHANGE_AGE.command));
        keyboardFirstRow.add(new KeyboardButton(Commands.CHANGE_GENDER.command));
        keyboardFirstRow.add(new KeyboardButton(Commands.CHANGE_CITY.command));

        keyboard.add(keyboardFirstRow);
        replyKeyboardMarkup.setKeyboard(keyboard);
        return replyKeyboardMarkup;
    }

    private ReplyKeyboardMarkup createConfigureSecretFriendProfileButtons() {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(true);

        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow keyboardFirstRow = new KeyboardRow();
        keyboardFirstRow.add(new KeyboardButton(Commands.CHANGE_SECRET_FRIEND_AGE.command));
        keyboardFirstRow.add(new KeyboardButton(Commands.CHANGE_SECRET_FRIEND_GENDER.command));
        keyboardFirstRow.add(new KeyboardButton(Commands.CHANGE_SECRET_FRIEND_CITY.command));

        keyboard.add(keyboardFirstRow);
        replyKeyboardMarkup.setKeyboard(keyboard);
        return replyKeyboardMarkup;
    }

    private ReplyKeyboardMarkup createMainButtons() {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(false);

        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow keyboardFirstRow = new KeyboardRow();
        keyboardFirstRow.add(new KeyboardButton(Commands.CONFIGURE_PROFILE.command));
        keyboardFirstRow.add(new KeyboardButton(Commands.CONFIGURE_SECRET_FRIEND_PROFILE.command));

        keyboard.add(keyboardFirstRow);
        replyKeyboardMarkup.setKeyboard(keyboard);
        return replyKeyboardMarkup;
    }
}
