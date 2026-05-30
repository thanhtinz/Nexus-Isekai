// NexusIsekai Client — UIScripts.cs
// Tất cả UI ingame: HUD, Chat, Notification, Quest, Shop, Combat, NPC,
// MissionPass, Pet, Title, Social, Mentor, Enhancement, GiftCode
// Dùng asset sprites, không dùng emoji

using System;
using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;
using TMPro;
using NexusIsekai.Data;
using NexusIsekai.Game;
using NexusIsekai.Network;

namespace NexusIsekai.UI
{
    // ════════════════════════════════════════════════════════
    // LoginUI.cs
    // ════════════════════════════════════════════════════════

    public class LoginUI : MonoBehaviour
    {
        public static LoginUI Instance { get; private set; }

        [Header("Panels")]
        public GameObject loginPanel;
        public GameObject registerPanel;

        [Header("Login")]
        public TMP_InputField usernameInput;
        public TMP_InputField passwordInput;
        public Button         btnLogin;
        public Button         btnToRegister;
        public TMP_Text       loginError;

        [Header("Register")]
        public TMP_InputField regUsernameInput;
        public TMP_InputField regPasswordInput;
        public TMP_InputField regEmailInput;
        public Button         btnRegister;
        public Button         btnToLogin;
        public TMP_Text       regError;

        [Header("Server")]
        public TMP_Text txtServerStatus;
        public TMP_Text txtVersion;

        private void Awake()
        {
            if (Instance != null && Instance != this) { Destroy(gameObject); return; }
            Instance = this;
        }

        private void Start()
        {
            btnLogin.onClick.AddListener(OnLogin);
            btnRegister.onClick.AddListener(OnRegister);
            btnToRegister.onClick.AddListener(() => { loginPanel.SetActive(false); registerPanel.SetActive(true); });
            btnToLogin.onClick.AddListener(() => { registerPanel.SetActive(false); loginPanel.SetActive(true); });

            loginPanel.SetActive(true);
            registerPanel.SetActive(false);
            loginError.gameObject.SetActive(false);
            regError.gameObject.SetActive(false);

            // Kết nối
            if (txtVersion) txtVersion.text = Application.version;
            ConnectToServer();
        }

        private void ConnectToServer()
        {
            if (txtServerStatus) txtServerStatus.text = "Đang kết nối...";
            btnLogin.interactable = false;

            if (GameClient.Instance == null) return;

            GameClient.Instance.OnConnected    += () => {
                if (txtServerStatus) txtServerStatus.text = "Đã kết nối";
                btnLogin.interactable = true;
            };
            GameClient.Instance.OnDisconnected += (msg) => {
                if (txtServerStatus) txtServerStatus.text = $"Mất kết nối: {msg}";
                btnLogin.interactable = false;
            };

            _ = GameClient.Instance.ConnectAsync();
        }

        private void OnLogin()
        {
            string u = usernameInput.text.Trim();
            string p = passwordInput.text;
            if (string.IsNullOrEmpty(u) || string.IsNullOrEmpty(p))
            {
                ShowError(loginError, "Vui lòng nhập đầy đủ thông tin.");
                return;
            }
            PacketBuilder.SendLogin(u, p);
        }

        private void OnRegister()
        {
            string u = regUsernameInput.text.Trim();
            string p = regPasswordInput.text;
            string e = regEmailInput.text.Trim();
            if (string.IsNullOrEmpty(u) || string.IsNullOrEmpty(p))
            {
                ShowError(regError, "Vui lòng nhập đầy đủ thông tin.");
                return;
            }
            PacketBuilder.SendRegister(u, p, e);
        }

        private void ShowError(TMP_Text txt, string msg)
        {
            txt.text = msg;
            txt.gameObject.SetActive(true);
        }

        public void ShowLoginError(string msg) => ShowError(loginError, msg);
        public void ShowRegisterError(string msg) => ShowError(regError, msg);
    }

    // ════════════════════════════════════════════════════════
    // CharSelectUI.cs
    // ════════════════════════════════════════════════════════

    public class CharSelectUI : MonoBehaviour
    {
        public static CharSelectUI Instance { get; private set; }

        [Header("Char Slots (3)")]
        public GameObject[] charSlotPanels = new GameObject[3];
        public TMP_Text[]   charNames      = new TMP_Text[3];
        public TMP_Text[]   charLevels     = new TMP_Text[3];
        public Image[]      charPortraits  = new Image[3];  // Sprite từ Resources
        public Button[]     btnSelect      = new Button[3];
        public Button[]     btnDelete      = new Button[3];

        [Header("Create Form")]
        public GameObject   createPanel;
        public TMP_InputField nameInput;
        public Button[]     classButtons;   // Mỗi nút = 1 class, sprite từ Resources
        public Toggle[]     genderToggles;  // 0=male, 1=female
        public Button       btnCreate;
        public Button       btnCancelCreate;
        public TMP_Text     createError;

        [Header("Bottom")]
        public Button btnNewChar;
        public Button btnEnterGame;

        private int _selectedSlot = -1;
        private int _selectedClass = 1;
        private int _selectedGender = 0;

        private void Awake()
        {
            if (Instance != null && Instance != this) { Destroy(gameObject); return; }
            Instance = this;
        }

        private void Start()
        {
            createPanel.SetActive(false);

            for (int i = 0; i < 3; i++)
            {
                int idx = i;
                btnSelect[i]?.onClick.AddListener(() => OnSelectChar(idx));
                btnDelete[i]?.onClick.AddListener(() => OnDeleteChar(idx));
            }

            for (int i = 0; i < classButtons.Length; i++)
            {
                int cls = i + 1;
                classButtons[i]?.onClick.AddListener(() => _selectedClass = cls);
            }

            if (genderToggles.Length >= 2)
            {
                genderToggles[0].onValueChanged.AddListener(v => { if (v) _selectedGender = 0; });
                genderToggles[1].onValueChanged.AddListener(v => { if (v) _selectedGender = 1; });
            }

            btnCreate.onClick.AddListener(OnCreate);
            btnCancelCreate.onClick.AddListener(() => createPanel.SetActive(false));
            btnNewChar.onClick.AddListener(() => createPanel.SetActive(true));
            btnEnterGame.onClick.AddListener(OnEnterGame);
            btnEnterGame.interactable = false;

            PacketBuilder.SendCharList();
        }

