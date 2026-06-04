package com.fantasyrealm.model.entity;
import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="players",
    indexes={@Index(name="idx_player_username",columnList="username"),
             @Index(name="idx_player_email",columnList="email")})
public class PlayerEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(unique=true,nullable=false,length=32) private String username;
    @Column(unique=true,nullable=false,length=128) private String email;
    @Column(name="password_hash",nullable=false) private String passwordHash;
    @Column(nullable=false,length=64) private String salt;
    @Column(name="created_at") private Instant createdAt=Instant.now();
    @Column(name="last_login") private Instant lastLogin;
    @Column(name="is_banned") private boolean banned=false;
    @Column(name="ban_reason") private String banReason;
    @Column(name="premium_expires") private Instant premiumExpires;

    public Long getId(){return id;}
    public String getUsername(){return username;} public void setUsername(String v){username=v;}
    public String getEmail(){return email;} public void setEmail(String v){email=v;}
    public String getPasswordHash(){return passwordHash;} public void setPasswordHash(String v){passwordHash=v;}
    public String getSalt(){return salt;} public void setSalt(String v){salt=v;}
    public Instant getLastLogin(){return lastLogin;} public void setLastLogin(Instant v){lastLogin=v;}
    public boolean isBanned(){return banned;} public void setBanned(boolean v){banned=v;}
    public String getBanReason(){return banReason;} public void setBanReason(String v){banReason=v;}
    public Instant getPremiumExpires(){return premiumExpires;} public void setPremiumExpires(Instant v){premiumExpires=v;}
}
