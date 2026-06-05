using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;
using FantasyRealm.Network;

namespace FantasyRealm.UI
{
    /// <summary>
    /// Thanh kỹ năng: hiện skill khả dụng, bấm để dùng (nhắm mob đang chọn),
    /// hiển thị cooldown. Nghe S_SKILL_LIST / S_SKILL_RESULT / S_SKILL_COOLDOWN.
    /// </summary>
    public class SkillBarUI : MonoBehaviour
    {
        [Header("UI")]
        public Transform  slotContainer;
        public GameObject skillSlotPrefab;  // Button + Text(tên) + Image(cooldown overlay)

        [System.Serializable]
        public class SkillInfo { public int id; public string name; public int mana, cooldownMs, type; }

        private readonly List<SkillInfo> _skills = new();
        private readonly Dictionary<int, GameObject> _slots = new();
        private readonly Dictionary<int, float> _cooldownEnd = new(); // skillId -> Time.time hết cd

        // mob đang được nhắm (set từ MobView khi click) — 0 nếu skill AoE/self
        public long SelectedTargetId { get; set; }

        void Start() {
            PacketRouter.Instance.Register(PacketType.S_SKILL_LIST,     OnSkillList);
            PacketRouter.Instance.Register(PacketType.S_SKILL_RESULT,   OnSkillResult);
            PacketRouter.Instance.Register(PacketType.S_SKILL_COOLDOWN, OnSkillCooldown);
            RequestSkills();
        }
        void OnDestroy() {
            if (PacketRouter.Instance == null) return;
            PacketRouter.Instance.Unregister(PacketType.S_SKILL_LIST,     OnSkillList);
            PacketRouter.Instance.Unregister(PacketType.S_SKILL_RESULT,   OnSkillResult);
            PacketRouter.Instance.Unregister(PacketType.S_SKILL_COOLDOWN, OnSkillCooldown);
        }

        public void RequestSkills() {
            GameNetworkManager.Instance?.Send(new Packet(PacketType.C_SKILL_LIST_REQ));
        }

        void OnSkillList(Packet p) {
            int count = p.ReadInt();
            _skills.Clear();
            for (int i = 0; i < count; i++) {
                _skills.Add(new SkillInfo {
                    id = p.ReadInt(), name = p.ReadString(),
                    mana = p.ReadInt(), cooldownMs = p.ReadInt(), type = p.ReadByte()
                });
            }
            Rebuild();
        }

        void Rebuild() {
            foreach (var go in _slots.Values) Destroy(go);
            _slots.Clear();
            if (skillSlotPrefab == null || slotContainer == null) return;

            foreach (var sk in _skills) {
                var go = Instantiate(skillSlotPrefab, slotContainer);
                var label = go.GetComponentInChildren<Text>();
                if (label) label.text = sk.name;
                int id = sk.id;
                var btn = go.GetComponent<Button>();
                if (btn) btn.onClick.AddListener(() => UseSkill(id));
                _slots[id] = go;
            }
        }

        void UseSkill(int skillId) {
            // còn cooldown?
            if (_cooldownEnd.TryGetValue(skillId, out float end) && Time.time < end) return;
            GameNetworkManager.Instance?.Send(new Packet(PacketType.C_USE_SKILL)
                .WriteInt(skillId).WriteLong(SelectedTargetId));
        }

        void OnSkillResult(Packet p) {
            int skillId = p.ReadInt(); int mp = p.ReadInt(); int hp = p.ReadInt();
            // bắt đầu cooldown hiển thị
            var sk = _skills.Find(x => x.id == skillId);
            if (sk != null) _cooldownEnd[skillId] = Time.time + sk.cooldownMs / 1000f;
        }

        void OnSkillCooldown(Packet p) {
            int skillId = p.ReadInt(); int remainMs = p.ReadInt();
            _cooldownEnd[skillId] = Time.time + remainMs / 1000f;
        }

        void Update() {
            // cập nhật overlay cooldown
            foreach (var kv in _slots) {
                if (!_cooldownEnd.TryGetValue(kv.Key, out float end)) continue;
                var overlay = kv.Value.transform.Find("Cooldown")?.GetComponent<Image>();
                if (overlay == null) continue;
                var sk = _skills.Find(x => x.id == kv.Key);
                float total = sk != null ? sk.cooldownMs / 1000f : 1f;
                float remain = end - Time.time;
                overlay.fillAmount = remain > 0 ? Mathf.Clamp01(remain / total) : 0;
            }
        }
    }
}
