const router = require('express').Router();
const { getAll, getOne, query } = require('../services/db');

const CATEGORIES = {
  main:    'Chính',
  sub:     'Phụ',
  special: 'Đặc biệt',
  unique:  'Riêng'
};
const EFFECT_TYPES = {
  damage:     'Sát thương đơn',
  aoe_damage: 'Sát thương vùng',
  heal:       'Hồi máu',
  buff_atk:   'Tăng công',
  buff_def:   'Tăng thủ',
  drain:      'Hút máu'
};

// ── Danh sách (nhóm theo phân loại) ────────────────────────────
router.get('/', async (req, res) => {
  const skills = await getAll(`
    SELECT s.*, a.file_path AS icon_url
    FROM skills s LEFT JOIN assets a ON s.icon_asset_id = a.id
    ORDER BY s.category, s.faction_id, s.sort_order, s.id`);
  const professions = await getAll("SELECT code, name_vn, name FROM professions ORDER BY name_vn");
  res.render('pages/skills/index', {
    title: 'Quản lý Kỹ năng', admin: req.session.admin,
    skills, professions, CATEGORIES, EFFECT_TYPES, activePage: 'skills'
  });
});

router.get('/new', async (req, res) => {
  const professions = await getAll("SELECT code, name_vn, name FROM professions ORDER BY name_vn");
  const icons = await getAll("SELECT id, name, file_path FROM assets WHERE category IN ('character','ui') ORDER BY name LIMIT 500");
  res.render('pages/skills/form', {
    title: 'Thêm kỹ năng', admin: req.session.admin,
    skill: null, professions, icons, CATEGORIES, EFFECT_TYPES, activePage: 'skills'
  });
});

router.post('/', async (req, res) => {
  const b = req.body;
  try {
    await query(`INSERT INTO skills
      (skill_code,name,name_vn,description,category,class_code,faction_id,effect_type,power,
       level_req,mana_cost,cooldown_ms,range_px,buff_duration_ms,icon_asset_id,anim_code,sort_order)
      VALUES($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,$14,$15,$16,$17)`,
      [b.skill_code, b.name, b.name_vn||null, b.description||null,
       b.category||'main', b.class_code||null, parseInt(b.faction_id)||0,
       b.effect_type||'damage', parseFloat(b.power)||1.0,
       parseInt(b.level_req)||1, parseInt(b.mana_cost)||10, parseInt(b.cooldown_ms)||3000,
       parseInt(b.range_px)||48, parseInt(b.buff_duration_ms)||10000,
       b.icon_asset_id||null, b.anim_code||null, parseInt(b.sort_order)||0]);
    res.redirect('/skills?success=Đã+thêm+kỹ+năng');
  } catch (e) { res.redirect('/skills/new?error=' + encodeURIComponent(e.message)); }
});

router.get('/:id/edit', async (req, res) => {
  const skill = await getOne('SELECT * FROM skills WHERE id=$1', [req.params.id]);
  if (!skill) return res.redirect('/skills');
  const professions = await getAll("SELECT code, name_vn, name FROM professions ORDER BY name_vn");
  const icons = await getAll("SELECT id, name, file_path FROM assets WHERE category IN ('character','ui') ORDER BY name LIMIT 500");
  res.render('pages/skills/form', {
    title: 'Sửa kỹ năng', admin: req.session.admin,
    skill, professions, icons, CATEGORIES, EFFECT_TYPES, activePage: 'skills'
  });
});

router.post('/:id', async (req, res) => {
  const b = req.body;
  await query(`UPDATE skills SET
    skill_code=$1,name=$2,name_vn=$3,description=$4,category=$5,class_code=$6,faction_id=$7,
    effect_type=$8,power=$9,level_req=$10,mana_cost=$11,cooldown_ms=$12,range_px=$13,
    buff_duration_ms=$14,icon_asset_id=$15,anim_code=$16,sort_order=$17,is_enabled=$18
    WHERE id=$19`,
    [b.skill_code, b.name, b.name_vn||null, b.description||null, b.category||'main',
     b.class_code||null, parseInt(b.faction_id)||0, b.effect_type||'damage', parseFloat(b.power)||1.0,
     parseInt(b.level_req)||1, parseInt(b.mana_cost)||10, parseInt(b.cooldown_ms)||3000,
     parseInt(b.range_px)||48, parseInt(b.buff_duration_ms)||10000,
     b.icon_asset_id||null, b.anim_code||null, parseInt(b.sort_order)||0,
     b.is_enabled==='1'||b.is_enabled==='on', req.params.id]);
  res.redirect('/skills');
});

router.delete('/:id', async (req, res) => {
  await query('DELETE FROM skills WHERE id=$1', [req.params.id]);
  res.json({ ok: true });
});

router.post('/:id/toggle', async (req, res) => {
  await query('UPDATE skills SET is_enabled=NOT is_enabled WHERE id=$1', [req.params.id]);
  res.json({ ok: true });
});

// ── Export JSON cho game server đọc ────────────────────────────
router.get('/export/json', async (req, res) => {
  const skills = await getAll(`
    SELECT skill_code,name,name_vn,description,category,class_code,faction_id,
           effect_type,power,level_req,mana_cost,cooldown_ms,range_px,buff_duration_ms,anim_code
    FROM skills WHERE is_enabled=TRUE ORDER BY category,faction_id,sort_order,id`);
  res.json({ generated: new Date().toISOString(), skills });
});

module.exports = router;
