// NexusIsekai Client - PacketHandlers.cs
// Xử lý tất cả S2C packet từ server

using System.Collections.Generic;
using NexusIsekai.Data;
using NexusIsekai.Network;
using NexusIsekai.UI;
using UnityEngine;
using UnityEngine.SceneManagement;

namespace NexusIsekai.Game
{
    /// <summary>
    /// MonoBehaviour đăng ký tất cả packet handler vào PacketDispatcher.
    /// Gắn vào một GameObject tồn tại xuyên suốt vòng đời game.
    /// </summary>
    public class PacketHandlers : MonoBehaviour
    {
        public static PacketHandlers Instance { get; private set; }

        private void Awake()
        {
            if (Instance != null && Instance != this) { Destroy(gameObject); return; }
            Instance = this;
            DontDestroyOnLoad(gameObject);
        }

        private void Start()
        {
            var d = PacketDispatcher.Instance;

            // Auth
            d.Register(PacketOpcode.S2C_LOGIN_OK,       OnLoginOk);
            d.Register(PacketOpcode.S2C_LOGIN_FAIL,      OnLoginFail);
            d.Register(PacketOpcode.S2C_REGISTER_OK,     OnRegisterOk);
            d.Register(PacketOpcode.S2C_REGISTER_FAIL,   OnRegisterFail);

            // Char select
            d.Register(PacketOpcode.S2C_CHAR_LIST,       OnCharList);
            d.Register(PacketOpcode.S2C_CHAR_CREATE_OK,  OnCharCreateOk);
            d.Register(PacketOpcode.S2C_CHAR_CREATE_FAIL,OnCharCreateFail);
            d.Register(PacketOpcode.S2C_CHAR_DELETE_OK,  OnCharDeleteOk);
            d.Register(PacketOpcode.S2C_CHAR_ENTER_GAME, OnEnterGame);
            d.Register(PacketOpcode.S2C_INTRO_VIDEO_CONFIG, OnIntroVideoConfig);
            d.Register(PacketOpcode.S2C_FARM_VISIT,   OnFarmVisit);
            d.Register(PacketOpcode.S2C_ANIMAL_BREED, OnAnimalBreed);
            d.Register(PacketOpcode.S2C_CHILD_SHOP,      OnChildShop);
            d.Register(PacketOpcode.S2C_CHILD_BUY,        OnChildBuy);
            d.Register(PacketOpcode.S2C_CHILD_INTERACT,   OnChildInteract);
            d.Register(PacketOpcode.S2C_CHILD_NPC_MOVE,   OnChildNpcMove);
            // Tương tác nội thất
            d.Register(PacketOpcode.S2C_FURNITURE_INTERACT, OnFurnitureInteract);
            d.Register(PacketOpcode.S2C_FURNITURE_STOP,     OnFurnitureStop);
            d.Register(PacketOpcode.S2C_FURNITURE_BUY,      OnFurnitureBuy);
            // Facility maps + cổng dịch chuyển
            d.Register(PacketOpcode.S2C_FACILITY_PORTALS, OnFacilityPortals);
            d.Register(PacketOpcode.S2C_FACILITY_ENTER,   OnFacilityEnter);
            d.Register(PacketOpcode.S2C_FACILITY_LEFT,    OnFacilityLeft);
            // Progression: Cánh/Hào quang, Danh vọng, Bestiary, Set
            d.Register(PacketOpcode.S2C_COSMETIC_LIST,      OnCosmeticList);
            d.Register(PacketOpcode.S2C_COSMETIC_EQUIP,     OnCosmeticEquip);
            d.Register(PacketOpcode.S2C_COSMETIC_UPGRADE,   OnCosmeticUpgrade);
            d.Register(PacketOpcode.S2C_REPUTATION_LIST,    OnReputationList);
            d.Register(PacketOpcode.S2C_REPUTATION_CLAIM,   OnReputationClaim);
            d.Register(PacketOpcode.S2C_REPUTATION_GAIN,    OnReputationGain);
            d.Register(PacketOpcode.S2C_BESTIARY_LIST,      OnBestiaryList);
            d.Register(PacketOpcode.S2C_BESTIARY_CLAIM,     OnBestiaryClaim);
            d.Register(PacketOpcode.S2C_BESTIARY_UNLOCK,    OnBestiaryUnlock);
            d.Register(PacketOpcode.S2C_SET_INFO,           OnSetInfo);
            d.Register(PacketOpcode.S2C_SET_BONUS_UPDATE,   OnSetBonusUpdate);
            // Extended gameplay S2C
            d.Register(PacketOpcode.S2C_AUTO_CONFIG,        OnAutoConfig);
            d.Register(PacketOpcode.S2C_AUTO_PLAY_STATE,    OnAutoPlayState);
            d.Register(PacketOpcode.S2C_BLOCK_OK,           OnBlockOk);
            d.Register(PacketOpcode.S2C_CHAR_ACTION,        OnCharAction);
            d.Register(PacketOpcode.S2C_EMOTE_SHOW,         OnEmoteShow);
            d.Register(PacketOpcode.S2C_GEM_SOCKET_OK,      OnGemSocketOk);
            d.Register(PacketOpcode.S2C_INSPECT_DATA,       OnInspectData);
            d.Register(PacketOpcode.S2C_NEWS_LIST,          OnNewsList);
            d.Register(PacketOpcode.S2C_PAIR_ACTION_REQ,    OnPairActionReq);
            d.Register(PacketOpcode.S2C_PAIR_ACTION_PLAY,   OnPairActionPlay);
            d.Register(PacketOpcode.S2C_REFINE_OK,          OnRefineOk);
            d.Register(PacketOpcode.S2C_TELEPORT_OK,        OnTeleportOk);
            d.Register(PacketOpcode.S2C_WAREHOUSE_DATA,     OnWarehouseData);
            d.Register(PacketOpcode.S2C_CLASS_STORY,     OnClassStory);

            // World
            d.Register(PacketOpcode.S2C_MAP_DATA,        OnMapData);
            d.Register(PacketOpcode.S2C_PLAYERS_IN_ZONE, OnPlayersInZone);
            d.Register(PacketOpcode.S2C_MONSTERS_IN_ZONE,OnMonstersInZone);
            d.Register(PacketOpcode.S2C_PLAYER_ENTER,    OnPlayerEnter);
            d.Register(PacketOpcode.S2C_PLAYER_LEAVE,    OnPlayerLeave);
            d.Register(PacketOpcode.S2C_PLAYER_MOVE,     OnPlayerMove);
            d.Register(PacketOpcode.S2C_POSITION_CORRECT,OnPositionCorrect);
            d.Register(PacketOpcode.S2C_MAP_CHANGE_FAILED,OnMapChangeFailed);

            // Combat
            d.Register(PacketOpcode.S2C_SKILL_RESULT,    OnSkillResult);
            d.Register(PacketOpcode.S2C_MONSTER_RESPAWN, OnMonsterRespawn);
            d.Register(PacketOpcode.S2C_LEVEL_UP,        OnLevelUp);
            d.Register(PacketOpcode.S2C_MONSTER_MOVE,    OnMonsterMove);

            // Inventory
            d.Register(PacketOpcode.S2C_INVENTORY_LIST,  OnInventoryList);
            d.Register(PacketOpcode.S2C_PLAYER_STATS,    OnPlayerStats);
            d.Register(PacketOpcode.S2C_SHOP_DATA,       OnShopData);
            d.Register(PacketOpcode.S2C_ITEM_ERROR,      OnItemError);

            // Quest
            d.Register(PacketOpcode.S2C_QUEST_LIST,      OnQuestList);
            d.Register(PacketOpcode.S2C_QUEST_ACCEPTED,  OnQuestAccepted);
            d.Register(PacketOpcode.S2C_QUEST_ERROR,     OnQuestError);
            d.Register(PacketOpcode.S2C_QUEST_PROGRESS,  OnQuestProgress);

            // Chat (mở rộng)
            d.Register(PacketOpcode.S2C_CHAT,             OnChat);
            d.Register(PacketOpcode.S2C_SYSTEM_MSG,       OnSystemMsg);
            d.Register(PacketOpcode.S2C_CHAT_RED_ENVELOPE,OnChatRedEnvelope);
            d.Register(PacketOpcode.S2C_CHAT_GRABBED,     OnChatGrabbed);
            d.Register(PacketOpcode.S2C_CHAT_GRAB_RESULT, OnChatGrabResult);
            d.Register(PacketOpcode.S2C_STICKER_LIST,     OnStickerList);

            // Guild (8xx)
            d.Register(PacketOpcode.S2C_GUILD_INFO,     OnGuildInfo);
            d.Register(PacketOpcode.S2C_GUILD_MEMBERS,  OnGuildMembers);
            d.Register(PacketOpcode.S2C_GUILD_INVITED,  OnGuildInvited);

            // Skill extended (9xx)
            d.Register(PacketOpcode.S2C_SKILL_LIST,       OnSkillList);
            d.Register(PacketOpcode.S2C_CLASS_SKILL_LIST, OnClassSkillList);

            // Enhancement (Axx)
            d.Register(PacketOpcode.S2C_ENHANCE_RESULT, OnEnhanceResult);

            // PvP (Bxx)
            d.Register(PacketOpcode.S2C_PVP_REQUEST,       OnPvpRequest);
            d.Register(PacketOpcode.S2C_PVP_START,         OnPvpStart);
            d.Register(PacketOpcode.S2C_PVP_COMBAT_RESULT, OnPvpCombatResult);
            d.Register(PacketOpcode.S2C_PVP_END,           OnPvpEnd);

            // Minigame (Cxx)
            d.Register(PacketOpcode.S2C_MINIGAME_ROOM_LIST,  OnMinigameRoomList);
            d.Register(PacketOpcode.S2C_MINIGAME_ROOM_UPDATE, OnMinigameRoomUpdate);
            d.Register(PacketOpcode.S2C_MINIGAME_BET,         OnMinigameBet);
            d.Register(PacketOpcode.S2C_MINIGAME_RESULT,      OnMinigameResult);

            // Farming (Dxx)
            d.Register(PacketOpcode.S2C_FARM_STATE,  OnFarmState);
            d.Register(PacketOpcode.S2C_FARM_UPDATE, OnFarmUpdate);

            // Housing (Exx)
            d.Register(PacketOpcode.S2C_HOUSE_INFO,      OnHouseInfo);
            d.Register(PacketOpcode.S2C_HOUSE_FURNITURE, OnHouseFurniture);
            d.Register(PacketOpcode.S2C_HOUSE_CATALOG,   OnHouseCatalog);

            // Leaderboard (Fxx)
            d.Register(PacketOpcode.S2C_LEADERBOARD, OnLeaderboard);

            // Trade
            d.Register(PacketOpcode.S2C_TRADE_REQUEST,       OnTradeRequest);
            d.Register(PacketOpcode.S2C_TRADE_UPDATE,        OnTradeUpdate);
            d.Register(PacketOpcode.S2C_TRADE_RESULT,        OnTradeResult);
            // Auction
            d.Register(PacketOpcode.S2C_AUCTION_LIST,        OnAuctionList);
            d.Register(PacketOpcode.S2C_AUCTION_RESULT,      OnAuctionResult);
            // Party
            d.Register(PacketOpcode.S2C_PARTY_INFO,          OnPartyInfo);
            d.Register(PacketOpcode.S2C_PARTY_INVITED,       OnPartyInvited);
            d.Register(PacketOpcode.S2C_PARTY_UPDATE,        OnPartyUpdate);
            // Dungeon
            d.Register(PacketOpcode.S2C_DUNGEON_LIST,        OnDungeonList);
            d.Register(PacketOpcode.S2C_DUNGEON_ENTER_OK,    OnDungeonEnterOk);
            d.Register(PacketOpcode.S2C_DUNGEON_RESULT,      OnDungeonResult);
            d.Register(PacketOpcode.S2C_DUNGEON_TIMER,       OnDungeonTimer);
            // Dialog
            d.Register(PacketOpcode.S2C_DIALOG_SHOW,         OnDialogShow);
            d.Register(PacketOpcode.S2C_DIALOG_OPTIONS,      OnDialogOptions);
            // Announcements
            d.Register(PacketOpcode.S2C_ANNOUNCEMENT_LIST,   OnAnnouncementList);
            d.Register(PacketOpcode.S2C_ANNOUNCEMENT_NEW,    OnAnnouncementNew);
            d.Register(PacketOpcode.S2C_SYSTEM_EVENT_LOG,    OnSystemEventLog);
            // Event Currency
            d.Register(PacketOpcode.S2C_EVENT_CURRENCY_LIST, OnEventCurrencyList);
            d.Register(PacketOpcode.S2C_EVENT_CURRENCY_SHOP, OnEventCurrencyShop);
            d.Register(PacketOpcode.S2C_EVENT_CURRENCY_UPDATE, OnEventCurrencyUpdate);


            d.Register(PacketOpcode.S2C_TOPUP_PACKAGES,  OnTopupPackages);
            d.Register(PacketOpcode.S2C_TOPUP_URL,       OnTopupUrl);
            d.Register(PacketOpcode.S2C_TOPUP_SUCCESS,   OnTopupSuccess);
            d.Register(PacketOpcode.S2C_TOPUP_HISTORY,   OnTopupHistory);
            d.Register(PacketOpcode.S2C_SERVER_LIST,      OnServerList);
            d.Register(PacketOpcode.S2C_CHANNEL_LIST,     OnChannelList);
            d.Register(PacketOpcode.S2C_SERVER_FULL,      OnServerFull);
            d.Register(PacketOpcode.S2C_CHANNEL_CHANGED,  OnChannelChanged);
            d.Register(PacketOpcode.S2C_INTRO_SCENES,     OnIntroScenes);
            d.Register(PacketOpcode.S2C_INTRO_NOT_NEEDED, OnIntroNotNeeded);
            d.Register(PacketOpcode.S2C_LOGIN_SCREEN_CFG, OnLoginScreenCfg);
            // Gacha + PvP Season + Social + Tutorial + Lang
            d.Register(PacketOpcode.S2C_GACHA_BANNER_LIST, OnGachaBannerList);
            d.Register(PacketOpcode.S2C_GACHA_RESULT,      OnGachaResult);
            d.Register(PacketOpcode.S2C_GACHA_CURRENCY,   OnGachaCurrency);
            d.Register(PacketOpcode.S2C_GACHA_HISTORY,     OnGachaHistory);
            d.Register(PacketOpcode.S2C_PVP_SEASON_INFO,   OnPvpSeasonInfo);
            d.Register(PacketOpcode.S2C_PVP_SEASON_RANK,   OnPvpSeasonRank);
            d.Register(PacketOpcode.S2C_SOCIAL_LOGIN_OK,   OnSocialLoginOk);
            d.Register(PacketOpcode.S2C_SOCIAL_LINK_OK,    OnSocialLinkOk);
            d.Register(PacketOpcode.S2C_TUTORIAL_STEP,     OnTutorialStep);
            d.Register(PacketOpcode.S2C_TUTORIAL_COMPLETE,  OnTutorialComplete);
            d.Register(PacketOpcode.S2C_LANG_PACK,         OnLangPack);
            d.Register(PacketOpcode.S2C_SETTINGS_DATA,     OnSettingsData);
            d.Register(PacketOpcode.S2C_SETTINGS_DEFAULTS, OnSettingsDefaults);
            d.Register(PacketOpcode.S2C_CLASS_CHANGE_OK, OnClassChangeOk);
            // Achievement + Daily Login + World Boss + Mail
            d.Register(PacketOpcode.S2C_ACHIEVEMENT_LIST,   OnAchievementList);
            d.Register(PacketOpcode.S2C_ACHIEVEMENT_UPDATE, OnAchievementUpdate);
            d.Register(PacketOpcode.S2C_ACHIEVEMENT_UNLOCK, OnAchievementUnlock);
            d.Register(PacketOpcode.S2C_DAILY_LOGIN_INFO,   OnDailyLoginInfo);
            d.Register(PacketOpcode.S2C_DAILY_LOGIN_CLAIMED,OnDailyLoginClaimed);
            d.Register(PacketOpcode.S2C_WORLD_BOSS_SPAWN,   OnWorldBossSpawn);
            d.Register(PacketOpcode.S2C_WORLD_BOSS_DEAD,    OnWorldBossDead);
            d.Register(PacketOpcode.S2C_WORLD_BOSS_UPDATE,  OnWorldBossUpdate);
            d.Register(PacketOpcode.S2C_MAIL_LIST,          OnMailList);
            d.Register(PacketOpcode.S2C_MAIL_NEW,           OnMailNew);
            // Core game handlers
            d.Register(PacketOpcode.S2C_CHAR_ERROR,         OnCharError);
            d.Register(PacketOpcode.S2C_INVENTORY_UPDATE,   OnInventoryUpdate);
            d.Register(PacketOpcode.S2C_MONSTER_LIST,       OnMonsterList);
            d.Register(PacketOpcode.S2C_NPC_LIST,           OnNpcList);
            d.Register(PacketOpcode.S2C_MONSTER_HP_UPDATE,  OnMonsterHpUpdate);
            d.Register(PacketOpcode.S2C_MONSTER_DIE,        OnMonsterDie);
            d.Register(PacketOpcode.S2C_PLAYER_HP_UPDATE,   OnPlayerHpUpdate);
            d.Register(PacketOpcode.S2C_PLAYER_DIE,         OnPlayerDie);
            d.Register(PacketOpcode.S2C_PLAYER_REVIVE,      OnPlayerRevive);
            d.Register(PacketOpcode.S2C_PLAYER_ONLINE,      OnPlayerOnline);
            d.Register(PacketOpcode.S2C_COMBAT_RESULT,      OnCombatResult);
            d.Register(PacketOpcode.S2C_EXP_GAIN,           OnExpGain);
            d.Register(PacketOpcode.S2C_QUEST_UPDATE,       OnQuestUpdate);
            d.Register(PacketOpcode.S2C_QUEST_COMPLETE,     OnQuestComplete);
            d.Register(PacketOpcode.S2C_SHOP_RESULT,        OnShopResult);
            d.Register(PacketOpcode.S2C_SKILL_EFFECT,       OnSkillEffect);
            d.Register(PacketOpcode.S2C_SKILL_COOLDOWN,     OnSkillCooldown);
            d.Register(PacketOpcode.S2C_GUILD_MSG,          OnGuildMsg);
            d.Register(PacketOpcode.S2C_EVENT_START,        OnEventStart);
            d.Register(PacketOpcode.S2C_SERVER_MSG,         OnServerMsg);
            d.Register(PacketOpcode.S2C_STORY_CG,           OnStoryCg);
            d.Register(PacketOpcode.S2C_PING_FROM_SERVER,   OnPingFromServer);

            // System
            d.Register(PacketOpcode.S2C_PONG,            OnPong);
            d.Register(PacketOpcode.S2C_KICK,            OnKick);
            d.Register(PacketOpcode.S2C_MAINTENANCE,     OnMaintenance);

            // Payment / Diamond (Axx)
            d.Register(PacketOpcode.S2C_TOPUP_OK,        OnTopupOk);
            d.Register(PacketOpcode.S2C_DIAMOND_UPDATE,  OnDiamondUpdate);
            d.Register(PacketOpcode.S2C_GIFTCODE_OK,     OnGiftcodeOk);
            d.Register(PacketOpcode.S2C_GIFTCODE_FAIL,   OnGiftcodeFail);

            // Mission Pass (Bxx)
            d.Register(PacketOpcode.S2C_PASS_INFO,       OnPassInfo);
            d.Register(PacketOpcode.S2C_PASS_CLAIM_OK,   OnPassClaimOk);
            d.Register(PacketOpcode.S2C_PASS_LEVEL_UP,   OnPassLevelUp);

            // Title (Cxx)
            d.Register(PacketOpcode.S2C_TITLE_LIST,      OnTitleList);
            d.Register(PacketOpcode.S2C_TITLE_GRANT,     OnTitleGrant);

            // Pet & Mount (Dxx)
            d.Register(PacketOpcode.S2C_PET_LIST,        OnPetList);
            d.Register(PacketOpcode.S2C_PET_UPDATE,      OnPetUpdate);
            d.Register(PacketOpcode.S2C_MOUNT_LIST,      OnMountList);

            // Social (Exx)
            d.Register(PacketOpcode.S2C_RELATIONSHIP,    OnRelationship);
            d.Register(PacketOpcode.S2C_WEDDING_EVENT,   OnWeddingEvent);
            d.Register(PacketOpcode.S2C_CHILD_LIST,      OnChildList);

            // Mentor (Fxx)
            d.Register(PacketOpcode.S2C_MENTOR_INFO,     OnMentorInfo);
            d.Register(PacketOpcode.S2C_MENTOR_GRADUATE, OnMentorGraduate);
        }

