package com.cherry.server.user.domain;

import com.cherry.server.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String nickname;

    @Column(nullable = false)
    private String password;

    private String profileImageUrl;

    @Builder
    public User(String email, String nickname, String password, String profileImageUrl) {
        this.email = email;
        this.nickname = nickname;
        this.password = password;
        this.profileImageUrl = profileImageUrl;
    }
}