        public void PopulateSlots(List<CharSlot> slots)
        {
            for (int i = 0; i < 3; i++)
            {
                if (i < slots.Count)
                {
                    var c = slots[i];
                    charNames[i].text  = c.Name;
                    charLevels[i].text = $"Lv.{c.Level} {c.ClassName}";
                    // Load portrait từ Resources/Portraits/class_{classId}_{gender}
                    var portrait = Resources.Load<Sprite>($"Portraits/class_{c.ClassId}_{c.Gender}");
                    if (portrait) charPortraits[i].sprite = portrait;
                    charSlotPanels[i].SetActive(true);
                    btnDelete[i].gameObject.SetActive(true);
                }
                else
                {
                    if (charNames.Length > i) charNames[i].text = "--- Trống ---";
                    if (charPortraits.Length > i && charPortraits[i]) charPortraits[i].sprite = null;
                    if (btnDelete.Length > i) btnDelete[i].gameObject.SetActive(false);
                }
            }
        }

        private void OnSelectChar(int slot)
        {
            _selectedSlot = slot;
            btnEnterGame.interactable = slot < GameState.Instance.CharSlots.Count;
            for (int i = 0; i < charSlotPanels.Length; i++)
            {
                var bg = charSlotPanels[i]?.GetComponent<Image>();
                if (bg) bg.color = i == slot ? new Color(0.3f, 0.5f, 1f, 0.4f) : Color.clear;
            }
        }

        private void OnDeleteChar(int slot)
        {
            if (slot >= GameState.Instance.CharSlots.Count) return;
            long charId = GameState.Instance.CharSlots[slot].CharId;
            PacketBuilder.SendCharDelete(charId);
        }

        private void OnCreate()
        {
            string n = nameInput.text.Trim();
            if (string.IsNullOrEmpty(n)) { createError.text = "Nhập tên nhân vật!"; return; }
            PacketBuilder.SendCharCreate(n, _selectedClass, _selectedGender);
        }

        private void OnEnterGame()
        {
            if (_selectedSlot < 0 || _selectedSlot >= GameState.Instance.CharSlots.Count) return;
            long charId = GameState.Instance.CharSlots[_selectedSlot].CharId;
            PacketBuilder.SendCharSelect(charId);
        }
    }

    // ════════════════════════════════════════════════════════
    // HUDUI.cs — HP/MP/EXP + Gold + Diamond + Minimap + Skills
    // ════════════════════════════════════════════════════════

    public class HUDUI : MonoBehaviour
    {
        public static HUDUI Instance { get; private set; }

        [Header("Stats bars")]
        public Slider   hpBar;
        public Slider   mpBar;
        public Slider   expBar;
        public TMP_Text txtHp;
        public TMP_Text txtMp;
        public TMP_Text txtLevel;
        public TMP_Text txtGold;
        public TMP_Text txtDiamond;

        [Header("Character portrait")]
        public Image    charPortrait;
        public TMP_Text charName;
        public TMP_Text charTitle;

        [Header("Minimap")]
        public RawImage minimapImage;

        [Header("Death Screen")]
        public GameObject deathScreen;
        public Button     btnRevive;

        [Header("Level Up FX")]
        public GameObject levelUpFx;
        public TMP_Text   txtLevelUpMsg;

        [Header("Skill Bar (7 slots)")]
        public SkillSlotUI[] skillSlots = new SkillSlotUI[7];

        [Header("Quick Buttons")]
        public Button btnInventory;
        public Button btnQuest;
        public Button btnMap;
        public Button btnPet;
        public Button btnPass;
        public Button btnMentor;
        public Button btnTitle;

        // Ping timer
        private float _pingTimer = 0f;
        private const float PING_INTERVAL = 30f;

        private void Awake()
        {
            if (Instance != null && Instance != this) { Destroy(gameObject); return; }
            Instance = this;
        }

        private void Start()
        {
            deathScreen?.SetActive(false);
            levelUpFx?.SetActive(false);

            btnRevive?.onClick.AddListener(OnRevive);

            // Quick buttons
            btnInventory?.onClick.AddListener(() => UIManager.Instance?.ToggleInventory());
            btnQuest?.onClick.AddListener(() => UIManager.Instance?.ToggleQuest());
            btnMap?.onClick.AddListener(() => UIManager.Instance?.ToggleMapOverlay());
            btnPet?.onClick.AddListener(() => UIManager.Instance?.TogglePetUI());
            btnPass?.onClick.AddListener(() => UIManager.Instance?.TogglePassUI());
            btnMentor?.onClick.AddListener(() => UIManager.Instance?.ToggleMentorUI());
            btnTitle?.onClick.AddListener(() => UIManager.Instance?.ToggleTitleUI());
        }

        private void Update()
        {
            // Auto ping
            _pingTimer += Time.deltaTime;
            if (_pingTimer >= PING_INTERVAL)
            {
                _pingTimer = 0f;
                PacketBuilder.SendPing();
            }

            // Keyboard shortcuts
            if (Input.GetKeyDown(KeyCode.I)) UIManager.Instance?.ToggleInventory();
            if (Input.GetKeyDown(KeyCode.Q)) UIManager.Instance?.ToggleQuest();
            if (Input.GetKeyDown(KeyCode.P)) UIManager.Instance?.TogglePassUI();
        }

        public void UpdateStats(PlayerData p)
        {
            if (p == null) return;
            hpBar.value  = p.MaxHp  > 0 ? (float)p.Hp  / p.MaxHp  : 0f;
            mpBar.value  = p.MaxMp  > 0 ? (float)p.Mp  / p.MaxMp  : 0f;
            expBar.value = p.ExpToNextLevel > 0 ? (float)p.Exp / p.ExpToNextLevel : 0f;
            txtHp.text   = $"{p.Hp}/{p.MaxHp}";
            txtMp.text   = $"{p.Mp}/{p.MaxMp}";
            txtLevel.text = $"Lv.{p.Level}";
            txtGold.text  = $"{p.Gold:N0}";
            RefreshDiamond();

            if (charName) charName.text = p.Name;

            // Portrait theo class + gender
            if (charPortrait)
            {
                var sp = Resources.Load<Sprite>($"Portraits/class_{p.ClassId}_{p.Gender}");
                if (sp) charPortrait.sprite = sp;
            }
        }

        public void RefreshDiamond()
        {
            if (txtDiamond)
                txtDiamond.text = $"{GameState.Instance.Diamond:N0}";
        }

        public void RefreshTitle()
        {
            if (!charTitle) return;
            var equipped = GameState.Instance.Titles?.Find(t => t.Equipped);
            charTitle.text = equipped != null ? equipped.Name : "";
            if (equipped != null)
            {
                ColorUtility.TryParseHtmlString("#" + equipped.ColorHex, out var col);
                charTitle.color = col;
            }
        }

        public void UpdateSkillSlot(int index, int skillId, Sprite icon, float cooldown)
        {
            if (index >= 0 && index < skillSlots.Length)
                skillSlots[index]?.SetSkill(skillId, icon, cooldown);
        }

