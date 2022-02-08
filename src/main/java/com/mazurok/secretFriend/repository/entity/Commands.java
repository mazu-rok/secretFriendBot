package com.mazurok.secretFriend.repository.entity;

import com.mazurok.secretFriend.exceptions.IllegalInputException;

import java.util.Map;

public enum Commands {
    START("/start",
            "/start"),
    CONFIGURE_PROFILE("/cmd: Configure profile",
            "/cmd: Налаштувати профіль"),
    CONFIGURE_SECRET_FRIEND_PROFILE("/cmd: Configure profile for your secret friend",
            "/cmd: Налаштувати профіль секретного друга"),
    SHOW_PROFILE("/cmd: Show my profile",
            "/cmd: Показати мій профіль"),

    GET_RANDOM_FRIEND("/cmd: Get random secret friend",
            "/cmd: Знайти випадкового друга"),
    START_AUTOMATIC_SEARCH("/cmd: Start automatic search for a secret friend",
            "/cmd: Почати автоматичний пошук друга");


    public final Map<String, String> command;

    Commands(String cmdEn, String cmdUk) {
        command = Map.of("en", cmdEn, "uk", cmdUk);
    }

    public static Commands fromString(String text) throws IllegalInputException {
        for (Commands c : Commands.values()) {
            if (c.command.containsValue(text)) {
                return c;
            }
        }
        throw new IllegalInputException("This command is not supported!");
    }

}
