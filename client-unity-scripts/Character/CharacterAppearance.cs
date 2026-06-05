using UnityEngine;
using FantasyRealm.UI;

namespace FantasyRealm.Character
{
    /// <summary>
    /// Áp ngoại hình paperdoll lên một player GameObject từ outfit JSON.
    /// Gắn component này vào player prefab. Bên trong có 4 SpriteRenderer con
    /// xếp theo sorting order: skin(0) → outfit(1) → eyes(2) → hair(3).
    ///
    /// outfit JSON từ server: {"race":"humn_male","skin":"humn_v03","eyes":"eye_02","hair":"bob2","outfit":"undi"}
    /// </summary>
    public class CharacterAppearance : MonoBehaviour
    {
        [Header("Layer renderers (gán trong prefab, thứ tự sorting tăng dần)")]
        public SpriteRenderer skinLayer;
        public SpriteRenderer outfitLayer;
        public SpriteRenderer eyesLayer;
        public SpriteRenderer hairLayer;

        [Header("Frame hiển thị (0 = idle south)")]
        public int frameIndex = 0;

        [System.Serializable]
        public class Outfit {
            public string race, skin, eyes, hair, outfit;
        }

        private Outfit _current;

        /// <summary>Áp ngoại hình từ chuỗi JSON server gửi.</summary>
        public void ApplyJson(string outfitJson) {
            if (string.IsNullOrEmpty(outfitJson)) return;
            Outfit o;
            try { o = JsonUtility.FromJson<Outfit>(outfitJson); }
            catch { Debug.LogWarning("[Appearance] outfit JSON lỗi: " + outfitJson); return; }
            if (o == null) return;
            _current = o;
            Apply(o);
        }

        /// <summary>Áp ngoại hình từ object.</summary>
        public void Apply(Outfit o) {
            SetLayer(skinLayer,   "skin",   o.skin);
            SetLayer(outfitLayer, "outfit", o.outfit);
            SetLayer(eyesLayer,   "eyes",   o.eyes);
            SetLayer(hairLayer,   "hair",   o.hair);
        }

        private void SetLayer(SpriteRenderer sr, string slot, string code) {
            if (sr == null) return;
            if (string.IsNullOrEmpty(code)) { sr.enabled = false; return; }
            // Load frame preview (idle) — trong game có thể đổi sang LoadFrame theo animation
            Sprite sp = (frameIndex == 0)
                ? CharPartLoader.Load(slot, code)
                : CharPartLoader.LoadFrame(slot, code, frameIndex);
            if (sp != null) { sr.sprite = sp; sr.enabled = true; }
            else { sr.enabled = false; }
        }

        public Outfit Current => _current;
    }
}
