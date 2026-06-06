const router = require('express').Router();
const { getAll, getOne, query } = require('../services/db');

const TIERS = { basic:'Cơ bản', comfort:'Tiện nghi', luxury:'Sang trọng', estate:'Dinh thự' };
const FURN_CATS = { bed:'Giường', seating:'Ghế', table:'Bàn', storage:'Tủ/Rương', decor:'Trang trí', lighting:'Đèn' };

// ── Loại nhà ───────────────────────────────────────────────────
router.get('/', async (req,res)=>{
  const types = await getAll(`SELECT ht.*, a.file_path AS icon_url,
    (SELECT COUNT(*) FROM houses h WHERE h.type_code=ht.type_code) AS house_count
    FROM house_types ht LEFT JOIN assets a ON ht.icon_asset_id=a.id ORDER BY ht.sort_order,ht.id`);
  res.render('pages/housing/index',{title:'Nhà ở & Tài sản',admin:req.session.admin,types,TIERS,activePage:'housing'});
});

router.get('/new', async (req,res)=>{
  const icons = await getAll("SELECT id,name,file_path FROM assets WHERE category IN ('ui','map') ORDER BY name LIMIT 300");
  res.render('pages/housing/form',{title:'Thêm loại nhà',admin:req.session.admin,type:null,icons,TIERS,activePage:'housing'});
});

router.post('/', async (req,res)=>{
  const b=req.body;
  try {
    await query(`INSERT INTO house_types (type_code,name,name_vn,description,tier,purchase_price,storage_slots,max_furniture,icon_asset_id,sort_order)
      VALUES($1,$2,$3,$4,$5,$6,$7,$8,$9,$10)`,
      [b.type_code,b.name,b.name_vn||null,b.description||null,b.tier||'basic',
       parseInt(b.purchase_price)||50000,parseInt(b.storage_slots)||20,parseInt(b.max_furniture)||30,
       b.icon_asset_id||null,parseInt(b.sort_order)||0]);
    res.redirect('/housing?success=Đã+thêm');
  } catch(e){ res.redirect('/housing/new?error='+encodeURIComponent(e.message)); }
});

router.get('/:id/edit', async (req,res)=>{
  const type = await getOne('SELECT * FROM house_types WHERE id=$1',[req.params.id]);
  if(!type) return res.redirect('/housing');
  const icons = await getAll("SELECT id,name,file_path FROM assets WHERE category IN ('ui','map') ORDER BY name LIMIT 300");
  res.render('pages/housing/form',{title:'Sửa loại nhà',admin:req.session.admin,type,icons,TIERS,activePage:'housing'});
});

router.post('/:id', async (req,res)=>{
  const b=req.body;
  await query(`UPDATE house_types SET type_code=$1,name=$2,name_vn=$3,description=$4,tier=$5,
    purchase_price=$6,storage_slots=$7,max_furniture=$8,icon_asset_id=$9,sort_order=$10,is_enabled=$11 WHERE id=$12`,
    [b.type_code,b.name,b.name_vn||null,b.description||null,b.tier||'basic',parseInt(b.purchase_price)||50000,
     parseInt(b.storage_slots)||20,parseInt(b.max_furniture)||30,b.icon_asset_id||null,parseInt(b.sort_order)||0,
     b.is_enabled==='1'||b.is_enabled==='on',req.params.id]);
  res.redirect('/housing');
});

router.delete('/:id', async (req,res)=>{ await query('DELETE FROM house_types WHERE id=$1',[req.params.id]); res.json({ok:true}); });

// ── Nhà cụ thể (lô đất) ────────────────────────────────────────
router.get('/lots', async (req,res)=>{
  const houses = await getAll(`SELECT h.*, ht.name_vn AS type_name FROM houses h
    LEFT JOIN house_types ht ON h.type_code=ht.type_code ORDER BY h.zone_id,h.id`);
  const types = await getAll("SELECT type_code,name_vn FROM house_types WHERE is_enabled=TRUE");
  res.render('pages/housing/lots',{title:'Lô đất',admin:req.session.admin,houses,types,activePage:'housing'});
});

router.post('/lots', async (req,res)=>{
  const b=req.body;
  await query(`INSERT INTO houses (type_code,address,zone_id,pos_x,pos_y) VALUES($1,$2,$3,$4,$5)`,
    [b.type_code,b.address,parseInt(b.zone_id)||1,parseFloat(b.pos_x)||100,parseFloat(b.pos_y)||100]);
  res.redirect('/housing/lots?success=Đã+tạo+lô');
});

// ── Catalog nội thất ───────────────────────────────────────────
router.get('/furniture', async (req,res)=>{
  const items = await getAll(`SELECT fc.*, a.file_path AS asset_url FROM furniture_catalog fc
    LEFT JOIN assets a ON fc.asset_id=a.id ORDER BY fc.category,fc.id`);
  res.render('pages/housing/furniture',{title:'Nội thất',admin:req.session.admin,items,FURN_CATS,activePage:'housing'});
});

router.post('/furniture', async (req,res)=>{
  const b=req.body;
  await query(`INSERT INTO furniture_catalog (furniture_code,name,name_vn,category,price,asset_id)
    VALUES($1,$2,$3,$4,$5,$6) ON CONFLICT (furniture_code) DO UPDATE SET name=$2,name_vn=$3,category=$4,price=$5`,
    [b.furniture_code,b.name,b.name_vn||null,b.category||'decor',parseInt(b.price)||1000,b.asset_id||null]);
  res.redirect('/housing/furniture?success=Đã+lưu');
});

router.get('/export/json', async (req,res)=>{
  const types = await getAll("SELECT type_code,name,name_vn,tier,purchase_price,storage_slots,max_furniture FROM house_types WHERE is_enabled=TRUE");
  const houses = await getAll("SELECT id,type_code,address,zone_id,pos_x,pos_y,owner_char_id FROM houses");
  const furniture = await getAll("SELECT furniture_code,name,name_vn,category,price FROM furniture_catalog WHERE is_enabled=TRUE");
  res.json({ generated:new Date().toISOString(), types, houses, furniture });
});

module.exports = router;
