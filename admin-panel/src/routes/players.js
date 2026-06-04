const router = require('express').Router();
const { getAll, getOne, query } = require('../services/db');
const { callServerAPI, getAllServersWithStatus } = require('../services/ServerManager');
const axios = require('axios');

// Helper: get server
async function getServer(serverId) {
  return getOne('SELECT * FROM game_servers WHERE id=$1', [serverId]);
}

// List players (from game server API)
router.get('/', async(req,res) => {
  const { server_id, search } = req.query;
  const servers = await getAll('SELECT id,name,slug,type FROM game_servers ORDER BY id');
  const sv = server_id ? await getServer(server_id) : servers[0];
  let players = [], error = null;
  if(sv) {
    try {
      const data = await callServerAPI(sv, 'GET', '/api/admin/players');
      players = data.players || [];
      if(search) players = players.filter(p =>
        p.name?.toLowerCase().includes(search.toLowerCase()) ||
        String(p.playerId).includes(search));
    } catch(e) { error = 'Không kết nối được server: ' + e.message; }
  }
  res.render('pages/players/index', {
    title: 'Quản lý người chơi', admin: req.session.admin,
    servers, selectedServer: sv, players, search: search||'', error, activePage: 'players'
  });
});

// Player detail
router.get('/:playerId', async(req,res) => {
  const { server_id } = req.query;
  const sv = await getServer(server_id);
  if(!sv) return res.redirect('/players');
  try {
    const data = await callServerAPI(sv, 'GET', `/api/admin/players/${req.params.playerId}`);
    res.render('pages/players/detail', {
      title: 'Chi tiết người chơi', admin: req.session.admin,
      server: sv, player: data, activePage: 'players'
    });
  } catch(e) {
    res.redirect('/players?error=' + encodeURIComponent(e.message));
  }
});

// Give gold
router.post('/:playerId/give-gold', async(req,res) => {
  const { server_id, amount } = req.body;
  const sv = await getServer(server_id);
  if(!sv) return res.json({ ok: false, error: 'Server not found' });
  try {
    const r = await callServerAPI(sv, 'POST', `/api/admin/players/${req.params.playerId}/give-gold`, { amount: parseInt(amount) });
    await query('INSERT INTO admin_action_logs(admin_id,action,target_type,target_id,details,ip)VALUES($1,$2,$3,$4,$5,$6)',
      [req.session.admin.id,'give_gold','player',req.params.playerId,JSON.stringify({amount,server_id}),req.ip]);
    res.json({ ok: true, message: r });
  } catch(e) { res.json({ ok: false, error: e.message }); }
});

// Teleport
router.post('/:playerId/teleport', async(req,res) => {
  const { server_id, zoneId, x, y } = req.body;
  const sv = await getServer(server_id);
  if(!sv) return res.json({ ok: false, error: 'Server not found' });
  try {
    const r = await callServerAPI(sv, 'POST', `/api/admin/players/${req.params.playerId}/teleport`,
      { zoneId: parseInt(zoneId), x: parseFloat(x)||100, y: parseFloat(y)||100 });
    res.json({ ok: true, message: r });
  } catch(e) { res.json({ ok: false, error: e.message }); }
});

// Kick
router.delete('/:playerId/kick', async(req,res) => {
  const { server_id } = req.body;
  const sv = await getServer(server_id);
  if(!sv) return res.json({ ok: false, error: 'Server not found' });
  try {
    const r = await callServerAPI(sv, 'DELETE', `/api/admin/players/${req.params.playerId}/kick`);
    await query('INSERT INTO admin_action_logs(admin_id,action,target_type,target_id,details,ip)VALUES($1,$2,$3,$4,$5,$6)',
      [req.session.admin.id,'kick','player',req.params.playerId,JSON.stringify({server_id}),req.ip]);
    res.json({ ok: true, message: r });
  } catch(e) { res.json({ ok: false, error: e.message }); }
});

// Broadcast to server
router.post('/broadcast', async(req,res) => {
  const { server_id, message } = req.body;
  const sv = await getServer(server_id);
  if(!sv) return res.json({ ok: false, error: 'Server not found' });
  try {
    const r = await callServerAPI(sv, 'POST', '/api/admin/broadcast', { message });
    await query('INSERT INTO admin_action_logs(admin_id,action,target_type,target_id,details,ip)VALUES($1,$2,$3,$4,$5,$6)',
      [req.session.admin.id,'broadcast','server',server_id,JSON.stringify({message}),req.ip]);
    res.json({ ok: true, message: r });
  } catch(e) { res.json({ ok: false, error: e.message }); }
});

module.exports = router;
