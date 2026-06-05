package com.fantasyrealm.combat;

/**
 * Định nghĩa 1 kỹ năng, nạp động từ DB (bảng skills do admin quản lý).
 * Thay cho enum cứng — thêm skill trong admin là server nạp được.
 */
public record SkillDef(
    String code, String name, String nameVn, String category,
    String classCode, int factionId,
    String effectType, float power,
    int levelReq, int manaCost, int cooldownMs, int rangePx, int buffDurationMs
) {
    /** Nhân vật có dùng được skill này không (theo phe + cấp). */
    public boolean availableFor(int faction, int level) {
        if (factionId != 0 && factionId != faction) return false;
        return level >= levelReq;
    }
}
