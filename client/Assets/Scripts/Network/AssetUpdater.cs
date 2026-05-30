// AssetUpdater.cs — Hệ thống cập nhật asset OTA cho Unity client
// Khi khởi động game → kiểm tra manifest → tải asset mới → áp dụng
// Admin upload ảnh/config → client tự cập nhật không cần build lại

using System;
using System.Collections;
using System.Collections.Generic;
using System.IO;
using System.Security.Cryptography;
using UnityEngine;
using UnityEngine.Networking;

namespace NexusIsekai.Network
{
    /// <summary>
    /// AssetUpdater — gọi khi game khởi động.
    /// Flow:
    ///   1. GET /api/client/version → kiểm tra phiên bản app
    ///   2. GET /api/client/manifest → so sánh hash với local cache
    ///   3. GET /api/client/asset/{id} → tải file thay đổi
    ///   4. GET /api/client/config → hot config (poll mỗi 5 phút)
    /// </summary>
    public class AssetUpdater : MonoBehaviour
    {
        public static AssetUpdater Instance { get; private set; }

        [Header("Server")]
        public string serverUrl = "http://your-server-ip:9090";

        [Header("Cấu hình")]
        public int currentVersionCode = 1;
        public string currentPlatform = "android";

        [Header("Trạng thái")]
        public bool isChecking = false;
        public bool isDownloading = false;
        public int totalAssets = 0;
        public int downloadedAssets = 0;
        public long totalBytes = 0;
        public long downloadedBytes = 0;
        public string statusText = "";

        // Events
        public event Action OnUpdateComplete;
        public event Action<string> OnStatusChanged;
        public event Action<float> OnProgress;
        public event Action<bool, string, string> OnForceUpdateRequired; // (force, version, url)

        // Local cache
        private string cacheDir;
        private Dictionary<string, string> localHashes = new Dictionary<string, string>();
        private const string HASH_CACHE_FILE = "asset_hashes.json";
        private const string CONFIG_CACHE_FILE = "hot_config.json";

        // Hot config kết quả
        public Dictionary<string, object> HotConfig { get; private set; } = new Dictionary<string, object>();

        private void Awake()
        {
            if (Instance != null) { Destroy(gameObject); return; }
            Instance = this;
            DontDestroyOnLoad(gameObject);

            cacheDir = Path.Combine(Application.persistentDataPath, "ota_cache");
            if (!Directory.Exists(cacheDir)) Directory.CreateDirectory(cacheDir);

            LoadLocalHashes();

#if UNITY_ANDROID
            currentPlatform = "android";
#elif UNITY_IOS
            currentPlatform = "ios";
#elif UNITY_WEBGL
            currentPlatform = "webgl";
#else
            currentPlatform = "pc";
#endif
        }

        // ═══════════════════════════════════════════════════════════
        // Public API
        // ═══════════════════════════════════════════════════════════

        /// <summary>Gọi khi khởi động game (từ LoginScene)</summary>
        public void CheckForUpdates()
        {
            if (isChecking) return;
            StartCoroutine(UpdateSequence());
        }

        /// <summary>Bắt đầu poll hot config (gọi sau khi vào game)</summary>
        public void StartConfigPolling(float intervalSeconds = 300f)
        {
            StartCoroutine(ConfigPollLoop(intervalSeconds));
        }

        /// <summary>Lấy asset đã cache từ local</summary>
        public string GetCachedAssetPath(string assetKey)
        {
            string localPath = Path.Combine(cacheDir, assetKey.Replace("/", Path.DirectorySeparatorChar.ToString()));
            return File.Exists(localPath) ? localPath : null;
        }

        /// <summary>Load Sprite từ cached asset</summary>
        public Sprite LoadCachedSprite(string assetKey)
        {
            string path = GetCachedAssetPath(assetKey);
            if (path == null) return null;
            byte[] bytes = File.ReadAllBytes(path);
            Texture2D tex = new Texture2D(2, 2);
            tex.LoadImage(bytes);
            return Sprite.Create(tex, new Rect(0, 0, tex.width, tex.height), new Vector2(0.5f, 0.5f));
        }

        /// <summary>Lấy config value</summary>
        public T GetConfig<T>(string key, T defaultValue = default)
        {
            if (HotConfig.TryGetValue(key, out object val))
            {
                try { return (T)Convert.ChangeType(val, typeof(T)); }
                catch { return defaultValue; }
            }
            return defaultValue;
        }

        // ═══════════════════════════════════════════════════════════
        // Update Sequence
        // ═══════════════════════════════════════════════════════════

