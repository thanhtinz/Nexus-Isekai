using System;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;
using FantasyRealm.Network;

namespace FantasyRealm.UI
{
    /// <summary>
    /// Màn tạo nhân vật đầu game.
    /// Luồng:
    ///   1. Gửi C_CHAR_CREATE_OPTIONS → nhận S_CHAR_CREATE_OPTIONS (JSON races/skin/eyes/hair/outfit)
    ///   2. Dựng UI chọn giới tính/chủng tộc + da + mắt + tóc + áo quần
    ///   3. Ghép sprite paperdoll preview real-time (layer: skin→outfit→eyes→hair)
    ///   4. Gửi C_CHAR_CREATE → nhận S_CHAR_CREATE_OK / S_CHAR_CREATE_FAIL
    ///
    /// Sprite layer load từ Resources hoặc AssetBundle theo code (vd "humn_v03").
    /// Đặt asset trong Resources/CharParts/{slot}/{code}.png để Resources.Load tìm thấy.
    /// </summary>
    public class CharacterCreationUI : MonoBehaviour
    {
        [Header("Network")]
        public GameNetworkManager net;

        [Header("UI - Input")]
        public InputField nameInput;
        public Transform raceContainer;    // chứa các nút chọn race/gender
        public Transform skinContainer;
        public Transform eyesContainer;
        public Transform hairContainer;
        public Transform outfitContainer;
        public Button optionButtonPrefab;  // prefab 1 nút lựa chọn
        public Button createButton;
        public Text errorText;

        [Header("UI - Preview (paperdoll layers, dưới→trên)")]
        public Image previewSkin;
        public Image previewOutfit;
        public Image previewEyes;
        public Image previewHair;

        [Header("Sprite loading")]
        public string resourcesPath = "CharParts"; // Resources/CharParts/{slot}/{code}

        // Dữ liệu options nhận từ server
        private CharOptions _opts;
        // Lựa chọn hiện tại
        private string _race, _skin, _eyes, _hair, _outfit;

        [Serializable] public class Opt {
            public string code, name_vn, race, gender, race_filter, gender_filter, hex_preview;
            public int color_index = -1;
            public bool is_default;
        }
        [Serializable] public class CharOptions {
            public List<Opt> races = new();
            public List<Opt> skin = new();
            public List<Opt> eyes = new();
            public List<Opt> hair = new();
            public List<Opt> outfit = new();
        }

        void OnEnable() {
            PacketRouter.Instance.Register(PacketType.S_CHAR_CREATE_OPTIONS, OnOptions);
            PacketRouter.Instance.Register(PacketType.S_CHAR_CREATE_OK, OnCreateOk);
            PacketRouter.Instance.Register(PacketType.S_CHAR_CREATE_FAIL, OnCreateFail);
            RequestOptions();
        }
        void OnDisable() {
            PacketRouter.Instance.Unregister(PacketType.S_CHAR_CREATE_OPTIONS, OnOptions);
            PacketRouter.Instance.Unregister(PacketType.S_CHAR_CREATE_OK, OnCreateOk);
            PacketRouter.Instance.Unregister(PacketType.S_CHAR_CREATE_FAIL, OnCreateFail);
        }

        void Start() {
            if (createButton) createButton.onClick.AddListener(SubmitCreate);
        }

        // ── 1. Xin options ─────────────────────────────────────────
        public void RequestOptions() {
            net.Send(new Packet(PacketType.C_CHAR_CREATE_OPTIONS));
        }

        void OnOptions(Packet p) {
            string json = p.ReadString();
            _opts = JsonUtility.FromJson<CharOptions>(WrapArrays(json));
            if (_opts == null) { ShowError("Lỗi đọc cấu hình"); return; }
            BuildButtons();
            // Chọn mặc định
            _race   = First(_opts.races)?.code;
            _skin   = Default(_opts.skin)   ?? First(_opts.skin)?.code;
            _eyes   = Default(_opts.eyes)   ?? First(_opts.eyes)?.code;
            _hair   = Default(_opts.hair)   ?? First(_opts.hair)?.code;
            _outfit = Default(_opts.outfit) ?? First(_opts.outfit)?.code;
            UpdatePreview();
        }

        // JsonUtility không parse được {"races":[...]} ở top-level array? Nó parse được object.
        // Hàm này giữ nguyên (server đã trả object), chỉ phòng trường hợp cần bọc.
        string WrapArrays(string json) => json;

