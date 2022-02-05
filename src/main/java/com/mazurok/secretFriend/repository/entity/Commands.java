package com.mazurok.secretFriend.repository.entity;

import com.mazurok.secretFriend.exceptions.IllegalInputException;

public enum Commands {
    START("/start"),
    CONFIGURE_PROFILE("/cmd: Configure profile"),
    CONFIGURE_SECRET_FRIEND_PROFILE("/cmd: Configure profile for your secret friend"),
    SHOW_PROFILE("/cmd: Show my profile"),

    GET_RANDOM_FRIEND("/cmd: Get random secret friend"),
    START_AUTOMATIC_SEARCH("/cmd: Start automatic search for a secret friend");

    public final String command;

    Commands(String command) {
        this.command = command;
    }

    public static Commands fromString(String text) throws IllegalInputException {
        for (Commands c : Commands.values()) {
            if (c.command.equals(text)) {
                return c;
            }
        }
        throw new IllegalInputException("This command is not supported!");
    }

}
