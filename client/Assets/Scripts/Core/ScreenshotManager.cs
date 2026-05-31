using UnityEngine;
public class ScreenshotManager : MonoBehaviour {
    public static ScreenshotManager Instance;
    void Awake() { if(!Instance) Instance=this; else Destroy(gameObject); }
    public void Capture() { string name=$"screenshot_{System.DateTime.Now:yyyyMMdd_HHmmss}.png"; ScreenCapture.CaptureScreenshot(name); NotificationManager.Instance?.Show("Screenshot saved!"); }
}