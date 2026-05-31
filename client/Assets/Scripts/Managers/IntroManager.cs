using UnityEngine;
using System.Collections;

/// <summary>
/// IntroManager — quản lý intro text (7 scene). Phối hợp với IntroVideoPlayer.
/// Flow: tạo nhân vật lần đầu → thử video → lỗi/máy yếu → fallback text.
/// </summary>
public class IntroManager : MonoBehaviour
{
    public static IntroManager Instance;
    public bool Playing { get; private set; }
    public System.Action OnTextIntroComplete;

    void Awake() { if (!Instance) Instance = this; else Destroy(gameObject); }

    /// <summary>Gọi sau khi tạo nhân vật lần đầu — ưu tiên video, fallback text.</summary>
    public void PlayFirstTimeIntro(System.Action onDone)
    {
        if (IntroVideoPlayer.Instance != null && SupportsVideo())
            IntroVideoPlayer.Instance.RequestAndPlay(onDone);
        else
        {
            OnTextIntroComplete = onDone;
            Request(); // text intro
        }
    }

    bool SupportsVideo()
    {
        // PC/Android/iOS/WebGL hỗ trợ; J2ME không (J2ME dùng client riêng nên luôn text)
        return Application.platform != RuntimePlatform.WebGLPlayer || true;
    }

    public void Request() => PacketBuilder.SendIntroRequest();
    public void Play(string json) { StartCoroutine(RunIntro(json)); }

    IEnumerator RunIntro(string json)
    {
        Playing = true;
        // IntroUI hiển thị từng scene (typewriter) — xem IntroUI.cs
        if (IntroUI.Instance != null) yield return IntroUI.Instance.ShowScenes(json);
        else yield return null;
        Playing = false;
        Complete();
    }

    public void Complete()
    {
        PacketBuilder.SendIntroComplete();
        Playing = false;
        OnTextIntroComplete?.Invoke();
    }

    public void Skip()
    {
        PacketBuilder.SendIntroSkip();
        Playing = false;
        OnTextIntroComplete?.Invoke();
    }
}
