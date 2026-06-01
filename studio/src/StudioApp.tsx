import { useState, useEffect, useCallback, useRef } from 'react';

/* ──────────────────────────────────────────────────────────────
   NEXUS STUDIO — tool bien tap data DOC LAP (project rieng, build/deploy rieng)
   nhung DUNG CHUNG he giao dien web (palette brand/surface/gold, font, class
   .card/.btn/.input). Khong thuoc admin. Ket noi server qua VITE_API_BASE + key rieng.
   Module: Skill / Mob / Effect / Map / Npc / Resource. AI-assist + tach anh.
   ────────────────────────────────────────────────────────────── */

const API_BASE = (import.meta as any).env?.VITE_API_BASE || '';

async function api(path: string, method = 'GET', body?: any) {
  const key = localStorage.getItem('studio_key') || '';
  const res = await fetch(API_BASE + path, {
    method,
    headers: { 'Content-Type': 'application/json', 'X-Admin-Key': key },
    body: body ? JSON.stringify(body) : undefined,
  });
  try { return await res.json(); } catch { return null; }
}

type Section = { key: string; label: string; endpoint: string; dataKey: string; pk: string; nameField: string };
const SECTIONS: Section[] = [
  { key: 'skill',    label: 'SKILL',     endpoint: '/api/skills',        dataKey: 'rows', pk: 'id',        nameField: 'name' },
  { key: 'mob',      label: 'MOB / BOSS',endpoint: '/api/monsters',      dataKey: 'rows', pk: 'id',        nameField: 'name' },
  { key: 'effect',   label: 'EFFECT',    endpoint: '/api/sound-events',  dataKey: 'rows', pk: 'event_key', nameField: 'event_key' },
  { key: 'map',      label: 'MAP',       endpoint: '/api/maps',          dataKey: 'rows', pk: 'id',        nameField: 'name' },
  { key: 'npc',      label: 'NPC',       endpoint: '/api/npcs',          dataKey: 'rows', pk: 'id',        nameField: 'name' },
  { key: 'resource', label: 'RESOURCE',  endpoint: '/api/audio-assets',  dataKey: 'rows', pk: 'id',        nameField: 'asset_key' },
];

type Row = Record<string, any>;

