using UnityEngine;
public class GachaManager : MonoBehaviour {
    public static GachaManager Instance;
    void Awake() { if (!Instance) Instance=this; else Destroy(gameObject); }
    public void LoadBanners() => PacketBuilder.SendGachaBannerList();
    public void Pull(int banner, int count) => PacketBuilder.SendGachaPull(banner, count);
    public void BuyTicket(int currId, int amt) => PacketBuilder.SendGachaBuyTicket(currId, amt);
    public void LoadCurrency() => PacketBuilder.SendGachaCurrency();
}