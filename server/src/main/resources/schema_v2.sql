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

CREATE TABLE IF NOT EXISTS topup_packages (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(128) NOT NULL,
    diamond      INT NOT NULL,
    bonus_diamond INT NOT NULL DEFAULT 0,
    price_vnd    INT NOT NULL,
    is_featured  TINYINT NOT NULL DEFAULT 0,
    is_active    TINYINT NOT NULL DEFAULT 1,
    icon_url     VARCHAR(256),
    sort_order   INT NOT NULL DEFAULT 0,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT IGNORE INTO topup_packages (id,name,diamond,bonus_diamond,price_vnd,is_featured,sort_order) VALUES
(1,'Gói Nhỏ',        100,  0,    10000, 0, 1),
(2,'Gói Vừa',        500,  50,   50000, 0, 2),
(3,'Gói Lớn',       1200, 200,  100000, 1, 3),
(4,'Gói VIP',       2500, 500,  200000, 0, 4),
(5,'Gói Siêu VIP',  6500,1500,  500000, 1, 5);

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

INSERT IGNORE INTO farm_seeds (id,name,growth_time_min,stages,harvest_item_id,harvest_qty_min,harvest_qty_max,seed_item_id,water_needed) VALUES
(1,'Lúa Mì',   30,4,101,3,8,201,2),
(2,'Cà Rốt',   60,4,102,2,6,202,3),
(3,'Bí Đỏ',   120,4,103,1,4,203,4),
(4,'Hoa Sen',  240,5,104,1,3,204,5),
(5,'Linh Thảo',480,6,105,1,2,205,6);

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

INSERT IGNORE INTO farm_animals (id,name,animal_type,feed_item_id,produce_item_id,produce_time_min,produce_qty) VALUES
(1,'Gà Vàng','bird',301,401,60,3),
(2,'Lợn Hồng','pig',302,402,120,1),
(3,'Cá Chép','fish',303,403,30,5),
(4,'Bò Sữa','cow',304,404,180,2);

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
CREATE TABLE IF NOT EXISTS guild_invites (
    guild_id        BIGINT NOT NULL,
    char_id         BIGINT NOT NULL,
    invited_by      BIGINT NOT NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (guild_id, char_id)
);

-- PvP duels table
CREATE TABLE IF NOT EXISTS pvp_duels (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    char_id_a       BIGINT NOT NULL,
    char_id_b       BIGINT NOT NULL,
    winner_char_id  BIGINT DEFAULT NULL,
    status          VARCHAR(16) NOT NULL DEFAULT 'pending',
    map_id          INT DEFAULT NULL,
    elo_change      INT DEFAULT 0,
    started_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ended_at        DATETIME DEFAULT NULL
);

-- Farm seeds/animals config
CREATE TABLE IF NOT EXISTS farm_seeds (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    seed_name       VARCHAR(64) NOT NULL,
    grow_time_min   INT NOT NULL DEFAULT 60,
    harvest_item_id INT NOT NULL,
    harvest_qty     INT NOT NULL DEFAULT 1,
    buy_price       INT NOT NULL DEFAULT 100,
    exp_reward      INT NOT NULL DEFAULT 10,
    is_active       TINYINT NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS farm_animals (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    animal_name     VARCHAR(64) NOT NULL,
    feed_item_id    INT NOT NULL DEFAULT 0,
    product_item_id INT NOT NULL,
    produce_time_min INT NOT NULL DEFAULT 120,
    buy_price       INT NOT NULL DEFAULT 500,
    is_active       TINYINT NOT NULL DEFAULT 1
);

-- Furniture catalog
CREATE TABLE IF NOT EXISTS furniture_catalog (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(64) NOT NULL,
    furniture_type  VARCHAR(32) NOT NULL DEFAULT 'decoration',
    price_gold      INT NOT NULL DEFAULT 1000,
    size            INT NOT NULL DEFAULT 1,
    icon_id         INT NOT NULL DEFAULT 0,
    is_active       TINYINT NOT NULL DEFAULT 1,
    sort_order      INT NOT NULL DEFAULT 0
);

INSERT IGNORE INTO farm_seeds (id,seed_name,grow_time_min,harvest_item_id,harvest_qty,buy_price) VALUES
(1,'Hat Lua',30,1001,5,50),(2,'Hat Ngo',45,1002,3,80),(3,'Hat Ca Chua',60,1003,4,100);

INSERT IGNORE INTO farm_animals (id,animal_name,product_item_id,produce_time_min,buy_price) VALUES
(1,'Ga',1010,60,200),(2,'Bo',1011,180,500),(3,'Ca',1012,90,300);

INSERT IGNORE INTO furniture_catalog (id,name,furniture_type,price_gold,size) VALUES
(1,'Ban Go','table',500,2),(2,'Giuong','bed',1000,4),(3,'Tu Quan Ao','wardrobe',800,2),
(4,'Den Lung','lamp',200,1),(5,'Cay Canh','plant',150,1);

-- ═════════════════════════════════════════════════════════════
-- 33. OTA ASSET UPDATE — Hệ thống cập nhật client qua mạng
-- ═════════════════════════════════════════════════════════════

-- Mỗi asset (ảnh, config, data) được track version + hash
CREATE TABLE IF NOT EXISTS client_assets (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    asset_key       VARCHAR(256) NOT NULL UNIQUE,  -- VD: "Sprites/Items/item_001.png", "Config/skills.json"
    asset_type      VARCHAR(32) NOT NULL,           -- image,config,audio,data,sprite_atlas,hud,icon,map_tile
    category        VARCHAR(64) NOT NULL DEFAULT 'general', -- items,monsters,npcs,hud,icons,maps,effects,ui,audio,config
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
