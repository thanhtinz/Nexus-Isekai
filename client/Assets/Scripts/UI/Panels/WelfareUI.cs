using UnityEngine;
/// <summary>WelfareUI — Phúc Lợi: cột trái danh sách, khung phải chi tiết + mốc thưởng free/premium.</summary>
public class WelfareUI : MonoBehaviour {
    public static WelfareUI Instance;
    void Awake(){ if(!Instance) Instance=this; else Destroy(gameObject); }
    public void Open(){ gameObject.SetActive(true); }
    // cột trái
    public void ClearList(){}
    public void AddWelfare(int id,string type,string name,string desc,int icon,int status,string claimMode,string gotoFeature,int price,int claimable,int secsLeft){}
    // khung phải
    public void SetDetailHeader(int id,string name,string desc,string type,string claimMode,long progress,bool premium,int price){}
    public void ClearMilestones(){}
    public void AddMilestone(int order,int requirement,string rewardJson,string rewardPremiumJson,string label,bool claimed,bool eligible){}
    public void ShowDetail(){}
    // thao tác
    public void OpenDetail(int id) => PacketBuilder.SendWelfareDetail(id);
    public void Claim(int id,int order) => PacketBuilder.SendWelfareClaim(id, order);
    public void ClaimAll(int id) => PacketBuilder.SendWelfareClaimAll(id);
    public void Activate(int id) => PacketBuilder.SendWelfareActivate(id);
    public void Purchase(int id) => PacketBuilder.SendWelfarePurchase(id);
}
