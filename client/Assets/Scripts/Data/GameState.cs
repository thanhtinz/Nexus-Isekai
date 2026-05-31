// NexusIsekai Client - GameState.cs
// Singleton lưu toàn bộ state game phía client

using System.Collections.Generic;
using NexusIsekai.Data;
using UnityEngine;

namespace NexusIsekai.Game
{
    public class GameState : MonoBehaviour
    {
        public static GameState Instance { get; private set; }

        // ── Auth & Session ───────────────────────────────────────
        public string LoggedInUsername  { get; set; }
        public bool   IsLoggedIn        { get; set; }

        // ── Character Select ────────────────────────────────────
        public List<CharSlot>    CharSlots  { get; set; } = new();
        public CharSlot          SelectedChar{ get; set; }

        // ── In-game player ──────────────────────────────────────
        public PlayerData              MyPlayer   { get; set; }
        public List<InventoryItem>     Inventory  { get; set; } = new();
        public List<QuestData>         Quests     { get; set; } = new();

        // ── World state ─────────────────────────────────────────
        public MapData                              CurrentMap { get; set; }
        public Dictionary<long, RemotePlayer>       Players    { get; set; } = new();
        public Dictionary<int, MonsterData>         Monsters   { get; set; } = new();

        // ── Shop ─────────────────────────────────────────────────
        public int            ActiveShopId  { get; set; }
        public List<ShopItem> ShopItems     { get; set; } = new();

        // ── Currency ─────────────────────────────────────────────
        public int Diamond { get; set; } = 0;

        // ── Titles ───────────────────────────────────────────────
        public List<GameObjects.TitleInfo> Titles { get; set; } = new();

        // ── Pet & Mount ──────────────────────────────────────────
        public List<GameObjects.PetInfo>   Pets   { get; set; } = new();
        public List<GameObjects.MountInfo> Mounts { get; set; } = new();

        // ── Social ───────────────────────────────────────────────
        public List<GameObjects.ChildInfo>    Children { get; set; } = new();
        public GameObjects.MentorRelation     Mentor   { get; set; }

        // ── Chat ────────────────────────────────────────────────
        public List<GameObjects.ChatMessage> ChatHistory { get; set; } = new();
        public const int MaxChatHistory = 200;

        public long AccountId { get; set; }   // account_id của session
        public string WebToken { get; set; }  // web token cho voice upload

        // ─────────────────────────────────────────────────────────

        private void Awake()
        {
            if (Instance != null && Instance != this) { Destroy(gameObject); return; }
            Instance = this;
            DontDestroyOnLoad(gameObject);
        }

        public void AddChatMessage(GameObjects.ChatMessage msg)
        {
            ChatHistory.Add(msg);
            if (ChatHistory.Count > MaxChatHistory)
                ChatHistory.RemoveAt(0);
        }

        public void AddDiamond(int amount)
        {
            Diamond += amount;
        }

        public void Reset()
        {
            IsLoggedIn  = false;
            MyPlayer    = null;
            Diamond     = 0;
            Inventory.Clear();
            Quests.Clear();
            Players.Clear();
            Monsters.Clear();
            ChatHistory.Clear();
            CharSlots.Clear();
            Titles.Clear();
            Pets.Clear();
            Mounts.Clear();
            Children.Clear();
            Mentor = null;
        }
    }

    // ── Visual hooks (animation impl khi co content) ──
    public void PlayCharAction(long charId, int actionId) { /* play action anim cho char trong zone */ }
    public void ShowEmote(long charId, int emoteId) { /* hien bieu cam tren dau char */ }
    public void PlayPairAction(long a, long b, int actionId) { /* play pair anim cho 2 char */ }

    public void PlayFurnitureAnimation(long charId, string animState){ /* ngồi ghế/nằm giường/ăn/uống */ }
    public void StopFurnitureAnimation(long charId){ /* đứng dậy */ }
}
