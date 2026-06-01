using System.Collections.Generic;
using UnityEngine;
/// <summary>SoulUI — Linh hồn quái: số linh hồn đang có + các mục đổi pet/thưởng.</summary>
public class SoulUI : MonoBehaviour {
    public static SoulUI Instance;
    public readonly Dictionary<int,int> Souls = new();
    public class Exchange { public int id; public string name; public int soulId, cost; }
    public readonly List<Exchange> Exchanges = new();
    void Awake(){ if(!Instance) Instance=this; else Destroy(gameObject); }
    public void Open(){ gameObject.SetActive(true); }
    public void ClearSouls() => Souls.Clear();
    public void AddSoul(int soulId, int qty){ Souls[soulId] = qty; }
    public void ClearExchanges() => Exchanges.Clear();
    public void AddExchange(int id, string name, int soulId, int cost)
        => Exchanges.Add(new Exchange{ id=id, name=name, soulId=soulId, cost=cost });
    public void DoExchange(int id) => PacketBuilder.SendSoulExchange(id);
}
