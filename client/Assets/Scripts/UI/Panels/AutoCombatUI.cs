using UnityEngine;
public class AutoCombatUI : MonoBehaviour {
    public static AutoCombatUI Instance;
    public bool AutoEnabled { get; private set; }
    bool priorityBoss=true, priorityElite, priorityPlayer;
    float autoRange = 8f;
    void Awake() { if(!Instance) Instance=this; else Destroy(gameObject); }
    public void Toggle() { AutoEnabled=!AutoEnabled; PacketBuilder.SendAutoPlay(AutoEnabled); }
    public void SetPriority(bool boss, bool elite, bool player) { priorityBoss=boss; priorityElite=elite; priorityPlayer=player; }
}