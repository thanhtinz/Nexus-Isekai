using UnityEngine;
/// <summary>OuterRealmUI — Ngoại Vực: chọn tầng theo level, vào khu PK.</summary>
public class OuterRealmUI : MonoBehaviour {
    public static OuterRealmUI Instance;
    void Awake(){ if(!Instance) Instance=this; else Destroy(gameObject); }
    public void Open(){ gameObject.SetActive(true); }
    public void ClearFloors(){}
    public void AddFloor(int floor, string name, int mapId, int minLv, bool pvp, int mMin, int mMax, bool canEnter){}
    public void Enter(int floor) => PacketBuilder.SendOuterEnter(floor);
    public void Leave() => PacketBuilder.SendOuterLeave();
}
