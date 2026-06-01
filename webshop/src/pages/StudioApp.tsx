import { useState, useEffect, useCallback, useRef } from 'react';

/* ──────────────────────────────────────────────────────────────
   GAME STUDIO — tool tách riêng (không thuộc admin), dùng data hệ thống.
   Trình biên tập trực quan: chọn module (Skill/Mob/Effect/Map/Resource) →
   thư viện → sửa thuộc tính (schema-driven) → AI-assist + tách ảnh + xem trước.
   Route: /studio
   ────────────────────────────────────────────────────────────── */

const API = (import.meta as any).env?.VITE_ADMIN_API || '';

async function api(path: string, method = 'GET', body?: any) {
  const key = localStorage.getItem('admin_key') || '';
  const res = await fetch(API + path, {
    method,
    headers: { 'Content-Type': 'application/json', 'X-Admin-Key': key },
    body: body ? JSON.stringify(body) : undefined,
  });
  try { return await res.json(); } catch { return null; }
}

type Section = { key: string; label: string; endpoint: string; dataKey: string; pk: string; nameField: string };
const SECTIONS: Section[] = [
  { key: 'skill',    label: 'SKILL',     endpoint: '/api/skills',        dataKey: 'rows', pk: 'id',         nameField: 'name' },
  { key: 'mob',      label: 'MOB / BOSS',endpoint: '/api/monsters',      dataKey: 'rows', pk: 'id',         nameField: 'name' },
  { key: 'effect',   label: 'EFFECT',    endpoint: '/api/sound-events',  dataKey: 'rows', pk: 'event_key',  nameField: 'event_key' },
  { key: 'map',      label: 'MAP',       endpoint: '/api/maps',          dataKey: 'rows', pk: 'id',         nameField: 'name' },
  { key: 'npc',      label: 'NPC',       endpoint: '/api/npcs',          dataKey: 'rows', pk: 'id',         nameField: 'name' },
  { key: 'resource', label: 'RESOURCE',  endpoint: '/api/audio-assets',  dataKey: 'rows', pk: 'id',         nameField: 'asset_key' },
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

  const filtered = list.filter(r =>
    String(r[section.nameField] ?? '').toLowerCase().includes(search.toLowerCase()) ||
    String(r[section.pk] ?? '').includes(search));

  return (
    <div className="h-screen w-screen flex flex-col bg-[#0b0b1a] text-gray-200 overflow-hidden">
      {/* Top bar */}
      <header className="flex items-center gap-4 px-4 h-12 bg-gradient-to-r from-[#2a1207] to-[#3a1a08] border-b border-[#5a2e10]">
        <div className="flex items-center gap-2">
          <span className="text-[#f0a020] font-bold tracking-widest text-sm">NEXUS STUDIO</span>
          <span className="text-[10px] text-gray-500">DATA TOOL</span>
        </div>
        <div className="flex-1" />
        <button onClick={save} disabled={!selected}
          className="px-3 py-1 text-xs rounded bg-[#f0a020] text-black font-semibold disabled:opacity-30">Save Data</button>
        <a href="/sys/internal/v2/dashboard" className="text-xs text-gray-400 hover:text-white">← Admin</a>
      </header>

      <div className="flex-1 flex min-h-0">
        {/* Left: section nav + library */}
        <aside className="w-64 flex flex-col bg-[#0e0e22] border-r border-white/5">
          <nav className="grid grid-cols-2 gap-1 p-2">
            {SECTIONS.map(s => (
              <button key={s.key} onClick={() => setSection(s)}
                className={`text-[11px] py-2 rounded font-medium tracking-wide ${
                  section.key === s.key ? 'bg-[#5a2e10] text-[#f0a020]' : 'bg-[#15152e] text-gray-400 hover:text-white'}`}>
                {s.label}
              </button>
            ))}
          </nav>
          <div className="px-2 pb-2">
            <input value={search} onChange={e => setSearch(e.target.value)} placeholder="Tim..."
              className="w-full bg-[#15152e] border border-white/10 rounded px-2 py-1.5 text-xs" />
          </div>
          <div className="flex-1 overflow-y-auto px-2 pb-4 space-y-0.5">
            {busy === 'list' && <div className="text-xs text-gray-500 p-2">Dang tai...</div>}
            {filtered.map((row, i) => (
              <button key={i} onClick={() => pick(row)}
                className={`w-full text-left text-xs px-2 py-1.5 rounded truncate ${
                  selected && selected[section.pk] === row[section.pk]
                    ? 'bg-[#f0a020] text-black' : 'text-gray-300 hover:bg-[#15152e]'}`}>
                <span className="opacity-50 mr-1">{row[section.pk]}</span>{row[section.nameField] ?? ''}
              </button>
            ))}
            {!busy && filtered.length === 0 && <div className="text-xs text-gray-600 p-2">Khong co du lieu</div>}
          </div>
        </aside>

        {/* Center: preview / frames */}
        <main className="flex-1 flex flex-col min-w-0 bg-[#08081a]">
          <div className="flex-1 flex items-center justify-center p-6">
            {tab === 'frames'
              ? <SpriteSlicer />
              : <PreviewArea row={draft} section={section} />}
          </div>
          {/* tab strip */}
          <div className="flex gap-1 px-3 h-9 items-center border-t border-white/5 bg-[#0e0e22]">
            {(['general','frames','vfx','preview'] as const).map(t => (
              <button key={t} onClick={() => setTab(t)}
                className={`px-3 py-1 text-[11px] rounded uppercase tracking-wide ${
                  tab === t ? 'bg-[#5a2e10] text-[#f0a020]' : 'text-gray-500 hover:text-gray-300'}`}>
                {t === 'general' ? 'General' : t === 'frames' ? 'Frame/Tach anh' : t === 'vfx' ? 'VFX/Effect' : 'Preview'}
              </button>
            ))}
          </div>
        </main>

        {/* Right: property editor */}
        <aside className="w-96 flex flex-col bg-[#0e0e22] border-l border-white/5 overflow-y-auto">
          {msg && <div className="m-3 text-xs px-2 py-1.5 rounded bg-[#1a1a3a] text-[#f0a020]">{msg}</div>}
          {!selected
            ? <div className="p-4 text-xs text-gray-600">Chon mot muc ben trai de sua.</div>
            : tab === 'vfx'
              ? <VfxTab draft={draft} setDraft={setDraft} setMsg={setMsg} busy={busy} setBusy={setBusy} />
              : <GeneralTab draft={draft} setDraft={setDraft} section={section}
                  setMsg={setMsg} busy={busy} setBusy={setBusy} />}
        </aside>
      </div>
    </div>
  );
}