        // ─────────────────────────────────────────────────────────────────
        // AUTH
        // ─────────────────────────────────────────────────────────────────

        private void OnLoginOk(PacketReader r)
        {
            string username = r.ReadString();
            GameState.Instance.LoggedInUsername = username;
            GameState.Instance.IsLoggedIn       = true;
            Debug.Log($"[Auth] Logged in as {username}");
            SceneManager.LoadScene("CharSelect");
        }

        private void OnLoginFail(PacketReader r)
        {
            string reason = r.ReadString();
            UIManager.Instance?.ShowNotification("Đăng nhập thất bại: " + reason, UINotificationType.Error);
        }

        private void OnRegisterOk(PacketReader r)
        {
            UIManager.Instance?.ShowNotification("Đăng ký thành công! Hãy đăng nhập.", UINotificationType.Success);
        }

        private void OnRegisterFail(PacketReader r)
        {
            string reason = r.ReadString();
            UIManager.Instance?.ShowNotification("Đăng ký thất bại: " + reason, UINotificationType.Error);
        }

        // ─────────────────────────────────────────────────────────────────
        // CHAR SELECT
        // ─────────────────────────────────────────────────────────────────

        private void OnCharList(PacketReader r)
        {
            int count = r.ReadShort();
            var list  = new List<CharSlot>(count);
            for (int i = 0; i < count; i++)
            {
                list.Add(new CharSlot {
                    CharId  = r.ReadInt(),
                    Name    = r.ReadString(),
                    ClassId = r.ReadByte(),
                    Level   = r.ReadInt(),
                    MapId   = r.ReadInt()
                });
            }
            GameState.Instance.CharSlots = list;
            CharSelectUI.Instance?.RefreshList(list);
        }

        private void OnCharCreateOk(PacketReader r)
        {
            UIManager.Instance?.ShowNotification("Tạo nhân vật thành công!", UINotificationType.Success);
            // Tự động yêu cầu lại danh sách char
            GameClient.Instance.Send(new PacketBuilder(PacketOpcode.C2S_CHAR_LIST));
        }

        private void OnCharCreateFail(PacketReader r)
        {
            string reason = r.ReadString();
            UIManager.Instance?.ShowNotification("Tạo nhân vật thất bại: " + reason, UINotificationType.Error);
        }

        private void OnCharDeleteOk(PacketReader r)
        {
            UIManager.Instance?.ShowNotification("Xóa nhân vật thành công.", UINotificationType.Info);
            GameClient.Instance.Send(new PacketBuilder(PacketOpcode.C2S_CHAR_LIST));
        }

        private void OnEnterGame(PacketReader r)
        {
            // Đọc đủ thông tin player để chuyển scene
            var p = new PlayerData {
                CharId  = r.ReadInt(),
                Name    = r.ReadString(),
                ClassId = r.ReadByte(),
                Level   = r.ReadInt(),
                Hp      = r.ReadInt(), MaxHp = r.ReadInt(),
                Mp      = r.ReadInt(), MaxMp = r.ReadInt(),
                Str     = r.ReadShort(), Agi = r.ReadShort(), Intel = r.ReadShort(),
                Gold    = r.ReadLong(),
                MapId   = r.ReadInt(),
                X       = r.ReadFloat(), Y = r.ReadFloat()
            };
            GameState.Instance.MyPlayer = p;
            Debug.Log($"[EnterGame] {p.Name} Lv{p.Level} {p.ClassDisplayName}");
            SceneManager.LoadScene("Game");
            // Lần đầu vào game → thử phát video intro (server quyết định đã xem chưa)
            IntroVideoPlayer.Instance?.RequestAndPlay(() => Debug.Log("[Intro] done, vào game"));
        }


