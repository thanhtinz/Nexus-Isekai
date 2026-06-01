using UnityEngine;
using System.Collections.Generic;
public class AudioManager : MonoBehaviour {
    public static AudioManager Instance;
    [SerializeField] AudioSource bgmSource, sfxSource, uiSource, ambientSource;
    Dictionary<string, AudioClip> clips = new();
    float master=0.8f, music=0.6f, sfx=0.8f, ui=0.7f, ambient=0.5f;
    void Awake() { if (!Instance) { Instance = this; DontDestroyOnLoad(gameObject); } else Destroy(gameObject); }
    public void PlayBGM(string key) { if (clips.TryGetValue(key, out var c)) { bgmSource.clip=c; bgmSource.volume=master*music; bgmSource.loop=true; bgmSource.Play(); } }
    public void PlaySFX(string key) { if (clips.TryGetValue(key, out var c)) sfxSource.PlayOneShot(c, master*sfx); }
    public void PlayUI(string key) { if (clips.TryGetValue(key, out var c)) uiSource.PlayOneShot(c, master*ui); }
    public void PlayVoice(string key) { if (clips.TryGetValue(key, out var c)) sfxSource.PlayOneShot(c, master*sfx); } // lời thoại class/npc
    public void SetVolume(string t, float v) { switch(t) { case "master":master=v;break; case "music":music=v;break; case "sfx":sfx=v;break; case "ui":ui=v;break; case "ambient":ambient=v;break; } }
    public void StopBGM() { bgmSource.Stop(); }
}