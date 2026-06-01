import { useEffect, useRef, useState } from 'react';

/* ── Spine Viewer ─────────────────────────────────────────────────────
   Nap spine-player (runtime web cua Esoteric) tu CDN → chon 3 file Spine da export
   (.json + .atlas + .png) tu Spine Editor → xem animation, doi skin/animation, chinh toc do.
   Studio KHONG tao rigging Spine (phai dung Spine Editor); day la cong cu XEM/DUYET + luu KHO. */

const SPINE_VER = '4.2'; // doi cho khop ban export tu Spine Editor (vd '4.1', '4.2'). unpkg lay ban moi nhat dong nay.
const JS_URL = `https://unpkg.com/@esotericsoftware/spine-player@${SPINE_VER}/dist/iife/spine-player.js`;
const CSS_URL = `https://unpkg.com/@esotericsoftware/spine-player@${SPINE_VER}/dist/spine-player.css`;

let loaderPromise: Promise<any> | null = null;
function loadSpine(): Promise<any> {
  if ((window as any).spine) return Promise.resolve((window as any).spine);
  if (loaderPromise) return loaderPromise;
  loaderPromise = new Promise((resolve, reject) => {
    const css = document.createElement('link'); css.rel = 'stylesheet'; css.href = CSS_URL; document.head.appendChild(css);
    const s = document.createElement('script'); s.src = JS_URL;
    s.onload = () => resolve((window as any).spine);
    s.onerror = () => reject(new Error('Khong tai duoc spine-player runtime (kiem tra mang/CDN)'));
    document.head.appendChild(s);
  });
  return loaderPromise;
}

const readText = (f: File) => new Promise<string>((res, rej) => { const r = new FileReader(); r.onload = () => res(String(r.result)); r.onerror = rej; r.readAsText(f); });
const readDataUrl = (f: File) => new Promise<string>((res, rej) => { const r = new FileReader(); r.onload = () => res(String(r.result)); r.onerror = rej; r.readAsDataURL(f); });
const toDataUri = (text: string, mime: string) => `data:${mime};base64,` + btoa(unescape(encodeURIComponent(text)));

