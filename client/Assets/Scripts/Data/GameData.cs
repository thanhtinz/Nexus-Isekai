// NexusIsekai Client - GameData.cs
// Các data model client, populate từ S2C packets + static config

using System.Collections.Generic;
using UnityEngine;

namespace NexusIsekai.Data
{
    // ─────────────────────────────────────────────────────────────────
    // PlayerData
    // ─────────────────────────────────────────────────────────────────

    public class PlayerData
    {
        public long   CharId        { get; set; }
        public string Name          { get; set; }
        public int    ClassId       { get; set; }
        public int    Gender        { get; set; }   // 0=male, 1=female
        public string ClassName     { get; set; }
        public int    Level         { get; set; }
        public long   Exp           { get; set; }
        public long   ExpToNextLevel{ get; set; }
        public int    Hp            { get; set; }
        public int    MaxHp         { get; set; }
        public int    Mp            { get; set; }
        public int    MaxMp         { get; set; }
        public int    Str           { get; set; }
        public int    Agi           { get; set; }
        public int    Intel         { get; set; }
        public int    Vit           { get; set; }
        public long   Gold          { get; set; }
        public int    MapId         { get; set; }
        public float  X             { get; set; }
        public float  Y             { get; set; }

        // Quan hệ xã hội
        public int    RelationStatus  { get; private set; }
        public long   RelationTarget  { get; private set; }
        public string RelationName    { get; private set; }

        public void UpdateRelation(int status, long targetId, string targetName)
        {
            RelationStatus = status;
            RelationTarget = targetId;
            RelationName   = targetName;
        }

        // Derived (từ equip)
        public int atk  { get; set; }
        public int def  { get; set; }
        public int maxHp => MaxHp;
        public int maxMp => MaxMp;
        public int gold  => (int)Gold;

        public string ClassDisplayName => ClassId switch {
            1 => "Kiếm Sĩ",
            2 => "Sát Thủ",
            3 => "Pháp Sư",
            4 => "Pháp Thủ",
            5 => "Cung Thủ",
            _ => "???"
        };
    }

    // ─────────────────────────────────────────────────────────────────
    // CharSlot
    // ─────────────────────────────────────────────────────────────────

    public class CharSlot
    {
        public long   CharId   { get; set; }
        public string Name     { get; set; }
        public int    ClassId  { get; set; }
        public int    Gender   { get; set; }
        public int    Level    { get; set; }
        public int    MapId    { get; set; }
        public string ClassName { get; set; }
    }

    // ─────────────────────────────────────────────────────────────────
    // InventoryItem
    // ─────────────────────────────────────────────────────────────────

    public enum ItemType { Consumable = 0, Weapon = 1, Armor = 2, Material = 3, Quest = 4, Cosmetic = 5 }

    public class InventoryItem
    {
        public long     inventoryId   { get; set; }
        public int      itemId        { get; set; }
        public string   name          { get; set; }
        public string   description   { get; set; }
        public int      quantity      { get; set; }
        public bool     isEquipped    { get; set; }
        public int      slot          { get; set; }  // 0=weapon..5=necklace, -1=bag
        public int      rarity        { get; set; }  // 0-4
        public ItemType type          { get; set; }
        public int      enhance_level { get; set; }

        // Stat bonuses
        public int atkBonus   { get; set; }
        public int defBonus   { get; set; }
        public int hpBonus    { get; set; }
        public int mpBonus    { get; set; }
        public int sellPrice  { get; set; }
    }

    // ─────────────────────────────────────────────────────────────────
    // Quest
    // ─────────────────────────────────────────────────────────────────

    public class QuestData
    {
        public int    Id          { get; set; }
        public string Title       { get; set; }
        public string Description { get; set; }
        public string RewardDesc  { get; set; }
        public int    Progress    { get; set; }
        public int    Target      { get; set; }
        public bool   IsCompleted { get; set; }
    }

    // ─────────────────────────────────────────────────────────────────
    // Map / World
    // ─────────────────────────────────────────────────────────────────

    public class MapData
    {
        public int    MapId      { get; set; }
        public string Name       { get; set; }
        public string Background { get; set; }
        public int    Width      { get; set; }
        public int    Height     { get; set; }
        public int    MinLevel   { get; set; }
    }

    public class RemotePlayer
    {
        public long   CharId    { get; set; }
        public string Name      { get; set; }
        public int    ClassId   { get; set; }
        public int    Gender    { get; set; }
        public int    Level     { get; set; }
        public int    Hp        { get; set; }
        public int    MaxHp     { get; set; }
        public float  X         { get; set; }
        public float  Y         { get; set; }
        public byte   Direction { get; set; }
    }

    public class MonsterData
    {
        public int    InstanceId { get; set; }
        public int    MonsterId  { get; set; }
        public string Name       { get; set; }
        public int    Hp         { get; set; }
        public int    MaxHp      { get; set; }
        public float  X          { get; set; }
        public float  Y          { get; set; }
        public bool   IsBoss     { get; set; }
    }

