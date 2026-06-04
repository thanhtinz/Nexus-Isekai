// NexusIsekai Client - PlayerController.cs
// Điều khiển nhân vật player: di chuyển, tấn công, dùng skill

using NexusIsekai.Data;
using NexusIsekai.Network;
using UnityEngine;

namespace NexusIsekai.Game
{
    [RequireComponent(typeof(Rigidbody2D))]
    public class PlayerController : MonoBehaviour
    {
        public static PlayerController Instance { get; private set; }

        [Header("Movement")]
        public float moveSpeed = 5f;

        [Header("Attack")]
        public float attackRange = 1.5f;
        public LayerMask monsterLayer;

        // Sprite theo class
        [Header("Sprites")]
        public Sprite[] classSprites; // index = classId - 1

        private Rigidbody2D _rb;
        private Animator    _anim;
        private SpriteRenderer _sr;

        private Vector2 _moveDir;
        private float   _lastSendTime;
        private const float SEND_INTERVAL = 0.05f; // 20 Hz

        // Target monster đang chọn
        private int _targetInstanceId = -1;

        private void Awake()
        {
            if (Instance != null && Instance != this) { Destroy(gameObject); return; }
            Instance = this;
        }

        private void Start()
        {
            _rb   = GetComponent<Rigidbody2D>();
            _anim = GetComponent<Animator>();
            _sr   = GetComponent<SpriteRenderer>();

            // Set sprite theo class
            var p = GameState.Instance.MyPlayer;
            if (p != null && classSprites != null && p.ClassId - 1 >= 0 && p.ClassId - 1 < classSprites.Length)
                _sr.sprite = classSprites[p.ClassId - 1];

            // Đặt vị trí ban đầu
            if (p != null)
                transform.position = new Vector3(p.X, p.Y, 0);
        }

        private void Update()
        {
            HandleInput();
            HandleAttackInput();
        }

        private void FixedUpdate()
        {
            _rb.linearVelocity = _moveDir * moveSpeed;

            // Gửi vị trí lên server theo interval
            if (Time.time - _lastSendTime >= SEND_INTERVAL && _moveDir != Vector2.zero)
            {
                SendMovePacket();
                _lastSendTime = Time.time;
            }
        }

        // ─────────────────────────────────────────
        // Input
        // ─────────────────────────────────────────

        private void HandleInput()
        {
            float h = Input.GetAxisRaw("Horizontal");
            float v = Input.GetAxisRaw("Vertical");
            _moveDir = new Vector2(h, v).normalized;

            if (_anim != null)
            {
                _anim.SetFloat("SpeedX", h);
                _anim.SetFloat("SpeedY", v);
                _anim.SetBool("IsMoving", _moveDir != Vector2.zero);
            }
        }

        private void HandleAttackInput()
        {
            // Tấn công thường: click chuột trái
            if (Input.GetMouseButtonDown(0))
            {
                // Raycast tìm monster
                Vector2 mouseWorld = Camera.main.ScreenToWorldPoint(Input.mousePosition);
                Collider2D hit = Physics2D.OverlapPoint(mouseWorld, monsterLayer);
                if (hit != null)
                {
                    var mi = hit.GetComponent<MonsterObject>();
                    if (mi != null) AttackMonster(mi.InstanceId);
                }
            }

            // Skill 1-5: phím 1-5
            for (int i = 1; i <= 5; i++)
            {
                if (Input.GetKeyDown(KeyCode.Alpha0 + i) && _targetInstanceId >= 0)
                    UseSkill(i, _targetInstanceId);
            }
        }

        // ─────────────────────────────────────────
        // Actions
        // ─────────────────────────────────────────

        public void AttackMonster(int monsterInstanceId)
        {
            _targetInstanceId = monsterInstanceId;
            PacketBuilder.SendAttack(monsterInstanceId);
        }

        public void UseSkill(int skillId, int monsterInstanceId)
        {
            PacketBuilder.SendUseSkill(skillId, monsterInstanceId);
        }

        public void InteractWithNpc(int npcId)
        {
            NexusIsekai.UI.NpcInteractionUI.Instance?.Open(npcId);
        }

        public void TryEnterPortal(int portalId)
        {
            PacketBuilder.SendMapChange(portalId);
        }

        private void SendMovePacket()
        {
            PacketBuilder.SendMove(transform.position.x, transform.position.y, DirectionByte());
        }

        private byte DirectionByte()
        {
            if (_moveDir.x > 0) return 0;   // Right
            if (_moveDir.x < 0) return 1;   // Left
            if (_moveDir.y > 0) return 2;   // Up
            return 3;                        // Down
        }

        // ─────────────────────────────────────────
        // Từ server
        // ─────────────────────────────────────────

        /// Server correction khi server phát hiện cheat
        public void ForcePosition(float x, float y)
        {
            transform.position = new Vector3(x, y, 0);
            _rb.linearVelocity = Vector2.zero;
        }
    }
}
