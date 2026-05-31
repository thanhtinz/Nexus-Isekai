import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { http } from '@/api/client'
import { Menu, X } from 'lucide-react'

/**
 * LandingPage — Vọng Linh Giới (Nexus Isekai)
 * Layout NSO, theme SÁNG fantasy. DỮ LIỆU LẤY TỪ API (không hardcode).
 * Responsive đa nền tảng. Đăng ký trong game (web chỉ đăng nhập).
 */

const palette = {
  ink: '#2a2350', inkSoft: '#5b5380', violet: '#6c3ef3', violetDeep: '#4820b8',
  gold: '#e0a020', goldSoft: '#f5c451',
}

interface Server { id: number; name: string; group_name?: string; online_count?: number }
interface NewsItem { id: number; title: string; category: string; summary?: string; published_at?: string }
interface DownloadLink { platform: string; label?: string; description?: string; url: string }
interface SocialLink { platform: string; display_name: string; url: string; color?: string }
interface RankRow { rank: number; name: string; value: number }

export default function LandingPage() {
  const [shown, setShown] = useState(false)
  const [mobileOpen, setMobileOpen] = useState(false)
  const [servers, setServers] = useState<Server[]>([])
  const [selectedServer, setSelectedServer] = useState<number>(0)
  const [news, setNews] = useState<NewsItem[]>([])
  const [downloads, setDownloads] = useState<DownloadLink[]>([])
  const [socials, setSocials] = useState<SocialLink[]>([])
  const [rankTab, setRankTab] = useState<'level' | 'pvp' | 'guild' | 'wealth'>('level')
  const [ranks, setRanks] = useState<RankRow[]>([])

  useEffect(() => { const t = setTimeout(() => setShown(true), 50); return () => clearTimeout(t) }, [])

  // Fetch servers (chọn server cho BXH)
  useEffect(() => {
    http.get('/servers').then((r: any) => {
      const list: Server[] = (r.data?.servers || []).filter((s: any) => s.status === 1 || s.status === undefined)
      setServers(list)
      if (list.length) setSelectedServer(list[0].id)
    }).catch(() => {})
  }, [])

  // Fetch news + downloads + socials (content tạo trong admin)
  useEffect(() => {
    http.get('/news-articles').then((r: any) => setNews(r.data?.articles || r.data?.news || [])).catch(() => {})
    http.get('/download-links').then((r: any) => setDownloads(r.data?.links || [])).catch(() => {})
    http.get('/social-links').then((r: any) => setSocials(r.data?.links || [])).catch(() => {})
  }, [])

  // Fetch ranking theo server + tab đã chọn
  useEffect(() => {
    if (!selectedServer) return
    http.get('/ranking', { params: { type: rankTab, server_id: selectedServer } })
      .then((r: any) => setRanks(r.data?.ranking || r.data?.rows || []))
      .catch(() => setRanks([]))
  }, [rankTab, selectedServer])

  const activeServer = servers.find(s => s.id === selectedServer)
  const onlineCount = activeServer?.online_count ?? 0

  const stats = [
    { label: 'Đang Online', value: String(onlineCount) },
    { label: 'Số Server', value: String(servers.length || '—') },
    { label: 'Phiên Bản', value: '1.0' },
    { label: 'Rate', value: 'x1' },
  ]

  const nav = ['Trang Chủ', 'Tải Game', 'Tính Năng', 'Tin Tức', 'BXH', 'Nạp Thẻ', 'Hỗ Trợ']
  const features = [
    { tag: 'PvP', title: 'Đấu Trường Sinh Tồn', desc: 'Ghép trận nhanh, leo hạng theo mùa, phần thưởng rõ ràng.' },
    { tag: 'Bảo Mật', title: 'Bảo Vệ Tài Khoản', desc: 'Mã hoá, lịch sử thao tác, khuyến nghị đổi mật khẩu.' },
    { tag: 'Tiện Ích', title: 'Mua Lại Vật Phẩm', desc: 'Khôi phục nhầm lẫn trong giới hạn cấu hình server.' },
    { tag: 'Liên Server', title: 'Chiến Trường Liên Server', desc: 'Mở mỗi tuần, tích điểm toàn cõi giới, vinh danh.' },
    { tag: 'PvE', title: 'Phó Bản & Boss Ngày', desc: 'Chuỗi nhiệm vụ, boss giờ vàng, quà hoàn thành mỗi ngày.' },
    { tag: 'Hoạt Động', title: 'Sự Kiện Nạp & Tích Luỹ', desc: 'Mốc nạp minh bạch, ưu đãi theo mùa, lịch sử dễ kiểm tra.' },
  ]
  const steps = [
    { n: 1, title: 'Tải Game', desc: 'Chọn nền tảng phù hợp thiết bị của bạn.' },
    { n: 2, title: 'Tạo Nhân Vật', desc: 'Đăng ký & tạo nhân vật ngay trong game.' },
    { n: 3, title: 'Đăng Nhập Web', desc: 'Dùng tài khoản game để nạp & nhận quà.' },
    { n: 4, title: 'Nhận Quà', desc: 'Theo dõi sự kiện tân thủ, điểm danh mỗi ngày.' },
  ]
  const rankTabs: [typeof rankTab, string][] = [['level', 'Top Cấp Độ'], ['pvp', 'Top Lực Chiến'], ['wealth', 'Top Nạp'], ['guild', 'Bang Hội']]

  const card = 'bg-[#fffdf7] border border-[#ece3d0] rounded-2xl shadow-[0_8px_24px_-18px_rgba(42,35,80,.4)]'

  return (
    <div className="min-h-screen" style={{ background: '#fbf7ee', color: palette.ink, fontFamily: "'Be Vietnam Pro', sans-serif" }}>
      {/* ambient mesh */}
      <div className="fixed inset-0 pointer-events-none z-0" style={{ opacity: .5,
        background: `radial-gradient(620px circle at 12% 8%, rgba(108,62,243,.10), transparent 60%), radial-gradient(560px circle at 88% 4%, rgba(224,160,32,.12), transparent 55%)` }} />

      <div className="relative z-10 max-w-6xl mx-auto px-4 sm:px-6">

        {/* HEADER */}
        <header className="flex items-center justify-between py-5 gap-3 relative">
          <div className="flex items-center gap-3">
            <div className="w-11 h-11 rounded-xl grid place-items-center text-white font-extrabold"
              style={{ background: `linear-gradient(135deg,${palette.violet},${palette.violetDeep})`, fontFamily: "'Cinzel',serif", boxShadow: '0 6px 18px rgba(108,62,243,.35)' }}>VL</div>
            <div className="leading-tight">
              <div className="font-extrabold text-base sm:text-lg tracking-wide" style={{ fontFamily: "'Cinzel',serif" }}>VỌNG LINH GIỚI</div>
              <div className="text-[11px] tracking-[2px]" style={{ color: palette.inkSoft }}>NEXUS ISEKAI</div>
            </div>
          </div>

          {/* Desktop nav */}
          <nav className="hidden lg:flex gap-5 items-center">
            {nav.map((n, i) => (<a key={n} href="#" className="text-sm font-semibold no-underline pb-1"
              style={{ color: i === 0 ? palette.violet : palette.inkSoft, borderBottom: i === 0 ? `2px solid ${palette.gold}` : '2px solid transparent' }}>{n}</a>))}
          </nav>

          <div className="flex items-center gap-2">
            <Link to="/login" className="text-sm font-bold px-4 sm:px-5 py-2 rounded-xl text-white no-underline whitespace-nowrap"
              style={{ background: `linear-gradient(135deg,${palette.violet},${palette.violetDeep})` }}>Đăng Nhập</Link>
            {/* Hamburger — chỉ mobile/tablet */}
            <button className="lg:hidden p-2 rounded-lg border" onClick={() => setMobileOpen(v => !v)} aria-label="Menu"
              style={{ borderColor: '#ece3d0', color: palette.ink, background: '#fffdf7' }}>
              {mobileOpen ? <X size={20} /> : <Menu size={20} />}
            </button>
          </div>

          {/* Mobile dropdown menu */}
          {mobileOpen && (
            <div className="lg:hidden absolute top-full right-0 left-0 mt-1 rounded-2xl border p-2 z-50 shadow-xl"
              style={{ background: '#fffdf7', borderColor: '#ece3d0' }}>
              {nav.map((n, i) => (
                <a key={n} href="#" onClick={() => setMobileOpen(false)}
                  className="block px-4 py-3 rounded-xl text-sm font-semibold no-underline"
                  style={{ color: i === 0 ? palette.violet : palette.inkSoft, background: i === 0 ? '#6c3ef310' : 'transparent' }}>{n}</a>
              ))}
            </div>
          )}
        </header>

        {/* HERO */}
        <section className="grid lg:grid-cols-2 gap-8 items-center py-6 transition-all duration-700"
          style={{ opacity: shown ? 1 : 0, transform: shown ? 'none' : 'translateY(16px)' }}>
          <div>
            <h1 className="font-extrabold text-5xl sm:text-6xl leading-none m-0" style={{ fontFamily: "'Cinzel',serif",
              background: `linear-gradient(135deg,${palette.ink},${palette.violet})`, WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}>
              VỌNG<br />LINH GIỚI
            </h1>
            <p className="text-base mt-4 max-w-md leading-relaxed" style={{ color: palette.inkSoft }}>
              Cõi giới giữa các thế giới — nơi Lưu Dân từ muôn phương hội tụ. Máy chủ ổn định, hoạt động mỗi ngày. Cày cuốc hợp lý, nạp an toàn.
            </p>
            <div className="flex flex-wrap gap-3 mt-6">
              <a href="#download" className="px-5 py-3 rounded-xl font-bold no-underline text-sm" style={{ background: `linear-gradient(135deg,${palette.gold},${palette.goldSoft})`, color: '#3a2a05' }}>Tải Game Ngay</a>
              <Link to="/store" className="px-5 py-3 rounded-xl font-bold no-underline text-sm border" style={{ background: '#fffdf7', color: palette.inkSoft, borderColor: '#ece3d0' }}>Nạp Thẻ</Link>
            </div>
            {/* note: đăng ký trong game */}
            <p className="text-xs mt-3" style={{ color: palette.inkSoft }}>Chưa có tài khoản? Tạo nhân vật & đăng ký ngay trong game sau khi tải.</p>
            <div className="flex mt-7 border rounded-xl overflow-hidden" style={{ borderColor: '#ece3d0', background: '#fffdf7' }}>
              {stats.map((s, i) => (<div key={s.label} className="flex-1 px-3 py-3" style={{ borderRight: i < stats.length - 1 ? '1px solid #ece3d0' : 'none' }}>
                <div className="text-[10px] uppercase tracking-wide" style={{ color: palette.inkSoft }}>{s.label}</div>
                <div className="text-lg font-bold mt-1" style={{ fontFamily: "'Philosopher',serif", color: palette.violet }}>{s.value}</div>
              </div>))}
            </div>
          </div>
          <div className="relative rounded-2xl overflow-hidden min-h-[280px] border" style={{ borderColor: '#ece3d0', background: 'linear-gradient(160deg,#f3edff,#fff8e8)', boxShadow: '0 20px 50px -20px rgba(108,62,243,.35)' }}>
            <svg viewBox="0 0 400 320" className="absolute inset-0 w-full h-full" style={{ opacity: .5 }}>
              {[140, 110, 80, 50].map((r, i) => (<circle key={r} cx="200" cy="150" r={r} fill="none" stroke={i % 2 ? palette.gold : palette.violet} strokeWidth="1.5" strokeDasharray={i % 2 ? '4 8' : '2 6'} opacity={.6 - i * .1} />))}
            </svg>
            <div className="absolute top-4 left-4 right-4 px-4 py-3 rounded-xl border" style={{ background: 'rgba(255,255,255,.82)', borderColor: '#ece3d0', backdropFilter: 'blur(6px)' }}>
              <div className="flex items-center gap-2"><span className="w-2.5 h-2.5 rounded-full bg-green-500" style={{ boxShadow: '0 0 0 4px rgba(34,197,94,.18)' }} /><strong className="text-sm">Máy chủ đang hoạt động</strong></div>
              <p className="text-xs mt-1.5 leading-snug" style={{ color: palette.inkSoft }}>Trạng thái ổn định, hỗ trợ người chơi mới và cập nhật theo mùa.</p>
            </div>
            <div className="absolute bottom-5 left-0 right-0 text-center">
              <div className="font-extrabold text-4xl tracking-wider" style={{ fontFamily: "'Cinzel',serif", color: palette.violetDeep, opacity: .92 }}>NEXUS</div>
              <div className="text-sm tracking-[6px]" style={{ fontFamily: "'Philosopher',serif", color: palette.gold }}>ISEKAI ONLINE</div>
            </div>
          </div>
        </section>

        {/* DOWNLOAD (từ admin) */}
        <Section kicker="Client Chính Thức" title="Tải Game" action={<Link to="/download" className="text-xs font-semibold px-3 py-2 rounded-lg border no-underline" style={{ color: palette.inkSoft, borderColor: '#ece3d0' }}>Hướng Dẫn Cài Đặt</Link>}>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            {(downloads.length ? downloads : [{ platform: 'Java' }, { platform: 'PC' }, { platform: 'APK' }, { platform: 'iOS' }] as any).map((d: DownloadLink, i: number) => (
              <div key={i} className={`${card} p-4`}>
                <div className="w-11 h-11 rounded-xl grid place-items-center font-extrabold" style={{ background: 'linear-gradient(135deg,#6c3ef322,#e0a02022)', color: palette.violet, fontFamily: "'Cinzel',serif" }}>{d.platform[0]}</div>
                <h3 className="mt-3 mb-0.5 text-lg" style={{ fontFamily: "'Philosopher',serif" }}>{d.platform}</h3>
                <div className="text-xs" style={{ color: palette.inkSoft }}>{d.description || d.label || ''}</div>
                <a href={d.url || '/download'} className="block text-center mt-4 py-2.5 rounded-lg font-bold text-sm no-underline" style={{ background: `linear-gradient(135deg,${palette.gold},${palette.goldSoft})`, color: '#3a2a05' }}>Tải Ngay</a>
              </div>
            ))}
          </div>
        </Section>

        {/* FEATURES */}
        <Section kicker="Định Hướng Máy Chủ" title="Cày Cuốc Hợp Lý, Cạnh Tranh Rõ Ràng">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            {features.map(f => (<div key={f.title} className={`${card} overflow-hidden`}>
              <div className="h-20 relative" style={{ background: `linear-gradient(135deg,${palette.violet},${palette.gold})` }}>
                <span className="absolute top-3 left-3 text-[11px] font-bold px-2.5 py-0.5 rounded-full" style={{ color: palette.violetDeep, background: 'rgba(255,255,255,.9)' }}>{f.tag}</span>
              </div>
              <div className="p-4">
                <h3 className="mb-1.5 text-lg" style={{ fontFamily: "'Philosopher',serif" }}>{f.title}</h3>
                <p className="text-sm leading-relaxed m-0" style={{ color: palette.inkSoft }}>{f.desc}</p>
              </div>
            </div>))}
          </div>
        </Section>

        {/* NEWS (từ admin) */}
        <Section kicker="Cập Nhật Mới Nhất" title="Tin Tức / Thông Báo" action={<Link to="/news" className="text-xs font-semibold px-3 py-2 rounded-lg border no-underline" style={{ color: palette.inkSoft, borderColor: '#ece3d0' }}>Xem Tất Cả</Link>}>
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-3">
            {(news.length ? news : []).slice(0, 6).map(n => (
              <Link key={n.id} to="/news" className={`${card} p-4 flex gap-3 items-center no-underline`} style={{ color: palette.ink }}>
                <div className="w-14 h-14 rounded-xl shrink-0 grid place-items-center font-extrabold text-[11px]" style={{ background: 'linear-gradient(135deg,#6c3ef333,#e0a02033)', color: palette.violet, fontFamily: "'Cinzel',serif" }}>
                  {(n.published_at || '').slice(5, 10) || 'NEW'}
                </div>
                <div>
                  <span className="text-[10px] font-bold uppercase tracking-wide" style={{ color: palette.gold }}>{n.category}</span>
                  <h4 className="my-0.5 text-[15px]" style={{ fontFamily: "'Philosopher',serif" }}>{n.title}</h4>
                  <p className="m-0 text-xs" style={{ color: palette.inkSoft }}>{n.summary}</p>
                </div>
              </Link>
            ))}
            {!news.length && <div className={`${card} p-6 text-center text-sm lg:col-span-2`} style={{ color: palette.inkSoft }}>Chưa có tin tức. Tạo trong trang Quản Trị.</div>}
          </div>
        </Section>

        {/* RANKING — CHỌN SERVER */}
        <Section kicker="Vinh Danh" title="Bảng Xếp Hạng" action={<Link to="/ranking" className="text-xs font-semibold px-3 py-2 rounded-lg border no-underline" style={{ color: palette.inkSoft, borderColor: '#ece3d0' }}>Xem Đầy Đủ</Link>}>
          <div className="flex flex-wrap gap-2 mb-3 items-center">
            {/* Server selector */}
            {servers.length > 0 && (
              <select value={selectedServer} onChange={e => setSelectedServer(Number(e.target.value))}
                className="px-4 py-2 rounded-lg text-sm font-semibold border outline-none" style={{ background: '#fffdf7', borderColor: '#ece3d0', color: palette.ink }}>
                {servers.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
              </select>
            )}
            {rankTabs.map(([k, label]) => (<button key={k} onClick={() => setRankTab(k)}
              className="px-4 py-2 rounded-lg text-sm font-bold border" style={{ borderColor: '#ece3d0', background: rankTab === k ? palette.violet : '#fffdf7', color: rankTab === k ? '#fff' : palette.inkSoft }}>{label}</button>))}
          </div>
          <div className={`${card} overflow-x-auto`}>
            <table className="w-full text-sm" style={{ borderCollapse: 'collapse' }}>
              <thead><tr className="text-left" style={{ background: '#f6f1e6', color: palette.inkSoft }}>
                <th className="px-4 py-3 text-xs font-bold uppercase">#</th>
                <th className="px-4 py-3 text-xs font-bold uppercase">Nhân Vật</th>
                <th className="px-4 py-3 text-xs font-bold uppercase text-right">{rankTab === 'wealth' ? 'Tổng Nạp' : rankTab === 'guild' ? 'Cấp' : rankTab === 'level' ? 'Cấp Độ' : 'Điểm'}</th>
              </tr></thead>
              <tbody>
                {ranks.length ? ranks.slice(0, 10).map((r, i) => (
                  <tr key={i} style={{ borderTop: '1px solid #ece3d0' }}>
                    <td className="px-4 py-3 font-extrabold" style={{ color: (r.rank ?? i + 1) <= 3 ? palette.gold : palette.inkSoft }}>{r.rank ?? i + 1}</td>
                    <td className="px-4 py-3 font-semibold">{r.name}</td>
                    <td className="px-4 py-3 text-right font-bold" style={{ color: palette.violet }}>{(r.value ?? 0).toLocaleString()}</td>
                  </tr>
                )) : (<tr><td colSpan={3} className="px-4 py-6 text-center text-sm" style={{ color: palette.inkSoft }}>Chưa có dữ liệu xếp hạng cho server này.</td></tr>)}
              </tbody>
            </table>
          </div>
        </Section>

        {/* NEWBIE GUIDE */}
        <Section kicker="Bắt Đầu Nhanh" title="Hướng Dẫn Tân Thủ">
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            {steps.map(s => (<div key={s.n} className={`${card} p-4`}>
              <div className="w-9 h-9 rounded-full grid place-items-center text-white font-extrabold" style={{ background: `linear-gradient(135deg,${palette.gold},${palette.goldSoft})`, fontFamily: "'Cinzel',serif" }}>{s.n}</div>
              <h4 className="mt-3 mb-1 text-[17px]" style={{ fontFamily: "'Philosopher',serif" }}>{s.title}</h4>
              <p className="text-xs leading-snug m-0" style={{ color: palette.inkSoft }}>{s.desc}</p>
            </div>))}
          </div>
        </Section>

        {/* SOCIAL (từ admin) */}
        <Section kicker="Cộng Đồng" title="Kết Nối Với Người Chơi">
          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-4">
            {(socials.length ? socials : []).map((s, i) => (
              <a key={i} href={s.url} target="_blank" rel="noreferrer" className={`${card} p-4 flex gap-3 items-center no-underline`} style={{ color: palette.ink }}>
                <span className="w-10 h-10 rounded-lg shrink-0" style={{ background: s.color || palette.violet }} />
                <div><div className="font-bold text-sm">{s.display_name}</div><div className="text-xs" style={{ color: palette.inkSoft }}>{s.platform}</div></div>
              </a>
            ))}
            {!socials.length && <div className={`${card} p-6 text-center text-sm md:col-span-4`} style={{ color: palette.inkSoft }}>Chưa có liên kết MXH. Tạo trong trang Quản Trị.</div>}
          </div>
        </Section>

        {/* FOOTER */}
        <footer className="mt-16 pt-8 pb-12 border-t grid grid-cols-2 md:grid-cols-4 gap-7" style={{ borderColor: '#ece3d0' }}>
          <div className="col-span-2 md:col-span-1">
            <div className="font-extrabold text-base" style={{ fontFamily: "'Cinzel',serif" }}>VỌNG LINH GIỚI</div>
            <p className="text-xs mt-2 leading-relaxed max-w-[280px]" style={{ color: palette.inkSoft }}>Nexus Isekai — cõi giới giữa các thế giới. Máy chủ ổn định, hỗ trợ nhanh.</p>
          </div>
          {[{ h: 'Liên Kết', items: [['Tải game', '/download'], ['Nạp thẻ', '/store'], ['Bảng xếp hạng', '/ranking']] },
            { h: 'Hướng Dẫn', items: [['Tin tức', '/news'], ['Hỗ trợ', '/support']] },
            { h: 'Hỗ Trợ', items: [['Cộng đồng', '/support']] }].map(col => (
            <div key={col.h}><div className="font-bold text-[13px] mb-2.5">{col.h}</div>
              {col.items.map(([label, to]) => (<Link key={label} to={to} className="block text-xs no-underline py-1" style={{ color: palette.inkSoft }}>{label}</Link>))}
            </div>))}
          <div className="col-span-2 md:col-span-4 text-[11px] pt-3 border-t" style={{ color: palette.inkSoft, borderColor: '#ece3d0' }}>
            © 2026 Nexus Isekai. Dành cho người chơi trên 12 tuổi. Chơi quá 180 phút mỗi ngày có hại cho sức khoẻ.
          </div>
        </footer>
      </div>
    </div>
  )
}

function Section({ kicker, title, action, children }: { kicker: string; title: string; action?: React.ReactNode; children: React.ReactNode }) {
  return (
    <section className="mt-14">
      <div className="flex justify-between items-end mb-5 gap-3">
        <div>
          <div className="text-[11px] font-bold tracking-[2px] uppercase" style={{ color: '#e0a020' }}>{kicker}</div>
          <h2 className="font-extrabold text-2xl sm:text-3xl mt-1.5" style={{ fontFamily: "'Cinzel',serif", color: '#2a2350' }}>{title}</h2>
        </div>
        {action}
      </div>
      {children}
    </section>
  )
}
