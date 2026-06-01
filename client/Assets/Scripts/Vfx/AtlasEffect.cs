using System.Collections.Generic;
using UnityEngine;

namespace NexusIsekai.Vfx
{
    /// <summary>
    /// Doc JSON do EffectComposer (studio, tab 'Effect atlas') xuat ra:
    ///   { type:"atlas-effect", fps, sprites:[{x,y,w,h}], frames:[{parts:[{sp,dx,dy,flip}]}] }
    /// va render bang cac SpriteRenderer con (1 part = 1 renderer), play theo fps.
    /// Toa do sprite trong editor goc TREN-TRAI; Unity goc DUOI-TRAI nen co bu y.
    /// </summary>
    public static class AtlasEffect
    {
        [System.Serializable] public class Data { public string type; public float fps = 16f; public SpriteRect[] sprites; public Frame[] frames; }
        [System.Serializable] public class SpriteRect { public int x, y, w, h; }
        [System.Serializable] public class Frame { public Part[] parts; }
        [System.Serializable] public class Part { public int sp; public float dx, dy; public bool flip; }

        public static Data Parse(string json)
        {
            if (string.IsNullOrEmpty(json)) return null;
            try { var d = JsonUtility.FromJson<Data>(json); return (d != null && d.frames != null) ? d : null; }
            catch { return null; }
        }
    }

    public class AtlasEffectPlayer : MonoBehaviour
    {
        public float pixelsPerUnit = 100f;
        public string sortingLayer = "Default";
        public int sortingBase = 100;

        private AtlasEffect.Data data;
        private Texture2D atlas;
        private Sprite[] sprites;
        private readonly List<SpriteRenderer> pool = new List<SpriteRenderer>();
        private float t; private int frame; private bool playing; private bool loop;
        public System.Action onFinished;

        /// <summary>Khoi tao + bat dau phat. atlas = texture chua cac sprite (tu KHO).</summary>
        public void Init(AtlasEffect.Data d, Texture2D atlasTex, bool loopFlag)
        {
            data = d; atlas = atlasTex; loop = loopFlag; frame = 0; t = 0f; playing = true;
            int n = (d != null && d.sprites != null) ? d.sprites.Length : 0;
            sprites = new Sprite[n];
            for (int i = 0; i < n; i++) sprites[i] = MakeSprite(d.sprites[i]);
            if (data != null && data.frames != null && data.frames.Length > 0) Render(0);
        }

        private Sprite MakeSprite(AtlasEffect.SpriteRect r)
        {
            if (atlas == null || r.w <= 0 || r.h <= 0) return null;
            float ry = atlas.height - (r.y + r.h); // top-left -> bottom-left
            return Sprite.Create(atlas, new Rect(r.x, ry, r.w, r.h), new Vector2(0.5f, 0.5f), Mathf.Max(1f, pixelsPerUnit));
        }

        private void EnsurePool(int n)
        {
            while (pool.Count < n)
            {
                var go = new GameObject("part" + pool.Count);
                go.transform.SetParent(transform, false);
                var sr = go.AddComponent<SpriteRenderer>();
                sr.sortingLayerName = sortingLayer;
                pool.Add(sr);
            }
        }

        private void Render(int fi)
        {
            var fr = (data.frames != null && fi >= 0 && fi < data.frames.Length) ? data.frames[fi] : null;
            int n = (fr != null && fr.parts != null) ? fr.parts.Length : 0;
            EnsurePool(n);
            float ppu = Mathf.Max(1f, pixelsPerUnit);
            for (int i = 0; i < pool.Count; i++)
            {
                var sr = pool[i];
                if (i < n)
                {
                    var p = fr.parts[i];
                    sr.enabled = true;
                    sr.sprite = (sprites != null && p.sp >= 0 && p.sp < sprites.Length) ? sprites[p.sp] : null;
                    sr.flipX = p.flip;
                    sr.sortingOrder = sortingBase + i;
                    sr.transform.localPosition = new Vector3(p.dx / ppu, -p.dy / ppu, 0f);
                }
                else sr.enabled = false;
            }
        }

        private void Update()
        {
            if (!playing || data == null || data.frames == null || data.frames.Length == 0) return;
            Render(Mathf.Min(frame, data.frames.Length - 1));
            t += Time.deltaTime * Mathf.Max(1f, data.fps);
            while (t >= 1f)
            {
                t -= 1f; frame++;
                if (frame >= data.frames.Length)
                {
                    if (loop) frame = 0;
                    else { playing = false; if (onFinished != null) onFinished(); break; }
                }
            }
        }

        private void OnDestroy()
        {
            if (sprites != null) foreach (var s in sprites) if (s != null) Destroy(s);
        }
    }
}
