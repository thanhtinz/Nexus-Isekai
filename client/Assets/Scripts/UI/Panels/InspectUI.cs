using UnityEngine;
public class InspectUI : MonoBehaviour {
    // View other player equipment and stats
    public static InspectUI Instance;
    bool isOpen;
    void Awake() { if(!Instance) Instance=this; else Destroy(gameObject); }
    public void Open() { isOpen=true; gameObject.SetActive(true); OnOpen(); }
    public void Close() { isOpen=false; gameObject.SetActive(false); }
    void OnOpen() { /* Load data from server */ }

    public void Show(long charId, string name, int classId, int level, int vip) {
        gameObject.SetActive(true);
        // Hien ten/class/level/vip + trang bi cua nguoi choi khac
    }
}