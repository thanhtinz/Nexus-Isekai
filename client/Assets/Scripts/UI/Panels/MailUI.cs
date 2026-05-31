using UnityEngine;
public class MailUI : MonoBehaviour {
    public static MailUI Instance;
    bool isOpen;
    void Awake() { if (!Instance) Instance=this; else Destroy(gameObject); }
    public void Open() { isOpen=true; gameObject.SetActive(true); OnOpen(); }
    public void Close() { isOpen=false; gameObject.SetActive(false); }
    void OnOpen() { /* SendMailList(); SendMailRead */ }
}