using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;
using TMPro;

/// <summary>
/// Quest panel: hiện danh sách quest đang làm, progress bar, nút complete/abandon.
/// Gán vào prefab QuestPanel trong scene Game.
/// </summary>
public class QuestUI : MonoBehaviour
{
    public static QuestUI Instance { get; private set; }

    [Header("Panel")]
    [SerializeField] private GameObject panel;

    [Header("Quest List")]
    [SerializeField] private Transform  listParent;
    [SerializeField] private GameObject questRowPrefab;  // Prefab: Text tên, Slider progress, 2 buttons

    [Header("Detail Panel")]
    [SerializeField] private GameObject detailPanel;
    [SerializeField] private TMP_Text   txtQuestName;
    [SerializeField] private TMP_Text   txtQuestDesc;
    [SerializeField] private TMP_Text   txtObjective;
    [SerializeField] private Slider     progressBar;
    [SerializeField] private TMP_Text   txtProgress;
    [SerializeField] private TMP_Text   txtReward;
    [SerializeField] private Button     btnComplete;
    [SerializeField] private Button     btnAbandon;
    [SerializeField] private Button     btnDetailClose;

    // Tabs: đang làm / hoàn thành
    [Header("Tabs")]
    [SerializeField] private Button     tabActive;
    [SerializeField] private Button     tabDone;
    [SerializeField] private Color      tabActiveColor   = new(0.3f, 0.6f, 1f);
    [SerializeField] private Color      tabInactiveColor = new(0.2f, 0.2f, 0.2f);

    // Runtime
    private List<QuestData>  _quests  = new();
    private List<GameObject> _rows    = new();
    private QuestData        _selected;
    private bool             _showDone;

    private void Awake()
    {
        if (Instance != null && Instance != this) { Destroy(gameObject); return; }
        Instance = this;

        btnComplete?.onClick.AddListener(OnClickComplete);
        btnAbandon?.onClick.AddListener(OnClickAbandon);
        btnDetailClose?.onClick.AddListener(() => detailPanel?.SetActive(false));
        tabActive?.onClick.AddListener(() => SwitchTab(false));
        tabDone?.onClick.AddListener(() => SwitchTab(true));

        if (panel) panel.SetActive(false);
        if (detailPanel) detailPanel.SetActive(false);
    }

    // ─────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────

    public void Toggle()
    {
        if (!panel) return;
        bool active = !panel.activeSelf;
        panel.SetActive(active);
        if (active) Refresh(GameState.Instance.Quests);
    }

    public void Refresh(List<QuestData> quests)
    {
        _quests = quests ?? new();
        RebuildList();
    }

    /// <summary>Cập nhật progress của 1 quest đang hiển thị (gọi từ PacketHandlers)</summary>
    public void UpdateProgress(int questId, int progress, int target)
    {
        var quest = _quests.Find(q => q.questId == questId);
        if (quest == null) return;
        quest.progress = progress;
        quest.target   = target;

        // Cập nhật row tương ứng
        RebuildList();

        // Cập nhật detail panel nếu đang mở quest này
        if (_selected?.questId == questId)
            ShowDetail(_selected);
    }

    // ─────────────────────────────────────────
    // List
    // ─────────────────────────────────────────

    private void SwitchTab(bool showDone)
    {
        _showDone = showDone;
        UpdateTabColors();
        RebuildList();
    }

    private void UpdateTabColors()
    {
        var actImg  = tabActive?.GetComponent<Image>();
        var doneImg = tabDone?.GetComponent<Image>();
        if (actImg)  actImg.color  = !_showDone ? tabActiveColor : tabInactiveColor;
        if (doneImg) doneImg.color = _showDone  ? tabActiveColor : tabInactiveColor;
    }

    private void RebuildList()
    {
        foreach (var go in _rows) Destroy(go);
        _rows.Clear();
        if (detailPanel) detailPanel.SetActive(false);

        var filtered = _quests.FindAll(q => _showDone
            ? q.status == QuestStatus.Completed
            : q.status == QuestStatus.Active || q.status == QuestStatus.ReadyToComplete);

        foreach (var quest in filtered)
        {
            var go = Instantiate(questRowPrefab, listParent);
            _rows.Add(go);

            var nameText = go.transform.Find("Name")?.GetComponent<TMP_Text>();
            if (nameText) nameText.text = quest.questName;

            // Progress bar
            var slider = go.transform.Find("Progress")?.GetComponent<Slider>();
            if (slider)
            {
                slider.minValue = 0;
                slider.maxValue = quest.target > 0 ? quest.target : 1;
                slider.value    = quest.progress;
            }

            var progText = go.transform.Find("ProgressText")?.GetComponent<TMP_Text>();
            if (progText) progText.text = $"{quest.progress}/{quest.target}";

            // Ready to complete indicator
            var readyMark = go.transform.Find("ReadyMark");
            if (readyMark) readyMark.gameObject.SetActive(quest.status == QuestStatus.ReadyToComplete);

            var btn = go.GetComponent<Button>();
            if (btn)
            {
                var captured = quest;
                btn.onClick.AddListener(() => ShowDetail(captured));
            }
        }

        // Empty state
        if (filtered.Count == 0)
        {
            var empty = Instantiate(questRowPrefab, listParent);
            _rows.Add(empty);
            var t = empty.transform.Find("Name")?.GetComponent<TMP_Text>();
            if (t) t.text = _showDone ? "Chưa hoàn thành quest nào." : "Không có quest đang làm.";
            var btn = empty.GetComponent<Button>();
            btn?.onClick.RemoveAllListeners();
        }
    }

    // ─────────────────────────────────────────
    // Detail
    // ─────────────────────────────────────────

    private void ShowDetail(QuestData quest)
    {
        _selected = quest;
        if (!detailPanel) return;
        detailPanel.SetActive(true);

        if (txtQuestName) txtQuestName.text = quest.questName;
        if (txtQuestDesc) txtQuestDesc.text = quest.description;
        if (txtObjective) txtObjective.text = quest.objective;

        if (progressBar)
        {
            progressBar.minValue = 0;
            progressBar.maxValue = quest.target > 0 ? quest.target : 1;
            progressBar.value    = quest.progress;
        }
        if (txtProgress) txtProgress.text = $"{quest.progress} / {quest.target}";

        // Reward
        if (txtReward) txtReward.text =
            $"EXP: {quest.rewardExp:N0}   Gold: {quest.rewardGold:N0}" +
            (quest.rewardItemId > 0 ? $"\nItem x{quest.rewardItemQty}" : "");

        bool canComplete = quest.status == QuestStatus.ReadyToComplete;
        bool canAbandon  = quest.status == QuestStatus.Active || canComplete;
        btnComplete?.gameObject.SetActive(canComplete);
        btnAbandon?.gameObject.SetActive(canAbandon && !_showDone);
    }

    // ─────────────────────────────────────────
    // Buttons
    // ─────────────────────────────────────────

    private void OnClickComplete()
    {
        if (_selected == null) return;
        GameClient.Instance.SendQuestComplete(_selected.questId);
        detailPanel?.SetActive(false);
    }

    private void OnClickAbandon()
    {
        if (_selected == null) return;
        GameClient.Instance.SendQuestAbandon(_selected.questId);
        detailPanel?.SetActive(false);
    }
}
