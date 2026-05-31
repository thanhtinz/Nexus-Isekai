using UnityEngine;
public class GameScene : MonoBehaviour {
    void Start() {
        AudioManager.Instance?.PlayBGM("bgm_village");
        PacketBuilder.SendSettingsLoad();
        PacketBuilder.SendGachaCurrency();
    }
}