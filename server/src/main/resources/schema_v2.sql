-- ============================================================
-- NEXUS ISEKAI - Schema V2 Migration
-- Chạy sau schema.sql để thêm các hệ thống mới
-- ============================================================

-- ─────────────────────────────────────────
-- 1. MULTI-SERVER & MAINTENANCE
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS game_servers (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(64) NOT NULL,
    server_type  TINYINT NOT NULL DEFAULT 0,    -- 0=main, 1=test, 2=event
    host         VARCHAR(128) NOT NULL,
    port         INT NOT NULL DEFAULT 7777,
    admin_port   INT NOT NULL DEFAULT 8080,
    status       TINYINT NOT NULL DEFAULT 0,    -- 0=offline,1=online,2=maintenance
    open_time    DATETIME NULL,                 -- NULL = luôn mở
    close_time   DATETIME NULL,
    max_players  INT NOT NULL DEFAULT 1000,
    description  TEXT,
    version      VARCHAR(32),
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS maintenance_schedule (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    server_id    INT NOT NULL DEFAULT 0,        -- 0 = all servers
    title        VARCHAR(128) NOT NULL,
    message      TEXT NOT NULL,
    start_time   DATETIME NOT NULL,
    end_time     DATETIME NOT NULL,
    patch_notes  TEXT,
    status       TINYINT NOT NULL DEFAULT 0,    -- 0=scheduled,1=active,2=done
    created_by   VARCHAR(64),
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (server_id) REFERENCES game_servers(id) ON DELETE CASCADE
);

-- Seed default servers
INSERT IGNORE INTO game_servers (id, name, server_type, host, port, admin_port, status, description, version)
VALUES
(1, 'Server Chính', 0, '127.0.0.1', 7777, 8080, 1, 'Server chính thức', '1.0.0'),
(2, 'Server Test',  1, '127.0.0.1', 7778, 8081, 0, 'Server thử nghiệm tính năng mới', '1.0.0-dev');

-- ─────────────────────────────────────────
-- 2. CURRENCY: DIAMOND
-- ─────────────────────────────────────────
ALTER TABLE accounts ADD COLUMN IF NOT EXISTS diamond    INT NOT NULL DEFAULT 0;
ALTER TABLE accounts ADD COLUMN IF NOT EXISTS web_token  VARCHAR(256) DEFAULT NULL; -- dùng cho webshop
ALTER TABLE characters ADD COLUMN IF NOT EXISTS gender   TINYINT NOT NULL DEFAULT 0; -- 0=male,1=female

CREATE TABLE IF NOT EXISTS diamond_transactions (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id   BIGINT NOT NULL,
    amount       INT NOT NULL,      -- + nạp / - tiêu
    type         VARCHAR(32) NOT NULL, -- 'topup','spend','giftcode','refund','bonus'
    ref_id       VARCHAR(128),      -- sepay transaction_id / giftcode / item_id
    description  VARCHAR(256),
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE
);

-- ─────────────────────────────────────────
-- 3. PAYMENT - SEPAY
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS sepay_config (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    api_key          VARCHAR(256) NOT NULL DEFAULT '',
    webhook_secret   VARCHAR(256) NOT NULL DEFAULT '',
    bank_account     VARCHAR(64) NOT NULL DEFAULT '',
    bank_name        VARCHAR(64) NOT NULL DEFAULT '',
    account_name     VARCHAR(128) NOT NULL DEFAULT '',
    callback_url     VARCHAR(256) NOT NULL DEFAULT '',
    is_active        TINYINT NOT NULL DEFAULT 0,
    updated_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

INSERT IGNORE INTO sepay_config (id) VALUES (1);



CREATE TABLE IF NOT EXISTS topup_orders (
    id              VARCHAR(64) PRIMARY KEY,          -- format: NI_{timestamp}_{random}
    account_id      BIGINT NOT NULL,
    package_id      INT NOT NULL,
    amount_vnd      INT NOT NULL,
    diamond         INT NOT NULL,
    bonus_diamond   INT NOT NULL DEFAULT 0,
    status          VARCHAR(16) NOT NULL DEFAULT 'pending', -- pending,paid,failed,refunded
    sepay_txn_id    VARCHAR(128),
    sepay_content   VARCHAR(256),
    is_first_topup  TINYINT NOT NULL DEFAULT 0,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    paid_at         TIMESTAMP NULL,
    FOREIGN KEY (account_id) REFERENCES accounts(id),
    FOREIGN KEY (package_id) REFERENCES topup_packages(id)
);

-- Nạp đầu nhận thưởng config
CREATE TABLE IF NOT EXISTS first_topup_rewards (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    item_id     INT NOT NULL,
    qty         INT NOT NULL DEFAULT 1,
    description VARCHAR(128)
);

INSERT IGNORE INTO first_topup_rewards (item_id, qty, description) VALUES
(1, 1, 'Gói Trang Bị Khởi Đầu'),
(2, 3, 'Bình Hồi Phục x3');

-- ─────────────────────────────────────────
-- 4. WEBSHOP ITEMS (không có trong shop ingame)
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS webshop_items (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(128) NOT NULL,
    description  TEXT,
    item_type    VARCHAR(32) NOT NULL, -- 'skin','cosmetic','pack','mount','pet'
    class_id     INT NOT NULL DEFAULT 0, -- 0 = all classes
    diamond_price INT NOT NULL DEFAULT 0,
    original_price INT NOT NULL DEFAULT 0, -- để hiển thị giá gốc nếu sale
    is_limited   TINYINT NOT NULL DEFAULT 0,
    stock        INT NOT NULL DEFAULT -1, -- -1 = unlimited
    is_active    TINYINT NOT NULL DEFAULT 1,
    is_featured  TINYINT NOT NULL DEFAULT 0,
    icon_url     VARCHAR(256),
    preview_url  VARCHAR(256),
    sort_order   INT NOT NULL DEFAULT 0,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS webshop_item_contents (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    webshop_item_id INT NOT NULL,
    item_id      INT NOT NULL,
    qty          INT NOT NULL DEFAULT 1,
    FOREIGN KEY (webshop_item_id) REFERENCES webshop_items(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS webshop_orders (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id   BIGINT NOT NULL,
    char_id      BIGINT NOT NULL,
    webshop_item_id INT NOT NULL,
    diamond_spent INT NOT NULL,
    status       VARCHAR(16) NOT NULL DEFAULT 'pending', -- pending,delivered,failed
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    delivered_at TIMESTAMP NULL,
    FOREIGN KEY (account_id) REFERENCES accounts(id),
    FOREIGN KEY (webshop_item_id) REFERENCES webshop_items(id)
);

-- ─────────────────────────────────────────
-- 5. MISSION PASS (Battle Pass)
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS mission_pass_seasons (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(128) NOT NULL,
    description  TEXT,
    start_date   DATE NOT NULL,
    end_date     DATE NOT NULL,
    free_diamond INT NOT NULL DEFAULT 0,   -- giá mua pass miễn phí (0 = free tier)
    premium_diamond INT NOT NULL DEFAULT 0,
    max_level    INT NOT NULL DEFAULT 100,
    is_active    TINYINT NOT NULL DEFAULT 0,
    banner_url   VARCHAR(256),
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS mission_pass_rewards (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    season_id    INT NOT NULL,
    level        INT NOT NULL,
    tier         TINYINT NOT NULL DEFAULT 0, -- 0=free,1=premium
    item_id      INT NOT NULL,
    item_qty     INT NOT NULL DEFAULT 1,
    diamond      INT NOT NULL DEFAULT 0,
    gold         INT NOT NULL DEFAULT 0,
    description  VARCHAR(128),
    FOREIGN KEY (season_id) REFERENCES mission_pass_seasons(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS mission_pass_tasks (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    season_id    INT NOT NULL,
    task_type    VARCHAR(32) NOT NULL, -- 'daily','weekly','seasonal'
    title        VARCHAR(128) NOT NULL,
    description  TEXT,
    task_condition VARCHAR(64) NOT NULL, -- 'kill_monster:50', 'login:1', 'complete_quest:3'
    target       INT NOT NULL DEFAULT 1,
    exp_reward   INT NOT NULL DEFAULT 10, -- pass EXP (không phải char EXP)
    FOREIGN KEY (season_id) REFERENCES mission_pass_seasons(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS player_mission_pass (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    char_id      BIGINT NOT NULL,
    season_id    INT NOT NULL,
    pass_level   INT NOT NULL DEFAULT 1,
    pass_exp     INT NOT NULL DEFAULT 0,
    has_premium  TINYINT NOT NULL DEFAULT 0,
    claimed_rewards TEXT, -- JSON array: [{"level":1,"tier":0}]
    UNIQUE KEY uk_char_season (char_id, season_id),
    FOREIGN KEY (season_id) REFERENCES mission_pass_seasons(id)
);

CREATE TABLE IF NOT EXISTS player_pass_task_progress (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    char_id      BIGINT NOT NULL,
    task_id      INT NOT NULL,
    season_id    INT NOT NULL,
    progress     INT NOT NULL DEFAULT 0,
    is_completed TINYINT NOT NULL DEFAULT 0,
    reset_date   DATE,
    UNIQUE KEY uk_char_task (char_id, task_id),
    FOREIGN KEY (task_id) REFERENCES mission_pass_tasks(id) ON DELETE CASCADE
);

-- ─────────────────────────────────────────
-- 6. GIFT CODES
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS giftcodes (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    code         VARCHAR(32) NOT NULL UNIQUE,
    name         VARCHAR(128),
    description  TEXT,
    max_uses     INT NOT NULL DEFAULT 1,      -- -1 = unlimited
    used_count   INT NOT NULL DEFAULT 0,
    min_level    INT NOT NULL DEFAULT 1,
    expires_at   DATETIME NULL,
    is_active    TINYINT NOT NULL DEFAULT 1,
    server_id    INT NOT NULL DEFAULT 0,      -- 0 = all servers
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by   VARCHAR(64)
);

CREATE TABLE IF NOT EXISTS giftcode_rewards (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    giftcode_id  INT NOT NULL,
    reward_type  VARCHAR(16) NOT NULL, -- 'item','diamond','gold','title','pet','mount'
    reward_id    INT NOT NULL DEFAULT 0,
    qty          INT NOT NULL DEFAULT 1,
    FOREIGN KEY (giftcode_id) REFERENCES giftcodes(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS giftcode_usage (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    giftcode_id  INT NOT NULL,
    char_id      BIGINT NOT NULL,
    account_id   BIGINT NOT NULL,
    used_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_code_char (giftcode_id, char_id),
    FOREIGN KEY (giftcode_id) REFERENCES giftcodes(id)
);

-- ─────────────────────────────────────────
-- 7. TITLES (Danh hiệu)
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS titles (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(64) NOT NULL,
    description  TEXT,
    title_type   VARCHAR(32) NOT NULL DEFAULT 'normal', -- 'quest','achievement','giftcode','event','purchase'
    stat_bonus   VARCHAR(256), -- JSON: {"str":5,"agi":3}
    color_hex    VARCHAR(8) NOT NULL DEFAULT 'FFFFFF',
    icon_id      INT NOT NULL DEFAULT 0,
    is_active    TINYINT NOT NULL DEFAULT 1,
    sort_order   INT NOT NULL DEFAULT 0,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS player_titles (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    char_id      BIGINT NOT NULL,
    title_id     INT NOT NULL,
    is_equipped  TINYINT NOT NULL DEFAULT 0,
    obtained_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    source       VARCHAR(64), -- 'quest_1','giftcode_ABCD','admin'
    UNIQUE KEY uk_char_title (char_id, title_id),
    FOREIGN KEY (title_id) REFERENCES titles(id)
);

INSERT IGNORE INTO titles (id,name,description,title_type,color_hex,sort_order) VALUES
(1,'Tân Binh',  'Nhân vật mới tạo',         'achievement','AAAAAA',1),
(2,'Chiến Binh','Hoàn thành 100 trận đấu',  'achievement','FF6600',2),
(3,'Thám Hiểm', 'Khám phá 10 bản đồ',       'achievement','00CC88',3),
(4,'Nạp Thủ',   'Nạp game lần đầu',         'purchase',   'FFD700',4),
(5,'Nhà Vô Địch','Đứng đầu bảng xếp hạng',  'achievement','FF0000',5);

-- ─────────────────────────────────────────
-- 8. SKILL SYSTEM EXTENDED (30-40 skills/class, 7 active)
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS skill_templates (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(64) NOT NULL,
    class_id     INT NOT NULL,              -- 0 = all classes
    skill_type   VARCHAR(16) NOT NULL DEFAULT 'active', -- active/passive/ultimate
    element      VARCHAR(16) NOT NULL DEFAULT 'none', -- fire,ice,lightning,none
    base_damage  INT NOT NULL DEFAULT 0,
    mp_cost      INT NOT NULL DEFAULT 0,
    cooldown_ms  INT NOT NULL DEFAULT 1000,
    range        FLOAT NOT NULL DEFAULT 1.0,
    max_level    INT NOT NULL DEFAULT 10,
    description  TEXT,
    icon_id      INT NOT NULL DEFAULT 0,
    unlock_level INT NOT NULL DEFAULT 1,
    is_active    TINYINT NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS character_skill_slots (
    char_id      BIGINT NOT NULL,
    slot_index   TINYINT NOT NULL,  -- 0-6 (7 skill slots)
    skill_id     INT NOT NULL,
    PRIMARY KEY (char_id, slot_index)
);

-- Mở rộng character_skills: thêm level và exp cho từng skill
ALTER TABLE character_skills ADD COLUMN IF NOT EXISTS skill_level INT NOT NULL DEFAULT 1;
ALTER TABLE character_skills ADD COLUMN IF NOT EXISTS skill_exp   INT NOT NULL DEFAULT 0;

-- ─────────────────────────────────────────
-- 9. ENHANCEMENT (Cường hoá)
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS enhancement_config (
    level        INT PRIMARY KEY,
    success_rate FLOAT NOT NULL,       -- % thành công
    cost_gold    INT NOT NULL DEFAULT 0,
    cost_diamond INT NOT NULL DEFAULT 0,
    cost_material_id INT NOT NULL DEFAULT 0,
    cost_material_qty INT NOT NULL DEFAULT 1,
    stat_bonus_pct FLOAT NOT NULL DEFAULT 0.05 -- % tăng stat mỗi level
);

INSERT IGNORE INTO enhancement_config VALUES
(1,100.0,500,  0,0,0,0.05),(2,90.0, 1000, 0,0,0,0.05),
(3,80.0, 2000, 0,0,0,0.05),(4,70.0, 5000, 0,0,0,0.07),
(5,60.0, 8000, 0,0,0,0.07),(6,50.0,12000, 0,5,1,1,0.10),
(7,40.0,20000, 0,5,1,2,0.10),(8,30.0,30000,10,5,2,3,0.12),
(9,20.0,50000,20,5,3,4,0.15),(10,10.0,100000,50,5,4,5,0.20);

ALTER TABLE character_inventory ADD COLUMN IF NOT EXISTS enhance_level INT NOT NULL DEFAULT 0;

-- ─────────────────────────────────────────
-- 10. PETS
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS pet_templates (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(64) NOT NULL,
    element      VARCHAR(16) NOT NULL DEFAULT 'none',
    rarity       TINYINT NOT NULL DEFAULT 1, -- 1=common,...5=legendary
    base_hp      INT NOT NULL DEFAULT 100,
    base_atk     INT NOT NULL DEFAULT 10,
    base_def     INT NOT NULL DEFAULT 5,
    skill_id     INT NOT NULL DEFAULT 0,
    icon_id      INT NOT NULL DEFAULT 0,
    obtain_source VARCHAR(64),
    is_active    TINYINT NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS player_pets (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    char_id      BIGINT NOT NULL,
    template_id  INT NOT NULL,
    nickname     VARCHAR(32),
    level        INT NOT NULL DEFAULT 1,
    exp          INT NOT NULL DEFAULT 0,
    hunger       INT NOT NULL DEFAULT 100, -- 0-100
    loyalty      INT NOT NULL DEFAULT 50,
    is_active    TINYINT NOT NULL DEFAULT 0, -- đang mang theo
    equipment    VARCHAR(256),              -- JSON pet gear
    obtained_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (template_id) REFERENCES pet_templates(id)
);

INSERT IGNORE INTO pet_templates (id,name,element,rarity,base_hp,base_atk,base_def,icon_id,obtain_source) VALUES
(1,'Cáo Con',    'fire',1,120,15,8, 2001,'quest'),
(2,'Thỏ Băng',   'ice', 1,100,12,10,2002,'shop'),
(3,'Rồng Mini',  'fire',5,500,80,40,2010,'webshop'),
(4,'Phượng Hoàng','lightning',4,350,60,30,2011,'event'),
(5,'Mèo Đêm',   'none',2,150,20,15,2003,'giftcode');

-- ─────────────────────────────────────────
-- 11. MOUNTS (Thú cưỡi)
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS mount_templates (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(64) NOT NULL,
    speed_bonus  FLOAT NOT NULL DEFAULT 0.2,  -- +20% tốc độ
    rarity       TINYINT NOT NULL DEFAULT 1,
    icon_id      INT NOT NULL DEFAULT 0,
    obtain_source VARCHAR(64),
    is_active    TINYINT NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS player_mounts (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    char_id      BIGINT NOT NULL,
    template_id  INT NOT NULL,
    is_active    TINYINT NOT NULL DEFAULT 0,
    level        INT NOT NULL DEFAULT 1,
    exp          INT NOT NULL DEFAULT 0,
    obtained_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (template_id) REFERENCES mount_templates(id)
);

INSERT IGNORE INTO mount_templates (id,name,speed_bonus,rarity,icon_id,obtain_source) VALUES
(1,'Ngựa Trắng',   0.20,1,3001,'quest'),
(2,'Hổ Vàng',      0.35,3,3002,'webshop'),
(3,'Rồng Thiêng',  0.50,5,3010,'event'),
(4,'Sói Đêm',      0.30,2,3003,'giftcode');

-- ─────────────────────────────────────────
-- 12. RELATIONSHIP & MARRIAGE
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS relationships (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    char_id_a    BIGINT NOT NULL,
    char_id_b    BIGINT NOT NULL,
    rel_type     VARCHAR(16) NOT NULL DEFAULT 'friend', -- friend,dating,married
    affection    INT NOT NULL DEFAULT 0,
    started_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_pair (char_id_a, char_id_b)
);

CREATE TABLE IF NOT EXISTS marriages (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    char_id_a    BIGINT NOT NULL,
    char_id_b    BIGINT NOT NULL,
    wedding_date TIMESTAMP,
    wedding_map  INT NOT NULL DEFAULT 0,
    ring_item_id INT NOT NULL DEFAULT 0,
    status       VARCHAR(16) NOT NULL DEFAULT 'engaged', -- engaged,married,divorced
    anniversary  DATE,
    UNIQUE KEY uk_marriage (char_id_a, char_id_b)
);

-- ─────────────────────────────────────────
-- 13. CHILDREN (Con cái)
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS children (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    marriage_id  BIGINT NOT NULL,
    name         VARCHAR(32) NOT NULL,
    gender       TINYINT NOT NULL DEFAULT 0,
    age          INT NOT NULL DEFAULT 0,
    level        INT NOT NULL DEFAULT 1,
    exp          INT NOT NULL DEFAULT 0,
    hp           INT NOT NULL DEFAULT 50,
    max_hp       INT NOT NULL DEFAULT 50,
    atk          INT NOT NULL DEFAULT 5,
    def          INT NOT NULL DEFAULT 3,
    skin_id      INT NOT NULL DEFAULT 0,
    is_active    TINYINT NOT NULL DEFAULT 0,     -- đang mang theo trong chiến đấu
    happiness    INT NOT NULL DEFAULT 100,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (marriage_id) REFERENCES marriages(id) ON DELETE CASCADE
);

-- ─────────────────────────────────────────
-- 14. HOUSING (Nhà)
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS houses (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    char_id      BIGINT NOT NULL UNIQUE,         -- chủ nhà
    spouse_id    BIGINT,                          -- NULL nếu chưa kết hôn
    house_level  INT NOT NULL DEFAULT 1,
    house_style  INT NOT NULL DEFAULT 0,
    map_id       INT NOT NULL DEFAULT 100,        -- map nhà riêng
    happiness    INT NOT NULL DEFAULT 100,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS house_furniture (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    house_id     BIGINT NOT NULL,
    furniture_id INT NOT NULL,
    pos_x        FLOAT NOT NULL DEFAULT 0,
    pos_y        FLOAT NOT NULL DEFAULT 0,
    rotation     INT NOT NULL DEFAULT 0,
    FOREIGN KEY (house_id) REFERENCES houses(id) ON DELETE CASCADE
);

-- ─────────────────────────────────────────
-- 15. FARMING (Trồng cây)
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS farm_plots (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    char_id      BIGINT NOT NULL,
    plot_index   INT NOT NULL,
    seed_id      INT NOT NULL DEFAULT 0,         -- 0 = trống
    planted_at   TIMESTAMP NULL,
    stage        TINYINT NOT NULL DEFAULT 0,     -- 0=empty,1=seed,2=sprout,3=mature
    water_count  INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_char_plot (char_id, plot_index)
);

CREATE TABLE IF NOT EXISTS animal_pens (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    char_id      BIGINT NOT NULL,
    pen_index    INT NOT NULL,
    animal_id    INT NOT NULL DEFAULT 0,
    animal_type  VARCHAR(16) NOT NULL DEFAULT 'fish', -- fish,bird,rabbit,cow
    name         VARCHAR(32),
    hunger       INT NOT NULL DEFAULT 100,
    happiness    INT NOT NULL DEFAULT 100,
    UNIQUE KEY uk_char_pen (char_id, pen_index)
);

-- ─────────────────────────────────────────
-- 16. MINIGAMES
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS minigame_rooms (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    game_type    VARCHAR(32) NOT NULL, -- 'bau_cua','tien_len','dua_thu','o_an_quan','da_ga','do_vui'
    room_name    VARCHAR(64),
    host_char_id BIGINT NOT NULL,
    max_players  INT NOT NULL DEFAULT 4,
    min_bet      INT NOT NULL DEFAULT 0,
    max_bet      INT NOT NULL DEFAULT 100000,
    currency     TINYINT NOT NULL DEFAULT 0, -- 0=gold,1=diamond
    status       VARCHAR(16) NOT NULL DEFAULT 'waiting', -- waiting,playing,finished
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS minigame_history (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id      BIGINT NOT NULL,
    char_id      BIGINT NOT NULL,
    game_type    VARCHAR(32) NOT NULL,
    bet_amount   INT NOT NULL DEFAULT 0,
    win_amount   INT NOT NULL DEFAULT 0,
    result       VARCHAR(16) NOT NULL, -- win,lose,draw
    played_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ─────────────────────────────────────────
-- 17. MENTOR SYSTEM (Sư đồ)
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS mentor_relationships (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    mentor_id    BIGINT NOT NULL,               -- char_id của thầy (hoặc NPC id < 0)
    student_id   BIGINT NOT NULL,
    is_npc_mentor TINYINT NOT NULL DEFAULT 0,
    status       VARCHAR(16) NOT NULL DEFAULT 'active', -- active,graduated,broken
    started_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    graduated_at TIMESTAMP NULL,
    UNIQUE KEY uk_mentor_student (mentor_id, student_id)
);

CREATE TABLE IF NOT EXISTS mentor_missions (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    title        VARCHAR(128) NOT NULL,
    description  TEXT,
    mission_type VARCHAR(32) NOT NULL,           -- 'reach_level','kill_boss','complete_quest'
    target_value INT NOT NULL DEFAULT 1,
    reward_exp   INT NOT NULL DEFAULT 0,
    reward_gold  INT NOT NULL DEFAULT 0,
    reward_item_id INT NOT NULL DEFAULT 0,
    sort_order   INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS mentor_mission_progress (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    relationship_id BIGINT NOT NULL,
    mission_id   INT NOT NULL,
    progress     INT NOT NULL DEFAULT 0,
    is_completed TINYINT NOT NULL DEFAULT 0,
    completed_at TIMESTAMP NULL,
    UNIQUE KEY uk_rel_mission (relationship_id, mission_id),
    FOREIGN KEY (relationship_id) REFERENCES mentor_relationships(id) ON DELETE CASCADE,
    FOREIGN KEY (mission_id) REFERENCES mentor_missions(id)
);

INSERT IGNORE INTO mentor_missions (id,title,description,mission_type,target_value,reward_exp,reward_gold,sort_order) VALUES
(1,'Đặt Nền Móng',  'Đệ tử đạt level 10',  'reach_level',  10, 500,  1000, 1),
(2,'Bước Đường Dài','Đệ tử đạt level 30',  'reach_level',  30, 2000, 5000, 2),
(3,'Thực Chiến',    'Đệ tử hạ 50 monster', 'kill_monster', 50, 1000, 2000, 3),
(4,'Xuất Sư',       'Đệ tử đạt level 50',  'reach_level',  50, 5000,10000, 4);

-- ─────────────────────────────────────────
-- 18. LEADERBOARD / RANKINGS
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS leaderboard_snapshots (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    char_id      BIGINT NOT NULL,
    char_name    VARCHAR(32) NOT NULL,
    class_id     INT NOT NULL,
    level        INT NOT NULL,
    rank_type    VARCHAR(32) NOT NULL, -- 'level','pvp','wealth','guild'
    rank_value   BIGINT NOT NULL DEFAULT 0,
    rank_pos     INT NOT NULL DEFAULT 0,
    snapshot_date DATE NOT NULL,
    UNIQUE KEY uk_char_rank_date (char_id, rank_type, snapshot_date)
);

-- ─────────────────────────────────────────
-- 19. CHARACTER CLASSES EXTENDED (admin có thể thêm/sửa/xoá)
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS class_templates (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(64) NOT NULL,
    name_en      VARCHAR(64),
    description  TEXT,
    base_hp      INT NOT NULL DEFAULT 200,
    base_mp      INT NOT NULL DEFAULT 100,
    base_str     INT NOT NULL DEFAULT 10,
    base_agi     INT NOT NULL DEFAULT 10,
    base_intel   INT NOT NULL DEFAULT 10,
    base_vit     INT NOT NULL DEFAULT 10,
    hp_per_level INT NOT NULL DEFAULT 20,
    mp_per_level INT NOT NULL DEFAULT 10,
    starter_weapon_id INT NOT NULL DEFAULT 1,
    first_quest_id INT NOT NULL DEFAULT 1,
    icon_id      INT NOT NULL DEFAULT 0,
    male_sprite  VARCHAR(256),
    female_sprite VARCHAR(256),
    is_active    TINYINT NOT NULL DEFAULT 1,
    sort_order   INT NOT NULL DEFAULT 0,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT IGNORE INTO class_templates (id,name,name_en,base_hp,base_mp,base_str,base_agi,base_intel,base_vit,starter_weapon_id,first_quest_id,sort_order) VALUES
(1,'Kiếm Sĩ',  'Warrior',  250,100,15,10, 5,12,1,1,1),
(2,'Sát Thủ',  'Rogue',    180,120,12,18, 8, 8,2,2,2),
(3,'Pháp Sư',  'Mage',     150,200, 5,10,18, 8,3,3,3),
(4,'Pháp Thủ', 'Isekai',   200,180,10,12,15,10,4,4,4),
(5,'Cung Thủ', 'Archer',   200,130, 8,16,10,10,5,5,5);

-- ─────────────────────────────────────────
-- 20. WEBSHOP: giới hạn mua & kho hàng
-- ─────────────────────────────────────────

-- Thêm cột giới hạn vào webshop_items (nếu chưa có)
ALTER TABLE webshop_items
    ADD COLUMN IF NOT EXISTS per_user_limit   INT NOT NULL DEFAULT -1,  -- -1 = không giới hạn
    ADD COLUMN IF NOT EXISTS per_user_period  VARCHAR(16) NOT NULL DEFAULT 'all', -- all, daily, weekly, monthly
    ADD COLUMN IF NOT EXISTS total_sold       INT NOT NULL DEFAULT 0;

-- Lịch sử mua theo user (để kiểm tra giới hạn)
CREATE TABLE IF NOT EXISTS webshop_purchase_log (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id      BIGINT NOT NULL,
    char_id         BIGINT NOT NULL,
    webshop_item_id INT NOT NULL,
    qty             INT NOT NULL DEFAULT 1,
    diamond_spent   INT NOT NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_account_item (account_id, webshop_item_id),
    FOREIGN KEY (webshop_item_id) REFERENCES webshop_items(id)
);

-- ─────────────────────────────────────────
-- 21. ADMIN ITEM WAREHOUSE (kho item admin)
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS admin_item_warehouse (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    item_id      INT NOT NULL,
    item_name    VARCHAR(128) NOT NULL DEFAULT '',
    item_type    VARCHAR(32) NOT NULL DEFAULT 'item', -- item,diamond,gold,pet,mount,title
    qty          INT NOT NULL DEFAULT 0,
    qty_total    INT NOT NULL DEFAULT 0,   -- tổng đã thêm vào
    qty_used     INT NOT NULL DEFAULT 0,   -- tổng đã dùng
    description  TEXT,
    icon_url     VARCHAR(256),
    is_active    TINYINT NOT NULL DEFAULT 1,
    created_by   VARCHAR(64),
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS admin_warehouse_log (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    warehouse_id BIGINT NOT NULL,
    action       VARCHAR(16) NOT NULL, -- 'add','use','adjust'
    qty_change   INT NOT NULL,
    reason       VARCHAR(256),
    ref_type     VARCHAR(32),   -- 'giftcode','event','manual','webshop'
    ref_id       VARCHAR(128),
    admin_user   VARCHAR(64),
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (warehouse_id) REFERENCES admin_item_warehouse(id)
);

-- ─────────────────────────────────────────
-- 22. MISSION PASS: nhiệm vụ cấu hình đầy đủ
-- ─────────────────────────────────────────

-- Thêm pass_type vào webshop_items để phân biệt đây là premium pass
-- pass_season_id liên kết đến season tương ứng
ALTER TABLE webshop_items
    ADD COLUMN IF NOT EXISTS pass_season_id INT NOT NULL DEFAULT 0;  -- >0 = đây là item mua premium pass

-- Cập nhật premium_diamond = 0 trong season (giá lấy từ webshop_items)
-- Bảng tasks đã có, thêm reset_type cho rõ
ALTER TABLE mission_pass_tasks
    ADD COLUMN IF NOT EXISTS reset_type VARCHAR(16) NOT NULL DEFAULT 'none'; -- none, daily, weekly

-- Mốc nhận thưởng bonus (ngoài reward theo level)
CREATE TABLE IF NOT EXISTS mission_pass_milestones (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    season_id    INT NOT NULL,
    milestone_exp INT NOT NULL,  -- đạt X pass exp thì nhận
    tier         TINYINT NOT NULL DEFAULT 0, -- 0=free, 1=premium
    item_id      INT NOT NULL DEFAULT 0,
    item_qty     INT NOT NULL DEFAULT 1,
    diamond      INT NOT NULL DEFAULT 0,
    gold         INT NOT NULL DEFAULT 0,
    description  VARCHAR(128),
    FOREIGN KEY (season_id) REFERENCES mission_pass_seasons(id) ON DELETE CASCADE
);


-- ─────────────────────────────────────────
-- 23. CHAT EXTENDED — sticker, lì xì, voice, cross-server
-- ─────────────────────────────────────────

-- Sticker packs
CREATE TABLE IF NOT EXISTS sticker_packs (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(64) NOT NULL,
    description VARCHAR(256),
    price_diamond INT NOT NULL DEFAULT 0,  -- 0 = free/default
    is_default  TINYINT NOT NULL DEFAULT 0,
    is_active   TINYINT NOT NULL DEFAULT 1,
    icon_id     INT NOT NULL DEFAULT 0,
    sort_order  INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS stickers (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    pack_id     INT NOT NULL,
    name        VARCHAR(64) NOT NULL,
    asset_key   VARCHAR(128) NOT NULL,  -- Resources path: "Stickers/pack1/sticker_01"
    sort_order  INT NOT NULL DEFAULT 0,
    FOREIGN KEY (pack_id) REFERENCES sticker_packs(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS player_sticker_packs (
    char_id     BIGINT NOT NULL,
    pack_id     INT NOT NULL,
    obtained_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (char_id, pack_id)
);

-- Red envelopes (lì xì)
CREATE TABLE IF NOT EXISTS red_envelopes (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    sender_char_id  BIGINT NOT NULL,
    sender_name     VARCHAR(32) NOT NULL,
    channel         TINYINT NOT NULL,           -- 0=world,1=map,2=guild
    channel_ref     BIGINT NOT NULL DEFAULT 0,  -- map_id or guild_id
    currency        TINYINT NOT NULL DEFAULT 0, -- 0=gold,1=diamond
    total_amount    INT NOT NULL,
    amount_per_grab INT NOT NULL,
    max_grabbers    INT NOT NULL,
    grabbed_count   INT NOT NULL DEFAULT 0,
    message         VARCHAR(128) NOT NULL DEFAULT '',
    expires_at      TIMESTAMP NOT NULL,
    status          VARCHAR(16) NOT NULL DEFAULT 'active', -- active,exhausted,expired
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS red_envelope_grabs (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    envelope_id BIGINT NOT NULL,
    char_id     BIGINT NOT NULL,
    char_name   VARCHAR(32) NOT NULL,
    amount      INT NOT NULL,
    grabbed_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_env_char (envelope_id, char_id),
    FOREIGN KEY (envelope_id) REFERENCES red_envelopes(id)
);

-- Voice messages (lưu URL/path, không lưu raw audio)
CREATE TABLE IF NOT EXISTS voice_messages (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    char_id     BIGINT NOT NULL,
    char_name   VARCHAR(32) NOT NULL,
    channel     TINYINT NOT NULL,
    channel_ref BIGINT NOT NULL DEFAULT 0,
    file_path   VARCHAR(256) NOT NULL,   -- đường dẫn file trên server
    duration_ms INT NOT NULL DEFAULT 0,  -- thời lượng ms
    file_size   INT NOT NULL DEFAULT 0,  -- bytes
    expires_at  TIMESTAMP NOT NULL,      -- auto-delete sau 7 ngày
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Chat history (lưu lại để reload khi re-login)
CREATE TABLE IF NOT EXISTS chat_history (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    server_id    INT NOT NULL DEFAULT 1,
    channel      TINYINT NOT NULL,
    channel_ref  BIGINT NOT NULL DEFAULT 0,
    sender_id    BIGINT NOT NULL,
    sender_name  VARCHAR(32) NOT NULL,
    content_type TINYINT NOT NULL DEFAULT 0,
    content      TEXT NOT NULL,         -- JSON: {"text":"..."} / {"stickerId":1} / v.v.
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_channel_time (channel, channel_ref, created_at)
);

-- Cross-server relay queue (server khác poll để nhận)
CREATE TABLE IF NOT EXISTS cross_server_relay (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_server INT NOT NULL,
    sender_id   BIGINT NOT NULL,
    sender_name VARCHAR(32) NOT NULL,
    content_type TINYINT NOT NULL DEFAULT 0,
    content     TEXT NOT NULL,
    sent_to     TEXT,   -- JSON array server ids đã nhận
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_created (created_at)
);

-- Seed default sticker packs
INSERT IGNORE INTO sticker_packs (id,name,is_default,is_active,sort_order) VALUES
(1,'Mặc Định',1,1,0),
(2,'Cute Pack',0,1,1),
(3,'Chiến Đấu',0,1,2);

INSERT IGNORE INTO stickers (id,pack_id,name,asset_key,sort_order) VALUES
(1,1,'Vui','Stickers/default/happy',0),(2,1,'Buồn','Stickers/default/sad',1),
(3,1,'Tức','Stickers/default/angry',2),(4,1,'OK','Stickers/default/ok',3),
(5,1,'GG','Stickers/default/gg',4),(6,1,'?!','Stickers/default/what',5),
(7,2,'Cute1','Stickers/cute/cat1',0),(8,2,'Cute2','Stickers/cute/cat2',1),
(9,3,'Attack','Stickers/battle/atk',0),(10,3,'Win','Stickers/battle/win',1);

-- ─────────────────────────────────────────
-- 24. GUILD extended
-- ─────────────────────────────────────────
ALTER TABLE guilds
    ADD COLUMN IF NOT EXISTS icon_id       INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS notice        TEXT,
    ADD COLUMN IF NOT EXISTS max_members   INT NOT NULL DEFAULT 50,
    ADD COLUMN IF NOT EXISTS gold          BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS is_active     TINYINT NOT NULL DEFAULT 1;

ALTER TABLE guild_members
    ADD COLUMN IF NOT EXISTS contribution  INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS title         VARCHAR(32) DEFAULT NULL;

CREATE TABLE IF NOT EXISTS guild_invites (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    guild_id    BIGINT NOT NULL,
    inviter_id  BIGINT NOT NULL,
    target_id   BIGINT NOT NULL,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_guild_target (guild_id, target_id),
    FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE
);

-- ─────────────────────────────────────────
-- 25. SKILL SYSTEM EXTENDED
-- ─────────────────────────────────────────
-- Seed 8 skills cho mỗi class (40 total)
INSERT IGNORE INTO skill_templates
(id,name,class_id,skill_type,element,base_damage,mp_cost,cooldown_ms,max_level,description,icon_id,unlock_level) VALUES
-- Kiếm Sĩ (class 1)
(101,'Chém Mạnh',1,'active','none',120,10,1500,10,'Đòn chém gây 120% sát thương',1001,1),
(102,'Phòng Thủ',1,'active','none',0,15,8000,10,'Giảm 50% sát thương nhận trong 3s',1002,5),
(103,'Xoáy Kiếm',1,'active','none',150,25,4000,10,'Tấn công tất cả quái xung quanh',1003,10),
(104,'Kiếm Khí',1,'active','none',200,35,6000,10,'Phóng khí kiếm xuyên qua kẻ thù',1004,15),
(105,'Hồi Máu',1,'passive','none',0,0,0,5,'Hồi 2% HP mỗi 5 giây',1005,1),
(106,'Tăng STR',1,'passive','none',0,0,0,5,'Tăng STR +10 mỗi level skill',1006,1),
(107,'Chiến Thần',1,'ultimate','fire',500,80,30000,5,'Bùng cháy 10 giây, ATK +100%',1007,30),
(108,'Đột Kích',1,'active','none',180,20,3000,10,'Lao tới mục tiêu gây 180% sát thương',1008,8),
-- Sát Thủ (class 2)
(201,'Đâm Nhanh',2,'active','none',110,8,1000,10,'Đâm nhanh 110% sát thương',2001,1),
(202,'Tàng Hình',2,'active','none',0,30,15000,5,'Ẩn thân 5 giây, tấn công gây x2',2002,10),
(203,'Ném Dao',2,'active','none',130,15,2000,10,'Ném dao từ xa',2003,5),
(204,'Chất Độc',2,'active','none',80,20,4000,10,'Gây độc liên tục 5 giây',2004,8),
(205,'Tốc Độ',2,'passive','none',0,0,0,5,'Tăng tốc di chuyển 5% mỗi level',2005,1),
(206,'Né Tránh',2,'passive','none',0,0,0,5,'Tăng 3% tỉ lệ né đòn mỗi level',2006,1),
(207,'Sát Thủ Ngầm',2,'ultimate','none',600,100,30000,5,'Ngay lập tức ẩn và tấn công chí mạng',2007,30),
(208,'Xiềng Xích',2,'active','none',0,25,10000,5,'Làm chậm kẻ thù 50% trong 3 giây',2008,12),
-- Pháp Sư (class 3)
(301,'Quả Cầu Lửa',3,'active','fire',150,20,2000,10,'Bắn quả cầu lửa',3001,1),
(302,'Băng Kết',3,'active','ice',130,25,4000,10,'Đóng băng kẻ thù trong 2 giây',3002,5),
(303,'Sét Đánh',3,'active','lightning',170,30,3000,10,'Sét đánh gây 170% sát thương',3003,10),
(304,'Bão Lửa',3,'active','fire',120,40,5000,10,'Bão lửa liên tục trên khu vực',3004,15),
(305,'Trí Tuệ',3,'passive','none',0,0,0,5,'Tăng INT +8 mỗi level skill',3005,1),
(306,'Tiết Kiệm MP',3,'passive','none',0,0,0,5,'Giảm 5% MP cost mỗi level',3006,1),
(307,'Thảm Hoạ Lửa',3,'ultimate','fire',800,120,30000,5,'Thiên thạch rơi gây sát thương khủng',3007,30),
(308,'Hồi MP',3,'active','none',0,0,8000,5,'Hồi 30% MP tối đa',3008,8),
-- Pháp Thủ (class 4)
(401,'Tên Lửa Thần',4,'active','fire',140,18,2500,10,'Bắn tên lửa phép thuật',4001,1),
(402,'Khiên Phép',4,'active','none',0,30,12000,5,'Tạo khiên hấp thụ 500 sát thương',4002,5),
(403,'Phép Hồi',4,'active','none',-200,35,5000,10,'Hồi phục HP nhân vật (âm = heal)',4003,10),
(404,'Phép Buff',4,'active','none',0,40,20000,5,'Tăng ATK+DEF toàn nhóm 20% trong 30s',4004,15),
(405,'Thánh Linh',4,'passive','none',0,0,0,5,'Tăng kháng phép +5% mỗi level',4005,1),
(406,'Tăng VIT',4,'passive','none',0,0,0,5,'Tăng VIT +8 mỗi level skill',4006,1),
(407,'Ngọn Lửa Thánh',4,'ultimate','fire',700,110,30000,5,'Gọi ngọn lửa thánh huỷ diệt',4007,30),
(408,'Dịch Chuyển',4,'active','none',0,20,8000,5,'Dịch chuyển tức thì đến vị trí chỉ định',4008,12),
-- Cung Thủ (class 5)
(501,'Mưa Tên',5,'active','none',100,15,2000,10,'Bắn loạt tên vào một khu vực',5001,1),
(502,'Tên Xuyên Thấu',5,'active','none',160,20,3000,10,'Tên xuyên qua nhiều kẻ thù',5002,5),
(503,'Mắt Thần',5,'active','none',0,10,6000,5,'Phát hiện kẻ thù ẩn trong 10 giây',5003,8),
(504,'Tên Lửa',5,'active','fire',180,25,4000,10,'Bắn tên lửa gây thêm sát thương lửa',5004,10),
(505,'Tăng AGI',5,'passive','none',0,0,0,5,'Tăng AGI +10 mỗi level skill',5005,1),
(506,'Mắt Sắc',5,'passive','none',0,0,0,5,'Tăng crit rate +3% mỗi level',5006,1),
(507,'Bão Tên',5,'ultimate','none',600,100,30000,5,'Bắn hàng trăm mũi tên trong 5 giây',5007,30),
(508,'Bẫy Chông',5,'active','none',120,20,8000,5,'Đặt bẫy gây sát thương kẻ đi qua',5008,12);

-- ─────────────────────────────────────────
-- 26. PVP SYSTEM
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS pvp_duels (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    challenger_id BIGINT NOT NULL,
    defender_id   BIGINT NOT NULL,
    map_id        INT NOT NULL DEFAULT 0,
    status        VARCHAR(16) NOT NULL DEFAULT 'pending', -- pending,active,finished,declined
    winner_id     BIGINT DEFAULT NULL,
    started_at    TIMESTAMP NULL,
    ended_at      TIMESTAMP NULL,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS pvp_stats (
    char_id  BIGINT PRIMARY KEY,
    wins     INT NOT NULL DEFAULT 0,
    losses   INT NOT NULL DEFAULT 0,
    rating   INT NOT NULL DEFAULT 1000,  -- ELO
    FOREIGN KEY (char_id) REFERENCES characters(id)
);

-- ─────────────────────────────────────────
-- 27. MINIGAME extended config
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS minigame_config (
    game_type    VARCHAR(32) PRIMARY KEY,
    min_bet      INT NOT NULL DEFAULT 100,
    max_bet      INT NOT NULL DEFAULT 1000000,
    house_edge   FLOAT NOT NULL DEFAULT 0.05,  -- 5% house advantage
    is_active    TINYINT NOT NULL DEFAULT 1,
    config_json  TEXT   -- game-specific config
);

INSERT IGNORE INTO minigame_config (game_type,min_bet,max_bet,house_edge,config_json) VALUES
('bau_cua',  100,500000,0.04,'{"dice_count":3,"symbols":["bau","cua","ca","tom","ga","nai"]}'),
('tien_len', 100,200000,0.05,'{"num_cards":13,"jokers":0}'),
('dua_thu',  100,300000,0.06,'{"num_lanes":6,"race_distance":100}'),
('o_an_quan',50, 100000,0.0, '{"holes":10,"stones_per_hole":5}'),
('da_ga',   500,1000000,0.05,'{"rounds":5}'),
('do_vui',  0,  0,       0.0,'{"questions_per_game":10}');

-- ─────────────────────────────────────────
-- 28. FARMING config
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS farm_seeds (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(64) NOT NULL,
    growth_time_min INT NOT NULL DEFAULT 60, -- phút để trưởng thành
    stages       INT NOT NULL DEFAULT 4,     -- số giai đoạn
    harvest_item_id INT NOT NULL,
    harvest_qty_min INT NOT NULL DEFAULT 1,
    harvest_qty_max INT NOT NULL DEFAULT 5,
    seed_item_id INT NOT NULL,               -- item để trồng
    water_needed INT NOT NULL DEFAULT 3,     -- lần tưới cần thiết
    is_active    TINYINT NOT NULL DEFAULT 1
);


CREATE TABLE IF NOT EXISTS farm_animals (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(64) NOT NULL,
    animal_type  VARCHAR(16) NOT NULL,  -- fish,bird,pig,cow
    feed_item_id INT NOT NULL,
    produce_item_id INT NOT NULL,
    produce_time_min INT NOT NULL DEFAULT 120,
    produce_qty  INT NOT NULL DEFAULT 1,
    is_active    TINYINT NOT NULL DEFAULT 1
);


-- ─────────────────────────────────────────
-- 29. HOUSING catalog
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS furniture_catalog (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(64) NOT NULL,
    furniture_type VARCHAR(32) NOT NULL, -- bed,table,chair,decoration,storage
    width        INT NOT NULL DEFAULT 1,
    height       INT NOT NULL DEFAULT 1,
    gold_price   INT NOT NULL DEFAULT 0,
    diamond_price INT NOT NULL DEFAULT 0,
    stat_bonus   VARCHAR(256),  -- JSON bonus khi đặt trong nhà
    icon_id      INT NOT NULL DEFAULT 0,
    is_active    TINYINT NOT NULL DEFAULT 1
);

INSERT IGNORE INTO furniture_catalog (id,name,furniture_type,width,height,gold_price,diamond_price) VALUES
(1,'Giường Đơn','bed',2,1,5000,0),(2,'Giường Đôi','bed',3,2,15000,0),
(3,'Bàn Gỗ','table',1,1,2000,0),(4,'Ghế Gỗ','chair',1,1,1000,0),
(5,'Tủ Sách','storage',1,2,8000,0),(6,'Chậu Hoa','decoration',1,1,3000,0),
(7,'Tranh Phong Cảnh','decoration',2,1,10000,50),(8,'Đèn Lồng','decoration',1,1,5000,0),
(9,'Bếp Lửa','decoration',1,1,12000,0),(10,'Bể Cá','decoration',2,1,0,100);

-- ─────────────────────────────────────────
-- 30. LEADERBOARD
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS leaderboard_cache (
    rank_type    VARCHAR(32) NOT NULL,   -- level,pvp_rating,wealth,guild_level
    rank_pos     INT NOT NULL,
    char_id      BIGINT NOT NULL,
    char_name    VARCHAR(32) NOT NULL,
    class_id     INT NOT NULL DEFAULT 1,
    gender       TINYINT NOT NULL DEFAULT 0,
    rank_value   BIGINT NOT NULL,
    updated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (rank_type, rank_pos)
);
ALTER TABLE characters ADD COLUMN IF NOT EXISTS server_id INT NOT NULL DEFAULT 1;

-- ─────────────────────────────────────────
-- 24. EVENT CURRENCY — tien te su kien phu
-- ─────────────────────────────────────────

CREATE TABLE IF NOT EXISTS event_currencies (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    event_id        INT NOT NULL DEFAULT 0,      -- 0 = thuong truc, >0 = gan su kien cu the
    currency_code   VARCHAR(32) NOT NULL UNIQUE,  -- VD: "hoa_sen", "sao_bang", "la_phong"
    display_name    VARCHAR(64) NOT NULL,          -- VD: "Hoa Sen", "Sao Bang"
    icon_asset      VARCHAR(128) NOT NULL DEFAULT 'Icons/Currency/default',
    description     VARCHAR(256) DEFAULT '',
    exchange_rate_gold    INT NOT NULL DEFAULT 0,  -- 1 token = ? gold (0 = khong doi duoc)
    exchange_rate_diamond INT NOT NULL DEFAULT 0,  -- 1 token = ? diamond
    is_active       TINYINT NOT NULL DEFAULT 1,
    expires_at      DATETIME DEFAULT NULL,         -- NULL = khong het han
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_active (is_active, expires_at)
);

CREATE TABLE IF NOT EXISTS player_event_currencies (
    char_id         BIGINT NOT NULL,
    currency_id     INT NOT NULL,
    amount          INT NOT NULL DEFAULT 0,
    last_earned_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (char_id, currency_id),
    FOREIGN KEY (currency_id) REFERENCES event_currencies(id) ON DELETE CASCADE
);

-- Event currency shop (item mua bang event token)
CREATE TABLE IF NOT EXISTS event_currency_shop (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    currency_id     INT NOT NULL,
    item_id         INT NOT NULL,
    item_name       VARCHAR(64) NOT NULL,
    price           INT NOT NULL,
    stock           INT NOT NULL DEFAULT -1,   -- -1 = vo han
    per_user_limit  INT NOT NULL DEFAULT 0,    -- 0 = khong gioi han
    sort_order      INT NOT NULL DEFAULT 0,
    is_active       TINYINT NOT NULL DEFAULT 1,
    FOREIGN KEY (currency_id) REFERENCES event_currencies(id)
);

-- Log giao dich event currency
CREATE TABLE IF NOT EXISTS event_currency_log (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    char_id         BIGINT NOT NULL,
    currency_id     INT NOT NULL,
    amount          INT NOT NULL,       -- + = nhan, - = tieu
    reason          VARCHAR(128) NOT NULL, -- 'quest_reward','shop_buy','exchange','admin_grant','event_drop'
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_char_cur (char_id, currency_id)
);

-- Seed VD
INSERT IGNORE INTO event_currencies (id,currency_code,display_name,icon_asset,description,exchange_rate_gold,is_active)
VALUES
(1,'hoa_sen','Hoa Sen','Icons/Currency/hoa_sen','Tien te su kien Tet Nguyen Dan',100,0),
(2,'sao_bang','Sao Bang','Icons/Currency/sao_bang','Tien te su kien He',50,0),
(3,'la_phong','La Phong','Icons/Currency/la_phong','Tien te su kien Thu',80,0);

-- ═════════════════════════════════════════════════════════════
-- 25. MASTER ITEM REGISTRY — Kho tong, moi thu trong game
-- ═════════════════════════════════════════════════════════════

-- Phan loai chinh cua moi "thing" trong game
-- Dung cho admin: khi can chon item (event, pass, giftcode) chi can nhap ID
CREATE TABLE IF NOT EXISTS master_registry (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    registry_type   VARCHAR(32) NOT NULL,   -- 'item','skin','pet','mount','title','map','event_currency','sticker_pack','furniture','seed','animal'
    ref_id          INT NOT NULL,           -- ID trong bang goc (items.id, pet_templates.id,...)
    display_name    VARCHAR(128) NOT NULL,
    category        VARCHAR(64) NOT NULL DEFAULT 'general', -- 'weapon','armor','consumable','material','cosmetic','mount','pet','title','map','currency','sticker','furniture','seed','animal'
    sub_category    VARCHAR(64) DEFAULT '',  -- 'sword','staff','hp_potion','cape_skin',...
    rarity          TINYINT NOT NULL DEFAULT 0,  -- 0=common,1=uncommon,2=rare,3=epic,4=legendary,5=mythic
    icon_asset      VARCHAR(256) DEFAULT 'Icons/default',
    description     TEXT,
    tags            VARCHAR(256) DEFAULT '', -- 'event_tet,limited,tradeable,stackable'
    is_tradeable    TINYINT NOT NULL DEFAULT 1,
    is_stackable    TINYINT NOT NULL DEFAULT 0,
    max_stack       INT NOT NULL DEFAULT 1,
    is_active       TINYINT NOT NULL DEFAULT 1,
    added_by        VARCHAR(64) DEFAULT 'system',
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_type (registry_type),
    INDEX idx_category (category, sub_category),
    INDEX idx_rarity (rarity),
    INDEX idx_tags (tags),
    UNIQUE KEY uk_type_ref (registry_type, ref_id)
);

-- Populate tu cac bang hien co (chay 1 lan khi setup)
-- INSERT IGNORE INTO master_registry (registry_type,ref_id,display_name,category,rarity)
--   SELECT 'item', id, name, CASE type WHEN 0 THEN 'weapon' ... END, rarity FROM items;

-- ═════════════════════════════════════════════════════════════
-- 26. TRADING — Giao dich giua player
-- ═════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS trade_sessions (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    player_a_id     BIGINT NOT NULL,
    player_b_id     BIGINT NOT NULL,
    status          VARCHAR(16) NOT NULL DEFAULT 'pending', -- pending,confirmed_a,confirmed_b,completed,cancelled
    gold_a          BIGINT NOT NULL DEFAULT 0,  -- vang player A dua
    gold_b          BIGINT NOT NULL DEFAULT 0,  -- vang player B dua
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at    DATETIME DEFAULT NULL
);

CREATE TABLE IF NOT EXISTS trade_items (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    trade_id        BIGINT NOT NULL,
    from_char_id    BIGINT NOT NULL,  -- nguoi dua item
    inventory_id    BIGINT NOT NULL,  -- character_inventory.id
    item_id         INT NOT NULL,
    qty             INT NOT NULL DEFAULT 1,
    FOREIGN KEY (trade_id) REFERENCES trade_sessions(id) ON DELETE CASCADE
);

-- ═════════════════════════════════════════════════════════════
-- 27. AUCTION HOUSE — Nha dau gia
-- ═════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS auction_listings (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    seller_char_id  BIGINT NOT NULL,
    seller_name     VARCHAR(32) NOT NULL,
    inventory_id    BIGINT NOT NULL,       -- item dang ban
    item_id         INT NOT NULL,
    item_name       VARCHAR(64) NOT NULL,
    qty             INT NOT NULL DEFAULT 1,
    enhance_level   INT NOT NULL DEFAULT 0,
    rarity          INT NOT NULL DEFAULT 0,
    start_price     BIGINT NOT NULL,       -- gia khoi diem
    buyout_price    BIGINT DEFAULT NULL,   -- gia mua ngay (NULL = ko co)
    current_bid     BIGINT NOT NULL DEFAULT 0,
    bidder_char_id  BIGINT DEFAULT NULL,   -- nguoi dau gia cao nhat hien tai
    bidder_name     VARCHAR(32) DEFAULT NULL,
    currency        TINYINT NOT NULL DEFAULT 0, -- 0=gold,1=diamond
    status          VARCHAR(16) NOT NULL DEFAULT 'active', -- active,sold,expired,cancelled
    expires_at      DATETIME NOT NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_status_expires (status, expires_at),
    INDEX idx_item (item_id, rarity),
    INDEX idx_seller (seller_char_id)
);

CREATE TABLE IF NOT EXISTS auction_bids (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    listing_id      BIGINT NOT NULL,
    bidder_char_id  BIGINT NOT NULL,
    amount          BIGINT NOT NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (listing_id) REFERENCES auction_listings(id)
);

-- Admin config cho auction
CREATE TABLE IF NOT EXISTS auction_config (
    config_key      VARCHAR(64) PRIMARY KEY,
    config_value    VARCHAR(256) NOT NULL,
    description     VARCHAR(256) DEFAULT ''
);
INSERT IGNORE INTO auction_config VALUES
('tax_rate','5','Thue ban hang (%)'),
('max_duration_hours','48','Thoi gian dang toi da (gio)'),
('min_price','100','Gia toi thieu'),
('max_listings_per_player','20','So luong dang ban toi da/player'),
('is_enabled','1','Bat/tat auction house');

-- ═════════════════════════════════════════════════════════════
-- 28. PARTY — Nhom chien dau
-- ═════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS parties (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    leader_char_id  BIGINT NOT NULL,
    name            VARCHAR(32) DEFAULT '',
    max_members     INT NOT NULL DEFAULT 4,
    status          VARCHAR(16) NOT NULL DEFAULT 'active', -- active,disbanded,in_dungeon
    dungeon_id      INT DEFAULT NULL,         -- NULL = khong trong dungeon
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS party_members (
    party_id        BIGINT NOT NULL,
    char_id         BIGINT NOT NULL,
    role            TINYINT NOT NULL DEFAULT 0, -- 0=member,1=leader
    joined_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (party_id, char_id),
    FOREIGN KEY (party_id) REFERENCES parties(id) ON DELETE CASCADE
);

-- ═════════════════════════════════════════════════════════════
-- 29. DUNGEON — Instance dungeon
-- ═════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS dungeon_templates (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(64) NOT NULL,
    description     TEXT,
    min_level       INT NOT NULL DEFAULT 1,
    max_players     INT NOT NULL DEFAULT 4,
    map_id          INT NOT NULL,              -- map rieng cho dungeon
    boss_monster_id INT DEFAULT NULL,          -- boss cuoi
    reward_exp      INT NOT NULL DEFAULT 0,
    reward_gold     INT NOT NULL DEFAULT 0,
    reward_items    TEXT,                       -- JSON: [{"item_id":1,"qty":1,"drop_rate":0.5}]
    cooldown_minutes INT NOT NULL DEFAULT 60,  -- thoi gian cho giua cac lan vao
    time_limit_minutes INT NOT NULL DEFAULT 30,
    difficulty      TINYINT NOT NULL DEFAULT 1, -- 1=normal,2=hard,3=hell
    is_active       TINYINT NOT NULL DEFAULT 1,
    sort_order      INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS dungeon_instances (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_id     INT NOT NULL,
    party_id        BIGINT NOT NULL,
    status          VARCHAR(16) NOT NULL DEFAULT 'active', -- active,completed,failed,expired
    started_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ended_at        DATETIME DEFAULT NULL,
    boss_killed     TINYINT NOT NULL DEFAULT 0,
    FOREIGN KEY (template_id) REFERENCES dungeon_templates(id)
);

CREATE TABLE IF NOT EXISTS dungeon_cooldowns (
    char_id         BIGINT NOT NULL,
    template_id     INT NOT NULL,
    next_entry_at   DATETIME NOT NULL,
    PRIMARY KEY (char_id, template_id)
);

-- Seed dungeon templates
INSERT IGNORE INTO dungeon_templates (id,name,min_level,max_players,map_id,reward_exp,reward_gold,difficulty) VALUES
(1,'Hang Doi Am U',10,4,101,500,2000,1),
(2,'Lau Dai Bi Mat',25,4,102,2000,8000,1),
(3,'Than Dien Co Dai',40,4,103,5000,20000,2),
(4,'Dia Nguc Tang 1',60,4,104,15000,50000,3);

-- ═════════════════════════════════════════════════════════════
-- 30. NPC DIALOG — Hoi thoai NPC (cay hoi thoai)
-- ═════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS npc_dialogs (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    npc_id          INT NOT NULL,
    dialog_key      VARCHAR(64) NOT NULL,      -- 'greeting','quest_1_start','shop_intro'
    text            TEXT NOT NULL,              -- Noi dung hoi thoai
    speaker         VARCHAR(32) DEFAULT '',     -- Ten nguoi noi (NPC name, hoac 'Player')
    next_dialog_id  INT DEFAULT NULL,           -- Dialog tiep theo (NULL = ket thuc)
    options         TEXT DEFAULT NULL,           -- JSON: [{"text":"Chap nhan","goto":5},{"text":"Tu choi","goto":null}]
    condition       TEXT DEFAULT NULL,           -- JSON: {"min_level":10,"has_item":5,"quest_completed":2}
    action          TEXT DEFAULT NULL,           -- JSON: {"give_item":1,"start_quest":3,"open_shop":2}
    sort_order      INT NOT NULL DEFAULT 0,
    FOREIGN KEY (npc_id) REFERENCES npcs(id) ON DELETE CASCADE,
    INDEX idx_npc_key (npc_id, dialog_key)
);

-- ═════════════════════════════════════════════════════════════
-- 31. SYSTEM ANNOUNCEMENTS — Thong bao he thong (sticky)
-- ═════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS system_announcements (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    title           VARCHAR(128) NOT NULL,
    content         TEXT NOT NULL,
    announce_type   VARCHAR(32) NOT NULL DEFAULT 'info', -- info,warning,event,maintenance,achievement
    priority        INT NOT NULL DEFAULT 0,    -- 0=normal,1=important,2=critical (sticky)
    is_sticky       TINYINT NOT NULL DEFAULT 0, -- 1 = luon hien tren dau
    target          VARCHAR(32) DEFAULT 'all', -- 'all','server_1','guild_123','char_456'
    start_at        DATETIME DEFAULT NULL,     -- NULL = ngay lap tuc
    expires_at      DATETIME DEFAULT NULL,     -- NULL = khong het han
    created_by      VARCHAR(64) DEFAULT 'admin',
    is_active       TINYINT NOT NULL DEFAULT 1,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_active_priority (is_active, priority DESC, created_at DESC)
);

-- Log su kien quan trong (tu dong insert boi server)
CREATE TABLE IF NOT EXISTS system_event_log (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type      VARCHAR(32) NOT NULL,   -- 'leaderboard_top','boss_kill','wedding','level_cap','first_clear','guild_war'
    char_id         BIGINT DEFAULT NULL,
    char_name       VARCHAR(32) DEFAULT '',
    message         TEXT NOT NULL,
    data_json       TEXT DEFAULT NULL,       -- extra data
    server_id       INT NOT NULL DEFAULT 1,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_type_time (event_type, created_at DESC)
);

-- Seed announcements
INSERT IGNORE INTO system_announcements (id,title,content,announce_type,priority,is_sticky) VALUES
(1,'Chao mung den Nexus Isekai!','Chuc cac Luu Dan co nhung trai nghiem tuyet voi tai Vong Linh Gioi.','info',0,1),
(2,'Luu y bao mat','Khong chia se mat khau voi bat ky ai. Admin khong bao gio hoi mat khau.','warning',1,1);

-- ═════════════════════════════════════════════════════════════
-- 32. RATE LIMITING — Gioi han tan suat goi
-- ═════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS rate_limit_config (
    config_key      VARCHAR(64) PRIMARY KEY,
    max_per_second  INT NOT NULL DEFAULT 10,
    max_per_minute  INT NOT NULL DEFAULT 100,
    description     VARCHAR(256) DEFAULT ''
);
INSERT IGNORE INTO rate_limit_config VALUES
('chat',5,60,'Gioi han chat'),
('move',20,600,'Gioi han di chuyen'),
('attack',10,200,'Gioi han tan cong'),
('trade',3,30,'Gioi han giao dich'),
('shop',5,60,'Gioi han mua ban');

-- Guild invites table (da tham chieu trong GuildHandler)


-- PvP duels table


-- Farm seeds/animals config




-- Furniture catalog


-- ═════════════════════════════════════════════════════════════
-- 33. OTA ASSET UPDATE — Hệ thống cập nhật client qua mạng
-- ═════════════════════════════════════════════════════════════

-- Mỗi asset (ảnh, config, data) được track version + hash
CREATE TABLE IF NOT EXISTS client_assets (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    asset_key       VARCHAR(256) NOT NULL UNIQUE,  -- VD: "Sprites/Items/item_001.png", "Config/skills.json"
    asset_type      VARCHAR(32) NOT NULL,           -- sprite,atlas,icon,bg,tileset,effect,particle,font,video,shader,config,data
    category        VARCHAR(64) NOT NULL DEFAULT 'general', -- CONTENT: weapon,armor,accessory,consumable,material,farm_seed,farm_crop,farm_feed,farm_produce,farm_animal,farm_tool,gem,cosmetic,pet,mount,monster,npc,skill,title,furniture | SCENE: map_bg,map_tile,sky,parallax | SYSTEM: ui,hud,icon,button,frame,font,effect,particle,loading,logo | DATA: config,localization
    file_path       VARCHAR(512) NOT NULL,          -- đường dẫn thực trên server
    file_size       INT NOT NULL DEFAULT 0,         -- bytes
    hash_md5        VARCHAR(32) NOT NULL DEFAULT '', -- MD5 hash để client so sánh
    version         INT NOT NULL DEFAULT 1,         -- tăng mỗi lần upload mới
    display_name    VARCHAR(128) DEFAULT '',
    description     VARCHAR(256) DEFAULT '',
    mime_type       VARCHAR(64) DEFAULT 'image/png',
    width           INT DEFAULT 0,                  -- cho ảnh
    height          INT DEFAULT 0,
    is_required     TINYINT NOT NULL DEFAULT 1,     -- 1=phải tải, 0=optional
    is_active       TINYINT NOT NULL DEFAULT 1,
    uploaded_by     VARCHAR(64) DEFAULT 'admin',
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_type_cat (asset_type, category),
    INDEX idx_version (version)
);

-- Bundle: nhóm asset để cập nhật đồng thời
CREATE TABLE IF NOT EXISTS asset_bundles (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    bundle_name     VARCHAR(128) NOT NULL,          -- "HUD Pack v2", "Monster Sprites v3"
    description     TEXT,
    version         INT NOT NULL DEFAULT 1,
    total_size      BIGINT NOT NULL DEFAULT 0,      -- tổng kích thước
    asset_count     INT NOT NULL DEFAULT 0,
    bundle_category VARCHAR(32) NOT NULL DEFAULT 'mixed', -- sprite,audio,ui,map,effect,font,mixed
    status          VARCHAR(16) NOT NULL DEFAULT 'draft', -- draft,published,archived
    published_at    DATETIME DEFAULT NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS asset_bundle_items (
    bundle_id       INT NOT NULL,
    asset_id        INT NOT NULL,
    PRIMARY KEY (bundle_id, asset_id),
    FOREIGN KEY (bundle_id) REFERENCES asset_bundles(id) ON DELETE CASCADE,
    FOREIGN KEY (asset_id) REFERENCES client_assets(id) ON DELETE CASCADE
);

-- Client version tracking
CREATE TABLE IF NOT EXISTS client_versions (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    platform        VARCHAR(16) NOT NULL,           -- android,ios,pc,webgl,j2me
    version_code    INT NOT NULL,                   -- 1,2,3... (tăng dần)
    version_name    VARCHAR(32) NOT NULL,            -- "1.0.0", "1.1.0"
    min_asset_version INT NOT NULL DEFAULT 1,       -- asset version tối thiểu cần có
    download_url    VARCHAR(512) DEFAULT '',         -- link tải APK/IPA/JAR mới
    release_notes   TEXT,
    is_force_update TINYINT NOT NULL DEFAULT 0,     -- 1=bắt buộc cập nhật
    is_latest       TINYINT NOT NULL DEFAULT 1,
    published_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_platform (platform, is_latest)
);

INSERT IGNORE INTO client_versions (id,platform,version_code,version_name,download_url) VALUES
(1,'android',1,'1.0.0','/download/NexusIsekai.apk'),
(2,'ios',1,'1.0.0',''),
(3,'pc',1,'1.0.0','/download/NexusIsekai-PC.jar'),
(4,'webgl',1,'1.0.0','/play');

-- Config hot-reload (key-value, client poll định kỳ)
CREATE TABLE IF NOT EXISTS hot_config (
    config_key      VARCHAR(128) NOT NULL PRIMARY KEY,
    config_value    TEXT NOT NULL,
    config_type     VARCHAR(16) NOT NULL DEFAULT 'string', -- string,int,float,json,bool
    category        VARCHAR(64) NOT NULL DEFAULT 'game',
    description     VARCHAR(256) DEFAULT '',
    version         INT NOT NULL DEFAULT 1,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

INSERT IGNORE INTO hot_config VALUES
('server_notice','','string','system','Thông báo trên màn hình login',1,NOW()),
('maintenance_mode','false','bool','system','Chế độ bảo trì',1,NOW()),
('double_exp_active','false','bool','event','Double EXP đang bật',1,NOW()),
('max_enhance_level','10','int','game','Cấp cường hoá tối đa',1,NOW()),
('pvp_enabled','true','bool','game','PvP có bật không',1,NOW()),
('auction_enabled','true','bool','game','Nhà đấu giá có bật không',1,NOW()),
('trade_enabled','true','bool','game','Giao dịch có bật không',1,NOW());

-- ═════════════════════════════════════════════════════════════
-- 34. STORY EDITOR — Quản lý cốt truyện + AI
-- ═════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS story_chapters (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    chapter_order   INT NOT NULL DEFAULT 0,
    title           VARCHAR(128) NOT NULL,
    synopsis        TEXT,                           -- tóm tắt
    full_text       LONGTEXT,                       -- nội dung đầy đủ
    region_id       INT DEFAULT NULL,               -- vùng đất liên quan
    min_level       INT NOT NULL DEFAULT 1,
    max_level       INT NOT NULL DEFAULT 99,
    prev_chapter_id INT DEFAULT NULL,
    next_chapter_id INT DEFAULT NULL,
    bundle_category VARCHAR(32) NOT NULL DEFAULT 'mixed', -- sprite,audio,ui,map,effect,font,mixed
    status          VARCHAR(16) NOT NULL DEFAULT 'draft', -- draft,published,archived
    cutscene_data   TEXT,                           -- JSON: cấu hình cutscene
    ai_generated    TINYINT NOT NULL DEFAULT 0,     -- 1=được AI tạo
    created_by      VARCHAR(64) DEFAULT 'admin',
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS story_quest_links (
    chapter_id      INT NOT NULL,
    quest_id        INT NOT NULL,
    quest_order     INT NOT NULL DEFAULT 0,         -- thứ tự quest trong chapter
    PRIMARY KEY (chapter_id, quest_id),
    FOREIGN KEY (chapter_id) REFERENCES story_chapters(id) ON DELETE CASCADE
);

-- AI generation log
CREATE TABLE IF NOT EXISTS ai_generation_log (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    gen_type        VARCHAR(32) NOT NULL,            -- quest,dialog,story,item_desc,event_desc,announcement
    prompt          TEXT NOT NULL,
    result          LONGTEXT,
    model           VARCHAR(64) DEFAULT 'claude-sonnet-4-20250514',
    tokens_used     INT DEFAULT 0,
    created_by      VARCHAR(64) DEFAULT 'admin',
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Seed story chapters
INSERT IGNORE INTO story_chapters (id,chapter_order,title,synopsis,min_level,status) VALUES
(1,1,'Thức Tỉnh','Lưu Dân tỉnh dậy tại Làng Khải Nguyên, không nhớ gì về quá khứ. Trưởng làng hướng dẫn những bước đầu tiên.',1,'published'),
(2,2,'Sương Mù Bí Ẩn','Tin đồn về quái vật xuất hiện ở Đồng Bằng Sương Mù. Lưu Dân nhận nhiệm vụ điều tra.',10,'published'),
(3,3,'Rừng Ám Ảnh','Ma khí nhiễm vào khu rừng phía Bắc. Cần tìm nguồn gốc sức mạnh tà ác.',25,'published'),
(4,4,'Liên Minh Thiên Quang','Đến thủ phủ, gia nhập liên minh. Tiết lộ về Giáo Phái Vọng Linh.',35,'draft'),
(5,5,'Phong Ấn Rung Chuyển','Phong ấn Azaroth bắt đầu yếu đi. Cuộc đua với thời gian.',50,'draft');

-- ═════════════════════════════════════════════════════════════
-- 35. AI REVIEW WORKFLOW — Duyệt nội dung AI trước khi lên production
-- ═════════════════════════════════════════════════════════════

ALTER TABLE ai_generation_log
    ADD COLUMN IF NOT EXISTS status        VARCHAR(16) NOT NULL DEFAULT 'draft',   -- draft,review,testing,approved,published,rejected
    ADD COLUMN IF NOT EXISTS reviewed_by   VARCHAR(64) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS reviewed_at   DATETIME DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS test_server   VARCHAR(32) DEFAULT NULL,               -- 'test_1','staging'
    ADD COLUMN IF NOT EXISTS applied_to    VARCHAR(256) DEFAULT NULL,              -- JSON: {"quest_id":5} hoặc {"dialog_id":12}
    ADD COLUMN IF NOT EXISTS reject_reason VARCHAR(256) DEFAULT NULL;

-- ═════════════════════════════════════════════════════════════
-- 36. ADMIN AUDIT LOG — Ai làm gì trong admin
-- ═════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS admin_audit_log (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    admin_user      VARCHAR(64) NOT NULL,
    action          VARCHAR(64) NOT NULL,       -- 'create_item','ban_player','approve_ai','upload_asset','grant_diamond'
    target_type     VARCHAR(32) DEFAULT '',      -- 'player','item','quest','announcement','config'
    target_id       VARCHAR(64) DEFAULT '',      -- ID của đối tượng bị ảnh hưởng
    details         TEXT,                         -- JSON chi tiết thay đổi
    ip_address      VARCHAR(45) DEFAULT '',
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_admin (admin_user, created_at DESC),
    INDEX idx_action (action, created_at DESC)
);

-- ═════════════════════════════════════════════════════════════
-- 37. PLAYER MAIL — Hệ thống thư trong game
-- ═════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS player_mail (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipient_id    BIGINT NOT NULL,            -- char_id người nhận
    sender_type     VARCHAR(16) NOT NULL DEFAULT 'system', -- system,admin,player,event
    sender_name     VARCHAR(64) NOT NULL DEFAULT 'He Thong',
    title           VARCHAR(128) NOT NULL,
    content         TEXT NOT NULL,
    attachment_json TEXT DEFAULT NULL,           -- JSON: [{"item_id":5,"qty":2},{"gold":1000}]
    is_read         TINYINT NOT NULL DEFAULT 0,
    is_claimed      TINYINT NOT NULL DEFAULT 0, -- đã nhận vật phẩm đính kèm chưa
    expires_at      DATETIME DEFAULT NULL,       -- NULL = không hết hạn
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_recipient (recipient_id, is_read, created_at DESC)
);

-- ═════════════════════════════════════════════════════════════
-- 38. ADMIN ROLES — Phân quyền admin
-- ═════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS admin_accounts (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(64) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,       -- BCrypt
    display_name    VARCHAR(64) NOT NULL,
    role            VARCHAR(32) NOT NULL DEFAULT 'gm', -- super_admin,admin,gm,support,content_editor,viewer
    permissions     TEXT,                         -- JSON: ["players","items","quests","story","ai","assets","ban","mail"]
    is_active       TINYINT NOT NULL DEFAULT 1,
    last_login      DATETIME DEFAULT NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT IGNORE INTO admin_accounts (id,username,password_hash,display_name,role,permissions) VALUES
(1,'admin','$2a$10$placeholder','Super Admin','super_admin','["*"]');

-- ═════════════════════════════════════════════════════════════
-- 39. SCHEDULED TASKS — Lịch sự kiện tự động
-- ═════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS scheduled_tasks (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    task_name       VARCHAR(64) NOT NULL,
    task_type       VARCHAR(32) NOT NULL,         -- 'double_exp','boss_spawn','maintenance','event_start','event_end','mail_blast','backup'
    cron_expression VARCHAR(64) DEFAULT NULL,     -- '0 20 * * 5' (thứ 6 lúc 20h)
    run_once_at     DATETIME DEFAULT NULL,        -- chạy 1 lần tại thời điểm này
    parameters      TEXT,                          -- JSON: {"exp_multiplier":2,"duration_hours":4}
    is_active       TINYINT NOT NULL DEFAULT 1,
    last_run_at     DATETIME DEFAULT NULL,
    next_run_at     DATETIME DEFAULT NULL,
    created_by      VARCHAR(64) DEFAULT 'admin',
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT IGNORE INTO scheduled_tasks (id,task_name,task_type,cron_expression,parameters) VALUES
(1,'Double EXP Cuoi Tuan','double_exp','0 20 * * 5','{"multiplier":2,"duration_hours":48}'),
(2,'Boss Spawn Hang Ngay','boss_spawn','0 12 * * *','{"boss_id":99,"map_id":5}'),
(3,'Backup DB','backup','0 3 * * *','{"keep_days":7}');

-- ═════════════════════════════════════════════════════════════
-- 40. PLAYER REPORTS — Hệ thống báo cáo / khiếu nại
-- ═════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS player_reports (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    reporter_id     BIGINT NOT NULL,             -- người báo cáo
    reporter_name   VARCHAR(32) NOT NULL,
    reported_id     BIGINT DEFAULT NULL,         -- người bị báo cáo (NULL = báo lỗi chung)
    reported_name   VARCHAR(32) DEFAULT '',
    report_type     VARCHAR(32) NOT NULL,         -- 'cheat','harassment','bug','suggestion','other'
    description     TEXT NOT NULL,
    evidence        TEXT DEFAULT NULL,            -- URL screenshot hoặc chat log
    status          VARCHAR(16) NOT NULL DEFAULT 'open', -- open,investigating,resolved,dismissed
    assigned_to     VARCHAR(64) DEFAULT NULL,     -- admin đang xử lý
    resolution      TEXT DEFAULT NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    resolved_at     DATETIME DEFAULT NULL,
    INDEX idx_status (status, created_at DESC)
);

-- ═════════════════════════════════════════════════════════════
-- 41. SERVER STATS — Thống kê server
-- ═════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS server_stats_hourly (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    recorded_at     DATETIME NOT NULL,
    online_count    INT NOT NULL DEFAULT 0,
    new_registrations INT NOT NULL DEFAULT 0,
    new_characters  INT NOT NULL DEFAULT 0,
    total_revenue   BIGINT NOT NULL DEFAULT 0,    -- diamond bán được
    gold_created    BIGINT NOT NULL DEFAULT 0,    -- gold sinh ra từ monster/quest
    gold_destroyed  BIGINT NOT NULL DEFAULT 0,    -- gold tiêu (shop, enhance, tax)
    trades_count    INT NOT NULL DEFAULT 0,
    pvp_matches     INT NOT NULL DEFAULT 0,
    dungeons_cleared INT NOT NULL DEFAULT 0,
    peak_online     INT NOT NULL DEFAULT 0,
    INDEX idx_time (recorded_at DESC)
);

-- ═════════════════════════════════════════════════════════════
-- 42. ACHIEVEMENT SYSTEM — Thành tựu
-- ═════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS achievements (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(64) NOT NULL,
    description     VARCHAR(256) NOT NULL,
    category        VARCHAR(32) NOT NULL DEFAULT 'general', -- combat,social,economy,exploration,collection,event
    icon_asset      VARCHAR(128) DEFAULT 'Icons/Achievement/default',
    condition_type  VARCHAR(32) NOT NULL,  -- 'kill_monster','reach_level','collect_item','complete_quest','win_pvp','gold_earned','login_days','enhance_success','join_guild','get_married','clear_dungeon'
    condition_value INT NOT NULL DEFAULT 1, -- số lượng cần đạt
    reward_type     VARCHAR(16) DEFAULT 'title',  -- title,item,gold,diamond,exp
    reward_id       INT DEFAULT 0,         -- title_id hoặc item_id
    reward_amount   INT DEFAULT 0,
    points          INT NOT NULL DEFAULT 10, -- điểm thành tựu
    is_hidden       TINYINT NOT NULL DEFAULT 0, -- 1=ẩn cho đến khi hoàn thành
    sort_order      INT NOT NULL DEFAULT 0,
    is_active       TINYINT NOT NULL DEFAULT 1,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS player_achievements (
    char_id         BIGINT NOT NULL,
    achievement_id  INT NOT NULL,
    progress        INT NOT NULL DEFAULT 0,
    completed       TINYINT NOT NULL DEFAULT 0,
    claimed         TINYINT NOT NULL DEFAULT 0,
    completed_at    DATETIME DEFAULT NULL,
    PRIMARY KEY (char_id, achievement_id),
    FOREIGN KEY (achievement_id) REFERENCES achievements(id)
);

INSERT IGNORE INTO achievements (id,name,description,category,condition_type,condition_value,reward_type,reward_amount,points) VALUES
(1,'Buoc Dau Tien','Dat cap 10','combat','reach_level',10,'exp',500,10),
(2,'Chien Binh','Dat cap 30','combat','reach_level',30,'gold',5000,20),
(3,'Anh Hung','Dat cap 50','combat','reach_level',50,'diamond',50,50),
(4,'Truyen Thuyet','Dat cap 99','combat','reach_level',99,'diamond',200,100),
(5,'Sat Thu Quai Vat','Tieu diet 100 quai vat','combat','kill_monster',100,'gold',1000,10),
(6,'Diet Long','Tieu diet 1000 quai vat','combat','kill_monster',1000,'diamond',20,30),
(7,'Nhiem Vu Gia','Hoan thanh 10 nhiem vu','exploration','complete_quest',10,'exp',1000,10),
(8,'Nguoi Kham Pha','Hoan thanh 50 nhiem vu','exploration','complete_quest',50,'gold',5000,25),
(9,'Dai Gia','So huu 100.000 vang','economy','gold_earned',100000,'diamond',10,15),
(10,'Trieu Phu','So huu 1.000.000 vang','economy','gold_earned',1000000,'diamond',50,30),
(11,'Cuong Hoa +5','Cuong hoa thanh cong +5','collection','enhance_success',5,'gold',2000,15),
(12,'Cuong Hoa +10','Cuong hoa thanh cong +10','collection','enhance_success',10,'diamond',100,50),
(13,'PvP Tan Thu','Thang 10 tran PvP','combat','win_pvp',10,'gold',2000,10),
(14,'Vo Dich','Thang 100 tran PvP','combat','win_pvp',100,'diamond',50,40),
(15,'Tham Hiem Gia','Xoa dungeon 10 lan','exploration','clear_dungeon',10,'gold',3000,15),
(16,'Ket Hon','Ket hon voi nguoi choi khac','social','get_married',1,'diamond',20,20),
(17,'Hoi Vien','Gia nhap guild','social','join_guild',1,'gold',500,5),
(18,'Trung Thanh','Dang nhap 7 ngay lien tiep','social','login_days',7,'diamond',10,10),
(19,'Cong Dan','Dang nhap 30 ngay lien tiep','social','login_days',30,'diamond',30,25),
(20,'Nha Thu Thap','So huu 50 vat pham khac nhau','collection','collect_item',50,'gold',5000,20);

-- ═════════════════════════════════════════════════════════════
-- 43. DAILY LOGIN REWARDS — Phần thưởng đăng nhập hàng ngày
-- ═════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS daily_login_rewards (
    day_number      INT NOT NULL PRIMARY KEY,  -- 1-7 (lặp lại)
    reward_type     VARCHAR(16) NOT NULL,       -- gold,diamond,item,exp,event_currency
    reward_id       INT NOT NULL DEFAULT 0,     -- item_id nếu type=item
    reward_amount   INT NOT NULL DEFAULT 0,
    bonus_streak    TINYINT NOT NULL DEFAULT 0,  -- 1=thưởng thêm cho streak đầy đủ
    description     VARCHAR(128) DEFAULT ''
);

CREATE TABLE IF NOT EXISTS player_daily_login (
    char_id         BIGINT NOT NULL PRIMARY KEY,
    current_day     INT NOT NULL DEFAULT 0,     -- ngày hiện tại trong chu kỳ (1-7)
    streak_count    INT NOT NULL DEFAULT 0,     -- số ngày liên tiếp
    last_login_date DATE DEFAULT NULL,          -- ngày đăng nhập gần nhất
    total_logins    INT NOT NULL DEFAULT 0,     -- tổng số ngày đã đăng nhập
    claimed_today   TINYINT NOT NULL DEFAULT 0  -- đã nhận thưởng hôm nay chưa
);

INSERT IGNORE INTO daily_login_rewards VALUES
(1,'gold',0,1000,0,'1.000 Vang'),
(2,'exp',0,500,0,'500 EXP'),
(3,'gold',0,2000,0,'2.000 Vang'),
(4,'item',1001,3,0,'3x Thuoc Hoi Mau'),
(5,'gold',0,3000,0,'3.000 Vang'),
(6,'exp',0,2000,0,'2.000 EXP'),
(7,'diamond',0,10,1,'10 Diamond + Thuong Streak');

-- ═════════════════════════════════════════════════════════════
-- 44. WORLD BOSS — Boss thế giới, spawn theo lịch
-- ═════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS world_bosses (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    monster_id      INT NOT NULL,
    name            VARCHAR(64) NOT NULL,
    map_id          INT NOT NULL,
    spawn_x         FLOAT NOT NULL DEFAULT 0,
    spawn_y         FLOAT NOT NULL DEFAULT 0,
    hp              INT NOT NULL DEFAULT 1000000,
    atk             INT NOT NULL DEFAULT 5000,
    def             INT NOT NULL DEFAULT 3000,
    reward_exp      INT NOT NULL DEFAULT 50000,
    reward_gold     INT NOT NULL DEFAULT 100000,
    loot_json       TEXT,                          -- JSON: [{"item_id":1,"drop_rate":0.1,"qty":1}]
    spawn_cron      VARCHAR(64) DEFAULT '0 20 * * 0', -- chủ nhật 20h
    duration_min    INT NOT NULL DEFAULT 30,        -- tồn tại tối đa 30 phút
    min_players     INT NOT NULL DEFAULT 5,         -- cần ít nhất 5 người
    announce_before_min INT NOT NULL DEFAULT 10,    -- thông báo trước 10 phút
    is_active       TINYINT NOT NULL DEFAULT 1,
    last_spawn_at   DATETIME DEFAULT NULL,
    last_killed_by  VARCHAR(32) DEFAULT NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT IGNORE INTO world_bosses (id,monster_id,name,map_id,spawn_x,spawn_y,hp,reward_exp,reward_gold,spawn_cron) VALUES
(1,901,'Rong Co Dai',5,50,50,500000,30000,50000,'0 20 * * 0'),
(2,902,'Kiem Thanh Am Anh',3,30,30,800000,50000,80000,'0 20 * * 3'),
(3,903,'Tieu Than Phan Than',6,60,60,2000000,100000,200000,'0 20 * * 6');

-- ═════════════════════════════════════════════════════════════
-- 45. MONSTER DROPS — Bảng drop item chi tiết
-- ═════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS monster_drops (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    monster_id      INT NOT NULL,
    item_id         INT NOT NULL,
    drop_rate       FLOAT NOT NULL DEFAULT 0.1,    -- 0.0-1.0 (10%)
    min_qty         INT NOT NULL DEFAULT 1,
    max_qty         INT NOT NULL DEFAULT 1,
    min_level       INT NOT NULL DEFAULT 1,         -- level tối thiểu monster
    is_active       TINYINT NOT NULL DEFAULT 1,
    INDEX idx_monster (monster_id)
);

-- ═════════════════════════════════════════════════════════════
-- 46. NPC SPAWN CONFIG — Quản lý spawn NPC trên map
-- ═════════════════════════════════════════════════════════════

-- Bảng npcs đã có sẵn (schema.sql) với map_id, pos_x, pos_y
-- Thêm fields mở rộng cho NPC management
ALTER TABLE npcs
    ADD COLUMN IF NOT EXISTS npc_type_detail VARCHAR(32) DEFAULT 'generic',  -- generic,quest_giver,shopkeeper,banker,blacksmith,stable_master,teleporter,guard
    ADD COLUMN IF NOT EXISTS interaction     VARCHAR(32) DEFAULT 'dialog',    -- dialog,shop,bank,enhance,teleport,quest,storage
    ADD COLUMN IF NOT EXISTS icon_asset      VARCHAR(128) DEFAULT 'Sprites/NPC/default',
    ADD COLUMN IF NOT EXISTS level_req       INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS quest_ids       VARCHAR(256) DEFAULT '',         -- JSON: [1,5,12] — quest mà NPC này cho
    ADD COLUMN IF NOT EXISTS visible_hours   VARCHAR(32) DEFAULT '',          -- "08:00-22:00" hoặc "" = luôn hiển thị
    ADD COLUMN IF NOT EXISTS respawn_sec     INT NOT NULL DEFAULT 0;          -- 0 = luôn tồn tại

-- ═════════════════════════════════════════════════════════════
-- 47. MONSTER SPAWN ZONES — Khu vực spawn quái
-- ═════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS monster_spawn_zones (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    map_id          INT NOT NULL,
    monster_id      INT NOT NULL,
    zone_x1         FLOAT NOT NULL,     -- góc trên trái
    zone_y1         FLOAT NOT NULL,
    zone_x2         FLOAT NOT NULL,     -- góc dưới phải
    zone_y2         FLOAT NOT NULL,
    max_count       INT NOT NULL DEFAULT 5,      -- số lượng tối đa trong zone
    respawn_sec     INT NOT NULL DEFAULT 30,     -- thời gian respawn
    is_active       TINYINT NOT NULL DEFAULT 1,
    INDEX idx_map (map_id)
);

-- ═════════════════════════════════════════════════════════════
-- 48. EVENT CURRENCY SHOP — CRUD cho shop event token (thiếu)
-- ═════════════════════════════════════════════════════════════
-- Bảng đã có, chỉ cần seed
INSERT IGNORE INTO event_currency_shop (id,currency_id,item_id,item_name,price,stock,per_user_limit,sort_order) VALUES
(1,1,1001,'Thuoc Hoi Mau x10',50,-1,0,1),
(2,1,2001,'Tui Do Mo Rong',200,100,1,2),
(3,1,3001,'Skin Doc Quyen Tet',500,50,1,3);

-- ═════════════════════════════════════════════════════════════

-- ═════════════════════════════════════════════════════════════
-- ANIMATION & SPRITE CONFIG — Generic 2D Sprite System
-- ═════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS character_sprite_config (
    config_key      VARCHAR(64) NOT NULL PRIMARY KEY,
    config_value    TEXT NOT NULL,
    description     VARCHAR(256) DEFAULT ''
);

INSERT IGNORE INTO character_sprite_config VALUES
('primary_system', '2d_sprite', 'Generic 2D sprite system'),
('sprite_path_format', 'Sprites/Characters/class_{classId}/{gender}/', 'Duong dan sprite theo class va gioi tinh'),
('creation_mode', 'class_gender', 'Tao nhan vat: chon class + gioi tinh'),
('animation_format', 'spine', 'Spine animation cho NPCs va effects');

-- Tao nhan vat: class_id(1-7) + gender(0/1)
ALTER TABLE characters ADD COLUMN IF NOT EXISTS gender TINYINT NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS animation_states (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    state_key       VARCHAR(32) NOT NULL UNIQUE,
    display_name    VARCHAR(64) NOT NULL,
    frame_count     INT NOT NULL DEFAULT 6,
    frame_rate      FLOAT NOT NULL DEFAULT 8.0,
    is_looping      TINYINT NOT NULL DEFAULT 1,
    sort_order      INT NOT NULL DEFAULT 0
);

INSERT IGNORE INTO animation_states (state_key,display_name,frame_count,frame_rate,is_looping) VALUES
('idle','Dung yen',4,6,1),('walk','Di bo',6,8,1),('run','Chay',6,10,1),
('attack','Tan cong',6,12,0),('cast','Thi phep',6,8,0),
('hurt','Trung don',2,8,0),('die','Chet',4,6,0),('revive','Hoi sinh',4,8,0);

CREATE TABLE IF NOT EXISTS class_animation_map (
    class_id        INT NOT NULL,
    action_key      VARCHAR(32) NOT NULL,
    animation_state VARCHAR(32) NOT NULL,
    PRIMARY KEY (class_id, action_key)
);

INSERT IGNORE INTO class_animation_map VALUES
(1,'attack','attack'),(2,'attack','cast'),(3,'attack','attack'),
(4,'attack','attack'),(5,'attack','attack'),(6,'attack','attack'),(7,'attack','attack');

-- ═════════════════════════════════════════════════════════════
-- GAME PROTECTION SYSTEM — Mã hoá, chống hack, bảo vệ
-- ═════════════════════════════════════════════════════════════

-- Client integrity check
CREATE TABLE IF NOT EXISTS client_integrity (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    platform        VARCHAR(16) NOT NULL,        -- android,ios,pc,webgl
    version         VARCHAR(32) NOT NULL,
    checksum_md5    VARCHAR(64) NOT NULL,         -- MD5 của client build
    checksum_sha256 VARCHAR(128) NOT NULL,
    is_valid        TINYINT NOT NULL DEFAULT 1,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY (platform, version)
);

-- Encryption keys (rotate mỗi ngày)
CREATE TABLE IF NOT EXISTS encryption_keys (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    key_type        VARCHAR(16) NOT NULL,         -- packet,asset,token
    key_value       VARCHAR(256) NOT NULL,
    valid_from      TIMESTAMP NOT NULL,
    valid_to        TIMESTAMP NOT NULL,
    is_active       TINYINT NOT NULL DEFAULT 1,
    INDEX idx_type_active (key_type, is_active)
);

-- Anti-cheat log
CREATE TABLE IF NOT EXISTS anticheat_log (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    char_id         BIGINT NOT NULL,
    account_id      BIGINT NOT NULL,
    violation_type  VARCHAR(32) NOT NULL,          -- speedhack,teleport,dmg_hack,packet_flood,memory_edit,injector
    severity        TINYINT NOT NULL DEFAULT 1,    -- 1=warn,2=kick,3=ban_temp,4=ban_perm
    detail          TEXT,
    client_ip       VARCHAR(45),
    device_id       VARCHAR(128),
    action_taken    VARCHAR(32) DEFAULT 'none',    -- none,warn,kick,ban_1d,ban_7d,ban_perm
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_char (char_id),
    INDEX idx_type (violation_type)
);

-- Device fingerprint (chống multi-account abuse)
CREATE TABLE IF NOT EXISTS device_fingerprints (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    account_id      BIGINT NOT NULL,
    device_id       VARCHAR(128) NOT NULL,
    device_model    VARCHAR(64),
    os_version      VARCHAR(32),
    screen_res      VARCHAR(16),
    first_seen      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_seen       TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_banned       TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY (account_id, device_id),
    INDEX idx_device (device_id)
);

-- Protection config
CREATE TABLE IF NOT EXISTS protection_config (
    config_key      VARCHAR(64) NOT NULL PRIMARY KEY,
    config_value    TEXT NOT NULL,
    description     VARCHAR(256) DEFAULT ''
);

INSERT IGNORE INTO protection_config VALUES
-- Packet encryption
('packet_encrypt','aes_128','Mã hoá packet: none, xor, aes_128, aes_256'),
('packet_key_rotate','86400','Thời gian rotate key (giây, 86400=1 ngày)'),
('packet_compress','lz4','Nén packet: none, lz4, zlib, gzip'),
('packet_sign','hmac_sha256','Chữ ký packet: none, crc32, hmac_sha256'),
-- Asset encryption
('asset_encrypt','aes_128','Mã hoá asset files: none, xor, aes_128'),
('asset_key','nexus_isekai_2024','Key mã hoá assets (thay đổi mỗi build)'),
('asset_verify','md5','Kiểm tra tính toàn vẹn asset: none, md5, sha256'),
-- Anti-cheat
('speed_check','true','Kiểm tra tốc độ di chuyển bất thường'),
('speed_max_tiles','8','Tốc độ tối đa (tiles/giây)'),
('teleport_check','true','Phát hiện teleport hack'),
('teleport_max_dist','50','Khoảng cách teleport tối đa cho phép (tiles)'),
('dmg_check','true','Kiểm tra sát thương bất thường'),
('dmg_max_ratio','3.0','Tỉ lệ sát thương tối đa so với base'),
('packet_flood_limit','100','Số packet tối đa/giây trước khi kick'),
('packet_flood_ban','300','Số packet/giây trước khi ban tạm'),
-- Client integrity
('integrity_check','true','Kiểm tra checksum client khi connect'),
('root_detect','true','Phát hiện thiết bị root/jailbreak'),
('emulator_detect','true','Phát hiện emulator'),
('injector_detect','true','Phát hiện memory injector (GameGuardian, Cheat Engine)'),
('debug_detect','true','Phát hiện debugger attach'),
-- Session
('session_timeout','1800','Timeout phiên (giây, 1800=30 phút)'),
('max_sessions','1','Số phiên đồng thời tối đa/tài khoản'),
('ip_ban_threshold','10','Số lần vi phạm trước khi ban IP'),
-- Obfuscation
('obfuscate_packets','true','Xáo trộn opcode mapping mỗi phiên'),
('obfuscate_seed','random','Seed xáo trộn: random hoặc giá trị cố định');

-- ═════════════════════════════════════════════════════════════
-- GACHA / TRIỆU HỒI
-- ═════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS gacha_banners (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(64) NOT NULL,
    banner_type     VARCHAR(16) NOT NULL DEFAULT 'standard',  -- standard,limited,weapon,pet,mount
    cost_type       VARCHAR(16) NOT NULL DEFAULT 'diamond',
    cost_single     INT NOT NULL DEFAULT 100,
    cost_multi_10   INT NOT NULL DEFAULT 900,
    pity_count      INT NOT NULL DEFAULT 80,        -- guaranteed SSR after N pulls
    start_date      TIMESTAMP NULL,
    end_date        TIMESTAMP NULL,
    is_active       TINYINT NOT NULL DEFAULT 1,
    banner_image    VARCHAR(128) DEFAULT '',
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS gacha_pool (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    banner_id       INT NOT NULL,
    reward_type     VARCHAR(16) NOT NULL,            -- item,pet,mount,skin,character
    reward_id       INT NOT NULL,
    rarity          TINYINT NOT NULL DEFAULT 0,      -- 0=N,1=R,2=SR,3=SSR,4=UR
    weight          INT NOT NULL DEFAULT 100,        -- tỉ lệ (weight/total_weight)
    is_featured     TINYINT NOT NULL DEFAULT 0,
    INDEX idx_banner (banner_id)
);

CREATE TABLE IF NOT EXISTS gacha_history (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    char_id         BIGINT NOT NULL,
    banner_id       INT NOT NULL,
    reward_type     VARCHAR(16) NOT NULL,
    reward_id       INT NOT NULL,
    rarity          TINYINT NOT NULL DEFAULT 0,
    pull_number     INT NOT NULL DEFAULT 1,          -- số lần kéo hiện tại (cho pity)
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_char (char_id, banner_id)
);

CREATE TABLE IF NOT EXISTS gacha_pity (
    char_id         BIGINT NOT NULL,
    banner_id       INT NOT NULL,
    pull_count      INT NOT NULL DEFAULT 0,
    last_ssr_pull   INT NOT NULL DEFAULT 0,
    PRIMARY KEY (char_id, banner_id)
);

INSERT IGNORE INTO gacha_banners (id,name,banner_type,cost_single,cost_multi_10,pity_count) VALUES
(1,'Trieu Hoi Thuong','standard',100,900,80),
(2,'Trieu Hoi Thu Cuoi','mount',200,1800,60),
(3,'Trieu Hoi Pet','pet',150,1350,50);

-- ═════════════════════════════════════════════════════════════
-- PVP SEASON
-- ═════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS pvp_seasons (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    season_name     VARCHAR(64) NOT NULL,
    start_date      TIMESTAMP NOT NULL,
    end_date        TIMESTAMP NOT NULL,
    is_active       TINYINT NOT NULL DEFAULT 0,
    reset_elo       TINYINT NOT NULL DEFAULT 1,      -- reset ELO mỗi mùa
    base_elo        INT NOT NULL DEFAULT 1000
);

CREATE TABLE IF NOT EXISTS pvp_season_rewards (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    season_id       INT NOT NULL,
    min_rank        INT NOT NULL,                    -- rank tối thiểu
    max_rank        INT NOT NULL,
    tier_name       VARCHAR(32) NOT NULL,            -- Bronze,Silver,Gold,Platinum,Diamond,Master,Grandmaster
    reward_type     VARCHAR(16) NOT NULL,
    reward_id       INT NOT NULL DEFAULT 0,
    reward_amount   INT NOT NULL DEFAULT 1,
    exclusive_skin  INT DEFAULT 0,                   -- skin độc quyền mùa
    INDEX idx_season (season_id)
);

CREATE TABLE IF NOT EXISTS pvp_player_season (
    char_id         BIGINT NOT NULL,
    season_id       INT NOT NULL,
    elo             INT NOT NULL DEFAULT 1000,
    wins            INT NOT NULL DEFAULT 0,
    losses          INT NOT NULL DEFAULT 0,
    win_streak      INT NOT NULL DEFAULT 0,
    max_streak      INT NOT NULL DEFAULT 0,
    tier            VARCHAR(32) NOT NULL DEFAULT 'Bronze',
    claimed_reward  TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (char_id, season_id),
    INDEX idx_elo (season_id, elo DESC)
);

INSERT IGNORE INTO pvp_seasons (id,season_name,start_date,end_date,is_active) VALUES
(1,'Mua 1 - Khai Mo','2025-01-01','2025-03-31',1);

-- ═════════════════════════════════════════════════════════════
-- SOCIAL LOGIN
-- ═════════════════════════════════════════════════════════════

ALTER TABLE accounts ADD COLUMN IF NOT EXISTS google_id VARCHAR(128) DEFAULT NULL;
ALTER TABLE accounts ADD COLUMN IF NOT EXISTS facebook_id VARCHAR(128) DEFAULT NULL;
ALTER TABLE accounts ADD COLUMN IF NOT EXISTS apple_id VARCHAR(128) DEFAULT NULL;
ALTER TABLE accounts ADD COLUMN IF NOT EXISTS login_method VARCHAR(16) NOT NULL DEFAULT 'local'; -- local,google,facebook,apple
ALTER TABLE accounts ADD COLUMN IF NOT EXISTS linked_email VARCHAR(128) DEFAULT NULL;

CREATE TABLE IF NOT EXISTS social_auth_tokens (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    account_id      BIGINT NOT NULL,
    provider        VARCHAR(16) NOT NULL,            -- google,facebook,apple
    provider_uid    VARCHAR(128) NOT NULL,
    access_token    TEXT,
    refresh_token   TEXT,
    expires_at      TIMESTAMP NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY (provider, provider_uid),
    INDEX idx_account (account_id)
);

-- ═════════════════════════════════════════════════════════════
-- PUSH NOTIFICATIONS (Firebase Cloud Messaging)
-- ═════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS push_tokens (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    account_id      BIGINT NOT NULL,
    device_token    VARCHAR(256) NOT NULL,
    platform        VARCHAR(16) NOT NULL,            -- android,ios
    is_active       TINYINT NOT NULL DEFAULT 1,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY (account_id, device_token)
);

CREATE TABLE IF NOT EXISTS push_campaigns (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    title           VARCHAR(128) NOT NULL,
    body            TEXT NOT NULL,
    target          VARCHAR(32) NOT NULL DEFAULT 'all', -- all,active,inactive,vip,custom
    target_filter   TEXT,                            -- JSON filter cho custom target
    scheduled_at    TIMESTAMP NULL,
    sent_at         TIMESTAMP NULL,
    sent_count      INT NOT NULL DEFAULT 0,
    status          VARCHAR(16) NOT NULL DEFAULT 'draft', -- draft,scheduled,sending,sent
    created_by      VARCHAR(64) DEFAULT 'admin',
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ═════════════════════════════════════════════════════════════
-- ANALYTICS
-- ═════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS analytics_events (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    char_id         BIGINT DEFAULT 0,
    account_id      BIGINT DEFAULT 0,
    event_type      VARCHAR(32) NOT NULL,            -- login,logout,level_up,purchase,gacha_pull,pvp_match,quest_complete,death,trade,chat
    event_data      JSON,
    session_id      VARCHAR(64),
    client_ip       VARCHAR(45),
    platform        VARCHAR(16),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_type_date (event_type, created_at),
    INDEX idx_char (char_id, created_at)
);

CREATE TABLE IF NOT EXISTS analytics_daily (
    date_key        DATE NOT NULL,
    dau             INT NOT NULL DEFAULT 0,          -- Daily Active Users
    new_users       INT NOT NULL DEFAULT 0,
    revenue         BIGINT NOT NULL DEFAULT 0,       -- tổng nạp (VND)
    sessions        INT NOT NULL DEFAULT 0,
    avg_session_min FLOAT NOT NULL DEFAULT 0,
    pvp_matches     INT NOT NULL DEFAULT 0,
    gacha_pulls     INT NOT NULL DEFAULT 0,
    items_traded    INT NOT NULL DEFAULT 0,
    PRIMARY KEY (date_key)
);

CREATE TABLE IF NOT EXISTS analytics_retention (
    cohort_date     DATE NOT NULL,                   -- ngày đăng ký
    day_n           INT NOT NULL,                    -- day 1, 3, 7, 14, 30
    cohort_size     INT NOT NULL DEFAULT 0,
    retained        INT NOT NULL DEFAULT 0,
    retention_rate  FLOAT NOT NULL DEFAULT 0,
    PRIMARY KEY (cohort_date, day_n)
);

CREATE TABLE IF NOT EXISTS analytics_funnel (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    funnel_name     VARCHAR(64) NOT NULL,            -- tutorial,first_purchase,first_pvp
    step_order      INT NOT NULL,
    step_name       VARCHAR(64) NOT NULL,
    users_entered   INT NOT NULL DEFAULT 0,
    users_completed INT NOT NULL DEFAULT 0,
    date_key        DATE NOT NULL,
    INDEX idx_funnel_date (funnel_name, date_key)
);

-- ═════════════════════════════════════════════════════════════
-- TUTORIAL SYSTEM
-- ═════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS tutorial_steps (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    step_key        VARCHAR(32) NOT NULL UNIQUE,
    step_order      INT NOT NULL,
    title           VARCHAR(64) NOT NULL,
    description     TEXT,
    target_ui       VARCHAR(64) DEFAULT '',           -- UI element to highlight
    arrow_dir       VARCHAR(16) DEFAULT 'none',       -- none,up,down,left,right
    require_action  VARCHAR(32) DEFAULT '',            -- tap_button,move,attack,open_inventory,equip_item
    reward_type     VARCHAR(16) DEFAULT '',
    reward_amount   INT NOT NULL DEFAULT 0,
    next_step       VARCHAR(32) DEFAULT '',
    can_skip        TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS player_tutorial (
    char_id         BIGINT NOT NULL PRIMARY KEY,
    current_step    VARCHAR(32) NOT NULL DEFAULT 'welcome',
    completed       TINYINT NOT NULL DEFAULT 0,
    skipped         TINYINT NOT NULL DEFAULT 0,
    completed_at    TIMESTAMP NULL
);

INSERT IGNORE INTO tutorial_steps (step_key,step_order,title,description,target_ui,arrow_dir,require_action,reward_type,reward_amount,next_step) VALUES
('welcome',1,'Chao mung!','Chao mung den Vong Linh Gioi!','','none','tap_button','',0,'move_intro'),
('move_intro',2,'Di chuyen','Dung joystick de di chuyen nhan vat','joystick','down','move','',0,'attack_intro'),
('attack_intro',3,'Tan cong','Cham vao nut tan cong de danh quai','btn_attack','right','attack','exp',50,'loot_intro'),
('loot_intro',4,'Nhat do','Di chuyen den vat pham roi de nhat','loot_item','down','pickup','',0,'inventory_intro'),
('inventory_intro',5,'Tui do','Mo tui do de xem vat pham','btn_inventory','left','open_inventory','',0,'equip_intro'),
('equip_intro',6,'Trang bi','Keo vat pham vao o trang bi','equip_slot','up','equip_item','gold',100,'quest_intro'),
('quest_intro',7,'Nhiem vu','Nhan nhiem vu tu NPC','quest_npc','right','accept_quest','',0,'chat_intro'),
('chat_intro',8,'Chat','Gui tin nhan cho nguoi choi khac','btn_chat','left','send_chat','diamond',10,'tutorial_complete'),
('tutorial_complete',9,'Hoan thanh!','Ban da san sang phieu luu!','','none','tap_button','gold',500,'');

-- ═════════════════════════════════════════════════════════════
-- LOCALIZATION — Tiếng Việt + Tiếng Anh
-- ═════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS localization (
    lang_key        VARCHAR(128) NOT NULL,
    lang_code       VARCHAR(5) NOT NULL,             -- vi, en
    text_value      TEXT NOT NULL,
    category        VARCHAR(32) DEFAULT 'ui',        -- ui,quest,item,skill,npc,system,tutorial
    PRIMARY KEY (lang_key, lang_code),
    INDEX idx_cat (category, lang_code)
);

-- Seed UI strings
INSERT IGNORE INTO localization (lang_key,lang_code,text_value,category) VALUES
-- UI chung
('btn_login','vi','Dang Nhap','ui'),('btn_login','en','Login','ui'),
('btn_register','vi','Dang Ky','ui'),('btn_register','en','Register','ui'),
('btn_play','vi','Choi','ui'),('btn_play','en','Play','ui'),
('btn_settings','vi','Cai Dat','ui'),('btn_settings','en','Settings','ui'),
('btn_inventory','vi','Tui Do','ui'),('btn_inventory','en','Inventory','ui'),
('btn_skills','vi','Ky Nang','ui'),('btn_skills','en','Skills','ui'),
('btn_quest','vi','Nhiem Vu','ui'),('btn_quest','en','Quests','ui'),
('btn_shop','vi','Cua Hang','ui'),('btn_shop','en','Shop','ui'),
('btn_chat','vi','Chat','ui'),('btn_chat','en','Chat','ui'),
('btn_guild','vi','Bang Hoi','ui'),('btn_guild','en','Guild','ui'),
('btn_party','vi','Nhom','ui'),('btn_party','en','Party','ui'),
('btn_mail','vi','Thu','ui'),('btn_mail','en','Mail','ui'),
('btn_gacha','vi','Trieu Hoi','ui'),('btn_gacha','en','Summon','ui'),
('btn_pvp','vi','Dau Truong','ui'),('btn_pvp','en','Arena','ui'),
('btn_confirm','vi','Xac Nhan','ui'),('btn_confirm','en','Confirm','ui'),
('btn_cancel','vi','Huy','ui'),('btn_cancel','en','Cancel','ui'),
('btn_close','vi','Dong','ui'),('btn_close','en','Close','ui'),
('btn_back','vi','Quay Lai','ui'),('btn_back','en','Back','ui'),
-- Character creation
('create_title','vi','Tao Nhan Vat','ui'),('create_title','en','Create Character','ui'),
('create_name','vi','Ten Nhan Vat','ui'),('create_name','en','Character Name','ui'),
('create_class','vi','Chon Nghe','ui'),('create_class','en','Select Class','ui'),
('create_gender','vi','Gioi Tinh','ui'),('create_gender','en','Gender','ui'),
('gender_male','vi','Nam','ui'),('gender_male','en','Male','ui'),
('gender_female','vi','Nu','ui'),('gender_female','en','Female','ui'),
-- Classes
('class_1','vi','Kiem Si','ui'),('class_1','en','Swordsman','ui'),
('class_2','vi','Phap Su','ui'),('class_2','en','Mage','ui'),
('class_3','vi','Xa Thu','ui'),('class_3','en','Gunner','ui'),
('class_4','vi','Slinger','ui'),('class_4','en','Slinger','ui'),
('class_5','vi','Axeman','ui'),('class_5','en','Axeman','ui'),
('class_6','vi','Quyen Su','ui'),('class_6','en','Brawler','ui'),
('class_7','vi','Cung Thu','ui'),('class_7','en','Archer','ui'),
-- HUD
('hud_level','vi','Cap','ui'),('hud_level','en','Lv','ui'),
('hud_exp','vi','Kinh Nghiem','ui'),('hud_exp','en','EXP','ui'),
('hud_gold','vi','Vang','ui'),('hud_gold','en','Gold','ui'),
('hud_diamond','vi','Kim Cuong','ui'),('hud_diamond','en','Diamond','ui'),
-- System
('sys_maintenance','vi','He thong dang bao tri','system'),('sys_maintenance','en','System maintenance','system'),
('sys_update','vi','Co ban cap nhat moi','system'),('sys_update','en','New update available','system'),
('sys_kicked','vi','Ban bi ngat ket noi','system'),('sys_kicked','en','You have been disconnected','system');

-- Sound/Music config (managed in admin)
CREATE TABLE IF NOT EXISTS audio_assets (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    asset_key       VARCHAR(64) NOT NULL UNIQUE,     -- bgm_village, sfx_sword_hit, ui_button_click
    asset_type      VARCHAR(8) NOT NULL,             -- bgm, sfx, ambient, voice, ui
    category        VARCHAR(32) NOT NULL DEFAULT 'general', -- BGM: scene_town,scene_field,scene_dungeon,scene_boss,scene_login,event | SFX: combat,skill,farm,ui_click,item,levelup | AMBIENT: nature,cave,rain | VOICE: npc,intro
    file_path       VARCHAR(256) NOT NULL,
    volume_default  FLOAT NOT NULL DEFAULT 1.0,
    is_loop         TINYINT NOT NULL DEFAULT 0,
    description     VARCHAR(128) DEFAULT '',
    is_active       TINYINT NOT NULL DEFAULT 1
);

INSERT IGNORE INTO audio_assets (asset_key,asset_type,file_path,is_loop,description) VALUES
('bgm_login','bgm','Audio/BGM/login.ogg',1,'Nhac man hinh dang nhap'),
('bgm_village','bgm','Audio/BGM/village.ogg',1,'Nhac lang'),
('bgm_field','bgm','Audio/BGM/field.ogg',1,'Nhac dong bang'),
('bgm_dungeon','bgm','Audio/BGM/dungeon.ogg',1,'Nhac dungeon'),
('bgm_boss','bgm','Audio/BGM/boss.ogg',1,'Nhac danh boss'),
('bgm_pvp','bgm','Audio/BGM/pvp.ogg',1,'Nhac PvP arena'),
('sfx_hit','sfx','Audio/SFX/hit.ogg',0,'Am thanh trung don'),
('sfx_crit','sfx','Audio/SFX/crit.ogg',0,'Am thanh chi mang'),
('sfx_die','sfx','Audio/SFX/die.ogg',0,'Am thanh chet'),
('sfx_levelup','sfx','Audio/SFX/levelup.ogg',0,'Am thanh len cap'),
('sfx_loot','sfx','Audio/SFX/loot.ogg',0,'Am thanh nhat do'),
('sfx_equip','sfx','Audio/SFX/equip.ogg',0,'Am thanh trang bi'),
('sfx_enhance_ok','sfx','Audio/SFX/enhance_ok.ogg',0,'Cuong hoa thanh cong'),
('sfx_enhance_fail','sfx','Audio/SFX/enhance_fail.ogg',0,'Cuong hoa that bai'),
('sfx_gacha','sfx','Audio/SFX/gacha.ogg',0,'Am thanh trieu hoi'),
('ui_click','ui','Audio/UI/click.ogg',0,'Click nut'),
('ui_open','ui','Audio/UI/open.ogg',0,'Mo panel'),
('ui_close','ui','Audio/UI/close.ogg',0,'Dong panel'),
('ui_coin','ui','Audio/UI/coin.ogg',0,'Nhan vang'),
('ambient_village','ambient','Audio/Ambient/village.ogg',1,'Am thanh moi truong lang'),
('ambient_forest','ambient','Audio/Ambient/forest.ogg',1,'Am thanh rung');

-- ═════════════════════════════════════════════════════════════
-- GACHA CURRENCY — Mỗi banner có item gacha riêng
-- ═════════════════════════════════════════════════════════════

-- Vé / Key / Mảnh triệu hồi — mỗi banner dùng 1 loại
CREATE TABLE IF NOT EXISTS gacha_currencies (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    currency_key    VARCHAR(32) NOT NULL UNIQUE,      -- 'ticket_standard','ticket_limited','key_pet','shard_weapon'
    display_name    VARCHAR(64) NOT NULL,
    description     VARCHAR(128) DEFAULT '',
    icon_asset      VARCHAR(128) DEFAULT '',
    -- Giá mua bằng diamond
    diamond_price   INT NOT NULL DEFAULT 100,         -- giá 1 vé = 100 diamond
    diamond_price_10 INT NOT NULL DEFAULT 900,        -- giá 10 vé = 900 diamond (giảm 10%)
    -- Giới hạn
    max_stack       INT NOT NULL DEFAULT 9999,
    is_tradeable    TINYINT NOT NULL DEFAULT 0,       -- không giao dịch được
    is_active       TINYINT NOT NULL DEFAULT 1
);

INSERT IGNORE INTO gacha_currencies (id,currency_key,display_name,description,diamond_price,diamond_price_10) VALUES
(1,'ticket_standard','Ve Trieu Hoi Thuong','Ve trieu hoi banner thuong, khong het han',100,900),
(2,'ticket_limited','Ve Trieu Hoi Gioi Han','Ve trieu hoi banner gioi han, het han theo mua',160,1440),
(3,'key_pet','Chia Khoa Thu Cuoi','Mo hom trieu hoi thu cuoi',200,1800),
(4,'key_mount','Chia Khoa Zu Ky','Mo hom trieu hoi tho cuoi',200,1800),
(5,'shard_weapon','Manh Vu Khi','Manh trieu hoi vu khi huyen thoai',250,2250);

-- Link banner → currency (mỗi banner dùng currency nào)
ALTER TABLE gacha_banners ADD COLUMN IF NOT EXISTS currency_id INT NOT NULL DEFAULT 1;
ALTER TABLE gacha_banners ADD COLUMN IF NOT EXISTS cost_currency_single INT NOT NULL DEFAULT 1;   -- 1 vé/lần
ALTER TABLE gacha_banners ADD COLUMN IF NOT EXISTS cost_currency_multi INT NOT NULL DEFAULT 10;    -- 10 vé/10 lần

-- Player sở hữu gacha currency
CREATE TABLE IF NOT EXISTS player_gacha_currency (
    char_id         BIGINT NOT NULL,
    currency_id     INT NOT NULL,
    amount          INT NOT NULL DEFAULT 0,
    total_earned    BIGINT NOT NULL DEFAULT 0,        -- tổng đã nhận (từ quest + mua + event)
    total_spent     BIGINT NOT NULL DEFAULT 0,        -- tổng đã dùng
    PRIMARY KEY (char_id, currency_id)
);

-- Nguồn kiếm gacha currency
CREATE TABLE IF NOT EXISTS gacha_currency_sources (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    currency_id     INT NOT NULL,
    source_type     VARCHAR(16) NOT NULL,             -- quest,daily_login,achievement,event,pvp_reward,shop,admin
    source_id       INT NOT NULL DEFAULT 0,           -- ID của quest/achievement/event
    amount          INT NOT NULL DEFAULT 1,           -- số lượng nhận
    description     VARCHAR(128) DEFAULT '',
    is_active       TINYINT NOT NULL DEFAULT 1,
    INDEX idx_currency (currency_id, source_type)
);

-- Seed: nguồn kiếm vé triệu hồi
INSERT IGNORE INTO gacha_currency_sources (currency_id,source_type,source_id,amount,description) VALUES
-- Vé thường — kiếm từ nhiều nguồn (ít)
(1,'daily_login',7,1,'Dang nhap ngay 7 nhan 1 ve'),
(1,'achievement',0,1,'Hoan thanh thanh tuu nhan 1 ve'),
(1,'quest',0,1,'Mot so nhiem vu chinh thuong 1 ve'),
(1,'pvp_reward',0,2,'Ket thuc mua PvP (Gold+) nhan 2 ve'),
(1,'event',0,1,'Su kien dac biet thuong ve'),
-- Vé giới hạn — rất hiếm, chủ yếu mua diamond
(2,'event',0,1,'Su kien gioi han thuong 1 ve'),
(2,'achievement',0,1,'Thanh tuu dac biet thuong 1 ve'),
-- Chìa khoá pet — hiếm
(3,'quest',0,1,'Quest dac biet thuong chia khoa'),
(3,'daily_login',7,1,'Dang nhap ngay 7 co co hoi nhan'),
-- Chìa khoá mount — rất hiếm
(4,'event',0,1,'Chi co trong event'),
-- Mảnh vũ khí — hiếm nhất
(5,'pvp_reward',0,1,'Top 10 PvP mua nhan 1 manh'),
(5,'event',0,1,'Event dac biet');

-- Shop mua vé bằng diamond (trong game shop)
-- Dùng cost_type='diamond' + cost_single/cost_multi_10 từ gacha_banners
-- Hoặc player mua trực tiếp gacha_currency bằng diamond qua API

-- ═════════════════════════════════════════════════════════════
-- INTRO CUTSCENE — Hoạt ảnh giới thiệu cốt truyện khi vào lần đầu
-- ═════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS intro_scenes (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    scene_order     INT NOT NULL,
    scene_type      VARCHAR(16) NOT NULL,           -- text,image,animation,video,transition
    -- Nội dung
    bg_image        VARCHAR(128) DEFAULT '',         -- ảnh nền
    bg_color        VARCHAR(8) DEFAULT '#000000',    -- màu nền (khi không có ảnh)
    character_image VARCHAR(128) DEFAULT '',         -- ảnh nhân vật/NPC nói
    character_pos   VARCHAR(8) DEFAULT 'center',     -- left,center,right
    -- Text
    narrator        VARCHAR(64) DEFAULT '',          -- tên người kể (trống = narrator)
    text_vi         TEXT,
    text_en         TEXT,
    text_effect     VARCHAR(16) DEFAULT 'typewriter', -- typewriter,fade,instant
    text_speed      FLOAT NOT NULL DEFAULT 0.05,     -- giây/ký tự
    -- Timing
    duration        FLOAT NOT NULL DEFAULT 0,        -- 0 = chờ tap, >0 = tự chuyển sau N giây
    transition_in   VARCHAR(16) DEFAULT 'fade',      -- fade,slide_left,slide_right,zoom,none
    transition_out  VARCHAR(16) DEFAULT 'fade',
    -- Audio
    bgm_key         VARCHAR(64) DEFAULT '',          -- key từ audio_assets
    sfx_key         VARCHAR(64) DEFAULT '',
    -- Flags
    can_skip        TINYINT NOT NULL DEFAULT 1,
    is_active       TINYINT NOT NULL DEFAULT 1
);

-- Cốt truyện intro (7 scenes)
INSERT IGNORE INTO intro_scenes (scene_order,scene_type,bg_image,narrator,text_vi,text_en,duration,bgm_key) VALUES
(1,'text','Intro/bg_dark.png','',
  '5000 năm trước, khi ranh giới giữa các thế giới vẫn còn vững chắc...',
  '5000 years ago, when the boundaries between worlds were still strong...',
  0,'bgm_login'),
(2,'text','Intro/bg_war.png','',
  'Tiểu Thần Azaroth phát động cuộc chiến Đại Hoành Điểu, nhằm phá vỡ bức tường ngăn cách các chiều không gian.',
  'The Lesser God Azaroth launched the Great Havoc War, seeking to shatter the walls between dimensions.',
  0,''),
(3,'text','Intro/bg_heroes.png','',
  'Bảy Anh Hùng Thượng Cổ đã hy sinh tính mạng để niêm phong hắn, nhưng để lại những vết nứt không gian rải khắp thế giới.',
  'Seven Ancient Heroes sacrificed their lives to seal him away, but left spatial cracks scattered across the world.',
  0,''),
(4,'text','Intro/bg_crack.png','',
  'Hiện tại, vết nứt đang mở rộng. Quái vật từ chiều không gian khác tràn vào. Giáo Phái Vọng Linh âm mưu phục sinh Azaroth.',
  'Now, the cracks are widening. Monsters from other dimensions pour in. The Nexus Cult plots to resurrect Azaroth.',
  0,''),
(5,'text','Intro/bg_portal.png','',
  'Và rồi, một cổng thời không xuất hiện trên bầu trời...',
  'And then, a temporal gate appeared in the sky...',
  0,''),
(6,'text','Intro/bg_player.png','',
  'Bạn — một Lưu Dân từ thế giới khác — bị cuốn vào Vọng Linh Giới. Sở hữu khả năng hấp thu linh lực đặc biệt, bạn là hy vọng cuối cùng.',
  'You — a Wanderer from another world — are pulled into the Nexus Realm. With the unique ability to absorb spirit energy, you are the last hope.',
  0,''),
(7,'text','Intro/bg_village.png','',
  'Hành trình bắt đầu tại Làng Khải Nguyên, nơi bạn tỉnh dậy giữa một thế giới xa lạ...',
  'Your journey begins at Genesis Village, where you awaken in a strange new world...',
  0,'bgm_village');

-- Player đã xem intro chưa
CREATE TABLE IF NOT EXISTS player_intro (
    account_id      BIGINT NOT NULL PRIMARY KEY,
    watched         TINYINT NOT NULL DEFAULT 0,
    skipped         TINYINT NOT NULL DEFAULT 0,
    watched_at      TIMESTAMP NULL
);

-- ═════════════════════════════════════════════════════════════
-- LOGIN SCREEN CONFIG — Background, logo, effects
-- ═════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS login_screen_config (
    config_key      VARCHAR(64) NOT NULL PRIMARY KEY,
    config_value    TEXT NOT NULL,
    description     VARCHAR(128) DEFAULT ''
);

INSERT IGNORE INTO login_screen_config VALUES
('bg_image','Login/bg_login.png','Ảnh nền login (1920x1080 hoặc 16:9)'),
('bg_video','','Video nền login (MP4, loop)'),
('bg_parallax','true','Hiệu ứng parallax khi nghiêng điện thoại'),
('logo_image','Login/logo.png','Logo game'),
('logo_animation','bounce','Hiệu ứng logo: none,bounce,pulse,glow'),
('particle_effect','firefly','Hiệu ứng hạt: none,firefly,snow,sakura,sparkle,ember'),
('particle_count','30','Số lượng hạt'),
('bgm_key','bgm_login','Nhạc nền login'),
('server_name','Vọng Linh Giới','Tên server hiển thị'),
('notice_text','','Thông báo trên màn hình login'),
('notice_color','#FFD700','Màu thông báo'),
('version_pos','bottom_right','Vị trí số phiên bản'),
('social_buttons','google,facebook,apple','Nút social login hiển thị'),
('event_banner','','Banner sự kiện trên login (ảnh, link)'),
('maintenance_mode','false','Chế độ bảo trì (chặn đăng nhập)'),
('maintenance_msg','Hệ thống đang bảo trì, vui lòng quay lại sau.','Thông báo bảo trì');

-- ═════════════════════════════════════════════════════════════
-- SERVER SELECTION — Chọn server + channel khi vào game
-- ═════════════════════════════════════════════════════════════

-- Mở rộng game_servers
ALTER TABLE game_servers
    ADD COLUMN IF NOT EXISTS group_name    VARCHAR(32) DEFAULT 'Mặc Định',  -- nhóm server (VN, SEA, Global)
    ADD COLUMN IF NOT EXISTS online_count  INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS load_status   TINYINT NOT NULL DEFAULT 0,      -- 0=smooth,1=normal,2=busy,3=full
    ADD COLUMN IF NOT EXISTS is_new        TINYINT NOT NULL DEFAULT 0,      -- badge "Mới"
    ADD COLUMN IF NOT EXISTS is_recommend  TINYINT NOT NULL DEFAULT 0,      -- badge "Đề xuất"
    ADD COLUMN IF NOT EXISTS is_hot        TINYINT NOT NULL DEFAULT 0,      -- badge "Hot"
    ADD COLUMN IF NOT EXISTS sort_order    INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS icon_url      VARCHAR(128) DEFAULT '';

-- Channels trong mỗi server (giảm tải, giống NRO)
CREATE TABLE IF NOT EXISTS server_channels (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    server_id       INT NOT NULL,
    channel_number  INT NOT NULL,                    -- 1,2,3...
    channel_name    VARCHAR(32) NOT NULL,             -- "Kênh 1", "Kênh 2"
    max_players     INT NOT NULL DEFAULT 200,
    online_count    INT NOT NULL DEFAULT 0,
    load_status     TINYINT NOT NULL DEFAULT 0,      -- 0=smooth,1=normal,2=busy,3=full
    is_active       TINYINT NOT NULL DEFAULT 1,
    UNIQUE KEY (server_id, channel_number)
);

-- Seed channels cho server 1
INSERT IGNORE INTO server_channels (server_id,channel_number,channel_name,max_players) VALUES
(1,1,'Kenh 1',200),(1,2,'Kenh 2',200),(1,3,'Kenh 3',200),
(1,4,'Kenh 4',200),(1,5,'Kenh 5',200);

-- Server mà player gần nhất đã chơi (auto-select)
ALTER TABLE accounts
    ADD COLUMN IF NOT EXISTS last_server_id  INT DEFAULT 1,
    ADD COLUMN IF NOT EXISTS last_channel_id INT DEFAULT 1;

-- Server transfer history
CREATE TABLE IF NOT EXISTS server_transfers (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    char_id         BIGINT NOT NULL,
    from_server     INT NOT NULL,
    to_server       INT NOT NULL,
    transfer_type   VARCHAR(16) NOT NULL DEFAULT 'manual', -- manual,merge,event
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ═════════════════════════════════════════════════════════════
-- SERVER MANAGEMENT — Tạo, quản lý, bảo trì, gộp server
-- ═════════════════════════════════════════════════════════════

-- Lịch sử gộp server
CREATE TABLE IF NOT EXISTS server_merges (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    source_server   INT NOT NULL,                    -- server bị gộp
    target_server   INT NOT NULL,                    -- server nhận
    merge_status    VARCHAR(16) NOT NULL DEFAULT 'pending', -- pending,processing,completed,failed
    chars_moved     INT NOT NULL DEFAULT 0,
    accounts_moved  INT NOT NULL DEFAULT 0,
    conflicts       INT NOT NULL DEFAULT 0,          -- trùng tên nhân vật
    conflict_log    TEXT,                             -- JSON chi tiết conflicts
    started_at      TIMESTAMP NULL,
    completed_at    TIMESTAMP NULL,
    created_by      VARCHAR(64) DEFAULT 'admin',
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Conflict resolution khi gộp (trùng tên)
CREATE TABLE IF NOT EXISTS merge_name_conflicts (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    merge_id        INT NOT NULL,
    char_id         BIGINT NOT NULL,
    old_name        VARCHAR(64) NOT NULL,
    new_name        VARCHAR(64) DEFAULT '',           -- tên mới (thêm suffix server cũ)
    resolved        TINYINT NOT NULL DEFAULT 0,
    INDEX idx_merge (merge_id)
);

-- Server monitoring realtime
CREATE TABLE IF NOT EXISTS server_monitor (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    server_id       INT NOT NULL,
    cpu_usage       FLOAT NOT NULL DEFAULT 0,
    ram_usage_mb    INT NOT NULL DEFAULT 0,
    ram_total_mb    INT NOT NULL DEFAULT 0,
    network_in_kb   INT NOT NULL DEFAULT 0,
    network_out_kb  INT NOT NULL DEFAULT 0,
    online_players  INT NOT NULL DEFAULT 0,
    tps             FLOAT NOT NULL DEFAULT 20.0,      -- ticks per second
    uptime_hours    FLOAT NOT NULL DEFAULT 0,
    recorded_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_server_time (server_id, recorded_at)
);

-- Maintenance window log
ALTER TABLE maintenance_schedule
    ADD COLUMN IF NOT EXISTS affected_servers VARCHAR(64) DEFAULT 'all', -- 'all' hoặc '1,2,3'
    ADD COLUMN IF NOT EXISTS notify_before_min INT NOT NULL DEFAULT 30,
    ADD COLUMN IF NOT EXISTS auto_kick TINYINT NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS kick_message VARCHAR(256) DEFAULT 'He thong sap bao tri, vui long dang xuat.',
    ADD COLUMN IF NOT EXISTS completed_at TIMESTAMP NULL;

-- ═════════════════════════════════════════════════════════════
-- IN-GAME TOPUP UI — Gói nạp hiển thị trong game
-- ═════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS topup_packages (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    package_key     VARCHAR(32) NOT NULL UNIQUE,
    display_name    VARCHAR(64) NOT NULL,
    description     VARCHAR(128) DEFAULT '',
    -- Giá tiền thật
    price_vnd       INT NOT NULL DEFAULT 0,
    price_usd       FLOAT NOT NULL DEFAULT 0,
    -- Nhận được
    diamond_base    INT NOT NULL DEFAULT 0,           -- diamond cơ bản
    diamond_bonus   INT NOT NULL DEFAULT 0,           -- diamond bonus (lần đầu x2, hoặc khuyến mãi)
    bonus_type      VARCHAR(16) DEFAULT 'first',      -- first=lần đầu, always=luôn có, event=sự kiện, none
    -- Hiển thị
    icon_asset      VARCHAR(128) DEFAULT '',
    badge           VARCHAR(16) DEFAULT '',            -- hot,best,new,limited,x2
    bg_color        VARCHAR(8) DEFAULT '#1a1a2e',
    border_color    VARCHAR(8) DEFAULT '#e94560',
    sort_order      INT NOT NULL DEFAULT 0,
    is_active       TINYINT NOT NULL DEFAULT 1,
    -- Khuyến mãi
    promo_start     TIMESTAMP NULL,
    promo_end       TIMESTAMP NULL,
    promo_bonus_pct INT NOT NULL DEFAULT 0             -- % bonus thêm khi khuyến mãi
);

INSERT IGNORE INTO topup_packages (id,package_key,display_name,price_vnd,price_usd,diamond_base,diamond_bonus,bonus_type,badge,sort_order) VALUES
(1,'pack_10k','Gói Khởi Đầu',10000,0.40,10,10,'first','',1),
(2,'pack_20k','Gói Tiết Kiệm',20000,0.80,22,22,'first','',2),
(3,'pack_50k','Gói Phổ Biến',50000,2.00,60,60,'first','hot',3),
(4,'pack_100k','Gói Giá Trị',100000,4.00,130,130,'first','best',4),
(5,'pack_200k','Gói Cao Cấp',200000,8.00,280,280,'first','',5),
(6,'pack_500k','Gói VIP',500000,20.00,750,750,'first','x2',6),
(7,'pack_1m','Gói Đại Gia',1000000,40.00,1600,1600,'first','limited',7),
(8,'pack_2m','Gói Thần Thoại',2000000,80.00,3500,3500,'first','',8);

-- Lịch sử nạp per package (track lần đầu để tính bonus)
CREATE TABLE IF NOT EXISTS topup_purchase_log (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id      BIGINT NOT NULL,
    char_id         BIGINT NOT NULL,
    package_id      INT NOT NULL,
    price_vnd       INT NOT NULL,
    diamond_received INT NOT NULL,
    is_first_buy    TINYINT NOT NULL DEFAULT 0,
    payment_method  VARCHAR(16) DEFAULT 'sepay',      -- sepay,google_play,app_store,momo
    transaction_id  VARCHAR(128) DEFAULT '',
    status          VARCHAR(16) NOT NULL DEFAULT 'completed',
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_account (account_id),
    INDEX idx_char (char_id)
);


('viettel','Viettel','10000,20000,50000,100000,200000,500000',0.001,20),
('mobifone','Mobifone','10000,20000,50000,100000,200000,500000',0.001,25),
('vinaphone','Vinaphone','10000,20000,50000,100000,200000,500000',0.001,25);

-- ═════════════════════════════════════════════════════════════
-- DOWNLOAD LINKS + SOCIAL LINKS + NEWS — Admin quản lý
-- ═════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS download_links (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    platform        VARCHAR(16) NOT NULL UNIQUE,      -- appstore,googleplay,pc,java,apk
    display_name    VARCHAR(32) NOT NULL,
    url             VARCHAR(256) NOT NULL DEFAULT '#',
    version         VARCHAR(16) NOT NULL DEFAULT '1.0.0',
    file_size       VARCHAR(16) DEFAULT '',
    is_active       TINYINT NOT NULL DEFAULT 1,
    sort_order      INT NOT NULL DEFAULT 0
);

INSERT IGNORE INTO download_links (platform,display_name,url,sort_order) VALUES
('appstore','App Store','#',1),
('googleplay','Google Play','#',2),
('pc','PC','#',3),
('java','Java','#',4),
('apk','APK','#',5);

CREATE TABLE IF NOT EXISTS social_links (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    platform        VARCHAR(16) NOT NULL UNIQUE,      -- facebook,zalo,discord,telegram,youtube,tiktok
    display_name    VARCHAR(32) NOT NULL,
    url             VARCHAR(256) NOT NULL DEFAULT '#',
    description     VARCHAR(64) DEFAULT '',
    is_active       TINYINT NOT NULL DEFAULT 1,
    sort_order      INT NOT NULL DEFAULT 0
);

INSERT IGNORE INTO social_links (platform,display_name,url,description,sort_order) VALUES
('facebook','Facebook','https://facebook.com/NexusIsekai','Fanpage chinh thuc',1),
('zalo','Zalo','https://zalo.me/NexusIsekai','Nhom cong dong',2),
('discord','Discord','https://discord.gg/NexusIsekai','Server Discord',3),
('telegram','Telegram','https://t.me/NexusIsekai','Kenh Telegram',4),
('youtube','YouTube','https://youtube.com/@NexusIsekai','Kenh YouTube',5),
('tiktok','TikTok','https://tiktok.com/@NexusIsekai','TikTok chinh thuc',6);

CREATE TABLE IF NOT EXISTS news_articles (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    title           VARCHAR(128) NOT NULL,
    category        VARCHAR(16) NOT NULL DEFAULT 'event', -- event,update,pvp,gacha,maintenance,notice
    summary         VARCHAR(256) DEFAULT '',
    content         TEXT NOT NULL,
    image_url       VARCHAR(256) DEFAULT '',
    is_pinned       TINYINT NOT NULL DEFAULT 0,
    is_published    TINYINT NOT NULL DEFAULT 0,
    published_at    TIMESTAMP NULL,
    created_by      VARCHAR(64) DEFAULT 'admin',
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

INSERT IGNORE INTO news_articles (id,title,category,summary,content,is_published,published_at) VALUES
(1,'Khai mo Nexus Isekai','event','Chao mung cac Luu Dan den voi Vong Linh Gioi!','Chao mung cac Luu Dan den voi Vong Linh Gioi! Game chinh thuc ra mat.',1,NOW()),
(2,'Mua PvP 1 bat dau','pvp','Mua PvP dau tien chinh thuc bat dau.','Chien dau de gianh skin doc quyen mua 1!',1,NOW()),
(3,'Banner Trieu Hoi Gioi Han','gacha','Ti le SSR tang gap doi!','Banner gioi han voi ti le SSR tang gap doi trong 7 ngay.',1,NOW());

-- ═════════════════════════════════════════════════════════════
-- MULTI-SERVER SUPPORT — Gift code, topup, shop, BXH per server
-- ═════════════════════════════════════════════════════════════

-- Gift code: thêm server filter
ALTER TABLE gift_codes ADD COLUMN IF NOT EXISTS server_ids VARCHAR(64) NOT NULL DEFAULT 'all';
-- 'all' = tat ca server, '1,2,3' = chi sv 1,2,3

-- Topup packages: per server
ALTER TABLE topup_packages ADD COLUMN IF NOT EXISTS server_ids VARCHAR(64) NOT NULL DEFAULT 'all';

-- Shop items: per server
ALTER TABLE shop_items ADD COLUMN IF NOT EXISTS server_ids VARCHAR(64) NOT NULL DEFAULT 'all';

-- Leaderboard cache: per server
ALTER TABLE leaderboard_cache ADD COLUMN IF NOT EXISTS server_id INT NOT NULL DEFAULT 0;
-- 0 = all servers combined, 1,2,3... = specific server
ALTER TABLE leaderboard_cache DROP PRIMARY KEY;
ALTER TABLE leaderboard_cache ADD PRIMARY KEY (rank_type, server_id, rank_pos);

-- Server copy log
CREATE TABLE IF NOT EXISTS server_copy_log (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    source_server   INT NOT NULL,
    target_server   INT NOT NULL,
    copy_type       VARCHAR(32) NOT NULL,            -- topup_packages,shop_items,gift_codes
    items_copied    INT NOT NULL DEFAULT 0,
    created_by      VARCHAR(64) DEFAULT 'admin',
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ═════════════════════════════════════════════════════════════
-- TOPUP PER CHARACTER — Nạp theo nhân vật, không phải account
-- ═════════════════════════════════════════════════════════════

-- Diamond lưu trong characters table (đã có gold, thêm diamond)
ALTER TABLE characters ADD COLUMN IF NOT EXISTS diamond BIGINT NOT NULL DEFAULT 0;
ALTER TABLE characters ADD COLUMN IF NOT EXISTS total_topup BIGINT NOT NULL DEFAULT 0; -- tổng nạp VND
ALTER TABLE characters ADD COLUMN IF NOT EXISTS vip_level INT NOT NULL DEFAULT 0;

-- topup_purchase_log đã có char_id — diamond cộng vào char_id cụ thể
-- shop purchase cũng theo char_id
-- gift code redeem theo char_id

-- ═════════════════════════════════════════════════════════════
-- PER-CHARACTER DATA — Mỗi nhân vật dữ liệu riêng biệt
-- ═════════════════════════════════════════════════════════════

-- Giới hạn 3 nhân vật / account / server
-- Check trong CharHandler khi tạo:
--   SELECT COUNT(*) FROM characters c
--   JOIN accounts a ON a.id=c.account_id
--   WHERE a.id=? AND a.last_server_id=?
-- Nếu >= 3 → từ chối tạo

-- Gift code: redeem per char_id (không phải account)
ALTER TABLE gift_code_usage ADD COLUMN IF NOT EXISTS char_id BIGINT NOT NULL DEFAULT 0;
-- Mỗi nhân vật nhập code 1 lần, không phải mỗi account
-- VD: Account có 3 char → cả 3 đều nhập được cùng 1 code (nếu code cho phép)

-- Đảm bảo mỗi NHÂN VẬT chỉ nhập 1 lần (không phải mỗi account)
ALTER TABLE gift_code_usage DROP INDEX IF EXISTS idx_unique_usage;
-- Unique: code_id + char_id (thay vì code_id + account_id)
-- ALTER TABLE gift_code_usage ADD UNIQUE KEY uq_code_char (code_id, char_id);

-- Gift code config: cho phép nhập theo account hay character
ALTER TABLE gift_codes ADD COLUMN IF NOT EXISTS redeem_per VARCHAR(8) NOT NULL DEFAULT 'char';
-- 'char' = mỗi nhân vật nhập 1 lần (3 char = 3 lần)
-- 'account' = mỗi account nhập 1 lần (dù có 3 char)

-- Max characters per server
INSERT IGNORE INTO protection_config VALUES
('max_chars_per_server','3','So nhan vat toi da moi account moi server');

-- ═════════════════════════════════════════════════════════════
-- ALL PER-CHARACTER — Mọi thứ theo nhân vật, không theo account
-- ═════════════════════════════════════════════════════════════

-- Topup: diamond vào char_id cụ thể (đã có)
-- Shop: mua item vào char_id cụ thể (đã có)
-- Gift code: redeem vào char_id (đã có)
-- Mission pass: progress per char_id
ALTER TABLE player_passes ADD COLUMN IF NOT EXISTS char_id BIGINT NOT NULL DEFAULT 0;
-- Gacha pity: đã per char_id ✓
-- PvP season: đã per char_id ✓
-- Achievement: đã per char_id ✓
-- Daily login: per char_id
ALTER TABLE daily_login_status ADD COLUMN IF NOT EXISTS char_id BIGINT NOT NULL DEFAULT 0;
-- Tutorial: per char_id (đã có)
-- Settings: per char_id (đã có)
-- Mail: per char_id (đã có)

-- Xoá redeem_per vì luôn là per character
-- ALTER TABLE gift_codes DROP COLUMN IF EXISTS redeem_per;
-- Mọi gift code đều nhập PER CHARACTER: mỗi char nhập 1 lần

-- VIP level: per character (đã thêm ở trên)
-- Mỗi nhân vật nạp riêng → VIP riêng

-- NGUYÊN TẮC: Account chỉ là login container
-- Mọi dữ liệu game = per character:
--   diamond, gold, inventory, equipment, quest, achievement,
--   guild, friends, pvp, gacha, daily login, mission pass,
--   settings, mail, tutorial, vip, topup history, gift code usage

-- ═════════════════════════════════════════════════════════════
-- AUTO COMBAT CONFIG
-- ═════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS player_auto_config (
    char_id             BIGINT NOT NULL PRIMARY KEY,
    auto_enabled        TINYINT NOT NULL DEFAULT 0,
    -- Target priority (1=highest)
    priority_boss       INT NOT NULL DEFAULT 1,
    priority_elite      INT NOT NULL DEFAULT 2,
    priority_normal     INT NOT NULL DEFAULT 3,
    priority_player     INT NOT NULL DEFAULT 0,      -- 0=off
    -- Range
    auto_range          FLOAT NOT NULL DEFAULT 8.0,
    -- Skills
    auto_skill_enabled  TINYINT NOT NULL DEFAULT 1,
    skill_order         VARCHAR(64) DEFAULT '1,2,3,4,5,6,7', -- skill slot order
    use_skill_on_cd     TINYINT NOT NULL DEFAULT 1,
    -- Loot
    auto_loot           TINYINT NOT NULL DEFAULT 1,
    loot_rare_only      TINYINT NOT NULL DEFAULT 0,
    loot_equip_only     TINYINT NOT NULL DEFAULT 0,
    -- Potion
    auto_hp_potion      TINYINT NOT NULL DEFAULT 1,
    hp_threshold        INT NOT NULL DEFAULT 50,      -- % HP de uong thuoc
    auto_mp_potion      TINYINT NOT NULL DEFAULT 1,
    mp_threshold        INT NOT NULL DEFAULT 30,
    -- Movement
    auto_return          TINYINT NOT NULL DEFAULT 1,   -- quay lai vi tri goc
    patrol_radius       FLOAT NOT NULL DEFAULT 10.0,
    -- Safety
    auto_revive         TINYINT NOT NULL DEFAULT 0,
    flee_hp_pct         INT NOT NULL DEFAULT 10        -- chay khi HP < X%
);

-- ═════════════════════════════════════════════════════════════
-- STICKER / EMOJI IN CHAT (rieng voi bieu cam nhan vat)
-- ═════════════════════════════════════════════════════════════









-- ═════════════════════════════════════════════════════════════
-- CHARACTER EXPRESSIONS + ACTIONS + PAIR ACTIONS
-- ═════════════════════════════════════════════════════════════

-- Bieu cam nhan vat (hien tren dau)
CREATE TABLE IF NOT EXISTS character_expressions (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    expr_key        VARCHAR(32) NOT NULL UNIQUE,
    display_name_vi VARCHAR(32) NOT NULL,
    display_name_en VARCHAR(32) NOT NULL,
    icon_path       VARCHAR(128) DEFAULT '',
    animation_key   VARCHAR(32) DEFAULT '',           -- Spine animation key
    duration        FLOAT NOT NULL DEFAULT 2.0,
    is_free         TINYINT NOT NULL DEFAULT 1,
    sort_order      INT NOT NULL DEFAULT 0
);

INSERT IGNORE INTO character_expressions (expr_key,display_name_vi,display_name_en,is_free,sort_order) VALUES
('happy','Vui','Happy',1,1),
('sad','Buon','Sad',1,2),
('angry','Gian','Angry',1,3),
('love','Yeu','Love',1,4),
('laugh','Cuoi','Laugh',1,5),
('cry','Khoc','Cry',1,6),
('shock','Soc','Shocked',1,7),
('sleep','Ngu','Sleep',1,8),
('think','Suy nghi','Thinking',1,9),
('cool','Cool','Cool',1,10);

-- Hanh dong nhan vat (animation toan than)
CREATE TABLE IF NOT EXISTS character_actions (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    action_key      VARCHAR(32) NOT NULL UNIQUE,
    display_name_vi VARCHAR(32) NOT NULL,
    display_name_en VARCHAR(32) NOT NULL,
    animation_key   VARCHAR(32) DEFAULT '',
    duration        FLOAT NOT NULL DEFAULT 3.0,
    is_pair         TINYINT NOT NULL DEFAULT 0,       -- 0=solo, 1=can nguoi thu 2
    pair_anim_key   VARCHAR(32) DEFAULT '',            -- anim nguoi thu 2
    is_free         TINYINT NOT NULL DEFAULT 1,
    sort_order      INT NOT NULL DEFAULT 0
);

INSERT IGNORE INTO character_actions (action_key,display_name_vi,display_name_en,is_pair,is_free,sort_order) VALUES
-- Solo actions
('sit','Ngoi','Sit',0,1,1),
('wave','Vay tay','Wave',0,1,2),
('dance','Nhay','Dance',0,1,3),
('bow','Cui chao','Bow',0,1,4),
('cheer','Co vu','Cheer',0,1,5),
('meditate','Ngoi thien','Meditate',0,1,6),
('flex','Khoe co','Flex',0,1,7),
('facepalm','Che mat','Facepalm',0,1,8),
-- Pair actions (can nguoi thu 2 dong y)
('hug','Om','Hug',1,1,10),
('highfive','Dap tay','High Five',1,1,11),
('handshake','Bat tay','Handshake',1,1,12),
('piggyback','Cong','Piggyback',1,0,13),
('dance_pair','Nhay doi','Dance Together',1,0,14),
('arm_wrestle','Van tay','Arm Wrestle',1,1,15);

-- ═════════════════════════════════════════════════════════════
-- PLAYER INTERACTION MENU — Cham vao nguoi choi khac
-- ═════════════════════════════════════════════════════════════

-- Khi tap vao player khac, hien menu:
-- Config menu items (admin co the bat/tat)
CREATE TABLE IF NOT EXISTS player_interact_menu (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    menu_key        VARCHAR(32) NOT NULL UNIQUE,
    display_name_vi VARCHAR(32) NOT NULL,
    display_name_en VARCHAR(32) NOT NULL,
    icon_path       VARCHAR(128) DEFAULT '',
    sort_order      INT NOT NULL DEFAULT 0,
    is_active       TINYINT NOT NULL DEFAULT 1,
    require_friend  TINYINT NOT NULL DEFAULT 0,       -- chi hien khi la ban be
    require_guild   TINYINT NOT NULL DEFAULT 0        -- chi hien khi cung guild
);

INSERT IGNORE INTO player_interact_menu (menu_key,display_name_vi,display_name_en,sort_order) VALUES
('inspect','Xem thong tin','Inspect',1),
('trade','Giao dich','Trade',2),
('add_friend','Ket ban','Add Friend',3),
('invite_party','Moi vao nhom','Invite to Party',4),
('invite_guild','Moi vao guild','Invite to Guild',5),
('duel','Thach dau','Duel',6),
('whisper','Nhan tin','Whisper',7),
('follow','Theo doi','Follow',8),
('action','Hanh dong','Action',9),
('block','Chan','Block',10),
('report','Bao cao','Report',11);

-- Daily login rewards seed
INSERT IGNORE INTO daily_login_rewards (day_number, reward_type, reward_amount) VALUES
(1,'gold',5000),(2,'diamond',10),(3,'gold',10000),(4,'ticket_standard',1),
(5,'diamond',20),(6,'gold',20000),(7,'ticket_limited',1)
ON DUPLICATE KEY UPDATE reward_amount=VALUES(reward_amount);

-- ═════════════════════════════════════════════════════════════
-- TABLES cho handler logic mới
-- ═════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS character_blocks (
    char_id BIGINT NOT NULL, blocked_char_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (char_id, blocked_char_id)
);



CREATE TABLE IF NOT EXISTS character_tutorial (
    char_id BIGINT NOT NULL PRIMARY KEY,
    current_step VARCHAR(32) DEFAULT 'welcome', completed TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS character_settings (
    char_id BIGINT NOT NULL PRIMARY KEY,
    language VARCHAR(8) DEFAULT 'vi', settings_json TEXT
);

CREATE TABLE IF NOT EXISTS character_warehouse (
    char_id BIGINT NOT NULL, item_id INT NOT NULL, quantity INT NOT NULL DEFAULT 0,
    PRIMARY KEY (char_id, item_id)
);







CREATE TABLE IF NOT EXISTS character_mail (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    char_id BIGINT NOT NULL, subject VARCHAR(128), body TEXT,
    reward_type VARCHAR(16), reward_amount INT DEFAULT 0,
    is_read TINYINT NOT NULL DEFAULT 0, claimed TINYINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, INDEX idx_char (char_id)
);



CREATE TABLE IF NOT EXISTS character_achievements (
    char_id BIGINT NOT NULL, achievement_id INT NOT NULL,
    progress INT NOT NULL DEFAULT 0, completed TINYINT NOT NULL DEFAULT 0, claimed TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (char_id, achievement_id)
);

ALTER TABLE player_auto_config ADD COLUMN IF NOT EXISTS config_json TEXT;
ALTER TABLE character_equipment ADD COLUMN IF NOT EXISTS refine_level INT NOT NULL DEFAULT 0;
ALTER TABLE characters ADD COLUMN IF NOT EXISTS guild_id INT DEFAULT NULL;
ALTER TABLE characters ADD COLUMN IF NOT EXISTS guild_rank VARCHAR(16) DEFAULT NULL;

-- ═════════════════════════════════════════════════════════════
-- INTRO VIDEO CONFIG — Video intro sau khi tạo nhân vật lần đầu
-- ═════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS intro_video_config (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    is_enabled      TINYINT NOT NULL DEFAULT 1,       -- bật/tắt video intro
    video_url       VARCHAR(256) NOT NULL,            -- URL MP4 (CDN) hoặc StreamingAssets path
    video_url_low   VARCHAR(256) DEFAULT '',          -- bản chất lượng thấp cho mạng yếu
    skippable       TINYINT NOT NULL DEFAULT 1,       -- cho phép skip
    skip_after_sec  INT NOT NULL DEFAULT 3,           -- hiện nút skip sau N giây
    show_once       TINYINT NOT NULL DEFAULT 1,       -- chỉ chiếu lần đầu (per account)
    fallback_to_text TINYINT NOT NULL DEFAULT 1,      -- máy không phát được video → dùng intro text
    duration_sec    INT NOT NULL DEFAULT 60,          -- độ dài video (cho progress bar)
    bgm_during      VARCHAR(64) DEFAULT '',           -- nhạc nền (nếu video không có audio)
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

INSERT IGNORE INTO intro_video_config (id, is_enabled, video_url, skippable, skip_after_sec, show_once, fallback_to_text)
VALUES (1, 1, 'Intro/intro_cinematic.mp4', 1, 3, 1, 1);

-- player_intro đã track watched per account (dùng chung cho video + text)

-- ═════════════════════════════════════════════════════════════
-- 1. CÁNH / HÀO QUANG (Wing / Aura) — cosmetic + chỉ số
-- ═════════════════════════════════════════════════════════════
CREATE TABLE IF NOT EXISTS cosmetic_templates (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(64) NOT NULL,
    cosmetic_type VARCHAR(16) NOT NULL DEFAULT 'wing',  -- 'wing','aura','halo'
    rarity        TINYINT NOT NULL DEFAULT 1,            -- 1=common..5=legendary
    stat_bonus    VARCHAR(256) DEFAULT '',               -- JSON {"hp":500,"atk":50,"def":30}
    effect_id     INT NOT NULL DEFAULT 0,                -- hiệu ứng visual
    icon_id       INT NOT NULL DEFAULT 0,
    sprite_id     INT NOT NULL DEFAULT 0,                -- sprite cánh/hào quang
    max_level     INT NOT NULL DEFAULT 10,               -- nâng cấp tăng chỉ số
    upgrade_cost  VARCHAR(256) DEFAULT '',               -- JSON cost mỗi cấp
    obtain_source VARCHAR(64),
    is_active     TINYINT NOT NULL DEFAULT 1
);
CREATE TABLE IF NOT EXISTS player_cosmetics (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    char_id       BIGINT NOT NULL,
    template_id   INT NOT NULL,
    level         INT NOT NULL DEFAULT 1,
    is_equipped   TINYINT NOT NULL DEFAULT 0,
    obtained_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_char_tpl (char_id, template_id),
    INDEX idx_char (char_id)
);

-- ═════════════════════════════════════════════════════════════
-- 2. DANH VỌNG / PHE PHÁI (Reputation / Faction)
-- ═════════════════════════════════════════════════════════════
CREATE TABLE IF NOT EXISTS factions (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(64) NOT NULL,
    description   TEXT,
    icon_id       INT NOT NULL DEFAULT 0,
    max_rep       INT NOT NULL DEFAULT 30000,
    is_active     TINYINT NOT NULL DEFAULT 1
);
CREATE TABLE IF NOT EXISTS faction_rep_tiers (    -- mốc danh vọng
    id            INT AUTO_INCREMENT PRIMARY KEY,
    faction_id    INT NOT NULL,
    tier_order    INT NOT NULL,                    -- 1=Lạ mặt,2=Thân thiện,3=Tôn kính,4=Sùng bái
    tier_name     VARCHAR(32) NOT NULL,
    rep_required  INT NOT NULL,
    reward_json   VARCHAR(256) DEFAULT '',         -- quà khi đạt mốc
    unlock_shop   TINYINT NOT NULL DEFAULT 0,      -- mở shop phe phái
    INDEX idx_faction (faction_id)
);
CREATE TABLE IF NOT EXISTS player_reputation (
    char_id       BIGINT NOT NULL,
    faction_id    INT NOT NULL,
    reputation    INT NOT NULL DEFAULT 0,
    current_tier  INT NOT NULL DEFAULT 1,
    PRIMARY KEY (char_id, faction_id)
);

-- ═════════════════════════════════════════════════════════════
-- 3. BESTIARY / SỔ TAY QUÁI VẬT (backend)
-- ═════════════════════════════════════════════════════════════
CREATE TABLE IF NOT EXISTS bestiary_entries (     -- mục sổ tay (gắn với monster)
    monster_id    INT NOT NULL PRIMARY KEY,
    lore_text     TEXT,                            -- mô tả/cốt truyện quái
    weakness      VARCHAR(64) DEFAULT '',          -- hệ khắc chế
    kills_to_unlock INT NOT NULL DEFAULT 10,       -- số lần giết để mở full info
    reward_json   VARCHAR(256) DEFAULT '',         -- quà khi mở khoá
    is_boss       TINYINT NOT NULL DEFAULT 0
);
CREATE TABLE IF NOT EXISTS character_bestiary (   -- tiến độ sưu tập per char
    char_id       BIGINT NOT NULL,
    monster_id    INT NOT NULL,
    kill_count    INT NOT NULL DEFAULT 0,
    is_unlocked   TINYINT NOT NULL DEFAULT 0,      -- đã đạt kills_to_unlock
    reward_claimed TINYINT NOT NULL DEFAULT 0,
    first_killed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (char_id, monster_id),
    INDEX idx_char (char_id)
);

-- ═════════════════════════════════════════════════════════════
-- 4. BỘ TRANG BỊ / SET BONUS
-- ═════════════════════════════════════════════════════════════
CREATE TABLE IF NOT EXISTS equipment_sets (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(64) NOT NULL,
    description   TEXT,
    rarity        TINYINT NOT NULL DEFAULT 3,
    is_active     TINYINT NOT NULL DEFAULT 1
);
CREATE TABLE IF NOT EXISTS equipment_set_items (  -- item nào thuộc set nào
    set_id        INT NOT NULL,
    item_id       INT NOT NULL,
    PRIMARY KEY (set_id, item_id),
    INDEX idx_item (item_id)
);
CREATE TABLE IF NOT EXISTS equipment_set_bonuses ( -- bonus theo số mảnh
    id            INT AUTO_INCREMENT PRIMARY KEY,
    set_id        INT NOT NULL,
    pieces_required INT NOT NULL,                  -- 2/4/6 mảnh
    stat_bonus    VARCHAR(256) NOT NULL,           -- JSON {"atk":100,"crit":5}
    effect_desc   VARCHAR(128) DEFAULT '',
    INDEX idx_set (set_id)
);

-- Seed mẫu
INSERT IGNORE INTO factions (id, name, description) VALUES
 (1,'Liên Minh Khải Nguyên','Phe phòng thủ vết nứt, bảo vệ Vọng Linh Giới'),
 (2,'Hội Thợ Săn Linh Thú','Phe săn bắt và thuần hoá quái vật'),
 (3,'Giáo Đoàn Ánh Sáng','Phe tu luyện, đối kháng Giáo Phái Vọng Linh');
INSERT IGNORE INTO faction_rep_tiers (faction_id, tier_order, tier_name, rep_required) VALUES
 (1,1,'Lạ Mặt',0),(1,2,'Thân Thiện',3000),(1,3,'Tôn Kính',9000),(1,4,'Sùng Bái',21000);

-- ═════════════════════════════════════════════════════════════
-- FACILITY MAPS + CỔNG DỊCH CHUYỂN (instanced)
-- Map riêng cho: guild, lễ đường, nhà/trồng cây/nuôi thú,
-- đấu trường, minigame, world boss, tân thủ.
-- Nằm "trên" map thường, vào qua cổng dịch chuyển.
-- ═════════════════════════════════════════════════════════════

ALTER TABLE maps ADD COLUMN IF NOT EXISTS map_category   VARCHAR(16) NOT NULL DEFAULT 'normal';
  -- normal,guild,wedding,housing,farm,arena,minigame,worldboss,tutorial,dungeon
ALTER TABLE maps ADD COLUMN IF NOT EXISTS instance_scope VARCHAR(16) NOT NULL DEFAULT 'static';
  -- static (chung), personal (theo char), guild (theo bang), party (theo tổ đội), room (theo phòng)
ALTER TABLE maps ADD COLUMN IF NOT EXISTS return_map_id  INT NOT NULL DEFAULT 0;   -- map quay về khi rời
ALTER TABLE maps ADD COLUMN IF NOT EXISTS return_x       FLOAT NOT NULL DEFAULT 0;
ALTER TABLE maps ADD COLUMN IF NOT EXISTS return_y       FLOAT NOT NULL DEFAULT 0;

ALTER TABLE map_portals ADD COLUMN IF NOT EXISTS portal_type       VARCHAR(16) NOT NULL DEFAULT 'normal'; -- normal,facility
ALTER TABLE map_portals ADD COLUMN IF NOT EXISTS facility_category VARCHAR(16) NOT NULL DEFAULT '';       -- guild,wedding,housing,farm,arena,minigame...
ALTER TABLE map_portals ADD COLUMN IF NOT EXISTS label             VARCHAR(32) NOT NULL DEFAULT '';        -- tên hiển thị cổng
ALTER TABLE map_portals ADD COLUMN IF NOT EXISTS icon_id           INT NOT NULL DEFAULT 0;

-- Instance đang sống (tổng quát hoá dungeon_instances)
CREATE TABLE IF NOT EXISTS map_instances (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_map_id INT NOT NULL,                 -- map facility gốc
    owner_type      VARCHAR(16) NOT NULL,         -- char,guild,party,room,global
    owner_id        BIGINT NOT NULL,              -- id chủ instance (charId/guildId/partyId/roomId)
    status          VARCHAR(16) NOT NULL DEFAULT 'active',
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_active     TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_instance (template_map_id, owner_type, owner_id),
    INDEX idx_owner (owner_type, owner_id)
);

-- Seed các facility map mẫu (file_name = asset team art làm)
INSERT IGNORE INTO maps (id, name, file_name, map_category, instance_scope, is_safe, return_map_id, return_x, return_y) VALUES
 (200,'Lãnh Địa Bang Hội','guild_hall', 'guild',    'guild',    1, 1, 100, 100),
 (201,'Lễ Đường',         'wedding_hall','wedding',  'party',    1, 1, 120, 100),
 (202,'Tư Gia',           'house',      'housing',  'personal', 1, 1, 140, 100),
 (203,'Vườn Nhà',         'home_garden','farm',     'personal', 1, 1, 160, 100),
 (204,'Đấu Trường',       'arena',      'arena',    'room',     0, 1, 180, 100),
 (205,'Sảnh Tiểu Trò Chơi','minigame_hall','minigame','room',   1, 1, 200, 100),
 (206,'Thí Luyện Tân Thủ','tutorial_zone','tutorial','personal',1, 1, 100, 120);

-- Seed cổng dịch chuyển vào facility (đặt tại Làng Khải Nguyên map_id=1)
INSERT IGNORE INTO map_portals (map_id, pos_x, pos_y, portal_type, facility_category, label, level_req) VALUES
 (1, 300, 200, 'facility', 'guild',    'Lãnh Địa Bang', 1),
 (1, 340, 200, 'facility', 'wedding',  'Lễ Đường',      1),
 (1, 380, 200, 'facility', 'housing',  'Tư Gia',        1),
 (1, 420, 200, 'facility', 'farm',     'Nông Trại',     1),
 (1, 460, 200, 'facility', 'arena',    'Đấu Trường',    10),
 (1, 500, 200, 'facility', 'minigame', 'Tiểu Trò Chơi', 1);

-- ═════════════════════════════════════════════════════════════
-- TƯƠNG TÁC NỘI THẤT + FLOW VƯỜN → NHÀ
-- Về nhà → ra MAP VƯỜN (trồng trọt/nuôi thú) → có nhà + cổng → MAP NHÀ (nội thất)
-- ═════════════════════════════════════════════════════════════

-- Loại tương tác cho nội thất: ngồi/nằm/ăn/uống
ALTER TABLE furniture_catalog ADD COLUMN IF NOT EXISTS interaction_type VARCHAR(16) NOT NULL DEFAULT 'none';
  -- none, sit (ghế), lie (giường), eat (bàn ăn/thức ăn), drink (nước), bath, mirror
ALTER TABLE furniture_catalog ADD COLUMN IF NOT EXISTS interact_anim    VARCHAR(32) NOT NULL DEFAULT '';
  -- tên animation khi tương tác
ALTER TABLE furniture_catalog ADD COLUMN IF NOT EXISTS restore_hp       INT NOT NULL DEFAULT 0;  -- hồi HP khi dùng (ăn/uống/nằm)
ALTER TABLE furniture_catalog ADD COLUMN IF NOT EXISTS restore_mp       INT NOT NULL DEFAULT 0;
ALTER TABLE furniture_catalog ADD COLUMN IF NOT EXISTS buff_json        VARCHAR(256) DEFAULT '';  -- buff tạm khi dùng (vd ăn no +exp%)

-- Gán interaction cho nội thất mẫu đã seed
UPDATE furniture_catalog SET interaction_type='lie',  interact_anim='lie_down', restore_hp=50, restore_mp=30 WHERE furniture_type='bed';
UPDATE furniture_catalog SET interaction_type='sit',  interact_anim='sit'       WHERE furniture_type='chair';
UPDATE furniture_catalog SET interaction_type='eat',  interact_anim='eat',  restore_hp=30 WHERE furniture_type='table';

-- Thêm vài nội thất tương tác mẫu
INSERT IGNORE INTO furniture_catalog (id,name,furniture_type,width,height,gold_price,interaction_type,interact_anim,restore_hp,restore_mp) VALUES
 (50,'Bàn Ăn Đầy Đủ','table',2,1,8000,'eat','eat',60,20),
 (51,'Bình Nước Mát','decoration',1,1,2000,'drink','drink',10,40),
 (52,'Ghế Sofa Êm','chair',2,1,12000,'sit','sit',0,0),
 (53,'Giường Hoàng Gia','bed',3,2,50000,'lie','lie_down',120,80),
 (54,'Bồn Tắm Thư Giãn','decoration',2,2,30000,'bath','bath',80,80);

-- house_furniture: lưu trạng thái đang có người dùng (tránh 2 người cùng ngồi 1 ghế)
ALTER TABLE house_furniture ADD COLUMN IF NOT EXISTS occupied_by BIGINT NOT NULL DEFAULT 0; -- char_id đang dùng (0=trống)

-- house: thêm map vườn riêng (garden) — về nhà ra đây trước
ALTER TABLE houses ADD COLUMN IF NOT EXISTS garden_map_id INT NOT NULL DEFAULT 203;

-- Đổi cổng ở làng: "Về Nhà" → ra VƯỜN (farm/garden map 203), bỏ cổng housing trực tiếp
DELETE FROM map_portals WHERE map_id=1 AND facility_category IN ('housing','farm');
INSERT IGNORE INTO map_portals (map_id, pos_x, pos_y, portal_type, facility_category, label, level_req) VALUES
 (1, 380, 200, 'facility', 'farm', 'Về Nhà', 1);

-- Trên MAP VƯỜN (203) đặt cổng vào NHÀ (house interior 202) — kèm "ảnh ngôi nhà"
INSERT IGNORE INTO map_portals (map_id, pos_x, pos_y, portal_type, facility_category, label, level_req, icon_id) VALUES
 (203, 250, 150, 'facility', 'housing', 'Vào Nhà', 1, 1);

-- ═════════════════════════════════════════════════════════════
-- NHÀ 2 TẦNG: Vườn (về nhà tới đây) → cổng → Nội thất nhà
-- + Tương tác nội thất: ngồi ghế, nằm giường, ăn, uống
-- ═════════════════════════════════════════════════════════════

-- Vườn = "về nhà" tới đây (trồng trọt + nuôi thú + nhà ngoại thất + cổng vào trong)
UPDATE maps SET name='Vườn Nhà', file_name='home_garden', map_category='home', instance_scope='personal'
  WHERE id=203;
-- Nội thất nhà (vào từ cổng trong vườn)
UPDATE maps SET name='Nội Thất Nhà', file_name='house_interior', map_category='house_interior', instance_scope='personal'
  WHERE id=202;

-- Cổng "Về Nhà" tại Làng Khải Nguyên → Vườn (thay cổng farm/housing cũ)
UPDATE map_portals SET facility_category='home', label='Về Nhà' WHERE map_id=1 AND facility_category='farm';
DELETE FROM map_portals WHERE map_id=1 AND facility_category='housing';

-- Cổng trong Vườn (map 203) → Nội thất nhà
INSERT IGNORE INTO map_portals (map_id, pos_x, pos_y, portal_type, facility_category, label, level_req) VALUES
 (203, 250, 150, 'facility', 'house_interior', 'Vào Nhà', 1);

-- Tương tác nội thất
ALTER TABLE furniture_catalog ADD COLUMN IF NOT EXISTS interaction_type   VARCHAR(16) NOT NULL DEFAULT 'none'; -- sit,lie,eat,drink,none
ALTER TABLE furniture_catalog ADD COLUMN IF NOT EXISTS animation_state    VARCHAR(32) NOT NULL DEFAULT '';      -- sit_chair,lie_bed,eat,drink
ALTER TABLE furniture_catalog ADD COLUMN IF NOT EXISTS interaction_effect VARCHAR(256) NOT NULL DEFAULT '';     -- JSON {"hp_regen":50,"buff":"rested","duration":300}
ALTER TABLE furniture_catalog ADD COLUMN IF NOT EXISTS is_consumable      TINYINT NOT NULL DEFAULT 0;           -- ăn/uống có tốn không

-- Gán tương tác cho nội thất sẵn có
UPDATE furniture_catalog SET interaction_type='lie',  animation_state='lie_bed',   interaction_effect='{"hp_regen":80,"mp_regen":80,"buff":"rested","duration":600}' WHERE furniture_type='bed';
UPDATE furniture_catalog SET interaction_type='sit',  animation_state='sit_chair', interaction_effect='{"hp_regen":30,"mp_regen":30}' WHERE furniture_type='chair';
UPDATE furniture_catalog SET interaction_type='none', animation_state=''          WHERE furniture_type IN ('decoration','storage','table');

-- Đồ ăn/uống đặt trong nhà (ăn/uống được)
INSERT IGNORE INTO furniture_catalog (id,name,furniture_type,width,height,gold_price,interaction_type,animation_state,interaction_effect,is_consumable) VALUES
 (50,'Bàn Ăn Thịnh Soạn','table',2,2,8000,'eat','eat','{"hp_regen":150,"buff":"well_fed","duration":1800}',1),
 (51,'Bình Nước Mát','decoration',1,1,2000,'drink','drink','{"mp_regen":100,"buff":"refreshed","duration":900}',1),
 (52,'Lò Sưởi','decoration',2,1,12000,'none','','{"buff":"warm","duration":600}',0);

-- ═════════════════════════════════════════════════════════════
-- CON CÁI MỞ RỘNG: nhu cầu, shop riêng, bảo mẫu, NPC trong nhà
-- ═════════════════════════════════════════════════════════════

-- Nhu cầu con (giảm dần theo thời gian, đáp ứng bằng ăn/uống/tả)
ALTER TABLE children ADD COLUMN IF NOT EXISTS hunger       INT NOT NULL DEFAULT 100; -- đói (0-100)
ALTER TABLE children ADD COLUMN IF NOT EXISTS thirst       INT NOT NULL DEFAULT 100; -- khát
ALTER TABLE children ADD COLUMN IF NOT EXISTS cleanliness  INT NOT NULL DEFAULT 100; -- sạch (tả)
ALTER TABLE children ADD COLUMN IF NOT EXISTS fashion_head INT NOT NULL DEFAULT 0;   -- thời trang đầu
ALTER TABLE children ADD COLUMN IF NOT EXISTS fashion_body INT NOT NULL DEFAULT 0;   -- thời trang thân
ALTER TABLE children ADD COLUMN IF NOT EXISTS nanny_until  DATETIME DEFAULT NULL;     -- bảo mẫu chăm tới lúc này
ALTER TABLE children ADD COLUMN IF NOT EXISTS last_care    TIMESTAMP DEFAULT CURRENT_TIMESTAMP; -- lần chăm gần nhất

-- Shop con cái
CREATE TABLE IF NOT EXISTS child_shop_items (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(64) NOT NULL,
    category      VARCHAR(16) NOT NULL,         -- fashion,food,drink,diaper,nanny
    gold_price    INT NOT NULL DEFAULT 0,
    diamond_price INT NOT NULL DEFAULT 0,
    effect_json   VARCHAR(256) DEFAULT '',      -- {"hunger":40} / {"thirst":50} / {"cleanliness":100}
    fashion_slot  VARCHAR(8) DEFAULT '',        -- head,body (nếu category=fashion)
    fashion_id    INT NOT NULL DEFAULT 0,        -- sprite thời trang
    nanny_hours   INT NOT NULL DEFAULT 0,        -- số giờ bảo mẫu (nếu category=nanny)
    icon_id       INT NOT NULL DEFAULT 0,
    is_active     TINYINT NOT NULL DEFAULT 1
);

INSERT IGNORE INTO child_shop_items (id,name,category,gold_price,diamond_price,effect_json,fashion_slot,fashion_id,nanny_hours) VALUES
 (1,'Sữa Bột',        'food',  500,0,'{"hunger":40}','',0,0),
 (2,'Cháo Dinh Dưỡng','food',  1200,0,'{"hunger":70}','',0,0),
 (3,'Nước Trái Cây',  'drink', 400,0,'{"thirst":50}','',0,0),
 (4,'Tả Em Bé',       'diaper',300,0,'{"cleanliness":100}','',0,0),
 (5,'Mũ Thỏ Hồng',    'fashion',0,30,'','head',101,0),
 (6,'Bộ Đồ Phi Hành Gia','fashion',0,80,'','body',201,0),
 (7,'Bảo Mẫu 8 Giờ',  'nanny', 5000,0,'','',0,8),
 (8,'Bảo Mẫu 24 Giờ', 'nanny', 0,50,'','',0,24);

-- ═════════════════════════════════════════════════════════════
-- NÂNG CẤP NÔNG TRẠI (học cơ chế Avatar-Sv)
-- + Sức khoẻ cây ảnh hưởng sản lượng, bón phân
-- + Thú: sức khoẻ, sản xuất theo thời gian, sinh sản, chết nếu bỏ bê
-- ═════════════════════════════════════════════════════════════

-- Cây: sức khoẻ (sucKhoe) + bón phân
ALTER TABLE farm_plots ADD COLUMN IF NOT EXISTS health      INT NOT NULL DEFAULT 100; -- 0-100, ảnh hưởng sản lượng
ALTER TABLE farm_plots ADD COLUMN IF NOT EXISTS fertilized  TINYINT NOT NULL DEFAULT 0; -- đã bón phân
ALTER TABLE farm_plots ADD COLUMN IF NOT EXISTS last_water  TIMESTAMP NULL;             -- lần tưới gần nhất (để tính héo)

-- Phân bón giảm thời gian trưởng thành (% )
ALTER TABLE farm_seeds ADD COLUMN IF NOT EXISTS fertilizer_item_id INT NOT NULL DEFAULT 0;

-- Thú: sức khoẻ + chu kỳ sản xuất + sinh sản + sống/chết
ALTER TABLE animal_pens ADD COLUMN IF NOT EXISTS health         INT NOT NULL DEFAULT 100;
ALTER TABLE animal_pens ADD COLUMN IF NOT EXISTS is_alive       TINYINT NOT NULL DEFAULT 1;
ALTER TABLE animal_pens ADD COLUMN IF NOT EXISTS produce_ready_at DATETIME NULL;          -- khi nào có sản phẩm
ALTER TABLE animal_pens ADD COLUMN IF NOT EXISTS breed_ready    TINYINT NOT NULL DEFAULT 0; -- sẵn sàng sinh sản
ALTER TABLE animal_pens ADD COLUMN IF NOT EXISTS level          INT NOT NULL DEFAULT 1;     -- cấp thú (sản lượng tăng)
ALTER TABLE animal_pens ADD COLUMN IF NOT EXISTS last_fed       TIMESTAMP NULL;

-- Thú: thông tin sinh sản
ALTER TABLE farm_animals ADD COLUMN IF NOT EXISTS breed_time_min INT NOT NULL DEFAULT 720; -- phút để sẵn sàng sinh sản

-- Phân bón mẫu (item)


-- ═════════════════════════════════════════════════════════════
-- NHÀ KHO + BÁN SẢN PHẨM (nông sản, sản phẩm thú)
-- ═════════════════════════════════════════════════════════════


-- Sức chứa kho (mở rộng được)
CREATE TABLE IF NOT EXISTS warehouse_info (
    char_id    BIGINT NOT NULL PRIMARY KEY,
    max_slots  INT NOT NULL DEFAULT 50,       -- số ô tối đa
    expansions INT NOT NULL DEFAULT 0          -- số lần mở rộng đã mua
);

-- ═════════════════════════════════════════════════════════════
-- FIX: thống nhất trang bị vào character_inventory (bỏ character_equipment ảo)
-- ═════════════════════════════════════════════════════════════
ALTER TABLE character_inventory ADD COLUMN IF NOT EXISTS is_equipped TINYINT NOT NULL DEFAULT 0;
ALTER TABLE character_inventory ADD COLUMN IF NOT EXISTS refine_level INT NOT NULL DEFAULT 0;
ALTER TABLE character_inventory ADD COLUMN IF NOT EXISTS gem_slot_1  INT NOT NULL DEFAULT 0;
ALTER TABLE character_inventory ADD COLUMN IF NOT EXISTS gem_slot_2  INT NOT NULL DEFAULT 0;
ALTER TABLE character_inventory ADD COLUMN IF NOT EXISTS gem_slot_3  INT NOT NULL DEFAULT 0;

-- Bảng config admin còn thiếu (admin CRUD quản lý)
CREATE TABLE IF NOT EXISTS gem_templates (
    id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(64) NOT NULL,
    gem_type VARCHAR(16), stat_bonus VARCHAR(256), tier TINYINT DEFAULT 1,
    icon_id INT DEFAULT 0, is_active TINYINT DEFAULT 1
);
CREATE TABLE IF NOT EXISTS equipment_slots (
    id INT AUTO_INCREMENT PRIMARY KEY, slot_index INT NOT NULL, name VARCHAR(32), is_active TINYINT DEFAULT 1
);
CREATE TABLE IF NOT EXISTS class_change_config (
    id INT AUTO_INCREMENT PRIMARY KEY, from_class INT NOT NULL, to_class INT NOT NULL,
    level_req INT DEFAULT 10, cost_gold INT DEFAULT 0, cost_item INT DEFAULT 0, is_active TINYINT DEFAULT 1
);
CREATE TABLE IF NOT EXISTS default_settings (
    setting_key VARCHAR(64) PRIMARY KEY, setting_value VARCHAR(256), description VARCHAR(128)
);
CREATE TABLE IF NOT EXISTS farmer_layer_mapping (
    id INT AUTO_INCREMENT PRIMARY KEY, layer_name VARCHAR(64), asset_id INT, sort_order INT DEFAULT 0
);

-- Bảng admin item editor (định nghĩa item chi tiết)
CREATE TABLE IF NOT EXISTS item_templates (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(128) NOT NULL, description TEXT,
    item_type VARCHAR(24), equip_slot VARCHAR(16), class_restrict VARCHAR(32),
    level_req INT DEFAULT 1, quality TINYINT DEFAULT 1,
    icon_asset VARCHAR(64), sprite_asset VARCHAR(64),
    stat_hp INT DEFAULT 0, stat_mp INT DEFAULT 0, stat_patk INT DEFAULT 0, stat_matk INT DEFAULT 0,
    stat_def INT DEFAULT 0, stat_crit INT DEFAULT 0, stat_dodge INT DEFAULT 0, stat_accuracy INT DEFAULT 0,
    stat_aspd INT DEFAULT 0, stat_mspd INT DEFAULT 0, stat_lifesteal INT DEFAULT 0, stat_resist INT DEFAULT 0,
    max_enhance INT DEFAULT 0, gem_slots INT DEFAULT 0, can_refine TINYINT DEFAULT 0, can_awaken TINYINT DEFAULT 0,
    buy_price INT DEFAULT 0, sell_price INT DEFAULT 0,
    is_tradeable TINYINT DEFAULT 1, is_stackable TINYINT DEFAULT 0, max_stack INT DEFAULT 1
);
-- Bảng admin asset pack (upload gói asset)
CREATE TABLE IF NOT EXISTS asset_packs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    pack_name VARCHAR(128), original_filename VARCHAR(256), extract_path VARCHAR(256),
    file_count INT DEFAULT 0, total_size BIGINT DEFAULT 0, pack_type VARCHAR(24),
    status VARCHAR(16) DEFAULT 'uploaded', created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
-- Giftcode đa server (server_ids CSV) — admin copy giữa server
CREATE TABLE IF NOT EXISTS gift_codes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE, reward_json VARCHAR(512),
    max_uses INT DEFAULT 1, used_count INT DEFAULT 0,
    server_ids VARCHAR(128) DEFAULT 'all', expires_at DATETIME NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ═════════════════════════════════════════════════════════════
-- FIX: characters thiếu cột gold (18 file dùng) — tiền tệ per-character
-- ═════════════════════════════════════════════════════════════
ALTER TABLE characters ADD COLUMN IF NOT EXISTS gold BIGINT NOT NULL DEFAULT 0;
ALTER TABLE characters ADD COLUMN IF NOT EXISTS last_played DATETIME NULL;

-- ═════════════════════════════════════════════════════════════
-- BỘ DATA NÔNG TRẠI ĐẦY ĐỦ (theme Vọng Linh Giới)
-- Cấu trúc tham khảo Avatar-Sv: thời gian chín / sản lượng / giá bán
-- ═════════════════════════════════════════════════════════════

-- ITEM: hạt giống (seed), nông sản (produce), thức ăn thú, sản phẩm thú, phân bón
INSERT IGNORE INTO items (id,name,description,type,level_req,sell_price,buy_price,icon_id) VALUES
 -- Hạt giống (mua ở shop)
 (6001,'Hạt Lúa Linh','Hạt giống Lúa Linh','seed',1,0,200,1),
 (6002,'Hạt Cà Rốt Vàng','Hạt giống Cà Rốt Vàng','seed',1,0,150,2),
 (6003,'Hạt Linh Chi','Hạt giống Linh Chi Thảo','seed',5,0,500,3),
 (6004,'Hạt Tiên Đào','Hạt giống Tiên Đào','seed',10,0,1200,4),
 (6005,'Hạt Hỏa Liên','Hạt giống Hỏa Liên (hệ hỏa)','seed',15,0,2000,5),
 (6006,'Hạt Băng Tâm Thảo','Hạt giống Băng Tâm Thảo (hệ băng)','seed',20,0,3000,6),
 (6007,'Hạt Tử Vân Quả','Hạt giống Tử Vân Quả (quý)','seed',30,0,6000,7),
 -- Nông sản (thu hoạch → bán)
 (6101,'Lúa Linh','Nông sản cơ bản','material',1,80,0,1),
 (6102,'Cà Rốt Vàng','Nông sản phổ biến','material',1,60,0,2),
 (6103,'Linh Chi Thảo','Dược liệu tu luyện','material',5,200,0,3),
 (6104,'Tiên Đào','Quả tiên hồi linh lực','material',10,450,0,4),
 (6105,'Hỏa Liên','Linh hoa hệ hỏa','material',15,700,0,5),
 (6106,'Băng Tâm Thảo','Linh thảo hệ băng','material',20,1000,0,6),
 (6107,'Tử Vân Quả','Quả quý luyện đan','material',30,2200,0,7),
 -- Thức ăn thú
 (6201,'Cỏ Linh','Thức ăn cho gia cầm','material',1,0,50,1),
 (6202,'Cám Tinh Hoa','Thức ăn cho gia súc','material',1,0,120,2),
 (6203,'Linh Ngư Nhĩ','Thức ăn cho thủy sinh','material',1,0,90,3),
 -- Sản phẩm thú (thu → bán)
 (6301,'Trứng Linh Kê','Sản phẩm chăn nuôi','material',1,50,0,1),
 (6302,'Sữa Linh Ngưu','Sản phẩm chăn nuôi','material',1,120,0,2),
 (6303,'Lông Vũ Quý','Sản phẩm chăn nuôi','material',1,90,0,3),
 (6304,'Tơ Linh Tằm','Sản phẩm cao cấp','material',1,250,0,4),
 -- Phân bón
 (6401,'Phân Linh Thổ','Bón phân: cây lớn nhanh + sản lượng cao','material',1,0,100,1);

-- HẠT GIỐNG → CÂY (farm_seeds): growth_time_min, stages, harvest item, yield, water, fertilizer
INSERT IGNORE INTO farm_seeds (id,name,growth_time_min,stages,harvest_item_id,harvest_qty_min,harvest_qty_max,seed_item_id,water_needed,fertilizer_item_id) VALUES
 (1,'Lúa Linh',          60, 4, 6101, 5, 12, 6001, 3, 6401),
 (2,'Cà Rốt Vàng',       45, 4, 6102, 4, 10, 6002, 2, 6401),
 (3,'Linh Chi Thảo',    180, 5, 6103, 2,  6, 6003, 4, 6401),
 (4,'Tiên Đào',         240, 5, 6104, 3,  8, 6004, 4, 6401),
 (5,'Hỏa Liên',         300, 5, 6105, 2,  5, 6005, 5, 6401),
 (6,'Băng Tâm Thảo',    300, 5, 6106, 2,  5, 6006, 5, 6401),
 (7,'Tử Vân Quả',       480, 6, 6107, 1,  4, 6007, 6, 6401);

-- THÚ NUÔI (farm_animals): feed, produce, produce_time, qty, breed_time
INSERT IGNORE INTO farm_animals (id,name,animal_type,feed_item_id,produce_item_id,produce_time_min,produce_qty,breed_time_min) VALUES
 (1,'Linh Kê',     'bird', 6201, 6301,  60, 3, 720),
 (2,'Linh Ngưu',   'cow',  6202, 6302, 120, 2, 1440),
 (3,'Thải Vũ Điểu','bird', 6201, 6303,  90, 2, 1080),
 (4,'Linh Tằm',    'bug',  6201, 6304, 150, 1, 1440),
 (5,'Linh Ngư',    'fish', 6203, 6301,  80, 2, 960);

-- ═════════════════════════════════════════════════════════════
-- QUẢN LÝ ITEM THEO DANH MỤC: category + cat_no (số thứ tự trong danh mục, từ 1)
-- id giữ nguyên là khóa duy nhất; admin/asset dùng category + cat_no
-- ═════════════════════════════════════════════════════════════
ALTER TABLE items ADD COLUMN IF NOT EXISTS category VARCHAR(24) NOT NULL DEFAULT 'misc';
ALTER TABLE items ADD COLUMN IF NOT EXISTS cat_no   INT NOT NULL DEFAULT 0;

-- Điền danh mục theo dải id (id = mốc + cat_no)
UPDATE items SET category='weapon',       cat_no=id-1000 WHERE id BETWEEN 1000 AND 1999;
UPDATE items SET category='armor',        cat_no=id-2000 WHERE id BETWEEN 2000 AND 2999;
UPDATE items SET category='accessory',    cat_no=id-3000 WHERE id BETWEEN 3000 AND 3999;
UPDATE items SET category='consumable',   cat_no=id-4000 WHERE id BETWEEN 4000 AND 4999;
UPDATE items SET category='material',     cat_no=id-5000 WHERE id BETWEEN 5000 AND 5999;
UPDATE items SET category='farm_seed',    cat_no=id-6000 WHERE id BETWEEN 6000 AND 6099;
UPDATE items SET category='farm_crop',    cat_no=id-6100 WHERE id BETWEEN 6100 AND 6199;
UPDATE items SET category='farm_feed',    cat_no=id-6200 WHERE id BETWEEN 6200 AND 6299;
UPDATE items SET category='farm_produce', cat_no=id-6300 WHERE id BETWEEN 6300 AND 6399;
UPDATE items SET category='farm_tool',    cat_no=id-6400 WHERE id BETWEEN 6400 AND 6499;
UPDATE items SET category='gem',          cat_no=id-7000 WHERE id BETWEEN 7000 AND 7999;
UPDATE items SET category='cosmetic',     cat_no=id-8000 WHERE id BETWEEN 8000 AND 8999;
UPDATE items SET category='quest',        cat_no=id-9000 WHERE id BETWEEN 9000 AND 9999;

-- Phân loại audio hiện có theo ngữ cảnh
UPDATE audio_assets SET category='scene_login'   WHERE asset_key='bgm_login';
UPDATE audio_assets SET category='scene_town'    WHERE asset_key='bgm_village';
UPDATE audio_assets SET category='scene_field'   WHERE asset_key='bgm_field';
UPDATE audio_assets SET category='scene_dungeon' WHERE asset_key='bgm_dungeon';
UPDATE audio_assets SET category='scene_boss'    WHERE asset_key IN ('bgm_boss','bgm_pvp');
UPDATE audio_assets SET category='combat'  WHERE asset_key IN ('sfx_hit','sfx_crit','sfx_die');
UPDATE audio_assets SET category='item'    WHERE asset_key IN ('sfx_loot','sfx_equip','sfx_enhance_ok','sfx_enhance_fail','sfx_gacha');
UPDATE audio_assets SET category='levelup' WHERE asset_key='sfx_levelup';
UPDATE audio_assets SET category='ui_click' WHERE asset_type='ui';
UPDATE audio_assets SET category='nature'  WHERE asset_type='ambient';

-- ═════════════════════════════════════════════════════════════
-- PHÂN DANH MỤC: thêm cột category cho các bảng còn thiếu hẳn
-- (các bảng khác đã phân loại bằng cột riêng — xem CONTENT_REGISTRY.md mục D)
-- ═════════════════════════════════════════════════════════════
ALTER TABLE monsters     ADD COLUMN IF NOT EXISTS category VARCHAR(24) NOT NULL DEFAULT 'normal'; -- normal,elite,boss,world_boss,minion,summon
ALTER TABLE shops        ADD COLUMN IF NOT EXISTS category VARCHAR(24) NOT NULL DEFAULT 'general'; -- general,weapon,armor,consumable,farm,gem,vip,event
ALTER TABLE world_bosses ADD COLUMN IF NOT EXISTS category VARCHAR(24) NOT NULL DEFAULT 'field';   -- field,raid,event,seasonal
ALTER TABLE gem_templates    ADD COLUMN IF NOT EXISTS category VARCHAR(24) NOT NULL DEFAULT 'stat'; -- stat,element,special (bổ sung cạnh gem_type)

-- Phân loại monster hiện có theo is_boss
UPDATE monsters SET category='boss' WHERE is_boss=1;
UPDATE monsters SET category='normal' WHERE is_boss=0;

-- ═════════════════════════════════════════════════════════════
-- BATCH TÍNH NĂNG MỚI: AFK, Chợ, Guild War, Boss hạn giờ, Ngoại Vực, PK mode, VIP
-- ═════════════════════════════════════════════════════════════

-- ───── 1. AFK / TREO MÁY (thẻ theo thời gian) ─────
CREATE TABLE IF NOT EXISTS afk_cards (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(64) NOT NULL,
    duration_hours INT NOT NULL,                 -- số giờ chạy AFK
    price_diamond  INT NOT NULL DEFAULT 0,
    exp_rate     FLOAT NOT NULL DEFAULT 1.0,      -- hệ số exp khi treo
    gold_rate    FLOAT NOT NULL DEFAULT 1.0,
    drop_rate    FLOAT NOT NULL DEFAULT 1.0,
    is_active    TINYINT NOT NULL DEFAULT 1
);
INSERT IGNORE INTO afk_cards (id,name,duration_hours,price_diamond,exp_rate,gold_rate,drop_rate) VALUES
 (1,'Thẻ AFK 2 giờ',   2,  50, 1.0,1.0,1.0),
 (2,'Thẻ AFK 6 giờ',   6, 120, 1.1,1.1,1.0),
 (3,'Thẻ AFK 12 giờ', 12, 200, 1.2,1.2,1.1),
 (4,'Thẻ AFK 24 giờ', 24, 350, 1.3,1.3,1.2),
 (5,'Thẻ AFK 7 ngày',168,1800,1.5,1.5,1.3);
-- phiên AFK của nhân vật (treo cả khi off)
CREATE TABLE IF NOT EXISTS character_afk (
    char_id      BIGINT NOT NULL PRIMARY KEY,
    card_id      INT NOT NULL,
    map_id       INT NOT NULL,
    started_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at   DATETIME NOT NULL,
    last_claim_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    accrued_exp  BIGINT NOT NULL DEFAULT 0,
    accrued_gold BIGINT NOT NULL DEFAULT 0,
    is_active    TINYINT NOT NULL DEFAULT 1
);

-- ───── 2. CHỢ NGƯỜI CHƠI (giá cố định, gold HOẶC diamond) ─────
CREATE TABLE IF NOT EXISTS market_listings (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    seller_char_id BIGINT NOT NULL,
    seller_name   VARCHAR(32) NOT NULL,
    inventory_id  BIGINT NOT NULL,
    item_id       INT NOT NULL,
    item_name     VARCHAR(64) NOT NULL,
    qty           INT NOT NULL DEFAULT 1,
    enhance_level INT NOT NULL DEFAULT 0,
    currency      TINYINT NOT NULL DEFAULT 1,    -- 1=gold, 2=diamond
    price         BIGINT NOT NULL,               -- giá cho cả lô
    category      VARCHAR(24) NOT NULL DEFAULT 'misc', -- lọc theo danh mục item
    listed_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at    DATETIME NULL,                 -- hết hạn tự trả lại
    status        VARCHAR(12) NOT NULL DEFAULT 'active', -- active,sold,cancelled,expired
    buyer_char_id BIGINT DEFAULT NULL,
    sold_at       DATETIME DEFAULT NULL,
    INDEX idx_status_cat (status, category), INDEX idx_seller (seller_char_id)
);

-- ───── 3. GUILD WAR (guild chiến) ─────
CREATE TABLE IF NOT EXISTS guild_wars (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    guild_a      INT NOT NULL, guild_b INT NOT NULL,
    map_id       INT NOT NULL,                   -- map chiến trường
    status       VARCHAR(12) NOT NULL DEFAULT 'scheduled', -- scheduled,ongoing,ended
    score_a      INT NOT NULL DEFAULT 0, score_b INT NOT NULL DEFAULT 0,
    start_at     DATETIME NOT NULL, end_at DATETIME NOT NULL,
    winner_guild INT DEFAULT NULL,
    reward_json  VARCHAR(512) DEFAULT NULL
);
CREATE TABLE IF NOT EXISTS guild_war_kills (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    war_id       INT NOT NULL, killer_char_id BIGINT NOT NULL, victim_char_id BIGINT NOT NULL,
    killer_guild INT NOT NULL, points INT NOT NULL DEFAULT 1, created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_war (war_id)
);

-- ───── 4. WORLD BOSS hạn giờ + first-kill + damage ranking ─────
ALTER TABLE world_bosses ADD COLUMN IF NOT EXISTS duration_min INT NOT NULL DEFAULT 30;   -- tồn tại bao lâu
ALTER TABLE world_bosses ADD COLUMN IF NOT EXISTS active_until DATETIME NULL;              -- thời điểm despawn
ALTER TABLE world_bosses ADD COLUMN IF NOT EXISTS current_hp BIGINT NULL;                  -- hp hiện tại (live)
ALTER TABLE world_bosses ADD COLUMN IF NOT EXISTS first_kill_reward_json VARCHAR(512) DEFAULT NULL; -- thưởng lớn cho người kết liễu
ALTER TABLE world_bosses ADD COLUMN IF NOT EXISTS is_alive TINYINT NOT NULL DEFAULT 0;
-- damage của từng người (xếp hạng thưởng theo sát thương)
CREATE TABLE IF NOT EXISTS world_boss_damage (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    boss_id      INT NOT NULL, spawn_seq INT NOT NULL DEFAULT 0, -- lần spawn thứ mấy
    char_id      BIGINT NOT NULL, char_name VARCHAR(32) NOT NULL,
    total_damage BIGINT NOT NULL DEFAULT 0, last_hit_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq (boss_id, spawn_seq, char_id), INDEX idx_boss (boss_id, spawn_seq)
);

-- ───── 5. NGOẠI VỰC (tháp nhiều tầng, theo level, có PK) ─────
CREATE TABLE IF NOT EXISTS outer_realm_floors (
    floor        INT NOT NULL PRIMARY KEY,        -- tầng 1,2,3...
    name         VARCHAR(64) NOT NULL,
    map_id       INT NOT NULL,
    min_level    INT NOT NULL DEFAULT 1,           -- level tối thiểu vào tầng
    max_players  INT NOT NULL DEFAULT 50,
    is_pvp       TINYINT NOT NULL DEFAULT 1,       -- ngoại vực cho PK
    monster_min_level INT NOT NULL DEFAULT 1, monster_max_level INT NOT NULL DEFAULT 10,
    clear_reward_json VARCHAR(512) DEFAULT NULL,
    is_active    TINYINT NOT NULL DEFAULT 1
);
CREATE TABLE IF NOT EXISTS outer_realm_bosses (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    floor        INT NOT NULL, monster_id INT NOT NULL, boss_level INT NOT NULL DEFAULT 1,
    spawn_x FLOAT DEFAULT 0, spawn_y FLOAT DEFAULT 0, reward_json VARCHAR(512) DEFAULT NULL,
    INDEX idx_floor (floor)
);
INSERT IGNORE INTO outer_realm_floors (floor,name,map_id,min_level,monster_min_level,monster_max_level) VALUES
 (1,'Ngoại Vực - Tầng 1',400,1,1,10),
 (2,'Ngoại Vực - Tầng 2',401,15,10,25),
 (3,'Ngoại Vực - Tầng 3',402,30,25,45),
 (4,'Ngoại Vực - Tầng 4',403,50,45,70),
 (5,'Ngoại Vực - Tầng 5',404,75,70,100);

-- ───── 6. PK MODE + TRUY NÃ + NHÀ TÙ ─────
ALTER TABLE characters ADD COLUMN IF NOT EXISTS combat_mode VARCHAR(12) NOT NULL DEFAULT 'peace'; -- peace,guild,faction,server,berserk
ALTER TABLE characters ADD COLUMN IF NOT EXISTS faction_id INT NOT NULL DEFAULT 0;
ALTER TABLE characters ADD COLUMN IF NOT EXISTS wanted_level INT NOT NULL DEFAULT 0;     -- mức truy nã (giết người vô tội)
ALTER TABLE characters ADD COLUMN IF NOT EXISTS pk_kills INT NOT NULL DEFAULT 0;
ALTER TABLE characters ADD COLUMN IF NOT EXISTS jailed_until DATETIME NULL;              -- bị nhốt tới khi nào
CREATE TABLE IF NOT EXISTS pk_log (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    killer_char_id BIGINT NOT NULL, victim_char_id BIGINT NOT NULL,
    map_id INT NOT NULL, killer_mode VARCHAR(12), victim_innocent TINYINT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP, INDEX idx_killer (killer_char_id)
);
-- map an toàn (tân thủ chỉ hoà bình)
ALTER TABLE maps ADD COLUMN IF NOT EXISTS force_peace TINYINT NOT NULL DEFAULT 0;  -- 1 = ép chế độ hoà bình
ALTER TABLE maps ADD COLUMN IF NOT EXISTS allow_pk TINYINT NOT NULL DEFAULT 0;

-- ───── 7. VIP (mốc thưởng + đặc quyền) ─────
CREATE TABLE IF NOT EXISTS vip_levels (
    vip_level    INT NOT NULL PRIMARY KEY,
    exp_required INT NOT NULL,                    -- điểm VIP (thường = tổng nạp)
    name         VARCHAR(32) NOT NULL,
    daily_diamond INT NOT NULL DEFAULT 0,         -- đặc quyền: kim cương mỗi ngày
    afk_bonus_pct FLOAT NOT NULL DEFAULT 0,       -- +% reward AFK
    extra_bag_slots INT NOT NULL DEFAULT 0,
    extra_market_slots INT NOT NULL DEFAULT 0,
    privileges_json VARCHAR(512) DEFAULT NULL     -- đặc quyền khác (auto-pick, free teleport...)
);
INSERT IGNORE INTO vip_levels (vip_level,exp_required,name,daily_diamond,afk_bonus_pct,extra_bag_slots,extra_market_slots) VALUES
 (0,0,'Thường',0,0,0,0),(1,100,'VIP 1',5,0.05,5,1),(2,500,'VIP 2',10,0.10,10,2),
 (3,1500,'VIP 3',20,0.15,15,3),(4,3000,'VIP 4',35,0.20,20,4),(5,6000,'VIP 5',50,0.30,30,5),
 (6,15000,'VIP 6',80,0.40,40,6),(7,30000,'VIP 7',120,0.50,50,8),(8,60000,'VIP 8',200,0.70,70,10);
-- mốc thưởng VIP (nhận 1 lần khi đạt mốc)
CREATE TABLE IF NOT EXISTS vip_milestone_rewards (
    vip_level    INT NOT NULL PRIMARY KEY,
    reward_json  VARCHAR(512) NOT NULL
);
CREATE TABLE IF NOT EXISTS character_vip_claims (
    char_id      BIGINT NOT NULL, vip_level INT NOT NULL,
    claimed_at   DATETIME DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY (char_id, vip_level)
);

-- ───── VIP: mở rộng nhiều quyền lợi theo từng mốc ─────
ALTER TABLE vip_levels ADD COLUMN IF NOT EXISTS exp_bonus_pct FLOAT NOT NULL DEFAULT 0;       -- +% EXP mọi nguồn
ALTER TABLE vip_levels ADD COLUMN IF NOT EXISTS drop_bonus_pct FLOAT NOT NULL DEFAULT 0;      -- +% tỉ lệ rơi đồ
ALTER TABLE vip_levels ADD COLUMN IF NOT EXISTS gold_bonus_pct FLOAT NOT NULL DEFAULT 0;      -- +% vàng kiếm được
ALTER TABLE vip_levels ADD COLUMN IF NOT EXISTS daily_gold INT NOT NULL DEFAULT 0;            -- vàng tặng mỗi ngày
ALTER TABLE vip_levels ADD COLUMN IF NOT EXISTS market_fee_discount_pct FLOAT NOT NULL DEFAULT 0; -- giảm phí chợ
ALTER TABLE vip_levels ADD COLUMN IF NOT EXISTS revive_discount_pct FLOAT NOT NULL DEFAULT 0; -- giảm phí hồi sinh
ALTER TABLE vip_levels ADD COLUMN IF NOT EXISTS afk_cap_hours INT NOT NULL DEFAULT 24;        -- trần giờ AFK offline
ALTER TABLE vip_levels ADD COLUMN IF NOT EXISTS free_teleport_daily INT NOT NULL DEFAULT 0;   -- dịch chuyển miễn phí/ngày
ALTER TABLE vip_levels ADD COLUMN IF NOT EXISTS auto_pickup TINYINT NOT NULL DEFAULT 0;       -- tự nhặt đồ
ALTER TABLE vip_levels ADD COLUMN IF NOT EXISTS name_color VARCHAR(8) DEFAULT NULL;           -- màu tên riêng
ALTER TABLE vip_levels ADD COLUMN IF NOT EXISTS exclusive_title_id INT NOT NULL DEFAULT 0;    -- danh hiệu độc quyền

-- Cập nhật quyền lợi chi tiết từng mốc (tăng dần)
UPDATE vip_levels SET exp_bonus_pct=0,  drop_bonus_pct=0,  gold_bonus_pct=0,  daily_gold=0,      market_fee_discount_pct=0,  revive_discount_pct=0,  afk_cap_hours=24,  free_teleport_daily=0,  auto_pickup=0, name_color=NULL,      exclusive_title_id=0  WHERE vip_level=0;
UPDATE vip_levels SET exp_bonus_pct=5,  drop_bonus_pct=3,  gold_bonus_pct=5,  daily_gold=5000,   market_fee_discount_pct=5,  revive_discount_pct=10, afk_cap_hours=36,  free_teleport_daily=0,  auto_pickup=0, name_color='#88cc88', exclusive_title_id=0  WHERE vip_level=1;
UPDATE vip_levels SET exp_bonus_pct=10, drop_bonus_pct=6,  gold_bonus_pct=10, daily_gold=12000,  market_fee_discount_pct=10, revive_discount_pct=20, afk_cap_hours=48,  free_teleport_daily=1,  auto_pickup=0, name_color='#66cc66', exclusive_title_id=0  WHERE vip_level=2;
UPDATE vip_levels SET exp_bonus_pct=15, drop_bonus_pct=10, gold_bonus_pct=15, daily_gold=25000,  market_fee_discount_pct=15, revive_discount_pct=30, afk_cap_hours=72,  free_teleport_daily=2,  auto_pickup=1, name_color='#44ccaa', exclusive_title_id=0  WHERE vip_level=3;
UPDATE vip_levels SET exp_bonus_pct=20, drop_bonus_pct=15, gold_bonus_pct=20, daily_gold=50000,  market_fee_discount_pct=20, revive_discount_pct=40, afk_cap_hours=96,  free_teleport_daily=3,  auto_pickup=1, name_color='#33ccff', exclusive_title_id=0  WHERE vip_level=4;
UPDATE vip_levels SET exp_bonus_pct=30, drop_bonus_pct=20, gold_bonus_pct=30, daily_gold=100000, market_fee_discount_pct=25, revive_discount_pct=50, afk_cap_hours=120, free_teleport_daily=5,  auto_pickup=1, name_color='#3399ff', exclusive_title_id=0  WHERE vip_level=5;
UPDATE vip_levels SET exp_bonus_pct=40, drop_bonus_pct=25, gold_bonus_pct=40, daily_gold=200000, market_fee_discount_pct=30, revive_discount_pct=60, afk_cap_hours=168, free_teleport_daily=8,  auto_pickup=1, name_color='#9966ff', exclusive_title_id=0  WHERE vip_level=6;
UPDATE vip_levels SET exp_bonus_pct=55, drop_bonus_pct=35, gold_bonus_pct=55, daily_gold=400000, market_fee_discount_pct=40, revive_discount_pct=75, afk_cap_hours=240, free_teleport_daily=12, auto_pickup=1, name_color='#ff66cc', exclusive_title_id=0  WHERE vip_level=7;
UPDATE vip_levels SET exp_bonus_pct=80, drop_bonus_pct=50, gold_bonus_pct=80, daily_gold=800000, market_fee_discount_pct=50, revive_discount_pct=100,afk_cap_hours=336, free_teleport_daily=20, auto_pickup=1, name_color='#ffcc00', exclusive_title_id=0  WHERE vip_level=8;

-- Thưởng MỐC VIP (nhận 1 lần khi đạt) — diamond + vàng + item, JSON do admin chỉnh thêm
INSERT IGNORE INTO vip_milestone_rewards (vip_level,reward_json) VALUES
 (1,'{"diamond":50,"gold":50000,"items":[[4001,5]]}'),
 (2,'{"diamond":120,"gold":120000,"items":[[4001,10],[4002,5]]}'),
 (3,'{"diamond":250,"gold":300000,"items":[[4002,10],[7001,1]]}'),
 (4,'{"diamond":500,"gold":600000,"items":[[7001,2],[2001,1]]}'),
 (5,'{"diamond":1000,"gold":1200000,"items":[[7001,3],[8001,1]]}'),
 (6,'{"diamond":2000,"gold":2500000,"items":[[8001,1],[1001,1]]}'),
 (7,'{"diamond":4000,"gold":5000000,"items":[[8001,2],[1002,1]]}'),
 (8,'{"diamond":8000,"gold":10000000,"items":[[8001,3],[1003,1]]}');

-- World boss: chu kỳ hồi sinh (phút) cho scheduler tự spawn
ALTER TABLE world_bosses ADD COLUMN IF NOT EXISTS spawn_interval_min INT NOT NULL DEFAULT 180;
ALTER TABLE world_bosses ADD COLUMN IF NOT EXISTS last_killer_name VARCHAR(32) DEFAULT NULL;

-- ═════════════════════════════════════════════════════════════
-- HỆ THỐNG HOẠT ĐỘNG (Activity Hub) — gom sự kiện/nhiệm vụ giới hạn thời gian
-- Admin bật/tắt + cấu hình thời gian, mốc thưởng, điều kiện.
-- ═════════════════════════════════════════════════════════════
CREATE TABLE IF NOT EXISTS activities (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    activity_type VARCHAR(24) NOT NULL,          -- login,online,daily_mission,weekly_mission,spending,exchange,ranking,boss_hunt,x2_exp,x2_drop,lucky_wheel,guild_event,cross_server,special
    name          VARCHAR(64) NOT NULL,
    description   VARCHAR(512) DEFAULT '',
    icon_id       INT NOT NULL DEFAULT 0,
    is_enabled    TINYINT NOT NULL DEFAULT 1,    -- admin bật/tắt
    start_at      DATETIME NULL,                 -- NULL = luôn mở
    end_at        DATETIME NULL,
    server_id     INT NOT NULL DEFAULT 0,        -- 0 = mọi server; >0 = riêng server; -1 = liên server
    sort_order    INT NOT NULL DEFAULT 0,
    -- với x2_exp/x2_drop: multiplier; với khác: cấu hình riêng
    multiplier    FLOAT NOT NULL DEFAULT 1.0,
    action_type   VARCHAR(16) NOT NULL DEFAULT 'progress', -- progress,claim,exchange,join,wheel,passive
    config_json   VARCHAR(1024) DEFAULT NULL,
    INDEX idx_type_enabled (activity_type, is_enabled)
);

-- Mốc thưởng / mục tiêu của một hoạt động (login ngày, online phút, điểm nhiệm vụ, mốc tiêu KC...)
CREATE TABLE IF NOT EXISTS activity_milestones (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    activity_id   INT NOT NULL,
    milestone_order INT NOT NULL DEFAULT 0,
    requirement   INT NOT NULL DEFAULT 0,        -- ngưỡng: ngày/phút/điểm/KC tiêu
    reward_json   VARCHAR(512) NOT NULL DEFAULT '{}',
    item_cost_id  INT NOT NULL DEFAULT 0,        -- đổi thưởng: vật phẩm cần tiêu
    item_cost_qty INT NOT NULL DEFAULT 0,
    exchange_limit INT NOT NULL DEFAULT 0,       -- đổi thưởng: giới hạn số lần (0 = không giới hạn)
    label         VARCHAR(64) DEFAULT '',
    INDEX idx_activity (activity_id, milestone_order)
);

-- Tiến độ của từng nhân vật trong từng hoạt động
CREATE TABLE IF NOT EXISTS activity_progress (
    char_id       BIGINT NOT NULL,
    activity_id   INT NOT NULL,
    progress      BIGINT NOT NULL DEFAULT 0,     -- giá trị tích luỹ (ngày/phút/điểm/KC)
    streak        INT NOT NULL DEFAULT 0,        -- chuỗi liên tiếp (login)
    last_tick_at  DATETIME NULL,                 -- mốc cập nhật cuối (login ngày/online)
    claimed_json  VARCHAR(512) NOT NULL DEFAULT '[]', -- danh sách milestone_order đã nhận
    updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (char_id, activity_id)
);

-- Log đổi thưởng (đếm giới hạn theo từng milestone)
CREATE TABLE IF NOT EXISTS activity_exchange_log (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    char_id       BIGINT NOT NULL,
    activity_id   INT NOT NULL,
    milestone_id  INT NOT NULL,
    count         INT NOT NULL DEFAULT 1,
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq (char_id, milestone_id)
);

-- Seed ví dụ (admin sẽ chỉnh/thêm)
INSERT IGNORE INTO activities (id,activity_type,name,description,is_enabled,action_type,multiplier) VALUES
 (1,'login','Quà Đăng Nhập','Đăng nhập mỗi ngày nhận quà, chuỗi liên tiếp thưởng lớn',1,'claim',1.0),
 (2,'online','Quà Online','Online tích thời gian mở mốc thưởng',1,'claim',1.0),
 (3,'daily_mission','Nhiệm Vụ Ngày','Hoàn thành nhiệm vụ nhận điểm hoạt động',1,'progress',1.0),
 (4,'weekly_mission','Nhiệm Vụ Tuần','Mục tiêu tuần, thưởng theo mốc điểm',1,'progress',1.0),
 (5,'spending','Tích Tiêu','Tiêu Kim Cương nhận thưởng theo mốc',1,'claim',1.0),
 (6,'exchange','Đổi Thưởng','Đổi vật phẩm sự kiện lấy phần thưởng',1,'exchange',1.0),
 (7,'x2_exp','X2 EXP','Nhân đôi kinh nghiệm trong thời gian sự kiện',0,'passive',2.0),
 (8,'x2_drop','X2 Tỉ Lệ Rơi','Nhân đôi tỉ lệ rơi đồ trong thời gian sự kiện',0,'passive',2.0);

INSERT IGNORE INTO activity_milestones (activity_id,milestone_order,requirement,reward_json,label) VALUES
 (1,1,1,'{"gold":10000}','Ngày 1'),(1,2,3,'{"diamond":20}','Ngày 3'),(1,3,7,'{"diamond":50,"items":[[7001,1]]}','Ngày 7'),
 (2,1,30,'{"gold":5000}','30 phút'),(2,2,60,'{"gold":15000}','60 phút'),(2,3,120,'{"diamond":30}','120 phút'),
 (3,1,100,'{"gold":20000}','100 điểm'),(3,2,300,'{"diamond":30}','300 điểm'),
 (5,1,100,'{"items":[[8001,1]]}','Tiêu 100 KC'),(5,2,500,'{"items":[[8001,3]]}','Tiêu 500 KC');

-- Sự kiện ĐUA TOP: thưởng theo khoảng hạng (Hạng 1, 2, 3, Xếp 4-5, 6-10...)
-- Xếp hạng theo activity_progress.progress (điểm sự kiện), KHÁC BXH thật.
CREATE TABLE IF NOT EXISTS activity_rank_rewards (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    activity_id   INT NOT NULL,
    rank_from     INT NOT NULL,                  -- hạng bắt đầu (1)
    rank_to       INT NOT NULL,                  -- hạng kết thúc (1 = chỉ hạng 1; 5 = tới hạng 5)
    reward_json   VARCHAR(512) NOT NULL DEFAULT '{}',
    label         VARCHAR(48) DEFAULT '',        -- "Hạng 1","Xếp 4-5"...
    INDEX idx_activity (activity_id, rank_from)
);
-- Đánh dấu đã phát thưởng đua top khi kết thúc (tránh phát trùng)
ALTER TABLE activities ADD COLUMN IF NOT EXISTS rewards_distributed TINYINT NOT NULL DEFAULT 0;

-- Seed: thêm 1 sự kiện đua top mẫu + mốc hạng (giống ảnh: top 10 chiến lực)
INSERT IGNORE INTO activities (id,activity_type,name,description,is_enabled,action_type,multiplier) VALUES
 (9,'ranking','BXH Chiến Lực','Đua top chiến lực, phát thưởng cho top 10 khi kết thúc',1,'progress',1.0);
INSERT IGNORE INTO activity_rank_rewards (activity_id,rank_from,rank_to,reward_json,label) VALUES
 (9,1,1,'{"diamond":500,"items":[[8001,1]]}','Hạng 1'),
 (9,2,2,'{"diamond":300,"items":[[8001,1]]}','Hạng 2'),
 (9,3,3,'{"diamond":200}','Hạng 3'),
 (9,4,5,'{"diamond":100}','Xếp 4-5'),
 (9,6,10,'{"diamond":50}','Xếp 6-10');

-- ═════════════════════════════════════════════════════════════
-- CATALOG LOẠI HOẠT ĐỘNG — phủ mọi tính năng ingame.
-- activities.activity_type tham chiếu type_key. Hệ thống ingame gọi
-- ActivityHandler.fire(charId, type_key, amount) → tự cộng tiến độ mọi
-- sự kiện đang bật khớp type. Admin tạo SK chỉ cần chọn type + đặt mốc.
-- ═════════════════════════════════════════════════════════════
CREATE TABLE IF NOT EXISTS activity_types (
    type_key      VARCHAR(24) NOT NULL PRIMARY KEY,
    display_name  VARCHAR(48) NOT NULL,
    category      VARCHAR(24) NOT NULL,          -- combat,progression,pvp,economy,collection,life,quest,faction,special
    unit          VARCHAR(24) NOT NULL DEFAULT 'diem', -- đơn vị tiến độ hiển thị
    default_action VARCHAR(16) NOT NULL DEFAULT 'progress', -- progress,claim,exchange,ranking,passive,wheel
    description   VARCHAR(256) DEFAULT ''
);
INSERT IGNORE INTO activity_types (type_key,display_name,category,unit,default_action,description) VALUES
 -- Combat / PvE
 ('kill_monster','Diet Quai','combat','con','progress','So quai tieu diet'),
 ('kill_boss','San Boss','combat','con','progress','So boss tieu diet'),
 ('world_boss','Boss The Gioi','combat','sat thuong','ranking','Sat thuong len world boss'),
 ('dungeon_clear','Vuot Ham Nguc','combat','luot','progress','So lan clear dungeon'),
 ('outer_clear','Vuot Ngoai Vuc','combat','tang','progress','So tang ngoai vuc vuot'),
 -- Progression
 ('gain_exp','Tich EXP','progression','exp','progress','Tong EXP kiem duoc'),
 ('level_up','Len Cap','progression','cap','progress','So cap tang'),
 ('enhance','Cuong Hoa','progression','lan','progress','So lan cuong hoa thanh cong'),
 ('refine','Tinh Luyen','progression','lan','progress','So lan tinh luyen'),
 ('gem_socket','Kham Ngoc','progression','lan','progress','So lan kham ngoc'),
 -- PvP
 ('pvp_win','Thang PvP','pvp','tran','progress','So tran PvP thang'),
 ('pvp_kill','Ha Guc PvP','pvp','mang','progress','So nguoi ha guc'),
 ('arena_rank','Dau Truong','pvp','diem','ranking','Diem dau truong'),
 ('guild_war_kill','Bang Chien','pvp','diem','ranking','Diem guild war'),
 ('power_rank','Dua Top Chien Luc','pvp','luc chien','ranking','Xep theo chien luc'),
 -- Economy
 ('spend_diamond','Tich Tieu KC','economy','KC','claim','Tong Kim Cuong tieu hao'),
 ('spend_gold','Tich Tieu Vang','economy','vang','claim','Tong vang tieu hao'),
 ('topup','Su Kien Nap','economy','KC nap','claim','Tong nap trong su kien'),
 ('market_sell','Ban Cho','economy','luot','progress','So luot ban tren cho'),
 ('market_buy','Mua Cho','economy','luot','progress','So luot mua tren cho'),
 -- Collection / Gacha
 ('gacha_pull','Trieu Hoi','collection','luot','progress','So lan quay gacha'),
 ('lucky_wheel','Vong Quay','collection','luot','wheel','Quay thuong bang ve/vat pham'),
 ('collect_pet','Suu Tap Pet','collection','con','progress','So pet so huu'),
 ('collect_mount','Suu Tap Thu Cuoi','collection','con','progress','So thu cuoi so huu'),
 ('collect_cosmetic','Suu Tap Trang Suc','collection','mon','progress','So cosmetic so huu'),
 -- Life / Social
 ('login','Qua Dang Nhap','life','ngay','claim','Diem danh moi ngay'),
 ('online','Qua Online','life','phut','claim','Thoi gian online'),
 ('fishing','Cau Ca','life','con','progress','So ca cau duoc'),
 ('farm_harvest','Thu Hoach Nong Trai','life','lan','progress','So lan thu hoach'),
 ('cooking','Nau An','life','mon','progress','So mon nau'),
 ('marriage','Ket Hon','life','su kien','progress','Hoat dong ket hon'),
 ('child_raise','Nuoi Con','life','lan','progress','Cham soc con'),
 -- Quest
 ('daily_mission','Nhiem Vu Ngay','quest','diem','progress','Diem nhiem vu ngay'),
 ('weekly_mission','Nhiem Vu Tuan','quest','diem','progress','Diem nhiem vu tuan'),
 ('quest_complete','Hoan Thanh NV','quest','nv','progress','So nhiem vu hoan thanh'),
 ('story_chapter','Cot Truyen','quest','chuong','progress','So chuong cot truyen'),
 -- Faction
 ('faction_rep','Danh Vong Phe','faction','diem','progress','Danh vong tich luy'),
 -- Special
 ('exchange','Doi Thuong','special','luot','exchange','Doi vat pham lay thuong'),
 ('ranking','Dua Top','special','diem','ranking','Xep hang theo diem su kien'),
 ('x2_exp','X2 EXP','special','-','passive','Nhan doi EXP trong thoi gian'),
 ('x2_drop','X2 Ti Le Roi','special','-','passive','Nhan doi ti le roi do'),
 ('guild_event','Su Kien Bang','special','diem','progress','Hoat dong cung bang hoi'),
 ('cross_server','Lien Server','special','diem','ranking','Thi dau lien may chu'),
 ('special','Su Kien Dac Biet','special','diem','progress','Su kien le/dac biet');

-- ───── BIẾN THỂ HOẠT ĐỘNG: điều kiện + cơ chế giải quyết + hiệu ứng bị động ─────
-- goal_mode: milestone (mốc - cũ) | race (ai đạt trước thắng) | ranking (top điểm) | passive (hiệu ứng trong thời gian, không tiến độ)
ALTER TABLE activities ADD COLUMN IF NOT EXISTS goal_mode VARCHAR(12) NOT NULL DEFAULT 'milestone';
-- điều kiện ĐẾM tiến độ (lọc cái gì mới tính): {"item_id":1234} | {"rarity_min":4} | {"subtype":"fish_rare"} | {"monster_id":50}
ALTER TABLE activities ADD COLUMN IF NOT EXISTS condition_json VARCHAR(512) DEFAULT NULL;
-- race: mục tiêu cần đạt + người thắng + thưởng người thắng
ALTER TABLE activities ADD COLUMN IF NOT EXISTS target INT NOT NULL DEFAULT 1;
ALTER TABLE activities ADD COLUMN IF NOT EXISTS winner_char_id BIGINT NULL;
ALTER TABLE activities ADD COLUMN IF NOT EXISTS win_reward_json VARCHAR(512) DEFAULT NULL;
-- passive: hiệu ứng trong thời gian SK — rớt thêm item/exclusive/x2
-- {"multiplier":2} | {"chance":0.1,"items":[[9100,1]]} | {"exclusive_item":9100,"chance":0.05}
ALTER TABLE activities ADD COLUMN IF NOT EXISTS drop_json VARCHAR(512) DEFAULT NULL;

-- Seed ví dụ biến thể:
-- 1) Câu cá QUÝ - ai câu được cá hiếm (rarity>=4) ĐẦU TIÊN thắng
INSERT IGNORE INTO activities (id,activity_type,name,description,is_enabled,action_type,goal_mode,condition_json,target,win_reward_json) VALUES
 (20,'fishing','Săn Cá Quý','Ai câu được Cá Quý (hiếm) đầu tiên giành chiến thắng sự kiện',1,'passive','race','{"rarity_min":4}',1,'{"diamond":1000,"items":[[8001,1]]}');
-- 2) Sự kiện RỚT EXCLUSIVE - trong thời gian SK, đánh quái có 10% rớt item sự kiện 9100 để đổi thưởng
INSERT IGNORE INTO activities (id,activity_type,name,description,is_enabled,action_type,goal_mode,drop_json) VALUES
 (21,'x2_drop','Mùa Rơi Bảo Vật','Trong thời gian sự kiện, diệt quái có cơ hội rớt Bảo Vật Sự Kiện để đổi thưởng',1,'passive','passive','{"chance":0.1,"items":[[9100,1]]}');
-- 3) Đổi thưởng dùng item sự kiện 9100 (gắn với SK rớt ở trên)
INSERT IGNORE INTO activities (id,activity_type,name,description,is_enabled,action_type,goal_mode) VALUES
 (22,'exchange','Đổi Bảo Vật','Dùng Bảo Vật Sự Kiện đổi phần thưởng giá trị',1,'exchange','milestone');
INSERT IGNORE INTO activity_milestones (activity_id,milestone_order,requirement,reward_json,item_cost_id,item_cost_qty,exchange_limit,label) VALUES
 (22,1,0,'{"diamond":100}',9100,10,5,'Đổi 10 Bảo Vật -> 100 KC'),
 (22,2,0,'{"items":[[8001,1]]}',9100,50,1,'Đổi 50 Bảo Vật -> Cánh hiếm');
