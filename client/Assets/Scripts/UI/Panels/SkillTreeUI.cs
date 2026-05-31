using UnityEngine;
public class SkillTreeUI : MonoBehaviour {
    // Skill tree with upgrade paths per class
    public static SkillTreeUI Instance;
    bool isOpen;
    void Awake() { if(!Instance) Instance=this; else Destroy(gameObject); }
    public void Open() { isOpen=true; gameObject.SetActive(true); OnOpen(); }
    public void Close() { isOpen=false; gameObject.SetActive(false); }
    void OnOpen() { /* Load data from server */ }
}