export default function StudioApp() {
  const [section, setSection] = useState<Section>(SECTIONS[0]);
  const [list, setList] = useState<Row[]>([]);
  const [search, setSearch] = useState('');
  const [selected, setSelected] = useState<Row | null>(null);
  const [draft, setDraft] = useState<Row>({});
  const [tab, setTab] = useState<'general' | 'frames' | 'vfx' | 'preview'>('general');
  const [busy, setBusy] = useState('');
  const [msg, setMsg] = useState('');
  const [keyVal, setKeyVal] = useState(localStorage.getItem('studio_key') || '');
  const [showConn, setShowConn] = useState(!localStorage.getItem('studio_key'));

  const loadList = useCallback(async (s: Section) => {
    setBusy('list'); setSelected(null);
    const r = await api(s.endpoint);
    setList(Array.isArray(r?.[s.dataKey]) ? r[s.dataKey] : (Array.isArray(r) ? r : []));
    setBusy('');
  }, []);

  useEffect(() => { loadList(section); }, [section, loadList]);

  const pick = (row: Row) => { setSelected(row); setDraft({ ...row }); setTab('general'); };

  const save = async () => {
    setBusy('save');
    const r = await api(section.endpoint, 'POST', { action: 'upsert', ...draft });
    setMsg(r?.error ? `Loi: ${r.error}` : 'Da luu');
    await loadList(section);
    setBusy('');
  };

  const saveKey = () => { localStorage.setItem('studio_key', keyVal); setShowConn(false); loadList(section); };

  const filtered = list.filter(r =>
    String(r[section.nameField] ?? '').toLowerCase().includes(search.toLowerCase()) ||
    String(r[section.pk] ?? '').includes(search));

  return (
    <div className="h-screen w-screen flex flex-col bg-surface-950 text-surface-100 overflow-hidden">
      {/* Top bar */}
      <header className="flex items-center gap-4 px-4 h-14 bg-surface-850 border-b border-white/5">
        <div className="flex items-center gap-2">
          <span className="font-display text-lg font-bold text-brand-400 tracking-wider">NEXUS STUDIO</span>
          <span className="badge bg-brand-500/15 text-brand-300">v1.0 · DATA TOOL</span>
        </div>
        <div className="flex-1" />
        <button onClick={() => setShowConn(v => !v)} className="btn-secondary !py-1.5 !px-3 text-xs">Ket noi</button>
        <button onClick={save} disabled={!selected} className="btn-gold !py-1.5 !px-3 text-xs">Save Data</button>
      </header>

      {/* Connection bar */}
      {showConn && (
        <div className="flex items-center gap-2 px-4 py-2.5 bg-surface-900 border-b border-white/10 text-xs">
          <span className="text-surface-200/60">API:</span>
          <span className="text-surface-100">{API_BASE || '(proxy /api)'}</span>
          <span className="text-surface-200/60 ml-3">Key:</span>
          <input value={keyVal} onChange={e => setKeyVal(e.target.value)} type="password" placeholder="Admin/Studio key"
            className="input !w-64 !py-1.5" />
          <button onClick={saveKey} className="btn-primary !py-1.5 !px-3">Luu & Ket noi</button>
          <span className="text-surface-200/40">Doi VITE_API_BASE trong .env de tro server khac.</span>
        </div>
      )}

      <div className="flex-1 flex min-h-0">
        {/* Left: module nav + library */}
        <aside className="w-64 flex flex-col bg-surface-850 border-r border-white/5">
          <nav className="grid grid-cols-2 gap-1.5 p-2.5">
            {SECTIONS.map(s => (
              <button key={s.key} onClick={() => setSection(s)}
                className={`text-[11px] py-2 rounded-lg font-medium tracking-wide transition-colors ${
                  section.key === s.key ? 'bg-brand-500 text-white' : 'bg-white/5 text-surface-200/70 hover:bg-white/10'}`}>
                {s.label}
              </button>
            ))}
          </nav>
          <div className="px-2.5 pb-2">
            <input value={search} onChange={e => setSearch(e.target.value)} placeholder="Tim..." className="input !py-2" />
          </div>
          <div className="flex-1 overflow-y-auto px-2.5 pb-4 space-y-0.5">
            {busy === 'list' && <div className="text-xs text-surface-200/50 p-2">Dang tai...</div>}
            {filtered.map((row, i) => (
              <button key={i} onClick={() => pick(row)}
                className={`w-full text-left text-xs px-2.5 py-2 rounded-lg truncate transition-colors ${
                  selected && selected[section.pk] === row[section.pk]
                    ? 'bg-brand-500/20 text-brand-200 border border-brand-500/30'
                    : 'text-surface-100/80 hover:bg-white/5'}`}>
                <span className="opacity-40 mr-1.5">{row[section.pk]}</span>{row[section.nameField] ?? ''}
              </button>
            ))}
            {!busy && filtered.length === 0 && <div className="text-xs text-surface-200/40 p-2">Khong co du lieu (kiem tra Ket noi)</div>}
          </div>
        </aside>

        {/* Center: preview / frames */}
        <main className="flex-1 flex flex-col min-w-0 bg-surface-950">
          <div className="flex-1 flex items-center justify-center p-6 overflow-auto">
            {tab === 'frames' ? <AnimationEditor draft={draft} setDraft={setDraft} setMsg={setMsg} /> : <PreviewArea row={draft} section={section} />}
          </div>
          <div className="flex gap-1.5 px-3 h-10 items-center border-t border-white/5 bg-surface-850">
            {(['general','frames','vfx','preview'] as const).map(t => (
              <button key={t} onClick={() => setTab(t)}
                className={`px-3 py-1.5 text-[11px] rounded-lg uppercase tracking-wide transition-colors ${
                  tab === t ? 'bg-brand-500/20 text-brand-200' : 'text-surface-200/50 hover:text-surface-100'}`}>
                {t === 'general' ? 'General' : t === 'frames' ? 'Frame/Tach anh' : t === 'vfx' ? 'VFX/Effect' : 'Preview'}
              </button>
            ))}
          </div>
        </main>

        {/* Right: property editor */}
        <aside className="w-96 flex flex-col bg-surface-850 border-l border-white/5 overflow-y-auto">
          {msg && <div className="m-3 text-xs px-3 py-2 rounded-lg bg-brand-500/10 text-brand-200 border border-brand-500/20">{msg}</div>}
          {!selected
            ? <div className="p-4 text-xs text-surface-200/50">Chon mot muc ben trai de sua.</div>
            : tab === 'vfx'
              ? <VfxTab draft={draft} setDraft={setDraft} setMsg={setMsg} busy={busy} setBusy={setBusy} />
              : <GeneralTab draft={draft} setDraft={setDraft} section={section} setMsg={setMsg} busy={busy} setBusy={setBusy} />}
        </aside>
      </div>
    </div>
  );
}

