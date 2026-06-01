// NexusIsekai Client - MapRenderer.cs & MonsterManager.cs & PlayerManager.cs

using System.Collections.Generic;
using NexusIsekai.Data;
using NexusIsekai.Network;
using UnityEngine;

namespace NexusIsekai.Game
{
    // ════════════════════════════════════════════════════════
    // MapRenderer.cs
    // Render background map, tải sprite từ Resources/<mapBackground>
    // Assets map/hud/icon lấy từ NRO gốc, đặt vào Resources/Maps/
    // ════════════════════════════════════════════════════════

    public class MapRenderer : MonoBehaviour
    {
        public static MapRenderer Instance { get; private set; }

        [Header("Map Background")]
        public SpriteRenderer backgroundRenderer;
        public Camera         gameCamera;

        [Header("Portals")]
        public GameObject portalPrefab;
        private readonly List<GameObject> _portals = new();

        private void Awake()
        {
            if (Instance != null && Instance != this) { Destroy(gameObject); return; }
            Instance = this;
        }

        /// <summary>
        /// Load map sau khi nhận S2C_MAP_DATA
        /// Background sprite phải có tên trùng với MapData.Background
        /// đặt trong Resources/Maps/
        /// </summary>
        public void LoadMap(MapData map, float spawnX, float spawnY)
        {
            // Tải sprite background
            var sprite = Resources.Load<Sprite>($"Maps/{map.Background}");
            if (sprite != null)
                backgroundRenderer.sprite = sprite;
            else
                Debug.LogWarning($"[MapRenderer] Không tìm thấy Maps/{map.Background}");

            // Scale background theo kích thước map
            float ppu = sprite != null ? sprite.pixelsPerUnit : 100f;
            backgroundRenderer.transform.localScale = Vector3.one;

            // Set camera bounds
            gameCamera.transform.position = new Vector3(spawnX, spawnY, -10f);

            // Clear portals cũ
            foreach (var p in _portals) Destroy(p);
            _portals.Clear();

            // Đặt player vào spawn point
            if (PlayerController.Instance != null)
                PlayerController.Instance.transform.position = new Vector3(spawnX, spawnY, 0);

            Debug.Log($"[MapRenderer] Loaded map: {map.Name} ({map.Width}x{map.Height})");
        }

        public void SpawnPortal(int portalId, float x, float y)
        {
            if (portalPrefab == null) return;
            var go = Instantiate(portalPrefab, new Vector3(x, y, 0), Quaternion.identity);
            var portal = go.GetComponent<PortalObject>();
            if (portal != null) portal.PortalId = portalId;
            _portals.Add(go);
        }
    }

    // ════════════════════════════════════════════════════════
    // MonsterManager.cs
    // Quản lý các MonsterObject trong scene
    // ════════════════════════════════════════════════════════

    public class MonsterManager : MonoBehaviour
    {
        public static MonsterManager Instance { get; private set; }

        [Header("Prefabs")]
        public GameObject normalMonsterPrefab;
        public GameObject bossMonsterPrefab;

        private readonly Dictionary<int, MonsterObject> _monsters = new();

        private void Awake()
        {
            if (Instance != null && Instance != this) { Destroy(gameObject); return; }
            Instance = this;
        }

        public void SpawnMonster(MonsterData data)
        {
            if (_monsters.ContainsKey(data.InstanceId)) return;

            var prefab = data.IsBoss ? bossMonsterPrefab : normalMonsterPrefab;
            if (prefab == null) return;

            var go  = Instantiate(prefab, new Vector3(data.X, data.Y, 0), Quaternion.identity);
            var obj = go.GetComponent<MonsterObject>();
            if (obj != null)
            {
                obj.InstanceId = data.InstanceId;
                obj.SetData(data);
                FxManager.Instance?.ApplySpine(go, FxManager.Instance.GetMonsterSpine(data.MonsterId));
            }
            _monsters[data.InstanceId] = obj;
        }

        public void KillMonster(int instanceId)
        {
            if (!_monsters.TryGetValue(instanceId, out var obj)) return;
            FxManager.Instance?.PlayMonsterSfx(obj.MonsterId, "death", obj.transform.position);
            obj.PlayDeathAnim();
            _monsters.Remove(instanceId);
            Destroy(obj.gameObject, 1.5f);
        }

        public Vector3 GetPos(int instanceId) => _monsters.TryGetValue(instanceId, out var o) && o != null ? o.transform.position : Vector3.zero;

        public void UpdateHp(int instanceId, int hp)
        {
            if (_monsters.TryGetValue(instanceId, out var obj))
                obj.UpdateHp(hp);
        }

        public void MoveMonster(int instanceId, float x, float y)
        {
            if (_monsters.TryGetValue(instanceId, out var obj))
                obj.MoveTo(x, y);
        }
    }

    // ════════════════════════════════════════════════════════
    // PlayerManager.cs
    // Quản lý RemotePlayer objects (player khác trong zone)
    // ════════════════════════════════════════════════════════

    public class PlayerManager : MonoBehaviour
    {
        public static PlayerManager Instance { get; private set; }

        [Header("Prefabs")]
        public GameObject remotePlayerPrefab;

        private readonly Dictionary<int, GameObject> _remotePlayers = new();

        private void Awake()
        {
            if (Instance != null && Instance != this) { Destroy(gameObject); return; }
            Instance = this;
        }

        public void SpawnRemotePlayer(RemotePlayer rp)
        {
            if (_remotePlayers.ContainsKey(rp.CharId)) return;
            if (remotePlayerPrefab == null) return;

            var go = Instantiate(remotePlayerPrefab, new Vector3(rp.X, rp.Y, 0), Quaternion.identity);

            // Set tên
            var nameTag = go.GetComponentInChildren<TMPro.TMP_Text>();
            if (nameTag != null) nameTag.text = $"{rp.Name} Lv{rp.Level}";

            _remotePlayers[rp.CharId] = go;
        }

