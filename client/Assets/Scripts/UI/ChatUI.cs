// NexusIsekai — ChatUI.cs
// Chat đầy đủ: text, sticker, emoji, toạ độ, item, lì xì, voice
// Channels: map, world, guild, PM, cross-server

using System;
using System.Collections;
using System.Collections.Generic;
using System.IO;
using System.Text;
using UnityEngine;
using UnityEngine.UI;
using UnityEngine.Networking;
using TMPro;
using NexusIsekai.Data;
using NexusIsekai.Network;
using NexusIsekai.Game;

namespace NexusIsekai.UI
{
    public class ChatUI : MonoBehaviour
    {
        public static ChatUI Instance { get; private set; }

        // ─────────────────────────────────────────
        // Inspector refs
        // ─────────────────────────────────────────

        [Header("Main panel")]
        public GameObject   panel;
        public ScrollRect   scrollRect;
        public Transform    msgContainer;
        public TMP_InputField chatInput;
        public Button       btnSend;
        public Button       btnToggleInput; // mobile: open/close keyboard area

        [Header("Channel tabs")]
        public Button btnTabMap;
        public Button btnTabWorld;
        public Button btnTabGuild;
        public Button btnTabCross;
        public Button btnTabPM;

        [Header("Tab highlight images")]
        public Image[] tabHighlights = new Image[5]; // map,world,guild,cross,pm
        public Color   tabActiveColor   = new Color(0.3f, 0.6f, 1f);
        public Color   tabInactiveColor = new Color(0.2f, 0.2f, 0.3f);

        [Header("Message prefabs")]
        public GameObject msgTextPrefab;       // text/emoji
        public GameObject msgStickerPrefab;    // sticker
        public GameObject msgLocationPrefab;   // toạ độ → nút teleport
        public GameObject msgItemPrefab;       // khoe item
        public GameObject msgEnvelopePrefab;   // lì xì → nút giựt
        public GameObject msgVoicePrefab;      // voice → nút play
        public GameObject msgSystemPrefab;     // system notice

        [Header("Extra input buttons")]
        public Button btnSticker;      // mở sticker panel
        public Button btnEmoji;        // mở emoji panel
        public Button btnShareLocation;
        public Button btnShareItem;
        public Button btnRedEnvelope;
        public Button btnVoice;        // hold to record

        [Header("Sticker panel")]
        public GameObject   stickerPanel;
        public Transform    stickerGrid;
        public GameObject   stickerBtnPrefab;  // Image + Button

        [Header("Red envelope dialog")]
        public GameObject   envelopeDialog;
        public TMP_InputField envAmountInput;
        public Slider       envGrabbersSlider;
        public TMP_Text     envGrabbersLabel;
        public Toggle       envGoldToggle;
        public Toggle       envDiamondToggle;
        public TMP_InputField envMessageInput;
        public Button       btnEnvSend;
        public Button       btnEnvCancel;

        [Header("PM")]
        public TMP_InputField pmTargetInput;  // tên người nhận khi channel=PM

        [Header("Voice recording")]
        public GameObject   voiceRecordOverlay;
        public Slider       voiceProgressBar;
        public TMP_Text     voiceTimerText;

        [Header("Notification badge")]
        public GameObject   unreadBadge;
        public TMP_Text     unreadCount;

        // ─────────────────────────────────────────
        // Runtime state
        // ─────────────────────────────────────────

        private string _channel = "world";
        private int    _unreadCount = 0;
        private bool   _panelOpen = true;
        private const  int MAX_MESSAGES = 200;
        private List<GameObject> _msgObjects = new();

        // Voice recording
        private AudioClip  _recordingClip;
        private float      _recordingStart;
        private bool       _isRecording;
        private const int  MAX_RECORD_SECONDS = 60;

        // Sticker data
        private List<StickerData> _stickers = new();

        // Envelope tracking (id → GameObject để update count)
        private Dictionary<long, GameObject> _envelopes = new();

        // ─────────────────────────────────────────
        // Unity lifecycle
        // ─────────────────────────────────────────

