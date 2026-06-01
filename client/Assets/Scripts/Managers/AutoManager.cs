using System.Collections.Generic;
using UnityEngine;
/// <summary>
/// AutoManager — chạy vòng auto phía client (đánh quái gần nhất, dùng máu/mana, skill).
/// Server cấp cờ được phép theo VIP; mỗi hành động vẫn gửi packet bình thường để server
/// xác thực (không bỏ qua kiểm tra tầm/cooldown → chống lạm dụng). Nhặt đồ/auto bán/auto
/// nhiệm vụ: team mở rộng thêm khi có API tương ứng (đánh dấu TODO).
/// </summary>
public class AutoManager : MonoBehaviour {
    public static AutoManager Instance;
    // bit cờ: attack=1, pickup=2, potion=4, skill=8, quest=16, sell=32
    public int EnabledFlags = 0;
    public int AllowedFlags = 0;
    public class AutoType { public string type; public string name; public int minVip; public int maxMinutes; public bool eligible; }
    public readonly List<AutoType> Types = new();
    public int HpPotionItemId = 0;   // gán theo bình máu trong túi
    public int MpPotionItemId = 0;
    float _tick;

    void Awake(){ if(!Instance) Instance=this; else Destroy(gameObject); }

    public void ClearConfig() => Types.Clear();
    public void AddAutoType(string type, string name, int minVip, int maxMinutes, bool eligible)
        => Types.Add(new AutoType{ type=type, name=name, minVip=minVip, maxMinutes=maxMinutes, eligible=eligible });
    public void OnConfigReady(){ /* UI vẽ menu auto theo Types */ }
    public void SetAllowedFlags(int allowed){ AllowedFlags = allowed; EnabledFlags &= allowed; }

    /// <summary>Người chơi bật/tắt auto → gửi server xin phép (server lọc theo VIP).</summary>
    public void RequestAuto(int flags){ EnabledFlags = flags; PacketBuilder.SendAutoSet(flags); }
    public void Toggle(int bit){ RequestAuto(EnabledFlags ^ bit); }

    void Update(){
        if(EnabledFlags == 0) return;
        _tick += Time.deltaTime;
        if(_tick < 0.5f) return;     // 2 nhịp/giây
        _tick = 0;
        if((EnabledFlags & 4) != 0) AutoPotion();
        if((EnabledFlags & 1) != 0) AutoAttack();
        // pickup(2)/quest(16)/sell(32): team bổ sung khi có API tương ứng
    }

    void AutoAttack(){
        var gs = GameState.Instance; if(gs == null) return;
        var mob = MonsterManager.Instance?.GetNearest(gs.PlayerPos);
        if(mob == null) return;
        if((EnabledFlags & 8) != 0) PacketBuilder.SendUseSkill(0, mob.MonsterId); // dùng skill slot 0
        else PacketBuilder.SendAttack(mob.MonsterId);
    }
    void AutoPotion(){
        var gs = GameState.Instance; if(gs == null) return;
        if(gs.HpPercent < 50 && HpPotionItemId > 0) PacketBuilder.SendUseItem(HpPotionItemId);
        if(gs.MpPercent < 30 && MpPotionItemId > 0) PacketBuilder.SendUseItem(MpPotionItemId);
    }
}