        public void StartCooldown(int slotIndex, float duration)
        {
            if (slotIndex >= 0 && slotIndex < skillSlots.Length)
                skillSlots[slotIndex]?.StartCooldown(duration);
        }

        public void ShowLevelUp(int level)
        {
            if (txtLevelUpMsg) txtLevelUpMsg.text = $"LEVEL UP!\n{level}";
            levelUpFx?.SetActive(true);
            CancelInvoke(nameof(HideLevelUp));
            Invoke(nameof(HideLevelUp), 3f);
        }
        private void HideLevelUp() => levelUpFx?.SetActive(false);

        public void ShowDeathScreen() => deathScreen?.SetActive(true);

        private void OnRevive()
        {
            deathScreen?.SetActive(false);
            // Server sẽ respawn khi nhận disconnect/reconnect hoặc packet respawn
        }
    }

    // ════════════════════════════════════════════════════════
    // SkillSlotUI — 1 trong 7 skill slots trên HUD
    // ════════════════════════════════════════════════════════

    public class SkillSlotUI : MonoBehaviour
    {
        [Header("Refs")]
        public Image    iconImage;
        public Image    cooldownOverlay;   // full-fill Image (radial fill)
        public TMP_Text cdText;
        public Button   btnUse;
        public TMP_Text keyLabel;          // "1"-"7"

        [Header("State")]
        public int   SlotIndex;
        public int   SkillId  { get; private set; }

        private float _cdDuration;
        private float _cdRemaining;
        private bool  _onCooldown;

        private void Awake()
        {
            btnUse?.onClick.AddListener(OnUse);
            if (cooldownOverlay) cooldownOverlay.fillAmount = 0f;
        }

        public void SetSkill(int skillId, Sprite icon, float totalCooldown)
        {
            SkillId  = skillId;
            _cdDuration = totalCooldown;
            if (iconImage && icon) iconImage.sprite = icon;
            if (keyLabel) keyLabel.text = (SlotIndex + 1).ToString();
        }

        public void StartCooldown(float duration)
        {
            _cdDuration   = duration;
            _cdRemaining  = duration;
            _onCooldown   = true;
            btnUse.interactable = false;
        }

        private void Update()
        {
            if (!_onCooldown) return;
            _cdRemaining -= Time.deltaTime;
            if (_cdRemaining <= 0)
            {
                _cdRemaining = 0;
                _onCooldown  = false;
                if (cooldownOverlay) cooldownOverlay.fillAmount = 0f;
                if (cdText) cdText.text = "";
                btnUse.interactable = true;
            }
            else
            {
                float pct = _cdRemaining / _cdDuration;
                if (cooldownOverlay) cooldownOverlay.fillAmount = pct;
                if (cdText) cdText.text = Mathf.CeilToInt(_cdRemaining).ToString();
            }
        }

        private void OnUse()
        {
            if (SkillId <= 0 || _onCooldown) return;
            var pc = PlayerController.Instance;
            if (pc == null) return;
            // Dùng target hiện tại hoặc self
            PacketBuilder.SendUseSkill(SkillId, 0);
        }
    }

    // ════════════════════════════════════════════════════════
    // ChatUI.cs
    // ════════════════════════════════════════════════════════

    // ════════════════════════════════════════════════════════
    // ChatUI — đã tách ra ChatUI.cs với đầy đủ tính năng
    // ════════════════════════════════════════════════════════
    // (text, sticker, emoji, location, item, lì xì, voice, cross-server)

    // ════════════════════════════════════════════════════════
    // QuestUI.cs — Danh sách & chi tiết nhiệm vụ
    // ════════════════════════════════════════════════════════

    public class QuestUI : MonoBehaviour
    {
        public static QuestUI Instance { get; private set; }

        [Header("Panel")]
        public GameObject panel;
        public Transform  listContainer;
        public GameObject questEntryPrefab;  // TMP_Text + Button

        [Header("Detail")]
        public GameObject detailPanel;
        public TMP_Text   txtTitle;
        public TMP_Text   txtDesc;
        public TMP_Text   txtProgress;
        public TMP_Text   txtReward;
        public Button     btnComplete;
        public Button     btnAbandon;
        public Button     btnClose;

        private QuestData _selected;
        private List<GameObject> _entries = new();

        private void Awake()
        {
            if (Instance != null && Instance != this) { Destroy(gameObject); return; }
            Instance = this;
        }

        private void Start()
        {
            panel?.SetActive(false);
            detailPanel?.SetActive(false);
            btnClose?.onClick.AddListener(() => { panel.SetActive(false); detailPanel.SetActive(false); });
            btnComplete?.onClick.AddListener(OnComplete);
            btnAbandon?.onClick.AddListener(OnAbandon);
        }

        public void Toggle()
        {
            if (!panel) return;
            bool nowActive = !panel.activeSelf;
            panel.SetActive(nowActive);
            if (nowActive)
            {
                PacketBuilder.SendQuestList();
            }
        }

        public void Refresh(List<QuestData> quests)
        {
            foreach (var go in _entries) Destroy(go);
            _entries.Clear();

            foreach (var q in quests)
            {
                var go = Instantiate(questEntryPrefab, listContainer);
                _entries.Add(go);

                var txt = go.GetComponentInChildren<TMP_Text>();
                if (txt)
                {
                    string status = q.IsCompleted ? "[Xong]" : $"{q.Progress}/{q.Target}";
                    txt.text = $"{q.Title} {status}";
                }

                var btn = go.GetComponent<Button>();
                var captured = q;
                btn?.onClick.AddListener(() => ShowDetail(captured));
            }
        }

        public void UpdateProgress(int questId, int progress, int target)
        {
            var q = GameState.Instance.Quests.Find(x => x.Id == questId);
            if (q == null) return;
            q.Progress = progress;
            q.Target   = target;
            if (_selected?.Id == questId)
                txtProgress.text = $"Tiến độ: {progress}/{target}";
        }

        private void ShowDetail(QuestData q)
        {
            _selected = q;
            detailPanel?.SetActive(true);
            txtTitle.text    = q.Title;
            txtDesc.text     = q.Description;
            txtProgress.text = $"Tiến độ: {q.Progress}/{q.Target}";
            txtReward.text   = q.RewardDesc;
            btnComplete.interactable = q.IsCompleted;
        }

        private void OnComplete()
        {
            if (_selected == null) return;
            PacketBuilder.SendQuestComplete(_selected.Id);
        }

        private void OnAbandon()
        {
            if (_selected == null) return;
            PacketBuilder.SendQuestAbandon(_selected.Id);
            detailPanel?.SetActive(false);
        }
    }

    // ════════════════════════════════════════════════════════
    // ShopUI.cs — Shop trong game (dùng vàng)
    // ════════════════════════════════════════════════════════

