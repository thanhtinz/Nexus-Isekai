using UnityEngine;
public class WorldBossUI : MonoBehaviour {
    public static WorldBossUI Instance;
    bool isOpen;
    void Awake() { if (!Instance) Instance=this; else Destroy(gameObject); }
    public void Open() { isOpen=true; gameObject.SetActive(true); OnOpen(); }
    public void Close() { isOpen=false; gameObject.SetActive(false); }
    void OnOpen() { /* SendWorldBossInfo(); SendWorldBossJoin */ }

    public void ClearBosses(){}
    public void AddBoss(int id,string name,int mapId,long hp,long cur,int secs){}
    public void UpdateHp(int id,long hp){}
    public void OnBossDead(int id,string killer){}
    public void ClearRank(){}
    public void AddRank(string name,long dmg){}
    public void ShowRank(){}
}