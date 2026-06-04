const router = require('express').Router();
const { getAll } = require('../services/db');

router.get('/', async(req,res) => {
  const { server_id, action, limit } = req.query;
  const lim = Math.min(parseInt(limit)||100, 500);
  let sql = `SELECT al.*,au.username FROM admin_action_logs al LEFT JOIN admin_users au ON al.admin_id=au.id WHERE 1=1`;
  const params = [];
  if(action){ sql += ` AND al.action=$${params.length+1}`; params.push(action); }
  sql += ` ORDER BY al.created_at DESC LIMIT ${lim}`;
  const logs = await getAll(sql, params);

  let serverLogs = [];
  if(server_id){
    serverLogs = await getAll(
      'SELECT * FROM server_logs WHERE server_id=$1 ORDER BY created_at DESC LIMIT 200',
      [server_id]);
  }
  const servers = await getAll('SELECT id,name,slug FROM game_servers ORDER BY id');
  res.render('pages/logs/index', {
    title: 'Nhật ký hệ thống', admin: req.session.admin,
    logs, serverLogs, servers, selectedServer: server_id||'',
    action: action||'', activePage: 'logs'
  });
});

module.exports = router;
