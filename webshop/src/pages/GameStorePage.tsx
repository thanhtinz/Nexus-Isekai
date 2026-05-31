import { useState } from 'react';
import { Gem, ShoppingCart } from 'lucide-react';
import TopupPage from './TopupPage';
import ShopPage from './ShopPage';

const TABS = [
  { key: 'topup', label: 'Nap', icon: Gem, component: TopupPage },
  { key: 'shop', label: 'Shop', icon: ShoppingCart, component: ShopPage },
];

export default function GameStorePage() {
  const [tab, setTab] = useState('topup');
  const ActiveTab = TABS.find(t => t.key === tab)?.component || TopupPage;

  return (
    <div className="min-h-screen bg-gradient-to-b from-gray-900 to-gray-800">
      <div className="sticky top-0 z-50 bg-gray-900/95 backdrop-blur border-b border-gray-700">
        <div className="max-w-4xl mx-auto flex">
          {TABS.map(t => (
            <button key={t.key} onClick={() => setTab(t.key)}
              className={`flex-1 py-4 flex items-center justify-center gap-2 font-bold text-sm transition-all
                ${tab === t.key ? 'text-yellow-400 border-b-2 border-yellow-400 bg-gray-800/50' : 'text-gray-400 hover:text-gray-200'}`}>
              <t.icon size={18} /> {t.label}
            </button>
          ))}
        </div>
      </div>
      <ActiveTab />
    </div>
  );
}
