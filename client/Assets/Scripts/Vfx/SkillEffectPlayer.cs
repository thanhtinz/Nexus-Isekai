using UnityEngine;

namespace NexusIsekai.Vfx
{
    /// <summary>
    /// Phat hieu ung skill (sheet luoi) tu S2C_SKILL_EFFECT: cat sheet cols×rows, play 'count' frame
    /// theo fps tai vi tri, phat sfx tai soundFrame. Mot lan roi tu huy.
    /// Anh effect: Resources/Sprites/Effects/{vfxKey}. Sfx: Resources/Audio/Sfx/{sfxKey}.
    /// worldScale/pixelsPerUnit chinh cho khop he toa do client (server gui x,y nhu khi dat mob/player).
    /// </summary>
    public class SkillEffectPlayer : MonoBehaviour
    {
        public static float worldScale = 1f;
        public static float pixelsPerUnit = 100f;

        private SpriteRenderer sr;
        private Texture2D tex;
        private Sprite[] frames;
        private int count, fps, soundFrame;
        private float t; private int frame;
        private AudioClip sfx; private bool sfxPlayed;

        public static void Spawn(string vfxKey, string sfxKey, float x, float y,
            int cols, int rows, int count, int fps, int ox, int oy, int scalePct, int soundFrame)
        {
            var tex = string.IsNullOrEmpty(vfxKey) ? null : Resources.Load<Texture2D>("Sprites/Effects/" + vfxKey);
            if (tex == null) return;
            var go = new GameObject("skillFx:" + vfxKey);
            float scale = Mathf.Max(0.01f, scalePct / 100f);
            go.transform.position = new Vector3(x * worldScale + ox / pixelsPerUnit, y * worldScale - oy / pixelsPerUnit, 0f);
            go.transform.localScale = Vector3.one * scale;
            var p = go.AddComponent<SkillEffectPlayer>();
            var clip = string.IsNullOrEmpty(sfxKey) ? null : Resources.Load<AudioClip>("Audio/Sfx/" + sfxKey);
            p.Init(tex, cols, rows, count, fps, soundFrame, clip);
        }

        private void Init(Texture2D t2, int cols, int rows, int n, int f, int sFrame, AudioClip clip)
        {
            tex = t2; count = Mathf.Max(1, n); fps = Mathf.Max(1, f); soundFrame = sFrame; sfx = clip;
            int c = Mathf.Max(1, cols), r = Mathf.Max(1, rows);
            sr = gameObject.AddComponent<SpriteRenderer>();
            tex.filterMode = FilterMode.Point;
            int fw = tex.width / c, fh = tex.height / r;
            frames = new Sprite[count];
            for (int i = 0; i < count; i++)
            {
                int sx = (i % c) * fw, syTop = (i / c) * fh;
                float ry = tex.height - (syTop + fh);
                frames[i] = Sprite.Create(tex, new Rect(sx, ry, fw, fh), new Vector2(0.5f, 0.5f), Mathf.Max(1f, pixelsPerUnit));
            }
            sr.sprite = frames[0];
        }

        private void Update()
        {
            if (frames == null) return;
            if (!sfxPlayed && sfx != null && soundFrame >= 0 && frame >= soundFrame)
            {
                AudioSource.PlayClipAtPoint(sfx, transform.position);
                sfxPlayed = true;
            }
            sr.sprite = frames[Mathf.Min(frame, frames.Length - 1)];
            t += Time.deltaTime * fps;
            while (t >= 1f)
            {
                t -= 1f; frame++;
                if (frame >= count) { Destroy(gameObject); return; }
            }
        }

        private void OnDestroy()
        {
            if (frames != null) foreach (var s in frames) if (s != null) Destroy(s);
        }
    }
}
