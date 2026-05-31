using UnityEngine;
public class SkillBar : MonoBehaviour {
    public static SkillBar Instance;
    [SerializeField] int maxSlots = 7;
    int[] skillIds = new int[7];
    float[] cooldowns = new float[7];
    void Awake() { if (!Instance) Instance=this; else Destroy(gameObject); }
    public void UseSkill(int slot) { if (cooldowns[slot] <= 0 && skillIds[slot] > 0) PacketBuilder.SendCastSkill(skillIds[slot], 0); }
    void Update() { for (int i=0;i<maxSlots;i++) if (cooldowns[i]>0) cooldowns[i]-=Time.deltaTime; }
}