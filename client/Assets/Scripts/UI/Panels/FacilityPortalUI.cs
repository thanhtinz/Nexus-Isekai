using UnityEngine;
/// <summary>FacilityPortalUI — hiển thị cổng dịch chuyển vào facility map (guild/lễ đường/nhà/...).</summary>
public class FacilityPortalUI : MonoBehaviour {
    public static FacilityPortalUI Instance;
    void Awake(){ if(!Instance) Instance=this; else Destroy(gameObject); }
    public void RequestPortals() => PacketBuilder.SendFacilityPortals();
    public void Clear(){ /* xoá list cổng */ }
    public void AddPortal(int id, float x, float y, string category, string label, int levelReq, int iconId){
        // Vẽ marker/nút cổng tại (x,y) — tap để vào
    }
    public void Enter(string category) => PacketBuilder.SendEnterFacility(category);
    public void Leave() => PacketBuilder.SendLeaveFacility();
}