    // ─────────────────────────────────────────────────────────────────
    // Shop
    // ─────────────────────────────────────────────────────────────────

    public class ShopItem
    {
        public int    ItemId      { get; set; }
        public string Name        { get; set; }
        public string Description { get; set; }
        public int    Price       { get; set; }
        public int    Stock       { get; set; }
    }

    // ─────────────────────────────────────────────────────────────────
    // NPC
    // ─────────────────────────────────────────────────────────────────

    public enum NpcType { Generic = 0, Shop = 1, Quest = 2, Warp = 3, Mentor = 4, Storage = 5 }

    public class NpcInfo
    {
        public int    Id     { get; set; }
        public string Name   { get; set; }
        public NpcType Type  { get; set; }
        public int    ShopId { get; set; }
    }

    // ─────────────────────────────────────────────────────────────────
    // Chat
    // ─────────────────────────────────────────────────────────────────

    public class ChatMessage
    {
        public string Channel { get; set; }  // "all","map","guild","system"
        public string Sender  { get; set; }
        public string Content { get; set; }
    }

    // ─────────────────────────────────────────────────────────────────
    // Enhancement config
    // ─────────────────────────────────────────────────────────────────

    public class EnhancementConfig
    {
        public int   Level        { get; set; }
        public float SuccessRate  { get; set; }
        public int   CostGold     { get; set; }
        public int   CostDiamond  { get; set; }
    }

    // ─────────────────────────────────────────────────────────────────
    // GameData singleton — static config cache
    // ─────────────────────────────────────────────────────────────────

    public class GameData : MonoBehaviour
    {
        public static GameData Instance { get; private set; }

        private Dictionary<int, NpcInfo>          _npcs       = new();
        private Dictionary<int, EnhancementConfig> _enhancement = new();

        private void Awake()
        {
            if (Instance != null && Instance != this) { Destroy(gameObject); return; }
            Instance = this;
            DontDestroyOnLoad(gameObject);
            LoadStaticData();
        }

        private void LoadStaticData()
        {
            // Enhancement config (sync với server schema)
            var configs = new (int, float, int, int)[] {
                (1,100f, 500,  0), (2, 90f,1000,  0), (3, 80f,2000,  0),
                (4, 70f,5000,  0), (5, 60f,8000,  0), (6, 50f,12000, 0),
                (7, 40f,20000, 0), (8, 30f,30000,10), (9, 20f,50000,20),
                (10,10f,100000,50)
            };
            foreach (var (lv, rate, gold, diamond) in configs)
                _enhancement[lv] = new EnhancementConfig { Level=lv, SuccessRate=rate, CostGold=gold, CostDiamond=diamond };
        }

        public void RegisterNpc(NpcInfo npc) => _npcs[npc.Id] = npc;

        public NpcInfo GetNpc(int id) => _npcs.TryGetValue(id, out var n) ? n : null;

