using UnityEngine;
public class DailyLoginUI : MonoBehaviour {
    public static DailyLoginUI Instance;
    bool isOpen;
    void Awake() { if (!Instance) Instance=this; else Destroy(gameObject); }
    public void Open() { isOpen=true; gameObject.SetActive(true); OnOpen(); }
    public void Close() { isOpen=false; gameObject.SetActive(false); }
    void OnOpen() { /* SendDailyLoginInfo(); SendDailyLoginClaim */ }
}