        public void RemoveRemotePlayer(int charId)
        {
            if (!_remotePlayers.TryGetValue(charId, out var go)) return;
            Destroy(go);
            _remotePlayers.Remove(charId);
        }

        public void MoveRemotePlayer(int charId, float x, float y, byte dir)
        {
            if (!_remotePlayers.TryGetValue(charId, out var go)) return;
            // Lerp smooth movement
            go.transform.position = Vector3.Lerp(
                go.transform.position,
                new Vector3(x, y, 0),
                Time.deltaTime * 15f);
        }

        public void ShowDeathEffect(int charId)
        {
            if (!_remotePlayers.TryGetValue(charId, out var go)) return;
            var anim = go.GetComponent<Animator>();
            anim?.SetTrigger("Die");
            // Fade out qua coroutine trên MonoBehaviour
            StartCoroutine(FadeOutAndDestroy(go, 1.5f));
            _remotePlayers.Remove(charId);
        }

        private System.Collections.IEnumerator FadeOutAndDestroy(GameObject go, float delay)
        {
            yield return new UnityEngine.WaitForSeconds(delay);
            // Fade ra
            var renderers = go.GetComponentsInChildren<SpriteRenderer>();
            float t = 0;
            while (t < 0.5f)
            {
                t += Time.deltaTime;
                float alpha = Mathf.Lerp(1f, 0f, t / 0.5f);
                foreach (var sr in renderers) { var c = sr.color; c.a = alpha; sr.color = c; }
                yield return null;
            }
            Destroy(go);
        }

    // ════════════════════════════════════════════════════════
    // MonsterObject.cs — component trên từng monster GameObject
    // ════════════════════════════════════════════════════════

    public class MonsterObject : MonoBehaviour
    {
        public int InstanceId { get; set; }
        public int MonsterId  { get; set; }

        [Header("UI")]
        public Slider    hpBar;
        public TMP.TMP_Text nameText;

        private Animator _anim;
        private int      _hp, _maxHp;

        private void Awake() => _anim = GetComponent<Animator>();

        public void SetData(MonsterData data)
        {
            MonsterId = data.MonsterId;
            _hp    = data.Hp;
            _maxHp = data.MaxHp;
            if (nameText != null) nameText.text = data.IsBoss ? $"[BOSS] {data.Name}" : data.Name;
            UpdateHpBar();
        }

        public void UpdateHp(int hp)
        {
            _hp = hp;
            UpdateHpBar();
        }

        public void PlayDeathAnim()
        {
            _anim?.SetTrigger("Die");
        }

        public void MoveTo(float x, float y)
        {
            transform.position = Vector3.Lerp(
                transform.position,
                new Vector3(x, y, 0),
                Time.deltaTime * 10f);
        }

        private void UpdateHpBar()
        {
            if (hpBar != null && _maxHp > 0)
                hpBar.value = (float)_hp / _maxHp;
        }

        /// Khi player click vào monster
        private void OnMouseDown()
        {
            PlayerController.Instance?.AttackMonster(InstanceId);
        }
    }

    // ════════════════════════════════════════════════════════
    // PortalObject.cs — trigger vào portal để chuyển map
    // ════════════════════════════════════════════════════════

    public class PortalObject : MonoBehaviour
    {
        public int PortalId { get; set; }

        private void OnTriggerEnter2D(Collider2D other)
        {
            if (other.CompareTag("Player"))
                PlayerController.Instance?.TryEnterPortal(PortalId);
        }
    }

    // ════════════════════════════════════════════════════════
    // Dữ liệu các hệ thống mới (sync với server DTOs)
    // ════════════════════════════════════════════════════════

    public class TitleInfo
    {
        public int    TitleId  { get; set; }
        public string Name     { get; set; } = "";
        public string ColorHex { get; set; } = "FFFFFF";
        public bool   Equipped { get; set; }
    }

    public class PetInfo
    {
        public long   PetId      { get; set; }
        public int    TemplateId { get; set; }
        public string Name       { get; set; } = "";
        public int    Rarity     { get; set; }
        public int    Level      { get; set; }
        public int    Hunger     { get; set; }
        public int    Loyalty    { get; set; }
        public int    IconId     { get; set; }
        public bool   IsActive   { get; set; }
    }

    public class MountInfo
    {
        public long   MountId    { get; set; }
        public int    TemplateId { get; set; }
        public string Name       { get; set; } = "";
        public int    Rarity     { get; set; }
        public int    Level      { get; set; }
        public float  SpeedBonus { get; set; }
        public int    IconId     { get; set; }
        public bool   IsActive   { get; set; }
    }

    public class ChildInfo
    {
        public long   ChildId    { get; set; }
        public string Name       { get; set; } = "";
        public int    Gender     { get; set; }
        public int    Age        { get; set; }
        public int    Level      { get; set; }
        public int    Hp         { get; set; }
        public int    MaxHp      { get; set; }
        public int    Atk        { get; set; }
        public int    Def        { get; set; }
        public int    SkinId     { get; set; }
        public bool   IsActive   { get; set; }
        public int    Happiness  { get; set; }
    }

    public class MentorRelation
    {
        public long   RelId       { get; set; }
        public long   MentorId   { get; set; }
        public long   StudentId  { get; set; }
        public bool   IsNpcMentor{ get; set; }
        public string Status     { get; set; } = "active";
    }

    // Chat message (sync với UIScripts ChatUI)
    public class ChatMessage
    {
        public string Channel { get; set; } = "map";
        public string Sender  { get; set; } = "";
        public string Content { get; set; } = "";
    }

}