        private void OnIntroVideoConfig(PacketReader r)
        {
            bool enabled = r.ReadBool();
            if (!enabled) { IntroVideoPlayer.Instance?.OnConfigReceived(false, "", "", false, 0, false, true); return; }
            string url     = r.ReadString();
            string urlLow  = r.ReadString();
            bool   skip    = r.ReadBool();
            int    skipSec = r.ReadInt();
            bool   fallback= r.ReadBool();
            bool   watched = r.ReadBool();
            IntroVideoPlayer.Instance?.OnConfigReceived(true, url, urlLow, skip, skipSec, fallback, watched);
        }


        private void OnAutoConfig(PacketReader r) {
            string json = r.ReadString();
            AutoCombatUI.Instance?.SendMessage("LoadConfig", json, SendMessageOptions.DontRequireReceiver);
        }
        private void OnAutoPlayState(PacketReader r) {
            bool on = r.ReadBool();
            Debug.Log($"[Auto] state={on}");
        }
        private void OnBlockOk(PacketReader r) {
            UIManager.Instance?.ShowNotification("Đã chặn người chơi", UINotificationType.Success);
        }
        private void OnCharAction(PacketReader r) {
            long charId = r.ReadLong(); int actionId = r.ReadInt();
            // Phát animation hành động cho nhân vật charId trong zone
            GameState.Instance?.PlayCharAction(charId, actionId);
        }
        private void OnEmoteShow(PacketReader r) {
            long charId = r.ReadLong(); int emoteId = r.ReadInt();
            // Hiện biểu cảm trên đầu nhân vật charId
            GameState.Instance?.ShowEmote(charId, emoteId);
        }
        private void OnGemSocketOk(PacketReader r) {
            UIManager.Instance?.ShowNotification("Khảm ngọc thành công", UINotificationType.Success);
        }
        private void OnInspectData(PacketReader r) {
            long charId = r.ReadLong(); string name = r.ReadString();
            int classId = r.ReadInt(); int level = r.ReadInt(); int vip = r.ReadInt();
            InspectUI.Instance?.Show(charId, name, classId, level, vip);
        }
        private void OnNewsList(PacketReader r) {
            int count = r.ReadShort();
            NewsUI.Instance?.Clear();
            for (int i = 0; i < count; i++) {
                int id = r.ReadInt(); string title = r.ReadString(); string cat = r.ReadString();
                NewsUI.Instance?.AddItem(id, title, cat);
            }
        }
        private void OnPairActionReq(PacketReader r) {
            long requesterId = r.ReadLong(); string name = r.ReadString(); int actionId = r.ReadInt();
            // Hiện popup: "{name} muốn {action} với bạn" → Accept/Decline
            ExpressionUI.Instance?.ShowPairRequest(requesterId, name, actionId);
        }
        private void OnPairActionPlay(PacketReader r) {
            long a = r.ReadLong(); long b = r.ReadLong(); int actionId = r.ReadInt();
            // Cả 2 nhân vật play pair animation
            GameState.Instance?.PlayPairAction(a, b, actionId);
        }
        private void OnRefineOk(PacketReader r) {
            UIManager.Instance?.ShowNotification("Tinh luyện thành công", UINotificationType.Success);
        }
        private void OnTeleportOk(PacketReader r) {
            int mapId = r.ReadInt(); float x = r.ReadFloat(); float y = r.ReadFloat();
            MapManager.Instance?.ChangeMap(mapId, x, y);
        }
        private void OnWarehouseData(PacketReader r) {
            WarehouseUI.Instance?.Open();
        }


        private void OnCosmeticList(PacketReader r) {
            int n = r.ReadShort();
            CosmeticUI.Instance?.Clear();
            for (int i=0;i<n;i++){
                long id=r.ReadLong(); int tpl=r.ReadInt(); string name=r.ReadString();
                string type=r.ReadString(); int rarity=r.ReadByte(); int lvl=r.ReadInt();
                int maxLvl=r.ReadInt(); bool equipped=r.ReadBool(); string stats=r.ReadString(); int sprite=r.ReadInt();
                CosmeticUI.Instance?.AddItem(id, name, type, rarity, lvl, maxLvl, equipped, stats, sprite);
            }
        }
        private void OnCosmeticEquip(PacketReader r) {
            long id=r.ReadLong(); bool ok=r.ReadBool();
            if(ok) CosmeticUI.Instance?.OnEquipped(id);
        }
        private void OnCosmeticUpgrade(PacketReader r) {
            long id=r.ReadLong(); int lvl=r.ReadInt();
            CosmeticUI.Instance?.OnUpgraded(id, lvl);
        }
        private void OnReputationList(PacketReader r) {
            int n=r.ReadShort();
            ReputationUI.Instance?.Clear();
            for(int i=0;i<n;i++){
                int id=r.ReadInt(); string name=r.ReadString(); string desc=r.ReadString();
                int icon=r.ReadInt(); int rep=r.ReadInt(); int maxRep=r.ReadInt(); int tier=r.ReadInt();
                ReputationUI.Instance?.AddFaction(id, name, desc, rep, maxRep, tier);
            }
        }
        private void OnReputationClaim(PacketReader r) {
            int faction=r.ReadInt(); int tier=r.ReadInt(); bool ok=r.ReadBool();
            if(ok) UIManager.Instance?.ShowNotification("Nhận quà danh vọng!", UINotificationType.Success);
        }
        private void OnReputationGain(PacketReader r) {
            int faction=r.ReadInt(); int amount=r.ReadInt(); int total=r.ReadInt();
            UIManager.Instance?.ShowNotification($"+{amount} danh vọng", UINotificationType.Info);
        }
        private void OnBestiaryList(PacketReader r) {
            int n=r.ReadShort();
            BestiaryUI.Instance?.Clear();
            for(int i=0;i<n;i++){
                int mid=r.ReadInt(); string lore=r.ReadString(); string weak=r.ReadString();
                int kills=r.ReadInt(); int need=r.ReadInt(); bool unlocked=r.ReadBool();
                bool claimed=r.ReadBool(); bool boss=r.ReadBool();
                BestiaryUI.Instance?.AddEntry(mid, lore, weak, kills, need, unlocked, claimed, boss);
            }
        }
        private void OnBestiaryClaim(PacketReader r) {
            int mid=r.ReadInt(); bool ok=r.ReadBool();
            if(ok) UIManager.Instance?.ShowNotification("Nhận quà sổ tay!", UINotificationType.Success);
        }
        private void OnBestiaryUnlock(PacketReader r) {
            int mid=r.ReadInt();
            UIManager.Instance?.ShowNotification("Mở khoá mục sổ tay mới!", UINotificationType.Info);
        }
        private void OnSetInfo(PacketReader r) {
            int n=r.ReadShort();
            SetBonusUI.Instance?.Clear();
            for(int i=0;i<n;i++){
                int setId=r.ReadInt(); string name=r.ReadString(); int pieces=r.ReadInt();
                int nb=r.ReadShort();
                for(int j=0;j<nb;j++){ int req=r.ReadInt(); string stats=r.ReadString(); string desc=r.ReadString();
                    SetBonusUI.Instance?.AddBonus(setId, name, pieces, req, stats, desc); }
            }
        }
        private void OnSetBonusUpdate(PacketReader r) {
            SetBonusUI.Instance?.Refresh();
        }


        private void OnFacilityPortals(PacketReader r) {
            int n=r.ReadShort();
            FacilityPortalUI.Instance?.Clear();
            for(int i=0;i<n;i++){
                int id=r.ReadInt(); float x=r.ReadFloat(); float y=r.ReadFloat();
                string cat=r.ReadString(); string label=r.ReadString(); int lvlReq=r.ReadInt(); int icon=r.ReadInt();
                FacilityPortalUI.Instance?.AddPortal(id, x, y, cat, label, lvlReq, icon);
            }
        }
        private void OnFacilityEnter(PacketReader r) {
            int mapId=r.ReadInt(); long instanceId=r.ReadLong();
            string name=r.ReadString(); string fileName=r.ReadString(); string cat=r.ReadString();
            MapManager.Instance?.EnterFacility(mapId, instanceId, name, fileName, cat);
        }
        private void OnFacilityLeft(PacketReader r) {
            int mapId=r.ReadInt(); float x=r.ReadFloat(); float y=r.ReadFloat();
            MapManager.Instance?.ChangeMap(mapId, x, y);
        }


        private void OnFurnitureInteract(PacketReader r) {
            long charId=r.ReadLong(); long fid=r.ReadLong();
            string type=r.ReadString(); string anim=r.ReadString();
            int hp=r.ReadInt(); int mp=r.ReadInt();
            // type: sit/lie/eat/drink/bath → play anim cho charId; cập nhật HP/MP nếu là mình
            HouseInteriorUI.Instance?.OnFurnitureUsed(charId, fid, type, anim, hp, mp);
        }
        private void OnFurnitureStop(PacketReader r) {
            long charId=r.ReadLong();
            HouseInteriorUI.Instance?.OnFurnitureStop(charId);
        }
        private void OnFurnitureBuy(PacketReader r) {
            int fid=r.ReadInt(); bool ok=r.ReadBool();
            if(ok) UIManager.Instance?.ShowNotification("Đã mua vật dụng!", UINotificationType.Success);
        }


        private void OnChildShop(PacketReader r) {
            int n=r.ReadShort(); ChildShopUI.Instance?.Clear();
            for(int i=0;i<n;i++){
                int id=r.ReadInt(); string name=r.ReadString(); string cat=r.ReadString();
                int gold=r.ReadInt(); int dia=r.ReadInt(); string slot=r.ReadString();
                int fid=r.ReadInt(); int nanny=r.ReadInt(); int icon=r.ReadInt();
                ChildShopUI.Instance?.AddItem(id, name, cat, gold, dia, slot, fid, nanny, icon);
            }
        }
        private void OnChildBuy(PacketReader r) {
            long childId=r.ReadLong(); int itemId=r.ReadInt(); bool ok=r.ReadBool();
            if(ok) UIManager.Instance?.ShowNotification("Đã mua cho bé!", UINotificationType.Success);
        }
        private void OnChildInteract(PacketReader r) {
            long parentId=r.ReadLong(); long childId=r.ReadLong();
            GameState.Instance?.PlayChildInteract(childId);
        }
        private void OnChildNpcMove(PacketReader r) {
            long childId=r.ReadLong(); float x=r.ReadFloat(); float y=r.ReadFloat();
            GameState.Instance?.MoveChildNpc(childId, x, y);
        }


        private void OnFarmVisit(PacketReader r) {
            long owner=r.ReadLong(); int n=r.ReadShort();
            FarmUI.Instance?.BeginVisit(owner);
            for(int i=0;i<n;i++){ int plot=r.ReadInt(); int seed=r.ReadInt(); int stage=r.ReadInt();
                FarmUI.Instance?.AddVisitPlot(plot, seed, stage); }
        }
        private void OnAnimalBreed(PacketReader r) {
            // server gửi qua S2C_SYSTEM_MSG là chính; hook để refresh nếu cần
            FarmUI.Instance?.RefreshAnimals();
        }

        private void OnClassStory(PacketReader r)
        {
            string story = r.ReadString();
            StoryUI.Instance?.ShowStory(story);
        }

        // ─────────────────────────────────────────────────────────────────
        // WORLD
        // ─────────────────────────────────────────────────────────────────

        private void OnMapData(PacketReader r)
        {
            var map = new MapData {
                MapId      = r.ReadInt(),
                Name       = r.ReadString(),
                Background = r.ReadString(),
                Width      = r.ReadInt(),
                Height     = r.ReadInt(),
                MinLevel   = r.ReadByte()
            };
            float spawnX = r.ReadFloat(), spawnY = r.ReadFloat();
            GameState.Instance.CurrentMap = map;
            MapRenderer.Instance?.LoadMap(map, spawnX, spawnY);

            // Báo server load xong
            GameClient.Instance.Send(new PacketBuilder(PacketOpcode.C2S_MAP_LOAD_DONE));
        }

