using UnityEngine;
public class ServerSelectManager : MonoBehaviour {
    public static ServerSelectManager Instance;
    void Awake() { if (!Instance) Instance=this; else Destroy(gameObject); }
    public void LoadServers() => PacketBuilder.SendServerList();
    public void Select(int id) => PacketBuilder.SendServerSelect(id);
    public void LoadChannels(int sid) => PacketBuilder.SendChannelList(sid);
    public void SelectChannel(int id) => PacketBuilder.SendChannelSelect(id);
}