package com.mazurok.secretFriend.repository.entity;

import lombok.Data;

@Data
public class SecretFriendConfig {
    private Gender gender;

    private Integer minAge;
    private Integer maxAge;

    private String city;
}
