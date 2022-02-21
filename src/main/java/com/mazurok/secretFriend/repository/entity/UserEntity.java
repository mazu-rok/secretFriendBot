package com.mazurok.secretFriend.repository.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.util.Pair;

import java.util.*;

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
    @Builder.Default
    private Stack<Pair<Stage, StagePart>> stages = new Stack<>();
    private Language language;
    @Builder.Default
    private Date createdAt = Calendar.getInstance(TimeZone.getDefault()).getTime();
    private Date updatedAt;
    private Date deletedAt;

    @DBRef(lazy = true)
    @ToString.Exclude
    private UserEntity secretFriend;
    @Builder.Default
    private SecretFriendConfig secretFriendConfig = new SecretFriendConfig();

    public void replaceLastStage(Pair<Stage, StagePart> newStage) {
        this.stages.pop();
        this.stages.add(newStage);
    }

    public void replaceLastStagePart(StagePart newStagePart) {
        Pair<Stage, StagePart> stage = this.stages.pop();
        this.stages.add(Pair.of(stage.getFirst(), newStagePart));
    }
}
