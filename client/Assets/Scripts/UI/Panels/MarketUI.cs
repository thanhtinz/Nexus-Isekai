using UnityEngine;
/// <summary>MarketUI — chợ người chơi: bán/mua giá cố định (vàng/kim cương).</summary>
public class MarketUI : MonoBehaviour {
    public static MarketUI Instance;
    void Awake(){ if(!Instance) Instance=this; else Destroy(gameObject); }
    public void Open(){ gameObject.SetActive(true); }
    public void ClearListings(){}
    public void AddListing(long id, string seller, int itemId, string itemName, int qty, int enh, int currency, long price, string cat){}
    public void Refresh() => PacketBuilder.SendMarketList("all", 0, 0);
    public void Sell(long invId, int qty, int currency, long price) => PacketBuilder.SendMarketSell(invId, qty, currency, price);
    public void Buy(long listingId) => PacketBuilder.SendMarketBuy(listingId);
    public void Cancel(long listingId) => PacketBuilder.SendMarketCancel(listingId);
}