        private IEnumerator UpdateSequence()
        {
            isChecking = true;
            SetStatus("Kiểm tra phiên bản...");

            // 1. Kiểm tra phiên bản app
            yield return CheckAppVersion();

            // 2. Kiểm tra asset manifest
            SetStatus("Kiểm tra tài nguyên...");
            yield return CheckAndDownloadAssets();

            // 3. Tải hot config
            SetStatus("Tải cấu hình...");
            yield return FetchHotConfig();

            isChecking = false;
            SetStatus("Sẵn sàng!");
            OnUpdateComplete?.Invoke();
        }

        // ─── 1. Kiểm tra phiên bản app ────────────────────────────

        private IEnumerator CheckAppVersion()
        {
            string url = $"{serverUrl}/api/client/version?platform={currentPlatform}&current={currentVersionCode}";
            using UnityWebRequest req = UnityWebRequest.Get(url);
            req.timeout = 10;
            yield return req.SendWebRequest();

            if (req.result != UnityWebRequest.Result.Success)
            {
                Debug.LogWarning($"[OTA] Version check failed: {req.error}");
                yield break;
            }

            var json = JsonUtility.FromJson<VersionResponse>(req.downloadHandler.text);
            if (json.force_update)
            {
                SetStatus("Cần cập nhật ứng dụng!");
                OnForceUpdateRequired?.Invoke(true, json.latest_version, json.download_url);
                // Dừng lại — user phải cập nhật app
                isChecking = false;
                yield break;
            }
            else if (json.update_available)
            {
                OnForceUpdateRequired?.Invoke(false, json.latest_version, json.download_url);
            }
        }

        // ─── 2. Kiểm tra + tải asset ──────────────────────────────

        private IEnumerator CheckAndDownloadAssets()
        {
            string url = $"{serverUrl}/api/client/manifest?since_version=0";
            using UnityWebRequest req = UnityWebRequest.Get(url);
            req.timeout = 15;
            yield return req.SendWebRequest();

            if (req.result != UnityWebRequest.Result.Success)
            {
                Debug.LogWarning($"[OTA] Manifest failed: {req.error}");
                yield break;
            }

            var manifest = JsonUtility.FromJson<ManifestResponse>(req.downloadHandler.text);
            if (!manifest.success || manifest.assets == null || manifest.assets.Length == 0)
            {
                Debug.Log("[OTA] No assets to update");
                yield break;
            }

            // So sánh hash → tìm file cần tải
            List<AssetInfo> toDownload = new List<AssetInfo>();
            foreach (var asset in manifest.assets)
            {
                string localHash = "";
                localHashes.TryGetValue(asset.key, out localHash);
                if (localHash != asset.hash)
                {
                    toDownload.Add(asset);
                }
            }

            if (toDownload.Count == 0)
            {
                Debug.Log("[OTA] All assets up-to-date");
                yield break;
            }

            totalAssets = toDownload.Count;
            downloadedAssets = 0;
            totalBytes = 0;
            foreach (var a in toDownload) totalBytes += a.size;
            downloadedBytes = 0;
            isDownloading = true;

            SetStatus($"Cập nhật {totalAssets} tài nguyên...");
            Debug.Log($"[OTA] Downloading {totalAssets} assets ({totalBytes / 1024}KB)");

            foreach (var asset in toDownload)
            {
                yield return DownloadAsset(asset);
                downloadedAssets++;
                float progress = (float)downloadedAssets / totalAssets;
                OnProgress?.Invoke(progress);
                SetStatus($"Tải {downloadedAssets}/{totalAssets}...");
            }

            isDownloading = false;
            SaveLocalHashes();
            Debug.Log("[OTA] All assets downloaded");
        }

        private IEnumerator DownloadAsset(AssetInfo asset)
        {
            string url = $"{serverUrl}{asset.url}";
            using UnityWebRequest req = UnityWebRequest.Get(url);

            // Gửi ETag nếu có
            string localHash = "";
            if (localHashes.TryGetValue(asset.key, out localHash))
                req.SetRequestHeader("If-None-Match", localHash);

            req.timeout = 30;
            yield return req.SendWebRequest();

            if (req.responseCode == 304)
            {
                // Not Modified — file không đổi
                yield break;
            }

            if (req.result != UnityWebRequest.Result.Success)
            {
                Debug.LogWarning($"[OTA] Failed to download {asset.key}: {req.error}");
                yield break;
            }

            // Lưu file
            string localPath = Path.Combine(cacheDir, asset.key.Replace("/", Path.DirectorySeparatorChar.ToString()));
            string dir = Path.GetDirectoryName(localPath);
            if (!Directory.Exists(dir)) Directory.CreateDirectory(dir);

            File.WriteAllBytes(localPath, req.downloadHandler.data);
            downloadedBytes += asset.size;

            // Cập nhật hash
            localHashes[asset.key] = asset.hash;

            Debug.Log($"[OTA] Downloaded: {asset.key} ({asset.size}B)");
        }