    public class ShopUI : MonoBehaviour
    {
        public static ShopUI Instance { get; private set; }

        [Header("Panel")]
        public GameObject panel;
        public TMP_Text   txtShopName;
        public Transform  itemGrid;
        public GameObject shopItemPrefab;
        public Button     btnClose;

        [Header("Selected item")]
        public Image      selectedIcon;
        public TMP_Text   selectedName;
        public TMP_Text   selectedPrice;
        public TMP_Text   selectedDesc;
        public Button     btnBuy;

        private int _activeShopId;
        private List<ShopItem> _items = new();
        private int _selectedItemId;
        private List<GameObject> _slots = new();

        private void Awake()
        {
            if (Instance != null && Instance != this) { Destroy(gameObject); return; }
            Instance = this;
        }

        private void Start()
        {
            panel?.SetActive(false);
            btnClose?.onClick.AddListener(() => panel.SetActive(false));
            btnBuy?.onClick.AddListener(OnBuy);
        }

        public void Open(int shopId, List<ShopItem> items)
        {
            _activeShopId = shopId;
            _items = items ?? new();
            panel?.SetActive(true);
            BuildGrid();
        }

        private void BuildGrid()
        {
            foreach (var go in _slots) Destroy(go);
            _slots.Clear();

            foreach (var item in _items)
            {
                var go = Instantiate(shopItemPrefab, itemGrid);
                _slots.Add(go);

                var icon = go.transform.Find("Icon")?.GetComponent<Image>();
                if (icon)
                {
                    var sp = Resources.Load<Sprite>($"Sprites/Items/item_{item.ItemId}");
                    if (sp) icon.sprite = sp;
                }

                var priceTxt = go.transform.Find("Price")?.GetComponent<TMP_Text>();
                if (priceTxt) priceTxt.text = $"{item.Price:N0}G";

                var btn = go.GetComponent<Button>();
                var captured = item;
                btn?.onClick.AddListener(() => SelectItem(captured));
            }
        }

        private void SelectItem(ShopItem item)
        {
            _selectedItemId = item.ItemId;
            if (selectedName)  selectedName.text  = item.Name;
            if (selectedPrice) selectedPrice.text = $"{item.Price:N0} vàng";
            if (selectedDesc)  selectedDesc.text  = item.Description;
            if (selectedIcon)
            {
                var sp = Resources.Load<Sprite>($"Sprites/Items/item_{item.ItemId}");
                if (sp) selectedIcon.sprite = sp;
            }
        }

        private void OnBuy()
        {
            if (_selectedItemId <= 0) return;
            PacketBuilder.SendShopBuy(_selectedItemId, 1);
        }
    }

    // ════════════════════════════════════════════════════════
    // CombatUI.cs — Floating damage text, skill effects, loot popup
    // ════════════════════════════════════════════════════════

    public class CombatUI : MonoBehaviour
    {
        public static CombatUI Instance { get; private set; }

        [Header("Floating Text")]
        public GameObject damageTextPrefab;  // TMP_Text + animation
        public Canvas     worldCanvas;        // World-space canvas

        [Header("Loot Popup")]
        public GameObject lootPopup;
        public TMP_Text   txtLootExp;
        public TMP_Text   txtLootGold;

        [Header("Skill effect prefabs")]
        public GameObject[] skillFxPrefabs;  // index = skillId - 1

        private void Awake()
        {
            if (Instance != null && Instance != this) { Destroy(gameObject); return; }
            Instance = this;
        }

        private void Start()
        {
            lootPopup?.SetActive(false);
        }

        /// <summary>Hiện số sát thương nổi lên đầu target</summary>
        public void ShowDamage(int targetInstanceId, int damage, bool isCrit)
        {
            // Tìm vị trí target trên world
            Vector3 pos = GetTargetWorldPos(targetInstanceId);
            if (damageTextPrefab && worldCanvas)
            {
                var go = Instantiate(damageTextPrefab, pos + Vector3.up * 0.5f,
                    Quaternion.identity, worldCanvas.transform);
                var txt = go.GetComponentInChildren<TMP_Text>();
                if (txt)
                {
                    txt.text = isCrit ? $"<size=140%><b>{damage}</b></size>" : damage.ToString();
                    txt.color = isCrit ? Color.yellow : Color.white;
                }
                Destroy(go, 1.5f);
            }
        }

        public void ShowSkillEffect(int skillId, int targetInstanceId, int damage)
        {
            Vector3 pos = GetTargetWorldPos(targetInstanceId);
            int idx = skillId - 1;
            if (idx >= 0 && idx < skillFxPrefabs.Length && skillFxPrefabs[idx])
            {
                var fx = Instantiate(skillFxPrefabs[idx], pos, Quaternion.identity);
                Destroy(fx, 2f);
            }
            if (damage > 0) ShowDamage(targetInstanceId, damage, false);
        }

        public void ShowLoot(int exp, int gold)
        {
            lootPopup?.SetActive(true);
            if (txtLootExp)  txtLootExp.text  = $"+{exp} EXP";
            if (txtLootGold) txtLootGold.text = $"+{gold:N0} G";
            CancelInvoke(nameof(HideLoot));
            Invoke(nameof(HideLoot), 2.5f);
        }
        private void HideLoot() => lootPopup?.SetActive(false);

        private Vector3 GetTargetWorldPos(int instanceId)
        {
            // Tìm MonsterObject hoặc PlayerObject theo instanceId
            foreach (var mo in FindObjectsOfType<MonsterObject>())
                if (mo.InstanceId == instanceId) return mo.transform.position;
            return Camera.main?.transform.position ?? Vector3.zero;
        }
    }

    // ════════════════════════════════════════════════════════
    // NpcInteractionUI.cs — Dialog + shop + quest NPC
    // ════════════════════════════════════════════════════════

    public class NpcInteractionUI : MonoBehaviour
    {
        public static NpcInteractionUI Instance { get; private set; }

        [Header("Panel")]
        public GameObject panel;
        public TMP_Text   npcName;
        public TMP_Text   dialogText;
        public Transform  optionContainer;
        public GameObject optionPrefab;   // Button + TMP_Text
        public Button     btnClose;

        [Header("NPC portrait")]
        public Image npcPortrait;

        private List<GameObject> _options = new();

        private void Awake()
        {
            if (Instance != null && Instance != this) { Destroy(gameObject); return; }
            Instance = this;
        }

        private void Start()
        {
            panel?.SetActive(false);
            btnClose?.onClick.AddListener(() => panel.SetActive(false));
        }

