package com.fantasyrealm.model.entity;
import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="market_listings",
    indexes={@Index(name="idx_listing_active",columnList="is_active,item_id"),
             @Index(name="idx_listing_seller",columnList="seller_id")})
public class MarketListingEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="seller_id",nullable=false) private Long sellerId;
    @Column(name="item_id",nullable=false) private Long itemId;
    @Column(nullable=false) private int quantity;
    @Column(nullable=false) private long price;
    @Column(name="listed_at") private Instant listedAt=Instant.now();
    @Column(name="expires_at") private Instant expiresAt;
    @Column(name="is_active") private boolean active=true;

    public Long getId(){return id;}
    public Long getSellerId(){return sellerId;} public void setSellerId(Long v){sellerId=v;}
    public Long getItemId(){return itemId;} public void setItemId(Long v){itemId=v;}
    public int getQuantity(){return quantity;} public void setQuantity(int v){quantity=v;}
    public long getPrice(){return price;} public void setPrice(long v){price=v;}
    public boolean isActive(){return active;} public void setActive(boolean v){active=v;}
    public Instant getListedAt(){return listedAt;}
    public Instant getExpiresAt(){return expiresAt;} public void setExpiresAt(Instant v){expiresAt=v;}
}
