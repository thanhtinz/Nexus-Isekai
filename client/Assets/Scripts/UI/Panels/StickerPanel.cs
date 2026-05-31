using UnityEngine;

public class StickerPanel : MonoBehaviour
{
    public static StickerPanel Instance;

    [SerializeField] Transform packTabParent;
    [SerializeField] Transform stickerGrid;
    [SerializeField] GameObject stickerPrefab;

    int currentPackId = 1;
    bool isOpen;

    void Awake() { if (!Instance) Instance = this; else Destroy(gameObject); }

    public void Open() { isOpen = true; gameObject.SetActive(true); LoadPack(currentPackId); }
    public void Close() { isOpen = false; gameObject.SetActive(false); }
    public void Toggle() { if (isOpen) Close(); else Open(); }

    public void SwitchPack(int packId)
    {
        currentPackId = packId;
        LoadPack(packId);
    }

    void LoadPack(int packId)
    {
        // Load stickers for pack from Resources or server
    }

    public void SendSticker(int stickerId)
    {
        // Send sticker as chat message type=sticker
        Close();
    }

    public void BuyPack(int packId)
    {
        // Purchase sticker pack with diamond
    }
}