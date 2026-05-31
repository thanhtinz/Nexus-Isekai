using UnityEngine;
public enum UINotificationType { Info, Success, Warning, Error }
public class UIManager : MonoBehaviour {
    public static UIManager Instance;
    void Awake() { if (!Instance) { Instance=this; DontDestroyOnLoad(gameObject); } else Destroy(gameObject); }
    public void ShowNotification(string msg, UINotificationType type = UINotificationType.Info) { NotificationManager.Instance?.Show(msg); }
    public void ShowPanel(string panelName) { /* Find and activate panel */ }
    public void CloseAll() { /* Close all open panels */ }
}