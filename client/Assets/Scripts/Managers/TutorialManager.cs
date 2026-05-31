using UnityEngine;
public class TutorialManager : MonoBehaviour {
    public static TutorialManager Instance;
    public string CurrentStep { get; private set; } = "welcome";
    public bool Active { get; private set; }
    void Awake() { if (!Instance) { Instance=this; DontDestroyOnLoad(gameObject); } else Destroy(gameObject); }
    public void ShowStep(string step, string title, string desc, string targetUI, string arrow) { CurrentStep=step; Active=true; }
    public void Complete() { PacketBuilder.SendTutorialProgress(CurrentStep); }
    public void Skip() { PacketBuilder.SendTutorialSkip(); Active=false; }
}