        Opt First(List<Opt> l) => (l != null && l.Count > 0) ? l[0] : null;
        string Default(List<Opt> l) { if (l == null) return null; foreach (var o in l) if (o.is_default) return o.code; return null; }

        // ── 2. Dựng nút lựa chọn ───────────────────────────────────
        void BuildButtons() {
            BuildSlot(raceContainer,   _opts.races,  c => { _race = c; FilterByRace(); UpdatePreview(); });
            BuildSlot(skinContainer,   _opts.skin,   c => { _skin = c; UpdatePreview(); });
            BuildSlot(eyesContainer,   _opts.eyes,   c => { _eyes = c; UpdatePreview(); });
            BuildSlot(hairContainer,   _opts.hair,   c => { _hair = c; UpdatePreview(); });
            BuildSlot(outfitContainer, _opts.outfit, c => { _outfit = c; UpdatePreview(); });
        }

        void BuildSlot(Transform container, List<Opt> opts, Action<string> onPick) {
            if (container == null || optionButtonPrefab == null) return;
            foreach (Transform t in container) Destroy(t.gameObject);
            foreach (var o in opts) {
                var btn = Instantiate(optionButtonPrefab, container);
                var label = btn.GetComponentInChildren<Text>();
                if (label) label.text = string.IsNullOrEmpty(o.name_vn) ? o.code : o.name_vn;
                // chấm màu cho mắt
                if (!string.IsNullOrEmpty(o.hex_preview)) {
                    var img = btn.GetComponent<Image>();
                    if (img && ColorUtility.TryParseHtmlString(o.hex_preview, out var col)) img.color = col;
                }
                string code = o.code;
                btn.onClick.AddListener(() => onPick(code));
            }
        }

        // Lọc skin theo race đang chọn (vd demn chỉ thấy da quỷ)
        void FilterByRace() {
            // đơn giản: rebuild skin theo race_filter
            // (giữ nhẹ — trong thực tế có thể ẩn/hiện thay vì rebuild)
        }

        // ── 3. Ghép preview paperdoll ──────────────────────────────
        void UpdatePreview() {
            SetLayer(previewSkin,   "skin",   _skin);
            SetLayer(previewOutfit, "outfit", _outfit);
            SetLayer(previewEyes,   "eyes",   _eyes);
            SetLayer(previewHair,   "hair",   _hair);
        }

        void SetLayer(Image img, string slot, string code) {
            if (img == null) return;
            if (string.IsNullOrEmpty(code)) { img.enabled = false; return; }
            // Load sprite: Resources/CharParts/{slot}/{code}
            var sp = Resources.Load<Sprite>($"{resourcesPath}/{slot}/{code}");
            if (sp != null) { img.sprite = sp; img.enabled = true; }
            else { img.enabled = false; Debug.LogWarning($"[CharCreate] Thiếu sprite {slot}/{code}"); }
        }

        // ── 4. Gửi tạo nhân vật ────────────────────────────────────
        public void SubmitCreate() {
            string name = nameInput ? nameInput.text.Trim() : "";
            if (name.Length < 2 || name.Length > 16) { ShowError("Tên phải 2-16 ký tự"); return; }
            if (string.IsNullOrEmpty(_race)) { ShowError("Chọn chủng tộc"); return; }

            net.Send(new Packet(PacketType.C_CHAR_CREATE)
                .WriteString(name)
                .WriteString(_race)
                .WriteString(_skin ?? "")
                .WriteString(_eyes ?? "")
                .WriteString(_hair ?? "")
                .WriteString(_outfit ?? ""));
            if (createButton) createButton.interactable = false;
        }

        void OnCreateOk(Packet p) {
            long id = p.ReadLong();
            string name = p.ReadString();
            int faction = p.ReadInt();
            string outfitJson = p.ReadString();
            Debug.Log($"[CharCreate] Tạo thành công: {name} (id {id}, faction {faction})");
            // TODO: chuyển sang scene game / load nhân vật
            gameObject.SetActive(false);
        }

        void OnCreateFail(Packet p) {
            ShowError(p.ReadString());
            if (createButton) createButton.interactable = true;
        }

        void ShowError(string msg) {
            if (errorText) errorText.text = msg;
            Debug.LogWarning("[CharCreate] " + msg);
        }
    }
}
