export default function DownloadPage() {
  const platforms = [
    { name: 'Android', icon: '🤖', link: '#', size: '~150MB', req: 'Android 7.0+' },
    { name: 'iOS', icon: '🍎', link: '#', size: '~200MB', req: 'iOS 14+' },
    { name: 'PC (Windows)', icon: '🖥️', link: '#', size: '~300MB', req: 'Windows 10+' },
    { name: 'APK Direct', icon: '📦', link: '#', size: '~150MB', req: 'Android 7.0+' },
  ];

  return (
    <div className="min-h-screen bg-gradient-to-b from-gray-900 to-gray-800 text-white">
      <div className="max-w-3xl mx-auto px-4 py-12">
        <h1 className="text-3xl font-bold text-center mb-2 text-yellow-400">📥 Tải Game</h1>
        <p className="text-center text-gray-400 mb-10">Nexus Isekai — Vọng Linh Giới</p>
        <div className="grid gap-4">
          {platforms.map(p => (
            <a key={p.name} href={p.link}
              className="flex items-center gap-4 bg-gray-800 hover:bg-gray-750 border border-gray-700 hover:border-yellow-500 rounded-xl p-5 transition">
              <span className="text-4xl">{p.icon}</span>
              <div className="flex-1">
                <h3 className="font-bold text-lg">{p.name}</h3>
                <p className="text-sm text-gray-400">{p.req} · {p.size}</p>
              </div>
              <span className="bg-yellow-500 text-black px-4 py-2 rounded-lg font-bold">Tải</span>
            </a>
          ))}
        </div>
        <p className="text-center text-xs text-gray-500 mt-8">Phiên bản mới nhất: v1.0.0</p>
      </div>
    </div>
  );
}