        private void Awake()
        {
            if (Instance != null && Instance != this) { Destroy(gameObject); return; }
            Instance = this;
        }

        private void Start()
        {
            // Send button
            btnSend.onClick.AddListener(OnSend);
            chatInput.onSubmit.AddListener(_ => OnSend());

            // Channel tabs
            btnTabMap?.onClick.AddListener(() => SetChannel("map"));
            btnTabWorld?.onClick.AddListener(() => SetChannel("world"));
            btnTabGuild?.onClick.AddListener(() => SetChannel("guild"));
            btnTabCross?.onClick.AddListener(() => SetChannel("cross"));
            btnTabPM?.onClick.AddListener(() => SetChannel("pm"));

            // Extra input
            btnSticker?.onClick.AddListener(ToggleStickerPanel);
            btnEmoji?.onClick.AddListener(ToggleEmojiPicker);
            btnShareLocation?.onClick.AddListener(ShareCurrentLocation);
            btnShareItem?.onClick.AddListener(OpenItemShare);
            btnRedEnvelope?.onClick.AddListener(() => envelopeDialog?.SetActive(true));
            btnToggleInput?.onClick.AddListener(TogglePanel);

            // Voice — hold to record
            if (btnVoice)
            {
                var trigger = btnVoice.gameObject.AddComponent<UnityEngine.EventSystems.EventTrigger>();
                var down = new UnityEngine.EventSystems.EventTrigger.Entry
                    { eventID = UnityEngine.EventSystems.EventTriggerType.PointerDown };
                down.callback.AddListener(_ => StartVoiceRecord());
                var up = new UnityEngine.EventSystems.EventTrigger.Entry
                    { eventID = UnityEngine.EventSystems.EventTriggerType.PointerUp };
                up.callback.AddListener(_ => StopVoiceRecord());
                trigger.triggers.Add(down); trigger.triggers.Add(up);
            }

            // Red envelope dialog
            btnEnvSend?.onClick.AddListener(OnSendEnvelope);
            btnEnvCancel?.onClick.AddListener(() => envelopeDialog?.SetActive(false));
            envGrabbersSlider?.onValueChanged.AddListener(v =>
            {
                if (envGrabbersLabel) envGrabbersLabel.text = $"{(int)v} người";
            });

            // Init state
            stickerPanel?.SetActive(false);
            envelopeDialog?.SetActive(false);
            voiceRecordOverlay?.SetActive(false);
            unreadBadge?.SetActive(false);

            SetChannel("world");

            // Load stickers
            PacketBuilder.SendPing(); // connection check
            // Request sticker list on login (handled in PacketHandlers)
        }

        private void Update()
        {
            // Voice recording timer
            if (_isRecording)
            {
                float elapsed = Time.time - _recordingStart;
                if (voiceProgressBar) voiceProgressBar.value = elapsed / MAX_RECORD_SECONDS;
                if (voiceTimerText)   voiceTimerText.text = $"{elapsed:F0}s / {MAX_RECORD_SECONDS}s";
                if (elapsed >= MAX_RECORD_SECONDS) StopVoiceRecord();
            }
        }

        // ─────────────────────────────────────────
        // Channel management
        // ─────────────────────────────────────────

        private void SetChannel(string ch)
        {
            _channel = ch;
            int tabIdx = ch switch { "map"=>0,"world"=>1,"guild"=>2,"cross"=>3,"pm"=>4, _=>1 };
            for (int i = 0; i < tabHighlights.Length; i++)
                if (tabHighlights[i]) tabHighlights[i].color = i==tabIdx ? tabActiveColor : tabInactiveColor;

            // Show/hide PM target input
            bool isPm = ch == "pm";
            if (pmTargetInput) pmTargetInput.gameObject.SetActive(isPm);
        }