        public void Open(int npcId)
        {
            panel?.SetActive(true);

            // Load NPC data
            var npc = GameData.Instance?.GetNpc(npcId);
            if (npc == null) { panel.SetActive(false); return; }

            if (npcName) npcName.text = npc.Name;

            var portrait = Resources.Load<Sprite>($"Portraits/npc_{npcId}");
            if (npcPortrait && portrait) npcPortrait.sprite = portrait;

            ClearOptions();

            // Tùy loại NPC
            switch (npc.Type)
            {
                case NpcType.Shop:
                    AddOption("Mua bán", () => {
                        panel.SetActive(false);
                        PacketBuilder.SendShopOpen(npc.ShopId);
                    });
                    break;
                case NpcType.Quest:
                    AddOption("Nhiệm vụ", () => {
                        panel.SetActive(false);
                        UIManager.Instance?.ToggleQuest();
                    });
                    break;
                case NpcType.Mentor:
                    AddOption("Bái sư", () => {
                        PacketBuilder.SendMentorAccept(-(npcId)); // NPC mentor dùng id âm
                        panel.SetActive(false);
                    });
                    break;
            }
            AddOption("Tạm biệt", () => panel.SetActive(false));
        }

        private void AddOption(string label, Action onClick)
        {
            var go = Instantiate(optionPrefab, optionContainer);
            _options.Add(go);
            var txt = go.GetComponentInChildren<TMP_Text>();
            if (txt) txt.text = label;
            var btn = go.GetComponent<Button>();
            btn?.onClick.AddListener(() => onClick());
        }

        private void ClearOptions()
        {
            foreach (var go in _options) Destroy(go);
            _options.Clear();
        }
    }

    // ════════════════════════════════════════════════════════
    // MissionPassUI.cs — Sổ Sứ Mệnh ingame (free + premium)
    // ════════════════════════════════════════════════════════

    public class MissionPassUI : MonoBehaviour
    {
        public static MissionPassUI Instance { get; private set; }

        [Header("Panel")]
        public GameObject panel;
        public Button     btnClose;

        [Header("Season info")]
        public TMP_Text   txtSeasonName;
        public TMP_Text   txtPassLevel;
        public Slider     expBar;
        public TMP_Text   txtExpProgress;
        public TMP_Text   txtEndDate;

        [Header("Premium")]
        public GameObject premiumBadge;
        public Button     btnBuyPremium;
        public TMP_Text   txtPremiumPrice;

        [Header("Reward list")]
        public ScrollRect rewardScroll;
        public Transform  rewardContainer;
        public GameObject rewardRowPrefab; // 1 hàng: level + free slot + premium slot

        [Header("Tasks")]
        public Transform  taskContainer;
        public GameObject taskEntryPrefab;

        // Cache
        private int  _seasonId;
        private int  _passLevel;
        private bool _hasPremium;
        private int  _premiumDiamond;
        private List<string> _claimedRewards = new();

        private void Awake()
        {
            if (Instance != null && Instance != this) { Destroy(gameObject); return; }
            Instance = this;
        }

        private void Start()
        {
            panel?.SetActive(false);
            btnClose?.onClick.AddListener(() => panel.SetActive(false));
            btnBuyPremium?.onClick.AddListener(OnBuyPremium);
        }

        public void Toggle()
        {
            if (!panel) return;
            bool nowActive = !panel.activeSelf;
            panel.SetActive(nowActive);
            if (nowActive) PacketBuilder.SendPassInfo();
        }

        /// <summary>Nhận dữ liệu pass từ server (S2C_PASS_INFO)</summary>
        public void Populate(int seasonId, bool isActive, int freeDiamond, int premiumDiamond,
            int maxLevel, int passLevel, int passExp, bool hasPremium,
            List<PassRewardData> rewards)
        {
            _seasonId      = seasonId;
            _passLevel     = passLevel;
            _hasPremium    = hasPremium;
            _premiumDiamond = premiumDiamond;

            if (txtPassLevel) txtPassLevel.text = $"Level {passLevel}";
            if (expBar)       expBar.value = (float)passExp / 100f;
            if (txtExpProgress) txtExpProgress.text = $"{passExp}/100 EXP";
            if (premiumBadge) premiumBadge.SetActive(hasPremium);
            if (btnBuyPremium) btnBuyPremium.gameObject.SetActive(!hasPremium && premiumDiamond > 0);
            if (txtPremiumPrice) txtPremiumPrice.text = $"{premiumDiamond:N0}";

            BuildRewardList(rewards, maxLevel, passLevel, hasPremium);
        }

        private void BuildRewardList(List<PassRewardData> rewards, int maxLevel, int passLevel, bool hasPremium)
        {
            foreach (Transform t in rewardContainer) Destroy(t.gameObject);

            // Nhóm theo level
            for (int lv = 1; lv <= maxLevel; lv++)
            {
                var freeR    = rewards.Find(r => r.Level == lv && r.Tier == 0);
                var premiumR = rewards.Find(r => r.Level == lv && r.Tier == 1);

                var row = Instantiate(rewardRowPrefab, rewardContainer);

                // Level label
                var lvTxt = row.transform.Find("LevelText")?.GetComponent<TMP_Text>();
                if (lvTxt) lvTxt.text = lv.ToString();

                // Highlight current level
                var bg = row.GetComponent<Image>();
                if (bg && lv == passLevel) bg.color = new Color(0.3f, 0.5f, 1f, 0.3f);

                // Free slot
                SetRewardSlot(row.transform.Find("FreeSlot"), freeR, lv <= passLevel,
                    _claimedRewards.Contains($"{lv}_0"));

                // Premium slot
                SetRewardSlot(row.transform.Find("PremiumSlot"), premiumR, lv <= passLevel && hasPremium,
                    _claimedRewards.Contains($"{lv}_1"));
            }
        }

        private void SetRewardSlot(Transform slot, PassRewardData reward, bool unlocked, bool claimed)
        {
            if (!slot) return;

            var iconImg = slot.Find("Icon")?.GetComponent<Image>();
            var lockImg = slot.Find("LockOverlay")?.GetComponent<Image>();
            var claimImg= slot.Find("ClaimedMark")?.GetComponent<Image>();
            var btn     = slot.GetComponent<Button>();

            if (lockImg)  lockImg.gameObject.SetActive(!unlocked && !claimed);
            if (claimImg) claimImg.gameObject.SetActive(claimed);

            if (reward != null && iconImg)
            {
                Sprite sp = null;
                if (reward.ItemId > 0)
                    sp = Resources.Load<Sprite>($"Sprites/Items/item_{reward.ItemId}");
                else if (reward.Diamond > 0)
                    sp = Resources.Load<Sprite>("Sprites/Icons/diamond");
                else if (reward.Gold > 0)
                    sp = Resources.Load<Sprite>("Sprites/Icons/gold");
                if (sp) iconImg.sprite = sp;
            }

            btn?.onClick.RemoveAllListeners();
            if (btn && reward != null && unlocked && !claimed)
            {
                var capturedLv   = reward.Level;
                var capturedTier = reward.Tier;
                btn.onClick.AddListener(() => PacketBuilder.SendPassClaim(capturedLv, (byte)capturedTier));
            }
        }

