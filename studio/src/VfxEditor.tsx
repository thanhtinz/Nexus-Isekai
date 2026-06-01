import { useEffect, useRef, useState } from 'react';

/* ── VFX Particle Editor ──────────────────────────────────────────────
   Editor hat truc quan: chinh thong so → preview canvas realtime → xuat JSON
   (vfx_config) gan vao item/skill/effect. Co preset san. Khong dung asset game thuong mai. */

type VfxParams = {
  emitRate: number; maxParticles: number;
  lifeMin: number; lifeMax: number;
  speedMin: number; speedMax: number;
  angle: number; spread: number; gravity: number;
  sizeStart: number; sizeEnd: number;
  colorStart: string; colorEnd: string;
  alphaStart: number; alphaEnd: number;
  blend: 'normal' | 'add';
  shape: 'point' | 'circle' | 'line';
  shapeSize: number;
  burst: number; loop: boolean; durationMs: number;
};

const DEFAULTS: VfxParams = {
  emitRate: 60, maxParticles: 400, lifeMin: 500, lifeMax: 1000,
  speedMin: 40, speedMax: 120, angle: -90, spread: 360, gravity: 60,
  sizeStart: 10, sizeEnd: 0, colorStart: '#ffd24d', colorEnd: '#ff5a3c',
  alphaStart: 1, alphaEnd: 0, blend: 'add', shape: 'point', shapeSize: 8,
  burst: 0, loop: true, durationMs: 1200,
};

const PRESETS: Record<string, Partial<VfxParams>> = {
  'No tung': { emitRate: 0, burst: 120, speedMin: 120, speedMax: 320, spread: 360, gravity: 120, lifeMin: 400, lifeMax: 800, sizeStart: 12, sizeEnd: 0, colorStart: '#ffe08a', colorEnd: '#ff3b2f', blend: 'add', loop: true, durationMs: 900 },
  'Lua':     { emitRate: 80, burst: 0, angle: -90, spread: 35, speedMin: 30, speedMax: 90, gravity: -40, lifeMin: 500, lifeMax: 1100, sizeStart: 14, sizeEnd: 2, colorStart: '#ffd24d', colorEnd: '#ff3b1f', blend: 'add' },
  'Khoi':    { emitRate: 30, burst: 0, angle: -90, spread: 25, speedMin: 20, speedMax: 50, gravity: -20, lifeMin: 1200, lifeMax: 2200, sizeStart: 16, sizeEnd: 46, colorStart: '#777777', colorEnd: '#222222', blend: 'normal', alphaStart: 0.5, alphaEnd: 0 },
  'Lap lanh':{ emitRate: 40, burst: 0, spread: 360, speedMin: 10, speedMax: 60, gravity: 0, lifeMin: 400, lifeMax: 900, sizeStart: 6, sizeEnd: 0, colorStart: '#ffffff', colorEnd: '#7fd8ff', blend: 'add' },
  'Hoi mau': { emitRate: 50, burst: 0, angle: -90, spread: 30, speedMin: 30, speedMax: 70, gravity: -30, lifeMin: 700, lifeMax: 1300, sizeStart: 10, sizeEnd: 0, colorStart: '#9bff8a', colorEnd: '#1fc46b', blend: 'add' },
  'Len cap': { emitRate: 0, burst: 80, angle: -90, spread: 50, speedMin: 80, speedMax: 200, gravity: -10, lifeMin: 600, lifeMax: 1200, sizeStart: 8, sizeEnd: 0, colorStart: '#ffe98a', colorEnd: '#ffae00', blend: 'add', loop: true, durationMs: 1100 },
};

type P = { x: number; y: number; vx: number; vy: number; life: number; max: number; size0: number; size1: number };

function hexToRgb(h: string) { const n = parseInt(h.replace('#', ''), 16); return [(n >> 16) & 255, (n >> 8) & 255, n & 255]; }
function lerp(a: number, b: number, t: number) { return a + (b - a) * t; }

