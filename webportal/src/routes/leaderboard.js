const router = require('express').Router();
const { query } = require('../db');
const axios = require('axios');

router.get('/', async (req, res) => {
  const type = req.query.type||'FASHION';
  const svId = req.query.server||'';
  const { rows: servers } = await query('SELECT * FROM game_servers WHERE is_active=true ORDER BY server_id').catch(()=>({rows:[]}));
  let leaderboard = [];
  if (servers.length) {
    const sv = svId ? servers.find(s=>s.server_id==svId) : servers[0];
    if (sv) {
      try {
        const r = await axios.get(`http://${sv.api_host}:${sv.api_port}/api/admin/leaderboard/${type}`,
          { auth: { username: sv.admin_user, password: sv.admin_pass }, timeout: 4000 });
        leaderboard = r.data.entries||[];
      } catch {}
    }
  }
  res.render('pages/leaderboard', { title: 'Bảng Xếp Hạng', servers, leaderboard, type, selectedServer: svId });
});
module.exports = router;