/* ── Editor tab: schema-driven fields + AI-assist ───────────── */
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
        <button onClick={aiDescribe} disabled={!!busy}
          className="px-2 py-1 text-[11px] rounded bg-[#2a1f4a] text-[#b090f0] disabled:opacity-40">
          {busy === 'ai-desc' ? '...' : 'AI: Mo ta'}</button>
        <button onClick={aiStats} disabled={!!busy}
          className="px-2 py-1 text-[11px] rounded bg-[#2a1f4a] text-[#b090f0] disabled:opacity-40">
          {busy === 'ai-stats' ? '...' : 'AI: Goi y chi so'}</button>
      </div>
      {fields.map(k => (
        <div key={k}>
          <label className="block text-[10px] uppercase tracking-wider text-gray-500 mb-0.5">{k}</label>
          {String(draft[k] ?? '').length > 60 || k === 'description'
            ? <textarea value={draft[k] ?? ''} onChange={e => set(k, e.target.value)} rows={3}
                className="w-full bg-[#15152e] border border-white/10 rounded px-2 py-1.5 text-xs" />
            : <input value={draft[k] ?? ''} onChange={e => set(k, e.target.value)}
                className="w-full bg-[#15152e] border border-white/10 rounded px-2 py-1.5 text-xs" />}
        </div>
      ))}
    </div>
  );
}

