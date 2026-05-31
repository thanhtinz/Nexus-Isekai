using UnityEngine;
public class DamagePopup : MonoBehaviour {
    public static void Show(Vector3 pos, int damage, bool crit) {
        Color c = crit ? Color.yellow : Color.white;
        string txt = crit ? damage + "!" : damage.ToString();
        FloatingText.Spawn(pos, txt, c);
    }
    public static void ShowHeal(Vector3 pos, int amount) { FloatingText.Spawn(pos, "+" + amount, Color.green); }
    public static void ShowMiss(Vector3 pos) { FloatingText.Spawn(pos, "MISS", Color.gray); }
    public static void ShowExp(Vector3 pos, int exp) { FloatingText.Spawn(pos, "+" + exp + " EXP", new Color(0.5f,0.8f,1f)); }
    public static void ShowGold(Vector3 pos, int gold) { FloatingText.Spawn(pos, "+" + gold + " G", Color.yellow); }
}