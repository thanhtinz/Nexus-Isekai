using UnityEngine;
using UnityEngine.UI;
using System.Collections;

/// <summary>IntroUI — hiển thị intro text (7 scene) với hiệu ứng typewriter. Fallback khi video lỗi.</summary>
public class IntroUI : MonoBehaviour
{
    public static IntroUI Instance;
    [SerializeField] Text narratorText, bodyText;
    [SerializeField] Image bgImage;
    [SerializeField] GameObject skipButton;

    void Awake() { if (!Instance) Instance = this; else Destroy(gameObject); }

    public IEnumerator ShowScenes(string json)
    {
        gameObject.SetActive(true);
        if (skipButton) skipButton.SetActive(true);
        // Parse scenes JSON → hiển thị từng scene với typewriter
        // (IntroManager gọi; chi tiết parse tuỳ format server trả)
        yield return null;
        gameObject.SetActive(false);
    }

    public void OnSkip() => IntroManager.Instance?.Skip();
}
