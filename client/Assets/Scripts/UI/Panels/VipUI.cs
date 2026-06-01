using UnityEngine;
/// <summary>VipUI — VIP: mốc thưởng + đặc quyền.</summary>
public class VipUI : MonoBehaviour {
    public static VipUI Instance;
    void Awake(){ if(!Instance) Instance=this; else Destroy(gameObject); }
    public void Open(){ gameObject.SetActive(true); }
    public void SetInfo(int vip, string name, int daily, float afkBonus, int bag, int market, int nextReq){}
    public void ClaimMilestone(int vipLevel) => PacketBuilder.SendVipClaim(vipLevel);
    public void ClaimDaily() => PacketBuilder.SendVipDaily();
}
