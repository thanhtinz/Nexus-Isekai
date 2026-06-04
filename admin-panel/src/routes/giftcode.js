const router = require('express').Router();
const { getAll, getOne, query } = require('../services/db');
const { v4: uuidv4 } = require('uuid');

router.get('/', async(req,res) => {
  const { server_id } = req.query;
  const servers = await getAll('SELECT id,name,slug FROM game_servers ORDER BY id');
  const codes = await getAll(`
    SELECT g.*,gs.name as server_name,au.username as creator_name,
      (SELECT COUNT(*) FROM giftcode_uses WHERE code_id=g.id) as real_uses
    FROM giftcodes g
    LEFT JOIN game_servers gs ON g.server_id=gs.id
    LEFT JOIN admin_users au ON g.created_by=au.id
    ORDER BY g.created_at DESC LIMIT 100`);
  res.render('pages/giftcode/index', {
    title: 'Giftcode', admin: req.session.admin,
    servers, codes, activePage: 'giftcode'
  });
});

router.get('/create', async(req,res) => {
  const servers = await getAll('SELECT id,name,slug FROM game_servers ORDER BY id');
  res.render('pages/giftcode/create', {
    title: 'Tạo Giftcode', admin: req.session.admin, servers, activePage: 'giftcode'
  });
});

router.post('/create', async(req,res) => {
  const { prefix, count, max_uses, server_id, description, expires_at, rewards_gold, rewards_items } = req.body;
  const rewards = [];
  if(rewards_gold && parseInt(rewards_gold)>0) rewards.push({ type:'gold', amount: parseInt(rewards_gold) });
  if(rewards_items) {
    rewards_items.split(',').map(s=>s.trim()).filter(Boolean).forEach(item => {
      const [id,qty] = item.split(':');
      if(id) rewards.push({ type:'item', itemId: parseInt(id), qty: parseInt(qty)||1 });
    });
  }
  const n = Math.min(parseInt(count)||1, 500);
  const codes = [];
  for(let i=0;i<n;i++){
    const code = prefix
      ? `${prefix.toUpperCase()}-${Math.random().toString(36).slice(2,8).toUpperCase()}`
      : Math.random().toString(36).slice(2,10).toUpperCase();
    codes.push(code);
    await query(
      'INSERT INTO giftcodes(code,description,rewards,max_uses,server_id,expires_at,created_by)VALUES($1,$2,$3,$4,$5,$6,$7)',
      [code, description||null, JSON.stringify(rewards), parseInt(max_uses)||1,
       server_id||null, expires_at||null, req.session.admin.id]
    ).catch(()=>{}); // ignore dup
  }
  await query('INSERT INTO admin_action_logs(admin_id,action,target_type,target_id,details,ip)VALUES($1,$2,$3,$4,$5,$6)',
    [req.session.admin.id,'create_giftcode','giftcode',codes[0],JSON.stringify({count:n,rewards}),req.ip]);
  res.redirect('/giftcode?success=Đã+tạo+'+n+'+giftcode');
});

router.post('/:id/toggle', async(req,res) => {
  await query('UPDATE giftcodes SET is_active=NOT is_active WHERE id=$1',[req.params.id]);
  res.json({ok:true});
});

router.delete('/:id', async(req,res) => {
  await query('DELETE FROM giftcodes WHERE id=$1',[req.params.id]);
  res.json({ok:true});
});

// Public redeem endpoint (called from game server or website)
router.post('/redeem', async(req,res) => {
  const { code, character_name, server_id } = req.body;
  try {
    const gc = await getOne(`
      SELECT * FROM giftcodes WHERE code=UPPER($1) AND is_active=TRUE
      AND (expires_at IS NULL OR expires_at > NOW())
      AND (server_id IS NULL OR server_id=$2)`,
      [code, server_id||0]);
    if(!gc) return res.json({ok:false, error:'Mã không hợp lệ hoặc đã hết hạn'});
    if(gc.uses >= gc.max_uses) return res.json({ok:false, error:'Mã đã hết lượt sử dụng'});
    const used = await getOne('SELECT id FROM giftcode_uses WHERE code_id=$1 AND character_name=$2',[gc.id,character_name]);
    if(used) return res.json({ok:false, error:'Nhân vật đã dùng mã này rồi'});
    await query('INSERT INTO giftcode_uses(code_id,character_name,server_id)VALUES($1,$2,$3)',[gc.id,character_name,server_id||null]);
    await query('UPDATE giftcodes SET uses=uses+1 WHERE id=$1',[gc.id]);
    res.json({ok:true, rewards: gc.rewards, message:'Nhập mã thành công!'});
  } catch(e){ res.json({ok:false, error:e.message}); }
});

module.exports = router;
