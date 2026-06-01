using UnityEngine;
public class AssetManager : MonoBehaviour {
    public static AssetManager Instance;
    void Awake() { if (!Instance) { Instance=this; DontDestroyOnLoad(gameObject); } else Destroy(gameObject); }
    public T Load<T>(string path) where T : Object => Resources.Load<T>(path);
    public Sprite LoadIcon(int iconId) => Load<Sprite>($"Sprites/Icons/Items/{iconId / 500}/{iconId}");
    public Sprite LoadMapBg(int mapId) => Load<Sprite>($"Sprites/Maps/backgrounds/bg_{mapId}");
}