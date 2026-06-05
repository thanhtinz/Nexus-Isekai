package com.fantasyrealm.player;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Đọc cấu hình tạo nhân vật (char_races, char_options) từ DB —
 * cùng nguồn admin panel cấu hình. Validate lựa chọn của client.
 */
@Service
public class CharCreationService {

    @Autowired private JdbcTemplate jdbc;

    /** Danh sách chủng tộc/giới tính đang bật. */
    public List<Map<String,Object>> getEnabledRaces() {
        return jdbc.queryForList(
            "SELECT code,race,race_name_vn,gender,faction_id FROM char_races " +
            "WHERE is_enabled=TRUE ORDER BY sort_order,id");
    }

    /** Lựa chọn theo slot (skin/eyes/hair/outfit) đang bật. */
    public List<Map<String,Object>> getOptions(String slot) {
        return jdbc.queryForList(
            "SELECT code,name_vn,color_index,race_filter,gender_filter,is_default " +
            "FROM char_options WHERE slot=? AND is_enabled=TRUE ORDER BY sort_order,id", slot);
    }

    /** Kiểm tra 1 lựa chọn có hợp lệ cho race/gender không. */
    public boolean isValidOption(String slot, String code, String race, String gender) {
        if (code == null || code.isEmpty()) return true; // cho phép bỏ trống (vd không tóc)
        List<Map<String,Object>> rows = jdbc.queryForList(
            "SELECT race_filter,gender_filter FROM char_options " +
            "WHERE slot=? AND code=? AND is_enabled=TRUE", slot, code);
        if (rows.isEmpty()) return false;
        Map<String,Object> o = rows.get(0);
        String rf = (String) o.get("race_filter");
        String gf = (String) o.get("gender_filter");
        if (rf != null && !rf.isBlank() && !Arrays.asList(rf.split(",")).contains(race)) return false;
        if (gf != null && !gf.isBlank() && !gf.equals(gender)) return false;
        return true;
    }

    /** Kiểm tra race code có tồn tại & bật. */
    public Map<String,Object> getRace(String code) {
        List<Map<String,Object>> r = jdbc.queryForList(
            "SELECT code,race,gender,faction_id FROM char_races WHERE code=? AND is_enabled=TRUE", code);
        return r.isEmpty() ? null : r.get(0);
    }

    /** Validate toàn bộ lựa chọn tạo nhân vật. Trả null nếu OK, hoặc thông báo lỗi. */
    public String validateCreation(String raceCode, String skin, String eyes, String hair, String outfit) {
        Map<String,Object> race = getRace(raceCode);
        if (race == null) return "Chủng tộc không hợp lệ";
        String r = (String) race.get("race");
        String g = (String) race.get("gender");
        if (!isValidOption("skin",   skin,   r, g)) return "Tông da không hợp lệ";
        if (!isValidOption("eyes",   eyes,   r, g)) return "Màu mắt không hợp lệ";
        if (!isValidOption("hair",   hair,   r, g)) return "Kiểu tóc không hợp lệ";
        if (!isValidOption("outfit", outfit, r, g)) return "Trang phục không hợp lệ";
        return null;
    }
}
