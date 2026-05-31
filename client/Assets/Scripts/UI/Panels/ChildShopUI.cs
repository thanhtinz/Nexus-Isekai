using UnityEngine;
/// <summary>ChildShopUI — shop con cái: thời trang, đồ ăn, uống, tả, bảo mẫu.</summary>
public class ChildShopUI : MonoBehaviour {
    public static ChildShopUI Instance;
    void Awake(){ if(!Instance) Instance=this; else Destroy(gameObject); }
    public void Open(){ gameObject.SetActive(true); PacketBuilder.SendChildShop(); }
    public void Clear(){ }
    public void AddItem(int id, string name, string category, int gold, int diamond, string slot, int fashionId, int nannyHours, int icon){ }
    public void Buy(long childId, int itemId) => PacketBuilder.SendChildBuy(childId, itemId);
    public void HireNanny(long childId, int itemId) => PacketBuilder.SendChildHireNanny(childId, itemId);
}