/* ── VFX tab: AI tạo cấu hình VFX (JSON) ────────────────────── */
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
    const key = 'vfx_key' in draft ? 'vfx_config' : ('config_json' in draft ? 'config_json' : null);
    if (key) { setDraft({ ...draft, [key]: out }); setMsg('Da gan VFX vao ' + key); }
    else setMsg('Muc nay khong co truong VFX/config de gan');
  };
  return (
    <div className="p-3 space-y-3">
      <p className="text-[11px] text-gray-400">Mo ta hieu ung (vd: "vu no lua do, rung man hinh, tia lua bay len") → AI tao JSON cau hinh VFX.</p>
      <textarea value={desc} onChange={e => setDesc(e.target.value)} rows={3} placeholder="Mo ta hieu ung..."
        className="w-full bg-[#15152e] border border-white/10 rounded px-2 py-1.5 text-xs" />
      <button onClick={gen} disabled={!!busy}
        className="px-3 py-1 text-xs rounded bg-[#2a1f4a] text-[#b090f0] disabled:opacity-40">
        {busy === 'ai-vfx' ? 'Dang tao...' : 'AI: Tao VFX'}</button>
      {out && <>
        <pre className="text-[10px] text-gray-300 bg-[#08081a] border border-white/10 rounded p-2 max-h-60 overflow-auto whitespace-pre-wrap">{out}</pre>
        <button onClick={apply} className="px-3 py-1 text-xs rounded bg-[#f0a020] text-black">Gan vao muc dang sua</button>
      </>}
    </div>
  );
}

/* ── Tách ảnh: upload sprite sheet → /api/studio/slice → khung frame ── */
function SpriteSlicer() {
  const [img, setImg] = useState<string>('');
  const [frames, setFrames] = useState<any[]>([]);
  const [dim, setDim] = useState({ w: 0, h: 0 });
  const [busy, setBusy] = useState(false);
  const fileRef = useRef<HTMLInputElement>(null);

  const onFile = (f: File) => {
    const reader = new FileReader();
    reader.onload = () => setImg(reader.result as string);
    reader.readAsDataURL(f);
    setFrames([]);
  };
  const slice = async () => {
    if (!img) return;
    setBusy(true);
    const r = await api('/api/studio/slice', 'POST', { image_base64: img });
    if (r?.success) { setFrames(r.frames); setDim({ w: r.width, h: r.height }); }
    setBusy(false);
  };

  return (
    <div className="w-full max-w-2xl space-y-3">
      <div className="flex items-center gap-2">
        <input ref={fileRef} type="file" accept="image/png,image/webp" className="hidden"
          onChange={e => e.target.files?.[0] && onFile(e.target.files[0])} />
        <button onClick={() => fileRef.current?.click()} className="px-3 py-1 text-xs rounded bg-[#15152e] border border-white/10">Chon sprite sheet</button>
        <button onClick={slice} disabled={!img || busy} className="px-3 py-1 text-xs rounded bg-[#f0a020] text-black disabled:opacity-30">
          {busy ? 'Dang tach...' : 'Tach anh (auto)'}</button>
        {frames.length > 0 && <span className="text-xs text-[#4ecca3]">{frames.length} frame</span>}
      </div>
      {img && (
        <div className="relative inline-block border border-white/10 rounded overflow-hidden bg-[#111]">
          <img src={img} alt="sheet" className="block max-w-full" style={{ imageRendering: 'pixelated' }} />
          {dim.w > 0 && frames.map((f, i) => (
            <div key={i} className="absolute border border-[#f0a020]/80"
              style={{ left: `${f.x / dim.w * 100}%`, top: `${f.y / dim.h * 100}%`, width: `${f.w / dim.w * 100}%`, height: `${f.h / dim.h * 100}%` }} />
          ))}
        </div>
      )}
      {!img && <p className="text-xs text-gray-600">Tai len sprite sheet (nen trong suot) — tool tu nhan dien tung khung hinh.</p>}
    </div>
  );
}

/* ── Preview ─────────────────────────────────────────────────── */
function PreviewArea({ row, section }: { row: Row; section: Section }) {
  return (
    <div className="text-center">
      <div className="w-40 h-40 mx-auto rounded-lg bg-[#15152e] border border-white/10 flex items-center justify-center text-gray-600 text-xs">
        Preview
      </div>
      <div className="mt-3 text-sm text-gray-300">{row[section.nameField] ?? '—'}</div>
      <div className="text-[11px] text-gray-600">ID: {row[section.pk] ?? '—'} · {section.label}</div>
    </div>
  );
}
