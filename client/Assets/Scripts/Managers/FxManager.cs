using System.Collections.Generic;
using UnityEngine;

/// <summary>
/// FxManager — tra spine/vfx/sfx theo id (server gửi qua S2C_FX_CONFIG, admin cấu hình).
/// - Skill: vfx lúc tung + vfx lúc trúng + sfx.
/// - Monster: spine skeleton + sfx đánh/bị đánh/chết.
/// - NPC: spine + sfx tương tác.
/// VFX prefab nạp từ Resources/Effects/&lt;key&gt;; sfx phát qua AudioManager theo audio_key.
/// </summary>
public class FxManager : MonoBehaviour {
    public static FxManager Instance;
    void Awake() { if (!Instance) { Instance = this; DontDestroyOnLoad(gameObject); } else Destroy(gameObject); }

    public struct SkillFx { public string vfx, vfxHit, sfx; }
    public struct MonsterFx { public string spine, sfxAttack, sfxHurt, sfxDeath; }
    public struct NpcFx { public string spine, sfx; }

    private readonly Dictionary<int, SkillFx> skills = new();
    private readonly Dictionary<int, MonsterFx> monsters = new();
    private readonly Dictionary<int, NpcFx> npcs = new();

    public void RegisterSkill(int id, string vfx, string vfxHit, string sfx) => skills[id] = new SkillFx { vfx = vfx, vfxHit = vfxHit, sfx = sfx };
    public void RegisterMonster(int id, string spine, string a, string h, string d) => monsters[id] = new MonsterFx { spine = spine, sfxAttack = a, sfxHurt = h, sfxDeath = d };
    public void RegisterNpc(int id, string spine, string sfx) => npcs[id] = new NpcFx { spine = spine, sfx = sfx };

    // ───── Skill ─────
    public void PlaySkillCast(int skillId, Vector3 pos) {
        if (!skills.TryGetValue(skillId, out var f)) return;
        if (!string.IsNullOrEmpty(f.vfx)) SpawnEffect(f.vfx, pos);
        if (!string.IsNullOrEmpty(f.sfx)) AudioManager.Instance?.PlaySFX(f.sfx);
    }
    public void PlaySkillHit(int skillId, Vector3 pos) {
        if (skills.TryGetValue(skillId, out var f) && !string.IsNullOrEmpty(f.vfxHit)) SpawnEffect(f.vfxHit, pos);
    }

    // ───── Monster ─────
    public string GetMonsterSpine(int monsterId) => monsters.TryGetValue(monsterId, out var f) ? f.spine : null;
    public void PlayMonsterSfx(int monsterId, string evt, Vector3 pos) {
        if (!monsters.TryGetValue(monsterId, out var f)) return;
        string key = evt == "attack" ? f.sfxAttack : evt == "death" ? f.sfxDeath : f.sfxHurt;
        if (!string.IsNullOrEmpty(key)) AudioManager.Instance?.PlaySFX(key);
    }

    // ───── NPC ─────
    public string GetNpcSpine(int npcId) => npcs.TryGetValue(npcId, out var f) ? f.spine : null;
    public void PlayNpcSfx(int npcId) { if (npcs.TryGetValue(npcId, out var f) && !string.IsNullOrEmpty(f.sfx)) AudioManager.Instance?.PlaySFX(f.sfx); }

    /// <summary>Nạp prefab hiệu ứng từ Resources/Effects/&lt;key&gt; rồi instantiate, tự huỷ sau 2s.</summary>
    public void SpawnEffect(string vfxKey, Vector3 pos) {
        var prefab = Resources.Load<GameObject>($"Effects/{vfxKey}");
        if (prefab == null) return;
        var go = Instantiate(prefab, pos, Quaternion.identity);
        Destroy(go, 2f);
    }

    /// <summary>Áp skeleton Spine cho 1 GameObject nếu có (team gắn SkeletonAnimation theo key).</summary>
    public void ApplySpine(GameObject go, string spineKey) {
        if (go == null || string.IsNullOrEmpty(spineKey)) return;
        // Nạp skeleton data từ Resources/Spine/<key> nếu dự án dùng Spine runtime.
        // Để tương thích, chỉ gắn key vào component đánh dấu để team xử lý render.
        var marker = go.GetComponent<SpineMarker>() ?? go.AddComponent<SpineMarker>();
        marker.spineKey = spineKey;
    }
}

/// <summary>Đánh dấu spine_key trên entity để lớp render Spine của team đọc.</summary>
public class SpineMarker : MonoBehaviour { public string spineKey; }
