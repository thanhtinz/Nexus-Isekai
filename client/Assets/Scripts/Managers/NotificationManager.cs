using UnityEngine;
using System.Collections.Generic;
public class NotificationManager : MonoBehaviour {
    public static NotificationManager Instance;
    Queue<string> queue = new();
    void Awake() { if (!Instance) { Instance=this; DontDestroyOnLoad(gameObject); } else Destroy(gameObject); }
    public void Show(string msg) { queue.Enqueue(msg); }
}