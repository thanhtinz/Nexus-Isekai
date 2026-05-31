using UnityEngine;
using UnityEngine.UI;
public class FPSCounter : MonoBehaviour {
    [SerializeField] Text fpsText, pingText;
    float timer; int frames;
    void Update() { frames++; timer+=Time.deltaTime; if(timer>=0.5f) { if(fpsText) fpsText.text=Mathf.RoundToInt(frames/timer)+" FPS"; frames=0; timer=0; } }
    public void UpdatePing(int ms) { if(pingText) pingText.text=ms+"ms"; }
}