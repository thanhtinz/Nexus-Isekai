CREATE TABLE IF NOT EXISTS assets (
  id SERIAL PRIMARY KEY,
  name VARCHAR(128) NOT NULL,
  type VARCHAR(32) NOT NULL,        -- sprite|animation|tile|ui|effect|model
  category VARCHAR(64),             -- character|npc|mob|item|environment|ui
  file_path VARCHAR(512),
  file_size BIGINT,
  mime_type VARCHAR(64),
  width INT, height INT,
  tags TEXT,
  server_id INT REFERENCES game_servers(id) ON DELETE SET NULL,
  uploaded_by INT REFERENCES admin_users(id),
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS items (
  id SERIAL PRIMARY KEY,
  item_id BIGINT UNIQUE NOT NULL,
  name VARCHAR(128) NOT NULL,
  name_vn VARCHAR(128),
  type VARCHAR(32) NOT NULL,        -- weapon|armor|consumable|material|seed|fish|tool|accessory|pet_food|special
  category VARCHAR(64),
  rarity SMALLINT DEFAULT 0,        -- 0=common 1=uncommon 2=rare 3=epic 4=legendary
  base_price BIGINT DEFAULT 100,
  sell_price BIGINT DEFAULT 50,
  stackable BOOLEAN DEFAULT TRUE,
  tradeable BOOLEAN DEFAULT TRUE,
  description TEXT,
  icon_asset_id INT REFERENCES assets(id),
  stats JSONB DEFAULT '{}',         -- {atk:5, def:2, speed:1}
  craft_recipe JSONB,               -- {ingredients:[{itemId,qty}], profType, levelReq}
  drop_sources TEXT,                -- JSON array of mob/npc ids
  server_id INT REFERENCES game_servers(id) ON DELETE SET NULL,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS npcs (
  id SERIAL PRIMARY KEY,
  template_id INT UNIQUE NOT NULL,
  name VARCHAR(64) NOT NULL,
  name_vn VARCHAR(64),
  type VARCHAR(32) DEFAULT 'merchant', -- merchant|quest|banker|guard|trainer|special
  faction_id INT DEFAULT 0,
  zone_id INT DEFAULT 1,
  default_x FLOAT DEFAULT 100,
  default_y FLOAT DEFAULT 100,
  sprite_asset_id INT REFERENCES assets(id),
  has_shop BOOLEAN DEFAULT FALSE,
  shop_items JSONB DEFAULT '[]',    -- [{itemId, price, stock}]
  has_dialog BOOLEAN DEFAULT TRUE,
  schedule JSONB DEFAULT '[]',      -- [{time, tx, ty, activity}]
  description TEXT,
  server_id INT REFERENCES game_servers(id) ON DELETE SET NULL,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS npc_dialogs (
  id SERIAL PRIMARY KEY,
  npc_id INT REFERENCES npcs(id) ON DELETE CASCADE,
  node_key VARCHAR(64) NOT NULL,    -- 'start', 'shop', 'quest1', etc.
  text TEXT NOT NULL,
  choices JSONB DEFAULT '[]',       -- [{text, nextNode, action}]
  condition VARCHAR(256),           -- 'level>=10', 'faction==1', etc.
  voice_asset_id INT REFERENCES assets(id),
  lang VARCHAR(8) DEFAULT 'vi',
  created_at TIMESTAMPTZ DEFAULT NOW(),
  UNIQUE(npc_id, node_key, lang)
);

CREATE TABLE IF NOT EXISTS mobs (
  id SERIAL PRIMARY KEY,
  template_id INT UNIQUE NOT NULL,
  name VARCHAR(64) NOT NULL,
  name_vn VARCHAR(64),
  type VARCHAR(32) DEFAULT 'normal', -- normal|elite|boss|miniboss|passive
  faction VARCHAR(32),
  zone_ids INT[] DEFAULT '{}',
  level_min INT DEFAULT 1,
  level_max INT DEFAULT 5,
  hp INT DEFAULT 100,
  atk INT DEFAULT 10,
  def INT DEFAULT 5,
  speed FLOAT DEFAULT 3.0,
  exp_reward INT DEFAULT 50,
  gold_reward_min INT DEFAULT 10,
  gold_reward_max INT DEFAULT 50,
  drop_table JSONB DEFAULT '[]',    -- [{itemId, chance, minQty, maxQty}]
  sprite_asset_id INT REFERENCES assets(id),
  ai_type VARCHAR(32) DEFAULT 'passive', -- passive|aggressive|patrol|boss
  respawn_secs INT DEFAULT 300,
  description TEXT,
  server_id INT REFERENCES game_servers(id) ON DELETE SET NULL,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS maps (
  id SERIAL PRIMARY KEY,
  zone_id INT UNIQUE NOT NULL,
  name VARCHAR(64) NOT NULL,
  name_vn VARCHAR(64),
  type VARCHAR(32) DEFAULT 'overworld', -- overworld|dungeon|city|instance|pvp
  width INT DEFAULT 300,
  height INT DEFAULT 300,
  tileset_asset_id INT REFERENCES assets(id),
  bg_music_id INT REFERENCES assets(id),
  ambient_sound_id INT REFERENCES assets(id),
  spawn_points JSONB DEFAULT '[]',  -- [{x,y,label}]
  portals JSONB DEFAULT '[]',       -- [{x,y,radius,toZone,toX,toY,label}]
  npc_spawns JSONB DEFAULT '[]',    -- [{templateId,x,y}]
  mob_spawns JSONB DEFAULT '[]',    -- [{templateId,x,y,count,radius}]
  properties JSONB DEFAULT '{}',    -- {isPvp, minLevel, isIndoor, weather}
  thumbnail_asset_id INT REFERENCES assets(id),
  server_id INT REFERENCES game_servers(id) ON DELETE SET NULL,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS professions (
  id SERIAL PRIMARY KEY,
  code VARCHAR(32) UNIQUE NOT NULL, -- FISHER, CHEF, etc.
  name VARCHAR(64) NOT NULL,
  name_vn VARCHAR(64),
  category VARCHAR(32) DEFAULT 'civilian', -- civilian|combat|special
  max_level INT DEFAULT 10,
  is_special BOOLEAN DEFAULT FALSE,
  faction_required INT DEFAULT 0,
  icon_asset_id INT REFERENCES assets(id),
  description TEXT,
  skills JSONB DEFAULT '[]',        -- [{level, name, description, effect}]
  exp_per_action JSONB DEFAULT '{}',-- {fish:5, sell:2}
  unlock_condition VARCHAR(256),
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS audio (
  id SERIAL PRIMARY KEY,
  name VARCHAR(128) NOT NULL,
  type VARCHAR(32) NOT NULL,        -- bgm|sfx|voice|ambient
  category VARCHAR(64),             -- combat|ui|nature|npc|event|zone
  file_path VARCHAR(512),
  file_size BIGINT,
  duration_secs FLOAT,
  format VARCHAR(16),               -- mp3|ogg|wav
  loop BOOLEAN DEFAULT FALSE,
  volume_default FLOAT DEFAULT 1.0,
  tags TEXT,
  zone_id INT,                      -- if bg music for a zone
  server_id INT REFERENCES game_servers(id) ON DELETE SET NULL,
  uploaded_by INT REFERENCES admin_users(id),
  created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_items_type    ON items(type,rarity);
CREATE INDEX IF NOT EXISTS idx_items_svid    ON items(server_id);
CREATE INDEX IF NOT EXISTS idx_npcs_zone     ON npcs(zone_id);
CREATE INDEX IF NOT EXISTS idx_mobs_zone     ON mobs USING GIN(zone_ids);
CREATE INDEX IF NOT EXISTS idx_dialogs_npc   ON npc_dialogs(npc_id);
CREATE INDEX IF NOT EXISTS idx_audio_type    ON audio(type,category);
CREATE INDEX IF NOT EXISTS idx_assets_type   ON assets(type,category);
-- ============================================================
-- CHARACTER CREATION — cấu hình màn tạo nhân vật đầu game
-- ============================================================

-- Chủng tộc + giới tính (mỗi tổ hợp = 1 option base)
CREATE TABLE IF NOT EXISTS char_races (
  id SERIAL PRIMARY KEY,
  code VARCHAR(32) UNIQUE NOT NULL,     -- humn_male, humn_female, demn_male...
  race VARCHAR(32) NOT NULL,            -- humn / demn / gbln / elf
  race_name_vn VARCHAR(64),            -- Con Người / Ma Tộc...
  gender VARCHAR(8) NOT NULL,           -- male / female
  faction_id INT DEFAULT 0,             -- 0 = mọi phe; hoặc khóa theo phe
  base_asset_id INT REFERENCES assets(id),  -- sprite body base (0bas)
  preview_asset_id INT REFERENCES assets(id),
  is_enabled BOOLEAN DEFAULT TRUE,
  sort_order INT DEFAULT 0,
  description TEXT,
  created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Các lựa chọn ngoại hình cơ bản (da, mắt, tóc, áo quần khởi đầu)
CREATE TABLE IF NOT EXISTS char_options (
  id SERIAL PRIMARY KEY,
  slot VARCHAR(16) NOT NULL,            -- skin | eyes | hair | outfit
  code VARCHAR(48) NOT NULL,            -- humn_v03, eye_blue, bob1_v02...
  name_vn VARCHAR(64),                 -- "Da sáng", "Mắt xanh", "Tóc bob"
  asset_id INT REFERENCES assets(id),  -- sprite layer tương ứng
  color_index INT,                      -- index màu (cho eyes: 0-10)
  hex_preview VARCHAR(9),               -- màu hiển thị nhanh (vd #4488ff)
  race_filter VARCHAR(64),              -- để trống = mọi chủng tộc; hoặc 'humn,elf'
  gender_filter VARCHAR(16),            -- để trống = mọi giới; 'male'/'female'
  is_default BOOLEAN DEFAULT FALSE,     -- lựa chọn mặc định khi mở màn tạo
  is_enabled BOOLEAN DEFAULT TRUE,
  sort_order INT DEFAULT 0,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  UNIQUE(slot, code)
);

CREATE INDEX IF NOT EXISTS idx_charopt_slot ON char_options(slot, is_enabled);
CREATE INDEX IF NOT EXISTS idx_charrace_enabled ON char_races(is_enabled, sort_order);
