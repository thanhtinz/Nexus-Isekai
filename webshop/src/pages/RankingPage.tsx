import { Swords, Trophy, Castle, Gem } from 'lucide-react';
import { useState, useEffect } from 'react';
import { api } from '../api/client';

interface RankEntry { rank: number; name: string; level: number; class_name: string; elo: number; guild: string; }

export default function RankingPage() {
  const [tab, setTab] = useState<'level'|'pvp'|'guild'|'wealth'>('level');
  const [serverId, setServerId] = useState(0);
  const [servers, setServers] = useState<any[]>([]);
  const [data, setData] = useState<RankEntry[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => { api.get('/api/download-links').catch(()=>{}); api.get('/api/social-links').then(r => {}).catch(()=>{});
    fetch('/api/ranking?type=level&server_id=0').catch(()=>{});
    loadServers(); }, []);
  useEffect(() => { loadRank(); }, [tab, serverId]);
  const loadServers = async () => { try { const r = await api.get('/api/servers'); setServers(r.data.servers || []); } catch { setServers([{id:0,name:'All'}]); } };
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
    <div className="min-h-screen bg-gradient-to-b from-gray-900 to-gray-800 text-white">
      <div className="max-w-4xl mx-auto px-4 py-8">
        <h1 className="text-3xl font-bold text-center mb-8 text-yellow-400">Bang Xep Hang</h1>
        <div className="flex gap-2 mb-4 justify-center">
          <select value={serverId} onChange={e => setServerId(Number(e.target.value))}
            className="bg-gray-700 text-white px-4 py-2 rounded-lg border border-gray-600">
            <option value={0}>All Servers</option>
            {servers.map((s: any) => <option key={s.id} value={s.id}>{s.name}</option>)}
          </select>
        </div>
        <div className="flex gap-2 mb-6 justify-center">
          {tabs.map(t => (
            <button key={t.key} onClick={() => setTab(t.key as any)}
              className={`px-4 py-2 rounded-lg font-medium transition ${tab === t.key ? 'bg-yellow-500 text-black' : 'bg-gray-700 hover:bg-gray-600'}`}>
              <t.Icon size={16} /> {t.label}
            </button>
          ))}
        </div>
        <div className="bg-gray-800 rounded-xl overflow-hidden">
          <table className="w-full">
            <thead><tr className="bg-gray-700 text-yellow-400">
              <th className="p-3 text-left w-16">#</th>
              <th className="p-3 text-left">Tên</th>
              <th className="p-3 text-center">Level</th>
              <th className="p-3 text-center">Class</th>
              <th className="p-3 text-right">{tab === 'pvp' ? 'ELO' : tab === 'wealth' ? 'Diamond' : 'EXP'}</th>
            </tr></thead>
            <tbody>
              {loading ? <tr><td colSpan={5} className="p-8 text-center text-gray-500">Đang tải...</td></tr> :
               data.length === 0 ? <tr><td colSpan={5} className="p-8 text-center text-gray-500">Chưa có dữ liệu</td></tr> :
               data.map((r, i) => (
                <tr key={i} className={`border-t border-gray-700 ${i < 3 ? 'bg-gray-750' : ''}`}>
                  <td className="p-3 font-bold">{i+1}</td>
                  <td className="p-3 font-medium">{r.name}</td>
                  <td className="p-3 text-center">{r.level}</td>
                  <td className="p-3 text-center text-sm text-gray-400">{r.class_name}</td>
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
