package com.mazurok.secretFriend.repository.entity;

public enum Language {
    en("English"),
    uk("Українська");

    public final String desc;

    Language(String description) {
        this.desc = description;
    }
}
