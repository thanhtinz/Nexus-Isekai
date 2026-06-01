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

/* Upload file (raw bytes) vao kho client_assets qua /api/assets/upload */
async function uploadAsset(assetKey: string, assetType: string, category: string, displayName: string, dataUrl: string) {
  const key = localStorage.getItem('studio_key') || '';
  const blob = await (await fetch(dataUrl)).blob();
  const res = await fetch(API_BASE + '/api/assets/upload', {
    method: 'POST',
    headers: {
      'X-Admin-Key': key, 'X-Asset-Key': assetKey, 'X-Asset-Type': assetType,
      'X-Category': category, 'X-Display-Name': displayName, 'Content-Type': blob.type || 'image/png',
    },
    body: blob,
  });
  try { return await res.json(); } catch { return null; }
}

/* Hot-reload de ap thay doi vao game khong can restart */
const RELOAD_MAP: Record<string, string> = { mob: 'monsters', map: 'maps', npc: 'npcs' };
async function reloadGame(sectionKey: string) {
  const t = RELOAD_MAP[sectionKey];
  if (t) { try { await api('/api/reload/' + t, 'POST'); return true; } catch { return false; } }
  return false;
}

type Section = { key: string; label: string; endpoint: string; dataKey: string; pk: string; nameField: string };
const SECTIONS: Section[] = [
  { key: 'skill',    label: 'SKILL',     endpoint: '/api/skills',        dataKey: 'rows', pk: 'id',        nameField: 'name' },
  { key: 'mob',      label: 'MOB / BOSS',endpoint: '/api/monsters',      dataKey: 'rows', pk: 'id',        nameField: 'name' },
  { key: 'effect',   label: 'EFFECT',    endpoint: '/api/sound-events',  dataKey: 'rows', pk: 'event_key', nameField: 'event_key' },
  { key: 'map',      label: 'MAP',       endpoint: '/api/maps',          dataKey: 'rows', pk: 'id',        nameField: 'name' },
  { key: 'npc',      label: 'NPC',       endpoint: '/api/npcs',          dataKey: 'rows', pk: 'id',        nameField: 'name' },
  { key: 'resource', label: 'RESOURCE',  endpoint: '/api/audio-assets',  dataKey: 'rows', pk: 'id',        nameField: 'asset_key' },
  { key: 'kho',      label: 'KHO ASSET', endpoint: '/api/assets',        dataKey: 'assets', pk: 'id',      nameField: 'asset_key' },
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
    const applied = await reloadGame(section.key);
    setMsg(r?.error ? `Loi: ${r.error}` : (applied ? 'Da luu + ap vao game (reload)' : 'Da luu'));
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
            {section.key === 'kho'
              ? <AssetLibrary setMsg={setMsg} />
              : section.key === 'map' && selected
              ? <MapBuilder map={draft} setMsg={setMsg} />
              : tab === 'frames' ? <AnimationEditor draft={draft} setDraft={setDraft} setMsg={setMsg} /> : <PreviewArea row={draft} section={section} />}
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
            : section.key === 'npc'
              ? <NpcEditor draft={draft} setDraft={setDraft} setMsg={setMsg} reload={() => loadList(section)} />
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
        <button onClick={async () => { if (!img) return; const r = await uploadAsset(`Sprites/${Date.now()}.png`, 'sprite', 'general', 'sheet', img); setMsg(r?.success ? 'Da luu sheet vao kho' : 'Loi luu kho'); }}
          disabled={!img} className="btn-secondary text-xs">Luu sheet vao kho</button>
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

/* ── NPC EDITOR: che do chuc nang (chips) hoac thoai+voice+action ── */
const NPC_FUNCTIONS = ['shop','quest','bank','craft','upgrade','teleport','heal','storage'];
const ACTION_TYPES = ['dialog','give_item','warp','emote'];

function NpcEditor({ draft, setDraft, setMsg, reload }: {
  draft: Row; setDraft: (r: Row) => void; setMsg: (s: string) => void; reload: () => void;
}) {
  const parse = (s: any, fb: any) => { try { return s ? JSON.parse(s) : fb; } catch { return fb; } };
  const mode: string = draft.interact_mode || 'function';
  const funcs: string[] = parse(draft.functions_json, []);
  const actions: any[] = parse(draft.action_json, []);
  const set = (k: string, v: any) => setDraft({ ...draft, [k]: v });
  const setFuncs = (f: string[]) => set('functions_json', JSON.stringify(f));
  const setActions = (a: any[]) => set('action_json', JSON.stringify(a));

  const toggleFunc = (f: string) => setFuncs(funcs.includes(f) ? funcs.filter(x => x !== f) : [...funcs, f]);
  const addAction = (t: string) => {
    const base: any = { type: t };
    if (t === 'dialog') base.text = '';
    if (t === 'give_item') { base.id = 0; base.qty = 1; }
    if (t === 'warp') { base.map = 0; base.x = 0; base.y = 0; }
    if (t === 'emote') base.name = '';
    setActions([...actions, base]);
  };
  const updAction = (i: number, k: string, v: any) => setActions(actions.map((a, j) => j === i ? { ...a, [k]: v } : a));
  const delAction = (i: number) => setActions(actions.filter((_, j) => j !== i));

  const save = async () => {
    const r = await api('/api/npcs', 'POST', draft);
    await reloadGame('npc');
    setMsg(r?.success ? 'Da luu NPC + ap vao game' : `Loi: ${r?.error || r?.message || 'luu NPC'}`);
    reload();
  };

  return (
    <div className="p-3 space-y-3 text-xs">
      {/* basic */}
      {['name','map_id','pos_x','pos_y','icon_id','spine_key'].map(k => (
        <div key={k}>
          <label className="block text-[10px] uppercase tracking-wider text-surface-200/50 mb-1">{k}</label>
          <input value={draft[k] ?? ''} onChange={e => set(k, e.target.value)} className="input" />
        </div>
      ))}

      {/* mode toggle */}
      <div>
        <label className="block text-[10px] uppercase tracking-wider text-surface-200/50 mb-1">Che do tuong tac</label>
        <div className="flex gap-1">
          {['function','dialog'].map(m => (
            <button key={m} onClick={() => set('interact_mode', m)}
              className={`flex-1 py-1.5 rounded-lg ${mode === m ? 'bg-brand-500 text-white' : 'bg-white/5 text-surface-200/70'}`}>
              {m === 'function' ? 'Chuc nang (menu)' : 'Thoai + Voice + Action'}</button>
          ))}
        </div>
      </div>

      {mode === 'function' ? (
        <div>
          <label className="block text-[10px] uppercase tracking-wider text-surface-200/50 mb-1">Chuc nang hien thi</label>
          <div className="flex flex-wrap gap-1.5">
            {NPC_FUNCTIONS.map(f => (
              <button key={f} onClick={() => toggleFunc(f)}
                className={`badge ${funcs.includes(f) ? 'bg-brand-500 text-white' : 'bg-white/5 text-surface-200/60'}`}>{f}</button>
            ))}
          </div>
          {funcs.includes('shop') && (
            <div className="mt-2">
              <label className="block text-[10px] uppercase tracking-wider text-surface-200/50 mb-1">shop_id</label>
              <input value={draft.shop_id ?? ''} onChange={e => set('shop_id', e.target.value)} className="input" />
            </div>
          )}
        </div>
      ) : (
        <div className="space-y-2">
          <div>
            <label className="block text-[10px] uppercase tracking-wider text-surface-200/50 mb-1">voice_key (khi tuong tac)</label>
            <input value={draft.voice_key ?? ''} onChange={e => set('voice_key', e.target.value)} className="input" />
          </div>
          <div className="flex items-center justify-between">
            <span className="text-[10px] uppercase tracking-wider text-surface-200/50">Chuoi hanh dong</span>
            <select onChange={e => { if (e.target.value) { addAction(e.target.value); e.target.value = ''; } }} className="input !w-28 !py-1">
              <option value="">+ Them...</option>
              {ACTION_TYPES.map(t => <option key={t} value={t}>{t}</option>)}
            </select>
          </div>
          {actions.map((a, i) => (
            <div key={i} className="card p-2 space-y-1">
              <div className="flex items-center justify-between">
                <span className="badge bg-brand-500/15 text-brand-300">{a.type}</span>
                <button onClick={() => delAction(i)} className="text-red-400 hover:text-red-300">×</button>
              </div>
              {a.type === 'dialog' && <textarea value={a.text} onChange={e => updAction(i, 'text', e.target.value)} rows={2} placeholder="Loi thoai..." className="input" />}
              {a.type === 'give_item' && <div className="flex gap-1">
                <input value={a.id} onChange={e => updAction(i, 'id', +e.target.value)} placeholder="item id" className="input !py-1" />
                <input value={a.qty} onChange={e => updAction(i, 'qty', +e.target.value)} placeholder="qty" className="input !py-1 !w-16" /></div>}
              {a.type === 'warp' && <div className="flex gap-1">
                <input value={a.map} onChange={e => updAction(i, 'map', +e.target.value)} placeholder="map" className="input !py-1" />
                <input value={a.x} onChange={e => updAction(i, 'x', +e.target.value)} placeholder="x" className="input !py-1 !w-16" />
                <input value={a.y} onChange={e => updAction(i, 'y', +e.target.value)} placeholder="y" className="input !py-1 !w-16" /></div>}
              {a.type === 'emote' && <input value={a.name} onChange={e => updAction(i, 'name', e.target.value)} placeholder="ten emote" className="input !py-1" />}
            </div>
          ))}
          {actions.length === 0 && <p className="text-surface-200/40">Chua co hanh dong. Them dialog/give_item/warp/emote.</p>}
        </div>
      )}

      <button onClick={save} className="btn-gold w-full !py-2">Luu NPC</button>
    </div>
  );
}

/* ── KHO ASSET: duyet + upload + xoa client_assets (file luu tren server) ── */
const ASSET_CATEGORIES = ['', 'map_bg', 'map_tile', 'sky', 'parallax', 'monster', 'npc', 'skill', 'effect', 'particle', 'weapon', 'armor', 'cosmetic', 'pet', 'mount', 'ui', 'hud', 'icon', 'general'];

function AssetLibrary({ setMsg }: { setMsg: (s: string) => void }) {
  const [assets, setAssets] = useState<Row[]>([]);
  const [cat, setCat] = useState('');
  const [q, setQ] = useState('');
  const [busy, setBusy] = useState(false);
  const [upCat, setUpCat] = useState('map_bg');
  const fileRef = useRef<HTMLInputElement>(null);

  const load = useCallback(async () => {
    setBusy(true);
    const r = await api(`/api/assets?category=${cat}&q=${encodeURIComponent(q)}`);
    setAssets(r?.assets || []); setBusy(false);
  }, [cat, q]);
  useEffect(() => { load(); }, [load]);

  const onFiles = async (files: FileList) => {
    setBusy(true);
    for (const f of Array.from(files)) {
      const dataUrl: string = await new Promise(res => { const r = new FileReader(); r.onload = () => res(r.result as string); r.readAsDataURL(f); });
      const key = `${upCat}/${f.name}`;
      const type = f.type.startsWith('image') ? 'sprite' : 'data';
      await uploadAsset(key, type, upCat, f.name, dataUrl);
    }
    setMsg(`Da upload ${files.length} file vao kho (${upCat})`);
    await load(); setBusy(false);
  };
  const del = async (id: number) => { await api('/api/assets', 'POST', { action: 'delete', id }); load(); };

  return (
    <div className="w-full h-full flex flex-col gap-3">
      {/* toolbar */}
      <div className="flex items-center gap-2 flex-wrap text-xs">
        <select value={cat} onChange={e => setCat(e.target.value)} className="input !w-40 !py-1.5">
          {ASSET_CATEGORIES.map(c => <option key={c} value={c}>{c || '— Tat ca —'}</option>)}
        </select>
        <input value={q} onChange={e => setQ(e.target.value)} placeholder="Tim asset..." className="input !w-48 !py-1.5" />
        <div className="flex-1" />
        <span className="text-surface-200/50">Upload vao:</span>
        <select value={upCat} onChange={e => setUpCat(e.target.value)} className="input !w-36 !py-1.5">
          {ASSET_CATEGORIES.filter(Boolean).map(c => <option key={c} value={c}>{c}</option>)}
        </select>
        <input ref={fileRef} type="file" accept="image/*" multiple className="hidden" onChange={e => e.target.files && onFiles(e.target.files)} />
        <button onClick={() => fileRef.current?.click()} disabled={busy} className="btn-gold !py-1.5 !px-3">{busy ? '...' : 'Upload'}</button>
      </div>

      {/* grid */}
      <div className="flex-1 overflow-y-auto card p-3">
        <div className="grid grid-cols-6 gap-2">
          {assets.map(a => (
            <div key={a.id} className="card p-1.5 group relative">
              <div className="aspect-square bg-surface-950 rounded overflow-hidden flex items-center justify-center">
                <img src={assetUrl(a.file_path)} alt="" className="max-w-full max-h-full object-contain" style={{ imageRendering: 'pixelated' }} />
              </div>
              <div className="text-[9px] text-surface-100 truncate mt-1" title={a.asset_key}>{a.asset_key}</div>
              <div className="text-[8px] text-surface-200/40">{a.category} · v{a.version}</div>
              <button onClick={() => del(a.id)} className="absolute top-1 right-1 hidden group-hover:block bg-red-500/80 text-white rounded w-4 h-4 text-[10px] leading-none">×</button>
            </div>
          ))}
        </div>
        {!busy && assets.length === 0 && <div className="text-xs text-surface-200/40 p-4 text-center">Kho trong. Upload sprite/bg/asset de dung trong Studio + game tu dong tai (theo version/hash).</div>}
      </div>
      <p className="text-[11px] text-surface-200/50">File luu tren server (thu muc client-assets). Game client tai ve theo version/hash — upload de tang version la client tu cap nhat.</p>
    </div>
  );
}

function PreviewArea({ row, section }: { row: Row; section: Section }) {
  const [imgEl, setImgEl] = useState<HTMLImageElement | null>(null);
  const [playing, setPlaying] = useState(true);
  const [cur, setCur] = useState(0);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const fileRef = useRef<HTMLInputElement>(null);

  // doc animation da luu trong data
  const anim = (() => {
    for (const k of ['animation_json', 'frames_json', 'config_json']) {
      try { const v = row[k]; if (typeof v === 'string' && v.includes('frames')) { const o = JSON.parse(v); if (Array.isArray(o.frames)) return o; } } catch {}
    }
    return null;
  })();
  const frames: any[] = anim?.frames || [];
  const delay = anim?.delay_ms || 120;
  const loop = anim?.loop ?? true;

  const onFile = (f: File) => { const r = new FileReader(); r.onload = () => { const el = new Image(); el.onload = () => setImgEl(el); el.src = r.result as string; }; r.readAsDataURL(f); };

  useEffect(() => {
    if (!playing || frames.length === 0) return;
    const id = setInterval(() => setCur(c => { const n = c + 1; if (n >= frames.length) return loop ? 0 : c; return n; }), delay);
    return () => clearInterval(id);
  }, [playing, frames.length, delay, loop]);

  useEffect(() => {
    const cv = canvasRef.current; if (!cv || !imgEl || frames.length === 0) return;
    const f = frames[cur % frames.length]; if (!f) return;
    const Z = 3; cv.width = f.w * Z; cv.height = f.h * Z;
    const ctx = cv.getContext('2d'); if (!ctx) return;
    ctx.imageSmoothingEnabled = false; ctx.clearRect(0, 0, cv.width, cv.height);
    ctx.drawImage(imgEl, f.x, f.y, f.w, f.h, 0, 0, f.w * Z, f.h * Z);
  }, [cur, imgEl, frames]);

  return (
    <div className="text-center space-y-3">
      {frames.length > 0 ? (
        <>
          <div className="w-48 h-48 mx-auto card flex items-center justify-center overflow-hidden"
            style={{ backgroundImage: 'repeating-conic-gradient(#1a1a35 0 25%, #131328 0 50%)', backgroundSize: '16px 16px' }}>
            {imgEl ? <canvas ref={canvasRef} style={{ imageRendering: 'pixelated' }} />
              : <span className="text-surface-200/40 text-xs px-3">Nap sprite sheet de phat {frames.length} frame da luu</span>}
          </div>
          <div className="flex gap-2 justify-center">
            <input ref={fileRef} type="file" accept="image/png,image/webp" className="hidden" onChange={e => e.target.files?.[0] && onFile(e.target.files[0])} />
            <button onClick={() => fileRef.current?.click()} className="btn-secondary text-xs">Nap sheet</button>
            {imgEl && <button onClick={() => setPlaying(p => !p)} className="btn-primary text-xs !py-1.5 !px-4">{playing ? 'Pause' : 'Play'}</button>}
          </div>
          <div className="text-[11px] text-surface-200/50">{frames.length} frame · {delay}ms · {loop ? 'loop' : 'once'}</div>
        </>
      ) : (
        <div className="w-40 h-40 mx-auto card flex items-center justify-center text-surface-200/40 text-xs">Chua co animation da luu</div>
      )}
      <div className="text-sm text-surface-100">{row[section.nameField] ?? '—'}</div>
      <div className="text-[11px] text-surface-200/50">ID: {row[section.pk] ?? '—'} · {section.label}</div>
    </div>
  );
}

/* ── MAP BUILDER: dat bg-asset/NPC/vung dich chuyen + AI nhan dien anh dung nhap ── */
type BgInst = { key: string; x: number; y: number; scale: number; z: number; file?: string };
type NpcMarker = { id: number; name: string; x: number; y: number };
type Portal = { x: number; y: number; w: number; h: number; dest_map: number; dest_x: number; dest_y: number };

function assetUrl(file?: string) {
  if (!file) return '';
  if (/^https?:/.test(file)) return file;
  return (API_BASE || '') + '/' + file.replace(/^\//, '');
}

function MapBuilder({ map, setMsg }: { map: Row; setMsg: (s: string) => void }) {
  const [palette, setPalette] = useState<Row[]>([]);
  const [npcs, setNpcs] = useState<Row[]>([]);
  const [bg, setBg] = useState<BgInst[]>([]);
  const [markers, setMarkers] = useState<NpcMarker[]>([]);
  const [portals, setPortals] = useState<Portal[]>([]);
  const [tool, setTool] = useState<'select' | 'bg' | 'npc' | 'portal'>('select');
  const [palKey, setPalKey] = useState<string>('');
  const [npcId, setNpcId] = useState<number>(0);
  const [sel, setSel] = useState<{ type: string; i: number } | null>(null);
  const [busy, setBusy] = useState(false);
  const stageRef = useRef<HTMLDivElement>(null);
  const fileRef = useRef<HTMLInputElement>(null);
  const drag = useRef<{ type: string; i: number; dx: number; dy: number } | null>(null);

  // load palette + npcs + layout co san
  useEffect(() => {
    (async () => {
      const pa = await api('/api/map-assets'); setPalette(pa?.rows || []);
      const np = await api('/api/npcs'); setNpcs((np?.rows || []).filter((n: Row) => String(n.map_id) === String(map.id)));
    })();
  }, [map.id]);
  useEffect(() => {
    try {
      const L = typeof map.layout_json === 'string' && map.layout_json ? JSON.parse(map.layout_json) : null;
      setBg(L?.bg || []); setMarkers(L?.npcs || []); setPortals(L?.portals || []);
    } catch { setBg([]); setMarkers([]); setPortals([]); }
  }, [map.id, map.layout_json]);

  const palFile = (key: string) => palette.find(p => p.asset_key === key)?.file_path;

  const onStageClick = (e: React.MouseEvent) => {
    if (drag.current) return;
    const r = stageRef.current!.getBoundingClientRect();
    const x = Math.round(e.clientX - r.left), y = Math.round(e.clientY - r.top);
    if (tool === 'bg' && palKey) setBg(b => [...b, { key: palKey, x, y, scale: 1, z: b.length, file: palFile(palKey) }]);
    else if (tool === 'npc' && npcId) { const n = npcs.find(x => x.id === npcId); if (n) setMarkers(m => [...m, { id: npcId, name: n.name, x, y }]); }
    else if (tool === 'portal') setPortals(p => [...p, { x, y, w: 64, h: 64, dest_map: 0, dest_x: 0, dest_y: 0 }]);
  };
  const startDrag = (e: React.MouseEvent, type: string, i: number) => {
    e.stopPropagation();
    const r = stageRef.current!.getBoundingClientRect();
    const item: any = type === 'bg' ? bg[i] : type === 'npc' ? markers[i] : portals[i];
    drag.current = { type, i, dx: (e.clientX - r.left) - item.x, dy: (e.clientY - r.top) - item.y };
    setSel({ type, i });
  };
  const onMove = (e: React.MouseEvent) => {
    if (!drag.current) return;
    const r = stageRef.current!.getBoundingClientRect();
    const x = Math.round(e.clientX - r.left - drag.current.dx), y = Math.round(e.clientY - r.top - drag.current.dy);
    const { type, i } = drag.current;
    if (type === 'bg') setBg(b => b.map((it, k) => k === i ? { ...it, x, y } : it));
    else if (type === 'npc') setMarkers(m => m.map((it, k) => k === i ? { ...it, x, y } : it));
    else setPortals(p => p.map((it, k) => k === i ? { ...it, x, y } : it));
  };
  const endDrag = () => { setTimeout(() => { drag.current = null; }, 0); };
  const delSel = () => {
    if (!sel) return;
    if (sel.type === 'bg') setBg(b => b.filter((_, k) => k !== sel.i));
    else if (sel.type === 'npc') setMarkers(m => m.filter((_, k) => k !== sel.i));
    else setPortals(p => p.filter((_, k) => k !== sel.i));
    setSel(null);
  };

  const aiSuggest = (f: File) => {
    const reader = new FileReader();
    reader.onload = async () => {
      setBusy(true);
      const r = await api('/api/studio/map-suggest', 'POST', {
        image_base64: reader.result,
        assets: palette.map(p => ({ key: p.asset_key, category: p.category })),
      });
      if (r?.success) {
        try {
          const L = JSON.parse(String(r.result).replace(/```json|```/g, '').trim());
          if (Array.isArray(L.bg)) setBg(L.bg.map((b: any, i: number) => ({ key: b.key, x: b.x || 0, y: b.y || 0, scale: b.scale || 1, z: b.z ?? i, file: palFile(b.key) })));
          setMsg('AI da dung nhap layout — chinh lai roi Luu');
        } catch { setMsg('AI tra ve khong phai JSON hop le'); }
      } else setMsg(`Loi: ${r?.message || 'AI'}`);
      setBusy(false);
    };
    reader.readAsDataURL(f);
  };

  const save = async () => {
    setBusy(true);
    const layout = JSON.stringify({ bg, npcs: markers, portals });
    const r = await api('/api/maps', 'POST', { action: 'upsert', id: map.id, layout_json: layout });
    setMsg(r?.error ? `Loi: ${r.error}` : 'Da luu map layout');
    setBusy(false);
  };

  // Dong bo vi tri NPC + portal tu canvas xuong thang bang npcs / map_portals
  const applyToTables = async () => {
    setBusy(true);
    // NPC: gui full row (tranh crudConfig ghi rong cot khac) + vi tri moi
    for (const mk of markers) {
      const full = npcs.find(n => n.id === mk.id);
      if (full) await api('/api/npcs', 'POST', { ...full, pos_x: mk.x, pos_y: mk.y, map_id: map.id });
    }
    // Portal: xoa portal cu cua map roi tao lai theo canvas
    const ex = await api(`/api/portals?map_id=${map.id}`);
    for (const p of (ex?.portals || [])) await api('/api/portals', 'POST', { action: 'delete', id: p.id });
    for (const p of portals) await api('/api/portals', 'POST', {
      action: 'create', from_map_id: map.id, to_map_id: p.dest_map || 0,
      from_x: p.x, from_y: p.y, to_x: p.dest_x || 0, to_y: p.dest_y || 0, min_level: 0,
    });
    await save();
    await reloadGame('map'); await reloadGame('npc');
    setMsg(`Da ap dung + reload: ${markers.length} NPC + ${portals.length} portal vao game`);
    setBusy(false);
  };

  return (
    <div className="w-full h-full flex gap-3">
      {/* palette + tools */}
      <div className="w-52 flex flex-col gap-2 text-xs">
        <div className="flex gap-1">
          {(['select','bg','npc','portal'] as const).map(t => (
            <button key={t} onClick={() => setTool(t)}
              className={`flex-1 py-1.5 rounded-lg ${tool === t ? 'bg-brand-500 text-white' : 'bg-white/5 text-surface-200/70'}`}>
              {t === 'select' ? 'Chon' : t === 'bg' ? 'BG' : t === 'npc' ? 'NPC' : 'Portal'}</button>
          ))}
        </div>
        <input ref={fileRef} type="file" accept="image/png,image/jpeg,image/webp" className="hidden"
          onChange={e => e.target.files?.[0] && aiSuggest(e.target.files[0])} />
        <button onClick={() => fileRef.current?.click()} disabled={busy} className="btn-secondary !text-brand-300 !py-1.5">
          {busy ? 'AI dang dung...' : 'AI: Nhan dien anh → dung'}</button>
        {tool === 'bg' && (
          <div className="flex-1 overflow-y-auto card p-2 grid grid-cols-3 gap-1">
            {palette.map(p => (
              <button key={p.asset_key} onClick={() => setPalKey(p.asset_key)} title={p.asset_key}
                className={`aspect-square rounded bg-surface-950 overflow-hidden border ${palKey === p.asset_key ? 'border-brand-500' : 'border-white/10'}`}>
                <img src={assetUrl(p.file_path)} alt="" className="w-full h-full object-contain" style={{ imageRendering: 'pixelated' }} />
              </button>
            ))}
            {palette.length === 0 && <span className="col-span-3 text-surface-200/40">Chua co bg asset (them o RESOURCE/client_assets)</span>}
          </div>
        )}
        {tool === 'npc' && (
          <select value={npcId} onChange={e => setNpcId(+e.target.value)} className="input">
            <option value={0}>— Chon NPC —</option>
            {npcs.map(n => <option key={n.id} value={n.id}>{n.id} · {n.name}</option>)}
          </select>
        )}
        {sel && <button onClick={delSel} className="btn-danger !py-1.5">Xoa muc dang chon</button>}
        <div className="mt-auto space-y-1.5">
          <button onClick={save} disabled={busy} className="btn-secondary w-full !py-2">Luu Layout</button>
          <button onClick={applyToTables} disabled={busy} className="btn-gold w-full !py-2">Luu + Ap dung xuong bang</button>
        </div>
      </div>

      {/* stage */}
      <div ref={stageRef} onClick={onStageClick} onMouseMove={onMove} onMouseUp={endDrag} onMouseLeave={endDrag}
        className="relative flex-1 card overflow-hidden cursor-crosshair"
        style={{ minHeight: 400, backgroundImage: 'repeating-conic-gradient(#11112a 0 25%, #0d0d24 0 50%)', backgroundSize: '24px 24px' }}>
        {bg.map((b, i) => (
          <img key={'b' + i} src={assetUrl(b.file)} alt="" draggable={false}
            onMouseDown={e => startDrag(e, 'bg', i)}
            className={`absolute select-none ${sel?.type === 'bg' && sel.i === i ? 'ring-2 ring-brand-500' : ''}`}
            style={{ left: b.x, top: b.y, transform: `scale(${b.scale})`, zIndex: b.z, imageRendering: 'pixelated' }} />
        ))}
        {portals.map((p, i) => (
          <div key={'p' + i} onMouseDown={e => startDrag(e, 'portal', i)}
            className={`absolute bg-emerald-500/20 border-2 border-emerald-400 flex items-center justify-center text-[10px] text-emerald-200 ${sel?.type === 'portal' && sel.i === i ? 'ring-2 ring-white' : ''}`}
            style={{ left: p.x, top: p.y, width: p.w, height: p.h, zIndex: 9998 }}>→{p.dest_map || '?'}</div>
        ))}
        {markers.map((mk, i) => (
          <div key={'n' + i} onMouseDown={e => startDrag(e, 'npc', i)}
            className={`absolute -translate-x-1/2 -translate-y-1/2 px-1.5 py-0.5 rounded bg-brand-500 text-white text-[10px] whitespace-nowrap ${sel?.type === 'npc' && sel.i === i ? 'ring-2 ring-white' : ''}`}
            style={{ left: mk.x, top: mk.y, zIndex: 9999 }}>◈ {mk.name}</div>
        ))}
        <div className="absolute bottom-1 right-2 text-[10px] text-surface-200/40">{map.name} · {bg.length} bg · {markers.length} npc · {portals.length} portal</div>
      </div>

      {/* portal inspector */}
      {sel?.type === 'portal' && (
        <div className="w-44 card p-2 space-y-2 text-xs h-fit">
          <div className="text-surface-200/60">Vung dich chuyen</div>
          {(['dest_map','dest_x','dest_y','w','h'] as const).map(f => (
            <label key={f} className="block">{f}
              <input type="number" value={(portals[sel.i] as any)[f]} className="input !py-1"
                onChange={e => setPortals(p => p.map((it, k) => k === sel.i ? { ...it, [f]: +e.target.value } : it))} /></label>
          ))}
        </div>
      )}
    </div>
  );
}
