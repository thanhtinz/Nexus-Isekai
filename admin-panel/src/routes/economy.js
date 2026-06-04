const router = require('express').Router();
const { getAll, getOne } = require('../services/db');
const { callServerAPI } = require('../services/ServerManager');

router.get('/', async(req,res) => {
  const { server_id } = req.query;
  const servers = await getAll('SELECT id,name,slug FROM game_servers ORDER BY id');
  const sv = server_id ? await getOne('SELECT * FROM game_servers WHERE id=$1',[server_id]) : servers[0];
  let market = [], leaderboard = [];
  if(sv) {
    try { const d = await callServerAPI(sv,'GET','/api/admin/leaderboard/WEALTH'); leaderboard=d.entries||[]; }catch(e){}
    try {
      const d = await callServerAPI(sv,'GET','/api/admin/players');
      // Enrich with gold stats
      const players = d.players||[];
      market = players.sort((a,b)=>(b.gold||0)-(a.gold||0)).slice(0,20);
    }catch(e){}
  }
  res.render('pages/economy/index', {
    title: 'Kinh tế & Giao dịch', admin: req.session.admin,
    servers, selectedServer: sv, market, leaderboard, activePage: 'economy'
  });
});

// Give items (multi-server)
router.post('/give-gold-all', async(req,res) => {
  const { amount, message } = req.body;
  const servers = await getAll("SELECT * FROM game_servers WHERE status='running'");
  const results = await Promise.allSettled(servers.map(sv =>
    callServerAPI(sv,'POST','/api/admin/broadcast',
      { message: `[Hệ Thống] ${message||'Tặng quà!'}` })
  ));
  res.json({ ok:true, sent: results.filter(r=>r.status==='fulfilled').length, total: servers.length });
});

module.exports = router;
