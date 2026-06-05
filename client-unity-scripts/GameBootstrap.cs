using UnityEngine;
using FantasyRealm.Network;
using FantasyRealm.Character;
using FantasyRealm.Systems;

namespace FantasyRealm
{
    /// <summary>
    /// Điểm khởi động scene game. Gắn vào 1 GameObject "GameManager" trong scene game.
    /// Nối các thành phần lại: network, nhân vật người chơi, camera, HUD.
    ///
    /// Thứ tự setup trong scene (xem docs/BUILD-UNITY.md):
    ///   - GameNetworkManager + PacketRouter (DontDestroyOnLoad, thường từ scene login)
    ///   - Player prefab có PlayerCharacterController + SpriteSheetAnimator + CharacterAppearance
    ///   - Main Camera có CameraFollow
    ///   - Canvas có PlayerHUD, ChatUI, InventoryUI, SocialUI...
    /// </summary>
    public class GameBootstrap : MonoBehaviour
    {
        [Header("Tham chiếu scene")]
        public PlayerCharacterController player;
        public CameraFollow camera2D;
        public OtherPlayerManager otherPlayers;

        [Header("Tự spawn player nếu chưa đặt sẵn")]
        public GameObject playerPrefab;
        public Vector2 spawnPosition = new Vector2(100, 100);

        void Start() {
            // Đảm bảo có network (nếu vào thẳng scene game khi test)
            if (GameNetworkManager.Instance == null)
                Debug.LogWarning("[Bootstrap] Chưa có GameNetworkManager — cần vào từ scene login, hoặc thêm vào scene.");

            // Spawn player nếu chưa gán
            if (player == null && playerPrefab != null) {
                var go = Instantiate(playerPrefab, spawnPosition, Quaternion.identity);
                player = go.GetComponent<PlayerCharacterController>();
            }

            // Nối camera bám theo player
            if (camera2D != null && player != null)
                camera2D.SetTarget(player.transform);

            // Áp ngoại hình đã chọn lúc tạo nhân vật
            if (player != null) {
                string outfit = PlayerPrefs.GetString("char_outfit", "");
                if (!string.IsNullOrEmpty(outfit)) player.ApplyOutfit(outfit);
            }

            // Xin dữ liệu zone hiện tại
            ClientZoneManager.Instance?.EnterZone(
                ClientZoneManager.Instance.CurrentZoneId, spawnPosition.x, spawnPosition.y);
        }
    }
}