        public void TogglePanel()
        {
            _panelOpen = !_panelOpen;
            // Chỉ ẩn/hiện phần input, vẫn giữ message list
            if (chatInput) chatInput.gameObject.SetActive(_panelOpen);
            if (btnSend)   btnSend.gameObject.SetActive(_panelOpen);
            if (_panelOpen) { _unreadCount = 0; unreadBadge?.SetActive(false); }
        }

        // ─────────────────────────────────────────
        // Sending messages
        // ─────────────────────────────────────────

        private void OnSend()
        {
            string text = chatInput.text.Trim();
            if (string.IsNullOrEmpty(text)) return;

            if (_channel == "pm")
            {
                string target = pmTargetInput?.text.Trim();
                if (string.IsNullOrEmpty(target)) return;
                // PM dùng C2S_CHAT với channel=PM + target name
                GameClient.Instance?.Send(
                    new PacketBuilder(PacketOpcode.C2S_CHAT)
                        .WriteByte(PacketBuilder.ChannelByte("pm"))
                        .WriteString(text)
                        .WriteString(target));
            }
            else
            {
                PacketBuilder.SendChat(_channel, text);
            }

            chatInput.text = "";
            chatInput.ActivateInputField();
        }

        private void ShareCurrentLocation()
        {
            var p = GameState.Instance.MyPlayer;
            if (p == null) return;
            PacketBuilder.SendLocation(_channel, p.MapId, p.X, p.Y);
        }

        private void OpenItemShare()
        {
            InventoryUI.Instance?.OpenForEnhancement(item =>
            {
                // Reuse enhancement mode to select item for showcasing
                PacketBuilder.SendItemShowcase(_channel, item.inventoryId);
            });
        }

        public void ShareItem(long instanceId)
        {
            PacketBuilder.SendItemShowcase(_channel, instanceId);
        }

        // ─────────────────────────────────────────
        // Sticker
        // ─────────────────────────────────────────

        public void PopulateStickers(List<StickerData> stickers)
        {
            _stickers = stickers;
            if (!stickerGrid) return;
            foreach (Transform t in stickerGrid) Destroy(t.gameObject);
            foreach (var s in stickers)
            {
                var go = Instantiate(stickerBtnPrefab, stickerGrid);
                var img = go.GetComponentInChildren<Image>();
                if (img)
                {
                    var sp = Resources.Load<Sprite>(s.AssetKey);
                    if (sp) img.sprite = sp;
                }
                var btn = go.GetComponent<Button>();
                int capturedId = s.Id;
                btn?.onClick.AddListener(() =>
                {
                    PacketBuilder.SendSticker(_channel, capturedId);
                    stickerPanel?.SetActive(false);
                });
            }
        }

        private void ToggleStickerPanel()
        {
            if (!stickerPanel) return;
            stickerPanel.SetActive(!stickerPanel.activeSelf);
        }

        private void ToggleEmojiPicker()
        {
            // Trên PC: dùng bàn phím emoji (Win+. / Cmd+Ctrl+Space)
            // Mobile: hiện picker custom hoặc dùng native
            // Simple: insert common emojis as unicode codepoints
        }

        // ─────────────────────────────────────────
        // Red Envelope
        // ─────────────────────────────────────────

        private void OnSendEnvelope()
        {
            if (!int.TryParse(envAmountInput.text, out int amount) || amount <= 0) return;
            int  grabbers = envGrabbersSlider ? (int)envGrabbersSlider.value : 5;
            byte currency = (envDiamondToggle != null && envDiamondToggle.isOn) ? (byte)1 : (byte)0;
            string msg    = envMessageInput?.text.Trim() ?? "";
            if (amount < grabbers) { UIManager.Instance?.ShowNotification("Số tiền phải >= số người giựt.", UINotificationType.Warning); return; }

            PacketBuilder.SendRedEnvelope(_channel, amount, (byte)grabbers, currency, msg);
            envelopeDialog?.SetActive(false);
        }

        // ─────────────────────────────────────────
        // Voice recording
        // ─────────────────────────────────────────

