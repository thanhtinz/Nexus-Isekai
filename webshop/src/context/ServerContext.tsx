import { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { api } from '../api/client';

interface Server { id: number; name: string; status: number; }
interface ServerContextType {
  servers: Server[];
  selectedServer: number;
  selectedServerName: string;
  setSelectedServer: (id: number) => void;
}

const ServerContext = createContext<ServerContextType>({
  servers: [], selectedServer: 0, selectedServerName: '', setSelectedServer: () => {}
});

export function ServerProvider({ children }: { children: ReactNode }) {
  const [servers, setServers] = useState<Server[]>([]);
  const [selectedServer, setServer] = useState<number>(() =>
    parseInt(localStorage.getItem('selected_server') || '0')
  );

  useEffect(() => {
    api.get('/api/servers').then((r: any) => {
      const list = (r.data.servers || []).filter((s: Server) => s.status === 1);
      setServers(list);
      if (selectedServer === 0 && list.length > 0) setServer(list[0].id);
    }).catch(() => {});
  }, []);

  const setSelectedServer = (id: number) => {
    setServer(id);
    localStorage.setItem('selected_server', String(id));
  };

  return (
    <ServerContext.Provider value={{
      servers, selectedServer,
      selectedServerName: servers.find(s => s.id === selectedServer)?.name || '',
      setSelectedServer
    }}>
      {children}
    </ServerContext.Provider>
  );
}

export const useServer = () => useContext(ServerContext);
