using UnityEngine;
public class BestiaryUI : MonoBehaviour {
    // Monster encyclopedia, drops, locations
    public static BestiaryUI Instance;
    bool isOpen;
    void Awake() { if(!Instance) Instance=this; else Destroy(gameObject); }
    public void Open() { isOpen=true; gameObject.SetActive(true); OnOpen(); }
    public void Close() { isOpen=false; gameObject.SetActive(false); }
    void OnOpen() { /* Load data from server */ }

    public void Clear(){ /* xoá list */ }
    public void AddEntry(int monsterId, string lore, string weakness, int kills, int need, bool unlocked, bool claimed, bool boss){ }
    public void ClaimReward(int monsterId) => PacketBuilder.SendBestiaryClaim(monsterId);
}