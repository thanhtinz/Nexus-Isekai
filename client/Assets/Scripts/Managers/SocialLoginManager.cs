using UnityEngine;
public class SocialLoginManager : MonoBehaviour {
    public static SocialLoginManager Instance;
    void Awake() { if (!Instance) { Instance=this; DontDestroyOnLoad(gameObject); } else Destroy(gameObject); }
    public void Login(string provider) { string token=""; PacketBuilder.SendSocialLogin(provider, token); }
    public void Link(string provider, string token) => PacketBuilder.SendSocialLink(provider, token);
    public void Unlink(string provider) => PacketBuilder.SendSocialUnlink(provider);
}