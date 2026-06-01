using System.Collections.Generic;
using UnityEngine;
/// <summary>BossScheduleUI — Bảng giờ boss: list boss + đếm ngược spawn + nút dẫn đường.</summary>
public class BossScheduleUI : MonoBehaviour {
    public static BossScheduleUI Instance;
    public class BossRow { public int id; public string name; public int monsterId; public int mapId, x, y; public long nextSpawn; public bool alive; }
    public readonly List<BossRow> Bosses = new();
    void Awake(){ if(!Instance) Instance=this; else Destroy(gameObject); }
    public void Open(){ gameObject.SetActive(true); }
    public void Refresh() => PacketBuilder.SendBossSchedule();
    public void Clear() => Bosses.Clear();
    public void AddBoss(int id, string name, int monsterId, int mapId, int x, int y, long nextSpawn, bool alive)
        => Bosses.Add(new BossRow{ id=id, name=name, monsterId=monsterId, mapId=mapId, x=x, y=y, nextSpawn=nextSpawn, alive=alive });
    /// <summary>Dẫn đường tới boss (đổi map + di chuyển tới toạ độ).</summary>
    public void GoTo(BossRow b){ /* chuyển map mapId rồi tự di chuyển tới (x,y) */ }
}
