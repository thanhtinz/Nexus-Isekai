import { useEffect, useRef, useState } from 'react';

/* ── Effect Composer (giong tab HIEU UNG cua HTTH/NRO) ───────────────────
   Effect = 1 ATLAS (sprite sheet) + danh sach SPRITE (o cat: x,y,w,h) + danh sach FRAME,
   moi frame ghep NHIEU PART (sprite + dx,dy + flip + thu tu lop). Day la chuan effect NRO/Avatar
   thuc su (manh hon grid-sheet vi sprite tai su dung, dat tu do, lat, xep lop).
   Play theo fps → export JSON { fps, sprites, frames } gan vao vfx_config + tai file. */

type Sprite = { x: number; y: number; w: number; h: number };
type Part = { sp: number; dx: number; dy: number; flip: boolean };
type Frame = { parts: Part[] };

const loadImg = (src: string) => new Promise<HTMLImageElement>((res, rej) => { const im = new Image(); im.onload = () => res(im); im.onerror = rej; im.src = src; });

export default function EffectComposer({ draft, setDraft, setMsg }: { draft: any; setDraft: (d: any) => void; setMsg: (s: string) => void }) {
  const cv = useRef<HTMLCanvasElement>(null);
  const [atlas, setAtlas] = useState<HTMLImageElement | null>(null);
  const [sprites, setSprites] = useState<Sprite[]>([]);
  const [frames, setFrames] = useState<Frame[]>([{ parts: [] }]);
  const [cur, setCur] = useState(0);          // frame hien tai
  const [selSp, setSelSp] = useState(0);       // sprite dang chon (de them part)
  const [selPart, setSelPart] = useState(-1);  // part dang chon trong frame
  const [fps, setFps] = useState(16);
  const [zoom, setZoom] = useState(2);
  const [playing, setPlaying] = useState(false);
  const [gridC, setGridC] = useState(4), [gridR, setGridR] = useState(4);

  const ref = useRef<any>({});
  ref.current = { atlas, sprites, frames, cur, fps, zoom, playing };

  // nap tu draft.vfx_config neu la dang effect atlas
  useEffect(() => {
    const raw = draft?.vfx_config || draft?.config_json;
    if (!raw) return;
    try { const o = JSON.parse(raw); if (Array.isArray(o.sprites) && Array.isArray(o.frames)) { setSprites(o.sprites); setFrames(o.frames.length ? o.frames : [{ parts: [] }]); setFps(o.fps || 16); } } catch { /* */ }
  }, []); // eslint-disable-line

  const loadAtlas = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const f = e.target.files?.[0]; if (!f) return; setAtlas(await loadImg(URL.createObjectURL(f)));
  };

  const autoGrid = () => {
    if (!atlas) { setMsg('Nap atlas truoc'); return; }
    const fw = Math.floor(atlas.width / Math.max(1, gridC)), fh = Math.floor(atlas.height / Math.max(1, gridR));
    const sp: Sprite[] = [];
    for (let r = 0; r < gridR; r++) for (let c = 0; c < gridC; c++) sp.push({ x: c * fw, y: r * fh, w: fw, h: fh });
    setSprites(sp); setMsg(`Tao ${sp.length} sprite tu luoi ${gridC}x${gridR}`);
  };
  const addSprite = () => setSprites(s => [...s, { x: 0, y: 0, w: 32, h: 32 }]);
  const editSprite = (i: number, k: keyof Sprite, v: number) => setSprites(s => s.map((sp, j) => j === i ? { ...sp, [k]: v } : sp));

  // frame ops
  const setFrame = (fn: (f: Frame) => Frame) => setFrames(fs => fs.map((f, i) => i === cur ? fn(f) : f));
  const addFrame = () => { setFrames(fs => [...fs.slice(0, cur + 1), { parts: [] }, ...fs.slice(cur + 1)]); setCur(c => c + 1); };
  const dupFrame = () => { setFrames(fs => [...fs.slice(0, cur + 1), { parts: fs[cur].parts.map(p => ({ ...p })) }, ...fs.slice(cur + 1)]); setCur(c => c + 1); };
  const delFrame = () => { if (frames.length <= 1) return; setFrames(fs => fs.filter((_, i) => i !== cur)); setCur(c => Math.max(0, Math.min(c, frames.length - 2))); };

  // part ops (tren frame hien tai)
  const addPart = () => { if (!sprites.length) { setMsg('Chua co sprite'); return; } setFrame(f => ({ parts: [...f.parts, { sp: selSp, dx: 0, dy: 0, flip: false }] })); setSelPart(frames[cur].parts.length); };
  const editPart = (i: number, k: keyof Part, v: any) => setFrame(f => ({ parts: f.parts.map((p, j) => j === i ? { ...p, [k]: v } : p) }));
  const delPart = (i: number) => setFrame(f => ({ parts: f.parts.filter((_, j) => j !== i) }));
  const movePart = (i: number, d: number) => setFrame(f => { const a = [...f.parts]; const j = i + d; if (j < 0 || j >= a.length) return f; [a[i], a[j]] = [a[j], a[i]]; return { parts: a }; });

  useEffect(() => {
    const c = cv.current; if (!c) return; const ctx = c.getContext('2d')!;
    let raf = 0, acc = 0, last = performance.now(), local = ref.current.cur;
    const draw = (now: number) => {
      const s = ref.current; const dt = now - last; last = now;
      if (s.playing && s.frames.length > 1) { acc += (s.fps * dt) / 1000; while (acc >= 1) { acc -= 1; local = (local + 1) % s.frames.length; setCur(local); } }
      else local = s.cur;
      const W = c.width, H = c.height, cx = W / 2, cy = H / 2, z = s.zoom;
      ctx.imageSmoothingEnabled = false; ctx.clearRect(0, 0, W, H); ctx.fillStyle = '#0b0b12'; ctx.fillRect(0, 0, W, H);
      ctx.strokeStyle = 'rgba(255,255,255,.08)'; ctx.beginPath(); ctx.moveTo(cx, 0); ctx.lineTo(cx, H); ctx.moveTo(0, cy); ctx.lineTo(W, cy); ctx.stroke();
      const fr = s.frames[Math.min(local, s.frames.length - 1)];
      if (s.atlas && fr) for (const p of fr.parts) {
        const sp = s.sprites[p.sp]; if (!sp) continue;
        const dw = sp.w * z, dh = sp.h * z, x = cx + p.dx * z, y = cy + p.dy * z;
        ctx.save();
        if (p.flip) { ctx.translate(x, y); ctx.scale(-1, 1); ctx.drawImage(s.atlas, sp.x, sp.y, sp.w, sp.h, -dw / 2, -dh / 2, dw, dh); }
        else ctx.drawImage(s.atlas, sp.x, sp.y, sp.w, sp.h, x - dw / 2, y - dh / 2, dw, dh);
        ctx.restore();
      }
      raf = requestAnimationFrame(draw);
    };
    raf = requestAnimationFrame(draw);
    return () => cancelAnimationFrame(raf);
  }, []);

  const exportJson = () => {
    const json = JSON.stringify({ type: 'atlas-effect', fps, sprites, frames });
    const key = 'vfx_config' in draft ? 'vfx_config' : ('config_json' in draft ? 'config_json' : 'vfx_config');
    setDraft({ ...draft, [key]: json });
    setMsg(`Da gan effect (${frames.length} frame, ${sprites.length} sprite) vao ${key} — Save o General de luu`);
  };
  const download = () => {
    const blob = new Blob([JSON.stringify({ type: 'atlas-effect', fps, sprites, frames }, null, 2)], { type: 'application/json' });
    const a = document.createElement('a'); a.href = URL.createObjectURL(blob); a.download = 'effect.json'; a.click();
  };

  const frame = frames[cur] || { parts: [] };

  return (
    <div className="w-full h-full flex gap-3 text-xs">
      {/* FRAMES */}
      <div className="w-28 flex flex-col">
        <div className="text-[10px] uppercase tracking-wider text-surface-200/50 mb-1">Frames ({frames.length})</div>
        <div className="flex-1 overflow-y-auto space-y-1">
          {frames.map((f, i) => (
            <button key={i} onClick={() => { setCur(i); setPlaying(false); }} className={`w-full px-2 py-1.5 rounded text-left ${i === cur ? 'bg-brand-500/25 text-brand-100' : 'bg-white/5 text-surface-200/60'}`}>
              F{i} <span className="text-surface-200/40">· {f.parts.length}p</span>
            </button>
          ))}
        </div>
        <div className="flex gap-1 mt-1">
          <button onClick={addFrame} className="badge bg-white/5 flex-1">+F</button>
          <button onClick={dupFrame} className="badge bg-white/5 flex-1">Dup</button>
          <button onClick={delFrame} className="badge bg-red-500/15 text-red-300 flex-1">X</button>
        </div>
      </div>

      {/* PREVIEW */}
      <div className="flex-1 flex flex-col items-center justify-center gap-2 min-w-0">
        <canvas ref={cv} width={460} height={400} className="rounded-xl border border-white/10" style={{ background: '#0b0b12' }} />
        <div className="flex items-center gap-2">
          <button onClick={() => setPlaying(p => !p)} className="btn-secondary !py-1.5 !px-3">{playing ? 'Tam dung' : 'Phat'}</button>
          <button onClick={() => setCur(c => Math.max(0, c - 1))} className="badge bg-white/5">◀</button>
          <span className="text-surface-200/60">F{cur}/{frames.length - 1}</span>
          <button onClick={() => setCur(c => Math.min(frames.length - 1, c + 1))} className="badge bg-white/5">▶</button>
          <label className="flex items-center gap-1 text-surface-200/60">fps<input type="number" value={fps} onChange={e => setFps(+e.target.value)} className="input !py-0.5 !w-14" /></label>
          <label className="flex items-center gap-1 text-surface-200/60">zoom<input type="range" min={1} max={6} step={0.5} value={zoom} onChange={e => setZoom(+e.target.value)} className="accent-brand-500 w-20" /></label>
        </div>
        <div className="flex gap-2">
          <button onClick={exportJson} className="btn-gold !py-1.5 !px-3">Xuat → vfx_config</button>
          <button onClick={download} className="btn-secondary !py-1.5 !px-3">Tai JSON</button>
        </div>
      </div>

      {/* SPRITES + PARTS */}
      <div className="w-72 overflow-y-auto space-y-3 pr-1">
        <div>
          <label className="block text-[11px] text-surface-200/70">Atlas (sprite sheet)<input type="file" accept="image/*" onChange={loadAtlas} className="block w-full text-[11px] mt-1" /></label>
          <div className="flex items-end gap-1 mt-1">
            <label className="text-[10px] text-surface-200/60">Cols<input type="number" value={gridC} onChange={e => setGridC(+e.target.value)} className="input !py-0.5 !w-12" /></label>
            <label className="text-[10px] text-surface-200/60">Rows<input type="number" value={gridR} onChange={e => setGridR(+e.target.value)} className="input !py-0.5 !w-12" /></label>
            <button onClick={autoGrid} className="badge bg-white/5 flex-1">Auto luoi</button>
            <button onClick={addSprite} className="badge bg-white/5">+rect</button>
          </div>
        </div>

        <div>
          <div className="text-[10px] uppercase tracking-wider text-surface-200/50 mb-1">Sprites ({sprites.length}) — chon de them part</div>
          <div className="max-h-32 overflow-y-auto space-y-1">
            {sprites.map((sp, i) => (
              <div key={i} onClick={() => setSelSp(i)} className={`px-2 py-1 rounded cursor-pointer ${i === selSp ? 'bg-brand-500/25 text-brand-100' : 'bg-white/5 text-surface-200/60'}`}>
                <div className="flex justify-between"><span>#{i}</span>
                  <span className="flex gap-1">
                    {(['x', 'y', 'w', 'h'] as const).map(k => (
                      <input key={k} type="number" value={sp[k]} onClick={e => e.stopPropagation()} onChange={e => editSprite(i, k, +e.target.value)} className="input !py-0 !px-1 !w-11 text-[10px]" title={k} />
                    ))}
                  </span>
                </div>
              </div>
            ))}
          </div>
        </div>

        <div>
          <div className="flex items-center justify-between mb-1">
            <div className="text-[10px] uppercase tracking-wider text-surface-200/50">Parts cua F{cur} ({frame.parts.length})</div>
            <button onClick={addPart} className="badge bg-emerald-500/20 text-emerald-300">+ part (sp#{selSp})</button>
          </div>
          <div className="space-y-1">
            {frame.parts.map((p, i) => (
              <div key={i} onClick={() => setSelPart(i)} className={`px-2 py-1 rounded ${i === selPart ? 'bg-brand-500/15' : 'bg-white/5'}`}>
                <div className="flex items-center gap-1">
                  <span className="text-surface-200/60 w-12">sp#{p.sp}</span>
                  <label className="text-[10px] text-surface-200/50">dx<input type="number" value={p.dx} onChange={e => editPart(i, 'dx', +e.target.value)} className="input !py-0 !px-1 !w-12 text-[10px]" /></label>
                  <label className="text-[10px] text-surface-200/50">dy<input type="number" value={p.dy} onChange={e => editPart(i, 'dy', +e.target.value)} className="input !py-0 !px-1 !w-12 text-[10px]" /></label>
                  <button onClick={() => editPart(i, 'flip', !p.flip)} className={`badge ${p.flip ? 'bg-brand-500/30 text-brand-100' : 'bg-white/5 text-surface-200/50'}`}>↔</button>
                </div>
                <div className="flex gap-1 mt-1">
                  <button onClick={() => movePart(i, -1)} className="badge bg-white/5 flex-1">▲ lop</button>
                  <button onClick={() => movePart(i, 1)} className="badge bg-white/5 flex-1">▼ lop</button>
                  <button onClick={() => delPart(i)} className="badge bg-red-500/15 text-red-300">X</button>
                </div>
              </div>
            ))}
            {frame.parts.length === 0 && <div className="text-[10px] text-surface-200/40 px-1">Chon 1 sprite o tren roi bam "+ part".</div>}
          </div>
        </div>
      </div>
    </div>
  );
}
