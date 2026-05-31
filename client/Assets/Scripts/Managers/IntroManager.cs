using UnityEngine;
using System.Collections;
public class IntroManager : MonoBehaviour {
    public static IntroManager Instance;
    public bool Playing { get; private set; }
    void Awake() { if (!Instance) Instance=this; else Destroy(gameObject); }
    public void Request() => PacketBuilder.SendIntroRequest();
    public void Play(string json) { StartCoroutine(RunIntro(json)); }
    IEnumerator RunIntro(string json) { Playing=true; yield return null; Playing=false; }
    public void Complete() { PacketBuilder.SendIntroComplete(); Playing=false; }
    public void Skip() { PacketBuilder.SendIntroSkip(); Playing=false; }
}