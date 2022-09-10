package com.mazurok.secretFriend.repository.entity;

import com.mazurok.secretFriend.exceptions.IllegalInputException;
import com.mazurok.secretFriend.exceptions.NotFoundException;

import java.util.Map;
import java.util.NoSuchElementException;

public enum Commands {
    START("/start",
            "/start"),
    CONFIGURE_PROFILE("| Configure profile |",
            "| Налаштувати профіль |"),
    CONFIGURE_SECRET_FRIEND_PROFILE("| Configure profile for your secret friend |",
            "| Налаштувати критерії пошуку |"),
    SHOW_PROFILE("| Show my profile |",
            "| Показати мій профіль |"),

    GET_RANDOM_FRIEND("| Get random secret friend |",
            "| Знайти випадкового друга |"),
    START_AUTOMATIC_SEARCH("| Start automatic search for a secret friend |",
            "| Почати автоматичний пошук друга |"),

    STOP_MESSAGING("| Stop messaging |", "| Зупинити спілкування |"),
    BLOCK_USER("| Block user |", "| Заблокувати користувача |"),
    CANCEL("| Cancel |", "| Відмінити |");


    public final Map<String, String> command;

    Commands(String cmdEn, String cmdUk) {
        command = Map.of(Language.en.name(), cmdEn, Language.uk.name(), cmdUk);
    }

    public String getLocalizedText(Language language) {
        return this.command.getOrDefault(language.name(), this.command.get(Language.en.name()));
    }

    public static Commands fromString(String text) throws NotFoundException {
        for (Commands c : Commands.values()) {
            if (c.command.containsValue(text)) {
                return c;
            }
        }
        throw new NotFoundException("This command is not supported!");
    }

}
