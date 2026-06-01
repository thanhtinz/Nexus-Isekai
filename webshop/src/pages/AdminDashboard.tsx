import { useState, useEffect, useCallback } from 'react';

// 
// AdminDashboard — Web-based admin panel
// Responsive mobile, gọi Admin API qua proxy
// Route: /admin (trong webshop app)
// 

const API_BASE = '/admin-api'; // proxy qua webshop server

interface TableRow { [key: string]: string | number | boolean }

//  API helper 

async function api(path: string, method = 'GET', body?: Record<string, string>) {
  const apiKey = localStorage.getItem('admin_key') || '';
  const opts: RequestInit = {
    method,
    headers: { 'Content-Type': 'application/json', 'X-Admin-Key': apiKey },
  };
  if (body && method !== 'GET') opts.body = JSON.stringify(body);
  const res = await fetch(`${API_BASE}${path}`, opts);
  return res.json();
}

//  Components 

function DataTable({ data, compact }: { data: TableRow[]; compact?: boolean }) {
  if (!data || data.length === 0) return <p className="text-gray-500 text-sm p-4">Khong co du lieu</p>;
  const keys = Object.keys(data[0]).slice(0, compact ? 5 : 12);
  return (
    <div className="overflow-x-auto">
      <table className="w-full text-xs">
        <thead>
          <tr className="border-b border-white/10">
            {keys.map(k => <th key={k} className="text-left p-2 text-gray-400 font-medium uppercase tracking-wider">{k}</th>)}
          </tr>
        </thead>
        <tbody>
          {data.slice(0, 50).map((row, i) => (
            <tr key={i} className="border-b border-white/5 hover:bg-white/5">
              {keys.map(k => (
                <td key={k} className="p-2 text-gray-300 max-w-[200px] truncate">
                  {String(row[k] ?? '')}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
      {data.length > 50 && <p className="text-gray-500 text-xs p-2">Hien thi 50/{data.length} dong</p>}
    </div>
  );
}

//  Config editor — them/sua/xoa cho bang config 
function ConfigEditor({ endpoint, pk, data, onReload }:
  { endpoint: string; pk: string; data: TableRow[]; onReload: () => void }) {
  const [editing, setEditing] = useState<TableRow | null>(null);
  const [isNew, setIsNew] = useState(false);

  // suy ra danh sach cot tu dong dau (loai cot thoi gian tu sinh)
  const cols = data.length > 0
    ? Object.keys(data[0]).filter(k => !['created_at', 'listed_at', 'last_spawn_at'].includes(k))
    : [pk];

  const startNew = () => {
    const blank: TableRow = {};
    cols.forEach(c => { if (c !== pk) blank[c] = ''; });
    setEditing(blank); setIsNew(true);
  };
  const startEdit = (row: TableRow) => { setEditing({ ...row }); setIsNew(false); };

  const save = async () => {
    if (!editing) return;
    const body: Record<string, string> = {};
    Object.keys(editing).forEach(k => { body[k] = String(editing[k] ?? ''); });
    if (isNew) delete body[pk]; // de DB tu sinh khoa (tru khi pk do nguoi nhap)
    if (isNew && editing[pk] !== undefined && String(editing[pk]).length > 0) body[pk] = String(editing[pk]);
    await api(endpoint, 'POST', body);
    setEditing(null); onReload();
  };
  const remove = async (row: TableRow) => {
    if (!window.confirm(`Xoa ban ghi ${pk}=${row[pk]}?`)) return;
    await api(`${endpoint}/${row[pk]}`, 'DELETE');
    onReload();
  };

  return (
    <div>
      <div className="flex items-center justify-between p-3 border-b border-white/10">
        <span className="text-xs text-gray-400">{data.length} ban ghi</span>
        <ActionButton onClick={startNew}>+ Them moi</ActionButton>
      </div>

      {editing && (
        <div className="p-4 bg-[#0d0d22] border-b border-white/10">
          <div className="grid grid-cols-2 md:grid-cols-3 gap-3">
            {cols.map(c => (
              <div key={c}>
                <label className="block text-[10px] text-gray-500 uppercase tracking-wider mb-1">
                  {c}{c === pk && isNew ? ' (de trong = tu sinh)' : ''}
                </label>
                <input
                  value={String(editing[c] ?? '')}
                  disabled={c === pk && !isNew}
                  onChange={e => setEditing({ ...editing, [c]: e.target.value })}
                  className="w-full bg-[#12122a] border border-white/10 rounded px-2 py-1 text-xs text-gray-200 disabled:opacity-50"
                />
              </div>
            ))}
          </div>
          <div className="flex gap-2 mt-3">
            <ActionButton onClick={save} variant="primary">Luu</ActionButton>
            <ActionButton onClick={() => setEditing(null)}>Huy</ActionButton>
          </div>
        </div>
      )}

      <div className="overflow-x-auto">
        <table className="w-full text-xs">
          <thead>
            <tr className="border-b border-white/10">
              {cols.slice(0, 8).map(k => <th key={k} className="text-left p-2 text-gray-400 font-medium uppercase tracking-wider">{k}</th>)}
              <th className="text-right p-2 text-gray-400 font-medium uppercase tracking-wider">Thao tac</th>
            </tr>
          </thead>
          <tbody>
            {data.slice(0, 100).map((row, i) => (
              <tr key={i} className="border-b border-white/5 hover:bg-white/5">
                {cols.slice(0, 8).map(k => (
                  <td key={k} className="p-2 text-gray-300 max-w-[180px] truncate">{String(row[k] ?? '')}</td>
                ))}
                <td className="p-2 text-right whitespace-nowrap">
                  <button onClick={() => startEdit(row)} className="text-[#8a5cf5] hover:underline mr-3">Sua</button>
                  <button onClick={() => remove(row)} className="text-red-400 hover:underline">Xoa</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function StatCard({ label, value, color = '#6c3ef3' }: { label: string; value: string | number; color?: string }) {
  return (
    <div className="bg-[#12122a] border border-white/5 rounded-xl p-4">
      <div className="text-xs text-gray-500 mb-1">{label}</div>
      <div className="text-2xl font-bold" style={{ color }}>{value}</div>
    </div>
  );
}

function ActionButton({ children, onClick, variant = 'primary' }: {
  children: React.ReactNode; onClick: () => void; variant?: 'primary' | 'danger' | 'outline'
}) {
  const cls = variant === 'primary' ? 'bg-[#6c3ef3] text-white hover:bg-[#8a5cf5]'
    : variant === 'danger' ? 'bg-red-600 text-white hover:bg-red-500'
    : 'border border-white/20 text-gray-300 hover:bg-white/10';
  return (
    <button onClick={onClick} className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-all ${cls}`}>
      {children}
    </button>
  );
}

//  Panel Configs 

interface PanelConfig {
  key: string;
  label: string;
  group: string;
  endpoint: string;
  dataKey: string;
  actions?: { label: string; endpoint: string; body: Record<string, string> }[];
  editable?: boolean;   // bat form them/sua/xoa
  pk?: string;          // cot khoa chinh (mac dinh 'id')
}

const PANELS: PanelConfig[] = [
  // Tong quan
  { key: 'status',       label: 'Trang Thai',      group: 'Tong Quan',  endpoint: '/api/status',            dataKey: '' },
  { key: 'stats',        label: 'Thong Ke',        group: 'Tong Quan',  endpoint: '/api/logs',              dataKey: 'logs' },
  { key: 'quick',        label: 'Thao Tac Nhanh',   group: 'Tong Quan',  endpoint: '',                       dataKey: '' },

  // Nguoi choi
  { key: 'players',      label: 'Nguoi Choi',      group: 'Nguoi Choi', endpoint: '/api/players',           dataKey: 'players' },
  { key: 'accounts',     label: 'Tai Khoan',       group: 'Nguoi Choi', endpoint: '/api/accounts',          dataKey: 'accounts' },
  { key: 'mail',         label: 'Thu',             group: 'Nguoi Choi', endpoint: '/api/mail',              dataKey: 'mails' },
  { key: 'reports',      label: 'Bao Cao',         group: 'Nguoi Choi', endpoint: '/api/reports',           dataKey: 'reports' },

  // Noi dung (cot truyen, item, NPC, ky nang, audio)
  { key: 'story',        label: 'Cot Truyen',      group: 'Noi Dung',   endpoint: '/api/story',             dataKey: 'chapters' },
  { key: 'quests',       label: 'Nhiem Vu',        group: 'Noi Dung',   endpoint: '/api/quests',            dataKey: 'quests' },
  { key: 'dialogs',      label: 'NPC Dialog',      group: 'Noi Dung',   endpoint: '/api/dialogs',           dataKey: 'dialogs' },
  { key: 'registry',     label: 'Kho Tong',        group: 'Noi Dung',   endpoint: '/api/registry',          dataKey: 'items' },
  { key: 'items',        label: 'Items',           group: 'Noi Dung',   endpoint: '/api/items',             dataKey: 'items' },
  { key: 'monsters',     label: 'Quai Vat / Boss', group: 'Noi Dung',   endpoint: '/api/monsters',          dataKey: 'rows', editable: true, pk: 'id' },
  { key: 'npcs',         label: 'NPC',             group: 'Noi Dung',   endpoint: '/api/npcs',              dataKey: 'rows', editable: true, pk: 'id' },
  { key: 'skills',       label: 'Ky Nang (VFX)',   group: 'Noi Dung',   endpoint: '/api/skills',            dataKey: 'rows', editable: true, pk: 'id' },
  { key: 'maps',         label: 'Ban Do (Nhac)',   group: 'Noi Dung',   endpoint: '/api/maps',              dataKey: 'rows', editable: true, pk: 'id' },
  { key: 'class_change', label: 'Chuyen Lop',      group: 'Noi Dung',   endpoint: '/api/class-change',      dataKey: 'rows', editable: true, pk: 'id' },
  { key: 'child_shop',   label: 'Shop Tre Em',     group: 'Noi Dung',   endpoint: '/api/child-shop',        dataKey: 'rows', editable: true, pk: 'id' },
  { key: 'voice_lines',  label: 'Loi Thoai (Voice)',group: 'Noi Dung',  endpoint: '/api/voice-lines',       dataKey: 'rows', editable: true, pk: 'id' },
  { key: 'sound_events', label: 'Am Thanh (Sound)',  group: 'Noi Dung',  endpoint: '/api/sound-events',      dataKey: 'rows', editable: true, pk: 'event_key' },
  { key: 'audio_assets', label: 'Kho Audio',         group: 'Noi Dung',  endpoint: '/api/audio-assets',      dataKey: 'rows', editable: true, pk: 'id' },

  // Kinh te (shop, nap, giftcode, su menh)
  { key: 'shop',         label: 'Shop NPC',        group: 'Kinh Te',    endpoint: '/api/shops',             dataKey: 'shops' },
  { key: 'webshop',      label: 'Webshop',         group: 'Kinh Te',    endpoint: '/api/webshop',           dataKey: 'products' },
  { key: 'auction',      label: 'Dau Gia',         group: 'Kinh Te',    endpoint: '/api/auction',           dataKey: 'listings' },
  { key: 'trades',       label: 'Giao Dich',       group: 'Kinh Te',    endpoint: '/api/trade/history',     dataKey: 'trades' },
  { key: 'giftcode',     label: 'Gift Code',       group: 'Kinh Te',    endpoint: '/api/giftcodes',         dataKey: 'codes' },
  { key: 'pass',         label: 'So Su Menh',      group: 'Kinh Te',    endpoint: '/api/pass/seasons',      dataKey: 'seasons' },
  { key: 'enhance',      label: 'Cuong Hoa',       group: 'Kinh Te',    endpoint: '/api/enhancement-config',dataKey: 'config' },
  { key: 'eventcur',     label: 'Tien Te SK',      group: 'Kinh Te',    endpoint: '/api/event-currency',    dataKey: 'currencies' },
  { key: 'first_topup',  label: 'Thuong Nap Dau',  group: 'Kinh Te',    endpoint: '/api/first-topup',       dataKey: 'rows', editable: true, pk: 'id' },
  { key: 'gift_rewards', label: 'Thuong Giftcode', group: 'Kinh Te',    endpoint: '/api/giftcode-rewards',  dataKey: 'rows', editable: true, pk: 'id' },
  { key: 'webshop_ct',   label: 'Webshop Noi Dung',group: 'Kinh Te',    endpoint: '/api/webshop-contents',  dataKey: 'rows', editable: true, pk: 'id' },

  // Xa hoi (guild, PvP, danh hieu, phe, pet)
  { key: 'guilds',       label: 'Guild',           group: 'Xa Hoi',     endpoint: '/api/guilds',            dataKey: 'guilds' },
  { key: 'party',        label: 'Nhom',            group: 'Xa Hoi',     endpoint: '/api/party/active',      dataKey: 'parties' },
  { key: 'pvp',          label: 'PvP',             group: 'Xa Hoi',     endpoint: '/api/pvp/history',       dataKey: 'matches' },
  { key: 'leaderboard',  label: 'BXH',             group: 'Xa Hoi',     endpoint: '/api/leaderboard',       dataKey: 'entries' },
  { key: 'chat',         label: 'Chat',            group: 'Xa Hoi',     endpoint: '/api/chat/history',      dataKey: 'messages' },
  { key: 'titles',       label: 'Danh Hieu',       group: 'Xa Hoi',     endpoint: '/api/titles',            dataKey: 'titles' },
  { key: 'pets',         label: 'Pet & Mount',     group: 'Xa Hoi',     endpoint: '/api/pets',              dataKey: 'pets' },
  { key: 'cosmetics',    label: 'Cosmetic',        group: 'Xa Hoi',     endpoint: '/api/cosmetics',         dataKey: 'rows', editable: true, pk: 'id' },
  { key: 'factions',     label: 'Phe',             group: 'Xa Hoi',     endpoint: '/api/factions',          dataKey: 'rows', editable: true, pk: 'id' },
  { key: 'faction_tiers',label: 'Moc Danh Vong',   group: 'Xa Hoi',     endpoint: '/api/faction-tiers',     dataKey: 'rows', editable: true, pk: 'id' },
  { key: 'pvp_rewards',  label: 'Thuong Mua PvP',  group: 'Xa Hoi',     endpoint: '/api/pvp-season-rewards',dataKey: 'rows', editable: true, pk: 'id' },

  // Tinh nang (endgame, hoat dong, phuc loi, minigame)
  { key: 'afk',          label: 'AFK / The Treo',  group: 'Tinh Nang',  endpoint: '/api/afk-cards',         dataKey: 'rows', editable: true, pk: 'id' },
  { key: 'vip_levels',   label: 'Moc VIP',         group: 'Tinh Nang',  endpoint: '/api/vip-levels',        dataKey: 'rows', editable: true, pk: 'vip_level' },
  { key: 'vip_rewards',  label: 'Thuong VIP',      group: 'Tinh Nang',  endpoint: '/api/vip-milestones',    dataKey: 'rows', editable: true, pk: 'vip_level' },
  { key: 'worldboss',    label: 'World Boss',      group: 'Tinh Nang',  endpoint: '/api/world-bosses-cfg',  dataKey: 'rows', editable: true, pk: 'id' },
  { key: 'outer_floors', label: 'Ngoai Vuc - Tang',group: 'Tinh Nang',  endpoint: '/api/outer-floors',      dataKey: 'rows', editable: true, pk: 'floor' },
  { key: 'outer_bosses', label: 'Ngoai Vuc - Boss',group: 'Tinh Nang',  endpoint: '/api/outer-bosses',      dataKey: 'rows', editable: true, pk: 'id' },
  { key: 'guildwar',     label: 'Guild War',       group: 'Tinh Nang',  endpoint: '/api/guild-wars',        dataKey: 'rows' },
  { key: 'market',       label: 'Cho Nguoi Choi',  group: 'Tinh Nang',  endpoint: '/api/market-admin',      dataKey: 'rows' },
  { key: 'activities',   label: 'Hoat Dong',       group: 'Tinh Nang',  endpoint: '/api/activities',        dataKey: 'rows', editable: true, pk: 'id' },
  { key: 'act_miles',    label: 'Hoat Dong - Moc', group: 'Tinh Nang',  endpoint: '/api/activity-milestones',dataKey: 'rows', editable: true, pk: 'id' },
  { key: 'act_ranks',    label: 'Hoat Dong - Hang',group: 'Tinh Nang',  endpoint: '/api/activity-rank-rewards',dataKey: 'rows', editable: true, pk: 'id' },
  { key: 'act_types',    label: 'Hoat Dong - Loai', group: 'Tinh Nang', endpoint: '/api/activity-types', dataKey: 'rows', editable: true, pk: 'type_key' },
  { key: 'welfare',      label: 'Phuc Loi',        group: 'Tinh Nang',  endpoint: '/api/welfare',           dataKey: 'rows', editable: true, pk: 'id' },
  { key: 'welfare_mile', label: 'Phuc Loi - Moc',  group: 'Tinh Nang',  endpoint: '/api/welfare-milestones',dataKey: 'rows', editable: true, pk: 'id' },
  { key: 'welfare_types',label: 'Phuc Loi - Loai', group: 'Tinh Nang',  endpoint: '/api/welfare-types',     dataKey: 'rows', editable: true, pk: 'type_key' },
  { key: 'treasure',     label: 'Kho Bau',         group: 'Tinh Nang',  endpoint: '/api/treasure',          dataKey: 'rows', editable: true, pk: 'id' },
  { key: 'lucky_wheels', label: 'Vong Quay',       group: 'Tinh Nang',  endpoint: '/api/lucky-wheels',      dataKey: 'rows', editable: true, pk: 'id' },
  { key: 'minigame_cfg', label: 'Minigame - Cau Hinh',group: 'Tinh Nang',endpoint: '/api/minigame-config',  dataKey: 'rows', editable: true, pk: 'game_type' },
  { key: 'auto_cfg',     label: 'Auto-play',       group: 'Tinh Nang',  endpoint: '/api/auto-config',       dataKey: 'rows', editable: true, pk: 'auto_type' },
  { key: 'option_ext',   label: 'Rut Option',      group: 'Tinh Nang',  endpoint: '/api/option-extract',    dataKey: 'rows', editable: true, pk: 'id' },
  { key: 'clan_beast',   label: 'Than Thu Bang',   group: 'Tinh Nang',  endpoint: '/api/clan-beast',        dataKey: 'rows', editable: true, pk: 'level' },
  { key: 'boss_sched',   label: 'Bang Gio Boss',   group: 'Tinh Nang',  endpoint: '/api/boss-schedule',     dataKey: 'rows', editable: true, pk: 'id' },
  { key: 'mob_soul',     label: 'Linh Hon Quai',   group: 'Tinh Nang',  endpoint: '/api/mob-soul',          dataKey: 'rows', editable: true, pk: 'monster_id' },
  { key: 'soul_exch',    label: 'Doi Linh Hon',    group: 'Tinh Nang',  endpoint: '/api/soul-exchange',     dataKey: 'rows', editable: true, pk: 'id' },

  // He thong (van hanh, OTA, server)
  { key: 'announce',     label: 'Thong Bao',       group: 'He Thong',   endpoint: '/api/announcements',     dataKey: 'announcements' },
  { key: 'assets',       label: 'Assets OTA',      group: 'He Thong',   endpoint: '/api/assets',            dataKey: 'assets' },
  { key: 'versions',     label: 'Phien Ban',       group: 'He Thong',   endpoint: '/api/client-versions',   dataKey: 'versions' },
  { key: 'hotconfig',    label: 'Hot Config',      group: 'He Thong',   endpoint: '/api/hot-config',        dataKey: 'configs' },
  { key: 'ratelimit',    label: 'Rate Limit',      group: 'He Thong',   endpoint: '/api/rate-limit',        dataKey: 'limits' },
  { key: 'dungeon',      label: 'Dungeon',         group: 'He Thong',   endpoint: '/api/dungeon',           dataKey: 'dungeons' },
  { key: 'farming',      label: 'Nong Trai',       group: 'He Thong',   endpoint: '/api/farming/seeds',     dataKey: 'seeds' },
  { key: 'housing',      label: 'Nha O',           group: 'He Thong',   endpoint: '/api/housing/catalog',   dataKey: 'furniture' },
  { key: 'minigame',     label: 'Minigame',        group: 'He Thong',   endpoint: '/api/minigame/config',   dataKey: 'config' },
  { key: 'schedule',     label: 'Lich Hen',        group: 'He Thong',   endpoint: '/api/scheduled-tasks',   dataKey: 'tasks' },
  { key: 'audit',        label: 'Audit Log',       group: 'He Thong',   endpoint: '/api/audit-log',         dataKey: 'logs' },
  { key: 'servers',      label: 'Servers',         group: 'He Thong',   endpoint: '/api/servers',           dataKey: 'servers' },

  // AI
  { key: 'ai',           label: 'AI Content',      group: 'AI & Review',endpoint: '/api/story',             dataKey: 'chapters' },
  { key: 'ai_log',       label: 'AI Log',          group: 'AI & Review',endpoint: '/api/logs?type=ai',      dataKey: 'logs' },
];

//  AI Generate Panel 

function QuickActions() {
  const [msg, setMsg] = useState('');
  const [busy, setBusy] = useState('');
  // người chơi
  const [pName, setPName] = useState('');
  const [cur, setCur] = useState('gold');
  const [amount, setAmount] = useState('');
  const [itemId, setItemId] = useState('');
  const [qty, setQty] = useState('1');
  const [level, setLevel] = useState('');
  const [muteMin, setMuteMin] = useState('10');
  // toàn server
  const [bcast, setBcast] = useState('');

  const run = async (label: string, path: string, body: Record<string,string>, needName = true) => {
    if (needName && !pName.trim()) { setMsg('Nhap ten nhan vat truoc'); return; }
    setBusy(label);
    try {
      const r = await api(path, 'POST', body);
      setMsg(r?.error ? `Loi: ${r.error}` : `OK: ${label}`);
    } catch { setMsg(`Loi: ${label}`); }
    setBusy('');
  };

  const Field = ({ ph, val, set, w = 'w-28', type = 'text' }:
    { ph: string; val: string; set: (s: string)=>void; w?: string; type?: string }) => (
    <input value={val} onChange={e => set(e.target.value)} placeholder={ph} type={type}
      className={`${w} bg-[#0a0a1e] border border-white/10 rounded px-2 py-1.5 text-xs text-gray-200`} />
  );

  const Card = ({ title, children }: { title: string; children: React.ReactNode }) => (
    <div className="bg-[#12122a] border border-white/5 rounded-xl p-4 space-y-3">
      <h3 className="text-sm font-medium text-white">{title}</h3>
      {children}
    </div>
  );

  return (
    <div className="space-y-4">
      {msg && <div className="text-xs px-3 py-2 rounded-lg bg-[#1a1a3a] text-[#f0c050]">{msg}</div>}

      <Card title="Nhan vat muc tieu">
        <Field ph="Ten nhan vat" val={pName} set={setPName} w="w-full md:w-64" />
      </Card>

      <div className="grid md:grid-cols-2 gap-4">
        <Card title="Cong / tru tien">
          <div className="flex flex-wrap items-center gap-2">
            <select value={cur} onChange={e => setCur(e.target.value)}
              className="bg-[#0a0a1e] border border-white/10 rounded px-2 py-1.5 text-xs text-gray-200">
              <option value="gold">Vang</option><option value="diamond">Kim cuong</option>
            </select>
            <Field ph="So luong (am = tru)" val={amount} set={setAmount} type="number" />
            <ActionButton onClick={() => run('Cong tien', '/api/give-currency', { charName: pName, currency: cur, amount })}>
              {busy === 'Cong tien' ? '...' : 'Thuc hien'}</ActionButton>
          </div>
        </Card>

        <Card title="Tang vat pham">
          <div className="flex flex-wrap items-center gap-2">
            <Field ph="Item ID" val={itemId} set={setItemId} type="number" w="w-24" />
            <Field ph="SL" val={qty} set={setQty} type="number" w="w-16" />
            <ActionButton onClick={() => run('Tang item', '/api/give-item', { charName: pName, itemId, qty })}>
              {busy === 'Tang item' ? '...' : 'Tang'}</ActionButton>
          </div>
        </Card>

        <Card title="Dat cap do">
          <div className="flex flex-wrap items-center gap-2">
            <Field ph="Level" val={level} set={setLevel} type="number" w="w-20" />
            <ActionButton onClick={() => run('Set level', '/api/set-level', { charName: pName, level })}>
              {busy === 'Set level' ? '...' : 'Dat'}</ActionButton>
          </div>
        </Card>

        <Card title="Ky luat nhan vat">
          <div className="flex flex-wrap items-center gap-2">
            <Field ph="Phut mute" val={muteMin} set={setMuteMin} type="number" w="w-20" />
            <ActionButton variant="outline" onClick={() => run('Mute', '/api/mute', { charName: pName, minutes: muteMin })}>Cam chat</ActionButton>
            <ActionButton variant="outline" onClick={() => run('Bo mute', '/api/mute', { charName: pName, minutes: '0' })}>Bo cam</ActionButton>
            <ActionButton variant="outline" onClick={() => run('Kick', '/api/kick', { charName: pName })}>Kick</ActionButton>
            <ActionButton variant="danger" onClick={() => run('Ban', '/api/ban', { username: pName })}>Ban</ActionButton>
            <ActionButton variant="outline" onClick={() => run('Unban', '/api/unban', { username: pName })}>Unban</ActionButton>
          </div>
          <p className="text-[10px] text-gray-500">Ban/Unban dung theo TEN TAI KHOAN; Kick/Mute theo TEN NHAN VAT.</p>
        </Card>

        <Card title="Toan server">
          <div className="flex flex-wrap items-center gap-2">
            <Field ph="Noi dung thong bao" val={bcast} set={setBcast} w="w-full md:w-72" />
            <ActionButton onClick={() => { if (bcast.trim()) run('Broadcast', '/api/broadcast', { message: bcast }, false); }}>Broadcast</ActionButton>
          </div>
          <div className="flex flex-wrap gap-2 pt-1">
            <ActionButton variant="outline" onClick={() => run('Reload config', '/api/reload', {}, false)}>Reload Config</ActionButton>
            <ActionButton variant="danger" onClick={() => run('Bao tri', '/api/maintenance', { enabled: 'true' }, false)}>Bao Tri</ActionButton>
          </div>
        </Card>
      </div>
    </div>
  );
}

function AIPanel() {
  const [genType, setGenType] = useState('quest');
  const [prompt, setPrompt] = useState('');
  const [result, setResult] = useState('');
  const [loading, setLoading] = useState(false);
  const [reviewStatus, setReviewStatus] = useState('draft');

  const generate = async () => {
    if (!prompt.trim()) return;
    setLoading(true); setResult('Dang tao...');
    const res = await api('/api/story/ai', 'POST', { gen_type: genType, prompt, context: '' });
    setResult(res.success ? res.result : `Loi: ${res.message}`);
    setLoading(false);
  };

  const approve = async () => {
    // Would approve the last generated content
    setReviewStatus('approved');
  };

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap gap-2">
        {['quest','dialog','story','item_desc','event_desc','announcement'].map(t => (
          <button key={t} onClick={() => setGenType(t)}
            className={`px-3 py-1.5 rounded-lg text-xs font-medium ${genType === t ? 'bg-[#6c3ef3] text-white' : 'bg-white/5 text-gray-400'}`}>
            {t}
          </button>
        ))}
      </div>

      <textarea value={prompt} onChange={e => setPrompt(e.target.value)}
        placeholder="Nhap yeu cau cho AI..." rows={3}
        className="w-full bg-[#1a1a35] border border-white/10 rounded-lg p-3 text-sm text-white placeholder-gray-600 resize-none" />

      <div className="flex gap-2">
        <ActionButton onClick={generate}>{loading ? 'Dang tao...' : 'Tao noi dung'}</ActionButton>
        <ActionButton onClick={approve} variant="outline">Duyet & Ap Dung</ActionButton>
      </div>

      {/* Review status */}
      <div className="flex gap-2 text-xs">
        {['draft','review','testing','approved','published','rejected'].map(s => (
          <span key={s} className={`px-2 py-1 rounded ${reviewStatus === s ? 'bg-[#6c3ef3] text-white' : 'bg-white/5 text-gray-500'}`}>
            {s}
          </span>
        ))}
      </div>

      {result && (
        <div className="bg-[#0d0d24] border border-white/10 rounded-lg p-4">
          <div className="flex justify-between items-center mb-2">
            <span className="text-xs text-gray-400">Ket qua AI:</span>
            <button onClick={() => navigator.clipboard.writeText(result)} className="text-xs text-[#4ecca3]">Copy</button>
          </div>
          <pre className="text-xs text-gray-300 whitespace-pre-wrap max-h-64 overflow-y-auto">{result}</pre>

          <div className="mt-3 flex gap-2">
            <ActionButton onClick={() => setReviewStatus('testing')} variant="outline">Test tren SV test</ActionButton>
            <ActionButton onClick={() => setReviewStatus('approved')}>Duyet</ActionButton>
            <ActionButton onClick={() => setReviewStatus('rejected')} variant="danger">Tu choi</ActionButton>
          </div>
        </div>
      )}
    </div>
  );
}

//  Main Dashboard 

export default function AdminDashboard() {
  const [apiKey, setApiKey] = useState(localStorage.getItem('admin_key') || '');
  const [authed, setAuthed] = useState(false);
  const [activePanel, setActivePanel] = useState('status');
  const [panelData, setPanelData] = useState<TableRow[]>([]);
  const [statusData, setStatusData] = useState<Record<string, unknown>>({});
  const [loading, setLoading] = useState(false);
  const [sidebarOpen, setSidebarOpen] = useState(false);
  // nhom dang mo (dropdown) — mac dinh mo nhom cua panel dang xem
  const [openGroups, setOpenGroups] = useState<string[]>(['Tong Quan']);
  const toggleGroup = (g: string) =>
    setOpenGroups(prev => prev.includes(g) ? prev.filter(x => x !== g) : [...prev, g]);

  const login = () => {
    if (apiKey.length > 5) {
      localStorage.setItem('admin_key', apiKey);
      setAuthed(true);
      loadPanel('status');
    }
  };

  const loadPanel = useCallback(async (key: string) => {
    setActivePanel(key);
    setSidebarOpen(false);
    setLoading(true);
    // tu mo dropdown chua panel vua chon
    const g = PANELS.find(p => p.key === key)?.group;
    if (g) setOpenGroups(prev => prev.includes(g) ? prev : [...prev, g]);

    const panel = PANELS.find(p => p.key === key);
    if (!panel) { setLoading(false); return; }

    try {
      const res = await api(panel.endpoint);
      if (key === 'status') {
        setStatusData(res);
        setPanelData([]);
      } else {
        setPanelData(res[panel.dataKey] || (Array.isArray(res) ? res : []));
      }
    } catch (e) {
      setPanelData([]);
    }
    setLoading(false);
  }, []);

  useEffect(() => {
    if (authed) loadPanel(activePanel);
  }, [authed]);

  //  Login screen 

  if (!authed) {
    return (
      <div className="min-h-screen bg-[#080818] flex items-center justify-center p-4">
        <div className="w-full max-w-sm bg-[#12122a] border border-white/10 rounded-2xl p-8">
          <h1 className="text-xl font-bold text-white text-center mb-6">Admin Panel</h1>
          <input value={apiKey} onChange={e => setApiKey(e.target.value)} type="password"
            placeholder="Admin API Key..."
            className="w-full bg-[#1a1a35] border border-white/10 rounded-lg p-3 text-sm text-white mb-4"
            onKeyDown={e => e.key === 'Enter' && login()} />
          <button onClick={login}
            className="w-full py-3 bg-[#6c3ef3] text-white rounded-lg font-semibold text-sm hover:bg-[#8a5cf5]">
            Dang Nhap
          </button>
        </div>
      </div>
    );
  }

  //  Groups 

  const GROUP_ORDER = ['Tong Quan','Nguoi Choi','Noi Dung','Kinh Te','Xa Hoi','Tinh Nang','He Thong','AI & Review'];
  const groups = [...new Set(PANELS.map(p => p.group))]
    .sort((a, b) => GROUP_ORDER.indexOf(a) - GROUP_ORDER.indexOf(b));
  const currentPanel = PANELS.find(p => p.key === activePanel);

  return (
    <div className="min-h-screen bg-[#080818] text-gray-200 flex">

      {/*  Sidebar  */}
      <aside className={`fixed md:static inset-y-0 left-0 z-40 w-56 bg-[#0a0a1e] border-r border-white/5
        transform transition-transform duration-200
        ${sidebarOpen ? 'translate-x-0' : '-translate-x-full md:translate-x-0'}`}>

        <div className="p-4 border-b border-white/5">
          <h2 className="text-sm font-bold text-[#f0c050] tracking-wider">NEXUS ADMIN</h2>
          <p className="text-[10px] text-gray-600">Web Dashboard</p>
        </div>

        <nav className="overflow-y-auto h-[calc(100vh-60px)] pb-20">
          {groups.map(g => {
            const open = openGroups.includes(g);
            const hasActive = PANELS.some(p => p.group === g && p.key === activePanel);
            return (
              <div key={g}>
                <button onClick={() => toggleGroup(g)}
                  className={`w-full flex items-center justify-between px-4 pt-3 pb-2 text-[10px] uppercase tracking-wider font-medium transition-colors ${
                    hasActive ? 'text-[#f0c050]' : 'text-gray-500 hover:text-gray-300'
                  }`}>
                  <span>{g}</span>
                  <span className={`transition-transform duration-150 ${open ? 'rotate-90' : ''}`}>{'\u203A'}</span>
                </button>
                {open && PANELS.filter(p => p.group === g).map(p => (
                  <button key={p.key} onClick={() => loadPanel(p.key)}
                    className={`w-full text-left px-6 py-2 text-xs transition-colors ${
                      activePanel === p.key
                        ? 'bg-[#6c3ef3]/20 text-[#8a5cf5] border-l-2 border-[#6c3ef3]'
                        : 'text-gray-400 hover:bg-white/5 hover:text-white border-l-2 border-transparent'
                    }`}>
                    {p.label}
                  </button>
                ))}
              </div>
            );
          })}
        </nav>
      </aside>

      {/*  Overlay (mobile)  */}
      {sidebarOpen && (
        <div className="fixed inset-0 bg-black/50 z-30 md:hidden" onClick={() => setSidebarOpen(false)} />
      )}

      {/*  Main content  */}
      <main className="flex-1 min-w-0">
        {/* Top bar */}
        <header className="sticky top-0 z-20 bg-[#080818]/90 backdrop-blur-xl border-b border-white/5 px-4 py-3 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <button className="md:hidden text-gray-400 text-xl" onClick={() => setSidebarOpen(!sidebarOpen)}>
              {'\u2630'}
            </button>
            <h1 className="text-sm font-semibold text-white">{currentPanel?.label || 'Dashboard'}</h1>
          </div>
          <div className="flex items-center gap-2">
            <button onClick={() => loadPanel(activePanel)} className="text-xs text-gray-400 hover:text-white px-2 py-1 rounded bg-white/5">
              Tai Lai
            </button>
            <button onClick={() => { setAuthed(false); localStorage.removeItem('admin_key'); }}
              className="text-xs text-red-400 hover:text-red-300 px-2 py-1 rounded bg-white/5">
              Dang Xuat
            </button>
          </div>
        </header>

        {/* Content area */}
        <div className="p-4">
          {loading && <div className="text-center py-8 text-gray-500 text-sm">Dang tai...</div>}

          {!loading && activePanel === 'status' && (
            <div className="space-y-6">
              <div className="grid grid-cols-2 md:grid-cols-5 gap-3">
                <StatCard label="Online" value={String(statusData.online ?? statusData.online_count ?? '—')} color="#4ecca3" />
                <StatCard label="RAM" value={statusData.ram_pct != null ? `${statusData.ram_pct}%` : '—'}
                  color={(Number(statusData.ram_pct) || 0) > 80 ? '#e84545' : '#6c3ef3'} />
                <StatCard label="CPU (load)" value={statusData.cpu_load != null ? String(statusData.cpu_load) : '—'} color="#4ecca3" />
                <StatCard label="Uptime" value={statusData.uptime_ms != null
                  ? `${Math.floor(Number(statusData.uptime_ms)/3600000)}h${Math.floor((Number(statusData.uptime_ms)%3600000)/60000)}m` : 'Offline'} color="#f0c050" />
                <StatCard label="RAM dung" value={statusData.ram_used_mb != null ? `${statusData.ram_used_mb}/${statusData.ram_max_mb}MB` : '—'} />
              </div>

              <div className="grid md:grid-cols-3 gap-4">
                <div className="bg-[#12122a] border border-white/5 rounded-xl p-4 space-y-2">
                  <h3 className="text-sm font-medium text-white mb-1">Dieu khien Server</h3>
                  <div className="flex flex-wrap gap-2">
                    <ActionButton variant="danger" onClick={() => api('/api/maintenance', 'POST', { enabled: 'true' })}>Bao Tri</ActionButton>
                    <ActionButton variant="outline" onClick={async () => { const r = await api('/api/save-data', 'POST', {}); alert(`Da luu ${r?.saved ?? 0} nguoi choi`); }}>Luu Data</ActionButton>
                    <ActionButton variant="danger" onClick={async () => { if (confirm('Kick TAT CA nguoi choi?')) { const r = await api('/api/kick-all', 'POST', {}); alert(`Da kick ${r?.kicked ?? 0}`); } }}>Kick All</ActionButton>
                    <ActionButton variant="outline" onClick={async () => { const v = window.prompt('He so EXP toan server (vd 2 = x2):', '1'); if (v) { await api('/api/exp-rate', 'POST', { exp: v }); alert(`EXP rate = x${v}`); } }}>Doi EXP</ActionButton>
                  </div>
                </div>

                <div className="bg-[#12122a] border border-white/5 rounded-xl p-4 space-y-2">
                  <h3 className="text-sm font-medium text-white mb-1">Cong cu Quan tri</h3>
                  <div className="flex flex-wrap gap-2">
                    <ActionButton onClick={() => loadPanel('quick')}>Thao Tac Nhanh</ActionButton>
                    <ActionButton variant="outline" onClick={() => { const msg = window.prompt('Noi dung thong bao:'); if (msg) api('/api/broadcast', 'POST', { message: msg }); }}>Thong Bao</ActionButton>
                    <ActionButton variant="outline" onClick={() => loadPanel('players')}>Nguoi Choi</ActionButton>
                  </div>
                </div>

                <div className="bg-[#12122a] border border-white/5 rounded-xl p-4 space-y-2">
                  <h3 className="text-sm font-medium text-white mb-1">Toi uu He thong</h3>
                  <div className="flex flex-wrap gap-2">
                    <ActionButton variant="outline" onClick={async () => { const r = await api('/api/gc', 'POST', {}); alert(`Giai phong ${r?.freed_kb ?? 0}KB. Dang dung ${r?.used_mb ?? '?'}MB`); loadPanel('status'); }}>Giai phong RAM (GC)</ActionButton>
                    <ActionButton variant="outline" onClick={() => loadPanel('status')}>Lam moi</ActionButton>
                  </div>
                </div>
              </div>
            </div>
          )}

          {!loading && activePanel === 'ai' && <AIPanel />}

          {!loading && activePanel === 'quick' && <QuickActions />}

          {!loading && activePanel !== 'status' && activePanel !== 'ai' && activePanel !== 'quick' && (
            <div className="bg-[#12122a] border border-white/5 rounded-xl overflow-hidden">
              {currentPanel?.editable
                ? <ConfigEditor endpoint={currentPanel.endpoint} pk={currentPanel.pk || 'id'} data={panelData} onReload={() => loadPanel(activePanel)} />
                : <DataTable data={panelData} />}
            </div>
          )}
        </div>
      </main>
    </div>
  );
}
