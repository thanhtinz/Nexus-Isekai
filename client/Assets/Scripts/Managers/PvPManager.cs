using UnityEngine;
public class PvPManager : MonoBehaviour {
    public static PvPManager Instance;
    void Awake() { if (!Instance) Instance=this; else Destroy(gameObject); }
    public void LoadSeason() => PacketBuilder.SendPvpSeasonInfo();
    public void LoadRank(int page) => PacketBuilder.SendPvpSeasonRank(page);
    public void ClaimReward() => PacketBuilder.SendPvpSeasonReward();
}