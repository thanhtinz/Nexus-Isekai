import { useEffect, useRef, useState } from 'react';

/* ── Composite Preview (giong khung giua NSO TOOL) ───────────────────────
   Ghep 3 lop: MAP nen + NHAN VAT + HIEU UNG (sprite-sheet luoi cols×rows) → play dong bo.
   DATA-DRIVEN: neu skill dang chon co vfx_key + grid (vfx_cols/rows/frames/fps) + offset
   (vfx_ox/oy/scale) thi tu nap sheet hieu ung tu KHO + chay dung cau hinh skill.
   Frame marker: hit_frame (S - trung don) + sound_frame (A - am thanh) hien tren timeline,
   sua truc tiep -> ghi vao draft (Save o General de luu). Game doc cung field. */

const API_BASE = (import.meta as any).env?.VITE_API_BASE || '';
const assetUrl = (file?: string) => !file ? '' : (/^https?:/.test(file) ? file : (API_BASE || '') + '/' + file.replace(/^\//, ''));
const loadImg = (src: string) => new Promise<HTMLImageElement>((res, rej) => { const im = new Image(); im.onload = () => res(im); im.onerror = rej; im.src = src; });
const pickUrl = (f: File) => URL.createObjectURL(f);
const N = (v: any, d: number) => { const n = Number(v); return Number.isFinite(n) ? n : d; };

let assetCache: Record<string, string> | null = null;
async function resolveAsset(key: string): Promise<string> {
  if (!key) return '';
  if (!assetCache) {
    try {
      const r = await fetch(API_BASE + '/api/assets', { headers: { 'X-Admin-Key': localStorage.getItem('studio_key') || '' } });
      const j = await r.json(); assetCache = {};
      for (const a of (j.assets || [])) assetCache[a.asset_key] = a.file_path;
    } catch { assetCache = {}; }
  }
  return assetUrl(assetCache[key]);
}

export default function CompositePreview({ draft, setDraft }: { draft: any; setDraft: (d: any) => void }) {
  const cv = useRef<HTMLCanvasElement>(null);
  const [bg, setBg] = useState<HTMLImageElement | null>(null);
  const [chr, setChr] = useState<HTMLImageElement | null>(null);
  const [fx, setFx] = useState<HTMLImageElement | null>(null);
  const [fxName, setFxName] = useState('');

  const [cols, setCols] = useState(5);
  const [rows, setRows] = useState(1);
  const [count, setCount] = useState(5);
  const [fps, setFps] = useState(16);
  const [loop, setLoop] = useState(true);
  const [zoom, setZoom] = useState(2);
  const [chrX, setChrX] = useState(0), [chrY, setChrY] = useState(0), [chrScale, setChrScale] = useState(1);
  const [fxX, setFxX] = useState(40), [fxY, setFxY] = useState(-10), [fxScale, setFxScale] = useState(1);
  const [playing, setPlaying] = useState(true);
  const [frame, setFrame] = useState(0);

  const hitFrame = N(draft?.hit_frame, -1);
  const soundFrame = N(draft?.sound_frame, -1);

  // tu nap effect + cau hinh theo skill dang chon (khi doi vfx_key)
  useEffect(() => {
    const key = draft?.vfx_key; if (!key) return;
    let cancel = false;
    (async () => {
      const url = await resolveAsset(key); if (!url || cancel) return;
      try { const im = await loadImg(url); if (!cancel) { setFx(im); setFxName(key); } } catch { /* */ }
    })();
    setCols(N(draft.vfx_cols, 1)); setRows(N(draft.vfx_rows, 1));
    setCount(N(draft.vfx_frames, 1)); setFps(N(draft.vfx_fps, 16));
    setFxX(N(draft.vfx_ox, 40)); setFxY(N(draft.vfx_oy, -10));
    setFxScale(N(draft.vfx_scale, 100) / 100);
    return () => { cancel = true; };
  }, [draft?.vfx_key]); // eslint-disable-line

  const ref = useRef<any>({});
  ref.current = { bg, chr, fx, cols, rows, count, fps, loop, zoom, chrX, chrY, chrScale, fxX, fxY, fxScale, playing, frame, hitFrame, soundFrame };

  const onFile = (setter: (im: HTMLImageElement) => void) => async (e: React.ChangeEvent<HTMLInputElement>) => {
    const f = e.target.files?.[0]; if (!f) return; setter(await loadImg(pickUrl(f)));
  };

  useEffect(() => {
    const c = cv.current; if (!c) return; const ctx = c.getContext('2d')!;
    let raf = 0; let acc = 0; let last = performance.now();
    const draw = (now: number) => {
      const s = ref.current; const dt = now - last; last = now;
      if (s.playing && s.count > 0) {
        acc += (s.fps * dt) / 1000;
        while (acc >= 1) { acc -= 1; let nf = s.frame + 1; if (nf >= s.count) nf = s.loop ? 0 : s.count - 1; s.frame = nf; setFrame(nf); }
      }
      const W = c.width, H = c.height; ctx.imageSmoothingEnabled = false;
      ctx.clearRect(0, 0, W, H); ctx.fillStyle = '#0b0b12'; ctx.fillRect(0, 0, W, H);
      const z = s.zoom, cxw = W / 2, cyw = H / 2;
      if (s.bg) { const iw = s.bg.width * z, ih = s.bg.height * z; ctx.drawImage(s.bg, cxw - iw / 2, cyw - ih / 2, iw, ih); }
      if (s.chr) { const w = s.chr.width * z * s.chrScale, h = s.chr.height * z * s.chrScale; ctx.drawImage(s.chr, cxw - w / 2 + s.chrX * z, cyw - h / 2 + s.chrY * z, w, h); }
      if (s.fx && s.count > 0) {
        const fw = Math.floor(s.fx.width / Math.max(1, s.cols)), fh = Math.floor(s.fx.height / Math.max(1, s.rows));
        const idx = Math.min(s.frame, s.count - 1), sx = (idx % s.cols) * fw, sy = Math.floor(idx / s.cols) * fh;
        const dw = fw * z * s.fxScale, dh = fh * z * s.fxScale;
        ctx.drawImage(s.fx, sx, sy, fw, fh, cxw - dw / 2 + s.fxX * z, cyw - dh / 2 + s.fxY * z, dw, dh);
      }
      ctx.strokeStyle = 'rgba(255,255,255,.08)'; ctx.beginPath();
      ctx.moveTo(cxw, 0); ctx.lineTo(cxw, H); ctx.moveTo(0, cyw); ctx.lineTo(W, cyw); ctx.stroke();
      raf = requestAnimationFrame(draw);
    };
    raf = requestAnimationFrame(draw);
    return () => cancelAnimationFrame(raf);
  }, []);

  const scrub = (f: number) => { setFrame(f); ref.current.frame = f; setPlaying(false); };
  const setMarker = (field: 'hit_frame' | 'sound_frame') => setDraft({ ...draft, [field]: frame });

  const Num = ({ label, v, set, min, max, step = 1 }: { label: string; v: number; set: (n: number) => void; min: number; max: number; step?: number }) => (
    <label className="block"><div className="flex justify-between text-[10px] text-surface-200/60"><span>{label}</span><span className="text-surface-100">{v}</span></div>
      <input type="range" min={min} max={max} step={step} value={v} onChange={e => set(Number(e.target.value))} className="w-full accent-brand-500" /></label>
  );

  const hasSkillFx = !!draft?.vfx_key;

  return (
    <div className="w-full h-full flex gap-4">
      <div className="flex-1 flex flex-col items-center justify-center gap-3">
        <canvas ref={cv} width={520} height={400} className="rounded-xl border border-white/10" style={{ background: '#0b0b12' }} />
        {/* Timeline frame + marker S/A (giong NSO TOOL) */}
        {count > 0 && (
          <div className="flex flex-wrap gap-1 justify-center max-w-[520px]">
            {Array.from({ length: count }).map((_, i) => (
              <button key={i} onClick={() => scrub(i)} title={`Frame ${i + 1}`}
                className={`relative w-7 h-7 rounded text-[10px] flex items-center justify-center border ${
                  i === frame ? 'bg-brand-500/30 border-brand-400 text-brand-100' : 'bg-white/5 border-white/10 text-surface-200/60'}`}>
                {i + 1}
                {i === hitFrame && <span className="absolute -top-1.5 -right-1.5 bg-red-500 text-white text-[8px] px-1 rounded">S</span>}
                {i === soundFrame && <span className="absolute -bottom-1.5 -right-1.5 bg-purple-500 text-white text-[8px] px-1 rounded">A</span>}
              </button>
            ))}
          </div>
        )}
        <div className="flex items-center gap-2">
          <button onClick={() => setPlaying(p => !p)} className="btn-secondary !py-1.5 !px-3 text-xs">{playing ? 'Tam dung' : 'Chay'}</button>
          <span className="text-[11px] text-surface-200/60">Frame {frame + 1}/{count}</span>
          {draft && 'hit_frame' in draft && (
            <>
              <button onClick={() => setMarker('hit_frame')} className="badge bg-red-500/20 text-red-300">Dat frame trung (S)</button>
              <button onClick={() => setMarker('sound_frame')} className="badge bg-purple-500/20 text-purple-300">Dat frame am (A)</button>
            </>
          )}
        </div>
      </div>

      <div className="w-72 space-y-3 overflow-y-auto pr-1">
        {hasSkillFx
          ? <div className="text-[11px] px-3 py-2 rounded-lg bg-brand-500/10 text-brand-200 border border-brand-500/20">Hieu ung tu skill: <b>{fxName || draft.vfx_key}</b> (tu nap tu KHO). Sua grid/offset o General → Save.</div>
          : <div className="text-[11px] px-3 py-2 rounded-lg bg-white/5 text-surface-200/60">Skill nay chua co vfx_key. Dat vfx_key (key sheet trong KHO) + grid o General, hoac chon sheet thu cong duoi.</div>}
        <div className="space-y-1.5">
          <label className="block text-[11px] text-surface-200/70">Map nen<input type="file" accept="image/*" onChange={onFile(setBg)} className="block w-full text-[11px] mt-1" /></label>
          <label className="block text-[11px] text-surface-200/70">Nhan vat<input type="file" accept="image/*" onChange={onFile(setChr)} className="block w-full text-[11px] mt-1" /></label>
          <label className="block text-[11px] text-surface-200/70">Sheet hieu ung (thu cong)<input type="file" accept="image/*" onChange={onFile(im => { setFx(im); setFxName('(thu cong)'); })} className="block w-full text-[11px] mt-1" /></label>
        </div>
        <div className="grid grid-cols-3 gap-2">
          <label className="block text-[10px] text-surface-200/60">Cols<input type="number" value={cols} onChange={e => setCols(+e.target.value)} className="input !py-1 text-xs mt-0.5" /></label>
          <label className="block text-[10px] text-surface-200/60">Rows<input type="number" value={rows} onChange={e => setRows(+e.target.value)} className="input !py-1 text-xs mt-0.5" /></label>
          <label className="block text-[10px] text-surface-200/60">Frames<input type="number" value={count} onChange={e => setCount(+e.target.value)} className="input !py-1 text-xs mt-0.5" /></label>
        </div>
        <Num label="FPS" v={fps} set={setFps} min={1} max={60} />
        <Num label="Zoom" v={zoom} set={setZoom} min={1} max={6} step={0.5} />
        <div className="text-[10px] uppercase tracking-wider text-surface-200/50 pt-1">Vi tri hieu ung tren nhan vat</div>
        <Num label="FX X" v={fxX} set={setFxX} min={-120} max={120} />
        <Num label="FX Y" v={fxY} set={setFxY} min={-120} max={120} />
        <Num label="FX scale" v={fxScale} set={setFxScale} min={0.2} max={4} step={0.1} />
        <div className="text-[10px] uppercase tracking-wider text-surface-200/50 pt-1">Nhan vat</div>
        <Num label="Char X" v={chrX} set={setChrX} min={-120} max={120} />
        <Num label="Char Y" v={chrY} set={setChrY} min={-120} max={120} />
        <Num label="Char scale" v={chrScale} set={setChrScale} min={0.2} max={4} step={0.1} />
        <label className="flex items-center gap-2 text-[11px] text-surface-200/70 pt-1"><input type="checkbox" checked={loop} onChange={e => setLoop(e.target.checked)} className="accent-brand-500" />Lap hieu ung</label>
      </div>
    </div>
  );
}
