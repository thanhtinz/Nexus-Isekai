using System.Collections;
using System.IO;
using UnityEngine;
using UnityEngine.UI;

namespace FantasyRealm.Systems
{
    /// <summary>
    /// Chụp ảnh trong game (tính năng xã hội Avatar-style): chụp màn hình,
    /// ẩn HUD lúc chụp, lưu vào thư mục, hiện preview. Có chế độ "selfie" zoom.
    /// </summary>
    public class PhotoSystem : MonoBehaviour
    {
        [Header("UI")]
        public GameObject hudRoot;        // ẩn khi chụp
        public GameObject photoModePanel; // khung chế độ chụp
        public Image      previewImage;   // xem ảnh vừa chụp
        public KeyCode    captureKey = KeyCode.P;

        [Header("Lưu ảnh")]
        public string folderName = "FantasyRealm_Photos";

        private bool _photoMode;

        void Update() {
            if (Input.GetKeyDown(captureKey)) Capture();
            if (_photoMode && Input.GetKeyDown(KeyCode.Escape)) ExitPhotoMode();
        }

        public void EnterPhotoMode() {
            _photoMode = true;
            if (photoModePanel) photoModePanel.SetActive(true);
        }
        public void ExitPhotoMode() {
            _photoMode = false;
            if (photoModePanel) photoModePanel.SetActive(false);
        }

        public void Capture() => StartCoroutine(CaptureRoutine());

        IEnumerator CaptureRoutine() {
            bool hudWas = hudRoot && hudRoot.activeSelf;
            if (hudRoot) hudRoot.SetActive(false);   // ẩn HUD cho ảnh sạch
            yield return new WaitForEndOfFrame();

            var tex = ScreenCapture.CaptureScreenshotAsTexture();

            if (hudRoot) hudRoot.SetActive(hudWas);  // hiện lại HUD

            // Lưu file PNG
            string dir = Path.Combine(Application.persistentDataPath, folderName);
            Directory.CreateDirectory(dir);
            string file = Path.Combine(dir, $"photo_{System.DateTime.Now:yyyyMMdd_HHmmss}.png");
            try { File.WriteAllBytes(file, tex.EncodeToPNG()); Debug.Log("[Photo] Đã lưu: " + file); }
            catch (System.Exception e) { Debug.LogWarning("[Photo] Lưu lỗi: " + e.Message); }

            // Hiện preview
            if (previewImage) {
                previewImage.sprite = Sprite.Create(tex,
                    new Rect(0, 0, tex.width, tex.height), new Vector2(.5f, .5f));
                previewImage.gameObject.SetActive(true);
            }
        }
    }
}