        private void OnBuyPremium()
        {
            // Mua premium ingame (deduct diamond từ account server-side)
            PacketBuilder.SendBuyPremiumPass();
        }

        public void MarkClaimed(int level, int tier)
        {
            _claimedRewards.Add($"{level}_{tier}");
        }

        // ─── DTOs ─────────────────────────────────────────────────

        public class PassRewardData
        {
            public int Level, Tier, ItemId, ItemQty, Diamond, Gold;
        }
    }

    // ════════════════════════════════════════════════════════
    // PetUI.cs — Quản lý pet + mount
    // ════════════════════════════════════════════════════════

    public class PetUI : MonoBehaviour
    {
        public static PetUI Instance { get; private set; }

        [Header("Panel")]
        public GameObject panel;
        public Button     btnClose;

        [Header("Tabs")]
        public Button btnTabPets;
        public Button btnTabMounts;
        public GameObject petListPanel;
        public GameObject mountListPanel;

        [Header("Pet list")]
        public Transform  petGrid;
        public GameObject petSlotPrefab;

        [Header("Mount list")]
        public Transform  mountGrid;
        public GameObject mountSlotPrefab;

        [Header("Active pet display (HUD)")]
        public Image    activePetIcon;
        public TMP_Text activePetName;
        public Slider   activePetHunger;

        private void Awake()
        {
            if (Instance != null && Instance != this) { Destroy(gameObject); return; }
            Instance = this;
        }

        private void Start()
        {
            panel?.SetActive(false);
            btnClose?.onClick.AddListener(() => panel.SetActive(false));
            btnTabPets?.onClick.AddListener(() => { petListPanel.SetActive(true); mountListPanel.SetActive(false); });
            btnTabMounts?.onClick.AddListener(() => { petListPanel.SetActive(false); mountListPanel.SetActive(true); });
        }

        public void Toggle()
        {
            if (!panel) return;
            bool nowActive = !panel.activeSelf;
            panel.SetActive(nowActive);
            if (nowActive)
            {
                PacketBuilder.SendPetList();
                PacketBuilder.SendMountList();
            }
        }

        public void PopulatePets(List<GameObjects.PetInfo> pets)
        {
            foreach (Transform t in petGrid) Destroy(t.gameObject);

            foreach (var pet in pets)
            {
                var slot = Instantiate(petSlotPrefab, petGrid);

                var nameT = slot.transform.Find("Name")?.GetComponent<TMP_Text>();
                if (nameT) nameT.text = pet.Name;

                var lvT = slot.transform.Find("Level")?.GetComponent<TMP_Text>();
                if (lvT) lvT.text = $"Lv.{pet.Level}";

                var icon = slot.transform.Find("Icon")?.GetComponent<Image>();
                if (icon)
                {
                    var sp = Resources.Load<Sprite>($"Sprites/Pets/pet_{pet.IconId}");
                    if (sp) icon.sprite = sp;
                }

                // Active indicator
                var activeDot = slot.transform.Find("ActiveDot")?.GetComponent<Image>();
                if (activeDot) activeDot.gameObject.SetActive(pet.IsActive);

                // Buttons
                var btnActivate = slot.transform.Find("BtnActivate")?.GetComponent<Button>();
                var capturedId = pet.PetId;
                btnActivate?.onClick.AddListener(() => PacketBuilder.SendPetSetActive(capturedId));

                var btnFeed = slot.transform.Find("BtnFeed")?.GetComponent<Button>();
                btnFeed?.onClick.AddListener(() => PacketBuilder.SendPetFeed(capturedId));

                // Update active pet HUD
                if (pet.IsActive) UpdateActivePetHUD(pet);
            }
        }

        public void PopulateMounts(List<GameObjects.MountInfo> mounts)
        {
            foreach (Transform t in mountGrid) Destroy(t.gameObject);
            foreach (var m in mounts)
            {
                var slot = Instantiate(mountSlotPrefab, mountGrid);
                var nameT = slot.transform.Find("Name")?.GetComponent<TMP_Text>();
                if (nameT) nameT.text = $"{m.Name} (+{(int)(m.SpeedBonus*100)}% spd)";

                var icon = slot.transform.Find("Icon")?.GetComponent<Image>();
                if (icon)
                {
                    var sp = Resources.Load<Sprite>($"Sprites/Mounts/mount_{m.IconId}");
                    if (sp) icon.sprite = sp;
                }

                var activeDot = slot.transform.Find("ActiveDot")?.GetComponent<Image>();
                if (activeDot) activeDot.gameObject.SetActive(m.IsActive);

                var btnActivate = slot.transform.Find("BtnActivate")?.GetComponent<Button>();
                var capturedId = m.MountId;
                btnActivate?.onClick.AddListener(() => PacketBuilder.SendMountSetActive(capturedId));
            }
        }

        private void UpdateActivePetHUD(GameObjects.PetInfo pet)
        {
            if (activePetName) activePetName.text = pet.Name;
            if (activePetHunger) activePetHunger.value = pet.Hunger / 100f;
            if (activePetIcon)
            {
                var sp = Resources.Load<Sprite>($"Sprites/Pets/pet_{pet.IconId}");
                if (sp) activePetIcon.sprite = sp;
            }
        }
    }

    // ════════════════════════════════════════════════════════
    // TitleUI.cs — Quản lý danh hiệu
    // ════════════════════════════════════════════════════════

    public class TitleUI : MonoBehaviour
    {
        public static TitleUI Instance { get; private set; }

        [Header("Panel")]
        public GameObject panel;
        public Button     btnClose;
        public Transform  titleList;
        public GameObject titleEntryPrefab;

        private void Awake()
        {
            if (Instance != null && Instance != this) { Destroy(gameObject); return; }
            Instance = this;
        }

        private void Start()
        {
            panel?.SetActive(false);
            btnClose?.onClick.AddListener(() => panel.SetActive(false));
        }

        public void Toggle()
        {
            if (!panel) return;
            bool nowActive = !panel.activeSelf;
            panel.SetActive(nowActive);
            if (nowActive) PacketBuilder.SendTitleList();
        }

