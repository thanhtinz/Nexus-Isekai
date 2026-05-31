using UnityEngine;
public class MapManager : MonoBehaviour {
    public static MapManager Instance;
    public int CurrentMapId { get; private set; }
    void Awake() { if (!Instance) Instance=this; else Destroy(gameObject); }
    public void LoadMap(int mapId) { CurrentMapId = mapId; }

    public void ChangeMap(int mapId, float x, float y) {
        // Tai map moi + dat vi tri nhan vat
    }
}