import { useState, useEffect, useRef } from 'react';

// ═══════════════════════════════════════════════════════════════
// Landing Page — Nexus Isekai
// React + TypeScript + Tailwind CSS
// Tich hop vao webshop app, route "/"
// ═══════════════════════════════════════════════════════════════

const CLASS_DATA = [
  { id: 1, name: 'Kiem Si',  role: 'Tank / DPS',         desc: 'Chien binh can chien, phong ngu va tan cong can bang.', color: '#e94560' },
  { id: 2, name: 'Sat Thu',  role: 'Burst DPS',           desc: 'Diet dich nhanh, ne tranh cao, chi mang.',              color: '#8a5cf5' },
  { id: 3, name: 'Phap Su',  role: 'AoE DPS',             desc: 'Phap thuat tam xa, sat thuong dien rong.',              color: '#f0c050' },
  { id: 4, name: 'Phap Thu', role: 'Healer / Support',    desc: 'Hoi mau, tang buff, bao ve dong doi.',                 color: '#4ecca3' },
  { id: 5, name: 'Cung Thu', role: 'Ranged DPS',          desc: 'Tan cong tam xa, ban tinh, khu vuc.',                  color: '#44a5ff' },
];

const FEATURES = [
  { title: '5 Nhanh Nghe',       text: 'Moi class 30-40 skill doc quyen, 7 slot active, cot truyen rieng.' },
  { title: 'PvP Lien Server',    text: 'Duel 1v1 ELO rating, dau truong lien server, xep hang toan cau.' },
  { title: 'Chat Da Kenh',       text: 'Text, sticker, toa do, khoe item, li xi vang/diamond, voice.' },
  { title: 'Guild & Xa Hoi',     text: 'Lap guild, hen ho, ket hon, sinh con, su tu mentor/student.' },
  { title: 'Cuong Hoa +10',      text: 'Nang cap vu khi +1 den +10, ti le that bai tang dan.' },
  { title: 'Nong Trai & Nha O',  text: 'Trong cay, nuoi dong vat, xay nha, dat noi that.' },
  { title: 'Mission Pass',       text: 'Free + Premium pass, 30 level, nhiem vu hang ngay/tuan.' },
  { title: 'Su Kien Lien Tuc',   text: 'Double EXP, boss event, tien te su kien, minigame.' },
  { title: 'Trading & Dau Gia',  text: 'Giao dich item giua nguoi choi, nha dau gia toan server.' },
  { title: 'Party & Dungeon',    text: 'Nhom 4 nguoi, dungeon instance, boss mechanic phuc tap.' },
];

const NEWS = [
  { tag: 'Tin Tuc', title: 'Khai mo server Thien Ha — Dang ky nhan thuong tan thu', date: '30/05', type: 'news' },
  { tag: 'Su Kien', title: 'Su kien Hoa Sen mua He — Nhan token doi qua doc quyen', date: '28/05', type: 'event' },
  { tag: 'Tin Tuc', title: 'Cap nhat v1.2 — Trading, Auction House, Party System', date: '25/05', type: 'news' },
  { tag: 'Huong Dan', title: 'Bi kip cuong hoa vu khi +10 thanh cong', date: '22/05', type: 'guide' },
  { tag: 'Su Kien', title: 'PvP mua 3 — Dau truong Bien Dam mo cua', date: '15/05', type: 'event' },
  { tag: 'Tin Tuc', title: 'Tong hop Gift Code thang 5/2025', date: '10/05', type: 'news' },
];

// ═══════════════════════════════════════════════════════════════
// Particles Canvas Component
// ═══════════════════════════════════════════════════════════════

