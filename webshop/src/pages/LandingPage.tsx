import { useState, useEffect } from 'react';

/**
 * LandingPage — Vọng Linh Giới (Nexus Isekai)
 * Layout theo style NSO nhưng theme SÁNG: ivory parchment + tím huyền bí + vàng kim.
 */

const C = {
  ink: '#2a2350', inkSoft: '#5b5380', violet: '#6c3ef3', violetDeep: '#4820b8',
  gold: '#e0a020', goldSoft: '#f5c451', cream: '#fbf7ee', paper: '#fffdf7', line: '#ece3d0',
};

function useReveal() {
  const [shown, setShown] = useState(false);
  useEffect(() => { const t = setTimeout(() => setShown(true), 50); return () => clearTimeout(t); }, []);
  return shown;
}

export default function LandingPage() {
  const shown = useReveal();
  const [rankTab, setRankTab] = useState<'power' | 'level' | 'topup' | 'guild'>('power');

  const stats = [
    { label: 'Đang Online', value: '0' }, { label: 'Người Chơi', value: '40' },
    { label: 'Phiên Bản', value: '1.0' }, { label: 'Rate', value: 'x1 — Cân Bằng' },
  ];
  const downloads = [
    { name: 'Java', sub: 'J2ME / điện thoại cũ', note: 'Nhẹ, máy yếu chơi được' },
    { name: 'PC', sub: 'Windows 32/64-bit', note: 'Gói chơi ổn định' },
    { name: 'APK', sub: 'Android 7.0+', note: 'Cài trực tiếp' },
    { name: 'iOS', sub: 'iPhone / iPad', note: 'Link ngoài nếu có' },
  ];
  const features = [
    { tag: 'PvP', title: 'Đấu Trường Sinh Tồn', desc: 'Ghép trận nhanh, leo hạng theo mùa, phần thưởng rõ ràng cho người chăm hoạt động.' },
    { tag: 'Bảo Mật', title: 'Bảo Vệ Tài Khoản', desc: 'Mã hoá, lịch sử thao tác, khuyến nghị đổi mật khẩu định kỳ.' },
    { tag: 'Tiện Ích', title: 'Mua Lại Vật Phẩm Đã Bán', desc: 'Khôi phục nhầm lẫn trong giới hạn cấu hình của server.' },
    { tag: 'Liên Server', title: 'Chiến Trường Liên Server', desc: 'Mở mỗi tuần, tích điểm toàn cõi giới, vinh danh bảng chiến trường.' },
    { tag: 'PvE', title: 'Phó Bản & Boss Ngày', desc: 'Chuỗi nhiệm vụ, boss giờ vàng, nguyên liệu nâng cấp & quà hoàn thành mỗi ngày.' },
    { tag: 'Hoạt Động', title: 'Sự Kiện Nạp & Tích Luỹ', desc: 'Mốc nạp minh bạch, ưu đãi theo mùa, lịch sử giao dịch dễ kiểm tra.' },
  ];
  const news = [
    { tag: 'Sự Kiện', date: '31/05', title: 'Khai Mở Vọng Linh Giới', body: 'Chào mừng các Lưu Dân đến với cõi giới giữa các thế giới.' },
    { tag: 'Cập Nhật', date: '31/05', title: 'Mùa PvP Đầu Tiên', body: 'Tranh hạng giành skin độc quyền và vé triệu hồi giới hạn.' },
    { tag: 'Triệu Hồi', date: '31/05', title: 'Banner Giới Hạn', body: 'Tỉ lệ SSR tăng gấp đôi trong 7 ngày đầu khai mở.' },
    { tag: 'Bảo Trì', date: '31/05', title: 'Lịch Bảo Trì Định Kỳ', body: 'Bảo trì 2h sáng thứ Tư hàng tuần để tối ưu máy chủ.' },
  ];
  const topupPacks = [
    { vnd: '20.000đ', label: 'Kích Hoạt', note: 'Mở đầy đủ tính năng tài khoản' },
    { vnd: '500.000đ', label: 'Thành Viên Bạc', note: 'Điểm danh tốt hơn, quà nguyên liệu' },
    { vnd: '1.000.000đ', label: 'Thành Viên Vàng', note: 'Ưu đãi hoạt động và vật phẩm hiếm' },
    { vnd: '2.000.000đ', label: 'Kim Cương', note: 'Gói hỗ trợ cao nhất theo mùa' },
  ];
  const rankData: Record<string, { rank: number; name: string; spd: number; score: number }[]> = {
    power: [
      { rank: 1, name: 'AzarothBane', spd: 2, score: 550 }, { rank: 2, name: 'LinhKiemKhach', spd: 2, score: 500 },
      { rank: 3, name: 'VoCucThien', spd: 6, score: 170 }, { rank: 4, name: 'HuyetLongVu', spd: 2, score: 50 },
      { rank: 5, name: 'BachVanTu', spd: 2, score: 40 },
    ],
    level: [{ rank: 1, name: 'VoCucThien', spd: 0, score: 99 }, { rank: 2, name: 'AzarothBane', spd: 0, score: 97 }, { rank: 3, name: 'LinhKiemKhach', spd: 0, score: 95 }],
    topup: [{ rank: 1, name: 'HuyetLongVu', spd: 0, score: 5000000 }, { rank: 2, name: 'BachVanTu', spd: 0, score: 2000000 }],
    guild: [{ rank: 1, name: 'Thiên Mệnh Hội', spd: 0, score: 12 }, { rank: 2, name: 'Hắc Long Bang', spd: 0, score: 9 }],
  };
  const steps = [
    { n: 1, title: 'Tải Game', desc: 'Chọn Java, PC, APK hoặc iOS theo thiết bị của bạn.' },
    { n: 2, title: 'Đăng Ký', desc: 'Tạo tài khoản, tên đăng nhập dễ nhớ, không chia sẻ mật khẩu.' },
    { n: 3, title: 'Đăng Nhập', desc: 'Vào game, tạo nhân vật và kích hoạt nhận quà.' },
    { n: 4, title: 'Nhận Quà', desc: 'Theo dõi sự kiện tân thủ, điểm danh và hoạt động mỗi ngày.' },
  ];
  const socials = [
    { name: 'Fanpage Facebook', sub: 'Cập nhật tin tức nhanh', color: '#1877f2' },
    { name: 'Zalo Community', sub: 'Hỗ trợ và thông báo', color: '#0068ff' },
    { name: 'Discord Server', sub: 'Kênh voice và cộng đồng', color: '#5865f2' },
    { name: 'Diễn Đàn', sub: 'Bài ghim, góp ý, hướng dẫn', color: '#8a62ff' },
  ];
  const nav = ['Trang Chủ', 'Tải Game', 'Tính Năng', 'Tin Tức', 'BXH', 'Nạp Thẻ', 'Hỗ Trợ'];

  return (
    <div style={{ background: C.cream, color: C.ink, fontFamily: "'Be Vietnam Pro', sans-serif", minHeight: '100vh' }}>
      <div style={{
        position: 'fixed', inset: 0, pointerEvents: 'none', opacity: 0.55, zIndex: 0,
        background: `radial-gradient(620px circle at 12% 8%, rgba(108,62,243,.10), transparent 60%), radial-gradient(560px circle at 88% 4%, rgba(224,160,32,.12), transparent 55%), radial-gradient(700px circle at 70% 70%, rgba(108,62,243,.06), transparent 60%)`,
      }} />
      <div style={{ position: 'relative', zIndex: 1, maxWidth: 1180, margin: '0 auto', padding: '0 20px' }}>

        <header style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '22px 0' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <div style={{ width: 44, height: 44, borderRadius: 12, display: 'grid', placeItems: 'center', background: `linear-gradient(135deg, ${C.violet}, ${C.violetDeep})`, boxShadow: '0 6px 18px rgba(108,62,243,.35)', color: '#fff', fontFamily: "'Cinzel', serif", fontWeight: 800 }}>VL</div>
            <div style={{ lineHeight: 1.05 }}>
              <div style={{ fontFamily: "'Cinzel', serif", fontWeight: 800, fontSize: 18, letterSpacing: 1 }}>VỌNG LINH GIỚI</div>
              <div style={{ fontSize: 11, color: C.inkSoft, letterSpacing: 2 }}>NEXUS ISEKAI</div>
            </div>
          </div>
          <nav style={{ display: 'flex', gap: 22, alignItems: 'center' }}>
            {nav.map((n, i) => (<a key={n} href="#" style={{ fontSize: 14, fontWeight: 600, color: i === 0 ? C.violet : C.inkSoft, textDecoration: 'none', borderBottom: i === 0 ? `2px solid ${C.gold}` : '2px solid transparent', paddingBottom: 4 }}>{n}</a>))}
          </nav>
          <div style={{ display: 'flex', gap: 10 }}>
            <a href="/login" style={btnGhost}>Đăng Nhập</a>
            <a href="/login" style={btnGold}>Đăng Ký</a>
          </div>
        </header>

        <section style={{ display: 'grid', gridTemplateColumns: '1.15fr 0.85fr', gap: 36, alignItems: 'center', padding: '34px 0 18px', opacity: shown ? 1 : 0, transform: shown ? 'none' : 'translateY(16px)', transition: 'all .7s ease' }}>
          <div>
            <h1 style={{ fontFamily: "'Cinzel', serif", fontWeight: 800, fontSize: 64, lineHeight: 1, margin: 0, background: `linear-gradient(135deg, ${C.ink}, ${C.violet})`, WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}>VỌNG<br />LINH GIỚI</h1>
            <p style={{ fontSize: 16, color: C.inkSoft, maxWidth: 460, marginTop: 18, lineHeight: 1.6 }}>Cõi giới giữa các thế giới — nơi Lưu Dân từ muôn phương hội tụ. Máy chủ ổn định, cộng đồng đông vui, hoạt động mỗi ngày. Cày cuốc hợp lý, nạp an toàn.</p>
            <div style={{ display: 'flex', gap: 12, marginTop: 26 }}>
              <a href="#download" style={{ ...btnGold, padding: '13px 22px', fontSize: 15 }}>Tải Game Ngay</a>
              <a href="/login" style={{ ...btnViolet, padding: '13px 22px', fontSize: 15 }}>Đăng Ký</a>
              <a href="/store" style={{ ...btnGhost, padding: '13px 22px', fontSize: 15 }}>Nạp Thẻ</a>
            </div>
            <div style={{ display: 'flex', marginTop: 30, border: `1px solid ${C.line}`, borderRadius: 14, overflow: 'hidden', background: C.paper }}>
              {stats.map((s, i) => (<div key={s.label} style={{ flex: 1, padding: '14px 16px', borderRight: i < stats.length - 1 ? `1px solid ${C.line}` : 'none' }}><div style={{ fontSize: 11, color: C.inkSoft, textTransform: 'uppercase', letterSpacing: 1 }}>{s.label}</div><div style={{ fontFamily: "'Philosopher', serif", fontSize: 20, fontWeight: 700, color: C.violet, marginTop: 4 }}>{s.value}</div></div>))}
            </div>
          </div>
          <div style={{ position: 'relative', borderRadius: 20, overflow: 'hidden', minHeight: 320, border: `1px solid ${C.line}`, background: `linear-gradient(160deg, #f3edff, #fff8e8)`, boxShadow: '0 20px 50px -20px rgba(108,62,243,.35)' }}>
            <svg viewBox="0 0 400 320" style={{ position: 'absolute', inset: 0, width: '100%', height: '100%', opacity: 0.5 }}>
              {[140, 110, 80, 50].map((r, i) => (<circle key={r} cx="200" cy="150" r={r} fill="none" stroke={i % 2 ? C.gold : C.violet} strokeWidth="1.5" strokeDasharray={i % 2 ? '4 8' : '2 6'} opacity={0.6 - i * 0.1} />))}
            </svg>
            <div style={{ position: 'absolute', top: 16, left: 16, right: 16, padding: '12px 16px', borderRadius: 12, background: 'rgba(255,255,255,.82)', backdropFilter: 'blur(6px)', border: `1px solid ${C.line}` }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}><span style={{ width: 9, height: 9, borderRadius: 99, background: '#22c55e', boxShadow: '0 0 0 4px rgba(34,197,94,.18)' }} /><strong style={{ fontSize: 14 }}>Máy chủ đang hoạt động</strong></div>
              <p style={{ fontSize: 12.5, color: C.inkSoft, margin: '6px 0 0', lineHeight: 1.5 }}>Trạng thái ổn định, hỗ trợ người chơi mới và cập nhật nội dung theo mùa.</p>
            </div>
            <div style={{ position: 'absolute', bottom: 20, left: 0, right: 0, textAlign: 'center' }}>
              <div style={{ fontFamily: "'Cinzel', serif", fontWeight: 800, fontSize: 40, color: C.violetDeep, letterSpacing: 2, opacity: 0.92 }}>NEXUS</div>
              <div style={{ fontFamily: "'Philosopher', serif", fontSize: 15, letterSpacing: 6, color: C.gold }}>ISEKAI ONLINE</div>
            </div>
          </div>
        </section>

        <Section id="download" kicker="Client Chính Thức" title="Tải Game" action={<a href="/download" style={btnGhostSm}>Xem Hướng Dẫn Cài Đặt</a>}>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4,1fr)', gap: 16 }}>
            {downloads.map(d => (<div key={d.name} style={card}>
              <div style={{ width: 46, height: 46, borderRadius: 12, background: `linear-gradient(135deg,${C.violet}22,${C.gold}22)`, display: 'grid', placeItems: 'center', fontFamily: "'Cinzel',serif", fontWeight: 800, color: C.violet }}>{d.name[0]}</div>
              <h3 style={{ margin: '14px 0 2px', fontFamily: "'Philosopher',serif", fontSize: 20 }}>{d.name}</h3>
              <div style={{ fontSize: 12.5, color: C.inkSoft }}>{d.sub}</div>
              <div style={{ fontSize: 12, color: C.inkSoft, marginTop: 2 }}>{d.note}</div>
              <a href="/download" style={{ ...btnGold, display: 'block', textAlign: 'center', marginTop: 16, padding: '10px 0', fontSize: 14 }}>Tải Ngay</a>
            </div>))}
          </div>
        </Section>

        <Section kicker="Định Hướng Máy Chủ" title="Cày Cuốc Hợp Lý, Cạnh Tranh Rõ Ràng">
          <p style={{ color: C.inkSoft, marginTop: -8, marginBottom: 22, maxWidth: 640 }}>Máy chủ tập trung vào nhịp chơi bền vững: hoạt động mỗi ngày, phần thưởng minh bạch, hỗ trợ tân thủ và không gây cảm giác đuối.</p>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3,1fr)', gap: 16 }}>
            {features.map(f => (<div key={f.title} style={{ ...card, overflow: 'hidden', padding: 0 }}>
              <div style={{ height: 96, background: `linear-gradient(135deg,${C.violet}, ${C.gold})`, position: 'relative' }}><span style={{ position: 'absolute', top: 12, left: 12, fontSize: 11, fontWeight: 700, color: C.violetDeep, background: 'rgba(255,255,255,.9)', padding: '3px 10px', borderRadius: 99 }}>{f.tag}</span></div>
              <div style={{ padding: 18 }}>
                <h3 style={{ margin: '0 0 6px', fontFamily: "'Philosopher',serif", fontSize: 19 }}>{f.title}</h3>
                <p style={{ fontSize: 13.5, color: C.inkSoft, lineHeight: 1.55, margin: 0 }}>{f.desc}</p>
                <a href="#" style={{ fontSize: 12.5, fontWeight: 700, color: C.violet, textDecoration: 'none', display: 'inline-block', marginTop: 12 }}>Xem chi tiết →</a>
              </div>
            </div>))}
          </div>
        </Section>

        <div style={{ display: 'grid', gridTemplateColumns: '1.4fr 1fr', gap: 28, marginTop: 56 }}>
          <div>
            <Kicker>Cập Nhật Mới Nhất</Kicker><Title>Tin Tức / Thông Báo</Title>
            <div style={{ marginTop: 18, display: 'flex', flexDirection: 'column', gap: 12 }}>
              {news.map(n => (<a key={n.title} href="/news" style={{ ...card, display: 'flex', gap: 14, alignItems: 'center', textDecoration: 'none', color: C.ink }}>
                <div style={{ width: 58, height: 58, borderRadius: 12, background: `linear-gradient(135deg,${C.violet}33,${C.gold}33)`, flexShrink: 0, display: 'grid', placeItems: 'center', fontFamily: "'Cinzel',serif", fontWeight: 800, color: C.violet, fontSize: 12 }}>{n.date}</div>
                <div><span style={{ fontSize: 10.5, fontWeight: 700, color: C.gold, textTransform: 'uppercase', letterSpacing: 1 }}>{n.tag}</span><h4 style={{ margin: '2px 0 3px', fontSize: 15.5, fontFamily: "'Philosopher',serif" }}>{n.title}</h4><p style={{ margin: 0, fontSize: 12.5, color: C.inkSoft }}>{n.body}</p></div>
              </a>))}
            </div>
          </div>
          <div>
            <Kicker>Ưu Đãi</Kicker><Title>Sự Kiện / Nạp Thẻ</Title>
            <div style={{ ...card, marginTop: 18, background: `linear-gradient(160deg,#fff8e8,#f3edff)` }}><strong style={{ fontSize: 15 }}>Nạp Thẻ Nhận Quà</strong><p style={{ fontSize: 12.5, color: C.inkSoft, margin: '6px 0 0' }}>Nạp trong thời gian sự kiện để nhận thêm phần thưởng theo mốc tích luỹ.</p></div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 10, marginTop: 12 }}>
              {topupPacks.map(p => (<div key={p.vnd} style={{ ...card, display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '14px 16px' }}>
                <div><div style={{ fontFamily: "'Philosopher',serif", fontSize: 18, fontWeight: 700, color: C.violetDeep }}>{p.vnd}</div><div style={{ fontSize: 12, color: C.inkSoft }}>{p.note}</div></div>
                <span style={{ fontSize: 12, fontWeight: 700, color: C.gold, whiteSpace: 'nowrap' }}>{p.label}</span>
              </div>))}
            </div>
          </div>
        </div>

        <Section kicker="Vinh Danh" title="Bảng Xếp Hạng" action={<a href="/ranking" style={btnGhostSm}>Xem Đầy Đủ</a>}>
          <div style={{ display: 'flex', gap: 8, marginBottom: 14 }}>
            {[['power', 'Top Lực Chiến'], ['level', 'Top Cấp Độ'], ['topup', 'Top Nạp'], ['guild', 'Bang Hội Mạnh']].map(([k, label]) => (
              <button key={k} onClick={() => setRankTab(k as any)} style={{ padding: '9px 16px', borderRadius: 10, fontSize: 13, fontWeight: 700, cursor: 'pointer', border: `1px solid ${C.line}`, background: rankTab === k ? C.violet : C.paper, color: rankTab === k ? '#fff' : C.inkSoft }}>{label}</button>))}
          </div>
          <div style={{ ...card, padding: 0, overflow: 'hidden' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 14 }}>
              <thead><tr style={{ background: '#f6f1e6', color: C.inkSoft, textAlign: 'left' }}>
                <th style={th}>#</th><th style={th}>Nhân Vật</th><th style={{ ...th, textAlign: 'center' }}>Gia Tốc</th>
                <th style={{ ...th, textAlign: 'right' }}>{rankTab === 'topup' ? 'Tổng Nạp' : rankTab === 'guild' ? 'Cấp' : rankTab === 'level' ? 'Cấp Độ' : 'Điểm'}</th>
              </tr></thead>
              <tbody>{rankData[rankTab].map(r => (<tr key={r.rank} style={{ borderTop: `1px solid ${C.line}` }}>
                <td style={{ ...td, fontWeight: 800, color: r.rank <= 3 ? C.gold : C.inkSoft }}>{r.rank <= 3 ? ['', '①', '②', '③'][r.rank] : r.rank}</td>
                <td style={{ ...td, fontWeight: 600 }}>{r.name}</td>
                <td style={{ ...td, textAlign: 'center', color: C.inkSoft }}>{r.spd}</td>
                <td style={{ ...td, textAlign: 'right', fontWeight: 700, color: C.violet }}>{r.score.toLocaleString()}</td>
              </tr>))}</tbody>
            </table>
          </div>
        </Section>

        <Section kicker="Bắt Đầu Nhanh" title="Hướng Dẫn Tân Thủ">
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4,1fr)', gap: 16 }}>
            {steps.map(s => (<div key={s.n} style={card}>
              <div style={{ width: 34, height: 34, borderRadius: 99, background: `linear-gradient(135deg,${C.gold},${C.goldSoft})`, color: '#fff', display: 'grid', placeItems: 'center', fontFamily: "'Cinzel',serif", fontWeight: 800 }}>{s.n}</div>
              <h4 style={{ margin: '12px 0 4px', fontFamily: "'Philosopher',serif", fontSize: 17 }}>{s.title}</h4>
              <p style={{ fontSize: 12.5, color: C.inkSoft, lineHeight: 1.5, margin: 0 }}>{s.desc}</p>
            </div>))}
          </div>
        </Section>

        <Section kicker="Cộng Đồng" title="Kết Nối Với Người Chơi">
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4,1fr)', gap: 16 }}>
            {socials.map(s => (<a key={s.name} href="/support" style={{ ...card, display: 'flex', gap: 12, alignItems: 'center', textDecoration: 'none', color: C.ink }}>
              <span style={{ width: 40, height: 40, borderRadius: 10, background: s.color, flexShrink: 0 }} />
              <div><div style={{ fontWeight: 700, fontSize: 14 }}>{s.name}</div><div style={{ fontSize: 12, color: C.inkSoft }}>{s.sub}</div></div>
            </a>))}
          </div>
        </Section>

        <footer style={{ marginTop: 64, padding: '34px 0 50px', borderTop: `1px solid ${C.line}`, display: 'grid', gridTemplateColumns: '1.4fr 1fr 1fr 1fr', gap: 28 }}>
          <div>
            <div style={{ fontFamily: "'Cinzel',serif", fontWeight: 800, fontSize: 17 }}>VỌNG LINH GIỚI</div>
            <p style={{ fontSize: 12.5, color: C.inkSoft, marginTop: 8, lineHeight: 1.6, maxWidth: 280 }}>Nexus Isekai — cõi giới giữa các thế giới. Máy chủ ổn định, hỗ trợ nhanh, hoạt động đều mỗi ngày.</p>
          </div>
          {[{ h: 'Liên Kết', items: ['Tải game', 'Đăng ký', 'Nạp thẻ', 'Bảng xếp hạng'] }, { h: 'Hướng Dẫn', items: ['Cài đặt', 'Hướng dẫn nạp', 'Bảo vệ tài khoản', 'Tin tức'] }, { h: 'Hỗ Trợ', items: ['Facebook', 'Zalo', 'Discord', 'Diễn đàn'] }].map(col => (
            <div key={col.h}><div style={{ fontWeight: 700, fontSize: 13, marginBottom: 10 }}>{col.h}</div>{col.items.map(it => (<a key={it} href="#" style={{ display: 'block', fontSize: 12.5, color: C.inkSoft, textDecoration: 'none', padding: '4px 0' }}>{it}</a>))}</div>))}
          <div style={{ gridColumn: '1 / -1', fontSize: 11.5, color: C.inkSoft, paddingTop: 14, borderTop: `1px solid ${C.line}` }}>© 2026 Nexus Isekai. Dành cho người chơi trên 12 tuổi. Chơi quá 180 phút mỗi ngày có hại cho sức khoẻ.</div>
        </footer>
      </div>
    </div>
  );
}

