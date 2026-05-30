package com.nexusisekai.game.mentor;

import com.nexusisekai.database.DatabaseManager;
import com.nexusisekai.game.shop.ItemManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;

/**
 * Hệ thống Sư Đồ.
 * - Player nhận thầy (player khác hoặc NPC)
 * - Hoàn thành nhiệm vụ → nhận thưởng → xuất sư khi đủ điều kiện
 * - Thầy có thể nhận nhiều đệ tử
 */
public class MentorManager {

    private static final Logger log = LoggerFactory.getLogger(MentorManager.class);
    private static MentorManager INSTANCE;

    public static synchronized MentorManager getInstance() {
        if (INSTANCE == null) INSTANCE = new MentorManager();
        return INSTANCE;
    }

    // ─────────────────────────────────────────
    // Relationship management
    // ─────────────────────────────────────────

    /** Đệ tử bái sư */
    public void acceptMentor(long studentId, long mentorId, boolean isMentorNpc) throws Exception {
        // Kiểm tra đã có thầy chưa
        if (hasActiveMentor(studentId))
            throw new IllegalStateException("Bạn đã có thầy rồi.");
        // Kiểm tra giới hạn đệ tử (tối đa 5)
        if (getMentorStudentCount(mentorId) >= 5)
            throw new IllegalStateException("Thầy đã có đủ 5 đệ tử.");

        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "INSERT INTO mentor_relationships (mentor_id,student_id,is_npc_mentor,status) VALUES (?,?,?,'active')")) {
            ps.setLong(1, mentorId); ps.setLong(2, studentId);
            ps.setInt(3, isMentorNpc ? 1 : 0);
            ps.executeUpdate();
        }
        log.info("[MENTOR] Student={} accepted mentor={} (npc={})", studentId, mentorId, isMentorNpc);
    }

    /** Xuất sư khi hoàn thành tất cả nhiệm vụ bắt buộc */
    public boolean graduate(long studentId) throws Exception {
        MentorRelationship rel = getActiveMentorship(studentId);
        if (rel == null) throw new IllegalStateException("Bạn không có thầy.");

        // Kiểm tra nhiệm vụ xuất sư (loại sort_order = max)
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            PreparedStatement check = c.prepareStatement(
                "SELECT COUNT(*) as total, SUM(p.is_completed) as done " +
                "FROM mentor_missions m " +
                "LEFT JOIN mentor_mission_progress p ON p.mission_id=m.id AND p.relationship_id=? " +
                "WHERE m.id=(SELECT id FROM mentor_missions ORDER BY sort_order DESC LIMIT 1)");
            check.setLong(1, rel.id);
            ResultSet rs = check.executeQuery();
            if (!rs.next() || rs.getInt("done") < 1)
                throw new IllegalStateException("Chưa hoàn thành điều kiện xuất sư.");

            PreparedStatement upd = c.prepareStatement(
                "UPDATE mentor_relationships SET status='graduated',graduated_at=NOW() WHERE id=?");
            upd.setLong(1, rel.id); upd.executeUpdate();
        }
        log.info("[MENTOR] Student={} graduated from mentor={}", studentId, rel.mentorId);
        return true;
    }

    /** Thầy khai trừ đệ tử */
    public void expelStudent(long mentorId, long studentId) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "UPDATE mentor_relationships SET status='broken' WHERE mentor_id=? AND student_id=? AND status='active'")) {
            ps.setLong(1, mentorId); ps.setLong(2, studentId);
            int rows = ps.executeUpdate();
            if (rows == 0) throw new IllegalStateException("Không tìm thấy quan hệ sư đồ.");
        }
    }

    // ─────────────────────────────────────────
    // Mission progress
    // ─────────────────────────────────────────

    public void trackMission(long studentId, String missionType, int value) throws SQLException {
        MentorRelationship rel = getActiveMentorship(studentId);
        if (rel == null) return;

        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            PreparedStatement tasks = c.prepareStatement(
                "SELECT m.id, m.mission_type, m.target_value, m.reward_exp, m.reward_gold, " +
                "m.reward_item_id, COALESCE(p.progress,0) as cur_progress, COALESCE(p.is_completed,0) as done " +
                "FROM mentor_missions m " +
                "LEFT JOIN mentor_mission_progress p ON p.mission_id=m.id AND p.relationship_id=? " +
                "WHERE m.mission_type=?");
            tasks.setLong(1, rel.id); tasks.setString(2, missionType);
            ResultSet rs = tasks.executeQuery();

            while (rs.next()) {
                if (rs.getInt("done") == 1) continue;
                int missionId = rs.getInt("id");
                int target    = rs.getInt("target_value");
                int progress  = rs.getInt("cur_progress") + value;
                boolean completed = progress >= target;

                c.prepareStatement(
                    "INSERT INTO mentor_mission_progress (relationship_id,mission_id,progress,is_completed" +
                    (completed ? ",completed_at" : "") + ") VALUES (" +
                    rel.id + "," + missionId + "," + Math.min(progress,target) + "," +
                    (completed?1:0) + (completed ? ",NOW()" : "") + ") " +
                    "ON DUPLICATE KEY UPDATE progress=LEAST(VALUES(progress)," + target + ")," +
                    "is_completed=" + (completed?1:0) + (completed?",completed_at=NOW()":""))
                    .executeUpdate();

                if (completed) {
                    // Trao thưởng cho đệ tử
                    int expReward  = rs.getInt("reward_exp");
                    int goldReward = rs.getInt("reward_gold");
                    int itemId     = rs.getInt("reward_item_id");

                    if (expReward > 0) c.prepareStatement(
                        "UPDATE characters SET exp=exp+" + expReward + " WHERE id=" + studentId)
                        .executeUpdate();
                    if (goldReward > 0) c.prepareStatement(
                        "UPDATE characters SET gold=gold+" + goldReward + " WHERE id=" + studentId)
                        .executeUpdate();
                    if (itemId > 0) ItemManager.getInstance().giveItem(studentId, itemId, 1);

                    log.info("[MENTOR] Student={} completed mission={}", studentId, missionId);
                }
            }
        }
    }

    // ─────────────────────────────────────────
    // Queries
    // ─────────────────────────────────────────

    public MentorRelationship getActiveMentorship(long studentId) throws SQLException {
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT * FROM mentor_relationships WHERE student_id=? AND status='active' LIMIT 1")) {
            ps.setLong(1, studentId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return null;
            MentorRelationship r = new MentorRelationship();
            r.id = rs.getLong("id"); r.mentorId = rs.getLong("mentor_id");
            r.studentId = studentId; r.isNpcMentor = rs.getInt("is_npc_mentor")==1;
            r.status = rs.getString("status");
            return r;
        }
    }

    public List<MentorRelationship> getStudents(long mentorId) throws SQLException {
        List<MentorRelationship> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT mr.*, ch.name as student_name, ch.level as student_level " +
                 "FROM mentor_relationships mr " +
                 "JOIN characters ch ON ch.id=mr.student_id " +
                 "WHERE mr.mentor_id=? AND mr.status='active'")) {
            ps.setLong(1, mentorId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                MentorRelationship r = new MentorRelationship();
                r.id = rs.getLong("id"); r.mentorId = mentorId;
                r.studentId = rs.getLong("student_id");
                r.studentName = rs.getString("student_name");
                r.studentLevel = rs.getInt("student_level");
                r.status = rs.getString("status");
                list.add(r);
            }
        }
        return list;
    }

    public List<Map<String,Object>> getMissionProgress(long relId) throws SQLException {
        List<Map<String,Object>> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT m.*, COALESCE(p.progress,0) as cur_progress, COALESCE(p.is_completed,0) as done " +
                 "FROM mentor_missions m " +
                 "LEFT JOIN mentor_mission_progress p ON p.mission_id=m.id AND p.relationship_id=? " +
                 "ORDER BY m.sort_order")) {
            ps.setLong(1, relId);
            ResultSet rs = ps.executeQuery();
            ResultSetMetaData meta = rs.getMetaData();
            while (rs.next()) {
                Map<String,Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= meta.getColumnCount(); i++)
                    row.put(meta.getColumnName(i), rs.getObject(i));
                list.add(row);
            }
        }
        return list;
    }

    private boolean hasActiveMentor(long studentId) throws SQLException {
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT COUNT(*) FROM mentor_relationships WHERE student_id=? AND status='active'")) {
            ps.setLong(1, studentId);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    private int getMentorStudentCount(long mentorId) throws SQLException {
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT COUNT(*) FROM mentor_relationships WHERE mentor_id=? AND status='active'")) {
            ps.setLong(1, mentorId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public static class MentorRelationship {
        public long id, mentorId, studentId;
        public boolean isNpcMentor;
        public String status, studentName;
        public int studentLevel;
    }
}
