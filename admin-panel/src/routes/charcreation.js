const router = require('express').Router();
const { getAll, getOne, query } = require('../services/db');

const SLOTS = ['skin', 'eyes', 'hair', 'outfit'];
const SLOT_NAMES = { skin: 'Da / Body', eyes: 'Mắt', hair: 'Tóc', outfit: 'Áo quần cơ bản' };

// ── Trang chính: races + options ───────────────────────────────
router.get('/', async (req, res) => {
  const races = await getAll(`
    SELECT r.*, a.file_path AS base_url, p.file_path AS preview_url
    FROM char_races r
    LEFT JOIN assets a ON r.base_asset_id = a.id
    LEFT JOIN assets p ON r.preview_asset_id = p.id
    ORDER BY r.sort_order, r.id`);

  const optionsBySlot = {};
  for (const slot of SLOTS) {
    optionsBySlot[slot] = await getAll(`
      SELECT o.*, a.file_path AS asset_url
      FROM char_options o
      LEFT JOIN assets a ON o.asset_id = a.id
      WHERE o.slot = $1
      ORDER BY o.sort_order, o.id`, [slot]);
  }

  res.render('pages/charcreation/index', {
    title: 'Tạo nhân vật — Cấu hình',
    admin: req.session.admin,
    races, optionsBySlot, SLOTS, SLOT_NAMES,
    activePage: 'charcreation'
  });
});

// ── RACES ──────────────────────────────────────────────────────
router.get('/race/new', async (req, res) => {
  const bases = await getAll("SELECT id,name,file_path FROM assets WHERE category='character' ORDER BY name LIMIT 500");
  res.render('pages/charcreation/race-form', {
    title: 'Thêm chủng tộc', admin: req.session.admin,
    race: null, bases, activePage: 'charcreation'
  });
});

router.post('/race', async (req, res) => {
  const { code, race, race_name_vn, gender, faction_id, base_asset_id, preview_asset_id, description, sort_order } = req.body;
  try {
    await query(`INSERT INTO char_races(code,race,race_name_vn,gender,faction_id,base_asset_id,preview_asset_id,description,sort_order)
      VALUES($1,$2,$3,$4,$5,$6,$7,$8,$9)`,
      [code, race, race_name_vn || null, gender, parseInt(faction_id) || 0,
       base_asset_id || null, preview_asset_id || null, description || null, parseInt(sort_order) || 0]);
    res.redirect('/charcreation?success=Đã+thêm+chủng+tộc');
  } catch (e) { res.redirect('/charcreation/race/new?error=' + encodeURIComponent(e.message)); }
});

router.get('/race/:id/edit', async (req, res) => {
  const race = await getOne('SELECT * FROM char_races WHERE id=$1', [req.params.id]);
  if (!race) return res.redirect('/charcreation');
  const bases = await getAll("SELECT id,name,file_path FROM assets WHERE category='character' ORDER BY name LIMIT 500");
  res.render('pages/charcreation/race-form', {
    title: 'Sửa chủng tộc', admin: req.session.admin,
    race, bases, activePage: 'charcreation'
  });
});

router.post('/race/:id', async (req, res) => {
  const { code, race, race_name_vn, gender, faction_id, base_asset_id, preview_asset_id, description, sort_order, is_enabled } = req.body;
  await query(`UPDATE char_races SET code=$1,race=$2,race_name_vn=$3,gender=$4,faction_id=$5,
    base_asset_id=$6,preview_asset_id=$7,description=$8,sort_order=$9,is_enabled=$10 WHERE id=$11`,
    [code, race, race_name_vn || null, gender, parseInt(faction_id) || 0,
     base_asset_id || null, preview_asset_id || null, description || null,
     parseInt(sort_order) || 0, is_enabled === '1' || is_enabled === 'on', req.params.id]);
  res.redirect('/charcreation');
});

router.delete('/race/:id', async (req, res) => {
  await query('DELETE FROM char_races WHERE id=$1', [req.params.id]);
  res.json({ ok: true });
});

router.post('/race/:id/toggle', async (req, res) => {
  await query('UPDATE char_races SET is_enabled=NOT is_enabled WHERE id=$1', [req.params.id]);
  res.json({ ok: true });
});