        private void StartVoiceRecord()
        {
            if (_isRecording) return;
            if (!Microphone.devices.Length.Equals(0) == false) { UIManager.Instance?.ShowNotification("Không có microphone!", UINotificationType.Error); return; }

            _recordingClip = Microphone.Start(null, false, MAX_RECORD_SECONDS, 16000);
            _recordingStart = Time.time;
            _isRecording = true;
            voiceRecordOverlay?.SetActive(true);
            if (voiceProgressBar) voiceProgressBar.value = 0;
        }

        private void StopVoiceRecord()
        {
            if (!_isRecording) return;
            float duration = Time.time - _recordingStart;
            Microphone.End(null);
            _isRecording = false;
            voiceRecordOverlay?.SetActive(false);

            if (duration < 0.5f) return; // quá ngắn

            int durationMs = Mathf.RoundToInt(duration * 1000);
            StartCoroutine(UploadVoice(_recordingClip, durationMs));
        }

        private IEnumerator UploadVoice(AudioClip clip, int durationMs)
        {
            // Convert AudioClip → WAV bytes
            byte[] wavBytes = ConvertToWav(clip);

            // Upload lên server
            var session = GameState.Instance.MyPlayer;
            if (session == null) yield break;

            string url = $"http://{GetServerHost()}/api/voice/upload";
            using var request = new UnityWebRequest(url, "POST");
            request.uploadHandler   = new UploadHandlerRaw(wavBytes);
            request.downloadHandler = new DownloadHandlerBuffer();
            request.SetRequestHeader("Content-Type", "audio/wav");
            request.SetRequestHeader("X-Duration-Ms", durationMs.ToString());
            // Token từ session
            request.SetRequestHeader("X-Account-Id", GameState.Instance.AccountId.ToString());
            request.SetRequestHeader("X-Token",      GameState.Instance.WebToken ?? "");
            request.SetRequestHeader("X-Char-Id",    GameState.Instance.MyPlayer?.CharId.ToString() ?? "0");
            request.SetRequestHeader("X-Char-Name",  GameState.Instance.MyPlayer?.Name ?? "");

            yield return request.SendWebRequest();

            if (request.result == UnityWebRequest.Result.Success)
            {
                string json = request.downloadHandler.text;
                // Parse url from response
                // Simple JSON extract: "url": "/api/voice/..."
                int urlIdx = json.IndexOf("\"url\":");
                if (urlIdx >= 0)
                {
                    int start = json.IndexOf('"', urlIdx + 6) + 1;
                    int end   = json.IndexOf('"', start);
                    string voiceUrl = json.Substring(start, end - start);
                    PacketBuilder.SendVoiceUrl(_channel, durationMs, voiceUrl);
                }
            }
            else
            {
                UIManager.Instance?.ShowNotification("Gửi voice thất bại!", UINotificationType.Error);
            }
        }

        private string GetServerHost()
        {
            // Lấy từ GameClient config
            return GameClient.Instance?.serverHost ?? "localhost:9090";
        }

        // ─────────────────────────────────────────
        // Receive message display
        // ─────────────────────────────────────────

        /// <summary>
        /// Gọi từ PacketHandlers khi nhận S2C_CHAT
        /// </summary>
        public void ReceiveChatMessage(ChatMessage msg)
        {
            AppendMessage(msg);

            // Badge nếu panel đóng
            if (!_panelOpen)
            {
                _unreadCount++;
                unreadBadge?.SetActive(true);
                if (unreadCount) unreadCount.text = _unreadCount > 99 ? "99+" : _unreadCount.ToString();
            }
        }

