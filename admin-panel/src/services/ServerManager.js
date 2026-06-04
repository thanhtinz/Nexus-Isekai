const Docker = require('dockerode');
const { query, getOne, getAll } = require('./db');
const axios = require('axios');

let docker;
try { docker = new Docker({ socketPath: process.env.DOCKER_SOCKET || '/var/run/docker.sock' }); }
catch(e) { docker = null; console.warn('[Docker] Not available:', e.message); }

// ── Live server status cache
const statusCache = new Map(); // serverId -> { online, players, uptime, ts }

async function getServerStatus(server) {
  const cached = statusCache.get(server.id);
  if(cached && Date.now() - cached.ts < 15000) return cached;
  try {
    const base = `http://${server.host}:${server.admin_port}`;
    const res = await axios.get(`${base}/api/admin/status`, {
      auth: { username: server.api_user, password: server.api_pass },
      timeout: 3000
    });
    const d = res.data;
    const status = { online: d.online||0, zones: d.zones||0, gameTime: d.gameTime||'--',
      season: d.season||'--', fullMoon: d.fullMoon||false,
      activeEvents: d.activeEvents||0, status: 'running', ts: Date.now() };
    statusCache.set(server.id, status);
    await query("UPDATE game_servers SET status='running' WHERE id=$1", [server.id]);
    return status;
  } catch(e) {
    const status = { online: 0, status: 'stopped', error: e.message, ts: Date.now() };
    statusCache.set(server.id, status);
    await query("UPDATE game_servers SET status='stopped' WHERE id=$1", [server.id]).catch(()=>{});
    return status;
  }
}

async function getAllServersWithStatus() {
  const servers = await getAll('SELECT * FROM game_servers ORDER BY id');
  return Promise.all(servers.map(async sv => ({
    ...sv, liveStatus: await getServerStatus(sv)
  })));
}

async function callServerAPI(server, method, path, data) {
  const base = `http://${server.host}:${server.admin_port}`;
  const res = await axios({ method, url: `${base}${path}`,
    auth: { username: server.api_user, password: server.api_pass },
    data, timeout: 8000 });
  return res.data;
}

// Docker-based server start
async function startServer(serverId) {
  const sv = await getOne('SELECT * FROM game_servers WHERE id=$1', [serverId]);
  if(!sv) throw new Error('Server not found');
  if(!docker) throw new Error('Docker not available — start manually');

  await query("UPDATE game_servers SET status='starting',updated_at=NOW() WHERE id=$1", [serverId]);
  await logServer(serverId, 'INFO', `Starting server ${sv.name}...`);

  const containerName = `fro-game-${sv.slug}`;

  // Remove old container if exists
  try {
    const old = docker.getContainer(containerName);
    await old.stop().catch(()=>{});
    await old.remove().catch(()=>{});
  } catch(e) {}

  const container = await docker.createContainer({
    Image: 'fro-game-server:latest',
    name: containerName,
    ExposedPorts: {
      [`${sv.game_port}/tcp`]: {},
      [`${sv.admin_port}/tcp`]: {}
    },
    HostConfig: {
      PortBindings: {
        [`${sv.game_port}/tcp`]: [{ HostPort: String(sv.game_port) }],
        [`${sv.admin_port}/tcp`]: [{ HostPort: String(sv.admin_port) }],
      },
      RestartPolicy: { Name: 'unless-stopped' }
    },
    Env: [
      `GAME_PORT=${sv.game_port}`,
      `HTTP_PORT=${sv.admin_port}`,
      `DB_URL=jdbc:postgresql://${process.env.DB_HOST||'postgres'}:5432/${sv.db_name||'fantasyrealm'}`,
      `DB_USER=${process.env.DB_USER||'fro'}`,
      `DB_PASS=${process.env.DB_PASS||'fro123'}`,
      `REDIS_HOST=${process.env.REDIS_HOST||'redis'}`,
      `ADMIN_USER=${sv.api_user}`,
      `ADMIN_PASS=${sv.api_pass}`,
      `SERVER_NAME=${sv.name}`,
      `SERVER_TYPE=${sv.type}`,
    ]
  });
  await container.start();
  const info = await container.inspect();
  await query("UPDATE game_servers SET container_id=$1,status='running',updated_at=NOW() WHERE id=$2",
    [info.Id, serverId]);
  await logServer(serverId, 'INFO', `Server ${sv.name} started (container: ${info.Id.slice(0,12)})`);
  return { success: true, containerId: info.Id };
}

async function stopServer(serverId) {
  const sv = await getOne('SELECT * FROM game_servers WHERE id=$1', [serverId]);
  if(!sv) throw new Error('Server not found');
  if(sv.container_id && docker) {
    const c = docker.getContainer(sv.container_id);
    await c.stop().catch(()=>{});
  }
  await query("UPDATE game_servers SET status='stopped',container_id=NULL,updated_at=NOW() WHERE id=$1", [serverId]);
  await logServer(serverId, 'INFO', `Server ${sv.name} stopped`);
  return { success: true };
}

async function restartServer(serverId) {
  await stopServer(serverId);
  await new Promise(r => setTimeout(r, 2000));
  return startServer(serverId);
}

async function getContainerLogs(serverId, tail=200) {
  const sv = await getOne('SELECT * FROM game_servers WHERE id=$1', [serverId]);
  if(!sv?.container_id || !docker) return [];
  const container = docker.getContainer(sv.container_id);
  const stream = await container.logs({ stdout:true, stderr:true, tail, timestamps:true });
  return stream.toString().split('\n').filter(Boolean).slice(-tail);
}

async function logServer(serverId, level, message) {
  await query('INSERT INTO server_logs(server_id,level,message) VALUES($1,$2,$3)',
    [serverId, level, message]).catch(()=>{});
}

module.exports = { getServerStatus, getAllServersWithStatus, callServerAPI,
  startServer, stopServer, restartServer, getContainerLogs, logServer };