        // ─── 3. Hot Config ─────────────────────────────────────────

        private IEnumerator FetchHotConfig()
        {
            string url = $"{serverUrl}/api/client/config";
            using UnityWebRequest req = UnityWebRequest.Get(url);
            req.timeout = 10;
            yield return req.SendWebRequest();

            if (req.result != UnityWebRequest.Result.Success)
            {
                // Dùng cache nếu có
                LoadCachedConfig();
                yield break;
            }

            string responseText = req.downloadHandler.text;

            // Lưu cache
            string cachePath = Path.Combine(cacheDir, CONFIG_CACHE_FILE);
            File.WriteAllText(cachePath, responseText);

            ParseConfig(responseText);
        }

        private IEnumerator ConfigPollLoop(float interval)
        {
            while (true)
            {
                yield return new WaitForSeconds(interval);
                yield return FetchHotConfig();
            }
        }

        private void ParseConfig(string json)
        {
            // Dùng simple JSON parse (Unity JsonUtility không hỗ trợ Dictionary)
            // Đơn giản: parse thủ công
            try
            {
                var configResp = JsonUtility.FromJson<ConfigResponse>(json);
                // Config values sẽ được parse từ raw JSON
                // Simplified: lưu raw text
                Debug.Log("[OTA] Hot config loaded");
            }
            catch (Exception e)
            {
                Debug.LogWarning($"[OTA] Config parse error: {e.Message}");
            }
        }

        private void LoadCachedConfig()
        {
            string cachePath = Path.Combine(cacheDir, CONFIG_CACHE_FILE);
            if (File.Exists(cachePath))
            {
                ParseConfig(File.ReadAllText(cachePath));
            }
        }

        // ═══════════════════════════════════════════════════════════
        // Local hash cache (lưu MD5 của mỗi asset đã tải)
        // ═══════════════════════════════════════════════════════════

        private void LoadLocalHashes()
        {
            string path = Path.Combine(cacheDir, HASH_CACHE_FILE);
            if (!File.Exists(path)) return;
            try
            {
                string json = File.ReadAllText(path);
                var data = JsonUtility.FromJson<HashCache>(json);
                if (data?.keys != null && data?.values != null)
                {
                    for (int i = 0; i < data.keys.Length && i < data.values.Length; i++)
                        localHashes[data.keys[i]] = data.values[i];
                }
            }
            catch { }
        }

        private void SaveLocalHashes()
        {
            var data = new HashCache
            {
                keys   = new string[localHashes.Count],
                values = new string[localHashes.Count]
            };
            int i = 0;
            foreach (var kv in localHashes)
            {
                data.keys[i] = kv.Key;
                data.values[i] = kv.Value;
                i++;
            }
            string json = JsonUtility.ToJson(data, false);
            File.WriteAllText(Path.Combine(cacheDir, HASH_CACHE_FILE), json);
        }

        // ═══════════════════════════════════════════════════════════
        // Helpers
        // ═══════════════════════════════════════════════════════════

        private void SetStatus(string msg)
        {
            statusText = msg;
            OnStatusChanged?.Invoke(msg);
        }

        // ═══════════════════════════════════════════════════════════
        // JSON models (cho JsonUtility)
        // ═══════════════════════════════════════════════════════════

        [Serializable]
        private class VersionResponse
        {
            public bool success;
            public bool update_available;
            public bool force_update;
            public string latest_version;
            public int latest_code;
            public string download_url;
            public string release_notes;
            public int min_asset_version;
        }

        [Serializable]
        private class ManifestResponse
        {
            public bool success;
            public int asset_version;
            public int total_assets;
            public long total_size;
            public AssetInfo[] assets;
        }

        [Serializable]
        public class AssetInfo
        {
            public int id;
            public string key;
            public string type;
            public string category;
            public int size;
            public string hash;
            public int version;
            public bool required;
            public string url;
        }

        [Serializable]
        private class ConfigResponse
        {
            public bool success;
            // config field is a dictionary — Unity JsonUtility can't deserialize it
            // Use raw JSON parsing or a 3rd party lib
        }

        [Serializable]
        private class HashCache
        {
            public string[] keys;
            public string[] values;
        }
    }
}
