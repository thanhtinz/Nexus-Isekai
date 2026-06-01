using UnityEngine;
/// <summary>VipUI — VIP: nhiều quyền lợi theo mốc + thưởng mốc nhận 1 lần.</summary>
public class VipUI : MonoBehaviour {
    public static VipUI Instance;
    void Awake(){ if(!Instance) Instance=this; else Destroy(gameObject); }
    public void Open(){ gameObject.SetActive(true); }

    /// <summary>Đặc quyền đầy đủ của mốc VIP hiện tại.</summary>
    public void SetInfo(int vip, string name, int topup, int nextReq,
                        int dailyDiamond, int dailyGold, float afkBonus, float expBonus,
                        float dropBonus, float goldBonus, int bagSlots, int marketSlots,
                        float marketFeeDiscount, float reviveDiscount, int afkCapHours,
                        int freeTeleportDaily, bool autoPickup, string nameColor){}

    public void ClearMilestones(){}
    public void AddMilestone(int vipLevel, bool eligible, bool claimed){}

    public void ClaimMilestone(int vipLevel) => PacketBuilder.SendVipClaim(vipLevel);
    public void ClaimDaily() => PacketBuilder.SendVipDaily();
}