function ParticlesBg() {
  const canvasRef = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    const c = canvasRef.current;
    if (!c) return;
    const ctx = c.getContext('2d');
    if (!ctx) return;

    let animId: number;
    const pts: { x:number; y:number; r:number; dx:number; dy:number; a:number }[] = [];

    function resize() { c.width = c.offsetWidth; c.height = c.offsetHeight; }
    resize();
    window.addEventListener('resize', resize);

    for (let i = 0; i < 50; i++) {
      pts.push({
        x: Math.random() * c.width, y: Math.random() * c.height,
        r: Math.random() * 2 + 0.5, dx: (Math.random() - 0.5) * 0.4,
        dy: (Math.random() - 0.5) * 0.4, a: Math.random() * 0.35 + 0.05,
      });
    }

    function draw() {
      ctx!.clearRect(0, 0, c.width, c.height);
      for (const p of pts) {
        p.x += p.dx; p.y += p.dy;
        if (p.x < 0 || p.x > c.width) p.dx *= -1;
        if (p.y < 0 || p.y > c.height) p.dy *= -1;
        ctx!.beginPath();
        ctx!.arc(p.x, p.y, p.r, 0, Math.PI * 2);
        ctx!.fillStyle = `rgba(108,62,243,${p.a})`;
        ctx!.fill();
      }
      // Draw connections
      for (let i = 0; i < pts.length; i++) {
        for (let j = i + 1; j < pts.length; j++) {
          const dist = Math.hypot(pts[i].x - pts[j].x, pts[i].y - pts[j].y);
          if (dist < 120) {
            ctx!.beginPath();
            ctx!.moveTo(pts[i].x, pts[i].y);
            ctx!.lineTo(pts[j].x, pts[j].y);
            ctx!.strokeStyle = `rgba(108,62,243,${0.08 * (1 - dist / 120)})`;
            ctx!.lineWidth = 0.5;
            ctx!.stroke();
          }
        }
      }
      animId = requestAnimationFrame(draw);
    }
    draw();
    return () => { cancelAnimationFrame(animId); window.removeEventListener('resize', resize); };
  }, []);

  return <canvas ref={canvasRef} className="absolute inset-0 w-full h-full pointer-events-none" />;
}

// ═══════════════════════════════════════════════════════════════
// Section Components
// ═══════════════════════════════════════════════════════════════

function SectionTitle({ children, sub }: { children: React.ReactNode; sub?: string }) {
  return (
    <div className="text-center mb-12">
      <h2 className="text-3xl md:text-4xl font-bold text-white mb-3"
          style={{ fontFamily: "'Cinzel', 'Georgia', serif" }}>{children}</h2>
      {sub && <p className="text-sm text-gray-400 max-w-xl mx-auto">{sub}</p>}
    </div>
  );
}

// ═══════════════════════════════════════════════════════════════
// Main Landing Page
// ═══════════════════════════════════════════════════════════════