        public void AppendMessage(ChatMessage msg)
        {
            GameObject prefab = msgTextPrefab;
            if (msg.ContentType == ChatContentType.Sticker)  prefab = msgStickerPrefab;
            if (msg.ContentType == ChatContentType.Location)  prefab = msgLocationPrefab;
            if (msg.ContentType == ChatContentType.Item)      prefab = msgItemPrefab;
            if (msg.ContentType == ChatContentType.Envelope)  prefab = msgEnvelopePrefab;
            if (msg.ContentType == ChatContentType.Voice)     prefab = msgVoicePrefab;
            if (msg.ContentType == ChatContentType.System)    prefab = msgSystemPrefab;
            if (prefab == null)                               prefab = msgTextPrefab;

            var go = Instantiate(prefab, msgContainer);
            _msgObjects.Add(go);

            // Populate theo type
            switch (msg.ContentType)
            {
                case ChatContentType.Text:
                case ChatContentType.Emoji:
                    ConfigureTextMsg(go, msg);
                    break;
                case ChatContentType.Sticker:
                    ConfigureStickerMsg(go, msg);
                    break;
                case ChatContentType.Location:
                    ConfigureLocationMsg(go, msg);
                    break;
                case ChatContentType.Item:
                    ConfigureItemMsg(go, msg);
                    break;
                case ChatContentType.Envelope:
                    ConfigureEnvelopeMsg(go, msg);
                    break;
                case ChatContentType.Voice:
                    ConfigureVoiceMsg(go, msg);
                    break;
                case ChatContentType.System:
                    ConfigureSystemMsg(go, msg);
                    break;
            }

            // Prune old messages
            if (_msgObjects.Count > MAX_MESSAGES)
            {
                Destroy(_msgObjects[0]);
                _msgObjects.RemoveAt(0);
            }

            // Scroll to bottom
            Canvas.ForceUpdateCanvases();
            if (scrollRect) scrollRect.verticalNormalizedPosition = 0f;
        }

        // ─────────────────────────────────────────
        // Message configurators
        // ─────────────────────────────────────────

        private void ConfigureTextMsg(GameObject go, ChatMessage msg)
        {
            var nameTxt = go.transform.Find("SenderName")?.GetComponent<TMP_Text>();
            var bodyTxt = go.transform.Find("MessageText")?.GetComponent<TMP_Text>();
            var timeTxt = go.transform.Find("Timestamp")?.GetComponent<TMP_Text>();

            if (nameTxt)
            {
                nameTxt.text  = msg.Sender;
                nameTxt.color = ChannelColor(msg.Channel);
            }
            if (bodyTxt) bodyTxt.text = msg.Content;
            if (timeTxt) timeTxt.text = DateTime.Now.ToString("HH:mm");

            // Align ngay/phải nếu là tin nhắn của mình
            bool isSelf = msg.Sender == GameState.Instance.MyPlayer?.Name;
            var layout = go.GetComponent<HorizontalLayoutGroup>();
            if (layout) layout.reverseArrangement = isSelf;
        }

        private void ConfigureStickerMsg(GameObject go, ChatMessage msg)
        {
            var nameTxt   = go.transform.Find("SenderName")?.GetComponent<TMP_Text>();
            var stickerImg = go.transform.Find("StickerImage")?.GetComponent<Image>();
            if (nameTxt)   { nameTxt.text = msg.Sender; nameTxt.color = ChannelColor(msg.Channel); }
            if (stickerImg && !string.IsNullOrEmpty(msg.StickerAssetKey))
            {
                var sp = Resources.Load<Sprite>(msg.StickerAssetKey);
                if (sp) stickerImg.sprite = sp;
            }
        }

        private void ConfigureLocationMsg(GameObject go, ChatMessage msg)
        {
            var nameTxt = go.transform.Find("SenderName")?.GetComponent<TMP_Text>();
            var locTxt  = go.transform.Find("LocationText")?.GetComponent<TMP_Text>();
            var mapImg  = go.transform.Find("MapIcon")?.GetComponent<Image>();
            var btnTp   = go.transform.Find("BtnTeleport")?.GetComponent<Button>();

            if (nameTxt) { nameTxt.text = msg.Sender; nameTxt.color = ChannelColor(msg.Channel); }
            if (locTxt)  locTxt.text = $"{msg.MapName} ({msg.LocationX:F0}, {msg.LocationY:F0})";

            // Load map thumbnail nếu có
            if (mapImg)
            {
                var sp = Resources.Load<Sprite>($"MapThumbs/map_{msg.LocationMapId}");
                if (sp) { mapImg.sprite = sp; mapImg.gameObject.SetActive(true); }
            }

            if (btnTp)
            {
                int mid = msg.LocationMapId; float lx = msg.LocationX, ly = msg.LocationY;
                btnTp.onClick.AddListener(() => {
                    // Teleport đến toạ độ (chỉ teleport nếu cùng map)
                    var p = GameState.Instance.MyPlayer;
                    if (p != null && p.MapId == mid)
                        PlayerController.Instance?.ForcePosition(lx, ly);
                    else
                        UIManager.Instance?.ShowNotification($"Di chuyển đến {msg.MapName} trước!", UINotificationType.Info);
                });
            }
        }

