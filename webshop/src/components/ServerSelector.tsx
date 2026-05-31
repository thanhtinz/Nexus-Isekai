import { useServer } from '../context/ServerContext';

export default function ServerSelector() {
  const { servers, selectedServer, setSelectedServer } = useServer();

  if (servers.length <= 1) return null;

  return (
    <div className="flex items-center gap-2">
      <select value={selectedServer} onChange={e => setSelectedServer(Number(e.target.value))}
        className="bg-gray-700 text-white text-sm px-3 py-1.5 rounded-lg border border-gray-600 focus:border-yellow-500 outline-none">
        {servers.map(s => (
          <option key={s.id} value={s.id}>{s.name}</option>
        ))}
      </select>
    </div>
  );
}
