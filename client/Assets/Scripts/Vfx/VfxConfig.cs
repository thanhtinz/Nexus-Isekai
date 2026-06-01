using UnityEngine;

namespace NexusIsekai.Vfx
{
    /// <summary>
    /// Khop 1-1 voi JSON do VFX Editor (studio) xuat ra (truong vfx_config).
    /// JsonUtility map theo TEN truong — dung y het editor.
    /// </summary>
    [System.Serializable]
    public class VfxConfig
    {
        public float emitRate = 60f;
        public int maxParticles = 400;
        public float lifeMin = 500f, lifeMax = 1000f;   // ms
        public float speedMin = 40f, speedMax = 120f;   // px/s
        public float angle = -90f, spread = 360f, gravity = 60f;
        public float sizeStart = 10f, sizeEnd = 0f;     // px
        public string colorStart = "#ffd24d", colorEnd = "#ff5a3c";
        public float alphaStart = 1f, alphaEnd = 0f;
        public string blend = "add";                     // add | normal
        public string shape = "point";                   // point | circle | line
        public float shapeSize = 8f;
        public int burst = 0;
        public bool loop = true;
        public float durationMs = 1200f;

        public static VfxConfig Parse(string json)
        {
            if (string.IsNullOrEmpty(json)) return new VfxConfig();
            try { return JsonUtility.FromJson<VfxConfig>(json) ?? new VfxConfig(); }
            catch { return new VfxConfig(); }
        }

        public Color ColStart() { return Hex(colorStart, alphaStart); }
        public Color ColEnd()   { return Hex(colorEnd, alphaEnd); }

        static Color Hex(string h, float a)
        {
            Color c;
            if (!string.IsNullOrEmpty(h) && ColorUtility.TryParseHtmlString(h, out c)) { c.a = a; return c; }
            return new Color(1f, 1f, 1f, a);
        }
    }
}
