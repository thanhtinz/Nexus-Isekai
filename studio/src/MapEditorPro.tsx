import { useEffect, useRef, useState } from 'react';

/* ── Map Editor Pro (giong tab BAN DO cua HTTH) ──────────────────────────
   Tile/Water layer (luoi) + Collision (o chan) + Object/NPC/Mob/Portal (dat tu do) +
   Brush/Erase/Fill + Undo/Redo + Export INSERT SQL / JSON. Luu vao maps.layout_json
   (mo rong: tileset/tiles/waterset/water/collision/objects/npcs/monsters/portals) +
   ap NPC/Portal xuong bang + reload game. Tuong thich layout cu {bg,npcs,portals}. */

const CELL = 24;
const API_BASE = (import.meta as any).env?.VITE_API_BASE || '';
const assetUrl = (file?: string) => !file ? '' : (/^https?:/.test(file) ? file : (API_BASE || '') + '/' + file.replace(/^\//, ''));
async function api(path: string, method = 'GET', body?: any) {
  const res = await fetch(API_BASE + path, { method, headers: { 'Content-Type': 'application/json', 'X-Admin-Key': localStorage.getItem('studio_key') || '' }, body: body ? JSON.stringify(body) : undefined });
  try { return await res.json(); } catch { return null; }
}

type Row = any;
type Obj = { key: string; x: number; y: number; scale: number };
type Marker = { id: number; name?: string; x: number; y: number };
type Portal = { x: number; y: number; w: number; h: number; dest_map: number; dest_x: number; dest_y: number };
type Layer = 'tile' | 'water' | 'collision';
type Tool = 'brush' | 'erase' | 'fill' | 'object' | 'npc' | 'mob' | 'portal' | 'select';

type Snap = { tileset: string[]; tiles: number[]; waterset: string[]; water: number[]; collision: number[]; objects: Obj[]; markers: Marker[]; mobs: Marker[]; portals: Portal[] };

export default function MapEditorPro({ map, setMsg }: { map: Row; setMsg: (s: string) => void }) {
  const W = Math.max(8, Math.min(120, Number(map.width) || 40));
  const H = Math.max(8, Math.min(120, Number(map.height) || 30));
  const cvRef = useRef<HTMLCanvasElement>(null);

  const [palette, setPalette] = useState<Row[]>([]);
  const [npcs, setNpcs] = useState<Row[]>([]);
  const [mobsCat, setMobsCat] = useState<Row[]>([]);

  const [tileset, setTileset] = useState<string[]>([]);
  const [tiles, setTiles] = useState<number[]>(() => new Array(W * H).fill(-1));
  const [waterset, setWaterset] = useState<string[]>([]);
  const [water, setWater] = useState<number[]>(() => new Array(W * H).fill(-1));
  const [collision, setCollision] = useState<number[]>(() => new Array(W * H).fill(0));
  const [objects, setObjects] = useState<Obj[]>([]);
  const [markers, setMarkers] = useState<Marker[]>([]);
  const [mobs, setMobs] = useState<Marker[]>([]);
  const [portals, setPortals] = useState<Portal[]>([]);

  const [tool, setTool] = useState<Tool>('brush');
  const [layer, setLayer] = useState<Layer>('tile');
  const [palKey, setPalKey] = useState('');
  const [npcId, setNpcId] = useState(0);
  const [mobId, setMobId] = useState(0);
  const [zoom, setZoom] = useState(1);
  const [showColl, setShowColl] = useState(true);
  const [showGrid, setShowGrid] = useState(true);
  const [sel, setSel] = useState<{ t: string; i: number } | null>(null);
  const [busy, setBusy] = useState(false);
  const [exp, setExp] = useState('');

  const undoRef = useRef<Snap[]>([]); const redoRef = useRef<Snap[]>([]);
  const imgCache = useRef<Record<string, HTMLImageElement>>({});
  const painting = useRef(false);
  const drag = useRef<{ t: string; i: number; ox: number; oy: number } | null>(null);

  const ref = useRef<any>({});
  ref.current = { tileset, tiles, waterset, water, collision, objects, markers, mobs, portals, sel, showColl, showGrid };

  // load palette + npc + mob + layout cu
  useEffect(() => {
    (async () => {
      const pa = await api('/api/map-assets'); setPalette(pa?.rows || []);
      const np = await api('/api/npcs'); setNpcs((np?.rows || []).filter((n: Row) => String(n.map_id) === String(map.id) || !n.map_id));
      const mb = await api('/api/monsters'); setMobsCat(mb?.rows || []);
    })();
  }, [map.id]);
  useEffect(() => {
    try {
      const L = typeof map.layout_json === 'string' && map.layout_json ? JSON.parse(map.layout_json) : null;
      if (!L) return;
      setTileset(L.tileset || []); setTiles(L.tiles && L.tiles.length === W * H ? L.tiles : new Array(W * H).fill(-1));
      setWaterset(L.waterset || []); setWater(L.water && L.water.length === W * H ? L.water : new Array(W * H).fill(-1));
      setCollision(L.collision && L.collision.length === W * H ? L.collision : new Array(W * H).fill(0));
      setObjects(L.objects || L.bg || []); setMarkers(L.npcs || []); setMobs(L.monsters || []); setPortals(L.portals || []);
    } catch { /* */ }
  }, [map.id]);

  const palFile = (key: string) => palette.find(p => p.asset_key === key)?.file_path;
  const getImg = (key: string): HTMLImageElement | null => {
    if (!key) return null;
    if (imgCache.current[key]) return imgCache.current[key];
    const f = palFile(key); if (!f) return null;
    const im = new Image(); im.src = assetUrl(f); imgCache.current[key] = im; return im;
  };
  const snap = (): Snap => ({ tileset: [...tileset], tiles: [...tiles], waterset: [...waterset], water: [...water], collision: [...collision], objects: objects.map(o => ({ ...o })), markers: markers.map(m => ({ ...m })), mobs: mobs.map(m => ({ ...m })), portals: portals.map(p => ({ ...p })) });
  const pushUndo = () => { undoRef.current.push(snap()); if (undoRef.current.length > 40) undoRef.current.shift(); redoRef.current = []; };
  const applySnap = (s: Snap) => { setTileset(s.tileset); setTiles(s.tiles); setWaterset(s.waterset); setWater(s.water); setCollision(s.collision); setObjects(s.objects); setMarkers(s.markers); setMobs(s.mobs); setPortals(s.portals); };
  const undo = () => { const s = undoRef.current.pop(); if (!s) return; redoRef.current.push(snap()); applySnap(s); };
  const redo = () => { const s = redoRef.current.pop(); if (!s) return; undoRef.current.push(snap()); applySnap(s); };

  // index trong tileset/waterset cho 1 key (them neu chua co)
  const idxOf = (set: string[], setSet: (s: string[]) => void, key: string) => {
    let i = set.indexOf(key); if (i < 0) { i = set.length; setSet([...set, key]); } return i;
  };

  const paintCell = (col: number, row: number) => {
    if (col < 0 || row < 0 || col >= W || row >= H) return;
    const k = row * W + col;
    if (layer === 'collision') {
      setCollision(c => { const a = [...c]; a[k] = tool === 'erase' ? 0 : 1; return a; }); return;
    }
    if (tool === 'erase') {
      if (layer === 'tile') setTiles(t => { const a = [...t]; a[k] = -1; return a; });
      else setWater(t => { const a = [...t]; a[k] = -1; return a; });
      return;
    }
    if (!palKey) return;
    if (layer === 'tile') { const ix = idxOf(tileset, setTileset, palKey); setTiles(t => { const a = [...t]; a[k] = ix; return a; }); }
    else { const ix = idxOf(waterset, setWaterset, palKey); setWater(t => { const a = [...t]; a[k] = ix; return a; }); }
  };

  const fillCell = (col: number, row: number) => {
    const k = row * W + col; const grid = layer === 'water' ? water : layer === 'collision' ? collision : tiles;
    const target = grid[k];
    let val: number;
    if (layer === 'collision') val = tool === 'erase' ? 0 : 1;
    else if (tool === 'erase') val = -1;
    else { if (!palKey) return; val = layer === 'water' ? idxOf(waterset, setWaterset, palKey) : idxOf(tileset, setTileset, palKey); }
    if (target === val) return;
    const out = [...grid]; const st = [k];
    while (st.length) {
      const c = st.pop()!; if (out[c] !== target) continue; out[c] = val;
      const cc = c % W, rr = Math.floor(c / W);
      if (cc > 0) st.push(c - 1); if (cc < W - 1) st.push(c + 1); if (rr > 0) st.push(c - W); if (rr < H - 1) st.push(c + W);
    }
    if (layer === 'water') setWater(out); else if (layer === 'collision') setCollision(out); else setTiles(out);
  };

  const pxFromEvent = (e: React.MouseEvent) => {
    const cv = cvRef.current!; const r = cv.getBoundingClientRect();
    return { px: (e.clientX - r.left) / r.width * cv.width, py: (e.clientY - r.top) / r.height * cv.height };
  };
  const hit = (px: number, py: number): { t: string; i: number } | null => {
    for (let i = portals.length - 1; i >= 0; i--) { const p = portals[i]; if (px >= p.x && px <= p.x + p.w && py >= p.y && py <= p.y + p.h) return { t: 'portal', i }; }
    for (let i = markers.length - 1; i >= 0; i--) if (Math.hypot(px - markers[i].x, py - markers[i].y) < 14) return { t: 'npc', i };
    for (let i = mobs.length - 1; i >= 0; i--) if (Math.hypot(px - mobs[i].x, py - mobs[i].y) < 14) return { t: 'mob', i };
    for (let i = objects.length - 1; i >= 0; i--) if (Math.hypot(px - objects[i].x, py - objects[i].y) < 18) return { t: 'object', i };
    return null;
  };

  const onDown = (e: React.MouseEvent) => {
    const { px, py } = pxFromEvent(e); const col = Math.floor(px / CELL), row = Math.floor(py / CELL);
    if (tool === 'brush' || tool === 'erase') { pushUndo(); painting.current = true; paintCell(col, row); }
    else if (tool === 'fill') { pushUndo(); fillCell(col, row); }
    else if (tool === 'object') { if (!palKey) { setMsg('Chon 1 object o palette'); return; } pushUndo(); setObjects(o => [...o, { key: palKey, x: px, y: py, scale: 1 }]); }
    else if (tool === 'npc') { if (!npcId) { setMsg('Chon NPC'); return; } pushUndo(); const n = npcs.find(x => x.id === npcId); setMarkers(m => [...m, { id: npcId, name: n?.name, x: px, y: py }]); }
    else if (tool === 'mob') { if (!mobId) { setMsg('Chon Mob'); return; } pushUndo(); const n = mobsCat.find(x => x.id === mobId); setMobs(m => [...m, { id: mobId, name: n?.name, x: px, y: py }]); }
    else if (tool === 'portal') { pushUndo(); setPortals(p => [...p, { x: px - 32, y: py - 32, w: 64, h: 64, dest_map: 0, dest_x: 0, dest_y: 0 }]); }
    else if (tool === 'select') { const h = hit(px, py); setSel(h); if (h) { const it: any = h.t === 'portal' ? portals[h.i] : h.t === 'npc' ? markers[h.i] : h.t === 'mob' ? mobs[h.i] : objects[h.i]; drag.current = { t: h.t, i: h.i, ox: px - it.x, oy: py - it.y }; } }
  };
  const onMoveCanvas = (e: React.MouseEvent) => {
    const { px, py } = pxFromEvent(e);
    if (painting.current) { paintCell(Math.floor(px / CELL), Math.floor(py / CELL)); return; }
    if (drag.current) {
      const { t, i, ox, oy } = drag.current; const x = px - ox, y = py - oy;
      if (t === 'portal') setPortals(p => p.map((it, k) => k === i ? { ...it, x, y } : it));
      else if (t === 'npc') setMarkers(m => m.map((it, k) => k === i ? { ...it, x, y } : it));
      else if (t === 'mob') setMobs(m => m.map((it, k) => k === i ? { ...it, x, y } : it));
      else setObjects(o => o.map((it, k) => k === i ? { ...it, x, y } : it));
    }
  };
  const onUp = () => { painting.current = false; drag.current = null; };
  const delSel = () => {
    if (!sel) return; pushUndo();
    if (sel.t === 'portal') setPortals(p => p.filter((_, k) => k !== sel.i));
    else if (sel.t === 'npc') setMarkers(m => m.filter((_, k) => k !== sel.i));
    else if (sel.t === 'mob') setMobs(m => m.filter((_, k) => k !== sel.i));
    else setObjects(o => o.filter((_, k) => k !== sel.i));
    setSel(null);
  };

  // render
  useEffect(() => {
    const cv = cvRef.current; if (!cv) return; const ctx = cv.getContext('2d')!;
    let raf = 0;
    const draw = () => {
      const s = ref.current; ctx.imageSmoothingEnabled = false;
      ctx.fillStyle = '#0b0b12'; ctx.fillRect(0, 0, cv.width, cv.height);
      const drawGrid = (grid: number[], set: string[]) => {
        for (let k = 0; k < grid.length; k++) { const ix = grid[k]; if (ix < 0) continue; const im = getImg(set[ix]); if (im && im.complete && im.width) ctx.drawImage(im, (k % W) * CELL, Math.floor(k / W) * CELL, CELL, CELL); }
      };
      drawGrid(s.tiles, s.tileset); drawGrid(s.water, s.waterset);
      for (const o of s.objects) { const im = getImg(o.key); if (im && im.complete && im.width) { const w = im.width * o.scale, h = im.height * o.scale; ctx.drawImage(im, o.x - w / 2, o.y - h / 2, w, h); } }
      if (s.showColl) { ctx.fillStyle = 'rgba(255,60,60,.32)'; for (let k = 0; k < s.collision.length; k++) if (s.collision[k]) ctx.fillRect((k % W) * CELL, Math.floor(k / W) * CELL, CELL, CELL); }
      for (const p of s.portals) { ctx.strokeStyle = '#22d3ee'; ctx.lineWidth = 2; ctx.strokeRect(p.x, p.y, p.w, p.h); ctx.fillStyle = 'rgba(34,211,238,.12)'; ctx.fillRect(p.x, p.y, p.w, p.h); }
      const dot = (m: Marker, col: string) => { ctx.fillStyle = col; ctx.beginPath(); ctx.arc(m.x, m.y, 8, 0, Math.PI * 2); ctx.fill(); ctx.fillStyle = '#fff'; ctx.font = '10px sans-serif'; ctx.fillText((m.name || ('#' + m.id)).slice(0, 10), m.x + 10, m.y + 3); };
      for (const m of s.markers) dot(m, '#34d399');
      for (const m of s.mobs) dot(m, '#f59e0b');
      if (s.showGrid) { ctx.strokeStyle = 'rgba(255,255,255,.06)'; ctx.lineWidth = 1; ctx.beginPath(); for (let c = 0; c <= W; c++) { ctx.moveTo(c * CELL, 0); ctx.lineTo(c * CELL, H * CELL); } for (let r = 0; r <= H; r++) { ctx.moveTo(0, r * CELL); ctx.lineTo(W * CELL, r * CELL); } ctx.stroke(); }
      if (s.sel) { const it: any = s.sel.t === 'portal' ? s.portals[s.sel.i] : s.sel.t === 'npc' ? s.markers[s.sel.i] : s.sel.t === 'mob' ? s.mobs[s.sel.i] : s.objects[s.sel.i]; if (it) { ctx.strokeStyle = '#fbbf24'; ctx.lineWidth = 2; if (s.sel.t === 'portal') ctx.strokeRect(it.x - 2, it.y - 2, it.w + 4, it.h + 4); else ctx.strokeRect(it.x - 18, it.y - 18, 36, 36); } }
      raf = requestAnimationFrame(draw);
    };
    raf = requestAnimationFrame(draw);
    return () => cancelAnimationFrame(raf);
  }, [W, H, palette]);

  const buildLayout = () => ({ w: W, h: H, tileset, tiles, waterset, water, collision, objects, bg: objects, npcs: markers, monsters: mobs, portals });
  const doExport = (kind: 'sql' | 'json') => {
    const L = buildLayout(); const j = JSON.stringify(L);
    if (kind === 'json') setExp(JSON.stringify(L, null, 2));
    else setExp(`UPDATE maps SET layout_json='${j.replace(/'/g, "''")}', width=${W}, height=${H} WHERE id=${map.id};`);
  };

  const save = async () => {
    setBusy(true);
    const r = await api('/api/maps', 'POST', { action: 'upsert', ...map, width: W, height: H, layout_json: JSON.stringify(buildLayout()) });
    setMsg(r?.error ? `Loi: ${r.error}` : 'Da luu map (tile/water/collision/objects/npc/mob/portal)');
    setBusy(false);
  };
  const applyGame = async () => {
    setBusy(true);
    for (const mk of markers) { const full = npcs.find(n => n.id === mk.id); if (full) await api('/api/npcs', 'POST', { ...full, pos_x: mk.x, pos_y: mk.y, map_id: map.id }); }
    const ex = await api(`/api/portals?map_id=${map.id}`);
    for (const p of (ex?.portals || [])) await api('/api/portals', 'POST', { action: 'delete', id: p.id });
    for (const p of portals) await api('/api/portals', 'POST', { action: 'create', from_map_id: map.id, to_map_id: p.dest_map || 0, from_x: p.x, from_y: p.y, to_x: p.dest_x || 0, to_y: p.dest_y || 0, min_level: 0 });
    await save();
    setMsg(`Da ap dung game: ${markers.length} NPC + ${portals.length} portal (nho Reload map o admin)`);
    setBusy(false);
  };

  const palTab = layer === 'water' ? 'water' : 'tile';
  const showPal = tool === 'brush' || tool === 'object' || (tool === 'fill' && layer !== 'collision');

  return (
    <div className="w-full h-full flex gap-3 text-xs">
      {/* TOOLS + PALETTE */}
      <div className="w-56 flex flex-col gap-2">
        <div className="grid grid-cols-4 gap-1">
          {(['brush', 'erase', 'fill', 'select', 'object', 'npc', 'mob', 'portal'] as const).map(t => (
            <button key={t} onClick={() => setTool(t)} className={`py-1.5 rounded ${tool === t ? 'bg-brand-500 text-white' : 'bg-white/5 text-surface-200/70'}`}>
              {t === 'brush' ? 'Ve' : t === 'erase' ? 'Tay' : t === 'fill' ? 'Do' : t === 'select' ? 'Chon' : t === 'object' ? 'Obj' : t === 'npc' ? 'NPC' : t === 'mob' ? 'Mob' : 'Cong'}</button>
          ))}
        </div>
        <div className="flex gap-1">
          {(['tile', 'water', 'collision'] as const).map(l => (
            <button key={l} onClick={() => setLayer(l)} className={`flex-1 py-1 rounded ${layer === l ? 'bg-surface-700 text-brand-200' : 'bg-white/5 text-surface-200/60'}`}>
              {l === 'tile' ? 'Tile' : l === 'water' ? 'Water' : 'Chan'}</button>
          ))}
        </div>
        <div className="flex gap-1">
          <button onClick={undo} className="badge bg-white/5 flex-1">Undo</button>
          <button onClick={redo} className="badge bg-white/5 flex-1">Redo</button>
          <button onClick={delSel} className="badge bg-red-500/15 text-red-300 flex-1">Xoa chon</button>
        </div>
        <div className="flex gap-2 text-[10px] text-surface-200/60">
          <label className="flex items-center gap-1"><input type="checkbox" checked={showColl} onChange={e => setShowColl(e.target.checked)} className="accent-brand-500" />Chan</label>
          <label className="flex items-center gap-1"><input type="checkbox" checked={showGrid} onChange={e => setShowGrid(e.target.checked)} className="accent-brand-500" />Luoi</label>
          <label className="flex items-center gap-1">Zoom<input type="range" min={0.5} max={2} step={0.25} value={zoom} onChange={e => setZoom(+e.target.value)} className="accent-brand-500 w-16" /></label>
        </div>

        {showPal && (
          <div className="flex-1 overflow-y-auto card p-2 grid grid-cols-3 gap-1">
            {palette.filter(p => tool === 'object' ? true : (palTab === 'water' ? /water|nuoc/i.test(p.category || '') : true)).map(p => (
              <button key={p.asset_key} onClick={() => setPalKey(p.asset_key)} title={p.asset_key}
                className={`aspect-square rounded bg-surface-950 overflow-hidden border ${palKey === p.asset_key ? 'border-brand-500' : 'border-white/10'}`}>
                <img src={assetUrl(p.file_path)} alt="" className="w-full h-full object-contain" style={{ imageRendering: 'pixelated' }} />
              </button>
            ))}
          </div>
        )}
        {tool === 'npc' && <select value={npcId} onChange={e => setNpcId(+e.target.value)} className="input !py-1"><option value={0}>-- NPC --</option>{npcs.map(n => <option key={n.id} value={n.id}>{n.id} · {n.name}</option>)}</select>}
        {tool === 'mob' && <select value={mobId} onChange={e => setMobId(+e.target.value)} className="input !py-1"><option value={0}>-- Mob --</option>{mobsCat.map(n => <option key={n.id} value={n.id}>{n.id} · {n.name}</option>)}</select>}

        <div className="flex gap-1 mt-auto">
          <button onClick={save} disabled={busy} className="btn-gold flex-1 !py-1.5">{busy ? '...' : 'Luu'}</button>
          <button onClick={applyGame} disabled={busy} className="btn-secondary flex-1 !py-1.5">Ap game</button>
        </div>
        <div className="flex gap-1">
          <button onClick={() => doExport('sql')} className="badge bg-white/5 flex-1">Export SQL</button>
          <button onClick={() => doExport('json')} className="badge bg-white/5 flex-1">Export JSON</button>
        </div>
      </div>

      {/* CANVAS */}
      <div className="flex-1 overflow-auto rounded-xl border border-white/10 bg-surface-950" style={{ maxHeight: '70vh' }}>
        <canvas ref={cvRef} width={W * CELL} height={H * CELL}
          onMouseDown={onDown} onMouseMove={onMoveCanvas} onMouseUp={onUp} onMouseLeave={onUp}
          style={{ width: W * CELL * zoom, height: H * CELL * zoom, imageRendering: 'pixelated', cursor: tool === 'select' ? 'move' : 'crosshair' }} />
      </div>

      {/* EXPORT / SEL */}
      {(exp || sel) && (
        <div className="w-72 space-y-2">
          {sel && sel.t === 'portal' && (
            <div className="card p-2 space-y-1">
              <div className="text-[10px] uppercase text-surface-200/50">Portal</div>
              {(['dest_map', 'dest_x', 'dest_y'] as const).map(k => (
                <label key={k} className="flex items-center justify-between text-[11px] text-surface-200/70">{k}
                  <input type="number" value={(portals[sel.i] as any)[k]} onChange={e => setPortals(p => p.map((it, i) => i === sel.i ? { ...it, [k]: +e.target.value } : it))} className="input !py-0.5 !w-24" /></label>
              ))}
            </div>
          )}
          {exp && <textarea readOnly value={exp} onFocus={e => e.target.select()} className="input !text-[10px] font-mono w-full h-80" />}
        </div>
      )}
    </div>
  );
}