function GeneralTab({ draft, setDraft, section, setMsg, busy, setBusy }: {
  draft: Row; setDraft: (r: Row) => void; section: Section;
  setMsg: (s: string) => void; busy: string; setBusy: (s: string) => void;
}) {
  const fields = Object.keys(draft).filter(k => k !== section.pk);
  const set = (k: string, v: any) => setDraft({ ...draft, [k]: v });

  const aiDescribe = async () => {
    setBusy('ai-desc');
    const r = await api('/api/studio/ai', 'POST', { task: 'gen_description', payload: JSON.stringify(draft) });
    if (r?.success && 'description' in draft) set('description', r.result.trim());
    setMsg(r?.success ? 'AI da tao mo ta' : `Loi: ${r?.message || 'AI'}`);
    setBusy('');
  };
  const aiStats = async () => {
    setBusy('ai-stats');
    const r = await api('/api/studio/ai', 'POST', { task: 'suggest_stats', payload: JSON.stringify(draft) });
    if (r?.success) {
      try { const s = JSON.parse(r.result.replace(/```json|```/g, '').trim());
        setDraft({ ...draft, ...Object.fromEntries(Object.entries(s).filter(([k]) => k in draft)) }); } catch {}
    }
    setMsg(r?.success ? 'AI da goi y chi so' : `Loi: ${r?.message || 'AI'}`);
    setBusy('');
  };

  return (
    <div className="p-3 space-y-3">
      <div className="flex gap-2 flex-wrap">
        <button onClick={aiDescribe} disabled={!!busy} className="btn-secondary !py-1.5 !px-2.5 text-[11px] !text-brand-300">
          {busy === 'ai-desc' ? '...' : 'AI: Mo ta'}</button>
        <button onClick={aiStats} disabled={!!busy} className="btn-secondary !py-1.5 !px-2.5 text-[11px] !text-brand-300">
          {busy === 'ai-stats' ? '...' : 'AI: Goi y chi so'}</button>
      </div>
      {fields.map(k => (
        <div key={k}>
          <label className="block text-[10px] uppercase tracking-wider text-surface-200/50 mb-1">{k}</label>
          {String(draft[k] ?? '').length > 60 || k === 'description'
            ? <textarea value={draft[k] ?? ''} onChange={e => set(k, e.target.value)} rows={3} className="input" />
            : <input value={draft[k] ?? ''} onChange={e => set(k, e.target.value)} className="input" />}
        </div>
      ))}
    </div>
  );
}

function VfxTab({ draft, setDraft, setMsg, busy, setBusy }: {
  draft: Row; setDraft: (r: Row) => void; setMsg: (s: string) => void; busy: string; setBusy: (s: string) => void;
}) {
  const [desc, setDesc] = useState('');
  const [out, setOut] = useState('');
  const gen = async () => {
    setBusy('ai-vfx');
    const r = await api('/api/studio/ai', 'POST', { task: 'gen_vfx', payload: desc || JSON.stringify(draft) });
    if (r?.success) setOut(r.result);
    setMsg(r?.success ? 'AI da tao cau hinh VFX' : `Loi: ${r?.message || 'AI'}`);
    setBusy('');
  };
  const apply = () => {
    const key = 'vfx_config' in draft ? 'vfx_config' : ('config_json' in draft ? 'config_json' : null);
    if (key) { setDraft({ ...draft, [key]: out }); setMsg('Da gan VFX vao ' + key); }
    else setMsg('Muc nay khong co truong VFX/config de gan');
  };
  return (
    <div className="p-3 space-y-3">
      <p className="text-[11px] text-surface-200/60">Mo ta hieu ung → AI tao JSON cau hinh VFX (particle/shake/flash).</p>
      <textarea value={desc} onChange={e => setDesc(e.target.value)} rows={3}
        placeholder="vd: vu no lua do, rung man hinh, tia lua bay len" className="input" />
      <button onClick={gen} disabled={!!busy} className="btn-secondary !text-brand-300 text-xs">
        {busy === 'ai-vfx' ? 'Dang tao...' : 'AI: Tao VFX'}</button>
      {out && <>
        <pre className="text-[10px] text-surface-100/80 card p-2.5 max-h-60 overflow-auto whitespace-pre-wrap">{out}</pre>
        <button onClick={apply} className="btn-gold text-xs">Gan vao muc dang sua</button>
      </>}
    </div>
  );
}

