using UnityEngine;
/// <summary>HouseInteriorUI — nội thất nhà: mua, đặt, tương tác (ngồi/nằm/ăn/uống).</summary>
public class HouseInteriorUI : MonoBehaviour {
    public static HouseInteriorUI Instance;
    void Awake(){ if(!Instance) Instance=this; else Destroy(gameObject); }
    public void Open(){ gameObject.SetActive(true); PacketBuilder.SendHouseFurniture(); }
    public void BuyFurniture(int furnitureId) => PacketBuilder.SendFurnitureBuy(furnitureId);
    public void Interact(long furnitureInstanceId) => PacketBuilder.SendFurnitureInteract(furnitureInstanceId);
    public void StandUp() => PacketBuilder.SendFurnitureStop();
    // type: sit/lie/eat/drink/bath
    public void OnFurnitureUsed(long charId, long fid, string type, string anim, int hp, int mp){
        // Play animation 'anim' cho nhân vật charId tại vị trí nội thất fid
    }
    public void OnFurnitureStop(long charId){ /* nhân vật charId đứng dậy */ }
}
