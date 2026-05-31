using UnityEngine;
public class ServerSelectScene : MonoBehaviour {
    void Start() { PacketBuilder.SendServerList(); }
    public void SelectServer(int id) { PacketBuilder.SendServerSelect(id); PacketBuilder.SendChannelList(id); }
    public void SelectChannel(int id) { PacketBuilder.SendChannelSelect(id); }
}