        public void Populate(List<GameObjects.TitleInfo> titles)
        {
            foreach (Transform t in titleList) Destroy(t.gameObject);
            foreach (var title in titles)
            {
                var go = Instantiate(titleEntryPrefab, titleList);

                var txt = go.GetComponentInChildren<TMP_Text>();
                if (txt)
                {
                    ColorUtility.TryParseHtmlString("#" + title.ColorHex, out var col);
                    txt.text  = title.Name;
                    txt.color = col;
                }

                // Equipped checkmark
                var check = go.transform.Find("EquippedCheck")?.GetComponent<Image>();
                if (check) check.gameObject.SetActive(title.Equipped);

                var btn = go.GetComponent<Button>();
                var capturedId = title.TitleId;
                btn?.onClick.AddListener(() => PacketBuilder.SendTitleEquip(capturedId));
            }
        }
    }

    // ════════════════════════════════════════════════════════
    // MentorUI.cs — Hệ thống sư đồ ingame
    // ════════════════════════════════════════════════════════

    public class MentorUI : MonoBehaviour
    {
        public static MentorUI Instance { get; private set; }

        [Header("Panel")]
        public GameObject panel;
        public Button     btnClose;

        [Header("Mentor info")]
        public GameObject mentorSection;
        public TMP_Text   txtMentorName;
        public Button     btnGraduate;

        [Header("Student list")]
        public Transform  studentList;
        public GameObject studentEntryPrefab;
        public TMP_Text   txtStudentCount;

        private void Awake()
        {
            if (Instance != null && Instance != this) { Destroy(gameObject); return; }
            Instance = this;
        }

        private void Start()
        {
            panel?.SetActive(false);
            btnClose?.onClick.AddListener(() => panel.SetActive(false));
            btnGraduate?.onClick.AddListener(() => PacketBuilder.SendMentorGraduate());
        }

        public void Toggle()
        {
            if (!panel) return;
            bool nowActive = !panel.activeSelf;
            panel.SetActive(nowActive);
            if (nowActive) PacketBuilder.SendMentorInfo();
        }

        public void Populate(bool hasMentor, long mentorId, bool isNpc,
            List<GameObjects.MentorRelation> students)
        {
            mentorSection?.SetActive(hasMentor);
            if (hasMentor && txtMentorName)
                txtMentorName.text = isNpc ? $"NPC #{-mentorId}" : $"Player #{mentorId}";

            if (btnGraduate) btnGraduate.gameObject.SetActive(hasMentor);

            if (txtStudentCount) txtStudentCount.text = $"Đệ tử: {students.Count}/5";

            foreach (Transform t in studentList) Destroy(t.gameObject);
            foreach (var s in students)
            {
                var go = Instantiate(studentEntryPrefab, studentList);
                var txt = go.GetComponentInChildren<TMP_Text>();
                if (txt) txt.text = $"Player #{s.StudentId}";
            }
        }
    }

    // ════════════════════════════════════════════════════════
    // GiftCodeUI.cs — Nhập gift code ingame
    // ════════════════════════════════════════════════════════

    public class GiftCodeUI : MonoBehaviour
    {
        public static GiftCodeUI Instance { get; private set; }

        [Header("Panel")]
        public GameObject     panel;
        public TMP_InputField codeInput;
        public Button         btnRedeem;
        public Button         btnClose;
        public TMP_Text       resultText;

        private void Awake()
        {
            if (Instance != null && Instance != this) { Destroy(gameObject); return; }
            Instance = this;
        }

        private void Start()
        {
            panel?.SetActive(false);
            btnClose?.onClick.AddListener(() => panel.SetActive(false));
            btnRedeem?.onClick.AddListener(OnRedeem);
            resultText?.gameObject.SetActive(false);
        }

        public void Toggle()
        {
            if (panel) panel.SetActive(!panel.activeSelf);
        }

        private void OnRedeem()
        {
            string code = codeInput?.text.Trim().ToUpper();
            if (string.IsNullOrEmpty(code)) return;
            PacketBuilder.SendGiftCode(code);
            if (codeInput) codeInput.text = "";
        }

        public void ShowResult(bool success, string msg)
        {
            if (!resultText) return;
            resultText.gameObject.SetActive(true);
            resultText.text  = msg;
            resultText.color = success ? Color.green : Color.red;
            CancelInvoke(nameof(HideResult));
            Invoke(nameof(HideResult), 4f);
        }
        private void HideResult() => resultText?.gameObject.SetActive(false);
    }

    // ════════════════════════════════════════════════════════
    // EnhancementUI.cs — Cường hoá vũ khí/trang bị
    // ════════════════════════════════════════════════════════

    public class EnhancementUI : MonoBehaviour
    {
        public static EnhancementUI Instance { get; private set; }

        [Header("Panel")]
        public GameObject panel;
        public Button     btnClose;

        [Header("Item to enhance")]
        public Image    itemIcon;
        public TMP_Text itemName;
        public TMP_Text enhanceLevel;
        public TMP_Text successRate;
        public TMP_Text costGold;
        public TMP_Text costDiamond;

        [Header("Actions")]
        public Button   btnEnhance;
        public Button   btnSelectItem;
        public TMP_Text resultText;

        [Header("FX")]
        public GameObject fxSuccess;
        public GameObject fxFail;

        private long _selectedItemId = -1;

        private void Awake()
        {
            if (Instance != null && Instance != this) { Destroy(gameObject); return; }
            Instance = this;
        }

        private void Start()
        {
            panel?.SetActive(false);
            btnClose?.onClick.AddListener(() => panel.SetActive(false));
            btnEnhance?.onClick.AddListener(OnEnhance);
            btnSelectItem?.onClick.AddListener(OnSelectItem);
            fxSuccess?.SetActive(false);
            fxFail?.SetActive(false);
            if (resultText) resultText.gameObject.SetActive(false);
        }

        public void Toggle() { if (panel) panel.SetActive(!panel.activeSelf); }

        private void OnSelectItem()
        {
            // Mở inventory ở chế độ "enhance select" - khi click item sẽ gọi SetSelectedItem
            InventoryUI.Instance?.OpenForEnhancement(item =>
            {
                SetSelectedItem(item);
                UIManager.Instance?.CloseInventory();
            });
        }

        public void SetSelectedItem(InventoryItem item)
        {
            _selectedItemId = item.inventoryId;
            if (itemName) itemName.text = $"{item.name} +{item.enhance_level}";
            if (enhanceLevel) enhanceLevel.text = $"+{item.enhance_level} → +{item.enhance_level + 1}";

            if (itemIcon)
            {
                var sp = Resources.Load<Sprite>($"Sprites/Items/item_{item.itemId}");
                if (sp) itemIcon.sprite = sp;
            }

            // Hiện config rate (lấy từ GameData)
            int nextLevel = item.enhance_level + 1;
            var config = GameData.Instance?.GetEnhancementConfig(nextLevel);
            if (config != null)
            {
                if (successRate) successRate.text = $"Tỉ lệ: {config.SuccessRate:F0}%";
                if (costGold)    costGold.text    = $"{config.CostGold:N0} G";
                if (costDiamond) costDiamond.text = config.CostDiamond > 0 ? $"{config.CostDiamond} Diamond" : "-";
            }
        }

