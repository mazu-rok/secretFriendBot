package com.mazurok.secretFriend.repository.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class UserEntity {
    /* Telegram properties */
    @Id
    private Long id;
    private String firstName;
    private String lastName;
    private String userName;
    private Long chatId;

    /* My properties */
    private Gender gender;
    private Integer age;
    private String city;
    private Stage stage;
    private StagePart stagePart;
    @Builder.Default
    private Date createdAt = Calendar.getInstance(TimeZone.getDefault()).getTime();
    private Date updatedAt;
    private Date deletedAt;

    private UserEntity secretFriend;
    @Builder.Default
    private SecretFriendConfig secretFriendConfig = new SecretFriendConfig();

}