        private void OnPlayersInZone(PacketReader r)
        {
            int count = r.ReadShort();
            GameState.Instance.Players.Clear();
            for (int i = 0; i < count; i++)
            {
                int len = r.ReadInt();
                var rp  = ReadRemotePlayer(r);
                GameState.Instance.Players[rp.CharId] = rp;
                PlayerManager.Instance?.SpawnRemotePlayer(rp);
            }
        }

        private void OnMonstersInZone(PacketReader r)
        {
            int count = r.ReadShort();
            GameState.Instance.Monsters.Clear();
            for (int i = 0; i < count; i++)
            {
                int len  = r.ReadInt();
                var mon  = ReadMonster(r);
                GameState.Instance.Monsters[mon.InstanceId] = mon;
                MonsterManager.Instance?.SpawnMonster(mon);
            }
        }

        private void OnPlayerEnter(PacketReader r)
        {
            int len = r.ReadInt();
            var rp  = ReadRemotePlayer(r);
            GameState.Instance.Players[rp.CharId] = rp;
            PlayerManager.Instance?.SpawnRemotePlayer(rp);
        }

        private void OnPlayerLeave(PacketReader r)
        {
            int charId = r.ReadInt();
            GameState.Instance.Players.Remove(charId);
            PlayerManager.Instance?.RemoveRemotePlayer(charId);
        }

        private void OnPlayerMove(PacketReader r)
        {
            int   charId = r.ReadInt();
            float x      = r.ReadFloat(), y = r.ReadFloat();
            byte  dir    = r.ReadByte();
            if (GameState.Instance.Players.TryGetValue(charId, out var rp))
            {
                rp.X = x; rp.Y = y; rp.Direction = dir;
            }
            PlayerManager.Instance?.MoveRemotePlayer(charId, x, y, dir);
        }

        private void OnPositionCorrect(PacketReader r)
        {
            float x = r.ReadFloat(), y = r.ReadFloat();
            PlayerController.Instance?.ForcePosition(x, y);
        }

        private void OnMapChangeFailed(PacketReader r)
        {
            string reason = r.ReadString();
            UIManager.Instance?.ShowNotification(reason, UINotificationType.Warning);
        }

        // ─────────────────────────────────────────────────────────────────
        // COMBAT
        // ─────────────────────────────────────────────────────────────────

        private void OnAttackResult(PacketReader r)
        {
            int   attackerId   = r.ReadInt();
            int   targetId     = r.ReadInt();    // instance id monster
            int   damage       = r.ReadInt();
            bool  isCrit       = r.ReadBool();
            int   monsterHp    = r.ReadInt();

            if (GameState.Instance.Monsters.TryGetValue(targetId, out var mon))
                mon.Hp = monsterHp;

            CombatUI.Instance?.ShowDamage(targetId, damage, isCrit);
            MonsterManager.Instance?.UpdateHp(targetId, monsterHp);
        }

        private void OnSkillResult(PacketReader r)
        {
            int   casterId     = r.ReadInt();
            int   targetId     = r.ReadInt();
            int   skillId      = r.ReadShort();
            int   damage       = r.ReadInt();
            int   monsterHp    = r.ReadInt();
            int   newMp        = r.ReadInt();

            if (GameState.Instance.MyPlayer != null)
                GameState.Instance.MyPlayer.Mp = newMp;
            if (GameState.Instance.Monsters.TryGetValue(targetId, out var mon))
                mon.Hp = monsterHp;

            CombatUI.Instance?.ShowSkillEffect(skillId, targetId, damage);
            HUDUI.Instance?.UpdateMp(newMp, GameState.Instance.MyPlayer?.MaxMp ?? 0);
        }

        private void OnMonsterDead(PacketReader r)
        {
            int instanceId = r.ReadInt();
            int expGained  = r.ReadInt();
            int goldGained = r.ReadInt();

            GameState.Instance.Monsters.Remove(instanceId);
            MonsterManager.Instance?.KillMonster(instanceId);
            CombatUI.Instance?.ShowLoot(expGained, goldGained);
        }

        private void OnPlayerDead(PacketReader r)
        {
            int charId = r.ReadInt();
            if (charId == GameState.Instance.MyPlayer?.CharId)
            {
                HUDUI.Instance?.ShowDeathScreen();
            }
            else
            {
                PlayerManager.Instance?.ShowDeathEffect(charId);
            }
        }

        private void OnMonsterRespawn(PacketReader r)
        {
            int len = r.ReadInt();
            var mon = ReadMonster(r);
            GameState.Instance.Monsters[mon.InstanceId] = mon;
            MonsterManager.Instance?.SpawnMonster(mon);
        }

        private void OnLevelUp(PacketReader r)
        {
            int newLevel = r.ReadInt();
            int newHp    = r.ReadInt(), newMaxHp = r.ReadInt();
            int newMp    = r.ReadInt(), newMaxMp = r.ReadInt();
            int newStr   = r.ReadShort();
            int newAgi   = r.ReadShort();
            int newIntel = r.ReadShort();

            var p = GameState.Instance.MyPlayer;
            if (p != null) {
                p.Level = newLevel;
                p.Hp = newHp; p.MaxHp = newMaxHp;
                p.Mp = newMp; p.MaxMp = newMaxMp;
                p.Str = newStr; p.Agi = newAgi; p.Intel = newIntel;
            }

            HUDUI.Instance?.ShowLevelUp(newLevel);
            HUDUI.Instance?.UpdateStats(p);
        }

        private void OnMonsterMove(PacketReader r)
        {
            int   instanceId = r.ReadInt();
            float x = r.ReadFloat(), y = r.ReadFloat();
            if (GameState.Instance.Monsters.TryGetValue(instanceId, out var mon))
            { mon.X = x; mon.Y = y; }
            MonsterManager.Instance?.MoveMonster(instanceId, x, y);
        }

        // ─────────────────────────────────────────────────────────────────
        // INVENTORY
        // ─────────────────────────────────────────────────────────────────

        private void OnInventoryList(PacketReader r)
        {
            int count = r.ReadShort();
            var list  = new List<InventoryItem>(count);
            for (int i = 0; i < count; i++)
            {
                list.Add(new InventoryItem {
                    InstanceId = r.ReadInt(),
                    ItemId     = r.ReadInt(),
                    Quantity   = r.ReadShort(),
                    IsEquipped = r.ReadBool(),
                    Slot       = r.ReadByte(),
                    BonusStr   = r.ReadShort(), BonusAgi = r.ReadShort(),
                    BonusInt   = r.ReadShort(), BonusHp  = r.ReadShort(),
                    BonusMp    = r.ReadShort(), BonusAtk = r.ReadShort(),
                    BonusDef   = r.ReadShort()
                });
            }
            GameState.Instance.Inventory = list;
            InventoryUI.Instance?.Refresh(list);
        }

        private void OnPlayerStats(PacketReader r)
        {
            var p = GameState.Instance.MyPlayer;
            if (p == null) return;

            int charId   = r.ReadInt();
            p.Hp         = r.ReadInt(); p.MaxHp = r.ReadInt();
            p.Mp         = r.ReadInt(); p.MaxMp = r.ReadInt();
            p.Gold       = r.ReadLong();
            p.Level      = r.ReadInt();
            p.Exp        = r.ReadLong();
            p.ExpToNextLevel = r.ReadLong();
            p.Str        = r.ReadShort();
            p.Agi        = r.ReadShort();
            p.Intel      = r.ReadShort();
            p.AttackBonus  = r.ReadShort();
            p.DefenseBonus = r.ReadShort();

            HUDUI.Instance?.UpdateStats(p);
        }

        private void OnShopData(PacketReader r)
        {
            int shopId = r.ReadInt();
            int count  = r.ReadShort();
            var items  = new List<ShopItem>(count);
            for (int i = 0; i < count; i++)
            {
                items.Add(new ShopItem { ItemId = r.ReadInt(), Price = r.ReadInt() });
            }
            GameState.Instance.ActiveShopId = shopId;
            GameState.Instance.ShopItems = items;
            ShopUI.Instance?.Open(shopId, items);
        }

        private void OnItemError(PacketReader r)
        {
            string msg = r.ReadString();
            UIManager.Instance?.ShowNotification(msg, UINotificationType.Error);
        }

        // ─────────────────────────────────────────────────────────────────
        // QUEST
        // ─────────────────────────────────────────────────────────────────

        private void OnQuestList(PacketReader r)
        {
            int count  = r.ReadShort();
            var quests = new List<QuestData>(count);
            for (int i = 0; i < count; i++)
            {
                quests.Add(new QuestData {
                    QuestId      = r.ReadInt(),
                    Status       = (QuestStatus)r.ReadByte(),
                    Progress     = r.ReadInt(),
                    TargetCount  = r.ReadInt(),
                    Name         = r.ReadString(),
                    Description  = r.ReadString(),
                    RewardExp    = r.ReadInt(),
                    RewardGold   = r.ReadLong(),
                    RewardItemId = r.ReadInt()
                });
            }
            GameState.Instance.Quests = quests;
            QuestUI.Instance?.Refresh(quests);
        }

        private void OnQuestAccepted(PacketReader r)
        {
            int    questId = r.ReadInt();
            string name    = r.ReadString();
            string story   = r.ReadString();
            if (!string.IsNullOrEmpty(story))
                StoryUI.Instance?.ShowStory(story);
            UIManager.Instance?.ShowNotification($"Nhận nhiệm vụ: {name}", UINotificationType.Info);
        }

        private void OnQuestCompleted(PacketReader r)
        {
            int questId = r.ReadInt();
            UIManager.Instance?.ShowNotification("Hoàn thành nhiệm vụ! Phần thưởng đã được trao.", UINotificationType.Success);
        }

        private void OnQuestError(PacketReader r)
        {
            string msg = r.ReadString();
            UIManager.Instance?.ShowNotification(msg, UINotificationType.Error);
        }

        private void OnQuestProgress(PacketReader r)
        {
            int questId  = r.ReadInt();
            int progress = r.ReadInt();
            int target   = r.ReadInt();
            var q = GameState.Instance.Quests.Find(x => x.QuestId == questId);
            if (q != null) q.Progress = progress;
            QuestUI.Instance?.UpdateProgress(questId, progress, target);
        }

        // ─────────────────────────────────────────────────────────────────
        // CHAT
        // ─────────────────────────────────────────────────────────────────

        // ─────────────────────────────────────────────────────────────────
        // CHAT — extended protocol
        // S2C_CHAT: [channel 1][content_type 1][sender_len 2][sender...][payload_len 2][payload...]
        // ─────────────────────────────────────────────────────────────────

        private void OnChat(PacketReader r)
        {
            byte channelByte  = r.ReadByte();
            byte contentType  = r.ReadByte();
            string senderName = r.ReadString();
            int payloadLen    = r.ReadShort() & 0xFFFF;
            byte[] payload    = payloadLen > 0 ? r.ReadBytes(payloadLen) : Array.Empty<byte>();

            string channel = channelByte switch {
                0 => "map", 1 => "world", 2 => "guild", 3 => "pm", 4 => "system", 5 => "cross", _ => "world"
            };

            var msg = new NexusIsekai.UI.ChatMessage
            {
                Channel     = channel,
                Sender      = senderName,
                ContentType = (NexusIsekai.UI.ChatContentType)contentType,
                IsSelf      = senderName == GameState.Instance.MyPlayer?.Name
            };

            // Parse payload theo contentType
            if (payloadLen > 0)
            {
                var pr = new PacketReader(payload);
                ParseChatPayload(msg, contentType, pr);
            }

            GameState.Instance.ChatHistory.Add(new GameObjects.ChatMessage {
                Channel = channel, Sender = senderName,
                Content = msg.Content ?? ""
            });
            if (GameState.Instance.ChatHistory.Count > 200)
                GameState.Instance.ChatHistory.RemoveAt(0);

            ChatUI.Instance?.ReceiveChatMessage(msg);
        }

