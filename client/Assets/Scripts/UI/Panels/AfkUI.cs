using UnityEngine;
/// <summary>AfkUI — treo máy: chọn thẻ theo giờ, xem trạng thái, nhận thưởng.</summary>
public class AfkUI : MonoBehaviour {
    public static AfkUI Instance;
    void Awake(){ if(!Instance) Instance=this; else Destroy(gameObject); }
    public void Open(){ gameObject.SetActive(true); }
    public void ClearCards(){}
    public void AddCard(int id, string name, int hours, int price, float expR, float goldR, float dropR){}
    public void SetStatus(int cardId, int secsLeft, long exp, long gold){}
    public void SetInactive(){}
    public void BuyCard(int id) => PacketBuilder.SendAfkBuy(id);
    public void Claim() => PacketBuilder.SendAfkClaim();
    public void Stop() => PacketBuilder.SendAfkStop();
}
