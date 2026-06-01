using UnityEngine;
/// <summary>ClanBeastUI — Thần Thú bang: hiển thị cấp/exp/buff, nút góp nuôi.</summary>
public class ClanBeastUI : MonoBehaviour {
    public static ClanBeastUI Instance;
    void Awake(){ if(!Instance) Instance=this; else Destroy(gameObject); }
    public void Open(){ gameObject.SetActive(true); PacketBuilder.SendClanBeastInfo(); }
    public void ShowNoGuild(){ GameState.Instance?.ShowToast("Ban chua o bang nao"); }
    public void Show(int level, long exp, long need, int skinId, string name, string buffJson){}
    public void Feed(int units) => PacketBuilder.SendClanBeastFeed(units);
}
