package com.mazurok.secretFriend.repository.entity;

import com.mazurok.secretFriend.exceptions.IllegalInputException;

public enum Commands {
    START("/start"),
    CONFIGURE_PROFILE("/cmd: Configure profile"),
    CHANGE_GENDER("/cmd: Change gender"),
    CHANGE_AGE("/cmd: Change age"),
    CHANGE_CITY("/cmd: Change city"),

    CONFIGURE_SECRET_FRIEND_PROFILE("/cmd: Configure profile for your secret friend"),
    CHANGE_SECRET_FRIEND_GENDER("/cmd: Change gender for your secret friend"),
    CHANGE_SECRET_FRIEND_AGE("/cmd: Change age for your secret friend"),
    CHANGE_SECRET_FRIEND_CITY("/cmd: Change city for your secret friend"),

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
