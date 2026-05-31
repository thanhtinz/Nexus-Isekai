-- ===================================================
-- NEXUS ISEKAI - Database Schema
-- MySQL 8.0+
-- ===================================================

CREATE DATABASE IF NOT EXISTS nexus_isekai CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE nexus_isekai;

-- ===================================================
-- ACCOUNTS & CHARACTERS
-- ===================================================

CREATE TABLE accounts (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(32) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,   -- BCrypt
    email       VARCHAR(255),
    gold        BIGINT DEFAULT 0,
    premium_points INT DEFAULT 0,
    is_banned   TINYINT DEFAULT 0,
    ban_reason  VARCHAR(255),
    is_admin    TINYINT DEFAULT 0,
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_login  DATETIME
);

CREATE TABLE characters (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id  BIGINT NOT NULL,
    name        VARCHAR(32) NOT NULL UNIQUE,
    class_id    TINYINT NOT NULL,           -- 1=Kiếm Sĩ,2=Sát Thủ,3=Pháp Sư,4=Pháp Thủ,5=Cung Thủ
    level       INT DEFAULT 1,
    exp         BIGINT DEFAULT 0,
    hp          INT DEFAULT 100,
    max_hp      INT DEFAULT 100,
    mp          INT DEFAULT 50,
    max_mp      INT DEFAULT 50,
    str_stat    INT DEFAULT 10,
    agi_stat    INT DEFAULT 10,
    int_stat    INT DEFAULT 10,
    vit_stat    INT DEFAULT 10,
    stat_points INT DEFAULT 0,
    skill_points INT DEFAULT 0,
    map_id      INT DEFAULT 1,
    pos_x       FLOAT DEFAULT 5.0,
    pos_y       FLOAT DEFAULT 5.0,
    story_chapter INT DEFAULT 1,           -- chương cốt truyện theo class
    story_flags  TEXT,                     -- JSON flags sự kiện cốt truyện
    guild_id    BIGINT DEFAULT NULL,
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE
);

CREATE TABLE character_skills (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    char_id     BIGINT NOT NULL,
    skill_id    INT NOT NULL,
    skill_level INT DEFAULT 1,
    UNIQUE KEY uq_char_skill (char_id, skill_id),
    FOREIGN KEY (char_id) REFERENCES characters(id) ON DELETE CASCADE
);

-- ===================================================
-- INVENTORY & ITEMS
-- ===================================================

CREATE TABLE items (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(64) NOT NULL,
    description TEXT,
    type        TINYINT NOT NULL,   -- 1=Vũ Khí,2=Giáp,3=Mũ,4=Nhẫn,5=Tiêu Hao,6=Quest,7=Trang Trí
    class_req   TINYINT DEFAULT 0,  -- 0=tất cả
    level_req   INT DEFAULT 1,
    sell_price  INT DEFAULT 0,
    buy_price   INT DEFAULT 0,
    icon_id     INT DEFAULT 0,      -- ID icon từ asset NRO
    stats_json  TEXT,               -- {"str":5,"agi":3,...}
    is_active   TINYINT DEFAULT 1
);

CREATE TABLE character_inventory (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    char_id     BIGINT NOT NULL,
    item_id     INT NOT NULL,
    qty         INT DEFAULT 1,
    slot        INT DEFAULT -1,     -- -1=bag, 0-9=equipment slots
    enchant     TINYINT DEFAULT 0,
    durability  INT DEFAULT 100,
    FOREIGN KEY (char_id) REFERENCES characters(id) ON DELETE CASCADE,
    FOREIGN KEY (item_id) REFERENCES items(id)
);

-- ===================================================
-- MAPS & WORLD
-- ===================================================

CREATE TABLE maps (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(64) NOT NULL,
    file_name   VARCHAR(128),           -- tên file map asset từ NRO
    width       INT DEFAULT 50,
    height      INT DEFAULT 50,
    min_level   INT DEFAULT 1,
    max_level   INT DEFAULT 999,
    is_pvp      TINYINT DEFAULT 0,
    is_safe     TINYINT DEFAULT 0,      -- safe zone (town)
    bg_music    VARCHAR(64),
    is_active   TINYINT DEFAULT 1
);

