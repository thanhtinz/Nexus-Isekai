using UnityEngine;
using UnityEngine.UI;

public class AutoCombatUI : MonoBehaviour
{
    public static AutoCombatUI Instance;

    // State
    public bool AutoEnabled { get; private set; }

    // Config (sync with server player_auto_config)
    public int PriorityBoss = 1, PriorityElite = 2, PriorityNormal = 3, PriorityPlayer = 0;
    public float AutoRange = 8f, PatrolRadius = 10f;
    public bool AutoSkill = true, UseSkillOnCD = true;
    public bool AutoLoot = true, LootRareOnly = false, LootEquipOnly = false;
    public bool AutoHpPotion = true, AutoMpPotion = true;
    public int HpThreshold = 50, MpThreshold = 30;
    public bool AutoReturn = true, AutoRevive = false;
    public int FleeHpPct = 10;
    public string SkillOrder = "1,2,3,4,5,6,7";

    [SerializeField] Toggle autoToggle;
    [SerializeField] Slider rangeSlider, hpSlider, mpSlider;

    void Awake() { if (!Instance) Instance = this; else Destroy(gameObject); }

    public void Toggle()
    {
        AutoEnabled = !AutoEnabled;
        PacketBuilder.SendAutoPlay(AutoEnabled);
        if (autoToggle) autoToggle.isOn = AutoEnabled;
    }

    public void Open()
    {
        gameObject.SetActive(true);
        // Load config from server
    }

    public void Close() { gameObject.SetActive(false); }

    public void SaveConfig()
    {
        // Send config to server as JSON
        string json = JsonUtility.ToJson(new AutoConfig {
            priority_boss = PriorityBoss, priority_elite = PriorityElite,
            priority_player = PriorityPlayer, auto_range = AutoRange,
            auto_skill = AutoSkill, auto_loot = AutoLoot,
            hp_threshold = HpThreshold, mp_threshold = MpThreshold,
            skill_order = SkillOrder, auto_revive = AutoRevive
        });
        PacketBuilder.SendSettingsSave(json);
    }

    [System.Serializable]
    struct AutoConfig {
        public int priority_boss, priority_elite, priority_player;
        public float auto_range;
        public bool auto_skill, auto_loot, auto_revive;
        public int hp_threshold, mp_threshold;
        public string skill_order;
    }
}