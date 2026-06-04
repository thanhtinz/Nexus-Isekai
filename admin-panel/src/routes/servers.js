const router = require('express').Router();
const { getAll, getOne, query } = require('../services/db');
const { startServer, stopServer, restartServer,
        getContainerLogs, getAllServersWithStatus,
        callServerAPI, getServerStatus } = require('../services/ServerManager');

router.get('/', async(req,res) => {
  const servers = await getAllServersWithStatus();
  res.render('pages/servers/index', {
    title: 'Quản lý Server', admin: req.session.admin,
    servers, activePage: 'servers'
  });
});

router.get('/add', async(req,res) => {
  res.render('pages/servers/add', {
    title: 'Thêm Server mới', admin: req.session.admin, activePage: 'servers'
  });
});

router.post('/add', async(req,res) => {
  const { name, slug, type, game_port, admin_port, db_name, description, max_players, host, api_user, api_pass } = req.body;
  try {
    await query(`INSERT INTO game_servers(name,slug,type,game_port,admin_port,db_name,description,max_players,host,api_user,api_pass)
      VALUES($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11)`,
      [name, slug.toLowerCase().replace(/\s+/g,'_'), type, parseInt(game_port), parseInt(admin_port),
       db_name||null, description||null, parseInt(max_players)||500, host||'localhost',
       api_user||'gm', api_pass||'gm_secret_2024']);
    await query('INSERT INTO admin_action_logs(admin_id,action,target_type,target_id,details,ip)VALUES($1,$2,$3,$4,$5,$6)',
      [req.session.admin.id,'add_server','server',slug,JSON.stringify({name,type}),req.ip]);
    res.redirect('/servers?success=Server+đã+được+thêm');
  } catch(e) { res.redirect('/servers/add?error='+encodeURIComponent(e.message)); }
});

router.get('/:id', async(req,res) => {
  const sv = await getOne('SELECT * FROM game_servers WHERE id=$1', [req.params.id]);
  if(!sv) return res.redirect('/servers');
  const [liveStatus, logs] = await Promise.all([
    getServerStatus(sv),
    getAll(`SELECT * FROM server_logs WHERE server_id=$1 ORDER BY created_at DESC LIMIT 100`, [sv.id])
  ]);
  let players = [], gameEvents = [];
  try { const d = await callServerAPI(sv,'GET','/api/admin/players'); players = d.players||[]; } catch(e){}
  try { const d = await callServerAPI(sv,'GET','/api/admin/events'); gameEvents = d.active||d||[]; } catch(e){}
  res.render('pages/servers/detail', {
    title: `Server: ${sv.name}`, admin: req.session.admin,
    server: sv, liveStatus, logs, players, gameEvents, activePage: 'servers'
  });
});

router.post('/:id/start',   async(req,res) => { try { await startServer(parseInt(req.params.id)); res.json({ok:true}); } catch(e){res.json({ok:false,error:e.message});} });
router.post('/:id/stop',    async(req,res) => { try { await stopServer(parseInt(req.params.id)); res.json({ok:true}); } catch(e){res.json({ok:false,error:e.message});} });
router.post('/:id/restart', async(req,res) => { try { await restartServer(parseInt(req.params.id)); res.json({ok:true}); } catch(e){res.json({ok:false,error:e.message});} });

router.get('/:id/logs', async(req,res) => {
  const sv = await getOne('SELECT * FROM game_servers WHERE id=$1',[req.params.id]);
  const dockerLogs = await getContainerLogs(sv?.id, 300).catch(()=>[]);
  const dbLogs = await getAll(`SELECT * FROM server_logs WHERE server_id=$1 ORDER BY created_at DESC LIMIT 200`,[req.params.id]);
  res.json({ docker: dockerLogs, db: dbLogs });
});

// Trigger event on specific server
router.post('/:id/trigger-event', async(req,res) => {
  const sv = await getOne('SELECT * FROM game_servers WHERE id=$1',[req.params.id]);
  if(!sv) return res.json({ok:false,error:'Not found'});
  try {
    const r = await callServerAPI(sv,'POST','/api/admin/events/trigger',{type:req.body.type});
    res.json({ok:true,message:r});
  } catch(e){ res.json({ok:false,error:e.message}); }
});

// Delete server
router.delete('/:id', async(req,res) => {
  const sv = await getOne('SELECT * FROM game_servers WHERE id=$1',[req.params.id]);
  if(sv) {
    await stopServer(sv.id).catch(()=>{});
    await query('DELETE FROM game_servers WHERE id=$1',[sv.id]);
  }
  res.json({ok:true});
});

module.exports = router;
