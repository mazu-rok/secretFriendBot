package com.mazurok.secretFriend.service;

import com.mazurok.secretFriend.repository.entity.Commands;
import com.mazurok.secretFriend.repository.entity.Language;
import com.mazurok.secretFriend.repository.entity.UserEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.List;

@Slf4j
@Service
public class ButtonsService {

    public ReplyKeyboardMarkup createMainButtons(Language language) {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
//        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
//        replyKeyboardMarkup.setOneTimeKeyboard(true);

        KeyboardRow keyboardRow1 = new KeyboardRow();
        keyboardRow1.add(new KeyboardButton(Commands.CONFIGURE_PROFILE.getLocalizedText(language)));
        keyboardRow1.add(new KeyboardButton(Commands.CONFIGURE_SECRET_FRIEND_PROFILE.getLocalizedText(language)));

        KeyboardRow keyboardRow2 = new KeyboardRow();
        keyboardRow2.add(new KeyboardButton(Commands.SHOW_PROFILE.getLocalizedText(language)));

        KeyboardRow keyboardRow3 = new KeyboardRow();
        keyboardRow3.add(new KeyboardButton(Commands.GET_RANDOM_FRIEND.getLocalizedText(language)));
//        keyboardRow3.add(new KeyboardButton(Commands.START_AUTOMATIC_SEARCH.command));

        replyKeyboardMarkup.setKeyboard(List.of(keyboardRow1, keyboardRow2, keyboardRow3));
        return replyKeyboardMarkup;
    }

    public ReplyKeyboardMarkup createMessagingButtons(Language lang) {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
//        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
//        replyKeyboardMarkup.setOneTimeKeyboard(true);

        KeyboardRow keyboardRow1 = new KeyboardRow();
        keyboardRow1.add(new KeyboardButton(Commands.STOP_MESSAGING.getLocalizedText(lang)));
        keyboardRow1.add(new KeyboardButton(Commands.BLOCK_USER.getLocalizedText(lang)));

        replyKeyboardMarkup.setKeyboard(List.of(keyboardRow1));
        return replyKeyboardMarkup;
    }

    public ReplyKeyboardMarkup createCancelButton(Language lang) {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
//        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
//        replyKeyboardMarkup.setOneTimeKeyboard(true);

        KeyboardRow keyboardRow1 = new KeyboardRow();
        keyboardRow1.add(new KeyboardButton(Commands.CANCEL.getLocalizedText(lang)));

        replyKeyboardMarkup.setKeyboard(List.of(keyboardRow1));
        return replyKeyboardMarkup;
    }
}
