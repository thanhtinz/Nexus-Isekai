package com.fantasyrealm.player;

import com.fantasyrealm.model.entity.CharacterEntity;
import com.fantasyrealm.protocol.Packet;
import com.fantasyrealm.protocol.PacketType;
import com.fantasyrealm.repository.CharacterJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Xử lý màn tạo nhân vật:
 *  - C_CHAR_CREATE_OPTIONS: client xin danh sách lựa chọn (race/skin/eyes/hair/outfit)
 *  - C_CHAR_CREATE:         client gửi lựa chọn → validate → tạo character
 */
@Component
public class CharCreationHandler {
    private static final Logger log = LoggerFactory.getLogger(CharCreationHandler.class);

    @Autowired private CharCreationService creation;
    @Autowired private CharacterJpaRepository charRepo;
    @Autowired private SessionManager sessions;

    /** Gửi toàn bộ option cho client dựng UI. Đóng gói dạng JSON cho gọn. */
    public void onRequestOptions(PlayerSession s, Packet p) {
        StringBuilder json = new StringBuilder("{");
        json.append("\"races\":").append(toJson(creation.getEnabledRaces())).append(',');
        json.append("\"skin\":").append(toJson(creation.getOptions("skin"))).append(',');
        json.append("\"eyes\":").append(toJson(creation.getOptions("eyes"))).append(',');
        json.append("\"hair\":").append(toJson(creation.getOptions("hair"))).append(',');
        json.append("\"outfit\":").append(toJson(creation.getOptions("outfit")));
        json.append('}');
        s.send(new Packet(PacketType.S_CHAR_CREATE_OPTIONS).writeString(json.toString()));
    }

    /** Client gửi: name, raceCode, skin, eyes, hair, outfit. */
    public void onCreate(PlayerSession s, Packet p) {
        String name     = p.readString();
        String raceCode = p.readString();
        String skin     = p.readString();
        String eyes     = p.readString();
        String hair     = p.readString();
        String outfit   = p.readString();

        // Validate tên
        if (name == null || name.length() < 2 || name.length() > 16) {
            s.send(new Packet(PacketType.S_CHAR_CREATE_FAIL).writeString("Tên phải 2-16 ký tự"));
            return;
        }
        if (charRepo.existsByName(name)) {
            s.send(new Packet(PacketType.S_CHAR_CREATE_FAIL).writeString("Tên đã tồn tại"));
            return;
        }

        // Validate lựa chọn ngoại hình theo cấu hình admin
        String err = creation.validateCreation(raceCode, skin, eyes, hair, outfit);
        if (err != null) {
            s.send(new Packet(PacketType.S_CHAR_CREATE_FAIL).writeString(err));
            return;
        }

        Map<String,Object> race = creation.getRace(raceCode);
        int factionId = race.get("faction_id") != null ? ((Number) race.get("faction_id")).intValue() : 0;

        // Lưu lựa chọn ngoại hình vào outfit_json
        String outfitJson = String.format(
            "{\"race\":\"%s\",\"skin\":\"%s\",\"eyes\":\"%s\",\"hair\":\"%s\",\"outfit\":\"%s\"}",
            raceCode, nz(skin), nz(eyes), nz(hair), nz(outfit));

        CharacterEntity c = new CharacterEntity();
        c.setPlayerId(s.getPlayerId());
        c.setName(name);
        c.setFactionId(factionId);
        c.setOutfitJson(outfitJson);
        charRepo.save(c);

        // Cập nhật session để các thao tác sau (đổi outfit, chat...) hoạt động
        s.setCharacterId(c.getId());
        s.setCharacterName(name);
        s.setOutfitJson(outfitJson);

        log.info("Tạo nhân vật: {} (player {}, race {})", name, s.getPlayerId(), raceCode);
        s.send(new Packet(PacketType.S_CHAR_CREATE_OK)
            .writeLong(c.getId())
            .writeString(name)
            .writeInt(factionId)
            .writeString(outfitJson));
    }

    private static String nz(String v) { return v == null ? "" : v; }

    /** JSON serializer tối giản cho List<Map> (tránh thêm dependency). */
    private static String toJson(List<Map<String,Object>> rows) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < rows.size(); i++) {
            if (i > 0) sb.append(',');
            Map<String,Object> r = rows.get(i);
            sb.append('{');
            boolean first = true;
            for (Map.Entry<String,Object> e : r.entrySet()) {
                Object v = e.getValue();
                if (v == null) continue; // bỏ field null → client dùng default, parse an toàn
                if (!first) sb.append(','); first = false;
                sb.append('"').append(e.getKey()).append("\":");
                if (v instanceof Number || v instanceof Boolean) sb.append(v);
                else sb.append('"').append(esc(v.toString())).append('"');
            }
            sb.append('}');
        }
        return sb.append(']').toString();
    }
    private static String esc(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
