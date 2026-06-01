using System.Collections;
using UnityEngine;

namespace NexusIsekai.Vfx
{
    /// <summary>
    /// Quan ly phat hieu ung tu vfx_config (JSON do studio xuat).
    ///
    /// Setup 1 lan: tao GameObject "VfxManager" trong scene → gan component nay →
    ///  - particlePrefab: prefab co ParticleSystem (+ ParticleSystemRenderer). VfxPlayer se tu them.
    ///  - additiveMaterial / alphaMaterial: 2 material hat (blend 'add' / 'normal').
    ///
    /// Dung: VfxManager.Instance.Play(skill.vfxConfig, targetWorldPos);
    /// (skill.vfxConfig = chuoi JSON lay tu skill_templates.vfx_config hoac item/effect.)
    /// </summary>
    public class VfxManager : MonoBehaviour
    {
        public static VfxManager Instance { get; private set; }

        public GameObject particlePrefab;
        public Material additiveMaterial;
        public Material alphaMaterial;
        public float defaultPixelsPerUnit = 100f;

        void Awake()
        {
            if (Instance != null && Instance != this) { Destroy(gameObject); return; }
            Instance = this;
        }

        /// <summary>Phat hieu ung tai vi tri the gioi. Tra ve GameObject de caller co the dung (vd aura đi theo).</summary>
        public GameObject Play(string vfxConfigJson, Vector3 worldPos)
        {
            if (particlePrefab == null) { Debug.LogWarning("[VFX] Chua gan particlePrefab cho VfxManager"); return null; }
            var cfg = VfxConfig.Parse(vfxConfigJson);
            var go = Instantiate(particlePrefab, worldPos, Quaternion.identity);

            var rend = go.GetComponent<ParticleSystemRenderer>();
            if (rend != null)
            {
                var m = cfg.blend == "add" ? additiveMaterial : alphaMaterial;
                if (m != null) rend.sharedMaterial = m;
            }

            var player = go.GetComponent<VfxPlayer>();
            if (player == null) player = go.AddComponent<VfxPlayer>();
            player.pixelsPerUnit = defaultPixelsPerUnit;
            player.Apply(cfg);

            if (!cfg.loop)
            {
                float life = (cfg.durationMs + cfg.lifeMax) / 1000f + 0.3f;
                StartCoroutine(KillAfter(go, life));
            }
            return go; // loop=true: caller tu Destroy khi ket thuc (vd het buff)
        }

        /// <summary>Phat roi tu huy sau ttlSeconds (dung cho hieu ung lap nhung gioi han thoi gian).</summary>
        public void PlayTimed(string vfxConfigJson, Vector3 worldPos, float ttlSeconds)
        {
            var go = Play(vfxConfigJson, worldPos);
            if (go != null) StartCoroutine(KillAfter(go, ttlSeconds));
        }

        IEnumerator KillAfter(GameObject go, float t)
        {
            yield return new WaitForSeconds(t);
            if (go != null) Destroy(go);
        }
    }
}