type Frame = { x: number; y: number; w: number; h: number };

/* Tach anh + lam hoat anh: slice → dai frame → phat (Play/Delay/Zoom/Loop) → xuat JSON. */
function AnimationEditor({ draft, setDraft, setMsg }: {
  draft: Row; setDraft: (r: Row) => void; setMsg: (s: string) => void;
}) {
  const [img, setImg] = useState<string>('');
  const [imgEl, setImgEl] = useState<HTMLImageElement | null>(null);
  const [frames, setFrames] = useState<Frame[]>([]);
  const [dim, setDim] = useState({ w: 0, h: 0 });
  const [order, setOrder] = useState<number[]>([]);   // thu tu phat (index vao frames)
  const [busy, setBusy] = useState(false);
  const [delay, setDelay] = useState(120);            // ms / frame
  const [zoom, setZoom] = useState(4);
  const [loop, setLoop] = useState(true);
  const [playing, setPlaying] = useState(false);
  const [cur, setCur] = useState(0);                  // vi tri trong order
  const fileRef = useRef<HTMLInputElement>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);

  const onFile = (f: File) => {
    const reader = new FileReader();
    reader.onload = () => {
      const src = reader.result as string; setImg(src); setFrames([]); setOrder([]); setPlaying(false);
      const el = new Image(); el.onload = () => { setImgEl(el); setDim({ w: el.width, h: el.height }); }; el.src = src;
    };
    reader.readAsDataURL(f);
  };
  const slice = async () => {
    if (!img) return;
    setBusy(true);
    const r = await api('/api/studio/slice', 'POST', { image_base64: img });
    if (r?.success) { setFrames(r.frames); setDim({ w: r.width, h: r.height }); setOrder(r.frames.map((_: any, i: number) => i)); setCur(0); }
    setBusy(false);
  };

  // ve frame hien tai len canvas preview
  useEffect(() => {
    const cv = canvasRef.current; if (!cv || !imgEl || order.length === 0) return;
    const f = frames[order[cur % order.length]]; if (!f) return;
    cv.width = f.w * zoom; cv.height = f.h * zoom;
    const ctx = cv.getContext('2d'); if (!ctx) return;
    ctx.imageSmoothingEnabled = false;
    ctx.clearRect(0, 0, cv.width, cv.height);
    ctx.drawImage(imgEl, f.x, f.y, f.w, f.h, 0, 0, f.w * zoom, f.h * zoom);
  }, [cur, order, frames, imgEl, zoom]);

  // vong phat
  useEffect(() => {
    if (!playing || order.length === 0) return;
    const id = setInterval(() => {
      setCur(c => {
        const n = c + 1;
        if (n >= order.length) { if (!loop) { setPlaying(false); return c; } return 0; }
        return n;
      });
    }, delay);
    return () => clearInterval(id);
  }, [playing, delay, order, loop]);

  const cropThumb = (f: Frame) => {
    if (!imgEl) return '';
    const c = document.createElement('canvas'); c.width = f.w; c.height = f.h;
    const ctx = c.getContext('2d'); if (!ctx) return '';
    ctx.imageSmoothingEnabled = false; ctx.drawImage(imgEl, f.x, f.y, f.w, f.h, 0, 0, f.w, f.h);
    return c.toDataURL();
  };
  const removeFromOrder = (pos: number) => setOrder(o => o.filter((_, i) => i !== pos));
  const moveFrame = (pos: number, dir: -1 | 1) => setOrder(o => {
    const n = [...o]; const t = pos + dir; if (t < 0 || t >= n.length) return o;
    [n[pos], n[t]] = [n[t], n[pos]]; return n;
  });

  const exportAnim = () => {
    const anim = { delay_ms: delay, loop, frames: order.map(i => frames[i]) };
    const json = JSON.stringify(anim);
    const field = 'animation_json' in draft ? 'animation_json' : ('frames_json' in draft ? 'frames_json' : ('config_json' in draft ? 'config_json' : null));
    if (field) { setDraft({ ...draft, [field]: json }); setMsg('Da gan hoat anh vao ' + field + ' (bam Save Data de luu)'); }
    else { navigator.clipboard?.writeText(json); setMsg('Muc nay khong co truong animation — da copy JSON vao clipboard'); }
  };

  return (
    <div className="w-full max-w-3xl space-y-3">
      {/* toolbar */}
      <div className="flex items-center gap-2 flex-wrap">
        <input ref={fileRef} type="file" accept="image/png,image/webp" className="hidden"
          onChange={e => e.target.files?.[0] && onFile(e.target.files[0])} />
        <button onClick={() => fileRef.current?.click()} className="btn-secondary text-xs">Chon sprite sheet</button>
        <button onClick={slice} disabled={!img || busy} className="btn-gold text-xs">{busy ? 'Dang tach...' : 'Tach anh (auto)'}</button>
        {frames.length > 0 && <span className="text-xs text-emerald-400">{frames.length} frame</span>}
      </div>

      {order.length > 0 && (
        <div className="card p-3 space-y-3">
          {/* preview + controls */}
          <div className="flex gap-4 items-start">
            <div className="flex items-center justify-center bg-surface-950 rounded-lg p-3 min-w-[160px] min-h-[160px]"
              style={{ backgroundImage: 'repeating-conic-gradient(#1a1a35 0 25%, #131328 0 50%)', backgroundSize: '16px 16px' }}>
              <canvas ref={canvasRef} style={{ imageRendering: 'pixelated' }} />
            </div>
            <div className="flex-1 space-y-2 text-xs">
              <div className="flex gap-2">
                <button onClick={() => setPlaying(p => !p)} className="btn-primary !py-1.5 !px-4">{playing ? 'Pause' : 'Play'}</button>
                <button onClick={() => { setCur(0); setPlaying(false); }} className="btn-secondary !py-1.5 !px-3">Stop</button>
              </div>
              <label className="block">Delay: {delay}ms
                <input type="range" min={30} max={500} step={10} value={delay} onChange={e => setDelay(+e.target.value)} className="w-full accent-brand-500" /></label>
              <label className="block">Zoom: {zoom}x
                <input type="range" min={1} max={8} value={zoom} onChange={e => setZoom(+e.target.value)} className="w-full accent-brand-500" /></label>
              <label className="flex items-center gap-2"><input type="checkbox" checked={loop} onChange={e => setLoop(e.target.checked)} className="accent-brand-500" /> Loop</label>
              <button onClick={exportAnim} className="btn-gold !py-1.5 !px-3 w-full">Xuat hoat anh → data</button>
            </div>
          </div>

          {/* frame strip */}
          <div className="flex gap-2 overflow-x-auto pb-1">
            {order.map((fi, pos) => (
              <div key={pos} className={`flex-shrink-0 rounded-lg border p-1 ${pos === cur % order.length ? 'border-brand-500' : 'border-white/10'}`}>
                <div className="w-14 h-14 bg-surface-950 rounded flex items-center justify-center overflow-hidden">
                  <img src={cropThumb(frames[fi])} alt={`f${fi}`} className="max-w-full max-h-full" style={{ imageRendering: 'pixelated' }} />
                </div>
                <div className="flex items-center justify-between mt-1 text-[9px] text-surface-200/50">
                  <button onClick={() => moveFrame(pos, -1)} className="hover:text-white px-1">‹</button>
                  <span>{pos + 1}</span>
                  <button onClick={() => moveFrame(pos, 1)} className="hover:text-white px-1">›</button>
                  <button onClick={() => removeFromOrder(pos)} className="hover:text-red-400 px-1">×</button>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* sheet voi khung frame */}
      {img && (
        <div className="relative inline-block card overflow-hidden">
          <img src={img} alt="sheet" className="block max-w-full" style={{ imageRendering: 'pixelated' }} />
          {dim.w > 0 && frames.map((f, i) => (
            <div key={i} className="absolute border border-gold-500/70"
              style={{ left: `${f.x / dim.w * 100}%`, top: `${f.y / dim.h * 100}%`, width: `${f.w / dim.w * 100}%`, height: `${f.h / dim.h * 100}%` }} />
          ))}
        </div>
      )}
      {!img && <p className="text-xs text-surface-200/50">Tai len sprite sheet (nen trong suot) — tool tu nhan dien tung khung hinh, ghep thanh hoat anh, xuat JSON vao data.</p>}
    </div>
  );
}

function PreviewArea({ row, section }: { row: Row; section: Section }) {
  return (
    <div className="text-center">
      <div className="w-40 h-40 mx-auto card flex items-center justify-center text-surface-200/40 text-xs">Preview</div>
      <div className="mt-3 text-sm text-surface-100">{row[section.nameField] ?? '—'}</div>
      <div className="text-[11px] text-surface-200/50">ID: {row[section.pk] ?? '—'} · {section.label}</div>
    </div>
  );
}
