using UnityEngine;
using System.Collections.Generic;
public class MinimapMarkers : MonoBehaviour {
    public static MinimapMarkers Instance;
    [SerializeField] Transform markerParent;
    [SerializeField] GameObject playerMarker, npcMarker, monsterMarker, questMarker, portalMarker;
    Dictionary<long, GameObject> markers = new();
    void Awake() { if(!Instance) Instance=this; else Destroy(gameObject); }
    public void AddMarker(long id, string type, Vector3 pos) { /* instantiate marker by type at pos */ }
    public void RemoveMarker(long id) { if(markers.TryGetValue(id, out var m)) { Destroy(m); markers.Remove(id); } }
    public void UpdateMarker(long id, Vector3 pos) { if(markers.TryGetValue(id, out var m)) m.transform.position=pos; }
}