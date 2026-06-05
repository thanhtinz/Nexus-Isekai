package com.fantasyrealm.model.entity;
import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="friendships",
    uniqueConstraints=@UniqueConstraint(columnNames={"char_a","char_b"}))
public class FriendshipEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="char_a",nullable=false) private Long charA;
    @Column(name="char_b",nullable=false) private Long charB;
    @Column(name="rel_type",length=16) private String relType="FRIEND";
    private int intimacy=0;
    @Column(name="created_at") private Instant createdAt=Instant.now();

    public FriendshipEntity() {}
    public FriendshipEntity(long a, long b) {
        // luôn giữ char_a < char_b theo ràng buộc CHECK
        this.charA = Math.min(a,b);
        this.charB = Math.max(a,b);
    }
    public Long getId(){return id;}
    public Long getCharA(){return charA;} public void setCharA(Long v){charA=v;}
    public Long getCharB(){return charB;} public void setCharB(Long v){charB=v;}
    public String getRelType(){return relType;} public void setRelType(String v){relType=v;}
    public int getIntimacy(){return intimacy;} public void setIntimacy(int v){intimacy=v;}
    public Instant getCreatedAt(){return createdAt;}
}
