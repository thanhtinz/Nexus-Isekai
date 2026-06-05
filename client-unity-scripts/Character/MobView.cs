using UnityEngine;
using UnityEngine.UI;

namespace FantasyRealm.Character
{
    /// <summary>
    /// Hiển thị 1 con quái: tên + thanh máu, nhận click để tấn công,
    /// hiệu ứng khi trúng đòn / chết. Gắn vào mob prefab.
    /// </summary>
    public class MobView : MonoBehaviour
    {
        [Header("UI (gán trong prefab)")]
        public Text nameLabel;
        public Slider hpBar;
        public Text damagePopupPrefab;   // text bay lên khi trúng đòn
        public Transform popupAnchor;
        public SpriteRenderer body;

        public long MobId { get; private set; }
        private int _maxHp, _hp;

        public void Init(long id, string name, int level, int maxHp, int hp) {
            MobId = id; _maxHp = maxHp; _hp = hp;
            if (nameLabel) nameLabel.text = $"{name} Lv.{level}";
            UpdateHpBar();
        }

        public void TakeDamage(int dmg, int hpRemain, bool crit) {
            _hp = hpRemain;
            UpdateHpBar();
            ShowDamagePopup(dmg, crit);
            // nhấp nháy đỏ
            if (body != null) StartCoroutine(FlashRed());
        }

        public void Die() {
            // hiệu ứng chết đơn giản: mờ dần rồi hủy
            Destroy(gameObject, 0.3f);
        }

        void UpdateHpBar() {
            if (hpBar != null) hpBar.value = _maxHp > 0 ? (float)_hp / _maxHp : 0;
        }

        void ShowDamagePopup(int dmg, bool crit) {
            if (damagePopupPrefab == null) return;
            var anchor = popupAnchor != null ? popupAnchor : transform;
            var popup = Instantiate(damagePopupPrefab, anchor.position, Quaternion.identity, anchor);
            popup.text = crit ? $"{dmg}!" : dmg.ToString();
            popup.color = crit ? Color.yellow : Color.white;
            Destroy(popup.gameObject, 1f);
        }

        System.Collections.IEnumerator FlashRed() {
            var orig = body.color;
            body.color = Color.red;
            yield return new WaitForSeconds(0.1f);
            if (body != null) body.color = orig;
        }

        // Click chuột / chạm vào quái → tấn công + chọn làm mục tiêu skill
        void OnMouseDown() {
            MobManager.Instance?.AttackMob(MobId);
            var skillBar = FindObjectOfType<FantasyRealm.UI.SkillBarUI>();
            if (skillBar != null) skillBar.SelectedTargetId = MobId;
        }
    }
}
