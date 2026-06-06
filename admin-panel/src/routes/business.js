const router = require('express').Router();
const { getAll, getOne, query } = require('../services/db');

const CATEGORIES = {
  food:'Ẩm thực', service:'Dịch vụ', medical:'Y tế', security:'An ninh',
  craft:'Chế tác', trade:'Thương mại', finance:'Tài chính', entertainment:'Giải trí'
};

// ── Loại ngành nghề ────────────────────────────────────────────
router.get('/', async (req, res) => {
  const types = await getAll(`SELECT bt.*, a.file_path AS icon_url,
    (SELECT COUNT(*) FROM businesses b WHERE b.type_code=bt.type_code) AS biz_count
    FROM business_types bt LEFT JOIN assets a ON bt.icon_asset_id=a.id
    ORDER BY bt.category, bt.sort_order, bt.id`);
  res.render('pages/business/index', {
    title:'Ngành nghề & Cơ sở', admin:req.session.admin, types, CATEGORIES, activePage:'business'
  });
});

router.get('/new', async (req, res) => {
  const icons = await getAll("SELECT id,name,file_path FROM assets WHERE category IN ('ui','character') ORDER BY name LIMIT 300");
  res.render('pages/business/form', { title:'Thêm ngành', admin:req.session.admin, type:null, icons, CATEGORIES, activePage:'business' });
});

router.post('/', async (req, res) => {
  const b = req.body;
  try {
    await query(`INSERT INTO business_types
      (type_code,name,name_vn,description,category,job_action,base_pay,purchase_price,daily_income,max_employees,icon_asset_id,sort_order)
      VALUES($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12)`,
      [b.type_code,b.name,b.name_vn||null,b.description||null,b.category||'service',
       b.job_action||null,parseInt(b.base_pay)||50,parseInt(b.purchase_price)||100000,
       parseInt(b.daily_income)||5000,parseInt(b.max_employees)||5,b.icon_asset_id||null,parseInt(b.sort_order)||0]);
    res.redirect('/business?success=Đã+thêm+ngành');
  } catch(e){ res.redirect('/business/new?error='+encodeURIComponent(e.message)); }
});

router.get('/:id/edit', async (req,res)=>{
  const type = await getOne('SELECT * FROM business_types WHERE id=$1',[req.params.id]);
  if(!type) return res.redirect('/business');
  const icons = await getAll("SELECT id,name,file_path FROM assets WHERE category IN ('ui','character') ORDER BY name LIMIT 300");
  res.render('pages/business/form',{title:'Sửa ngành',admin:req.session.admin,type,icons,CATEGORIES,activePage:'business'});
});

router.post('/:id', async (req,res)=>{
  const b=req.body;
  await query(`UPDATE business_types SET type_code=$1,name=$2,name_vn=$3,description=$4,category=$5,
    job_action=$6,base_pay=$7,purchase_price=$8,daily_income=$9,max_employees=$10,icon_asset_id=$11,
    sort_order=$12,is_enabled=$13 WHERE id=$14`,
    [b.type_code,b.name,b.name_vn||null,b.description||null,b.category||'service',b.job_action||null,
     parseInt(b.base_pay)||50,parseInt(b.purchase_price)||100000,parseInt(b.daily_income)||5000,
     parseInt(b.max_employees)||5,b.icon_asset_id||null,parseInt(b.sort_order)||0,
     b.is_enabled==='1'||b.is_enabled==='on',req.params.id]);
  res.redirect('/business');
});

router.delete('/:id', async (req,res)=>{ await query('DELETE FROM business_types WHERE id=$1',[req.params.id]); res.json({ok:true}); });
router.post('/:id/toggle', async (req,res)=>{ await query('UPDATE business_types SET is_enabled=NOT is_enabled WHERE id=$1',[req.params.id]); res.json({ok:true}); });

// ── Cơ sở cụ thể (đặt trên bản đồ) ─────────────────────────────
router.get('/places', async (req,res)=>{
  const places = await getAll(`SELECT b.*, bt.name_vn AS type_name FROM businesses b
    LEFT JOIN business_types bt ON b.type_code=bt.type_code ORDER BY b.zone_id, b.id`);
  const types = await getAll("SELECT type_code,name_vn FROM business_types WHERE is_enabled=TRUE");
  res.render('pages/business/places',{title:'Cơ sở',admin:req.session.admin,places,types,activePage:'business'});
});

router.post('/places', async (req,res)=>{
  const b=req.body;
  await query(`INSERT INTO businesses (type_code,name,zone_id,pos_x,pos_y) VALUES($1,$2,$3,$4,$5)`,
    [b.type_code,b.name,parseInt(b.zone_id)||1,parseFloat(b.pos_x)||100,parseFloat(b.pos_y)||100]);
  res.redirect('/business/places?success=Đã+đặt+cơ+sở');
});

router.get('/export/json', async (req,res)=>{
  const types = await getAll(`SELECT type_code,name,name_vn,category,job_action,base_pay,
    purchase_price,daily_income,max_employees FROM business_types WHERE is_enabled=TRUE ORDER BY sort_order`);
  const places = await getAll(`SELECT id,type_code,name,zone_id,pos_x,pos_y,owner_char_id FROM businesses`);
  res.json({ generated:new Date().toISOString(), types, places });
});

module.exports = router;
