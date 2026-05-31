import { useAuth } from '../context/AuthContext';

const CLASS_NAMES = ['','Swordsman','Mage','Gunner','Slinger','Axeman','Brawler','Archer'];

export default function ServerSelectPage() {
  const { servers, selectedServer, selectServer, characters, selectCharacter, selectedChar } = useAuth();

  return (
    <div className="min-h-screen bg-gradient-to-b from-gray-900 to-gray-800 text-white">
      <div className="max-w-2xl mx-auto px-4 py-12">
        <h1 className="text-2xl font-bold text-center mb-8 text-yellow-400">Select Server</h1>

        <div className="grid gap-2 mb-8">
          {servers.map((s: any) => (
            <button key={s.id} onClick={() => selectServer(s.id)}
              className={`flex items-center justify-between p-4 rounded-xl border transition
                ${selectedServer === s.id ? 'bg-yellow-500/10 border-yellow-500' : 'bg-gray-800 border-gray-700 hover:border-gray-500'}`}>
              <span className="font-bold">{s.name}</span>
              <span className="text-xs text-gray-400">{s.group_name}</span>
            </button>
          ))}
        </div>

        {selectedServer > 0 && (
          <>
            <h2 className="text-xl font-bold mb-4 text-yellow-400">Select Character</h2>
            {characters.length === 0 ? (
              <p className="text-gray-500 text-center py-8">No characters on this server</p>
            ) : (
              <div className="grid gap-2">
                {characters.map(c => (
                  <button key={c.id} onClick={() => selectCharacter(c)}
                    className={`flex items-center justify-between p-4 rounded-xl border transition
                      ${selectedChar?.id === c.id ? 'bg-yellow-500/10 border-yellow-500' : 'bg-gray-800 border-gray-700 hover:border-gray-500'}`}>
                    <div>
                      <span className="font-bold">{c.name}</span>
                      <span className="text-sm text-gray-400 ml-2">{CLASS_NAMES[c.class_id] || 'Unknown'}</span>
                    </div>
                    <span className="text-sm text-gray-400">Lv.{c.level}</span>
                  </button>
                ))}
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}