CREATE TABLE map_portals (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    map_id      INT NOT NULL,
    pos_x       FLOAT,
    pos_y       FLOAT,
    dest_map_id INT,
    dest_x      FLOAT,
    dest_y      FLOAT,
    level_req   INT DEFAULT 1,
    FOREIGN KEY (map_id) REFERENCES maps(id)
);

-- ===================================================
-- NPCs & MONSTERS
-- ===================================================

CREATE TABLE npcs (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(64) NOT NULL,
    map_id      INT,
    pos_x       FLOAT,
    pos_y       FLOAT,
    npc_type    TINYINT DEFAULT 1,   -- 1=Cửa Hàng,2=Quest,3=Kho,4=Crafting,5=Bank
    dialog_json TEXT,                -- dialog tree JSON
    shop_id     INT DEFAULT NULL,
    icon_id     INT DEFAULT 0,
    is_active   TINYINT DEFAULT 1
);

CREATE TABLE monsters (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(64) NOT NULL,
    level       INT DEFAULT 1,
    hp          INT DEFAULT 100,
    atk         INT DEFAULT 10,
    def         INT DEFAULT 5,
    speed       INT DEFAULT 100,
    exp_reward  INT DEFAULT 10,
    gold_reward INT DEFAULT 5,
    map_id      INT,
    spawn_x     FLOAT,
    spawn_y     FLOAT,
    aggro_range FLOAT DEFAULT 3.0,
    respawn_sec INT DEFAULT 30,
    is_boss     TINYINT DEFAULT 0,
    loot_json   TEXT,               -- [{"item_id":1,"chance":0.1,"qty":1},...]
    icon_id     INT DEFAULT 0,
    is_active   TINYINT DEFAULT 1
);

-- ===================================================
-- QUESTS / STORY
-- ===================================================

CREATE TABLE quests (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(128) NOT NULL,
    description TEXT,
    class_req   TINYINT DEFAULT 0,       -- 0=tất cả, 1-5=class cụ thể
    min_level   INT DEFAULT 1,
    chapter     INT DEFAULT 1,
    quest_type  TINYINT DEFAULT 1,       -- 1=main,2=side,3=daily,4=event
    objectives  TEXT,                    -- JSON mảng objective
    rewards_json TEXT,                   -- {"exp":500,"gold":100,"items":[...]}
    next_quest_id INT DEFAULT NULL,
    is_active   TINYINT DEFAULT 1
);

CREATE TABLE character_quests (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    char_id     BIGINT NOT NULL,
    quest_id    INT NOT NULL,
    status      TINYINT DEFAULT 0,   -- 0=available,1=active,2=complete
    progress    TEXT,                -- JSON progress từng objective
    accepted_at DATETIME,
    completed_at DATETIME,
    UNIQUE KEY uq_char_quest (char_id, quest_id),
    FOREIGN KEY (char_id) REFERENCES characters(id) ON DELETE CASCADE
);

-- ===================================================
-- SHOPS & EVENTS
-- ===================================================

CREATE TABLE shops (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(64) NOT NULL,
    currency    TINYINT DEFAULT 1,       -- 1=gold,2=premium
    is_active   TINYINT DEFAULT 1
);

CREATE TABLE shop_items (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    shop_id     INT NOT NULL,
    item_id     INT NOT NULL,
    price       INT NOT NULL,
    stock       INT DEFAULT -1,          -- -1=unlimited
    FOREIGN KEY (shop_id) REFERENCES shops(id),
    FOREIGN KEY (item_id) REFERENCES items(id)
);

CREATE TABLE events (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(128) NOT NULL,
    event_type  TINYINT DEFAULT 1,       -- 1=boss,2=double_exp,3=drop_rate,4=custom
    start_time  DATETIME,
    end_time    DATETIME,
    repeat_cron VARCHAR(64),             -- cron expression nếu lặp lại
    params_json TEXT,                    -- tham số riêng mỗi loại event
    is_active   TINYINT DEFAULT 1
);

-- ===================================================
-- GUILDS
-- ===================================================

