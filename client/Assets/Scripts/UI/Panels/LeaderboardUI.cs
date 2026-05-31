using UnityEngine;
using UnityEngine.UI;
using System.Collections.Generic;

namespace NexusIsekai.UI
{
    public class LeaderboardUI : MonoBehaviour
    {
        public static LeaderboardUI Instance;

        public class Entry
        {
            public int Rank;
            public long CharId;
            public string Name;
            public int ClassId;
            public int Gender;
            public long RankValue;
        }

        [SerializeField] Transform contentParent;
        [SerializeField] GameObject entryPrefab;
        [SerializeField] Text myRankText;

        string[] rankTypes = { "level", "pvp_rating", "guild_level", "wealth" };
        string currentType = "level";
        int myRank;
        List<Entry> entries = new();
        bool isOpen;

        void Awake()
        {
            if (Instance == null) Instance = this;
            else Destroy(gameObject);
        }

        public void Open()
        {
            isOpen = true;
            gameObject.SetActive(true);
            SwitchTab("level");
        }

        public void Close()
        {
            isOpen = false;
            gameObject.SetActive(false);
        }

        public void SwitchTab(string type)
        {
            currentType = type;
            PacketBuilder.SendLeaderboard(type);
        }

        public void OnTabLevel()       => SwitchTab("level");
        public void OnTabPvP()         => SwitchTab("pvp_rating");
        public void OnTabGuild()       => SwitchTab("guild_level");
        public void OnTabWealth()      => SwitchTab("wealth");

        public void Populate(string rankType, int myRank, List<Entry> entries)
        {
            this.myRank = myRank;
            this.entries = entries;

            if (myRankText) myRankText.text = myRank > 0 ? $"#{myRank}" : "---";

            // Clear old entries
            if (contentParent)
                foreach (Transform child in contentParent)
                    Destroy(child.gameObject);

            // Create entry rows
            foreach (var e in entries)
            {
                if (!entryPrefab || !contentParent) continue;
                var go = Instantiate(entryPrefab, contentParent);
                var texts = go.GetComponentsInChildren<Text>();
                if (texts.Length >= 4)
                {
                    texts[0].text = e.Rank <= 3 ? new[] { "", "#1", "#2", "#3" }[e.Rank] : $"#{e.Rank}";
                    texts[1].text = e.Name;
                    texts[2].text = GameData.CharacterAppearance.ClassNames[e.ClassId];
                    texts[3].text = FormatValue(rankType, e.RankValue);

                    // Gold color for top 3
                    if (e.Rank <= 3)
                        texts[0].color = new[] { Color.white, new Color(1f, 0.84f, 0f), new Color(0.75f, 0.75f, 0.75f), new Color(0.8f, 0.5f, 0.2f) }[e.Rank];
                }
            }
        }

        string FormatValue(string type, long value)
        {
            return type switch
            {
                "level" => $"Lv.{value}",
                "pvp_rating" => $"{value} ELO",
                "wealth" => value >= 1000000 ? $"{value / 1000000}M" : value >= 1000 ? $"{value / 1000}K" : value.ToString(),
                "guild_level" => $"Lv.{value}",
                _ => value.ToString()
            };
        }
    }
}
