using UnityEngine;
public class CombatManager : MonoBehaviour {
    public static CombatManager Instance;
    void Awake() { if (!Instance) Instance=this; else Destroy(gameObject); }
    public void Attack(long targetId) { PacketBuilder.SendAttack(targetId); AudioManager.Instance?.PlaySFX("sfx_hit"); }
    public void CastSkill(int skillId, long targetId) { PacketBuilder.SendCastSkill(skillId, targetId); }
}