        private void ConfigureItemMsg(GameObject go, ChatMessage msg)
        {
            var nameTxt  = go.transform.Find("SenderName")?.GetComponent<TMP_Text>();
            var itemName = go.transform.Find("ItemName")?.GetComponent<TMP_Text>();
            var itemIcon = go.transform.Find("ItemIcon")?.GetComponent<Image>();
            var rarityBg = go.transform.Find("RarityBg")?.GetComponent<Image>();
            var enhLvl   = go.transform.Find("EnhanceLevel")?.GetComponent<TMP_Text>();
            var stats    = go.transform.Find("StatsText")?.GetComponent<TMP_Text>();

            if (nameTxt)  { nameTxt.text = msg.Sender; nameTxt.color = ChannelColor(msg.Channel); }
            if (itemName) itemName.text  = msg.ItemName + (msg.ItemEnhanceLevel > 0 ? $" +{msg.ItemEnhanceLevel}" : "");
            if (enhLvl && msg.ItemEnhanceLevel > 0) enhLvl.text = $"+{msg.ItemEnhanceLevel}";
            if (stats)    stats.text     = msg.ItemAtkBonus > 0 ? $"ATK +{msg.ItemAtkBonus}" : "";

            if (itemIcon)
            {
                var sp = Resources.Load<Sprite>($"Sprites/Items/item_{msg.ItemId}");
                if (sp) itemIcon.sprite = sp;
            }
            if (rarityBg)
            {
                rarityBg.color = msg.ItemRarity switch {
                    1 => Color.green,
                    2 => Color.blue,
                    3 => new Color(0.6f, 0f, 1f),
                    4 => new Color(1f, 0.5f, 0f),
                    _ => Color.white
                };
            }
        }

        private void ConfigureEnvelopeMsg(GameObject go, ChatMessage msg)
        {
            var nameTxt   = go.transform.Find("SenderName")?.GetComponent<TMP_Text>();
            var amtTxt    = go.transform.Find("AmountText")?.GetComponent<TMP_Text>();
            var msgTxt    = go.transform.Find("MessageText")?.GetComponent<TMP_Text>();
            var countTxt  = go.transform.Find("CountText")?.GetComponent<TMP_Text>();
            var btnGrab   = go.transform.Find("BtnGrab")?.GetComponent<Button>();
            var curIcon   = go.transform.Find("CurrencyIcon")?.GetComponent<Image>();

            if (nameTxt) nameTxt.text = msg.Sender;
            if (msgTxt)  msgTxt.text  = msg.EnvelopeMessage;
            if (amtTxt)  amtTxt.text  = $"{msg.EnvelopeAmountPerGrab} {(msg.EnvelopeCurrency==0?"Vàng":"Diamond")}/lượt";
            if (countTxt) countTxt.text = $"Còn {msg.EnvelopeRemaining}/{msg.EnvelopeMaxGrabbers}";

            // Currency icon
            if (curIcon)
            {
                var sp = Resources.Load<Sprite>(msg.EnvelopeCurrency == 0 ? "Sprites/Icons/gold" : "Sprites/Icons/diamond");
                if (sp) curIcon.sprite = sp;
            }

            // Track để update count sau
            long eid = msg.EnvelopeId;
            _envelopes[eid] = go;

            if (btnGrab)
            {
                btnGrab.onClick.AddListener(() => PacketBuilder.SendGrabEnvelope(eid));
            }
        }

