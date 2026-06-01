using UnityEngine;
/// <summary>LuckyWheelUI — Vòng Quay May Mắn: vẽ ô theo segments, quay tới ô server trả về.</summary>
public class LuckyWheelUI : MonoBehaviour {
    public static LuckyWheelUI Instance;
    void Awake(){ if(!Instance) Instance=this; else Destroy(gameObject); }
    public void Open(){ gameObject.SetActive(true); }
    public void ClearList(){}
    public void AddWheel(int id,string name,string desc,int icon,int cost,int currency,int itemId,int pity,string[] segmentLabels){}
    public void PlaySpin(int segmentIndex,string label,string msg){ /* animate kim quay tới segmentIndex */ GameState.Instance?.ShowToast(msg); }
    public void Spin(int wheelId) => PacketBuilder.SendWheelSpin(wheelId);
}
