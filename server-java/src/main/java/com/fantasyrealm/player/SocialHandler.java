package com.fantasyrealm.player;
import com.fantasyrealm.protocol.*;
import com.fantasyrealm.repository.MailJpaRepository;
import com.fantasyrealm.repository.FriendshipJpaRepository;
import com.fantasyrealm.model.entity.MailEntity;
import com.fantasyrealm.model.entity.FriendshipEntity;
import com.fantasyrealm.inventory.InventoryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.time.Instant;

@Component
public class SocialHandler {
    private static final Logger log = LoggerFactory.getLogger(SocialHandler.class);
    @Autowired private SessionManager     sessions;
    @Autowired private MailJpaRepository  mailRepo;
    @Autowired private FriendshipJpaRepository friendRepo;
    @Autowired private InventoryManager   inventory;

    public void onFriendRequest(PlayerSession s, Packet p) {
        long targetId = p.readLong();
        PlayerSession target = sessions.getByPlayerId(targetId);
        if (target == null) { s.send(err("Người chơi không online")); return; }
        Packet req = new Packet(PacketType.S_FRIEND_REQUEST)
            .writeLong(s.getPlayerId()).writeString(s.getCharacterName());
        target.send(req);
    }

    public void onFriendAccept(PlayerSession s, Packet p) {
        long fromId = p.readLong();
        PlayerSession from = sessions.getByPlayerId(fromId);

        // Lưu quan hệ bạn bè vào DB (dùng characterId, giữ char_a < char_b)
        long meChar   = s.getCharacterId();
        long fromChar = (from != null) ? from.getCharacterId() : fromId;
        if (meChar > 0 && fromChar > 0 && meChar != fromChar) {
            long a = Math.min(meChar, fromChar), b = Math.max(meChar, fromChar);
            try {
                if (!friendRepo.existsByCharAAndCharB(a, b)) {
                    friendRepo.save(new FriendshipEntity(a, b));
                    log.info("Friendship saved: {} <-> {}", a, b);
                }
            } catch (Exception e) { log.warn("Lưu friendship lỗi: {}", e.getMessage()); }
        }

        if (from != null) {
            from.send(new Packet(PacketType.S_FRIEND_ACCEPT)
                .writeLong(s.getPlayerId()).writeString(s.getCharacterName()));
        }
    }

    public void onMailSend(PlayerSession s, Packet p) {
        long   toId    = p.readLong();
        String subject = p.readString();
        String body    = p.readString();
        long   itemId  = p.readLong();  // 0 = no item
        long   gold    = p.readLong();  // 0 = no gold

        if (gold < 0 || gold > s.getGold()) { s.send(err("Không đủ gold")); return; }

        MailEntity mail = new MailEntity();
        mail.setFromId(s.getCharacterId());
        mail.setToId(toId);
        mail.setSubject(subject.length() > 128 ? subject.substring(0,128) : subject);
        mail.setBody(body.length() > 5000 ? body.substring(0,5000) : body);
        mail.setItemId(itemId > 0 ? itemId : null);
        mail.setGoldAmount(gold);
        mail.setExpiresAt(Instant.now().plusSeconds(30L*24*3600));
        mailRepo.save(mail);

        if (gold > 0) s.setGold(s.getGold() - gold);

        PlayerSession target = sessions.getByPlayerId(toId);
        if (target != null) {
            target.send(new Packet(PacketType.S_MAIL_RECEIVE)
                .writeLong(s.getPlayerId()).writeString(s.getCharacterName())
                .writeString(subject).writeString(body).writeLong(itemId).writeLong(gold));
        }
        log.info("Mail {} -> {} gold={}", s.getCharacterName(), toId, gold);
    }

    public void onGiftSend(PlayerSession s, Packet p) {
        long   toId   = p.readLong();
        long   itemId = p.readLong();
        String msg    = p.readString();

        // Kiểm tra & trừ vật phẩm khỏi túi người gửi
        if (!inventory.has(s.getCharacterId(), itemId, 1)) {
            s.send(err("Bạn không có vật phẩm này"));
            return;
        }
        if (!inventory.remove(s.getCharacterId(), itemId, 1)) {
            s.send(err("Không thể gửi quà"));
            return;
        }

        PlayerSession target = sessions.getByPlayerId(toId);
        if (target != null) {
            // Người nhận online → cộng thẳng vào túi + thông báo
            inventory.add(target.getCharacterId(), itemId, 1);
            target.send(new Packet(PacketType.S_GIFT_RECEIVE)
                .writeLong(s.getPlayerId()).writeString(s.getCharacterName())
                .writeLong(itemId).writeString(msg));
        } else {
            // Người nhận offline → gửi qua hòm thư kèm vật phẩm
            MailEntity mail = new MailEntity();
            mail.setFromId(s.getCharacterId());
            mail.setToId(toId);
            mail.setSubject("Bạn nhận được một món quà");
            mail.setBody(msg == null ? "" : msg);
            mail.setItemId(itemId);
            mail.setGoldAmount(0);
            mail.setExpiresAt(Instant.now().plusSeconds(30L*24*3600));
            mailRepo.save(mail);
        }
        log.info("Gift {} -> {} item={}", s.getCharacterName(), toId, itemId);
    }

    public void onDonate(PlayerSession s, Packet p) {
        long toId  = p.readLong();
        long amount= p.readLong();
        int  type  = p.readByte(); // 0=gold 1=flower 2=heart
        if (amount < 0 || amount > s.getGold()) { s.send(err("Không đủ gold")); return; }
        PlayerSession target = sessions.getByPlayerId(toId);
        if (target == null) { s.send(err("Người biểu diễn không online")); return; }
        if (amount > 0) {
            s.setGold(s.getGold() - amount);
            target.setGold(target.getGold() + amount);
        }
        Packet d = new Packet(PacketType.S_DONATE_RESULT)
            .writeLong(s.getPlayerId()).writeString(s.getCharacterName())
            .writeLong(amount).writeByte(type);
        target.send(d);
        s.send(d);
    }

    private Packet err(String msg) {
        return new Packet(PacketType.S_ERROR).writeString(msg);
    }
}
