import { useState, useEffect } from 'react';
import { api } from '../api/client';
const SocialIcon = ({ d, color }: { d: string; color: string }) => (
  <svg viewBox="0 0 24 24" className={`w-7 h-7 ${color}`}><path fill="currentColor" d={d}/></svg>
);

export default function SupportPage() {
  const [apiLinks, setApiLinks] = useState<any>({});
  useEffect(() => {
    api.get('/api/social-links').then(r => {
      const map: any = {};
      (r.data.links || []).forEach((l: any) => { if (l.is_active) map[l.platform] = l; });
      setApiLinks(map);
    }).catch(() => {});
  }, []);
  const socials = [
    { name: 'Facebook', url: 'https://facebook.com/NexusIsekai', desc: 'Fanpage chinh thuc', color: 'from-blue-600 to-blue-700',
      icon: 'M24 12.073c0-6.627-5.373-12-12-12s-12 5.373-12 12c0 5.99 4.388 10.954 10.125 11.854v-8.385H7.078v-3.47h3.047V9.43c0-3.007 1.792-4.669 4.533-4.669 1.312 0 2.686.235 2.686.235v2.953H15.83c-1.491 0-1.956.925-1.956 1.874v2.25h3.328l-.532 3.47h-2.796v8.385C19.612 23.027 24 18.062 24 12.073z' },
    { name: 'Zalo', url: 'https://zalo.me/NexusIsekai', desc: 'Nhom Zalo cong dong', color: 'from-blue-500 to-blue-600',
      icon: 'M12 0C5.373 0 0 5.373 0 12s5.373 12 12 12 12-5.373 12-12S18.627 0 12 0zm5.568 8.16c-.18-.135-.32-.18-.48-.18H14.4c-.24 0-.36.12-.36.36v1.8c0 .24.12.36.36.36h1.2v4.92H9.6V10.5h1.2c.24 0 .36-.12.36-.36v-1.8c0-.24-.12-.36-.36-.36H6.48c-.16 0-.3.045-.48.18C5.28 8.76 5 9.66 5 10.5v3c0 3.3 2.7 6 6 6h2c3.3 0 6-2.7 6-6v-3c0-.84-.28-1.74-1.432-2.34z' },
    { name: 'Discord', url: 'https://discord.gg/NexusIsekai', desc: 'Server Discord', color: 'from-indigo-600 to-indigo-700',
      icon: 'M20.317 4.37a19.791 19.791 0 00-4.885-1.515.074.074 0 00-.079.037c-.21.375-.444.864-.608 1.25a18.27 18.27 0 00-5.487 0 12.64 12.64 0 00-.617-1.25.077.077 0 00-.079-.037A19.736 19.736 0 003.677 4.37a.07.07 0 00-.032.027C.533 9.046-.32 13.58.099 18.057a.082.082 0 00.031.057 19.9 19.9 0 005.993 3.03.078.078 0 00.084-.028c.462-.63.874-1.295 1.226-1.994a.076.076 0 00-.041-.106 13.107 13.107 0 01-1.872-.892.077.077 0 01-.008-.128 10.2 10.2 0 00.372-.292.074.074 0 01.077-.01c3.928 1.793 8.18 1.793 12.062 0a.074.074 0 01.078.01c.12.098.246.198.373.292a.077.077 0 01-.006.127 12.299 12.299 0 01-1.873.892.077.077 0 00-.041.107c.36.698.772 1.362 1.225 1.993a.076.076 0 00.084.028 19.839 19.839 0 006.002-3.03.077.077 0 00.032-.054c.5-5.177-.838-9.674-3.549-13.66a.061.061 0 00-.031-.03z' },
    { name: 'Telegram', url: 'https://t.me/NexusIsekai', desc: 'Kenh Telegram', color: 'from-sky-500 to-sky-600',
      icon: 'M11.944 0A12 12 0 000 12a12 12 0 0012 12 12 12 0 0012-12A12 12 0 0012 0a12 12 0 00-.056 0zm4.962 7.224c.1-.002.321.023.465.14a.506.506 0 01.171.325c.016.093.036.306.02.472-.18 1.898-.962 6.502-1.36 8.627-.168.9-.499 1.201-.82 1.23-.696.065-1.225-.46-1.9-.902-1.056-.693-1.653-1.124-2.678-1.8-1.185-.78-.417-1.21.258-1.91.177-.184 3.247-2.977 3.307-3.23.007-.032.014-.15-.056-.212s-.174-.041-.249-.024c-.106.024-1.793 1.14-5.061 3.345-.479.33-.913.49-1.302.48-.428-.008-1.252-.241-1.865-.44-.752-.245-1.349-.374-1.297-.789.027-.216.325-.437.893-.663 3.498-1.524 5.83-2.529 6.998-3.014 3.332-1.386 4.025-1.627 4.476-1.635z' },
    { name: 'YouTube', url: 'https://youtube.com/@NexusIsekai', desc: 'Kenh YouTube', color: 'from-red-600 to-red-700',
      icon: 'M23.498 6.186a3.016 3.016 0 00-2.122-2.136C19.505 3.545 12 3.545 12 3.545s-7.505 0-9.377.505A3.017 3.017 0 00.502 6.186C0 8.07 0 12 0 12s0 3.93.502 5.814a3.016 3.016 0 002.122 2.136c1.871.505 9.376.505 9.376.505s7.505 0 9.377-.505a3.015 3.015 0 002.122-2.136C24 15.93 24 12 24 12s0-3.93-.502-5.814zM9.545 15.568V8.432L15.818 12l-6.273 3.568z' },
    { name: 'TikTok', url: 'https://tiktok.com/@NexusIsekai', desc: 'TikTok chinh thuc', color: 'from-gray-700 to-gray-800',
      icon: 'M12.525.02c1.31-.02 2.61-.01 3.91-.02.08 1.53.63 3.09 1.75 4.17 1.12 1.11 2.7 1.62 4.24 1.79v4.03c-1.44-.05-2.89-.35-4.2-.97-.57-.26-1.1-.59-1.62-.93-.01 2.92.01 5.84-.02 8.75-.08 1.4-.54 2.79-1.35 3.94-1.31 1.92-3.58 3.17-5.91 3.21-1.43.08-2.86-.31-4.08-1.03-2.02-1.19-3.44-3.37-3.65-5.71-.02-.5-.03-1-.01-1.49.18-1.9 1.12-3.72 2.58-4.96 1.66-1.44 3.98-2.13 6.15-1.72.02 1.48-.04 2.96-.04 4.44-.99-.32-2.15-.23-3.02.37-.63.41-1.11 1.04-1.36 1.75-.21.51-.15 1.07-.14 1.61.24 1.64 1.82 3.02 3.5 2.87 1.12-.01 2.19-.66 2.77-1.61.19-.33.4-.67.41-1.06.1-1.79.06-3.57.07-5.36.01-4.03-.01-8.05.02-12.07z' },
  ];

  return (
    <div className="min-h-screen bg-gradient-to-b from-gray-900 to-gray-800 text-white">
      <div className="max-w-lg mx-auto px-4 py-12">
        <h1 className="text-3xl font-bold text-center mb-2 text-yellow-400">Ho Tro & Cong Dong</h1>
        <p className="text-center text-gray-400 mb-10">Theo doi & lien he qua cac kenh</p>
        <div className="grid gap-3">
          {socials.map(s => (
            <a key={s.name} href={(apiLinks[s.name.toLowerCase()]?.url || s.url)} target="_blank" rel="noopener noreferrer"
              className={`flex items-center gap-4 bg-gradient-to-r ${s.color} rounded-xl p-4 hover:scale-[1.02] transition-transform`}>
              <SocialIcon d={s.icon} color="text-white" />
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
