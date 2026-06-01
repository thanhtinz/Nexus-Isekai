import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const CLASS_NAMES = ['','Swordsman','Mage','Gunner','Slinger','Axeman','Brawler','Archer','Assassin'];

export default function ServerSelectPage() {
  const navigate = useNavigate();
  const { servers, selectedServer, selectServer, characters, selectCharacter, selectedChar } = useAuth();

  return (
    <div className="min-h-screen bg-gradient-to-b from-gray-900 to-gray-800 text-[#2a2350]">
      <div className="max-w-2xl mx-auto px-4 py-12">
        <h1 className="text-2xl font-bold text-center mb-8 text-yellow-400">Select Server</h1>

        <div className="grid gap-2 mb-8">
          {servers.map((s: any) => (
            <button key={s.id} onClick={() => selectServer(s.id)}
              className={`flex items-center justify-between p-4 rounded-xl border transition
                ${selectedServer === s.id ? 'bg-yellow-500/10 border-yellow-500' : 'bg-[#fffdf7] border-[#ece3d0] hover:border-gray-500'}`}>
              <span className="font-bold">{s.name}</span>
              <span className="text-xs text-[#5b5380]">{s.group_name}</span>
            </button>
          ))}
        </div>

        {selectedServer > 0 && (
          <>
            <h2 className="text-xl font-bold mb-4 text-yellow-400">Select Character</h2>
            {characters.length === 0 ? (
              <p className="text-[#5b5380] text-center py-8">No characters on this server</p>
            ) : (
              <div className="grid gap-2">
                {characters.map(c => (
                  <button key={c.id} onClick={() => selectCharacter(c)}
                    className={`flex items-center justify-between p-4 rounded-xl border transition
                      ${selectedChar?.id === c.id ? 'bg-yellow-500/10 border-yellow-500' : 'bg-[#fffdf7] border-[#ece3d0] hover:border-gray-500'}`}>
                    <div>
                      <span className="font-bold">{c.name}</span>
                      <span className="text-sm text-[#5b5380] ml-2">{CLASS_NAMES[c.class_id] || 'Unknown'}</span>
                    </div>
                    <span className="text-sm text-[#5b5380]">Lv.{c.level}</span>
                  </button>
                ))}
              </div>
            )}
          </>
        )}
      {selectedChar && (
            <div className="mt-8 text-center">
              <button onClick={() => navigate('/store')}
                className="bg-yellow-500 text-black font-bold px-8 py-3 rounded-xl hover:bg-yellow-400 transition">
                Continue as {selectedChar.name}
              </button>
              <p className="text-xs text-[#5b5380] mt-2">All purchases will go to this character</p>
            </div>
          )}
      </div>
    </div>
  );
}
