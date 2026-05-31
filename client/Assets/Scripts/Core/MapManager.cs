using UnityEngine;
public class MapManager : MonoBehaviour {
    public static MapManager Instance;
    public int CurrentMapId { get; private set; }
    void Awake() { if (!Instance) Instance=this; else Destroy(gameObject); }
    public void LoadMap(int mapId) { CurrentMapId = mapId; }

    public void ChangeMap(int mapId, float x, float y) {
        // Tai map moi + dat vi tri nhan vat
    }

    public void EnterFacility(int mapId, long instanceId, string name, string fileName, string category){
        // Tải map facility (asset fileName), set instance hiện tại, đặt nhân vật vào
        ChangeMap(mapId, 50, 50);
    }
}