const router = require('express').Router();
const { getAll, getOne, query } = require('../services/db');
const { callServerAPI } = require('../services/ServerManager');

router.get('/', async(req,res) => {
  const { server_id } = req.query;
  const servers = await getAll('SELECT id,name,slug,type FROM game_servers ORDER BY id');
  const sv = server_id ? await getOne('SELECT * FROM game_servers WHERE id=$1',[server_id]) : servers[0];
  const dbEvents = await getAll(
    server_id
      ? 'SELECT e.*,au.username as creator FROM game_events e LEFT JOIN admin_users au ON e.created_by=au.id WHERE e.server_id=$1 ORDER BY e.created_at DESC'
      : 'SELECT e.*,au.username as creator,gs.name as server_name FROM game_events e LEFT JOIN admin_users au ON e.created_by=au.id LEFT JOIN game_servers gs ON e.server_id=gs.id ORDER BY e.created_at DESC',
    server_id ? [server_id] : []
  );
  let liveEvents = [];
  if(sv){ try{ const d=await callServerAPI(sv,'GET','/api/admin/events'); liveEvents=d.active||d||[]; }catch(e){} }
  res.render('pages/events/index', {
    title: 'Sự kiện', admin: req.session.admin,
    servers, selectedServer: sv, dbEvents, liveEvents, activePage: 'events'
  });
});

router.post('/trigger', async(req,res) => {
  const { server_id, type, all_servers } = req.body;
  try {
    let targets = [];
    if(all_servers) targets = await getAll("SELECT * FROM game_servers WHERE status='running'");
    else { const sv = await getOne('SELECT * FROM game_servers WHERE id=$1',[server_id]); if(sv) targets=[sv]; }

    const results = await Promise.allSettled(targets.map(sv =>
      callServerAPI(sv,'POST','/api/admin/events/trigger',{type})
    ));
    const ok = results.filter(r=>r.status==='fulfilled').length;
    await query('INSERT INTO admin_action_logs(admin_id,action,target_type,target_id,details,ip)VALUES($1,$2,$3,$4,$5,$6)',
      [req.session.admin.id,'trigger_event','event',type,JSON.stringify({server_id,all_servers}),req.ip]);
    res.json({ ok:true, sent:ok, total:targets.length });
  } catch(e){ res.json({ok:false,error:e.message}); }
});

router.post('/schedule', async(req,res) => {
  const { name, type, description, server_id, starts_at, ends_at, rewards } = req.body;
  try {
    await query(
      'INSERT INTO game_events(name,type,description,server_id,starts_at,ends_at,rewards,created_by)VALUES($1,$2,$3,$4,$5,$6,$7,$8)',
      [name, type, description, server_id||null,
       starts_at||null, ends_at||null,
       JSON.stringify(rewards ? rewards.split(',').map(r=>r.trim()) : []),
       req.session.admin.id]);
    res.json({ok:true});
  } catch(e){ res.json({ok:false,error:e.message}); }
});

module.exports = router;
