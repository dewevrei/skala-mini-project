package com.dewevrei.aikanban.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_google_id", columnNames = "google_id"),
        @UniqueConstraint(name = "uk_users_email", columnNames = "email"),
        @UniqueConstraint(name = "uk_users_nickname", columnNames = "nickname")
})
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "google_id", nullable = false, length = 255)
    private String googleId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, length = 320)
    private String email;

    @Column(nullable = false, length = 255)
    private String nickname;

    protected User() {
    }

    public User(String googleId, String name, String email, String nickname) {
        this.googleId = googleId;
        this.name = name;
        this.email = email;
        this.nickname = nickname;
    }

    public Long getId() { return id; }
    public String getGoogleId() { return googleId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getNickname() { return nickname; }

    public void updateGoogleProfile(String name, String email) {
        this.name = name;
        this.email = email;
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }
}
