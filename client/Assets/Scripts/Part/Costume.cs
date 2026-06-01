using System.Collections.Generic;
using UnityEngine;

namespace NexusIsekai.Part
{
    /// <summary>
    /// Doc costume JSON do PartComposer (studio, tab 'Part/Trang phuc') xuat ra:
    ///   { type:"costume", fps, frameCount, slots:[{name,cols,rows,frames,visible,offsets:[{f,dx,dy}]}] }
    /// va render nhan vat paper-doll: moi slot 1 SpriteRenderer, xep lop theo thu tu slot,
    /// cat frame tu sheet theo luoi, dat offset theo frame. Anh tung slot do caller cung cap
    /// (Dictionary slotName -> Texture2D, resolve tu KHO).
    /// </summary>
    public static class Costume
    {
        [System.Serializable] public class Data { public string type; public float fps = 10f; public int frameCount = 8; public Slot[] slots; }
        [System.Serializable] public class Slot { public string name; public int cols = 1, rows = 1, frames = 1; public bool visible = true; public Off[] offsets; }
        [System.Serializable] public class Off { public int f; public float dx, dy; }

        public static Data Parse(string json)
        {
            if (string.IsNullOrEmpty(json)) return null;
            try { var d = JsonUtility.FromJson<Data>(json); return (d != null && d.slots != null) ? d : null; }
            catch { return null; }
        }
    }

    public class CostumeRenderer : MonoBehaviour
    {
        public float pixelsPerUnit = 100f;
        public int sortingBase = 0;

        private class SlotRT { public Costume.Slot slot; public SpriteRenderer sr; public Sprite[] frameSprites; public Dictionary<int, Vector2> off; }
        private readonly List<SlotRT> rts = new List<SlotRT>();
        private Costume.Data data;
        private int frame; private float t; private bool playing = true;

        /// <summary>Gan costume + anh tung slot (slotName -> texture sheet).</summary>
        public void SetCostume(Costume.Data d, Dictionary<string, Texture2D> slotTextures)
        {
            Clear();
            data = d; frame = 0; t = 0f; playing = true;
            if (d == null || d.slots == null) return;
            float ppu = Mathf.Max(1f, pixelsPerUnit);
            int order = 0;
            foreach (var s in d.slots)
            {
                if (!s.visible) continue;
                Texture2D tex = null;
                if (slotTextures != null) slotTextures.TryGetValue(s.name, out tex);

                var go = new GameObject("slot_" + s.name);
                go.transform.SetParent(transform, false);
                var sr = go.AddComponent<SpriteRenderer>();
                sr.sortingOrder = sortingBase + order; order++;

                var rt = new SlotRT { slot = s, sr = sr, off = new Dictionary<int, Vector2>() };
                if (s.offsets != null) foreach (var o in s.offsets) rt.off[o.f] = new Vector2(o.dx, o.dy);

                if (tex != null)
                {
                    int fw = tex.width / Mathf.Max(1, s.cols), fh = tex.height / Mathf.Max(1, s.rows);
                    int fc = Mathf.Max(1, s.frames);
                    rt.frameSprites = new Sprite[fc];
                    for (int fi = 0; fi < fc; fi++)
                    {
                        int sx = (fi % Mathf.Max(1, s.cols)) * fw, syTop = (fi / Mathf.Max(1, s.cols)) * fh;
                        float ry = tex.height - (syTop + fh);
                        rt.frameSprites[fi] = Sprite.Create(tex, new Rect(sx, ry, fw, fh), new Vector2(0.5f, 0.5f), ppu);
                    }
                }
                rts.Add(rt);
            }
        }

        private void Apply(int globalFrame)
        {
            float ppu = Mathf.Max(1f, pixelsPerUnit);
            foreach (var rt in rts)
            {
                if (rt.frameSprites != null && rt.frameSprites.Length > 0)
                    rt.sr.sprite = rt.frameSprites[globalFrame % rt.frameSprites.Length];
                Vector2 o;
                if (!rt.off.TryGetValue(globalFrame, out o)) o = Vector2.zero;
                rt.sr.transform.localPosition = new Vector3(o.x / ppu, -o.y / ppu, 0f);
            }
        }

        private void Update()
        {
            if (data == null || rts.Count == 0) return;
            int fc = Mathf.Max(1, data.frameCount);
            Apply(frame % fc);
            if (playing && fc > 1)
            {
                t += Time.deltaTime * Mathf.Max(1f, data.fps);
                while (t >= 1f) { t -= 1f; frame = (frame + 1) % fc; }
            }
        }

        public void SetPlaying(bool p) { playing = p; }
        public void SetFrame(int f) { frame = Mathf.Max(0, f); }

        private void Clear()
        {
            for (int i = transform.childCount - 1; i >= 0; i--) Destroy(transform.GetChild(i).gameObject);
            foreach (var rt in rts) if (rt.frameSprites != null) foreach (var sp in rt.frameSprites) if (sp != null) Destroy(sp);
            rts.Clear();
        }

        private void OnDestroy() { Clear(); }
    }
}
