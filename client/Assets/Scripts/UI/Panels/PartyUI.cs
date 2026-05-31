using UnityEngine;
public class PartyUI : MonoBehaviour {
    public static PartyUI Instance;
    bool isOpen;
    void Awake() { if (!Instance) Instance=this; else Destroy(gameObject); }
    public void Open() { isOpen=true; gameObject.SetActive(true); OnOpen(); }
    public void Close() { isOpen=false; gameObject.SetActive(false); }
    void OnOpen() { /* SendPartyCreate(); SendPartyInvite */ }
}