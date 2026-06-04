const router = require('express').Router();
const { getAll, getOne } = require('../services/db');
const { getAllServersWithStatus } = require('../services/ServerManager');

router.get('/', async(req,res) => {
  try {
    const [servers, recentNews, recentGiftcodes, recentLogs] = await Promise.all([
      getAllServersWithStatus(),
      getAll('SELECT * FROM news_articles ORDER BY created_at DESC LIMIT 5'),
      getAll('SELECT * FROM giftcodes ORDER BY created_at DESC LIMIT 5'),
      getAll('SELECT al.*,au.username FROM admin_action_logs al LEFT JOIN admin_users au ON al.admin_id=au.id ORDER BY al.created_at DESC LIMIT 20'),
    ]);
    const totalOnline = servers.reduce((s,sv) => s + (sv.liveStatus?.online||0), 0);
    res.render('pages/dashboard/index', {
      title: 'Dashboard', admin: req.session.admin,
      servers, totalOnline, recentNews, recentGiftcodes, recentLogs,
      activePage: 'dashboard'
    });
  } catch(e) {
    res.render('pages/dashboard/index', {
      title: 'Dashboard', admin: req.session.admin,
      servers: [], totalOnline: 0, recentNews: [], recentGiftcodes: [], recentLogs: [],
      error: e.message, activePage: 'dashboard'
    });
  }
});
module.exports = router;
