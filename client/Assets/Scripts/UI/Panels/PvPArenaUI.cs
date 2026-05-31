using UnityEngine;
public class PvPArenaUI : MonoBehaviour {
    public static PvPArenaUI Instance;
    bool isOpen;
    void Awake() { if (!Instance) Instance=this; else Destroy(gameObject); }
    public void Open() { isOpen=true; gameObject.SetActive(true); OnOpen(); }
    public void Close() { isOpen=false; gameObject.SetActive(false); }
    void OnOpen() { /* PvPManager.Instance?.LoadSeason() */ }
}