        private void ConfigureVoiceMsg(GameObject go, ChatMessage msg)
        {
            var nameTxt  = go.transform.Find("SenderName")?.GetComponent<TMP_Text>();
            var durTxt   = go.transform.Find("DurationText")?.GetComponent<TMP_Text>();
            var waveImg  = go.transform.Find("Waveform")?.GetComponent<Image>();
            var btnPlay  = go.transform.Find("BtnPlay")?.GetComponent<Button>();
            var progressBar = go.transform.Find("PlayProgress")?.GetComponent<Slider>();

            if (nameTxt) { nameTxt.text = msg.Sender; nameTxt.color = ChannelColor(msg.Channel); }
            if (durTxt)  durTxt.text = $"{msg.VoiceDurationMs / 1000f:F1}s";

            if (btnPlay)
            {
                string voiceUrl = msg.VoiceUrl;
                bool isPlaying  = false;
                btnPlay.onClick.AddListener(() => {
                    if (!isPlaying)
                    {
                        isPlaying = true;
                        // Icon play → pause
                        var playIcon = btnPlay.transform.Find("PlayIcon")?.GetComponent<Image>();
                        var sp = Resources.Load<Sprite>("Sprites/Icons/pause");
                        if (playIcon && sp) playIcon.sprite = sp;
                        // Download và play audio
                        StartCoroutine(PlayVoice(voiceUrl, msg.VoiceDurationMs, progressBar, () => {
                            isPlaying = false;
                            var playIcon2 = btnPlay.transform.Find("PlayIcon")?.GetComponent<Image>();
                            var sp2 = Resources.Load<Sprite>("Sprites/Icons/play");
                            if (playIcon2 && sp2) playIcon2.sprite = sp2;
                        }));
                    }
                });
            }
        }

        private void ConfigureSystemMsg(GameObject go, ChatMessage msg)
        {
            var txt = go.GetComponentInChildren<TMP_Text>();
            if (txt)
            {
                txt.text  = msg.Content;
                txt.color = Color.yellow;
            }
        }

        // ─────────────────────────────────────────
        // Red envelope update (sau khi có người giựt)
        // ─────────────────────────────────────────

        public void UpdateEnvelopeCount(long envelopeId, string grabberName, int amount, int remaining)
        {
            if (!_envelopes.TryGetValue(envelopeId, out var go)) return;
            var countTxt = go.transform.Find("CountText")?.GetComponent<TMP_Text>();
            if (countTxt)
            {
                // Cập nhật remaining count
                var maxTxt = go.transform.Find("AmountText")?.GetComponent<TMP_Text>();
                if (countTxt) countTxt.text = remaining > 0 ? $"Còn {remaining} lượt" : "Đã hết!";
            }

            // Disable grab button nếu hết
            if (remaining <= 0)
            {
                var btn = go.transform.Find("BtnGrab")?.GetComponent<Button>();
                if (btn) btn.interactable = false;
                _envelopes.Remove(envelopeId);
            }
        }

        // ─────────────────────────────────────────
        // Voice playback
        // ─────────────────────────────────────────

        private IEnumerator PlayVoice(string url, int durationMs, Slider progress, Action onDone)
        {
            string fullUrl = url.StartsWith("http") ? url : $"http://{GetServerHost()}{url}";
            using var request = UnityWebRequestMultimedia.GetAudioClip(fullUrl, AudioType.UNKNOWN);
            yield return request.SendWebRequest();

            if (request.result != UnityWebRequest.Result.Success) { onDone?.Invoke(); yield break; }

            AudioClip clip = DownloadHandlerAudioClip.GetContent(request);
            if (!clip) { onDone?.Invoke(); yield break; }

            var src = gameObject.GetComponent<AudioSource>() ?? gameObject.AddComponent<AudioSource>();
            src.clip = clip;
            src.Play();

            float dur = clip.length;
            float elapsed = 0;
            while (elapsed < dur)
            {
                elapsed += Time.deltaTime;
                if (progress) progress.value = elapsed / dur;
                yield return null;
            }
            if (progress) progress.value = 0;
            onDone?.Invoke();
        }

