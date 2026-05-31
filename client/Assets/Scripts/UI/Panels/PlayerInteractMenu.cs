using UnityEngine;
using UnityEngine.UI;

public class PlayerInteractMenu : MonoBehaviour
{
    public static PlayerInteractMenu Instance;

    long targetCharId;
    string targetName;

    [SerializeField] Text targetNameText;
    [SerializeField] Transform buttonParent;

    void Awake() { if (!Instance) Instance = this; else Destroy(gameObject); }

    public void Show(long charId, string name, Vector3 screenPos)
    {
        targetCharId = charId;
        targetName = name;
        if (targetNameText) targetNameText.text = name;
        transform.position = screenPos;
        gameObject.SetActive(true);
    }

    public void Hide() { gameObject.SetActive(false); }

    // Menu actions
    public void OnInspect()     { PacketBuilder.SendInspectPlayer(targetCharId); Hide(); }
    public void OnTrade()       { PacketBuilder.SendTrade(targetCharId); Hide(); }
    public void OnAddFriend()   { PacketBuilder.SendFriendRequest(targetCharId); Hide(); }
    public void OnInviteParty() { PacketBuilder.SendPartyInvite(targetCharId); Hide(); }
    public void OnInviteGuild() { PacketBuilder.SendGuildInvite(targetCharId); Hide(); }
    public void OnDuel()        { PacketBuilder.SendDuelRequest(targetCharId); Hide(); }
    public void OnWhisper()     { ChatUI.Instance?.OpenWhisper(targetName); Hide(); }
    public void OnFollow()      { /* follow target movement */ Hide(); }
    public void OnAction()      { ExpressionUI.Instance?.Open(); /* show pair action options */ }
    public void OnBlock()       { PacketBuilder.SendBlock(targetCharId); Hide(); }
    public void OnReport()      { PacketBuilder.SendReport(targetCharId, ""); Hide(); }
}