import { useState, useEffect } from 'react';
import { api } from '../api/client';

interface NewsItem { id: number; title: string; category: string; content: string; image: string; created_at: string; }

export default function NewsPage() {
  const [news, setNews] = useState<NewsItem[]>([]);
  const [selected, setSelected] = useState<NewsItem|null>(null);
  const [filter, setFilter] = useState('all');

  useEffect(() => {
    api.get('/api/news-articles').then((r: any) => setNews(r.data.articles || [])).catch(() => setNews([
      { id: 1, title: 'Khai mở Nexus Isekai', category: 'event', content: 'Chào mừng các Lưu Dân đến với Vọng Linh Giới!', image: '', created_at: '2025-01-01' },
      { id: 2, title: 'Mùa PvP 1 bắt đầu', category: 'pvp', content: 'Mùa PvP đầu tiên chính thức bắt đầu. Chiến đấu để giành skin độc quyền!', image: '', created_at: '2025-01-15' },
      { id: 3, title: 'Banner Triệu Hồi Giới Hạn', category: 'gacha', content: 'Banner giới hạn với tỉ lệ SSR tăng gấp đôi!', image: '', created_at: '2025-02-01' },
    ]));
  }, []);

  const cats = [
    { key: 'all', label: 'Tất cả' },
    { key: 'event', label: 'Sự kiện' },
    { key: 'update', label: 'Cập nhật' },
    { key: 'pvp', label: 'PvP' },
    { key: 'gacha', label: 'Triệu hồi' },
    { key: 'maintenance', label: 'Bảo trì' },
  ];

  const filtered = filter === 'all' ? news : news.filter(n => n.category === filter);

  return (
    <div className="min-h-screen bg-gradient-to-b from-gray-900 to-gray-800 text-white">
      <div className="max-w-4xl mx-auto px-4 py-8">
        <h1 className="text-3xl font-bold text-center mb-8 text-yellow-400">Tin Tuc</h1>
        <div className="flex gap-2 mb-6 flex-wrap justify-center">
          {cats.map(c => (
            <button key={c.key} onClick={() => setFilter(c.key)}
              className={`px-3 py-1 rounded-full text-sm ${filter === c.key ? 'bg-yellow-500 text-black' : 'bg-gray-700'}`}>
              {c.label}
            </button>
          ))}
        </div>
        {selected ? (
          <div className="bg-gray-800 rounded-xl p-6">
            <button onClick={() => setSelected(null)} className="text-yellow-400 mb-4">← Quay lại</button>
            <h2 className="text-2xl font-bold mb-2">{selected.title}</h2>
            <p className="text-gray-400 text-sm mb-4">{selected.created_at}</p>
            <p className="text-gray-300 leading-relaxed">{selected.content}</p>
          </div>
        ) : (
          <div className="grid gap-4">
            {filtered.map(n => (
              <div key={n.id} onClick={() => setSelected(n)}
                className="bg-gray-800 hover:bg-gray-750 border border-gray-700 hover:border-yellow-500 rounded-xl p-5 cursor-pointer transition">
                <div className="flex justify-between items-start">
                  <div>
                    <span className="text-xs px-2 py-0.5 rounded bg-gray-700 text-yellow-400 mr-2">{n.category}</span>
                    <h3 className="font-bold text-lg mt-1">{n.title}</h3>
                  </div>
                  <span className="text-xs text-gray-500">{n.created_at}</span>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
