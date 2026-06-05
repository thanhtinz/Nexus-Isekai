package com.fantasyrealm.player;
import com.fantasyrealm.protocol.*;
import com.fantasyrealm.zone.ZoneManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatHandler {
    private static final int MAX_LEN = 200;
    private static final long COOLDOWN_MS = 500;
    @Autowired private ZoneManager    zoneManager;
    @Autowired private SessionManager sessions;
    @Autowired private com.fantasyrealm.gm.GmService gmService;
    private final ConcurrentHashMap<Long,Long> lastChat = new ConcurrentHashMap<>();

    public void onChat(PlayerSession s, Packet p) {
        String msg = p.readString();
        int    ch  = p.readByte(); // 0=zone 1=faction 2=trade 3=all

        if (msg == null || msg.isBlank() || msg.length() > MAX_LEN) return;

        // GM chat command: tin nhắn bắt đầu "/" → xử lý như lệnh GM
        if (msg.startsWith("/") && s.isGm()) {
            String result = gmService.executeCommand(s, msg);
            s.send(new Packet(PacketType.S_GM_RESULT).writeString(result));
            return;
        }

        if (s.isMuted()) {
            s.send(new Packet(PacketType.S_NOTIFY).writeString("Bạn đang bị cấm chat"));
            return;
        }

        long now = System.currentTimeMillis();
        Long last = lastChat.put(s.getPlayerId(), now);
        if (last != null && now - last < COOLDOWN_MS) return;

        msg = msg.replace("<","&lt;").replace(">","&gt;");

        Packet out = new Packet(PacketType.S_CHAT)
            .writeLong(s.getPlayerId()).writeString(s.getCharacterName())
            .writeString(msg).writeByte(ch);

        switch (ch) {
            case 0 -> zoneManager.broadcastZone(s.getCurrentZoneId(), out);
            case 1 -> sessions.broadcastFaction(
                          s.getFaction() != null ? s.getFaction().id : 0, out);
            case 2 -> zoneManager.broadcastZone(s.getCurrentZoneId(), out);
            case 3 -> sessions.broadcastAll(out);
        }
    }

    public void onWhisper(PlayerSession s, Packet p) {
        long   targetId = p.readLong();
        String msg      = p.readString();
        if (msg == null || msg.isBlank() || msg.length() > MAX_LEN) return;

        PlayerSession target = sessions.getByPlayerId(targetId);
        if (target == null) {
            s.send(new Packet(PacketType.S_ERROR).writeString("Người chơi không online"));
            return;
        }
        Packet w = new Packet(PacketType.S_WHISPER)
            .writeLong(s.getPlayerId()).writeString(s.getCharacterName()).writeString(msg);
        target.send(w);
        s.send(w); // echo to self
    }
}