        private void OnEnhance()
        {
            if (_selectedItemId < 0) return;
            PacketBuilder.SendEnhanceItem(_selectedItemId);
        }

        public void ShowResult(bool success)
        {
            if (resultText)
            {
                resultText.gameObject.SetActive(true);
                resultText.text  = success ? "Cường hoá thành công!" : "Cường hoá thất bại!";
                resultText.color = success ? Color.green : Color.red;
            }
            (success ? fxSuccess : fxFail)?.SetActive(true);
            StartCoroutine(HideResultCo(success));
        }

        private IEnumerator HideResultCo(bool success)
        {
            yield return new WaitForSeconds(2f);
            resultText?.gameObject.SetActive(false);
            (success ? fxSuccess : fxFail)?.SetActive(false);
        }
    }

    // ════════════════════════════════════════════════════════
    // UIManager.cs — Điều phối tất cả UI panels
    // ════════════════════════════════════════════════════════

    public enum UINotificationType { Info, Success, Warning, Error }

    public class UIManager : MonoBehaviour
    {
        public static UIManager Instance { get; private set; }

        [Header("Notification")]
        public GameObject notifPanel;
        public TMP_Text   notifText;
        public Image      notifIcon;

        [Header("Sprites for notification types")]
        public Sprite iconInfo;
        public Sprite iconSuccess;
        public Sprite iconWarning;
        public Sprite iconError;

        [Header("Loading")]
        public GameObject loadingOverlay;
        public TMP_Text   loadingText;

        private void Awake()
        {
            if (Instance != null && Instance != this) { Destroy(gameObject); return; }
            Instance = this;
            DontDestroyOnLoad(gameObject);
        }

        private void Start()
        {
            notifPanel?.SetActive(false);
            loadingOverlay?.SetActive(false);
        }

        // ─── Notification ────────────────────────────────────────

        public void ShowNotification(string msg, UINotificationType type = UINotificationType.Info)
        {
            if (!notifPanel) return;
            notifText.text  = msg;
            notifText.color = type switch {
                UINotificationType.Success => new Color(0.2f, 1f, 0.4f),
                UINotificationType.Warning => new Color(1f, 0.85f, 0.2f),
                UINotificationType.Error   => new Color(1f, 0.3f, 0.3f),
                _                          => Color.white
            };
            if (notifIcon)
                notifIcon.sprite = type switch {
                    UINotificationType.Success => iconSuccess,
                    UINotificationType.Warning => iconWarning,
                    UINotificationType.Error   => iconError,
                    _                          => iconInfo
                };
            notifPanel.SetActive(true);
            CancelInvoke(nameof(HideNotif));
            Invoke(nameof(HideNotif), 3.5f);
        }
        private void HideNotif() => notifPanel?.SetActive(false);

        // ─── Loading ──────────────────────────────────────────────

        public void ShowLoading(string msg = "Đang tải...")
        {
            if (loadingOverlay) { loadingOverlay.SetActive(true); }
            if (loadingText)    { loadingText.text = msg; }
        }
        public void HideLoading() => loadingOverlay?.SetActive(false);

        // ─── Panel toggles ────────────────────────────────────────

        public void ToggleInventory()   => InventoryUI.Instance?.Toggle();
        public void CloseInventory()    => InventoryUI.Instance?.panel?.SetActive(false);
        public void ToggleQuest()       => QuestUI.Instance?.Toggle();
        public void TogglePetUI()       => PetUI.Instance?.Toggle();
        public void TogglePassUI()      => MissionPassUI.Instance?.Toggle();
        public void ToggleMentorUI()    => MentorUI.Instance?.Toggle();
        public void ToggleTitleUI()     => TitleUI.Instance?.Toggle();
        public void ToggleGiftCode()    => GiftCodeUI.Instance?.Toggle();
        public void ToggleEnhancement() => EnhancementUI.Instance?.Toggle();

        public void ToggleMapOverlay()
        {
            if (!mapOverlay) return;
            mapOverlay.SetActive(!mapOverlay.activeSelf);
        }

        // ── Confirm Dialog ────────────────────────────────────────

        [Header("Confirm Dialog")]
        public GameObject  confirmDialog;
        public TMP_Text    confirmTitle;
        public TMP_Text    confirmBody;
        public Button      confirmYes;
        public Button      confirmNo;

        public void ShowConfirmDialog(string title, string body, System.Action onYes, System.Action onNo = null)
        {
            if (!confirmDialog) { onYes?.Invoke(); return; }
            confirmDialog.SetActive(true);
            if (confirmTitle) confirmTitle.text = title;
            if (confirmBody)  confirmBody.text  = body;
            confirmYes.onClick.RemoveAllListeners();
            confirmNo.onClick.RemoveAllListeners();
            confirmYes.onClick.AddListener(() => { confirmDialog.SetActive(false); onYes?.Invoke(); });
            confirmNo.onClick.AddListener(() =>  { confirmDialog.SetActive(false); onNo?.Invoke(); });
        }

        [Header("Map Overlay")]
        public GameObject mapOverlay;
    }

    // ════════════════════════════════════════════════════════
    // StoryUI.cs — Cốt truyện / Cutscene
    // ════════════════════════════════════════════════════════

    public class StoryUI : MonoBehaviour
    {
        public static StoryUI Instance { get; private set; }

        public GameObject panel;
        public Image      cgImage;
        public TMP_Text   storyText;
        public Button     btnNext;
        public Button     btnSkip;

        private List<string> _lines = new();
        private int _lineIndex;

        private void Awake()
        {
            if (Instance != null && Instance != this) { Destroy(gameObject); return; }
            Instance = this;
        }

        private void Start()
        {
            btnNext?.onClick.AddListener(NextLine);
            btnSkip?.onClick.AddListener(() => panel.SetActive(false));
            panel?.SetActive(false);
        }

        public void ShowStory(string story, string cgPath = null)
        {
            _lines = new List<string>(story.Split('\n'));
            _lineIndex = 0;
            panel?.SetActive(true);
            if (cgImage && !string.IsNullOrEmpty(cgPath))
            {
                var sp = Resources.Load<Sprite>(cgPath);
                if (sp) cgImage.sprite = sp;
            }
            ShowLine();
        }

        private void ShowLine()
        {
            if (_lineIndex < _lines.Count)
                storyText.text = _lines[_lineIndex];
        }

        private void NextLine()
        {
            _lineIndex++;
            if (_lineIndex >= _lines.Count) { panel.SetActive(false); return; }
            ShowLine();
        }
    }

} // namespace NexusIsekai.UI
