using UnityEngine;
using System.Collections.Generic;
public class LocalizationManager : MonoBehaviour {
    public static LocalizationManager Instance;
    string lang = "vi";
    Dictionary<string,string> strings = new();
    void Awake() { if (!Instance) { Instance = this; DontDestroyOnLoad(gameObject); Load(lang); } else Destroy(gameObject); }
    public void Load(string l) { lang=l; var j=Resources.Load<TextAsset>($"Lang/{l}"); if(j) strings=JsonUtility.FromJson<Dictionary<string,string>>(j.text)??new(); PacketBuilder.SendLangSet(l); }
    public string Get(string key) => strings.TryGetValue(key, out var v) ? v : key;
}