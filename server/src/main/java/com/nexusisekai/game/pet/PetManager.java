package com.nexusisekai.game.pet;

import com.nexusisekai.database.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quản lý Pet và Mount.
 */
public class PetManager {

    private static final Logger log = LoggerFactory.getLogger(PetManager.class);
    private static PetManager INSTANCE;
    private final Map<Integer, PetTemplate> petCache = new ConcurrentHashMap<>();
    private final Map<Integer, MountTemplate> mountCache = new ConcurrentHashMap<>();

    public static synchronized PetManager getInstance() {
        if (INSTANCE == null) INSTANCE = new PetManager();
        return INSTANCE;
    }

    public void loadAll() throws SQLException {
        petCache.clear(); mountCache.clear();
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            // Pets
            try (PreparedStatement ps = c.prepareStatement("SELECT * FROM pet_templates WHERE is_active=1");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PetTemplate t = new PetTemplate(
                        rs.getInt("id"), rs.getString("name"), rs.getString("element"),
                        rs.getInt("rarity"), rs.getInt("base_hp"), rs.getInt("base_atk"),
                        rs.getInt("base_def"), rs.getInt("skill_id"), rs.getInt("icon_id"));
                    t.personalities = rs.getString("personalities"); t.colors = rs.getString("colors");
                    t.catchRate = rs.getInt("catch_rate"); t.capturable = rs.getInt("capturable"); t.evolveTo = rs.getInt("evolve_to");
                    petCache.put(t.id, t);
                }
            }
            // Mounts
            try (PreparedStatement ps = c.prepareStatement("SELECT * FROM mount_templates WHERE is_active=1");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    MountTemplate t = new MountTemplate(
                        rs.getInt("id"), rs.getString("name"), rs.getFloat("speed_bonus"),
                        rs.getInt("rarity"), rs.getInt("icon_id"));
                    mountCache.put(t.id, t);
                }
            }
        }
        log.info("[PET] Loaded {} pets, {} mounts.", petCache.size(), mountCache.size());
    }

    // ─── Pet operations ───────────────────────

    private static final java.util.Random RNG = new java.util.Random();
    /** Chon ngau nhien 1 phan tu tu pool CSV (tinh cach / mau). */
    private static String pick(String csv) {
        if (csv == null || csv.isEmpty()) return "";
        String[] a = csv.split(",");
        return a[RNG.nextInt(a.length)].trim();
    }

    /** Nhan 1 pet: tu roll tinh cach + mau tu pool cua template (moi con mot ve). */
    public long givePet(long charId, int templateId) throws SQLException {
        PetTemplate t = petCache.get(templateId);
        String personality = t != null ? pick(t.personalities) : "";
        String color = t != null ? pick(t.colors) : "";
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "INSERT INTO player_pets (char_id,template_id,personality,color) VALUES (?,?,?,?)",
                 Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, charId); ps.setInt(2, templateId);
            ps.setString(3, personality); ps.setString(4, color);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            return keys.next() ? keys.getLong(1) : -1;
        }
    }

    /**
     * Bat thu hoang bang KET THAN (khong combat): kiem capturable + roll catch_rate.
     * Tra ve: -1 = khong the bat, 0 = truot (thu chay), >0 = bat duoc (petId).
     */
    public long catchPet(long charId, int templateId) throws SQLException {
        PetTemplate t = petCache.get(templateId);
        if (t == null || t.capturable == 0) return -1;
        if (RNG.nextInt(100) >= Math.max(1, t.catchRate)) return 0;
        return givePet(charId, templateId);
    }

    /** Tien hoa pet len template evolve_to khi du than thiet (loyalty>=80). Tra ve true neu thanh cong. */
    public boolean evolvePet(long petId, long charId) throws SQLException {
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT template_id, loyalty FROM player_pets WHERE id=? AND char_id=?")) {
            ps.setLong(1, petId); ps.setLong(2, charId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return false;
            int tid = rs.getInt("template_id"), loyalty = rs.getInt("loyalty");
            PetTemplate t = petCache.get(tid);
            if (t == null || t.evolveTo == 0 || loyalty < 80) return false;
            PreparedStatement upd = c.prepareStatement(
                "UPDATE player_pets SET template_id=? WHERE id=? AND char_id=?");
            upd.setInt(1, t.evolveTo); upd.setLong(2, petId); upd.setLong(3, charId);
            upd.executeUpdate();
            return true;
        }
    }

    public List<PlayerPet> getPlayerPets(long charId) throws SQLException {
        List<PlayerPet> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT p.*, t.name as tname, t.element, t.rarity, t.icon_id " +
                 "FROM player_pets p JOIN pet_templates t ON t.id=p.template_id " +
                 "WHERE p.char_id=? ORDER BY p.id")) {
            ps.setLong(1, charId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                PlayerPet p = new PlayerPet();
                p.id = rs.getLong("id"); p.charId = charId;
                p.templateId = rs.getInt("template_id");
                p.name = rs.getString("nickname") != null ? rs.getString("nickname") : rs.getString("tname");
                p.element = rs.getString("element"); p.rarity = rs.getInt("rarity");
                p.level = rs.getInt("level"); p.exp = rs.getInt("exp");
                p.hunger = rs.getInt("hunger"); p.loyalty = rs.getInt("loyalty");
                p.isActive = rs.getInt("is_active") == 1; p.iconId = rs.getInt("icon_id");
                list.add(p);
            }
        }
        return list;
    }

    public void setActivePet(long charId, long petId) throws SQLException {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            c.prepareStatement("UPDATE player_pets SET is_active=0 WHERE char_id=" + charId)
                .executeUpdate();
            PreparedStatement ps = c.prepareStatement(
                "UPDATE player_pets SET is_active=1 WHERE id=? AND char_id=?");
            ps.setLong(1, petId); ps.setLong(2, charId); ps.executeUpdate();
        }
    }

    public void feedPet(long petId, long charId, int foodItemId) throws SQLException {
        // Tăng hunger & loyalty
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "UPDATE player_pets SET hunger=LEAST(hunger+30,100),loyalty=LEAST(loyalty+5,100) " +
                 "WHERE id=? AND char_id=?")) {
            ps.setLong(1, petId); ps.setLong(2, charId); ps.executeUpdate();
        }
    }

    public void levelUpPet(long petId, long charId, int expGain) throws SQLException {
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT level, exp FROM player_pets WHERE id=? AND char_id=?")) {
            ps.setLong(1, petId); ps.setLong(2, charId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return;
            int level = rs.getInt("level");
            int exp   = rs.getInt("exp") + expGain;
            int expToNext = level * 100;
            while (exp >= expToNext && level < 100) {
                exp -= expToNext; level++;
                expToNext = level * 100;
            }
            int finalLevel = level, finalExp = exp;
            PreparedStatement upd = c.prepareStatement(
                "UPDATE player_pets SET level=?,exp=? WHERE id=?");
            upd.setInt(1, finalLevel); upd.setInt(2, finalExp); upd.setLong(3, petId);
            upd.executeUpdate();
        }
    }

    // ─── Mount operations ─────────────────────

    public void giveMount(long charId, int templateId) throws SQLException {
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "INSERT IGNORE INTO player_mounts (char_id,template_id) VALUES (?,?)")) {
            ps.setLong(1, charId); ps.setInt(2, templateId); ps.executeUpdate();
        }
    }

    public List<PlayerMount> getPlayerMounts(long charId) throws SQLException {
        List<PlayerMount> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT pm.*, t.name as tname, t.speed_bonus, t.rarity, t.icon_id " +
                 "FROM player_mounts pm JOIN mount_templates t ON t.id=pm.template_id " +
                 "WHERE pm.char_id=? ORDER BY pm.id")) {
            ps.setLong(1, charId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                PlayerMount m = new PlayerMount();
                m.id = rs.getLong("id"); m.templateId = rs.getInt("template_id");
                m.name = rs.getString("tname"); m.speedBonus = rs.getFloat("speed_bonus");
                m.rarity = rs.getInt("rarity"); m.level = rs.getInt("level");
                m.isActive = rs.getInt("is_active") == 1; m.iconId = rs.getInt("icon_id");
                list.add(m);
            }
        }
        return list;
    }

    public void setActiveMount(long charId, long mountId) throws SQLException {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            c.prepareStatement("UPDATE player_mounts SET is_active=0 WHERE char_id=" + charId)
                .executeUpdate();
            PreparedStatement ps = c.prepareStatement(
                "UPDATE player_mounts SET is_active=1 WHERE id=? AND char_id=?");
            ps.setLong(1, mountId); ps.setLong(2, charId); ps.executeUpdate();
        }
    }

    public float getActiveMountSpeedBonus(long charId) throws SQLException {
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT t.speed_bonus FROM player_mounts pm " +
                 "JOIN mount_templates t ON t.id=pm.template_id " +
                 "WHERE pm.char_id=? AND pm.is_active=1 LIMIT 1")) {
            ps.setLong(1, charId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getFloat(1) : 0f;
        }
    }

    // ─── Admin ───────────────────────────────

    public Collection<PetTemplate> getAllPetTemplates() { return petCache.values(); }
    public Collection<MountTemplate> getAllMountTemplates() { return mountCache.values(); }

    // ─── DTOs ────────────────────────────────

    public static class PetTemplate {
        public final int id, rarity, baseHp, baseAtk, baseDef, skillId, iconId;
        public final String name, element;
        public String personalities = "", colors = ""; // pool CSV (data-driven tu Studio)
        public int catchRate = 50, capturable = 1, evolveTo = 0;
        public PetTemplate(int id, String name, String el, int rar, int hp, int atk, int def, int skill, int icon) {
            this.id=id; this.name=name; element=el; rarity=rar;
            baseHp=hp; baseAtk=atk; baseDef=def; skillId=skill; iconId=icon;
        }
    }
    public static class MountTemplate {
        public final int id, rarity, iconId; public final String name; public final float speedBonus;
        public MountTemplate(int id, String name, float speed, int rar, int icon) {
            this.id=id; this.name=name; speedBonus=speed; rarity=rar; iconId=icon;
        }
    }
    public static class PlayerPet {
        public long id, charId; public int templateId, rarity, level, exp, hunger, loyalty, iconId;
        public String name, element; public boolean isActive;
    }
    public static class PlayerMount {
        public long id; public int templateId, rarity, level, iconId;
        public String name; public float speedBonus; public boolean isActive;
    }
}
