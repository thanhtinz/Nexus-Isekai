using UnityEngine;
public class GachaUI : MonoBehaviour {
    public static GachaUI Instance;
    bool isOpen;
    void Awake() { if (!Instance) Instance=this; else Destroy(gameObject); }
    public void Open() { isOpen=true; gameObject.SetActive(true); OnOpen(); }
    public void Close() { isOpen=false; gameObject.SetActive(false); }
    void OnOpen() { /* GachaManager.Instance?.LoadBanners() */ }
}