function Kicker({ children }: { children: React.ReactNode }) { return <div style={{ fontSize: 11.5, fontWeight: 700, letterSpacing: 2, textTransform: 'uppercase', color: '#e0a020' }}>{children}</div>; }
function Title({ children }: { children: React.ReactNode }) { return <h2 style={{ fontFamily: "'Cinzel', serif", fontWeight: 800, fontSize: 30, margin: '6px 0 0', color: '#2a2350' }}>{children}</h2>; }
function Section({ id, kicker, title, action, children }: { id?: string; kicker: string; title: string; action?: React.ReactNode; children: React.ReactNode }) {
  return (<section id={id} style={{ marginTop: 56 }}><div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', marginBottom: 20 }}><div><Kicker>{kicker}</Kicker><Title>{title}</Title></div>{action}</div>{children}</section>);
}

const card: React.CSSProperties = { background: '#fffdf7', border: '1px solid #ece3d0', borderRadius: 16, padding: 18, boxShadow: '0 8px 24px -18px rgba(42,35,80,.4)' };
const th: React.CSSProperties = { padding: '13px 18px', fontSize: 12, fontWeight: 700, textTransform: 'uppercase', letterSpacing: 1 };
const td: React.CSSProperties = { padding: '13px 18px' };
const btnGold: React.CSSProperties = { background: 'linear-gradient(135deg,#e0a020,#f5c451)', color: '#3a2a05', fontWeight: 700, padding: '9px 18px', borderRadius: 10, textDecoration: 'none', fontSize: 14, boxShadow: '0 6px 16px -6px rgba(224,160,32,.6)' };
const btnViolet: React.CSSProperties = { background: 'linear-gradient(135deg,#6c3ef3,#4820b8)', color: '#fff', fontWeight: 700, padding: '9px 18px', borderRadius: 10, textDecoration: 'none', fontSize: 14, boxShadow: '0 6px 16px -6px rgba(108,62,243,.5)' };
const btnGhost: React.CSSProperties = { background: '#fffdf7', color: '#5b5380', fontWeight: 600, padding: '9px 16px', borderRadius: 10, textDecoration: 'none', fontSize: 14, border: '1px solid #ece3d0' };
const btnGhostSm: React.CSSProperties = { ...btnGhost, fontSize: 12.5, padding: '8px 14px' };
