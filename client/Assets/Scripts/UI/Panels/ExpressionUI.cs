using UnityEngine;

public class ExpressionUI : MonoBehaviour
{
    public static ExpressionUI Instance;
    bool isOpen;
    void Awake() { if (!Instance) Instance = this; else Destroy(gameObject); }

    public void Open() { isOpen = true; gameObject.SetActive(true); }
    public void Close() { isOpen = false; gameObject.SetActive(false); }

    // Bieu cam (hien icon tren dau nhan vat)
    public void SendExpression(int exprId) { PacketBuilder.SendEmote(exprId); Close(); }

    // Hanh dong solo (sit, wave, dance...)
    public void SendAction(int actionId) { PacketBuilder.SendCharAction(actionId); Close(); }

    // Hanh dong doi (hug, highfive... can target dong y)
    public void SendPairAction(int actionId, long targetCharId)
    {
        PacketBuilder.SendPairAction(actionId, targetCharId);
        Close();
    }

    public void ShowPairRequest(long requesterId, string name, int actionId) {
        // Popup: "{name} muon hanh dong voi ban" → Accept goi SendPairActionAccept
        gameObject.SetActive(true);
    }
}