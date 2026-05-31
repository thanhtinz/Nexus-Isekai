using UnityEngine;
public class SpineCharacterLoader : MonoBehaviour {
    public static SpineCharacterLoader Instance;
    void Awake() { if (!Instance) Instance=this; else Destroy(gameObject); }
    public void LoadNPC(string npcId, Transform parent) {
        var data = Resources.Load<Spine.Unity.SkeletonDataAsset>($"Sprites/Characters/NPCs/npc_{npcId}/sbody_{npcId}");
        if (data != null) { /* Instantiate Spine skeleton */ }
    }
}