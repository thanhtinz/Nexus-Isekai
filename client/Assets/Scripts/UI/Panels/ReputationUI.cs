using UnityEngine;
/// <summary>ReputationUI — Danh vọng/Phe phái. Hiển thị faction + thanh rep + mốc tier.</summary>
public class ReputationUI : MonoBehaviour {
    public static ReputationUI Instance;
    void Awake(){ if(!Instance) Instance=this; else Destroy(gameObject); }
    public void Open(){ gameObject.SetActive(true); PacketBuilder.SendReputationList(); }
    public void Clear(){ /* xoá list faction */ }
    public void AddFaction(int id, string name, string desc, int rep, int maxRep, int tier){ /* thêm dòng faction */ }
    public void ClaimTier(int factionId, int tier) => PacketBuilder.SendReputationClaim(factionId, tier);
}
