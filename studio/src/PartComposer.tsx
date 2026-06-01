import { useEffect, useRef, useState } from 'react';

/* ── Part Composer (paper-doll, giong tab PART cua HTTH/NRO/Avatar) ──────
   Nhan vat = nhieu LOP PART (Cloak/Body/Leg/Head/Hair/Hat/Weapon), moi part 1 sheet anh
   (luoi frame) + offset dx,dy theo TUNG FRAME. Cac lop xep theo thu tu (z-order) va play
   dong bo theo frame → ra dang nhan vat dong. Export costume JSON gan vao draft + tai file.
   Sprite-sheet 2D, khong dung asset thuong mai. */

type Slot = {
  name: string;
  img: HTMLImageElement | null;
  cols: number; rows: number; frames: number;
  offsets: Record<number, [number, number]>; // dx,dy theo frame index
  visible: boolean;
};

const DEFAULT_SLOTS = ['Cloak', 'Leg', 'Body', 'Head', 'Hair', 'Hat', 'Weapon']; // sau -> truoc
const loadImg = (src: string) => new Promise<HTMLImageElement>((res, rej) => { const im = new Image(); im.onload = () => res(im); im.onerror = rej; im.src = src; });

export default function PartComposer({ draft, setDraft, setMsg }: { draft: any; setDraft: (d: any) => void; setMsg: (s: string) => void }) {
  const cv = useRef<HTMLCanvasElement>(null);
  const [slots, setSlots] = useState<Slot[]>(() => DEFAULT_SLOTS.map(n => ({ name: n, img: null, cols: 1, rows: 1, frames: 1, offsets: {}, visible: true })));
  const [sel, setSel] = useState(2); // Body
  const [frameCount, setFrameCount] = useState(8);
  const [cur, setCur] = useState(0);
  const [fps, setFps] = useState(10);
  const [zoom, setZoom] = useState(3);
  const [flip, setFlip] = useState(false);
  const [playing, setPlaying] = useState(false);

  const ref = useRef<any>({}); ref.current = { slots, cur, fps, zoom, flip, playing, frameCount };

  const upd = (i: number, fn: (s: Slot) => Slot) => setSlots(ss => ss.map((s, j) => j === i ? fn(s) : s));
  const loadSheet = (i: number) => async (e: React.ChangeEvent<HTMLInputElement>) => {
    const f = e.target.files?.[0]; if (!f) return; const im = await loadImg(URL.createObjectURL(f));
    upd(i, s => ({ ...s, img: im }));
  };
  const move = (i: number, d: number) => setSlots(ss => { const a = [...ss]; const j = i + d; if (j < 0 || j >= a.length) return ss; [a[i], a[j]] = [a[j], a[i]]; return a; });

  // edit offset cua slot dang chon tai frame hien tai
  const curOff = (s: Slot): [number, number] => s.offsets[cur] || [0, 0];
  const setOff = (k: 0 | 1, v: number) => upd(sel, s => ({ ...s, offsets: { ...s.offsets, [cur]: (() => { const o = (s.offsets[cur] || [0, 0]).slice() as [number, number]; o[k] = v; return o; })() } }));

  useEffect(() => {
    const c = cv.current; if (!c) return; const ctx = c.getContext('2d')!;
    let raf = 0, acc = 0, last = performance.now(), local = 0;
    const draw = (now: number) => {
      const st = ref.current; const dt = now - last; last = now;
      if (st.playing && st.frameCount > 1) { acc += (st.fps * dt) / 1000; while (acc >= 1) { acc -= 1; local = (local + 1) % st.frameCount; setCur(local); } }
      else local = st.cur;
      const W = c.width, H = c.height, cx = W / 2, cy = H / 2, z = st.zoom;
      ctx.imageSmoothingEnabled = false; ctx.clearRect(0, 0, W, H); ctx.fillStyle = '#0b0b12'; ctx.fillRect(0, 0, W, H);
      ctx.strokeStyle = 'rgba(255,255,255,.08)'; ctx.beginPath(); ctx.moveTo(cx, 0); ctx.lineTo(cx, H); ctx.moveTo(0, cy); ctx.lineTo(W, cy); ctx.stroke();
      for (const s of st.slots) {
        if (!s.visible || !s.img) continue;
        const fw = Math.floor(s.img.width / Math.max(1, s.cols)), fh = Math.floor(s.img.height / Math.max(1, s.rows));
        const fi = s.frames > 0 ? local % s.frames : 0;
        const sx = (fi % s.cols) * fw, sy = Math.floor(fi / s.cols) * fh;
        const off = s.offsets[local] || [0, 0];
        const dw = fw * z, dh = fh * z, x = cx + off[0] * z, y = cy + off[1] * z;
        ctx.save();
        if (st.flip) { ctx.translate(x, y); ctx.scale(-1, 1); ctx.drawImage(s.img, sx, sy, fw, fh, -dw / 2, -dh / 2, dw, dh); }
        else ctx.drawImage(s.img, sx, sy, fw, fh, x - dw / 2, y - dh / 2, dw, dh);
        ctx.restore();
      }
      raf = requestAnimationFrame(draw);
    };
    raf = requestAnimationFrame(draw);
    return () => cancelAnimationFrame(raf);
  }, []);

  const buildJson = () => ({
    type: 'costume', fps, frameCount,
    slots: slots.map(s => ({ name: s.name, cols: s.cols, rows: s.rows, frames: s.frames, offsets: s.offsets, visible: s.visible })),
  });
  const exportJson = () => {
    const key = 'costume_json' in draft ? 'costume_json' : ('config_json' in draft ? 'config_json' : 'costume_json');
    setDraft({ ...draft, [key]: JSON.stringify(buildJson()) });
    setMsg(`Da gan costume (${slots.filter(s => s.img).length}/${slots.length} lop) vao ${key} — Save o General. (Anh tung lop luu rieng vao KHO.)`);
  };
  const download = () => {
    const blob = new Blob([JSON.stringify(buildJson(), null, 2)], { type: 'application/json' });
    const a = document.createElement('a'); a.href = URL.createObjectURL(blob); a.download = 'costume.json'; a.click();
  };

  const s = slots[sel]; const off = curOff(s);

  return (
    <div className="w-full h-full flex gap-3 text-xs">
      {/* LOP (z-order: tren = sau, duoi = truoc) */}
      <div className="w-36 flex flex-col">
        <div className="text-[10px] uppercase tracking-wider text-surface-200/50 mb-1">Lop (sau → truoc)</div>
        <div className="flex-1 overflow-y-auto space-y-1">
          {slots.map((sl, i) => (
            <div key={i} className={`px-2 py-1.5 rounded ${i === sel ? 'bg-brand-500/25' : 'bg-white/5'}`}>
              <div className="flex items-center justify-between">
                <button onClick={() => setSel(i)} className={`flex-1 text-left ${i === sel ? 'text-brand-100' : 'text-surface-200/70'}`}>
                  {sl.name}{sl.img ? '' : ' ·'}
                </button>
                <button onClick={() => upd(i, x => ({ ...x, visible: !x.visible }))} className={`badge ${sl.visible ? 'bg-emerald-500/20 text-emerald-300' : 'bg-white/5 text-surface-200/40'}`}>{sl.visible ? 'Hien' : 'An'}</button>
              </div>
              <div className="flex gap-1 mt-1">
                <button onClick={() => move(i, -1)} className="badge bg-white/5 flex-1">▲</button>
                <button onClick={() => move(i, 1)} className="badge bg-white/5 flex-1">▼</button>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* PREVIEW */}
      <div className="flex-1 flex flex-col items-center justify-center gap-2 min-w-0">
        <canvas ref={cv} width={420} height={420} className="rounded-xl border border-white/10" style={{ background: '#0b0b12' }} />
        <div className="flex items-center gap-2 flex-wrap justify-center">
          <button onClick={() => setPlaying(p => !p)} className="btn-secondary !py-1.5 !px-3">{playing ? 'Tam dung' : 'Phat'}</button>
          <button onClick={() => setCur(c => Math.max(0, c - 1))} className="badge bg-white/5">◀</button>
          <span className="text-surface-200/60">F{cur}/{frameCount - 1}</span>
          <button onClick={() => setCur(c => Math.min(frameCount - 1, c + 1))} className="badge bg-white/5">▶</button>
          <button onClick={() => setFlip(f => !f)} className={`badge ${flip ? 'bg-brand-500/30 text-brand-100' : 'bg-white/5'}`}>↔ Huong</button>
          <label className="flex items-center gap-1 text-surface-200/60">frames<input type="number" value={frameCount} onChange={e => setFrameCount(Math.max(1, +e.target.value))} className="input !py-0.5 !w-14" /></label>
          <label className="flex items-center gap-1 text-surface-200/60">fps<input type="number" value={fps} onChange={e => setFps(+e.target.value)} className="input !py-0.5 !w-12" /></label>
          <label className="flex items-center gap-1 text-surface-200/60">zoom<input type="range" min={1} max={6} step={0.5} value={zoom} onChange={e => setZoom(+e.target.value)} className="accent-brand-500 w-20" /></label>
        </div>
        <div className="flex gap-2">
          <button onClick={exportJson} className="btn-gold !py-1.5 !px-3">Xuat → costume_json</button>
          <button onClick={download} className="btn-secondary !py-1.5 !px-3">Tai JSON</button>
        </div>
      </div>

      {/* SLOT DANG CHON */}
      <div className="w-64 overflow-y-auto space-y-3 pr-1">
        <div className="text-[10px] uppercase tracking-wider text-surface-200/50">Sua lop: <b className="text-surface-100">{s.name}</b></div>
        <label className="block text-[11px] text-surface-200/70">Sheet anh lop<input type="file" accept="image/*" onChange={loadSheet(sel)} className="block w-full text-[11px] mt-1" /></label>
        <div className="grid grid-cols-3 gap-2">
          <label className="block text-[10px] text-surface-200/60">Cols<input type="number" value={s.cols} onChange={e => upd(sel, x => ({ ...x, cols: Math.max(1, +e.target.value) }))} className="input !py-1 mt-0.5" /></label>
          <label className="block text-[10px] text-surface-200/60">Rows<input type="number" value={s.rows} onChange={e => upd(sel, x => ({ ...x, rows: Math.max(1, +e.target.value) }))} className="input !py-1 mt-0.5" /></label>
          <label className="block text-[10px] text-surface-200/60">Frames<input type="number" value={s.frames} onChange={e => upd(sel, x => ({ ...x, frames: Math.max(1, +e.target.value) }))} className="input !py-1 mt-0.5" /></label>
        </div>
        <div className="border-t border-white/5 pt-2">
          <div className="text-[10px] uppercase tracking-wider text-surface-200/50 mb-1">Offset tai frame {cur}</div>
          <div className="grid grid-cols-2 gap-2">
            <label className="block text-[10px] text-surface-200/60">dx<input type="number" value={off[0]} onChange={e => setOff(0, +e.target.value)} className="input !py-1 mt-0.5" /></label>
            <label className="block text-[10px] text-surface-200/60">dy<input type="number" value={off[1]} onChange={e => setOff(1, +e.target.value)} className="input !py-1 mt-0.5" /></label>
          </div>
          <p className="text-[10px] text-surface-200/40 mt-1">Moi frame co offset rieng — chinh tu khop cac lop (vd vu khi theo tay). Frame chua dat = 0,0.</p>
        </div>
      </div>
    </div>
  );
}
