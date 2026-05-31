using UnityEngine;
public class LoginScene : MonoBehaviour {
    string username = "", password = "";
    void Start() { AudioManager.Instance?.PlayBGM("bgm_login"); PacketBuilder.SendLoginScreenConfig(); }
    public void OnLogin() { if (username.Length > 0) PacketBuilder.SendLogin(username, password); }
    public void OnRegister() { if (username.Length > 0) PacketBuilder.SendRegister(username, password); }
    public void OnGoogleLogin() { SocialLoginManager.Instance?.Login("google"); }
    public void OnFacebookLogin() { SocialLoginManager.Instance?.Login("facebook"); }
}