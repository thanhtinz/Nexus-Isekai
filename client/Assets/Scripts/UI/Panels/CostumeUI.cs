using UnityEngine;
public class CostumeUI : MonoBehaviour {
    // Skin/costume wardrobe, preview, equip
    public static CostumeUI Instance;
    bool isOpen;
    void Awake() { if(!Instance) Instance=this; else Destroy(gameObject); }
    public void Open() { isOpen=true; gameObject.SetActive(true); OnOpen(); }
    public void Close() { isOpen=false; gameObject.SetActive(false); }
    void OnOpen() { /* Load data from server */ }
}