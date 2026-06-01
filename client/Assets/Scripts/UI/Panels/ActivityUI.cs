using UnityEngine;
/// <summary>ActivityUI — Hoat Dong: cot trai danh sach, khung phai chi tiet + moc thuong.</summary>
public class ActivityUI : MonoBehaviour {
    public static ActivityUI Instance;
    void Awake(){ if(!Instance) Instance=this; else Destroy(gameObject); }
    public void Open(){ gameObject.SetActive(true); }

    // cot trai
    public void ClearList(){}
    public void AddActivity(int id, string type, string name, string desc, int icon, int status,
                            string action, float multiplier, long progress, int total, int done, int secsLeft){}
    // khung phai
    public void SetDetailHeader(int id, string name, string desc, string type, string action, long progress, int toStart, int toEnd){}
    public void ClearMilestones(){}
    public void AddMilestone(int msId, int order, int requirement, string rewardJson, string label,
                             int costId, int costQty, int exchangeLimit, bool claimed, bool eligible){}
    public void ShowDetail(){}

    // thao tac
    public void OpenDetail(int id) => PacketBuilder.SendActivityDetail(id);
    public void Claim(int id, int order) => PacketBuilder.SendActivityClaim(id, order);
    public void Exchange(int id, int milestoneId) => PacketBuilder.SendActivityExchange(id, milestoneId);
    public void QuickJoin(int id) => PacketBuilder.SendActivityJoin(id);
    public void ClearRanking(){}
    public void SetMyRank(int rank, long score){}
    public void AddRankRow(int rank, string name, long score){}
    public void AddRankTier(int rankFrom, int rankTo, string label, string rewardJson){}
    public void ShowRanking(){}
    public void OpenRanking(int id) => PacketBuilder.SendActivityRanking(id);
}