        public EnhancementConfig GetEnhancementConfig(int level)
            => _enhancement.TryGetValue(level, out var c) ? c : null;
    }
}

    // ─────────────────────────────────────────────────────────────────
    // Player data
    // ─────────────────────────────────────────────────────────────────

    public class PlayerData
    {
        public int    CharId       { get; set; }
        public string Name         { get; set; }
        public int    ClassId      { get; set; }
        public string ClassName    { get; set; }
        public int    Level        { get; set; }
        public long   Exp          { get; set; }
        public long   ExpToNextLevel{ get; set; }
        public int    Hp           { get; set; }
        public int    MaxHp        { get; set; }
        public int    Mp           { get; set; }
        public int    MaxMp        { get; set; }
        public int    Str          { get; set; }
        public int    Agi          { get; set; }
        public int    Intel        { get; set; }
        public long   Gold         { get; set; }
        public int    MapId        { get; set; }
        public float  X            { get; set; }
        public float  Y            { get; set; }
        public int    GuildId      { get; set; }

        // Derived
        public int AttackBonus  { get; set; }
        public int DefenseBonus { get; set; }

        public string ClassDisplayName => ClassId switch {
            1 => "Kiếm Sĩ",
            2 => "Sát Thủ",
            3 => "Pháp Sư",
            4 => "Pháp Thủ",
            5 => "Cung Thủ",
            _ => "Không xác định"
        };
    }

    // ─────────────────────────────────────────────────────────────────
    // Character select slot
    // ─────────────────────────────────────────────────────────────────

    public class CharSlot
    {
        public int    CharId   { get; set; }
        public string Name     { get; set; }
        public int    ClassId  { get; set; }
        public int    Level    { get; set; }
        public int    MapId    { get; set; }
    }

    // ─────────────────────────────────────────────────────────────────
    // Inventory
    // ─────────────────────────────────────────────────────────────────

    public class InventoryItem
    {
        public int  InstanceId  { get; set; }
        public int  ItemId      { get; set; }
        public int  Quantity    { get; set; }
        public bool IsEquipped  { get; set; }
        public int  Slot        { get; set; }

        // Bonus stats từ trang bị
        public int BonusStr { get; set; }
        public int BonusAgi { get; set; }
        public int BonusInt { get; set; }
        public int BonusHp  { get; set; }
        public int BonusMp  { get; set; }
        public int BonusAtk { get; set; }
        public int BonusDef { get; set; }
    }

    // ─────────────────────────────────────────────────────────────────
    // Quest
    // ─────────────────────────────────────────────────────────────────

    public enum QuestStatus { Active = 0, Completed = 1, Failed = 2 }

    public class QuestData
    {
        public int         QuestId      { get; set; }
        public QuestStatus Status       { get; set; }
        public int         Progress     { get; set; }
        public int         TargetCount  { get; set; }
        public string      Name         { get; set; }
        public string      Description  { get; set; }
        public int         RewardExp    { get; set; }
        public long        RewardGold   { get; set; }
        public int         RewardItemId { get; set; }
    }

    // ─────────────────────────────────────────────────────────────────
    // Map & World
    // ─────────────────────────────────────────────────────────────────

    public class MapData
    {
        public int    MapId      { get; set; }
        public string Name       { get; set; }
        public string Background { get; set; }  // tên asset
        public int    Width      { get; set; }
        public int    Height     { get; set; }
        public int    MinLevel   { get; set; }
    }

    public class RemotePlayer
    {
        public int    CharId   { get; set; }
        public string Name     { get; set; }
        public int    ClassId  { get; set; }
        public int    Level    { get; set; }
        public int    Hp       { get; set; }
        public int    MaxHp    { get; set; }
        public float  X        { get; set; }
        public float  Y        { get; set; }
        public byte   Direction{ get; set; }
    }

    public class MonsterData
    {
        public int    InstanceId { get; set; }
        public int    MonsterId  { get; set; }
        public string Name       { get; set; }
        public int    Hp         { get; set; }
        public int    MaxHp      { get; set; }
        public float  X          { get; set; }
        public float  Y          { get; set; }
        public bool   IsBoss     { get; set; }
    }

    // ─────────────────────────────────────────────────────────────────
    // Shop
    // ─────────────────────────────────────────────────────────────────

    public class ShopItem
    {
        public int ItemId  { get; set; }
        public int Price   { get; set; }
        public string Name { get; set; }
    }

    // ─────────────────────────────────────────────────────────────────
    // Chat message
    // ─────────────────────────────────────────────────────────────────

    public enum ChatChannel : byte { All = 0, Map = 1, Guild = 2, PM = 3, System = 4 }

    public class ChatMessage
    {
        public ChatChannel Channel    { get; set; }
        public string      SenderName { get; set; }
        public string      TargetName { get; set; }  // chỉ dùng khi channel=PM
        public string      Message    { get; set; }
        public bool        IsSelf     { get; set; }  // PM từ bản thân gửi đi
    
    // ─── Character Appearance (Mana Seed sprite layers) ──────

    [System.Serializable]
    public class CharacterAppearance
    {
        public int ClassId   = 1;   // 1-7 (chọn lúc tạo nhân vật)
        public int Gender    = 0;   // 0=nam, 1=nữ

        // Sprite system = 2D, loaded from server config
        public const string SpriteSystem = "2d_sprite";

        // Class names
        public static readonly string[] ClassNames = {
            "", "Kiem Si", "Phap Su", "Xa Thu", "Slinger", "Axeman", "Quyen Su", "Cung Thu"
        };

        public string GetClassName() => ClassId >= 1 && ClassId <= 7 ? ClassNames[ClassId] : "Unknown";
        public string GetSpritePath() => $"Sprites/Characters/class_{ClassId}/{(Gender == 0 ? "male" : "female")}/";
    };

        /// <summary>Sprite path cho body (loaded from server config)</summary>
        public string GetBodySpritePath() => $"Sprites/Characters/{BodyType}/body.png";

        /// <summary>Sprite path cho hair</summary>
        public string GetHairSpritePath() => $"Sprites/Characters/{BodyType}/hair_{HairStyle}.png";

        /// <summary>Sprite paths (generic, loaded from server sprite config)</summary>
        public string GetPreviewPath() => $"Sprites/Characters/{BodyType}/preview.png";
    }

    // Animation state (loaded from server config)
    [System.Serializable]
    public class AnimationState
    {
        public string StateKey;       // "walk", "attack_sword", "farm_hoe"
        public int RowDown, RowUp, RowRight, RowLeft;
        public int FrameCount;
        public float FrameRate;
        public bool IsLooping;
        public string EffectKey;
    }

}
}
