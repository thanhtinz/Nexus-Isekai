package com.fantasyrealm.model.entity;
import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="mailbox",
    indexes={@Index(name="idx_mail_to",columnList="to_id,is_read")})
public class MailEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="from_id") private Long fromId;
    @Column(name="to_id",nullable=false) private Long toId;
    @Column(length=128) private String subject;
    @Column(columnDefinition="TEXT") private String body;
    @Column(name="item_id") private Long itemId;
    @Column(name="gold_amount") private long goldAmount=0;
    @Column(name="is_read") private boolean read=false;
    @Column(name="sent_at") private Instant sentAt=Instant.now();
    @Column(name="expires_at") private Instant expiresAt;

    public Long getId(){return id;}
    public Long getFromId(){return fromId;} public void setFromId(Long v){fromId=v;}
    public Long getToId(){return toId;} public void setToId(Long v){toId=v;}
    public String getSubject(){return subject;} public void setSubject(String v){subject=v;}
    public String getBody(){return body;} public void setBody(String v){body=v;}
    public Long getItemId(){return itemId;} public void setItemId(Long v){itemId=v;}
    public long getGoldAmount(){return goldAmount;} public void setGoldAmount(long v){goldAmount=v;}
    public boolean isRead(){return read;} public void setRead(boolean v){read=v;}
    public Instant getSentAt(){return sentAt;}
    public Instant getExpiresAt(){return expiresAt;} public void setExpiresAt(Instant v){expiresAt=v;}
}
