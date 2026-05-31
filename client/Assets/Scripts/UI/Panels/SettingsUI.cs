using UnityEngine;
public class SettingsUI : MonoBehaviour {
    public static SettingsUI Instance;
    bool isOpen;
    void Awake() { if (!Instance) Instance=this; else Destroy(gameObject); }
    public void Open() { isOpen=true; gameObject.SetActive(true); OnOpen(); }
    public void Close() { isOpen=false; gameObject.SetActive(false); }
    void OnOpen() { /* SendSettingsLoad(); SendSettingsSave */ }
}