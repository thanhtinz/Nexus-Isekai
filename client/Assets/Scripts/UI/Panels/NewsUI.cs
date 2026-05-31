using UnityEngine;
public class NewsUI : MonoBehaviour {
    // In-game news panel, categories, detail view
    public static NewsUI Instance;
    bool isOpen;
    void Awake() { if(!Instance) Instance=this; else Destroy(gameObject); }
    public void Open() { isOpen=true; gameObject.SetActive(true); OnOpen(); }
    public void Close() { isOpen=false; gameObject.SetActive(false); }
    void OnOpen() { /* Load data from server */ }

    public void Clear() { /* xoa list tin tuc */ }
    public void AddItem(int id, string title, string category) { /* them dong tin tuc */ }
}