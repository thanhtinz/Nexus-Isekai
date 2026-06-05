using System.Collections.Generic;
using UnityEngine;

namespace FantasyRealm.Character
{
    /// <summary>
    /// Animator điều khiển bằng code cho sprite sheet kiểu Mana Seed / Seliel.
    ///
    /// Sheet page-1 (walk/run) là lưới 8x8 ô 64x64 (512x512). Mỗi animation gồm
    /// các CELL INDEX cụ thể (xem "farmer base animation guide.png"), KHÔNG tuần tự.
    /// 4 hướng gốc: down(S), up(N), right(E). Left(W) = lật ngang right.
    /// Hướng chéo: tái dùng hướng gần nhất (top-down Avatar không cần chéo riêng).
    ///
    /// Vì repo không kèm .controller/.anim, animator này tự đổi sprite theo thời gian.
    /// Sprite từ CharPartLoader.LoadFrame(slot, code, cellIndex) — cần slice sheet
    /// thành sprite con đặt tên {code}_{cellIndex} trong Unity (xem BUILD-UNITY.md).
    /// </summary>
    public class SpriteSheetAnimator : MonoBehaviour
    {
        public enum Dir { Down, Up, Left, Right }
        public enum Anim { Idle, Walk, Run }

        [System.Serializable]
        public class Clip {
            public Anim anim;
            public Dir dir;
            public int[] cells;        // cell index trên sheet
            public float frameMs = 135;
            public bool flipX;         // lật ngang (cho hướng Left)
        }

        // Cell map theo Mana Seed guide (page 1). Down/Up/Right; Left = flip Right.
        // Idle = frame đầu của Walk mỗi hướng.
        private static readonly Dictionary<(Anim,Dir), (int[] cells, bool flip)> MAP = new() {
            // WALK — mỗi hướng 8 frame
            {(Anim.Walk, Dir.Down),  (new[]{32,33,34,35,36,37,38,39}, false)},
            {(Anim.Walk, Dir.Up),    (new[]{40,41,42,43,44,45,46,47}, false)},
            {(Anim.Walk, Dir.Right), (new[]{48,49,50,51,52,53,54,55}, false)},
            {(Anim.Walk, Dir.Left),  (new[]{48,49,50,51,52,53,54,55}, true)},
            // RUN — frame khác (theo guide, dùng tạm cùng vùng + 8)
            {(Anim.Run, Dir.Down),  (new[]{56,57,58,59,60,61,62,63}, false)},
            {(Anim.Run, Dir.Up),    (new[]{64,65,66,67,68,69,70,71}, false)},
            {(Anim.Run, Dir.Right), (new[]{72,73,74,75,76,77,78,79}, false)},
            {(Anim.Run, Dir.Left),  (new[]{72,73,74,75,76,77,78,79}, true)},
            // IDLE — frame 0 mỗi hướng
            {(Anim.Idle, Dir.Down),  (new[]{0}, false)},
            {(Anim.Idle, Dir.Up),    (new[]{8}, false)},
            {(Anim.Idle, Dir.Right), (new[]{16}, false)},
            {(Anim.Idle, Dir.Left),  (new[]{16}, true)},
        };

        [Header("Layer renderers (paperdoll)")]
        public SpriteRenderer[] layers;   // skin, outfit, eyes, hair (cùng frame)
        public string[] layerSlots = { "skin", "outfit", "eyes", "hair" };
        public string[] layerCodes = new string[4]; // code mỗi layer, set khi apply outfit

        [Header("State")]
        public Anim currentAnim = Anim.Idle;
        public Dir  currentDir  = Dir.Down;

        private int _frame;
        private float _timer;
        private int[] _cells;
        private bool _flip;
        private float _frameMs = 135;

        void Start() => SetState(Anim.Idle, Dir.Down);

        public void SetCodes(string skin, string outfit, string eyes, string hair) {
            layerCodes = new[] { skin, outfit, eyes, hair };
            RedrawFrame();
        }

        public void SetState(Anim a, Dir d) {
            if (a == currentAnim && d == currentDir) return;
            currentAnim = a; currentDir = d;
            if (MAP.TryGetValue((a, d), out var m)) { _cells = m.cells; _flip = m.flip; }
            else { _cells = new[] { 0 }; _flip = false; }
            _frame = 0; _timer = 0;
            RedrawFrame();
        }

        /// <summary>Đặt hướng+anim từ vector di chuyển.</summary>
        public void SetMovement(Vector2 velocity, bool running) {
            if (velocity.sqrMagnitude < 0.01f) { SetState(Anim.Idle, currentDir); return; }
            Dir d = Mathf.Abs(velocity.x) > Mathf.Abs(velocity.y)
                ? (velocity.x < 0 ? Dir.Left : Dir.Right)
                : (velocity.y > 0 ? Dir.Up : Dir.Down);
            SetState(running ? Anim.Run : Anim.Walk, d);
        }

        void Update() {
            if (_cells == null || _cells.Length <= 1) return;
            _timer += Time.deltaTime * 1000f;
            if (_timer >= _frameMs) {
                _timer -= _frameMs;
                _frame = (_frame + 1) % _cells.Length;
                RedrawFrame();
            }
        }

        void RedrawFrame() {
            if (_cells == null || layers == null) return;
            int cell = _cells[Mathf.Clamp(_frame, 0, _cells.Length - 1)];
            for (int i = 0; i < layers.Length; i++) {
                if (layers[i] == null) continue;
                string slot = i < layerSlots.Length ? layerSlots[i] : null;
                string code = i < layerCodes.Length ? layerCodes[i] : null;
                if (string.IsNullOrEmpty(code) || slot == null) { layers[i].enabled = false; continue; }
                var sp = UI.CharPartLoader.LoadFrame(slot, code, cell);
                if (sp != null) { layers[i].sprite = sp; layers[i].enabled = true; layers[i].flipX = _flip; }
                else layers[i].enabled = false;
            }
        }
    }
}
