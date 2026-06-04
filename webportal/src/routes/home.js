const router = require('express').Router();
const { query } = require('../db');
const axios = require('axios');

router.get('/', async (req, res) => {
  const { rows: news } = await query(
    'SELECT id,title,slug,excerpt,category,thumbnail,created_at FROM news WHERE is_published=true ORDER BY is_pinned DESC,created_at DESC LIMIT 6'
  ).catch(() => ({rows:[]}));
  
  const { rows: events } = await query(
    'SELECT * FROM news WHERE is_published=true AND category=$1 ORDER BY created_at DESC LIMIT 3',
    ['event']
  ).catch(() => ({rows:[]}));
  
  const { rows: servers } = await query('SELECT * FROM game_servers WHERE is_active=true ORDER BY server_id').catch(() => ({rows:[]}));
  
  // Get online counts
  const serverStats = await Promise.all(servers.map(async sv => {
    try {
      const r = await axios.get(`http://${sv.api_host}:${sv.api_port}/api/admin/status`, {
        auth: { username: sv.admin_user, password: sv.admin_pass }, timeout: 3000
      });
      return { ...sv, online: r.data.online||0, gameTime: r.data.gameTime, season: r.data.season };
    } catch { return { ...sv, online: 0 }; }
  }));
  
  res.render('pages/home', { title: 'Trang Chủ', news, events, servers: serverStats, page: 'home' });
});
module.exports = router;
