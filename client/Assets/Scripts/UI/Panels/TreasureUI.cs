using UnityEngine;
/// <summary>TreasureUI — Kho Báu: danh sách rương + đào nhận thưởng ngẫu nhiên.</summary>
public class TreasureUI : MonoBehaviour {
    public static TreasureUI Instance;
    void Awake(){ if(!Instance) Instance=this; else Destroy(gameObject); }
    public void Open(){ gameObject.SetActive(true); }
    public void ClearList(){}
    public void AddChest(int id,string name,string desc,int icon,int cost,int currency,int itemId,int dailyLimit,int remaining){}
    public void ShowResult(string msg,string rewardLabel){ GameState.Instance?.ShowToast(msg); }
    public void Dig(int chestId) => PacketBuilder.SendTreasureDig(chestId);
}
