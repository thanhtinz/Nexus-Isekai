using UnityEngine;

namespace FantasyRealm.Character
{
    /// <summary>
    /// Nội suy mượt vị trí người chơi khác giữa các gói S_PLAYER_MOVE (20Hz),
    /// đồng thời chạy animation đi bộ theo hướng. Gắn vào player prefab.
    /// </summary>
    public class RemotePlayerMover : MonoBehaviour
    {
        public SpriteSheetAnimator animator;
        public float lerpSpeed = 12f;
        public float idleAfter = 0.25f; // giây không nhận update → về idle

        private Vector3 _target;
        private float   _lastMoveTime;
        private bool    _moving;

        void Awake() { _target = transform.position; }

        public void MoveTo(Vector3 pos, int facing) {
            _target = pos;
            _lastMoveTime = Time.time;
            _moving = true;
            if (animator != null) {
                var d = facing switch {
                    1 => SpriteSheetAnimator.Dir.Up,
                    2 => SpriteSheetAnimator.Dir.Left,
                    3 => SpriteSheetAnimator.Dir.Right,
                    _ => SpriteSheetAnimator.Dir.Down
                };
                animator.SetState(SpriteSheetAnimator.Anim.Walk, d);
            }
        }

        void Update() {
            transform.position = Vector3.Lerp(transform.position, _target, Time.deltaTime * lerpSpeed);
            if (_moving && Time.time - _lastMoveTime > idleAfter) {
                _moving = false;
                if (animator != null) animator.SetState(SpriteSheetAnimator.Anim.Idle, animator.currentDir);
            }
        }
    }
}
