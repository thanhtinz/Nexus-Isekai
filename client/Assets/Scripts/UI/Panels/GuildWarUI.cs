using UnityEngine;
/// <summary>GuildWarUI — guild chiến: xem trận, tuyên chiến, tham gia.</summary>
public class GuildWarUI : MonoBehaviour {
    public static GuildWarUI Instance;
    void Awake(){ if(!Instance) Instance=this; else Destroy(gameObject); }
    public void Open(){ gameObject.SetActive(true); }
    public void ClearWars(){}
    public void AddWar(int id, int ga, int gb, int mapId, string status, int sa, int sb, int secs){}
    public void Declare(int targetGuild) => PacketBuilder.SendGuildWarDeclare(targetGuild);
    public void Join(int warId) => PacketBuilder.SendGuildWarJoin(warId);
}