        private void ParseChatPayload(NexusIsekai.UI.ChatMessage msg, byte contentType, PacketReader pr)
        {
            switch (contentType)
            {
                case 0: // Text
                    msg.Content = pr.Remaining > 0 ? new string(
                        System.Text.Encoding.UTF8.GetChars(pr.ReadBytes(pr.Remaining))) : "";
                    break;

                case 1: // Sticker
                    int stickerId = pr.ReadInt();
                    // Lookup asset key từ sticker cache
                    msg.StickerAssetKey = ChatUI.Instance?._stickers.Find(s => s.Id == stickerId)?.AssetKey
                        ?? $"Stickers/default/sticker_{stickerId}";
                    break;

                case 2: // Emoji
                    int emojiCode = pr.ReadInt();
                    msg.Content = char.ConvertFromUtf32(emojiCode);
                    break;

                case 3: // Location
                    msg.LocationMapId = pr.ReadInt();
                    msg.LocationX     = pr.ReadFloat();
                    msg.LocationY     = pr.ReadFloat();
                    msg.MapName       = pr.ReadString();
                    msg.Content       = $"[Toạ độ] {msg.MapName}";
                    break;

                case 4: // Item showcase
                    msg.ItemId           = pr.ReadInt();
                    msg.ItemEnhanceLevel = pr.ReadByte();
                    msg.ItemRarity       = pr.ReadByte();
                    msg.ItemAtkBonus     = pr.ReadInt();
                    msg.ItemName         = pr.ReadString();
                    msg.Content          = $"[Item] {msg.ItemName}";
                    break;

                case 5: // Red envelope
                    msg.EnvelopeId            = pr.ReadLong();
                    msg.EnvelopeAmountPerGrab = pr.ReadInt();
                    msg.EnvelopeMaxGrabbers   = pr.ReadByte();
                    msg.EnvelopeRemaining     = pr.ReadByte();
                    msg.EnvelopeCurrency      = pr.ReadByte();
                    msg.EnvelopeMessage       = pr.ReadString();
                    msg.Content               = $"[Lì xì] {msg.EnvelopeMessage}";
                    break;

                case 6: // Voice
                    msg.VoiceDurationMs = pr.ReadInt();
                    msg.VoiceUrl        = pr.ReadString();
                    msg.Content         = $"[Voice {msg.VoiceDurationMs/1000f:F1}s]";
                    break;
            }
        }

        private void OnSystemMsg(PacketReader r)
        {
            string text = r.ReadString();
            var msg = new NexusIsekai.UI.ChatMessage {
                ContentType = NexusIsekai.UI.ChatContentType.System,
                Channel = "system",
                Sender  = "System",
                Content = text
            };
            ChatUI.Instance?.ReceiveChatMessage(msg);
            UIManager.Instance?.ShowNotification(text, UINotificationType.Info);
        }

        private void OnChatRedEnvelope(PacketReader r)
        {
            long   envId    = r.ReadLong();
            byte   channel  = r.ReadByte();
            string sender   = r.ReadString();
            int    perGrab  = r.ReadInt();
            int    maxGrab  = r.ReadByte() & 0xFF;
            int    remaining = r.ReadByte() & 0xFF;
            byte   currency = r.ReadByte();
            string message  = r.ReadString();

            string ch = channel switch { 0=>"map",1=>"world",2=>"guild",_=>"world" };

            var msg = new NexusIsekai.UI.ChatMessage {
                ContentType          = NexusIsekai.UI.ChatContentType.Envelope,
                Channel              = ch,
                Sender               = sender,
                EnvelopeId           = envId,
                EnvelopeAmountPerGrab= perGrab,
                EnvelopeMaxGrabbers  = maxGrab,
                EnvelopeRemaining    = remaining,
                EnvelopeCurrency     = currency,
                EnvelopeMessage      = message,
                Content              = $"[Lì xì] {message}"
            };
            ChatUI.Instance?.ReceiveChatMessage(msg);
            UIManager.Instance?.ShowNotification($"{sender} vừa thả lì xì {perGrab}x{maxGrab}!", UINotificationType.Success);
        }

        private void OnChatGrabbed(PacketReader r)
        {
            long   envId     = r.ReadLong();
            string grabber   = r.ReadString();
            int    amount    = r.ReadInt();
            int    remaining = r.ReadByte() & 0xFF;

            ChatUI.Instance?.UpdateEnvelopeCount(envId, grabber, amount, remaining);

            // Nếu mình là người giựt thì sẽ nhận S2C_CHAT_GRAB_RESULT riêng
        }

        private void OnChatGrabResult(PacketReader r)
        {
            long   envId   = r.ReadLong();
            bool   success = r.ReadBool();
            int    amount  = r.ReadInt();
            string msg     = r.ReadString();

            UIManager.Instance?.ShowNotification(msg,
                success ? UINotificationType.Success : UINotificationType.Warning);

            if (success)
            {
                // Cộng vào currency display
                HUDUI.Instance?.RefreshDiamond();
            }
        }

        private void OnStickerList(PacketReader r)
        {
            int count = r.ReadShort() & 0xFFFF;
            var list  = new List<NexusIsekai.UI.ChatUI.StickerData>();
            for (int i = 0; i < count; i++)
            {
                list.Add(new NexusIsekai.UI.ChatUI.StickerData {
                    Id       = r.ReadInt(),
                    PackId   = r.ReadInt(),
                    AssetKey = r.ReadString()
                });
            }
            ChatUI.Instance?.PopulateStickers(list);
        }

        // ─────────────────────────────────────────────────────────────────
        // SYSTEM
        // ─────────────────────────────────────────────────────────────────

        private void OnPong(PacketReader r) { /* latency tracking */ }

        private void OnKick(PacketReader r)
        {
            GameState.Instance.Reset();
            UIManager.Instance?.ShowNotification("Bạn đã bị kick khỏi server.", UINotificationType.Warning);
            SceneManager.LoadScene("Login");
        }

        // ─────────────────────────────────────────────────────────────────
        // Helpers
        // ─────────────────────────────────────────────────────────────────

        private static RemotePlayer ReadRemotePlayer(PacketReader r) => new RemotePlayer {
            CharId    = r.ReadInt(),
            Name      = r.ReadString(),
            ClassId   = r.ReadByte(),
            Level     = r.ReadInt(),
            Hp        = r.ReadInt(),
            MaxHp     = r.ReadInt(),
            X         = r.ReadFloat(),
            Y         = r.ReadFloat(),
            Direction = r.ReadByte()
        };

        private static MonsterData ReadMonster(PacketReader r) => new MonsterData {
            InstanceId = r.ReadInt(),
            MonsterId  = r.ReadInt(),
            Name       = r.ReadString(),
            Hp         = r.ReadInt(),
            MaxHp      = r.ReadInt(),
            X          = r.ReadFloat(),
            Y          = r.ReadFloat(),
            IsBoss     = r.ReadBool()
        };
    }

    // ─────────────────────────────────────────────────────────────────
    // SYSTEM (mới)
    // ─────────────────────────────────────────────────────────────────

    private void OnMaintenance(PacketReader r)
    {
        string msg = r.ReadString();
        UIManager.Instance?.ShowNotification($"Bảo trì: {msg}", UINotificationType.Warning);
    }

    // ─────────────────────────────────────────────────────────────────
    // PAYMENT / DIAMOND (Axx)
    // ─────────────────────────────────────────────────────────────────

    private void OnTopupOk(PacketReader r)
    {
        int diamond = r.ReadInt();
        bool isFirst = r.ReadBool();
        GameState.Instance.AddDiamond(diamond);
        UIManager.Instance?.ShowNotification(
            $"Nạp thành công! +{diamond:N0} Diamond" + (isFirst ? " (Phần thưởng nạp đầu!)" : ""),
            UINotificationType.Success);
        HUDUI.Instance?.RefreshDiamond();
    }

    private void OnDiamondUpdate(PacketReader r)
    {
        int diamond = r.ReadInt();
        GameState.Instance.Diamond = diamond;
        HUDUI.Instance?.RefreshDiamond();
    }

    private void OnGiftcodeOk(PacketReader r)
    {
        string msg = r.ReadString();
        UIManager.Instance?.ShowNotification(msg, UINotificationType.Success);
        NexusIsekai.UI.GiftCodeUI.Instance?.ShowResult(true, msg);
    }

    private void OnGiftcodeFail(PacketReader r)
    {
        string msg = r.ReadString();
        UIManager.Instance?.ShowNotification(msg, UINotificationType.Error);
        NexusIsekai.UI.GiftCodeUI.Instance?.ShowResult(false, msg);
    }

    // ─────────────────────────────────────────────────────────────────
    // MISSION PASS (Bxx)
    // ─────────────────────────────────────────────────────────────────

    private void OnPassInfo(PacketReader r)
    {
        int seasonId    = r.ReadInt();
        if (seasonId == 0) { UIManager.Instance?.ShowNotification("Chưa có season nào.", UINotificationType.Info); return; }
        bool isActive   = r.ReadBool();
        int freeDiamond = r.ReadInt();
        int premDiamond = r.ReadInt();
        int maxLevel    = r.ReadInt();
        int passLevel   = r.ReadInt();
        int passExp     = r.ReadInt();
        bool hasPremium = r.ReadBool();
        int rewardCount = r.ReadShort();

        var rewards = new List<NexusIsekai.UI.MissionPassUI.PassRewardData>();
        for (int i = 0; i < rewardCount; i++)
        {
            int lv      = r.ReadInt();
            int tier    = r.ReadByte();
            int itemId  = r.ReadInt();
            int itemQty = r.ReadInt();
            int diamond = r.ReadInt();
            int gold    = r.ReadInt();
            bool claimed= r.ReadBool();
            rewards.Add(new NexusIsekai.UI.MissionPassUI.PassRewardData {
                Level=lv, Tier=tier, ItemId=itemId, ItemQty=itemQty, Diamond=diamond, Gold=gold
            });
            if (claimed) NexusIsekai.UI.MissionPassUI.Instance?.MarkClaimed(lv, tier);
        }

        NexusIsekai.UI.MissionPassUI.Instance?.Populate(
            seasonId, isActive, freeDiamond, premDiamond, maxLevel, passLevel, passExp, hasPremium, rewards);
    }

    private void OnPassClaimOk(PacketReader r)
    {
        int level    = r.ReadInt();
        int tier     = r.ReadByte();
        int claimCnt = r.ReadByte();
        UIManager.Instance?.ShowNotification($"Nhận thưởng level {level} ({(tier==0?"Free":"Premium")}) thành công!", UINotificationType.Success);
        HUDUI.Instance?.RefreshDiamond();
    }

    private void OnPassLevelUp(PacketReader r)
    {
        // Pass level up notification
        UIManager.Instance?.ShowNotification("Pass level lên!", UINotificationType.Success);
    }

    // ─────────────────────────────────────────────────────────────────
    // TITLE (Cxx)
    // ─────────────────────────────────────────────────────────────────

    private void OnTitleList(PacketReader r)
    {
        int count = r.ReadShort();
        var titles = new List<GameObjects.TitleInfo>();
        for (int i = 0; i < count; i++)
        {
            titles.Add(new GameObjects.TitleInfo {
                TitleId  = r.ReadInt(),
                Name     = r.ReadString(),
                ColorHex = r.ReadString(),
                Equipped = r.ReadBool()
            });
        }
        GameState.Instance.Titles = titles;
        NexusIsekai.UI.TitleUI.Instance?.Populate(titles);
        HUDUI.Instance?.RefreshTitle();
    }

    private void OnTitleGrant(PacketReader r)
    {
        int titleId = r.ReadInt();
        UIManager.Instance?.ShowNotification("Nhận được danh hiệu mới!", UINotificationType.Success);
        // Refresh title list
        PacketBuilder.SendTitleList();
    }

    // ─────────────────────────────────────────────────────────────────
    // PET / MOUNT (Dxx)
    // ─────────────────────────────────────────────────────────────────

    private void OnPetList(PacketReader r)
    {
        int count = r.ReadShort();
        var pets = new List<GameObjects.PetInfo>();
        for (int i = 0; i < count; i++)
        {
            pets.Add(new GameObjects.PetInfo {
                PetId      = r.ReadLong(),
                TemplateId = r.ReadInt(),
                Name       = r.ReadString(),
                Rarity     = r.ReadByte(),
                Level      = r.ReadInt(),
                Hunger     = r.ReadInt(),
                Loyalty    = r.ReadInt(),
                IconId     = r.ReadInt(),
                IsActive   = r.ReadBool()
            });
        }
        GameState.Instance.Pets = pets;
        NexusIsekai.UI.PetUI.Instance?.PopulatePets(pets);
    }