export default function VfxEditor({ draft, setDraft, setMsg }: {
  draft: any; setDraft: (d: any) => void; setMsg: (s: string) => void;
}) {
  const [p, setP] = useState<VfxParams>(() => {
    const raw = draft?.vfx_config || draft?.config_json;
    if (raw) { try { return { ...DEFAULTS, ...JSON.parse(raw) }; } catch { /* ignore */ } }
    return DEFAULTS;
  });
  const [playing, setPlaying] = useState(true);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const pref = useRef(p); pref.current = p;
  const particles = useRef<P[]>([]);
  const acc = useRef(0); const t0 = useRef(performance.now()); const last = useRef(performance.now());
  const playRef = useRef(playing); playRef.current = playing;

  const set = <K extends keyof VfxParams>(k: K, v: VfxParams[K]) => setP(s => ({ ...s, [k]: v }));

  useEffect(() => {
    const cv = canvasRef.current; if (!cv) return;
    const ctx = cv.getContext('2d')!;
    let raf = 0;
    const W = cv.width, H = cv.height, ox = W / 2, oy = H / 2 + 40;

    const emit = (n: number) => {
      const s = pref.current;
      for (let i = 0; i < n; i++) {
        if (particles.current.length >= s.maxParticles) break;
        const a = (s.angle + (Math.random() - 0.5) * s.spread) * Math.PI / 180;
        const sp = lerp(s.speedMin, s.speedMax, Math.random());
        let px = ox, py = oy;
        if (s.shape === 'circle') { const r = Math.random() * s.shapeSize, ang = Math.random() * Math.PI * 2; px += Math.cos(ang) * r; py += Math.sin(ang) * r; }
        else if (s.shape === 'line') { px += (Math.random() - 0.5) * s.shapeSize * 2; }
        const life = lerp(s.lifeMin, s.lifeMax, Math.random());
        particles.current.push({ x: px, y: py, vx: Math.cos(a) * sp, vy: Math.sin(a) * sp, life, max: life, size0: s.sizeStart, size1: s.sizeEnd });
      }
    };

    const frame = (now: number) => {
      const s = pref.current;
      const dt = Math.min(50, now - last.current); last.current = now;
      if (playRef.current) {
        const elapsed = now - t0.current;
        if (s.loop && elapsed > s.durationMs) { t0.current = now; if (s.burst > 0) emit(s.burst); }
        if (elapsed < s.durationMs || !s.loop) acc.current += (s.emitRate * dt) / 1000;
        while (acc.current >= 1) { emit(1); acc.current -= 1; }
        const arr = particles.current; const next: P[] = [];
        for (const q of arr) {
          q.life -= dt; if (q.life <= 0) continue;
          q.vy += (s.gravity * dt) / 1000; q.x += (q.vx * dt) / 1000; q.y += (q.vy * dt) / 1000;
          next.push(q);
        }
        particles.current = next;
      }
      // ve
      ctx.clearRect(0, 0, W, H);
      ctx.fillStyle = '#0b0b12'; ctx.fillRect(0, 0, W, H);
      ctx.globalCompositeOperation = s.blend === 'add' ? 'lighter' : 'source-over';
      const c0 = hexToRgb(s.colorStart), c1 = hexToRgb(s.colorEnd);
      for (const q of particles.current) {
        const t = 1 - q.life / q.max;
        const r = Math.max(0.5, lerp(q.size0, q.size1, t));
        const cr = Math.round(lerp(c0[0], c1[0], t)), cg = Math.round(lerp(c0[1], c1[1], t)), cb = Math.round(lerp(c0[2], c1[2], t));
        const al = lerp(s.alphaStart, s.alphaEnd, t);
        const g = ctx.createRadialGradient(q.x, q.y, 0, q.x, q.y, r);
        g.addColorStop(0, `rgba(${cr},${cg},${cb},${al})`);
        g.addColorStop(1, `rgba(${cr},${cg},${cb},0)`);
        ctx.fillStyle = g; ctx.beginPath(); ctx.arc(q.x, q.y, r, 0, Math.PI * 2); ctx.fill();
      }
      ctx.globalCompositeOperation = 'source-over';
      ctx.fillStyle = 'rgba(255,255,255,.15)'; ctx.fillRect(ox - 6, oy - 1, 12, 2); ctx.fillRect(ox - 1, oy - 6, 2, 12);
      raf = requestAnimationFrame(frame);
    };
    raf = requestAnimationFrame(frame);
    return () => cancelAnimationFrame(raf);
  }, []);

  const applyPreset = (name: string) => { setP(s => ({ ...DEFAULTS, ...s, ...PRESETS[name] })); particles.current = []; t0.current = performance.now(); };
  const restart = () => { particles.current = []; t0.current = performance.now(); acc.current = 0; setPlaying(true); };

  const exportJson = () => {
    const json = JSON.stringify(p);
    const key = 'vfx_config' in draft ? 'vfx_config' : ('config_json' in draft ? 'config_json' : 'vfx_config');
    setDraft({ ...draft, [key]: json });
    setMsg('Da gan VFX vao ' + key + ' (nho Luu o General de ghi DB)');
  };

  const Slider = ({ label, k, min, max, step = 1 }: { label: string; k: keyof VfxParams; min: number; max: number; step?: number }) => (
    <label className="block">
      <div className="flex justify-between text-[10px] text-surface-200/60"><span>{label}</span><span className="text-surface-100">{p[k] as number}</span></div>
      <input type="range" min={min} max={max} step={step} value={p[k] as number}
        onChange={e => set(k, Number(e.target.value) as any)} className="w-full accent-brand-500" />
    </label>
  );

  return (
    <div className="w-full h-full flex gap-4">
      {/* Preview */}
      <div className="flex-1 flex flex-col items-center justify-center gap-3">
        <canvas ref={canvasRef} width={420} height={420}
          className="rounded-xl border border-white/10" style={{ background: '#0b0b12' }} />
        <div className="flex gap-2">
          <button onClick={() => setPlaying(v => !v)} className="btn-secondary !py-1.5 !px-3 text-xs">{playing ? 'Tam dung' : 'Chay'}</button>
          <button onClick={restart} className="btn-secondary !py-1.5 !px-3 text-xs">Lam lai</button>
          <button onClick={exportJson} className="btn-gold !py-1.5 !px-3 text-xs">Xuat JSON → gan</button>
        </div>
      </div>
      {/* Controls */}
      <div className="w-72 overflow-y-auto space-y-3 pr-1">
        <div>
          <div className="text-[10px] uppercase tracking-wider text-surface-200/50 mb-1">Preset</div>
          <div className="flex flex-wrap gap-1">
            {Object.keys(PRESETS).map(n => (
              <button key={n} onClick={() => applyPreset(n)} className="badge bg-white/5 text-surface-200/70 hover:bg-brand-500/20">{n}</button>
            ))}
          </div>
        </div>
        <Slider label="Emit/s" k="emitRate" min={0} max={300} />
        <Slider label="Burst (no)" k="burst" min={0} max={400} />
        <Slider label="Max hat" k="maxParticles" min={50} max={2000} step={50} />
        <div className="grid grid-cols-2 gap-2">
          <Slider label="Song min (ms)" k="lifeMin" min={100} max={3000} step={50} />
          <Slider label="Song max" k="lifeMax" min={100} max={3000} step={50} />
          <Slider label="Toc min" k="speedMin" min={0} max={400} />
          <Slider label="Toc max" k="speedMax" min={0} max={400} />
        </div>
        <Slider label="Goc phun" k="angle" min={-180} max={180} />
        <Slider label="Toe (spread)" k="spread" min={0} max={360} />
        <Slider label="Trong luc" k="gravity" min={-200} max={200} />
        <div className="grid grid-cols-2 gap-2">
          <Slider label="Size dau" k="sizeStart" min={0} max={60} />
          <Slider label="Size cuoi" k="sizeEnd" min={0} max={60} />
          <Slider label="Alpha dau" k="alphaStart" min={0} max={1} step={0.05} />
          <Slider label="Alpha cuoi" k="alphaEnd" min={0} max={1} step={0.05} />
        </div>
        <div className="grid grid-cols-2 gap-2">
          <label className="block"><div className="text-[10px] text-surface-200/60">Mau dau</div>
            <input type="color" value={p.colorStart} onChange={e => set('colorStart', e.target.value)} className="w-full h-7 rounded bg-transparent" /></label>
          <label className="block"><div className="text-[10px] text-surface-200/60">Mau cuoi</div>
            <input type="color" value={p.colorEnd} onChange={e => set('colorEnd', e.target.value)} className="w-full h-7 rounded bg-transparent" /></label>
        </div>
        <div className="grid grid-cols-2 gap-2">
          <label className="block"><div className="text-[10px] text-surface-200/60">Blend</div>
            <select value={p.blend} onChange={e => set('blend', e.target.value as any)} className="input !py-1 text-xs">
              <option value="add">Cong sang (add)</option><option value="normal">Thuong</option></select></label>
          <label className="block"><div className="text-[10px] text-surface-200/60">Nguon phun</div>
            <select value={p.shape} onChange={e => set('shape', e.target.value as any)} className="input !py-1 text-xs">
              <option value="point">Diem</option><option value="circle">Vong tron</option><option value="line">Duong</option></select></label>
        </div>
        <Slider label="Kich thuoc nguon" k="shapeSize" min={0} max={80} />
        <div className="grid grid-cols-2 gap-2 items-end">
          <Slider label="Chu ky (ms)" k="durationMs" min={200} max={4000} step={50} />
          <label className="flex items-center gap-2 text-[11px] text-surface-200/70">
            <input type="checkbox" checked={p.loop} onChange={e => set('loop', e.target.checked)} className="accent-brand-500" />Lap
          </label>
        </div>
      </div>
    </div>
  );
}
