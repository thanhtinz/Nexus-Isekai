import { useState, useEffect } from 'react';
import { api } from '../api/client';
export default function DownloadPage() {
  const [links, setLinks] = useState<any>({});
  useEffect(() => {
    api.get('/api/download-links').then((r: any) => {
      const map: any = {};
      (r.data.links || []).forEach((l: any) => { if (l.is_active) map[l.platform] = l; });
      setLinks(map);
    }).catch(() => {});
  }, []);
  const getUrl = (key: string) => links[key]?.url || '#';
  const getVer = () => links['apk']?.version || '1.0.0';
  const downloads = [
    { key: 'appstore', url: '#', badge: (
      <div className="flex items-center gap-3 px-8 py-4">
        <svg viewBox="0 0 24 24" className="w-8 h-8 fill-white"><path d="M18.71 19.5c-.83 1.24-1.71 2.45-3.05 2.47-1.34.03-1.77-.79-3.29-.79-1.53 0-2 .77-3.27.82-1.31.05-2.3-1.32-3.14-2.53C4.25 17 2.94 12.45 4.7 9.39c.87-1.52 2.43-2.48 4.12-2.51 1.28-.02 2.5.87 3.29.87.78 0 2.26-1.07 3.8-.91.65.03 2.47.26 3.64 1.98-.09.06-2.17 1.28-2.15 3.81.03 3.02 2.65 4.03 2.68 4.04-.03.07-.42 1.44-1.38 2.83M13 3.5c.73-.83 1.94-1.46 2.94-1.5.13 1.17-.34 2.35-1.04 3.19-.69.85-1.83 1.51-2.95 1.42-.15-1.15.41-2.35 1.05-3.11z"/></svg>
        <div className="text-left"><p className="text-[10px] leading-tight">Download on the</p><p className="text-xl font-semibold leading-tight">App Store</p></div>
      </div>
    )},
    { key: 'googleplay', url: '#', badge: (
      <div className="flex items-center gap-3 px-8 py-4">
        <svg viewBox="0 0 24 24" className="w-8 h-8"><path fill="#4285F4" d="M22.018 13.298l-3.919 2.218-3.515-3.493 3.543-3.521 3.891 2.202a1.49 1.49 0 010 2.594z"/><path fill="#34A853" d="M1.017.512C.89.715.82.96.82 1.238v21.524c0 .277.07.522.197.725l11.35-11.264L1.017.512z"/><path fill="#FBBC04" d="M12.367 12.223L1.017 23.487c.2.128.442.2.698.2.18 0 .362-.04.528-.123l16.088-9.106-5.964-2.235z"/><path fill="#EA4335" d="M12.367 12.223l5.964-5.927L2.243.188A1.46 1.46 0 001.715.065c-.256 0-.498.072-.698.2l11.35 11.958z"/></svg>
        <div className="text-left"><p className="text-[10px] leading-tight">GET IT ON</p><p className="text-xl font-semibold leading-tight">Google Play</p></div>
      </div>
    )},
    { key: 'pc', url: '#', badge: (
      <div className="flex items-center gap-3 px-8 py-4">
        <svg viewBox="0 0 24 24" className="w-7 h-7 fill-white"><path d="M0 3.449L9.75 2.1v9.451H0m10.949-9.602L24 0v11.4H10.949M0 12.6h9.75v9.451L0 20.699M10.949 12.6H24V24l-12.9-1.801"/></svg>
        <p className="text-xl font-semibold">PC</p>
      </div>
    )},
    { key: 'java', url: '#', badge: (
      <div className="flex items-center gap-3 px-8 py-4">
        <svg viewBox="0 0 24 24" className="w-7 h-7 fill-white"><path d="M8.851 18.56s-.917.534.653.714c1.902.218 2.874.187 4.969-.211 0 0 .552.346 1.321.646-4.699 2.013-10.633-.118-6.943-1.149M8.276 15.933s-1.028.762.542.924c2.032.209 3.636.227 6.413-.308 0 0 .384.389.987.602-5.679 1.661-12.007.13-7.942-1.218M13.116 11.475c1.158 1.333-.304 2.533-.304 2.533s2.939-1.518 1.589-3.418c-1.261-1.772-2.228-2.652 3.007-5.688 0 0-8.216 2.051-4.292 6.573"/><path d="M19.33 20.504s.679.559-.747.991c-2.712.822-11.288 1.069-13.669.033-.856-.373.75-.89 1.254-.998.527-.114.828-.093.828-.093-.953-.671-6.156 1.317-2.643 1.887 9.58 1.553 17.462-.7 14.977-1.82M9.292 13.21s-4.362 1.036-1.544 1.412c1.189.159 3.561.123 5.77-.062 1.806-.152 3.618-.477 3.618-.477s-.637.272-1.098.587c-4.429 1.165-12.986.623-10.522-.568 2.082-1.006 3.776-.892 3.776-.892M17.116 17.584c4.503-2.34 2.421-4.589.968-4.285-.355.074-.515.138-.515.138s.132-.207.385-.297c2.875-1.011 5.086 2.981-.928 4.562 0 0 .07-.062.09-.118"/><path d="M14.401 0s2.494 2.494-2.365 6.33c-3.896 3.077-.889 4.832 0 6.836-2.274-2.053-3.943-3.858-2.824-5.539 1.644-2.469 6.197-3.665 5.189-7.627"/><path d="M9.734 23.924c4.322.277 10.959-.154 11.116-2.198 0 0-.302.775-3.572 1.391-3.688.694-8.239.613-10.937.168 0 0 .553.457 3.393.639"/></svg>
        <p className="text-xl font-semibold">JAVA</p>
      </div>
    )},
    { key: 'apk', url: '#', badge: (
      <div className="flex items-center gap-3 px-8 py-4">
        <svg viewBox="0 0 24 24" className="w-7 h-7 fill-white"><path d="M17.523 15.341a.908.908 0 00-.915.913c0 .502.41.912.915.912a.907.907 0 00.912-.912.908.908 0 00-.912-.913m-11.046 0a.908.908 0 00-.915.913c0 .502.41.912.915.912a.907.907 0 00.912-.912.908.908 0 00-.912-.913m11.39-6.117l1.997-3.467a.416.416 0 00-.152-.567.416.416 0 00-.567.152l-2.024 3.513A12.267 12.267 0 0012 7.984c-1.813 0-3.53.395-5.121 1.071L4.855 5.542a.416.416 0 00-.567-.152.416.416 0 00-.152.567l1.997 3.467C2.69 11.378.342 15.048 0 19.2h24c-.342-4.152-2.69-7.822-6.133-9.976"/></svg>
        <p className="text-xl font-semibold">APK</p>
      </div>
    )},
  ];

  return (
    <div className="min-h-screen bg-gradient-to-b from-gray-900 to-gray-800 text-[#2a2350]">
      <div className="max-w-2xl mx-auto px-4 py-12">
        <h1 className="text-3xl font-bold text-center mb-2 text-yellow-400">Tai Game</h1>
        <p className="text-center text-[#5b5380] mb-10">Nexus Isekai — Vong Linh Gioi</p>
        <div className="grid grid-cols-2 gap-3 mb-4">
          {downloads.slice(0,2).map(d => (
            <a key={d.key} href={getUrl(d.key)} className="bg-[#fbf7ee] hover:bg-[#fffdf7] border border-[#ece3d0] hover:border-gray-500 rounded-xl transition-all hover:scale-[1.02]">
              {d.badge}
            </a>
          ))}
        </div>
        <div className="grid grid-cols-3 gap-3">
          {downloads.slice(2).map(d => (
            <a key={d.key} href={getUrl(d.key)} className="bg-[#fbf7ee] hover:bg-[#fffdf7] border border-[#ece3d0] hover:border-gray-500 rounded-xl transition-all hover:scale-[1.02]">
              {d.badge}
            </a>
          ))}
        </div>
        <p className="text-center text-xs text-[#5b5380] mt-8">Phien ban: v{getVer()}</p>
      </div>
    </div>
  );
}
