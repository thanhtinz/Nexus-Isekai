using UnityEngine;

namespace NexusIsekai.Vfx
{
    /// <summary>
    /// Ap VfxConfig (tu editor) vao 1 ParticleSystem cua Unity.
    /// Quy doi px → unit theo pixelsPerUnit. Gan vao GameObject co ParticleSystem.
    /// </summary>
    [RequireComponent(typeof(ParticleSystem))]
    public class VfxPlayer : MonoBehaviour
    {
        public float pixelsPerUnit = 100f;
        private ParticleSystem ps;

        void Awake() { ps = GetComponent<ParticleSystem>(); }

        public void Apply(VfxConfig c)
        {
            if (ps == null) ps = GetComponent<ParticleSystem>();
            ps.Stop(true, ParticleSystemStopBehavior.StopEmittingAndClear);
            float ppu = Mathf.Max(1f, pixelsPerUnit);

            var main = ps.main;
            main.loop = c.loop;
            main.duration = Mathf.Max(0.05f, c.durationMs / 1000f);
            main.startLifetime = new ParticleSystem.MinMaxCurve(c.lifeMin / 1000f, c.lifeMax / 1000f);
            main.startSpeed = new ParticleSystem.MinMaxCurve(c.speedMin / ppu, c.speedMax / ppu);
            main.startSize = new ParticleSystem.MinMaxCurve(Mathf.Max(0.001f, c.sizeStart / ppu));
            main.startColor = c.ColStart();
            main.maxParticles = Mathf.Max(1, c.maxParticles);
            main.gravityModifier = 0f; // dung forceOverLifetime thay cho gravity

            var emission = ps.emission;
            emission.enabled = true;
            emission.rateOverTime = c.emitRate;
            if (c.burst > 0)
                emission.SetBursts(new[] { new ParticleSystem.Burst(0f, (short)Mathf.Clamp(c.burst, 1, 10000)) });
            else
                emission.SetBursts(new ParticleSystem.Burst[0]);

            var shape = ps.shape;
            shape.enabled = true;
            if (c.shape == "circle")
            {
                shape.shapeType = ParticleSystemShapeType.Circle;
                shape.radius = Mathf.Max(0.001f, c.shapeSize / ppu);
            }
            else if (c.shape == "line")
            {
                shape.shapeType = ParticleSystemShapeType.SingleSidedEdge;
                shape.radius = Mathf.Max(0.001f, c.shapeSize / ppu);
            }
            else
            {
                shape.shapeType = ParticleSystemShapeType.Cone;
                shape.radius = 0.001f;
                shape.angle = Mathf.Clamp(c.spread / 2f, 0f, 89f);
            }
            // Huong phun: editor goc y huong xuong, Unity y huong len → bu (-angle-90 quanh truc Z).
            transform.localRotation = Quaternion.Euler(0f, 0f, -c.angle - 90f);

            var col = ps.colorOverLifetime;
            col.enabled = true;
            var grad = new Gradient();
            grad.SetKeys(
                new[] { new GradientColorKey(c.ColStart(), 0f), new GradientColorKey(c.ColEnd(), 1f) },
                new[] { new GradientAlphaKey(c.alphaStart, 0f), new GradientAlphaKey(c.alphaEnd, 1f) });
            col.color = new ParticleSystem.MinMaxGradient(grad);

            var sol = ps.sizeOverLifetime;
            sol.enabled = true;
            float s0 = Mathf.Max(0.001f, c.sizeStart);
            float s1 = Mathf.Max(0.001f, c.sizeEnd);
            // curve tuong doi voi startSize: 1 → s1/s0
            var curve = AnimationCurve.Linear(0f, 1f, 1f, s1 / s0);
            sol.size = new ParticleSystem.MinMaxCurve(1f, curve);

            var fol = ps.forceOverLifetime;
            fol.enabled = Mathf.Abs(c.gravity) > 0.001f;
            // editor gravity duong = keo xuong (y down) → Unity luc y am.
            fol.y = new ParticleSystem.MinMaxCurve(-c.gravity / ppu);

            ps.Play();
        }
    }
}
