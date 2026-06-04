const router = require('express').Router();
const { getAll, getOne, query } = require('../services/db');

async function redeemGiftcode(code, character_name, server_id) {
  const gc = await getOne(
    `SELECT * FROM giftcodes WHERE code=UPPER($1) AND is_active=TRUE
     AND (expires_at IS NULL OR expires_at > NOW())
     AND (server_id IS NULL OR server_id=$2)`,
    [code, server_id || 0]);
  if (!gc) return { ok: false, error: 'Mã không hợp lệ hoặc đã hết hạn' };
  if (gc.uses >= gc.max_uses) return { ok: false, error: 'Mã đã hết lượt sử dụng' };
  const used = await getOne('SELECT id FROM giftcode_uses WHERE code_id=$1 AND character_name=$2', [gc.id, character_name]);
  if (used) return { ok: false, error: 'Nhân vật đã dùng mã này rồi' };
  await query('INSERT INTO giftcode_uses(code_id,character_name,server_id) VALUES($1,$2,$3)', [gc.id, character_name, server_id || null]);
  await query('UPDATE giftcodes SET uses=uses+1 WHERE id=$1', [gc.id]);
  return { ok: true, rewards: gc.rewards, message: 'Nhập mã thành công!' };
}

// Public news
router.get('/news', async (req, res) => {
  const { category, server_id, limit } = req.query;
  const lim = Math.min(parseInt(limit) || 10, 50);
  let sql = `SELECT id,title,slug,category,thumbnail,excerpt,published_at,views,is_pinned
    FROM news_articles WHERE is_published=TRUE`;
  const params = [];
  if (category) { sql += ` AND category=$${params.length + 1}`; params.push(category); }
  if (server_id) { sql += ` AND (server_id=$${params.length + 1} OR server_id IS NULL)`; params.push(server_id); }
  sql += ` ORDER BY is_pinned DESC, published_at DESC LIMIT ${lim}`;
  res.json({ articles: await getAll(sql, params) });
});

router.get('/news/:slug', async (req, res) => {
  const article = await getOne('SELECT * FROM news_articles WHERE slug=$1 AND is_published=TRUE', [req.params.slug]);
  if (!article) return res.status(404).json({ error: 'Not found' });
  await query('UPDATE news_articles SET views=views+1 WHERE id=$1', [article.id]);
  res.json(article);
});

// Public server list
router.get('/servers', async (req, res) => {
  const servers = await getAll('SELECT id,name,slug,type,status,max_players,description,opens_at FROM game_servers ORDER BY type,id');
  res.json({ servers });
});

// Giftcode redeem
router.post('/giftcode/redeem', async (req, res) => {
  try {
    const { code, character_name, server_id } = req.body;
    if (!code || !character_name) return res.json({ ok: false, error: 'Thiếu thông tin' });
    const result = await redeemGiftcode(code, character_name, server_id);
    res.json(result);
  } catch (e) {
    res.json({ ok: false, error: e.message });
  }
});

// Cross-server ban check
router.get('/ban-check/:characterName', async (req, res) => {
  const ban = await getOne(
    'SELECT * FROM cross_server_bans WHERE character_name=$1 AND (expires_at IS NULL OR expires_at > NOW())',
    [req.params.characterName]);
  res.json({ banned: !!ban, reason: ban?.reason || null });
});

module.exports = router;