CREATE TABLE guilds (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(32) NOT NULL UNIQUE,
    leader_id   BIGINT NOT NULL,
    description TEXT,
    level       INT DEFAULT 1,
    exp         BIGINT DEFAULT 0,
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE guild_members (
    guild_id    BIGINT NOT NULL,
    char_id     BIGINT NOT NULL,
    role        TINYINT DEFAULT 3,       -- 1=leader,2=officer,3=member
    joined_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (guild_id, char_id),
    FOREIGN KEY (guild_id) REFERENCES guilds(id),
    FOREIGN KEY (char_id) REFERENCES characters(id)
);

-- ===================================================
-- SERVER LOGS (admin)
-- ===================================================

CREATE TABLE server_logs (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    log_type    VARCHAR(32),     -- LOGIN,LOGOUT,CHAT,COMBAT,TRADE,ADMIN,...
    account_id  BIGINT,
    char_name   VARCHAR(32),
    message     TEXT,
    ip_address  VARCHAR(64),
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- ===================================================
-- SEED DATA - Maps
-- ===================================================
INSERT INTO maps (id,name,file_name,width,height,min_level,max_level,is_safe) VALUES
(1,'Làng Khởi Đầu','map_village_start',30,30,1,10,1),
(2,'Rừng Tây','map_forest_west',50,50,5,20,0),
(3,'Hang Động Bóng Tối','map_cave_dark',40,40,15,30,0),
(4,'Đồng Bằng Chiến','map_plains_battle',60,60,25,50,0),
(5,'Tháp Ma Vương','map_demon_tower',50,70,40,80,0),
(6,'Thành Phố Trung Tâm','map_city_central',80,80,1,999,1),
(7,'Chiến Trường PvP','map_pvp_arena',40,40,20,999,0);

UPDATE maps SET is_pvp=1 WHERE id=7;

-- ===================================================
-- SEED DATA - Items cơ bản
-- ===================================================
INSERT INTO items (id,name,description,type,level_req,sell_price,buy_price,icon_id) VALUES
(1001,'Kiếm Gỗ','Vũ khí khởi đầu của kiếm sĩ',1,1,5,50,1),
(1002,'Dao Găm Sắt','Vũ khí khởi đầu của sát thủ',1,1,5,50,2),
(1003,'Gậy Phép','Vũ khí khởi đầu của pháp sư',1,1,5,50,3),
(1004,'Cung Gỗ','Vũ khí khởi đầu của cung thủ',1,1,5,50,4),
(4001,'Bình Máu Nhỏ','Hồi 100 HP',5,1,2,20,1),
(4002,'Bình Mana Nhỏ','Hồi 50 MP',5,1,2,20,2),
(2001,'Áo Giáp Da','Giáp cơ bản',2,1,10,80,1);

-- ===================================================
-- SEED DATA - Classes starter quests (mỗi class có story riêng)
-- ===================================================
INSERT INTO quests (id,name,description,class_req,min_level,chapter,quest_type,objectives,rewards_json,next_quest_id) VALUES
-- Kiếm Sĩ
(101,'[KS] Thức Tỉnh Kiếm Hồn',
 'Con đường của một kiếm sĩ bắt đầu từ sự kiên định. Hãy chứng minh bản thân bằng cách tiêu diệt 5 con sói rừng.',
 1,1,1,1,
 '[{"type":"kill","monster_id":1,"target":5,"desc":"Tiêu diệt 5 Sói Rừng"}]',
 '{"exp":200,"gold":50,"items":[{"id":1,"qty":1}]}',
 102),
(102,'[KS] Bí Thuật Phân Tâm',
 'Sư phụ truyền cho ngươi kỹ năng đầu tiên. Hãy thực hành kỹ năng Chém Xoáy 3 lần trong chiến đấu.',
 1,3,1,1,
 '[{"type":"use_skill","skill_id":101,"target":3,"desc":"Dùng Chém Xoáy 3 lần"}]',
 '{"exp":500,"gold":100,"items":[{"id":5,"qty":5}]}',
 NULL),
-- Sát Thủ
(201,'[ST] Bóng Tối Thức Tỉnh',
 'Ngươi là bóng tối, ngươi là kẻ ẩn mình. Hãy ám sát 3 mục tiêu mà không bị phát hiện.',
 2,1,1,1,
 '[{"type":"kill","monster_id":2,"target":3,"desc":"Tiêu diệt 3 Goblin từ phía sau"}]',
 '{"exp":200,"gold":50,"items":[{"id":2,"qty":1}]}',
 202),
(202,'[ST] Độc Tố Đầu Tiên',
 'Một sát thủ cần biết dùng độc. Hãy thu thập 3 túi nọc nhện.',
 2,3,1,1,
 '[{"type":"collect","item_id":99,"target":3,"desc":"Thu thập 3 Túi Nọc Nhện"}]',
 '{"exp":500,"gold":100}',
 NULL),
-- Pháp Sư
(301,'[PS] Ngọn Lửa Đầu Tiên',
 'Phép thuật là con đường của tri thức. Hãy dùng Cầu Lửa để tiêu diệt 5 con quỷ bùn.',
 3,1,1,1,
 '[{"type":"kill_with_skill","skill_id":201,"monster_id":3,"target":5,"desc":"Dùng Cầu Lửa giết 5 Quỷ Bùn"}]',
 '{"exp":200,"gold":50,"items":[{"id":3,"qty":1},{"id":6,"qty":5}]}',
 302),
(302,'[PS] Bí Thư Cổ Đại',
 'Một cuốn bí thư bị đánh mất trong hang động. Hãy tìm lại nó.',
 3,5,1,1,
 '[{"type":"collect","item_id":98,"target":1,"desc":"Tìm Bí Thư Cổ Đại"}]',
 '{"exp":800,"gold":200,"skill_points":1}',
 NULL),
-- Pháp Thủ
(401,'[PT] Giữa Hai Thế Giới',
 'Ngươi đứng giữa phép thuật và chiến đấu. Hãy dùng cả kỹ năng vật lý lẫn phép thuật để chiến thắng 5 trận.',
 4,1,1,1,
 '[{"type":"win_battle","target":5,"desc":"Thắng 5 trận đấu"}]',
 '{"exp":200,"gold":50}',
 NULL),
-- Cung Thủ
(501,'[CT] Mắt Đại Bàng',
 'Cung thủ cần đôi mắt tinh tường. Hãy bắn hạ 5 mục tiêu từ khoảng cách tối đa.',
 5,1,1,1,
 '[{"type":"long_range_kill","target":5,"desc":"Hạ 5 mục tiêu từ xa"}]',
 '{"exp":200,"gold":50,"items":[{"id":4,"qty":1}]}',
 NULL);

-- Monsters seed
INSERT INTO monsters (id,name,level,hp,atk,def,speed,exp_reward,gold_reward,map_id,spawn_x,spawn_y,respawn_sec,is_boss,icon_id) VALUES
(1,'Sói Rừng',3,80,15,5,120,15,8,2,10,10,30,0,1001),
(2,'Goblin',2,60,12,3,100,10,5,2,15,15,25,0,1002),
(3,'Quỷ Bùn',4,100,18,8,90,20,10,3,10,10,35,0,1003),
(4,'Nhện Độc',6,120,22,10,110,25,12,3,20,20,40,0,1004),
(5,'Tên Lính Xương',10,200,30,15,100,40,20,3,15,15,60,0,1005),
(10,'[BOSS] Vua Sói',20,5000,100,40,80,500,200,2,25,25,600,1,1010),
(11,'[BOSS] Tướng Goblin',15,3000,80,30,90,300,150,2,30,30,600,1,1011),
(20,'[BOSS] Ma Vương Lv1',50,50000,300,120,70,5000,2000,5,25,35,3600,1,1020);

-- Shops seed
INSERT INTO shops (id,name,currency) VALUES (1,'Cửa Hàng Chung',1),(2,'Shop Premium',2);
INSERT INTO shop_items (shop_id,item_id,price,stock) VALUES
(1,4001,20,-1),(1,4002,20,-1),(1,2001,80,-1),
(2,4001,1,-1),(2,4002,1,-1);

-- NPC seed
INSERT INTO npcs (id,name,map_id,pos_x,pos_y,npc_type,shop_id) VALUES
(1,'Người Bán Đồ Làng',1,5,5,1,1),
(2,'Hướng Dẫn Viên',1,7,5,2,NULL),
(3,'Kho Đồ',1,3,5,3,NULL),
(4,'Người Bán Đồ Thành Phố',6,10,10,1,1),
(5,'Merchant Premium',6,12,10,1,2);
