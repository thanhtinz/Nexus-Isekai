using UnityEngine;
/// <summary>SetBonusUI — Bộ trang bị. Hiển thị set đang mặc + bonus theo số mảnh.</summary>
public class SetBonusUI : MonoBehaviour {
    public static SetBonusUI Instance;
    void Awake(){ if(!Instance) Instance=this; else Destroy(gameObject); }
    public void Open(){ gameObject.SetActive(true); PacketBuilder.SendSetInfo(); }
    public void Clear(){ /* xoá list set */ }
    public void AddBonus(int setId, string name, int pieces, int req, string stats, string desc){ /* thêm dòng bonus, active nếu pieces>=req */ }
    public void Refresh() => PacketBuilder.SendSetInfo();
}
