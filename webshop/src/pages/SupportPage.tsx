export default function SupportPage() {
  const socials = [
    { name: 'Facebook', icon: '📘', url: 'https://facebook.com/NexusIsekai', desc: 'Fanpage chính thức', color: 'from-blue-600 to-blue-700' },
    { name: 'Zalo', icon: '💬', url: 'https://zalo.me/NexusIsekai', desc: 'Nhóm Zalo cộng đồng', color: 'from-blue-500 to-blue-600' },
    { name: 'Discord', icon: '🎮', url: 'https://discord.gg/NexusIsekai', desc: 'Server Discord', color: 'from-indigo-600 to-indigo-700' },
    { name: 'Telegram', icon: '✈️', url: 'https://t.me/NexusIsekai', desc: 'Kênh Telegram', color: 'from-sky-500 to-sky-600' },
    { name: 'YouTube', icon: '▶️', url: 'https://youtube.com/@NexusIsekai', desc: 'Kênh YouTube', color: 'from-red-600 to-red-700' },
    { name: 'TikTok', icon: '🎵', url: 'https://tiktok.com/@NexusIsekai', desc: 'TikTok chính thức', color: 'from-gray-700 to-gray-800' },
  ];

  return (
    <div className="min-h-screen bg-gradient-to-b from-gray-900 to-gray-800 text-white">
      <div className="max-w-lg mx-auto px-4 py-12">
        <h1 className="text-3xl font-bold text-center mb-2 text-yellow-400">Hỗ Trợ & Cộng Đồng</h1>
        <p className="text-center text-gray-400 mb-10">Theo dõi & liên hệ qua các kênh</p>
        <div className="grid gap-3">
          {socials.map(s => (
            <a key={s.name} href={s.url} target="_blank" rel="noopener noreferrer"
              className={`flex items-center gap-4 bg-gradient-to-r ${s.color} rounded-xl p-4 hover:scale-[1.02] transition-transform`}>
              <span className="text-3xl">{s.icon}</span>
              <div>
                <h3 className="font-bold text-lg">{s.name}</h3>
                <p className="text-sm text-white/70">{s.desc}</p>
              </div>
            </a>
          ))}
        </div>
      </div>
    </div>
  );
}
