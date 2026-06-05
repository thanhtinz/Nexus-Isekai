using UnityEngine;

namespace FantasyRealm.Systems
{
    /// <summary>
    /// Camera 2D top-down bám theo nhân vật, mượt, có giới hạn biên bản đồ (tùy chọn).
    /// Gắn vào Main Camera, kéo target = transform nhân vật người chơi.
    /// </summary>
    public class CameraFollow : MonoBehaviour
    {
        public Transform target;
        public float smoothTime = 0.12f;
        public Vector2 offset = Vector2.zero;

        [Header("Giới hạn biên (0 = không giới hạn)")]
        public Vector2 min = Vector2.zero;
        public Vector2 max = Vector2.zero;

        private Vector3 _vel;

        void LateUpdate() {
            if (target == null) return;
            Vector3 goal = new Vector3(target.position.x + offset.x,
                                       target.position.y + offset.y,
                                       transform.position.z);
            if (max != Vector2.zero) {
                goal.x = Mathf.Clamp(goal.x, min.x, max.x);
                goal.y = Mathf.Clamp(goal.y, min.y, max.y);
            }
            transform.position = Vector3.SmoothDamp(transform.position, goal, ref _vel, smoothTime);
        }

        public void SetTarget(Transform t) => target = t;
    }
}