export default function LandingPage() {
  const [newsTab, setNewsTab] = useState('all');
  const [mobileMenu, setMobileMenu] = useState(false);

  const filteredNews = newsTab === 'all' ? NEWS : NEWS.filter(n => n.type === newsTab);

  return (
    <div className="min-h-screen bg-[#080818] text-gray-200">

      {/* ═══ NAV ═══ */}
      <nav className="fixed top-0 w-full z-50 bg-[#080818]/90 backdrop-blur-xl border-b border-white/5">
        <div className="max-w-7xl mx-auto flex items-center justify-between h-16 px-5">
          <a href="/" className="text-xl font-bold tracking-widest"
             style={{ fontFamily: "'Cinzel', serif" }}>
            <span className="text-[#f0c050]">NEXUS</span>
            <span className="text-[#8a5cf5] ml-1">ISEKAI</span>
          </a>

          {/* Desktop links */}
          <div className="hidden md:flex items-center gap-7">
            {[
              { href: '#features', label: 'Tinh Nang' },
              { href: '#news',     label: 'Tin Tuc' },
              { href: '#classes',  label: 'Nhanh Nghe' },
              { href: '#download', label: 'Tai Game' },
            ].map(l => (
              <a key={l.href} href={l.href}
                 className="text-xs uppercase tracking-wider text-gray-400 hover:text-[#f0c050] transition-colors font-medium">
                {l.label}
              </a>
            ))}
            <a href="/topup"
               className="text-xs uppercase tracking-wider font-semibold px-5 py-2 rounded-lg bg-[#6c3ef3] text-white hover:bg-[#8a5cf5] transition-all hover:-translate-y-0.5 shadow-lg shadow-purple-900/30">
              Nap Tien
            </a>
          </div>

          {/* Mobile toggle */}
          <button className="md:hidden text-gray-300 text-2xl" onClick={() => setMobileMenu(!mobileMenu)}>
            {mobileMenu ? '\u2715' : '\u2630'}
          </button>
        </div>

        {/* Mobile menu */}
        {mobileMenu && (
          <div className="md:hidden bg-[#0a0a20] border-t border-white/5 px-5 py-4 flex flex-col gap-3">
            {['#features|Tinh Nang','#news|Tin Tuc','#classes|Nhanh Nghe','#download|Tai Game'].map(s => {
              const [href, label] = s.split('|');
              return <a key={href} href={href} onClick={() => setMobileMenu(false)}
                        className="text-sm text-gray-400 hover:text-white py-1">{label}</a>;
            })}
            <a href="/topup" className="text-sm font-semibold text-[#6c3ef3] py-1">Nap Tien</a>
          </div>
        )}
      </nav>

      {/* ═══ HERO ═══ */}
      <section id="hero" className="relative min-h-screen flex items-center justify-center pt-16 overflow-hidden">
        <ParticlesBg />
        <div className="absolute inset-0 bg-gradient-to-b from-[#080818] via-[#0a0a28]/80 to-[#12122a]" />
        {/* Radial glows */}
        <div className="absolute top-1/4 left-1/4 w-96 h-96 bg-purple-600/10 rounded-full blur-3xl" />
        <div className="absolute bottom-1/3 right-1/4 w-80 h-80 bg-teal-500/8 rounded-full blur-3xl" />

        <div className="relative z-10 text-center px-6 max-w-3xl">
          {/* Logo placeholder */}
          <div className="w-40 h-40 mx-auto mb-6 rounded-2xl bg-gradient-to-br from-[#6c3ef3] to-[#f0c050] flex items-center justify-center shadow-2xl shadow-purple-900/40"
               style={{ fontFamily: "'Cinzel', serif" }}>
            {/* Thay bang: <img src="/assets/logo.png" /> */}
            <span className="text-5xl font-black text-white tracking-wider">NI</span>
          </div>

          <h1 className="text-4xl sm:text-5xl md:text-6xl font-black text-white mb-3 leading-tight"
              style={{ fontFamily: "'Cinzel', serif", textShadow: '0 4px 30px rgba(108,62,243,0.3)' }}>
            Nexus Isekai
          </h1>
          <p className="text-[#f0c050] italic text-sm sm:text-base tracking-[0.3em] mb-6 uppercase">
            Vong Linh Gioi — The Realm Between Worlds
          </p>
          <p className="text-gray-400 text-sm sm:text-base max-w-lg mx-auto mb-10 leading-relaxed">
            Buoc vao the gioi huyen bi noi cac luc dia tu nhieu chieu khong gian hop nhat.
            Chien dau, xay dung guild, ket hon, va kham pha bi mat cua Tieu Than Azaroth.
          </p>

          <div className="flex flex-wrap gap-4 justify-center">
            <a href="#download"
               className="px-8 py-3.5 rounded-xl bg-gradient-to-r from-[#6c3ef3] to-[#8a5cf5] text-white font-semibold text-sm shadow-lg shadow-purple-900/40 hover:-translate-y-1 transition-all">
              Tai Game Ngay
            </a>
            <a href="/topup"
               className="px-8 py-3.5 rounded-xl bg-gradient-to-r from-[#d4a43a] to-[#f0c050] text-[#1a1a2e] font-semibold text-sm shadow-lg shadow-yellow-900/30 hover:-translate-y-1 transition-all">
              Nap Diamond
            </a>
            <a href="#features"
               className="px-8 py-3.5 rounded-xl border-2 border-[#4ecca3] text-[#4ecca3] font-semibold text-sm hover:bg-[#4ecca3] hover:text-[#080818] transition-all">
              Tim Hieu Them
            </a>
          </div>
        </div>

        {/* Scroll indicator */}
        <div className="absolute bottom-8 left-1/2 -translate-x-1/2 animate-bounce">
          <div className="w-6 h-10 rounded-full border-2 border-white/20 flex justify-center pt-2">
            <div className="w-1 h-2.5 bg-white/40 rounded-full" />
          </div>
        </div>
      </section>

      {/* ═══ FEATURES ═══ */}
      <section id="features" className="py-20 px-5 bg-[#0d0d24]">
        <div className="max-w-7xl mx-auto">
          <SectionTitle sub="Mot the gioi day du voi hang tram tinh nang cho nguoi choi">
            Tinh Nang Noi Bat
          </SectionTitle>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4">
            {FEATURES.map((f, i) => (
              <div key={i}
                   className="group bg-[#12122a] border border-white/5 rounded-2xl p-6 hover:border-[#6c3ef3]/50 hover:-translate-y-1 transition-all duration-300">
                {/* Icon placeholder */}
                <div className="w-11 h-11 rounded-xl bg-gradient-to-br from-[#6c3ef3] to-[#4ecca3] mb-4 opacity-80 group-hover:opacity-100 transition-opacity" />
                <h3 className="text-sm font-semibold text-white mb-2">{f.title}</h3>
                <p className="text-xs text-gray-500 leading-relaxed">{f.text}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ═══ NEWS ═══ */}
      <section id="news" className="py-20 px-5 bg-[#080818]">
        <div className="max-w-3xl mx-auto">
          <SectionTitle sub="Cap nhat moi nhat tu Vong Linh Gioi">Tin Tuc & Su Kien</SectionTitle>

          <div className="flex justify-center gap-1 mb-8">
            {[
              { key: 'all',   label: 'Tat Ca' },
              { key: 'news',  label: 'Tin Tuc' },
              { key: 'event', label: 'Su Kien' },
              { key: 'guide', label: 'Huong Dan' },
            ].map(t => (
              <button key={t.key} onClick={() => setNewsTab(t.key)}
                      className={`px-5 py-2 text-xs font-semibold uppercase tracking-wider transition-all rounded-lg ${
                        newsTab === t.key
                          ? 'bg-[#6c3ef3] text-white'
                          : 'text-gray-500 hover:text-white hover:bg-white/5'
                      }`}>
                {t.label}
              </button>
            ))}
          </div>

          <div className="space-y-0 divide-y divide-white/5">
            {filteredNews.map((n, i) => (
              <a key={i} href="#" className="flex items-center justify-between py-3.5 hover:pl-2 transition-all group">
                <div className="flex items-center gap-3">
                  <span className={`text-[10px] font-bold px-2 py-0.5 rounded ${
                    n.type === 'event' ? 'bg-red-600' : n.type === 'guide' ? 'bg-teal-600' : 'bg-[#6c3ef3]'
                  } text-white`}>{n.tag}</span>
                  <span className="text-sm text-gray-300 group-hover:text-white transition-colors">{n.title}</span>
                </div>
                <span className="text-xs text-gray-600 flex-shrink-0 ml-4">{n.date}</span>
              </a>
            ))}
          </div>
        </div>
      </section>

      {/* ═══ CLASSES ═══ */}
      <section id="classes" className="py-20 px-5 bg-[#0d0d24]">
        <div className="max-w-5xl mx-auto">
          <SectionTitle sub="Chon huong di rieng trong Vong Linh Gioi">5 Nhanh Nghe</SectionTitle>

          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-4">
            {CLASS_DATA.map(c => (
              <div key={c.id}
                   className="group text-center bg-[#12122a] border border-white/5 rounded-2xl p-5 hover:border-opacity-60 hover:-translate-y-1 transition-all duration-300"
                   style={{ '--ring-color': c.color } as React.CSSProperties}>
                {/* Avatar placeholder */}
                <div className="w-16 h-16 rounded-full mx-auto mb-3 flex items-center justify-center text-xl font-bold text-white"
                     style={{ border: `3px solid ${c.color}`, background: `${c.color}20` }}>
                  {/* Thay bang sprite: <img src={`/assets/class_${c.id}.png`} /> */}
                  {c.name[0]}
                </div>
                <h3 className="text-sm font-semibold text-white mb-1">{c.name}</h3>
                <div className="text-[10px] font-medium mb-2" style={{ color: c.color }}>{c.role}</div>
                <p className="text-xs text-gray-500 leading-relaxed">{c.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ═══ SCREENSHOTS ═══ */}
      <section className="py-20 px-5 bg-[#080818]">
        <div className="max-w-6xl mx-auto">
          <SectionTitle sub="Kham pha the gioi game qua hinh anh">Hinh Anh</SectionTitle>

          <div className="grid grid-cols-2 md:grid-cols-3 gap-3">
            {['Login', 'The Gioi', 'Boss Fight', 'Chat & Li Xi', 'PvP Arena', 'Nong Trai'].map((label, i) => (
              <div key={i}
                   className="aspect-video bg-[#12122a] border border-white/5 rounded-xl flex items-center justify-center text-xs text-gray-600 hover:border-[#6c3ef3]/40 hover:scale-[1.02] transition-all">
                {/* Thay bang: <img src={`/assets/ss_${i+1}.jpg`} className="w-full h-full object-cover rounded-xl" /> */}
                {label}
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ═══ DOWNLOAD ═══ */}
      <section id="download" className="py-20 px-5 bg-gradient-to-b from-[#0d0d24] to-[#080818]">
        <div className="max-w-4xl mx-auto text-center">
          <SectionTitle sub="Choi mien phi tren moi nen tang">Tai Game</SectionTitle>

          <div className="flex flex-wrap justify-center gap-5 mt-10">
            {[
              { name: 'Android', sub: 'APK Download', href: '/download/NexusIsekai.apk', active: true },
              { name: 'iOS',     sub: 'App Store',    href: '#', active: false },
              { name: 'PC',      sub: 'Windows JAR',  href: '/download/NexusIsekai-PC.jar', active: true },
              { name: 'Web',     sub: 'Choi tren trinh duyet', href: '/play', active: true },
            ].map((d, i) => (
              <div key={i} className="w-44 bg-[#12122a] border border-white/5 rounded-2xl p-6 text-center">
                {/* Platform icon placeholder */}
                <div className="w-12 h-12 rounded-xl bg-white/5 mx-auto mb-3 flex items-center justify-center text-lg font-bold text-gray-400">
                  {d.name[0]}
                </div>
                <h3 className="text-sm font-semibold text-white mb-1">{d.name}</h3>
                <p className="text-[10px] text-gray-500 mb-4">{d.sub}</p>
                {d.active ? (
                  <a href={d.href}
                     className="block w-full py-2 rounded-lg bg-[#6c3ef3] text-white text-xs font-semibold hover:bg-[#8a5cf5] transition-colors">
                    Tai Ngay
                  </a>
                ) : (
                  <span className="block w-full py-2 rounded-lg border border-white/10 text-gray-500 text-xs">
                    Sap Ra Mat
                  </span>
                )}
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ═══ SUPPORT ═══ */}
      <section className="py-20 px-5 bg-[#0d0d24]">
        <div className="max-w-4xl mx-auto">
          <SectionTitle sub="Can giup do? Lien he ngay">Ho Tro</SectionTitle>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            {[
              { title: 'Fanpage',  desc: 'Tin tuc va su kien moi nhat',     btn: 'Facebook',    href: '#', style: 'border' },
              { title: 'Zalo',     desc: 'Cong dong nguoi choi, ho tro nhanh', btn: 'Tham Gia', href: '#', style: 'border' },
              { title: 'Nap Tien', desc: 'Cong tu dong qua QR ngan hang',   btn: 'Webshop',     href: '/topup', style: 'gold' },
              { title: 'Bao Loi',  desc: 'Gui bao cao loi ve team dev',     btn: 'Email',       href: 'mailto:support@nexusisekai.vn', style: 'border' },
            ].map((s, i) => (
              <div key={i} className="bg-[#12122a] border border-white/5 rounded-2xl p-6 text-center">
                <h3 className="text-sm font-semibold text-white mb-2">{s.title}</h3>
                <p className="text-xs text-gray-500 mb-4">{s.desc}</p>
                <a href={s.href}
                   className={`inline-block px-5 py-2 rounded-lg text-xs font-semibold transition-all ${
                     s.style === 'gold'
                       ? 'bg-gradient-to-r from-[#d4a43a] to-[#f0c050] text-[#1a1a2e]'
                       : 'border border-[#4ecca3] text-[#4ecca3] hover:bg-[#4ecca3] hover:text-[#080818]'
                   }`}>
                  {s.btn}
                </a>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ═══ FOOTER ═══ */}
      <footer className="bg-[#050510] border-t border-white/5 py-10 px-5">
        <div className="max-w-7xl mx-auto flex flex-col md:flex-row items-center justify-between gap-6">
          <span className="text-lg font-bold tracking-widest" style={{ fontFamily: "'Cinzel', serif" }}>
            <span className="text-[#f0c050]">NEXUS</span> <span className="text-[#8a5cf5]">ISEKAI</span>
          </span>
          <div className="flex flex-wrap gap-6 text-xs text-gray-500">
            <a href="#features" className="hover:text-white transition-colors">Tinh Nang</a>
            <a href="#news" className="hover:text-white transition-colors">Tin Tuc</a>
            <a href="#download" className="hover:text-white transition-colors">Tai Game</a>
            <a href="/topup" className="hover:text-white transition-colors">Nap Tien</a>
            <a href="https://github.com/thanhtinz/Nexus-Isekai" target="_blank" rel="noreferrer"
               className="hover:text-white transition-colors">GitHub</a>
          </div>
        </div>
        <p className="text-center text-[10px] text-gray-700 mt-6">
          2025 Nexus Isekai Team. MIT License. Fan site — Community.
        </p>
      </footer>
    </div>
  );
}
