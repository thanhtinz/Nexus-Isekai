using UnityEngine;
public class WarehouseUI : MonoBehaviour {
    // Storage across characters (per server)
    public static WarehouseUI Instance;
    bool isOpen;
    void Awake() { if(!Instance) Instance=this; else Destroy(gameObject); }
    public void Open() { isOpen=true; gameObject.SetActive(true); OnOpen(); }
    public void Close() { isOpen=false; gameObject.SetActive(false); }
    void OnOpen() { /* Load data from server */ }

    public void SetCapacity(int used, int max){ /* hiển thị x/y ô */ }
    public void Clear(){ }
    public void AddItem(int itemId, int qty, string name, int sellPrice, int icon){ }
    public void Deposit(int itemId, int qty) => PacketBuilder.SendWarehouseDeposit(itemId, qty);
    public void Withdraw(int itemId, int qty) => PacketBuilder.SendWarehouseWithdraw(itemId, qty);
    public void Sell(int itemId, int qty) => PacketBuilder.SendWarehouseSell(itemId, qty);
    public void RequestList() => PacketBuilder.SendWarehouseList();
}