    private void OnPetUpdate(PacketReader r)
    {
        PacketBuilder.SendPetList(); // Refresh
    }

    private void OnMountList(PacketReader r)
    {
        int count = r.ReadShort();
        var mounts = new List<GameObjects.MountInfo>();
        for (int i = 0; i < count; i++)
        {
            mounts.Add(new GameObjects.MountInfo {
                MountId    = r.ReadLong(),
                TemplateId = r.ReadInt(),
                Name       = r.ReadString(),
                Rarity     = r.ReadByte(),
                Level      = r.ReadInt(),
                SpeedBonus = r.ReadFloat(),
                IconId     = r.ReadInt(),
                IsActive   = r.ReadBool()
            });
        }
        GameState.Instance.Mounts = mounts;
        NexusIsekai.UI.PetUI.Instance?.PopulateMounts(mounts);
    }

    // ─────────────────────────────────────────────────────────────────
    // SOCIAL / MARRIAGE / CHILDREN (Exx)
    // ─────────────────────────────────────────────────────────────────

    private void OnRelationship(PacketReader r)
    {
        int  status       = r.ReadByte();   // 0=none,1=friend,2=dating,3=engaged,4=married
        long targetCharId = r.ReadLong();
        string targetName = r.ReadString();
        GameState.Instance.MyPlayer?.UpdateRelation(status, targetCharId, targetName);
        string label = status switch {
            1 => "bạn bè", 2 => "đang hẹn hò", 3 => "đính hôn", 4 => "kết hôn",
            _ => ""
        };
        if (!string.IsNullOrEmpty(label))
            UIManager.Instance?.ShowNotification($"Quan hệ với {targetName}: {label}", UINotificationType.Info);
    }

    private void OnWeddingEvent(PacketReader r)
    {
        long charIdA = r.ReadLong();
        long charIdB = r.ReadLong();
        string msg   = r.ReadString();
        ChatUI.Instance?.ReceiveChatMessage(new NexusIsekai.UI.ChatMessage
        {
            ContentType = NexusIsekai.UI.ChatContentType.System,
            Channel     = "system",
            Sender      = "System",
            Content     = msg
        });
    }

    private void OnChildList(PacketReader r)
    {
        int count = r.ReadShort();
        var children = new List<GameObjects.ChildInfo>();
        for (int i = 0; i < count; i++)
        {
            children.Add(new GameObjects.ChildInfo {
                ChildId   = r.ReadLong(),
                Name      = r.ReadString(),
                Gender    = r.ReadByte(),
                Age       = r.ReadInt(),
                Level     = r.ReadInt(),
                Hp        = r.ReadInt(),
                MaxHp     = r.ReadInt(),
                Happiness = r.ReadInt(),
                IsActive  = r.ReadBool()
            });
        }
        GameState.Instance.Children = children;
        // Refresh HUD/UI nếu có
        HUDUI.Instance?.RefreshDiamond(); // triggers general UI refresh
    }

    // ─────────────────────────────────────────────────────────────────
    // MENTOR (Fxx)
    // ─────────────────────────────────────────────────────────────────

    private void OnMentorInfo(PacketReader r)
    {
        bool hasMentor = r.ReadBool();
        long mentorId  = hasMentor ? r.ReadLong() : 0;
        bool isNpc     = hasMentor && r.ReadBool();
        int studentCount = r.ReadShort();

        var students = new List<GameObjects.MentorRelation>();
        for (int i = 0; i < studentCount; i++)
        {
            long studId   = r.ReadLong();
            string sName  = r.ReadString();
            int sLevel    = r.ReadInt();
            students.Add(new GameObjects.MentorRelation { StudentId = studId });
        }
        NexusIsekai.UI.MentorUI.Instance?.Populate(hasMentor, mentorId, isNpc, students);
    }

    private void OnMentorGraduate(PacketReader r)
    {
        UIManager.Instance?.ShowNotification("Chúc mừng bạn đã xuất sư!", UINotificationType.Success);
    }

    // ─────────────────────────────────────────────────────────────────
    // GUILD (8xx)
    // ─────────────────────────────────────────────────────────────────

    // S2C_GUILD_INFO: [long guildId][str name][long leaderId][int level]
    //                 [long exp][long gold][int memberCount][int maxMembers][str notice]
    private void OnGuildInfo(PacketReader r)
    {
        long   guildId     = r.ReadLong();
        string guildName   = r.ReadString();
        long   leaderId    = r.ReadLong();
        int    level       = r.ReadInt();
        long   exp         = r.ReadLong();
        long   gold        = r.ReadLong();
        int    memberCount = r.ReadInt();
        int    maxMembers  = r.ReadInt();
        string notice      = r.ReadString();

        NexusIsekai.UI.GuildUI.Instance?.ShowGuildInfo(
            guildId, guildName, leaderId, level, exp, gold, memberCount, maxMembers, notice);
    }

    // S2C_GUILD_MEMBERS: [short count]([long charId][str name][byte role][int contrib])...
    private void OnGuildMembers(PacketReader r)
    {
        int count = r.ReadShort();
        var members = new List<NexusIsekai.UI.GuildUI.MemberEntry>();
        for (int i = 0; i < count; i++)
        {
            members.Add(new NexusIsekai.UI.GuildUI.MemberEntry {
                CharId       = r.ReadLong(),
                Name         = r.ReadString(),
                Role         = r.ReadByte(),
                Contribution = r.ReadInt()
            });
        }
        NexusIsekai.UI.GuildUI.Instance?.PopulateMembers(members);
    }

    // S2C_GUILD_INVITED: [long guildId][str guildName]
    private void OnGuildInvited(PacketReader r)
    {
        long   guildId   = r.ReadLong();
        string guildName = r.ReadString();
        UIManager.Instance?.ShowNotification($"Bạn được mời vào guild '{guildName}'!", UINotificationType.Info);
        // Show accept/decline prompt
        NexusIsekai.UI.GuildUI.Instance?.ShowInvitePrompt(guildId, guildName);
    }

    // ─────────────────────────────────────────────────────────────────
    // SKILL EXTENDED (9xx)
    // ─────────────────────────────────────────────────────────────────

    // S2C_SKILL_LIST: [short count]([int id][int lv][int mp][int cd][int dmg][str name][str elem])...
    //                 [int slot0..6]
    private void OnSkillList(PacketReader r)
    {
        int count = r.ReadShort();
        var skills = new List<NexusIsekai.UI.SkillUI.SkillEntry>();
        for (int i = 0; i < count; i++)
        {
            skills.Add(new NexusIsekai.UI.SkillUI.SkillEntry {
                SkillId    = r.ReadInt(),
                Level      = r.ReadInt(),
                MpCost     = r.ReadInt(),
                CooldownMs = r.ReadInt(),
                BaseDamage = r.ReadInt(),
                Name       = r.ReadString(),
                Element    = r.ReadString()
            });
        }
        int[] slots = new int[7];
        for (int i = 0; i < 7; i++) slots[i] = r.ReadInt();

        NexusIsekai.UI.SkillUI.Instance?.Populate(skills, slots);
        // Update HUD skill bar
        for (int i = 0; i < 7; i++)
        {
            if (slots[i] > 0)
            {
                var s = skills.Find(x => x.SkillId == slots[i]);
                if (s != null)
                {
                    var sp = UnityEngine.Resources.Load<UnityEngine.Sprite>($"Sprites/Skills/skill_{s.SkillId}");
                    HUDUI.Instance?.UpdateSkillSlot(i, s.SkillId, sp, s.CooldownMs / 1000f);
                }
            }
        }
    }

    // S2C_CLASS_SKILL_LIST: all learnable skills of player's class
    private void OnClassSkillList(PacketReader r)
    {
        int count = r.ReadShort();
        var all = new List<NexusIsekai.UI.SkillUI.SkillTemplate>();
        for (int i = 0; i < count; i++)
        {
            all.Add(new NexusIsekai.UI.SkillUI.SkillTemplate {
                SkillId     = r.ReadInt(),
                Name        = r.ReadString(),
                BaseDamage  = r.ReadInt(),
                MpCost      = r.ReadInt(),
                CooldownMs  = r.ReadInt(),
                MaxLevel    = r.ReadInt(),
                UnlockLevel = r.ReadInt(),
                IconId      = r.ReadInt(),
                SkillType   = r.ReadString()
            });
        }
        NexusIsekai.UI.SkillUI.Instance?.PopulateClassSkills(all);
    }

    // ─────────────────────────────────────────────────────────────────
    // ENHANCEMENT
    // ─────────────────────────────────────────────────────────────────

    // S2C_ENHANCE_RESULT: [long instanceId][byte result 0=fail_keep,1=success,2=fail_drop][int newLevel][str msg]
    private void OnEnhanceResult(PacketReader r)
    {
        long   instanceId = r.ReadLong();
        byte   result     = r.ReadByte();
        int    newLevel   = r.ReadInt();
        string msg        = r.ReadString();

        bool success  = result == 1;
        bool dropped  = result == 2;

        UIManager.Instance?.ShowNotification(msg,
            success ? UINotificationType.Success : UINotificationType.Warning);
        NexusIsekai.UI.EnhancementUI.Instance?.ShowResult(success);

        // Update inventory item
        var item = GameState.Instance.Inventory.Find(x => x.inventoryId == instanceId);
        if (item != null) item.enhance_level = newLevel;
        InventoryUI.Instance?.Refresh(GameState.Instance.Inventory);
    }

    // ─────────────────────────────────────────────────────────────────
    // PVP (Bxx)
    // ─────────────────────────────────────────────────────────────────

    // S2C_PVP_REQUEST: [long challengerCharId][short nameLen][name][int level]
    private void OnPvpRequest(PacketReader r)
    {
        long   challengerId = r.ReadLong();
        string name         = r.ReadString();
        int    level        = r.ReadInt();
        UIManager.Instance?.ShowNotification($"{name} (Lv.{level}) thách đấu bạn!", UINotificationType.Warning);
        NexusIsekai.UI.PvPUI.Instance?.ShowChallenge(challengerId, name, level);
    }

    // S2C_PVP_START: [long duelId][long charIdA][long charIdB]
    private void OnPvpStart(PacketReader r)
    {
        long duelId  = r.ReadLong();
        long charIdA = r.ReadLong();
        long charIdB = r.ReadLong();
        UIManager.Instance?.ShowNotification("Bắt đầu đấu!", UINotificationType.Info);
        NexusIsekai.UI.PvPUI.Instance?.StartDuel(duelId, charIdA, charIdB);
    }

    // S2C_PVP_COMBAT_RESULT: [long atkId][long defId][int dmg][byte crit][int defHp][int defMaxHp]
    private void OnPvpCombatResult(PacketReader r)
    {
        long atkId  = r.ReadLong();
        long defId  = r.ReadLong();
        int  dmg    = r.ReadInt();
        bool crit   = r.ReadBool();
        int  defHp  = r.ReadInt();
        int  defMax = r.ReadInt();

        CombatUI.Instance?.ShowDamage((int)defId, dmg, crit);
        NexusIsekai.UI.PvPUI.Instance?.UpdateHealth(defId, defHp, defMax);
    }

    // S2C_PVP_END: [long winnerId][byte reasonLen][reason]
    private void OnPvpEnd(PacketReader r)
    {
        long   winnerId = r.ReadLong();
        string reason   = r.ReadString();
        bool   iWon     = winnerId == GameState.Instance.MyPlayer?.CharId;
        UIManager.Instance?.ShowNotification(
            iWon ? $"Bạn thắng! ({reason})" : $"Bạn thua. ({reason})",
            iWon ? UINotificationType.Success : UINotificationType.Warning);
        NexusIsekai.UI.PvPUI.Instance?.EndDuel(winnerId, reason);
    }

    // ─────────────────────────────────────────────────────────────────
    // MINIGAME (Cxx)
    // ─────────────────────────────────────────────────────────────────

