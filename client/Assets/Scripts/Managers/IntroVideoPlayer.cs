using UnityEngine;
using UnityEngine.UI;
using UnityEngine.Video;
using System.Collections;

/// <summary>
/// IntroVideoPlayer — Phát video intro sau khi tạo nhân vật lần đầu.
/// Dùng Unity VideoPlayer, stream MP4 từ URL (CDN) hoặc StreamingAssets.
/// Có nút Skip (hiện sau N giây), progress bar, fallback text intro nếu lỗi.
/// </summary>
public class IntroVideoPlayer : MonoBehaviour
{
    public static IntroVideoPlayer Instance;

    [SerializeField] VideoPlayer videoPlayer;
    [SerializeField] RawImage videoSurface;
    [SerializeField] GameObject skipButton;
    [SerializeField] Slider progressBar;
    [SerializeField] GameObject loadingSpinner;

    bool isPlaying;
    System.Action onFinished;

    // Config nhận từ server
    string videoUrl;
    bool skippable = true;
    int skipAfterSec = 3;
    bool fallbackToText = true;

    void Awake() { if (!Instance) Instance = this; else Destroy(gameObject); }

    /// <summary>Gọi sau khi tạo nhân vật lần đầu. Server trả config qua S2C_INTRO_VIDEO_CONFIG.</summary>
    public void RequestAndPlay(System.Action onComplete)
    {
        onFinished = onComplete;
        PacketBuilder.SendIntroVideoConfigRequest();
        // Server phản hồi → OnConfigReceived()
    }

    /// <summary>Nhận config từ server (gọi từ PacketHandlers).</summary>
    public void OnConfigReceived(bool enabled, string url, string urlLow, bool canSkip, int skipSec, bool fallback, bool alreadyWatched)
    {
        if (!enabled || alreadyWatched)
        {
            // Đã xem rồi hoặc tắt → vào game luôn
            onFinished?.Invoke();
            return;
        }

        videoUrl = ChooseQuality(url, urlLow);
        skippable = canSkip;
        skipAfterSec = skipSec;
        fallbackToText = fallback;

        gameObject.SetActive(true);
        StartCoroutine(PlayVideo());
    }

    string ChooseQuality(string high, string low)
    {
        // Mạng yếu → bản low (nếu có)
        if (!string.IsNullOrEmpty(low) && Application.internetReachability == NetworkReachability.ReachableViaCarrierDataNetwork)
            return low;
        return high;
    }

    IEnumerator PlayVideo()
    {
        isPlaying = true;
        if (loadingSpinner) loadingSpinner.SetActive(true);
        if (skipButton) skipButton.SetActive(false);

        // Resolve URL: nếu là path tương đối → StreamingAssets, nếu http → stream
        string fullUrl = videoUrl.StartsWith("http")
            ? videoUrl
            : System.IO.Path.Combine(Application.streamingAssetsPath, videoUrl);

        videoPlayer.source = VideoSource.Url;
        videoPlayer.url = fullUrl;
        videoPlayer.renderMode = VideoRenderMode.RenderTexture;
        videoPlayer.isLooping = false;
        videoPlayer.errorReceived += OnVideoError;
        videoPlayer.loopPointReached += OnVideoEnd;

        videoPlayer.Prepare();
        float prepTimeout = 10f;
        while (!videoPlayer.isPrepared && prepTimeout > 0)
        {
            prepTimeout -= Time.deltaTime;
            yield return null;
        }

        if (!videoPlayer.isPrepared)
        {
            // Không load được video → fallback
            HandleFallback();
            yield break;
        }

        if (loadingSpinner) loadingSpinner.SetActive(false);
        if (videoSurface) videoSurface.texture = videoPlayer.texture;
        videoPlayer.Play();

        // Hiện nút skip sau N giây
        if (skippable)
        {
            yield return new WaitForSeconds(skipAfterSec);
            if (skipButton) skipButton.SetActive(true);
        }

        // Cập nhật progress bar
        while (isPlaying && videoPlayer.isPlaying)
        {
            if (progressBar && videoPlayer.length > 0)
                progressBar.value = (float)(videoPlayer.time / videoPlayer.length);
            yield return null;
        }
    }

    void OnVideoError(VideoPlayer vp, string message)
    {
        Debug.LogWarning("Intro video error: " + message);
        HandleFallback();
    }

    void HandleFallback()
    {
        if (fallbackToText && IntroManager.Instance != null)
        {
            // Chuyển sang intro text (7 scene cũ)
            gameObject.SetActive(false);
            IntroManager.Instance.Request();
            IntroManager.Instance.OnTextIntroComplete = () => Finish();
        }
        else Finish();
    }

    void OnVideoEnd(VideoPlayer vp) => Finish();

    public void OnSkipPressed()
    {
        if (!skippable) return;
        videoPlayer.Stop();
        Finish();
    }

    void Finish()
    {
        if (!isPlaying) return;
        isPlaying = false;
        PacketBuilder.SendIntroComplete(); // mark watched per account
        gameObject.SetActive(false);
        onFinished?.Invoke();
    }
}
