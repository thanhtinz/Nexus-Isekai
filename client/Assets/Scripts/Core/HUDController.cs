using UnityEngine;
using UnityEngine.UI;
public class HUDController : MonoBehaviour {
    public static HUDController Instance;
    [SerializeField] Slider hpBar, mpBar, expBar;
    [SerializeField] Text levelText, goldText, diamondText, nameText;
    void Awake() { if (!Instance) Instance=this; else Destroy(gameObject); }
    public void UpdateHP(int cur, int max) { if (hpBar) hpBar.value = (float)cur/max; }
    public void UpdateMP(int cur, int max) { if (mpBar) mpBar.value = (float)cur/max; }
    public void UpdateEXP(long cur, long max) { if (expBar) expBar.value = (float)cur/max; }
    public void UpdateLevel(int lv) { if (levelText) levelText.text = lv.ToString(); }
    public void UpdateGold(long g) { if (goldText) goldText.text = g.ToString("N0"); }
    public void UpdateDiamond(long d) { if (diamondText) diamondText.text = d.ToString("N0"); }
}