    private void OnMinigameRoomList(PacketReader r)
    {
        string gameType = r.ReadString();
        int    count    = r.ReadShort();
        var    rooms    = new List<NexusIsekai.UI.MinigameUI.RoomEntry>();
        for (int i = 0; i < count; i++)
        {
            rooms.Add(new NexusIsekai.UI.MinigameUI.RoomEntry {
                RoomId   = r.ReadLong(),
                HostName = r.ReadString(),
                MinBet   = r.ReadInt(),
                MaxBet   = r.ReadInt(),
                Currency = r.ReadByte()
            });
        }
        NexusIsekai.UI.MinigameUI.Instance?.ShowRoomList(gameType, rooms);
    }

    private void OnMinigameRoomUpdate(PacketReader r)
    {
        long roomId  = r.ReadLong();
        byte status  = r.ReadByte();
        byte players = r.ReadByte();
        NexusIsekai.UI.MinigameUI.Instance?.UpdateRoom(roomId, status, players);
    }

    private void OnMinigameBet(PacketReader r)
    {
        string name   = r.ReadString();
        int    symbol = r.ReadByte();
        int    amount = r.ReadInt();
        NexusIsekai.UI.MinigameUI.Instance?.AppendBetLog(name, symbol, amount);
    }

    // S2C_MINIGAME_RESULT — format khác nhau tùy game type (3 dice cho bầu cua)
    private void OnMinigameResult(PacketReader r)
    {
        NexusIsekai.UI.MinigameUI.Instance?.ShowResult(r);
    }

    // ─────────────────────────────────────────────────────────────────
    // FARMING (Dxx)
    // ─────────────────────────────────────────────────────────────────

    // S2C_FARM_STATE: [short count]([int plotIdx][int seedId][int stage][int maxStages][int water][int waterNeeded][str name])...
    private void OnFarmState(PacketReader r)
    {
        int count = r.ReadShort();
        var plots = new List<NexusIsekai.UI.FarmingUI.PlotData>();
        for (int i = 0; i < count; i++)
        {
            plots.Add(new NexusIsekai.UI.FarmingUI.PlotData {
                PlotIndex  = r.ReadInt(),
                SeedId     = r.ReadInt(),
                Stage      = r.ReadInt(),
                MaxStages  = r.ReadInt(),
                WaterCount = r.ReadInt(),
                WaterNeeded= r.ReadInt(),
                SeedName   = r.ReadString()
            });
        }
        NexusIsekai.UI.FarmingUI.Instance?.Populate(plots);
    }

    private void OnFarmUpdate(PacketReader r)
    {
        // Refresh farm state
        PacketBuilder.SendFarmState();
    }

    // ─────────────────────────────────────────────────────────────────
    // HOUSING (Exx)
    // ─────────────────────────────────────────────────────────────────

    private void OnHouseInfo(PacketReader r)
    {
        long houseId = r.ReadLong();
        int  level   = r.ReadInt();
        int  style   = r.ReadInt();
        int  happy   = r.ReadInt();
        NexusIsekai.UI.HousingUI.Instance?.ShowHouseInfo(houseId, level, style, happy);
    }

    // S2C_HOUSE_FURNITURE: [short count]([long id][int furnId][float x][float y][int rot][str name])...
    private void OnHouseFurniture(PacketReader r)
    {
        int count = r.ReadShort();
        var list  = new List<NexusIsekai.UI.HousingUI.FurnitureInstance>();
        for (int i = 0; i < count; i++)
        {
            list.Add(new NexusIsekai.UI.HousingUI.FurnitureInstance {
                InstanceId  = r.ReadLong(),
                FurnitureId = r.ReadInt(),
                X           = r.ReadFloat(),
                Y           = r.ReadFloat(),
                Rotation    = r.ReadInt(),
                Name        = r.ReadString()
            });
        }
        NexusIsekai.UI.HousingUI.Instance?.PopulateFurniture(list);
    }

    // S2C_HOUSE_CATALOG: [short count]([int id][str name][int gold][int diamond][int w][int h])...
    private void OnHouseCatalog(PacketReader r)
    {
        int count = r.ReadShort();
        var list  = new List<NexusIsekai.UI.HousingUI.CatalogEntry>();
        for (int i = 0; i < count; i++)
        {
            list.Add(new NexusIsekai.UI.HousingUI.CatalogEntry {
                Id           = r.ReadInt(),
                Name         = r.ReadString(),
                GoldPrice    = r.ReadInt(),
                DiamondPrice = r.ReadInt(),
                Width        = r.ReadInt(),
                Height       = r.ReadInt()
            });
        }
        NexusIsekai.UI.HousingUI.Instance?.PopulateCatalog(list);
    }

    // ─────────────────────────────────────────────────────────────────
    // LEADERBOARD (Fxx)
    // ─────────────────────────────────────────────────────────────────

    // S2C_LEADERBOARD: [str rankType][int myRank][short count]([int rank][long charId][str name][int class][byte gender][long value])...
    private void OnLeaderboard(PacketReader r)
    {
        string rankType = r.ReadString();
        int    myRank   = r.ReadInt();
        int    count    = r.ReadShort();
        var    entries  = new List<NexusIsekai.UI.LeaderboardUI.Entry>();
        for (int i = 0; i < count; i++)
        {
            entries.Add(new NexusIsekai.UI.LeaderboardUI.Entry {
                Rank      = r.ReadInt(),
                CharId    = r.ReadLong(),
                Name      = r.ReadString(),
                ClassId   = r.ReadInt(),
                Gender    = r.ReadByte(),
                RankValue = r.ReadLong()
            });
        }
        NexusIsekai.UI.LeaderboardUI.Instance?.Populate(rankType, myRank, entries);
    }

    // ═══════════════════════════════════════════════════════════════
    // TRADE
    // ═══════════════════════════════════════════════════════════════

    private void OnTradeRequest(PacketReader r)
    {
        long fromId = r.ReadLong(); string name = r.ReadString();
        UIManager.Instance?.ShowConfirmDialog(
            "Yeu cau giao dich", $"{name} muon giao dich voi ban.",
            onYes: () => PacketBuilder.SendTradeRespond(fromId, true),
            onNo:  () => PacketBuilder.SendTradeRespond(fromId, false));
    }

    private void OnTradeUpdate(PacketReader r)
    {
        long tradeId = r.ReadLong(); bool confA = r.ReadBool(); bool confB = r.ReadBool();
        long goldA = r.ReadLong(); long goldB = r.ReadLong();
        int itemsA = r.ReadShort(); int itemsB = r.ReadShort();
        // TODO: update TradeUI khi co
    }

    private void OnTradeResult(PacketReader r)
    {
        bool ok = r.ReadBool(); string msg = r.ReadString();
        UIManager.Instance?.ShowNotification(msg, ok ? UINotificationType.Success : UINotificationType.Error);
    }

    // ═══════════════════════════════════════════════════════════════
    // AUCTION
    // ═══════════════════════════════════════════════════════════════

    private void OnAuctionList(PacketReader r)
    {
        int count = r.ReadShort();
        for (int i = 0; i < count; i++)
        {
            long id = r.ReadLong(); string name = r.ReadString(); int itemId = r.ReadInt();
            int qty = r.ReadInt(); int rarity = r.ReadByte(); int enhance = r.ReadByte();
            long startPrice = r.ReadLong(); long currentBid = r.ReadLong();
            long buyout = r.ReadLong(); string seller = r.ReadString(); int currency = r.ReadByte();
        }
        // TODO: populate AuctionUI khi co
    }

    private void OnAuctionResult(PacketReader r)
    {
        bool ok = r.ReadBool(); string msg = r.ReadString();
        UIManager.Instance?.ShowNotification(msg, ok ? UINotificationType.Success : UINotificationType.Warning);
    }

    // ═══════════════════════════════════════════════════════════════
    // PARTY
    // ═══════════════════════════════════════════════════════════════

    private void OnPartyInfo(PacketReader r)
    {
        long partyId = r.ReadLong(); int count = r.ReadShort();
        for (int i = 0; i < count; i++)
        {
            long charId = r.ReadLong(); string name = r.ReadString();
            int level = r.ReadInt(); int role = r.ReadByte(); int classId = r.ReadByte();
        }
        // TODO: populate PartyUI
    }

    private void OnPartyInvited(PacketReader r)
    {
        long leaderId = r.ReadLong(); string leaderName = r.ReadString();
        UIManager.Instance?.ShowConfirmDialog(
            "Moi vao nhom", $"{leaderName} moi ban vao nhom.",
            onYes: () => PacketBuilder.SendPartyAccept(leaderId));
    }

    private void OnPartyUpdate(PacketReader r) { /* refresh party UI */ }

    // ═══════════════════════════════════════════════════════════════
    // DUNGEON
    // ═══════════════════════════════════════════════════════════════

    private void OnDungeonList(PacketReader r)
    {
        int count = r.ReadShort();
        for (int i = 0; i < count; i++)
        {
            int id = r.ReadInt(); string name = r.ReadString(); int minLvl = r.ReadInt();
            int maxPlayers = r.ReadInt(); int diff = r.ReadByte(); int timeLimit = r.ReadInt();
            int expReward = r.ReadInt(); int goldReward = r.ReadInt();
        }
        // TODO: populate DungeonUI
    }

    private void OnDungeonEnterOk(PacketReader r)
    {
        long instanceId = r.ReadLong(); int templateId = r.ReadInt();
        UIManager.Instance?.ShowNotification("Vao dungeon thanh cong!", UINotificationType.Success);
    }

    private void OnDungeonResult(PacketReader r)
    {
        bool cleared = r.ReadBool(); int expReward = r.ReadInt(); int goldReward = r.ReadInt();
        string msg = cleared ? $"Hoan thanh dungeon! +{expReward} EXP +{goldReward}G" : "That bai!";
        UIManager.Instance?.ShowNotification(msg, cleared ? UINotificationType.Success : UINotificationType.Error);
    }

    private void OnDungeonTimer(PacketReader r)
    {
        int remainingSeconds = r.ReadInt();
        // TODO: update dungeon timer UI
    }

    // ═══════════════════════════════════════════════════════════════
    // NPC DIALOG
    // ═══════════════════════════════════════════════════════════════

    private void OnDialogShow(PacketReader r)
    {
        int dialogId = r.ReadInt(); int npcId = r.ReadInt();
        string speaker = r.ReadString(); string text = r.ReadString();
        string optionsJson = r.ReadString(); int nextId = r.ReadInt();
        // TODO: hien dialog UI voi cac lua chon
        ChatUI.Instance?.ReceiveChatMessage(new NexusIsekai.UI.ChatMessage
        {
            ContentType = NexusIsekai.UI.ChatContentType.System,
            Channel = "system", Sender = speaker, Content = text
        });
    }

    private void OnDialogOptions(PacketReader r) { /* additional options data */ }

    // ═══════════════════════════════════════════════════════════════
    // ANNOUNCEMENTS & SYSTEM EVENT LOG
    // ═══════════════════════════════════════════════════════════════

    private void OnAnnouncementList(PacketReader r)
    {
        int count = r.ReadShort();
        for (int i = 0; i < count; i++)
        {
            int id = r.ReadInt(); string title = r.ReadString(); string content = r.ReadString();
            string type = r.ReadString(); int priority = r.ReadByte(); bool sticky = r.ReadBool();
            // Sticky announcements hien len chat tab "He Thong"
            ChatUI.Instance?.ReceiveChatMessage(new NexusIsekai.UI.ChatMessage
            {
                ContentType = NexusIsekai.UI.ChatContentType.System,
                Channel = "system", Sender = "Thong Bao",
                Content = (sticky ? "[Ghim] " : "") + title + ": " + content
            });
        }
    }

    private void OnAnnouncementNew(PacketReader r)
    {
        string type = r.ReadString(); string charName = r.ReadString(); string message = r.ReadString();
        ChatUI.Instance?.ReceiveChatMessage(new NexusIsekai.UI.ChatMessage
        {
            ContentType = NexusIsekai.UI.ChatContentType.System,
            Channel = "system", Sender = "He Thong", Content = message
        });
        UIManager.Instance?.ShowNotification(message, UINotificationType.Info);
    }

