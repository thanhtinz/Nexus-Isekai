import { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { api } from '../api/client';

interface Character { id: number; name: string; class_id: number; level: number; gender: number; }
interface AuthContextType {
  isLoggedIn: boolean;
  accountId: number;
  username: string;
  selectedServer: number;
  selectedChar: Character | null;
  characters: Character[];
  servers: any[];
  login: (user: string, pass: string) => Promise<boolean>;
  logout: () => void;
  selectServer: (id: number) => void;
  selectCharacter: (char: Character) => void;
  ready: boolean; // logged in + server + character selected
}

const AuthContext = createContext<AuthContextType>({} as AuthContextType);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [accountId, setAccountId] = useState(() => parseInt(sessionStorage.getItem('acc_id') || '0'));
  const [username, setUsername] = useState(() => sessionStorage.getItem('username') || '');
  const [token, setToken] = useState(() => sessionStorage.getItem('token') || '');
  const [selectedServer, setSelectedServer] = useState(() => parseInt(sessionStorage.getItem('server_id') || '0'));
  const [selectedChar, setSelectedChar] = useState<Character | null>(null);
  const [characters, setCharacters] = useState<Character[]>([]);
  const [servers, setServers] = useState<any[]>([]);

  const isLoggedIn = accountId > 0 && token.length > 0;
  const ready = isLoggedIn && selectedServer > 0 && selectedChar !== null;

  useEffect(() => {
    api.get('/api/servers').then(r => setServers((r.data.servers || []).filter((s: any) => s.status === 1))).catch(() => {});
  }, []);

  useEffect(() => {
    if (isLoggedIn && selectedServer > 0) {
      api.get(`/api/characters?account_id=${accountId}&server_id=${selectedServer}`)
        .then(r => { const chars = r.data.characters || []; setCharacters(chars); if (chars.length > 0 && !selectedChar) setSelectedChar(chars[0]); })
        .catch(() => setCharacters([]));
    }
  }, [accountId, selectedServer]);

  const login = async (user: string, pass: string): Promise<boolean> => {
    try {
      const r = await api.post('/api/web-login', { username: user, password: pass });
      if (r.data.success) {
        setAccountId(r.data.account_id);
        setUsername(user);
        setToken(r.data.token || 'session');
        sessionStorage.setItem('acc_id', String(r.data.account_id));
        sessionStorage.setItem('username', user);
        sessionStorage.setItem('token', r.data.token || 'session');
        return true;
      }
    } catch {}
    return false;
  };

  const logout = () => {
    setAccountId(0); setUsername(''); setToken(''); setSelectedServer(0); setSelectedChar(null);
    sessionStorage.clear();
  };

  const selectServer = (id: number) => {
    setSelectedServer(id); setSelectedChar(null); setCharacters([]);
    sessionStorage.setItem('server_id', String(id));
  };

  const selectCharacter = (char: Character) => setSelectedChar(char);

  return (
    <AuthContext.Provider value={{ isLoggedIn, accountId, username, selectedServer, selectedChar, characters, servers, login, logout, selectServer, selectCharacter, ready }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);
