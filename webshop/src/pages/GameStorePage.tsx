import { useState } from 'react';
import TopupPage from './TopupPage';
import ShopPage from './ShopPage';
import RankingPage from './RankingPage';

const TABS = [
  { key: 'topup', label: '💎 Nạp', component: TopupPage },
  { key: 'shop', label: '🛒 Shop', component: ShopPage },
  { key: 'ranking', label: '🏆 BXH', component: RankingPage },
];

export default function GameStorePage() {
  const [tab, setTab] = useState('topup');
  const ActiveTab = TABS.find(t => t.key === tab)?.component || TopupPage;

  return (
    <div className="min-h-screen bg-gradient-to-b from-gray-900 to-gray-800">
      {/* Tab bar */}
      <div className="sticky top-0 z-50 bg-gray-900/95 backdrop-blur border-b border-gray-700">
        <div className="max-w-4xl mx-auto flex">
          {TABS.map(t => (
            <button key={t.key} onClick={() => setTab(t.key)}
              className={`flex-1 py-4 text-center font-bold text-sm transition-all
                ${tab === t.key
                  ? 'text-yellow-400 border-b-2 border-yellow-400 bg-gray-800/50'
                  : 'text-gray-400 hover:text-gray-200'}`}>
              {t.label}
            </button>
          ))}
        </div>
      </div>
      {/* Active tab content */}
      <ActiveTab />
    </div>
  );
}
