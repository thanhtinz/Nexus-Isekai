const router = require('express').Router();
const { query } = require('../db');

router.get('/', (req, res) => {
  const msg = req.query.msg||'', err = req.query.err||'';
  res.render('pages/giftcode', { title: 'Nhập Giftcode', msg, err });
});

router.post('/redeem', async (req, res) => {
  const { code, character_id, server_id } = req.body;
  if (!code || !character_id) return res.redirect('/giftcode?err=Vui lòng điền đầy đủ thông tin');
  try {
    const { rows } = await query('SELECT * FROM giftcodes WHERE code=$1', [code.toUpperCase()]);
    const gc = rows[0];
    if (!gc) return res.redirect('/giftcode?err=Mã không tồn tại');
    if (!gc.is_active) return res.redirect('/giftcode?err=Mã đã bị vô hiệu hóa');
    if (gc.expires_at && new Date(gc.expires_at)<new Date()) return res.redirect('/giftcode?err=Mã đã hết hạn');
    const used = parseInt((await query('SELECT COUNT(*) FROM giftcode_uses WHERE code_id=$1',[gc.id])).rows[0].count);
    if (gc.max_uses>0 && used>=gc.max_uses) return res.redirect('/giftcode?err=Mã đã hết lượt sử dụng');
    const dup = (await query('SELECT 1 FROM giftcode_uses WHERE code_id=$1 AND character_id=$2',[gc.id,character_id])).rows.length;
    if (dup) return res.redirect('/giftcode?err=Bạn đã nhận mã này rồi');
    await query('INSERT INTO giftcode_uses(code_id,character_id,server_id,used_at) VALUES($1,$2,$3,NOW())',[gc.id,character_id,server_id||null]);
    const rewards = JSON.parse(gc.rewards_json||'{}');
    const msg = `Nhận thành công! ${rewards.gold?'💰 '+rewards.gold.toLocaleString()+'G':''}`;
    res.redirect('/giftcode?msg=' + encodeURIComponent(msg));
  } catch(e) { res.redirect('/giftcode?err=' + encodeURIComponent(e.message)); }
});
module.exports = router;