        // ─────────────────────────────────────────
        // Helpers
        // ─────────────────────────────────────────

        private Color ChannelColor(string ch) => ch switch {
            "system" => new Color(1f, 0.85f, 0.2f),
            "guild"  => new Color(0.2f, 1f, 0.5f),
            "world"  => Color.white,
            "cross"  => new Color(0.6f, 0.8f, 1f),
            "pm"     => new Color(1f, 0.7f, 1f),
            "map"    => new Color(0.85f, 0.85f, 1f),
            _        => Color.white
        };

        // ─────────────────────────────────────────
        // WAV conversion (AudioClip → byte[])
        // ─────────────────────────────────────────

        private static byte[] ConvertToWav(AudioClip clip)
        {
            float[] samples = new float[clip.samples * clip.channels];
            clip.GetData(samples, 0);

            using var ms  = new MemoryStream();
            using var bw  = new BinaryWriter(ms);

            int sampleRate  = clip.frequency;
            int channels    = clip.channels;
            int bitsPerSample = 16;
            int dataLen     = samples.Length * 2;

            bw.Write(Encoding.ASCII.GetBytes("RIFF"));
            bw.Write(36 + dataLen);
            bw.Write(Encoding.ASCII.GetBytes("WAVE"));
            bw.Write(Encoding.ASCII.GetBytes("fmt "));
            bw.Write(16); bw.Write((short)1); bw.Write((short)channels);
            bw.Write(sampleRate); bw.Write(sampleRate * channels * bitsPerSample / 8);
            bw.Write((short)(channels * bitsPerSample / 8)); bw.Write((short)bitsPerSample);
            bw.Write(Encoding.ASCII.GetBytes("data")); bw.Write(dataLen);

            foreach (float s in samples)
            {
                short v = (short)Mathf.Clamp(s * 32767f, -32768f, 32767f);
                bw.Write(v);
            }
            return ms.ToArray();
        }

        // ─────────────────────────────────────────
        // Data classes
        // ─────────────────────────────────────────

        public class StickerData
        {
            public int    Id;
            public int    PackId;
            public string AssetKey;
        }
    }

    // ─────────────────────────────────────────
    // ChatMessage (extended với tất cả content types)
    // ─────────────────────────────────────────

    public enum ChatContentType : byte
    {
        Text     = 0,
        Sticker  = 1,
        Emoji    = 2,
        Location = 3,
        Item     = 4,
        Envelope = 5,
        Voice    = 6,
        System   = 7
    }

    public class ChatMessage
    {
        public ChatContentType ContentType { get; set; } = ChatContentType.Text;
        public string Channel  { get; set; } = "world";
        public string Sender   { get; set; } = "";
        public string Content  { get; set; } = "";
        public bool   IsSelf   { get; set; }

        // Sticker
        public string StickerAssetKey { get; set; }

        // Location
        public int   LocationMapId { get; set; }
        public float LocationX     { get; set; }
        public float LocationY     { get; set; }
        public string MapName      { get; set; }

        // Item showcase
        public int    ItemId          { get; set; }
        public string ItemName        { get; set; }
        public int    ItemRarity      { get; set; }
        public int    ItemEnhanceLevel{ get; set; }
        public int    ItemAtkBonus    { get; set; }

        // Red envelope
        public long   EnvelopeId        { get; set; }
        public int    EnvelopeAmountPerGrab { get; set; }
        public int    EnvelopeMaxGrabbers   { get; set; }
        public int    EnvelopeRemaining     { get; set; }
        public byte   EnvelopeCurrency      { get; set; }
        public string EnvelopeMessage       { get; set; }

        // Voice
        public string VoiceUrl        { get; set; }
        public int    VoiceDurationMs { get; set; }
    }
}