// ── OPTIONS (skin/eyes/hair/outfit) ────────────────────────────
router.get('/option/new', async (req, res) => {
  const slot = req.query.slot || 'skin';
  const assets = await getAll("SELECT id,name,file_path FROM assets WHERE category='character' ORDER BY name LIMIT 500");
  res.render('pages/charcreation/option-form', {
    title: 'Thêm lựa chọn', admin: req.session.admin,
    option: null, slot, assets, activePage: 'charcreation'
  });
});

router.post('/option', async (req, res) => {
  const { slot, code, name_vn, asset_id, color_index, hex_preview, race_filter, gender_filter, is_default, sort_order } = req.body;
  try {
    await query(`INSERT INTO char_options(slot,code,name_vn,asset_id,color_index,hex_preview,race_filter,gender_filter,is_default,sort_order)
      VALUES($1,$2,$3,$4,$5,$6,$7,$8,$9,$10)`,
      [slot, code, name_vn || null, asset_id || null,
       color_index !== '' ? parseInt(color_index) : null, hex_preview || null,
       race_filter || null, gender_filter || null,
       is_default === '1' || is_default === 'on', parseInt(sort_order) || 0]);
    res.redirect('/charcreation?success=Đã+thêm+lựa+chọn');
  } catch (e) { res.redirect('/charcreation/option/new?slot=' + slot + '&error=' + encodeURIComponent(e.message)); }
});

router.get('/option/:id/edit', async (req, res) => {
  const option = await getOne('SELECT * FROM char_options WHERE id=$1', [req.params.id]);
  if (!option) return res.redirect('/charcreation');
  const assets = await getAll("SELECT id,name,file_path FROM assets WHERE category='character' ORDER BY name LIMIT 500");
  res.render('pages/charcreation/option-form', {
    title: 'Sửa lựa chọn', admin: req.session.admin,
    option, slot: option.slot, assets, activePage: 'charcreation'
  });
});

router.post('/option/:id', async (req, res) => {
  const { slot, code, name_vn, asset_id, color_index, hex_preview, race_filter, gender_filter, is_default, is_enabled, sort_order } = req.body;
  await query(`UPDATE char_options SET slot=$1,code=$2,name_vn=$3,asset_id=$4,color_index=$5,
    hex_preview=$6,race_filter=$7,gender_filter=$8,is_default=$9,is_enabled=$10,sort_order=$11 WHERE id=$12`,
    [slot, code, name_vn || null, asset_id || null,
     color_index !== '' ? parseInt(color_index) : null, hex_preview || null,
     race_filter || null, gender_filter || null,
     is_default === '1' || is_default === 'on',
     is_enabled === '1' || is_enabled === 'on', parseInt(sort_order) || 0, req.params.id]);
  res.redirect('/charcreation');
});

router.delete('/option/:id', async (req, res) => {
  await query('DELETE FROM char_options WHERE id=$1', [req.params.id]);
  res.json({ ok: true });
});

router.post('/option/:id/toggle', async (req, res) => {
  await query('UPDATE char_options SET is_enabled=NOT is_enabled WHERE id=$1', [req.params.id]);
  res.json({ ok: true });
});

// ── EXPORT JSON cho client đọc lúc tạo nhân vật ────────────────
router.get('/export/json', async (req, res) => {
  const races = await getAll(`
    SELECT r.code,r.race,r.race_name_vn,r.gender,r.faction_id,r.description,
           a.file_path AS base_sprite, p.file_path AS preview
    FROM char_races r
    LEFT JOIN assets a ON r.base_asset_id=a.id
    LEFT JOIN assets p ON r.preview_asset_id=p.id
    WHERE r.is_enabled=TRUE ORDER BY r.sort_order,r.id`);

  const options = {};
  for (const slot of SLOTS) {
    options[slot] = await getAll(`
      SELECT o.code,o.name_vn,o.color_index,o.hex_preview,o.race_filter,o.gender_filter,o.is_default,
             a.file_path AS sprite
      FROM char_options o LEFT JOIN assets a ON o.asset_id=a.id
      WHERE o.slot=$1 AND o.is_enabled=TRUE ORDER BY o.sort_order,o.id`, [slot]);
  }

  res.json({
    generated: new Date().toISOString(),
    races,
    options,
    layer_order: ['0bas', '1out', '2clo', '3fac', '4har', '5hat'],
    note: 'Client đọc file này để dựng màn tạo nhân vật. Ghép sprite theo layer_order.'
  });
});

module.exports = router;
