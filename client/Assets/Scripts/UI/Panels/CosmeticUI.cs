using UnityEngine;
/// <summary>CosmeticUI — Cánh/Hào quang. Trang bị + nâng cấp (có chỉ số).</summary>
public class CosmeticUI : MonoBehaviour {
    public static CosmeticUI Instance;
    void Awake(){ if(!Instance) Instance=this; else Destroy(gameObject); }
    public void Open(){ gameObject.SetActive(true); PacketBuilder.SendCosmeticList(); }
    public void Clear(){ /* xoá list */ }
    public void AddItem(long id, string name, string type, int rarity, int lvl, int maxLvl, bool equipped, string stats, int sprite){ }
    public void OnEquipped(long id){ /* cập nhật UI đã mặc */ }
    public void OnUpgraded(long id, int lvl){ /* cập nhật cấp */ }
    public void Equip(long id) => PacketBuilder.SendCosmeticEquip(id);
    public void Upgrade(long id) => PacketBuilder.SendCosmeticUpgrade(id);
}