    private void OnSystemEventLog(PacketReader r)
    {
        int count = r.ReadShort();
        for (int i = 0; i < count; i++)
        {
            string eventType = r.ReadString(); string charName = r.ReadString(); string msg = r.ReadString();
            ChatUI.Instance?.ReceiveChatMessage(new NexusIsekai.UI.ChatMessage
            {
                ContentType = NexusIsekai.UI.ChatContentType.System,
                Channel = "system", Sender = "Log", Content = $"[{eventType}] {msg}"
            });
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // EVENT CURRENCY
    // ═══════════════════════════════════════════════════════════════

    private void OnEventCurrencyList(PacketReader r)
    {
        int count = r.ReadShort();
        for (int i = 0; i < count; i++)
        {
            int id = r.ReadInt(); string code = r.ReadString(); string name = r.ReadString();
            string icon = r.ReadString(); int amount = r.ReadInt(); int exchangeRate = r.ReadInt();
        }
        // TODO: populate EventCurrencyUI
    }

    private void OnEventCurrencyShop(PacketReader r)
    {
        int count = r.ReadShort();
        for (int i = 0; i < count; i++)
        {
            int id = r.ReadInt(); int itemId = r.ReadInt(); string name = r.ReadString();
            int price = r.ReadInt(); int stock = r.ReadInt();
        }
        // TODO: populate EventShopUI
    }

    private void OnEventCurrencyUpdate(PacketReader r)
    {
        int currencyId = r.ReadInt(); int newAmount = r.ReadInt();
        UIManager.Instance?.ShowNotification($"Token cap nhat: {newAmount}", UINotificationType.Info);
    }

    // ═══════════════════════════════════════════════════════════
    // ACHIEVEMENT + DAILY LOGIN + WORLD BOSS + MAIL + CORE
    // ═══════════════════════════════════════════════════════════

    private void OnAchievementList(PacketReader r) { int count = r.ReadShort(); for (int i=0;i<count;i++) { r.ReadInt(); r.ReadString(); r.ReadString(); r.ReadString(); r.ReadString(); r.ReadInt(); r.ReadInt(); r.ReadByte(); r.ReadByte(); r.ReadInt(); r.ReadString(); r.ReadInt(); } }
    private void OnAchievementUpdate(PacketReader r) { r.ReadInt(); r.ReadInt(); }
    private void OnAchievementUnlock(PacketReader r) { string name = r.ReadString(); UIManager.Instance?.ShowNotification("Thanh tuu: " + name, UINotificationType.Success); }

    private void OnDailyLoginInfo(PacketReader r) { int day = r.ReadInt(); int streak = r.ReadInt(); bool claimed = r.ReadBool(); int count = r.ReadShort(); for (int i=0;i<count;i++) { r.ReadInt(); r.ReadString(); r.ReadInt(); r.ReadString(); } }
    private void OnDailyLoginClaimed(PacketReader r) { bool ok = r.ReadBool(); UIManager.Instance?.ShowNotification("Nhan thuong dang nhap!", UINotificationType.Success); }

    private void OnWorldBossSpawn(PacketReader r) { int count = r.ReadShort(); for (int i=0;i<count;i++) { r.ReadInt(); r.ReadString(); r.ReadInt(); r.ReadInt(); r.ReadString(); r.ReadInt(); } }
    private void OnWorldBossDead(PacketReader r) { string boss = r.ReadString(); string killer = r.ReadString(); UIManager.Instance?.ShowNotification(killer + " ha " + boss + "!", UINotificationType.Info); }
    private void OnWorldBossUpdate(PacketReader r) { r.ReadInt(); r.ReadInt(); }

    private void OnMailList(PacketReader r) { int count = r.ReadShort(); for (int i=0;i<count;i++) { r.ReadLong(); r.ReadString(); r.ReadString(); r.ReadString(); r.ReadByte(); r.ReadByte(); r.ReadString(); } }
    private void OnMailNew(PacketReader r) { string sender = r.ReadString(); string title = r.ReadString(); UIManager.Instance?.ShowNotification("Thu moi: " + title, UINotificationType.Info); }

    // ─── Core game handlers ───────────────────────────────────

    private void OnLoginResult(PacketReader r) { bool ok = r.ReadBool(); string msg = r.ReadString(); if (!ok) UIManager.Instance?.ShowNotification(msg, UINotificationType.Error); }
    private void OnRegisterResult(PacketReader r) { bool ok = r.ReadBool(); string msg = r.ReadString(); UIManager.Instance?.ShowNotification(msg, ok ? UINotificationType.Success : UINotificationType.Error); }
    private void OnCharSelectOk(PacketReader r) { /* Load game scene */ }
    private void OnCharError(PacketReader r) { string msg = r.ReadString(); UIManager.Instance?.ShowNotification(msg, UINotificationType.Error); }
    private void OnInventoryUpdate(PacketReader r) { /* Refresh inventory UI */ }
    private void OnMonsterList(PacketReader r) { int count = r.ReadShort(); for (int i=0;i<count;i++) { r.ReadLong(); r.ReadInt(); r.ReadFloat(); r.ReadFloat(); r.ReadInt(); } }
    private void OnNpcList(PacketReader r) { int count = r.ReadShort(); for (int i=0;i<count;i++) { r.ReadInt(); r.ReadString(); r.ReadFloat(); r.ReadFloat(); } }
    private void OnMonsterHpUpdate(PacketReader r) { r.ReadLong(); r.ReadInt(); r.ReadInt(); }
    private void OnMonsterDie(PacketReader r) { r.ReadLong(); r.ReadInt(); r.ReadInt(); }
    private void OnPlayerHpUpdate(PacketReader r) { r.ReadLong(); r.ReadInt(); r.ReadInt(); r.ReadInt(); }
    private void OnPlayerDie(PacketReader r) { r.ReadLong(); UIManager.Instance?.ShowNotification("Ban da chet!", UINotificationType.Error); }
    private void OnPlayerRevive(PacketReader r) { r.ReadLong(); r.ReadFloat(); r.ReadFloat(); }
    private void OnPlayerOnline(PacketReader r) { string name = r.ReadString(); bool on = r.ReadBool(); }
    private void OnCombatResult(PacketReader r) { r.ReadLong(); r.ReadInt(); r.ReadBool(); }
    private void OnExpGain(PacketReader r) { int exp = r.ReadInt(); int total = r.ReadInt(); }
    private void OnQuestUpdate(PacketReader r) { r.ReadInt(); r.ReadInt(); r.ReadInt(); }
    private void OnQuestComplete(PacketReader r) { int qid = r.ReadInt(); UIManager.Instance?.ShowNotification("Hoan thanh nhiem vu!", UINotificationType.Success); }
    private void OnShopResult(PacketReader r) { bool ok = r.ReadBool(); string msg = r.ReadString(); UIManager.Instance?.ShowNotification(msg, ok ? UINotificationType.Success : UINotificationType.Warning); }
    private void OnSkillEffect(PacketReader r) { r.ReadLong(); r.ReadInt(); r.ReadFloat(); r.ReadFloat(); }
    private void OnSkillCooldown(PacketReader r) { r.ReadInt(); r.ReadInt(); }
    private void OnGuildMsg(PacketReader r) { string msg = r.ReadString(); }
    private void OnEventStart(PacketReader r) { string name = r.ReadString(); UIManager.Instance?.ShowNotification("Su kien: " + name, UINotificationType.Info); }
    private void OnServerMsg(PacketReader r) { string msg = r.ReadString(); UIManager.Instance?.ShowNotification(msg, UINotificationType.Info); }
    private void OnStoryCg(PacketReader r) { r.ReadInt(); r.ReadString(); }

    // ── GACHA + PVP + SOCIAL + TUTORIAL + LANG ──────────

    private void OnTopupPackages(PacketReader r) { int count = r.ReadShort(); for (int i=0;i<count;i++) { r.ReadInt(); r.ReadString(); r.ReadString(); r.ReadInt(); r.ReadInt(); r.ReadInt(); r.ReadString(); r.ReadString(); } }
    private void OnTopupUrl(PacketReader r) { string url = r.ReadString(); Application.OpenURL(url); }
    private void OnTopupSuccess(PacketReader r) { int diamond = r.ReadInt(); UIManager.Instance?.ShowNotification("Nap thanh cong! +" + diamond + " diamond", UINotificationType.Success); }
    private void OnTopupHistory(PacketReader r) { int count = r.ReadShort(); for (int i=0;i<count;i++) { r.ReadInt(); r.ReadString(); r.ReadInt(); r.ReadInt(); r.ReadString(); } }

    private void OnServerList(PacketReader r) { int count = r.ReadShort(); for (int i=0;i<count;i++) { r.ReadInt(); r.ReadString(); r.ReadString(); r.ReadInt(); r.ReadByte(); r.ReadByte(); r.ReadByte(); r.ReadByte(); } }
    private void OnChannelList(PacketReader r) { int count = r.ReadShort(); for (int i=0;i<count;i++) { r.ReadInt(); r.ReadInt(); r.ReadString(); r.ReadInt(); r.ReadByte(); } }
    private void OnServerFull(PacketReader r) { UIManager.Instance?.ShowNotification("Server day!", UINotificationType.Warning); }
    private void OnChannelChanged(PacketReader r) { int ch = r.ReadInt(); string name = r.ReadString(); }

    private void OnIntroScenes(PacketReader r) { int count = r.ReadShort(); for (int i=0;i<count;i++) { r.ReadInt(); r.ReadString(); r.ReadString(); r.ReadString(); r.ReadString(); r.ReadFloat(); r.ReadString(); r.ReadString(); } }
    private void OnIntroNotNeeded(PacketReader r) { /* skip intro, go to game */ }
    private void OnLoginScreenCfg(PacketReader r) { string json = r.ReadString(); /* apply login screen config */ }

    private void OnGachaBannerList(PacketReader r) { int count = r.ReadShort(); for (int i=0;i<count;i++) { r.ReadInt(); r.ReadString(); r.ReadString(); r.ReadInt(); r.ReadInt(); r.ReadInt(); } }
    private void OnGachaResult(PacketReader r) { int count = r.ReadShort(); for (int i=0;i<count;i++) { r.ReadString(); r.ReadInt(); r.ReadByte(); } UIManager.Instance?.ShowNotification("Trieu hoi thanh cong!", UINotificationType.Success); }
    private void OnGachaCurrency(PacketReader r) { int count = r.ReadShort(); for (int i=0;i<count;i++) { r.ReadInt(); r.ReadString(); r.ReadInt(); } }
    private void OnGachaHistory(PacketReader r) { int count = r.ReadShort(); for (int i=0;i<count;i++) { r.ReadString(); r.ReadInt(); r.ReadByte(); r.ReadInt(); } }

    private void OnPvpSeasonInfo(PacketReader r) { r.ReadInt(); r.ReadString(); r.ReadInt(); r.ReadInt(); r.ReadInt(); r.ReadString(); }
    private void OnPvpSeasonRank(PacketReader r) { int count = r.ReadShort(); for (int i=0;i<count;i++) { r.ReadString(); r.ReadInt(); r.ReadInt(); r.ReadInt(); r.ReadString(); } }

    private void OnSocialLoginOk(PacketReader r) { bool ok = r.ReadBool(); string msg = r.ReadString(); }
    private void OnSocialLinkOk(PacketReader r) { string provider = r.ReadString(); bool ok = r.ReadBool(); UIManager.Instance?.ShowNotification("Lien ket " + provider + " thanh cong!", UINotificationType.Success); }

    private void OnTutorialStep(PacketReader r) { string step = r.ReadString(); string title = r.ReadString(); string desc = r.ReadString(); string target = r.ReadString(); string arrow = r.ReadString(); }
    private void OnTutorialComplete(PacketReader r) { UIManager.Instance?.ShowNotification("Hoan thanh huong dan!", UINotificationType.Success); }

    private void OnLangPack(PacketReader r) { string json = r.ReadString(); /* Apply language pack */ }

    private void OnSettingsData(PacketReader r) { string json = r.ReadString(); /* Apply settings */ }
    private void OnSettingsDefaults(PacketReader r) { string json = r.ReadString(); /* Load default settings */ }

    private void OnClassChangeOk(PacketReader r) {
        int classId = r.ReadInt(); string className = r.ReadString();
        UIManager.Instance?.ShowNotification("Chuyen class: " + className, UINotificationType.Success);
    }

    private void OnPingFromServer(PacketReader r) { /* pong */ }

}