export default function SpineViewer({ setMsg }: { setMsg: (s: string) => void }) {
  const holder = useRef<HTMLDivElement>(null);
  const playerRef = useRef<any>(null);
  const [jsonF, setJsonF] = useState<File | null>(null);
  const [atlasF, setAtlasF] = useState<File | null>(null);
  const [pngFs, setPngFs] = useState<File[]>([]);
  const [anims, setAnims] = useState<string[]>([]);
  const [skins, setSkins] = useState<string[]>([]);
  const [anim, setAnim] = useState('');
  const [skin, setSkin] = useState('');
  const [speed, setSpeed] = useState(1);
  const [err, setErr] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => () => { try { playerRef.current?.dispose(); } catch { /* */ } }, []);

  const load = async () => {
    setErr('');
    if (!jsonF || !atlasF || pngFs.length === 0) { setErr('Chon du 3 file: .json (skeleton) + .atlas + .png'); return; }
    setLoading(true);
    try {
      const spine = await loadSpine();
      const jsonText = await readText(jsonF);
      const atlasText = await readText(atlasF);
      const pngUris: Record<string, string> = {};
      for (const f of pngFs) pngUris[f.name] = await readDataUrl(f);

      // ten anh trong atlas (cac dong ket thuc .png) → map sang png da chon
      const imgNames = atlasText.split('\n').map(l => l.trim()).filter(l => /\.png$/i.test(l));
      const raw: Record<string, string> = {
        [jsonF.name]: toDataUri(jsonText, 'application/json'),
        [atlasF.name]: toDataUri(atlasText, 'text/plain'),
      };
      imgNames.forEach((nm, i) => { raw[nm] = pngUris[nm] || Object.values(pngUris)[i] || Object.values(pngUris)[0]; });
      // dam bao moi png cung co mat theo ten goc
      for (const f of pngFs) raw[f.name] = pngUris[f.name];

      try { playerRef.current?.dispose(); } catch { /* */ }
      if (holder.current) holder.current.innerHTML = '';

      playerRef.current = new spine.SpinePlayer(holder.current, {
        jsonUrl: jsonF.name,
        atlasUrl: atlasF.name,
        rawDataURIs: raw,
        premultipliedAlpha: true,
        backgroundColor: '#0b0b12',
        alpha: true,
        showControls: true,
        success: (player: any) => {
          const data = player.skeleton.data;
          const an = data.animations.map((a: any) => a.name);
          const sk = data.skins.map((s: any) => s.name);
          setAnims(an); setSkins(sk);
          setAnim(an[0] || ''); setSkin(sk[0] || 'default');
          setLoading(false);
          setMsg(`Spine OK: ${an.length} animation, ${sk.length} skin`);
        },
        error: (_p: any, reason: any) => {
          setLoading(false);
          setErr('Loi nap Spine: ' + (reason || 'co the do phien ban runtime khong khop ban export. Sua SPINE_VER.'));
        },
      });
    } catch (e: any) { setLoading(false); setErr(e?.message || 'Loi'); }
  };

  const playAnim = (name: string) => { setAnim(name); try { playerRef.current?.setAnimation(name, true); } catch { /* */ } };
  const setSkinName = (name: string) => {
    setSkin(name);
    try { const sk = playerRef.current.skeleton; sk.setSkinByName(name); sk.setSlotsToSetupPose(); } catch { /* */ }
  };
  const changeSpeed = (v: number) => { setSpeed(v); try { playerRef.current.animationState.timeScale = v; } catch { /* */ } };

  return (
    <div className="w-full h-full flex gap-4">
      <div className="flex-1 flex flex-col">
        <div ref={holder} className="flex-1 min-h-[360px] rounded-xl border border-white/10 overflow-hidden" style={{ background: '#0b0b12' }} />
        {err && <div className="mt-2 text-xs px-3 py-2 rounded-lg bg-red-500/15 text-red-300 border border-red-500/30">{err}</div>}
      </div>

      <div className="w-72 space-y-3 overflow-y-auto pr-1">
        <p className="text-[11px] text-surface-200/60 leading-relaxed">
          Export tu <b>Spine Editor</b> ra <b>.json + .atlas + .png</b> roi chon o duoi. Studio chi XEM/DUYET
          (khong tao rigging). Phien ban runtime: <b>{SPINE_VER}</b> — sua trong code neu ban export khac.
        </p>
        <label className="block text-[11px] text-surface-200/70">skeleton .json
          <input type="file" accept=".json" onChange={e => setJsonF(e.target.files?.[0] || null)} className="block w-full text-[11px] mt-1" /></label>
        <label className="block text-[11px] text-surface-200/70">.atlas
          <input type="file" accept=".atlas,.txt" onChange={e => setAtlasF(e.target.files?.[0] || null)} className="block w-full text-[11px] mt-1" /></label>
        <label className="block text-[11px] text-surface-200/70">texture .png (1+)
          <input type="file" accept=".png" multiple onChange={e => setPngFs(Array.from(e.target.files || []))} className="block w-full text-[11px] mt-1" /></label>
        <button onClick={load} disabled={loading} className="btn-gold w-full !py-2 text-xs">{loading ? 'Dang nap...' : 'Nap & Xem'}</button>

        {anims.length > 0 && (
          <>
            <label className="block text-[11px] text-surface-200/70">Animation
              <select value={anim} onChange={e => playAnim(e.target.value)} className="input !py-1 text-xs mt-1">
                {anims.map(a => <option key={a} value={a}>{a}</option>)}</select></label>
            <label className="block text-[11px] text-surface-200/70">Skin
              <select value={skin} onChange={e => setSkinName(e.target.value)} className="input !py-1 text-xs mt-1">
                {skins.map(s => <option key={s} value={s}>{s}</option>)}</select></label>
            <label className="block text-[11px] text-surface-200/70">
              <div className="flex justify-between"><span>Toc do</span><span className="text-surface-100">{speed.toFixed(2)}x</span></div>
              <input type="range" min={0} max={3} step={0.05} value={speed} onChange={e => changeSpeed(Number(e.target.value))} className="w-full accent-brand-500" /></label>
          </>
        )}
      </div>
    </div>
  );
}
