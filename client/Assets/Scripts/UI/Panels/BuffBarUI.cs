using UnityEngine;
using System.Collections.Generic;
public class BuffBarUI : MonoBehaviour {
    public static BuffBarUI Instance;
    [SerializeField] Transform buffParent, debuffParent;
    [SerializeField] GameObject buffIconPrefab;
    Dictionary<int, (GameObject go, float remaining)> activeBuffs = new();
    void Awake() { if(!Instance) Instance=this; else Destroy(gameObject); }
    public void AddBuff(int id, string name, float duration, bool isDebuff) { /* create icon, start timer */ }
    public void RemoveBuff(int id) { if(activeBuffs.TryGetValue(id, out var b)) { Destroy(b.go); activeBuffs.Remove(id); } }
    void Update() { /* tick down timers, remove expired */ }
}