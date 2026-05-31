import { Swords, Trophy, Castle, Gem } from 'lucide-react';
import { useState, useEffect } from 'react';
import { useServer } from '../context/ServerContext';
import { api } from '../api/client';

interface RankEntry { rank: number; name: string; level: number; class_name: string; elo: number; guild: string; }

export default function RankingPage() {
  const [tab, setTab] = useState<'level'|'pvp'|'guild'|'wealth'>('level');
  const { selectedServer: serverId } = useServer();
  const [data, setData] = useState<RankEntry[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => { api.get('/api/download-links').catch(()=>{}); api.get('/api/social-links').then((r: any) => {}).catch(()=>{});
    fetch('/api/ranking?type=level&server_id=0').catch(()=>{});
    }, []);
  useEffect(() => { loadRank(); }, [tab, serverId]);
  const loadRank = async () => {
    setLoading(true);
    try { const res = await api.get(`/api/ranking?type=${tab}&server_id=${serverId}`); setData(res.data.rankings || []); }
    catch { setData([]); }
    setLoading(false);
  };

  const tabs = [
    { key: 'level', label: 'Cap Do', Icon: Swords },
    { key: 'pvp', label: 'PvP', Icon: Trophy },
    { key: 'guild', label: 'Bang Hoi', Icon: Castle },
    { key: 'wealth', label: 'Dai Gia', Icon: Gem },
  ];

  return (
    <div className="min-h-screen bg-gradient-to-b from-gray-900 to-gray-800 text-[#2a2350]">
      <div className="max-w-4xl mx-auto px-4 py-8">
        <h1 className="text-3xl font-bold text-center mb-8 text-yellow-400">Bang Xep Hang</h1>
        <div className="flex gap-2 mb-6 justify-center">
          {tabs.map(t => (
            <button key={t.key} onClick={() => setTab(t.key as any)}
              className={`px-4 py-2 rounded-lg font-medium transition ${tab === t.key ? 'bg-yellow-500 text-black' : 'bg-[#f6f1e6] hover:bg-[#ece3d0]'}`}>
              <t.Icon size={16} /> {t.label}
            </button>
          ))}
        </div>
        <div className="bg-[#fffdf7] rounded-xl overflow-hidden">
          <table className="w-full">
            <thead><tr className="bg-[#f6f1e6] text-yellow-400">
              <th className="p-3 text-left w-16">#</th>
              <th className="p-3 text-left">Tên</th>
              <th className="p-3 text-center">Level</th>
              <th className="p-3 text-center">Class</th>
              <th className="p-3 text-right">{tab === 'pvp' ? 'ELO' : tab === 'wealth' ? 'Diamond' : 'EXP'}</th>
            </tr></thead>
            <tbody>
              {loading ? <tr><td colSpan={5} className="p-8 text-center text-[#5b5380]">Đang tải...</td></tr> :
               data.length === 0 ? <tr><td colSpan={5} className="p-8 text-center text-[#5b5380]">Chưa có dữ liệu</td></tr> :
               data.map((r, i) => (
                <tr key={i} className={`border-t border-[#ece3d0] ${i < 3 ? 'bg-[#f6f1e6]' : ''}`}>
                  <td className="p-3 font-bold">{i+1}</td>
                  <td className="p-3 font-medium">{r.name}</td>
                  <td className="p-3 text-center">{r.level}</td>
                  <td className="p-3 text-center text-sm text-[#5b5380]">{r.class_name}</td>
                  <td className="p-3 text-right text-yellow-400 font-medium">{r.elo?.toLocaleString()}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
