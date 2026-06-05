const router = require('express').Router();
const {getAll,getOne,query} = require('../services/db');

router.get('/', async(req,res)=>{
  const profs=await getAll(`SELECT p.*,a.file_path as icon_url FROM professions p
    LEFT JOIN assets a ON p.icon_asset_id=a.id ORDER BY p.category,p.code`);
  res.render('pages/professions/index',{title:'Nghề nghiệp',admin:req.session.admin,
    profs,activePage:'professions'});
});

router.get('/create', async(req,res)=>{
  const icons=await getAll("SELECT id,name,file_path FROM assets WHERE type='sprite' AND category='ui' ORDER BY name LIMIT 100");
  res.render('pages/professions/form',{title:'Tạo nghề',admin:req.session.admin,
    prof:null,icons,activePage:'professions'});
});

router.post('/create', async(req,res)=>{
  const {code,name,name_vn,category,max_level,is_special,faction_required,
    description,icon_asset_id,skills,unlock_condition}=req.body;
  let skillsJson='[]';
  try{skillsJson=JSON.stringify(JSON.parse(skills||'[]'));}catch(e){skillsJson=skills||'[]';}
  try{
    await query(`INSERT INTO professions(code,name,name_vn,category,max_level,is_special,
      faction_required,description,icon_asset_id,skills,unlock_condition)
      VALUES($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11)`,
      [code.toUpperCase(),name,name_vn||null,category||'civilian',
       parseInt(max_level)||10,!!is_special,parseInt(faction_required)||0,
       description||null,icon_asset_id||null,skillsJson,unlock_condition||null]);
    res.redirect('/professions');
  }catch(e){res.redirect('/professions/create?error='+encodeURIComponent(e.message));}
});

router.get('/:id/edit', async(req,res)=>{
  const prof=await getOne('SELECT * FROM professions WHERE id=$1',[req.params.id]);
  if(!prof)return res.redirect('/professions');
  const icons=await getAll("SELECT id,name,file_path FROM assets WHERE type='sprite' ORDER BY name LIMIT 100");
  res.render('pages/professions/form',{title:'Sửa nghề: '+prof.name,admin:req.session.admin,
    prof,icons,activePage:'professions'});
});

router.post('/:id/edit', async(req,res)=>{
  const {name,name_vn,category,max_level,is_special,faction_required,
    description,icon_asset_id,skills,unlock_condition}=req.body;
  let skillsJson='[]';
  try{skillsJson=JSON.stringify(JSON.parse(skills||'[]'));}catch(e){skillsJson=skills||'[]';}
  await query(`UPDATE professions SET name=$1,name_vn=$2,category=$3,max_level=$4,is_special=$5,
    faction_required=$6,description=$7,icon_asset_id=$8,skills=$9,unlock_condition=$10,
    updated_at=NOW() WHERE id=$11`,
    [name,name_vn||null,category||'civilian',parseInt(max_level)||10,!!is_special,
     parseInt(faction_required)||0,description||null,icon_asset_id||null,
     skillsJson,unlock_condition||null,req.params.id]);
  res.redirect('/professions');
});

router.delete('/:id', async(req,res)=>{
  await query('DELETE FROM professions WHERE id=$1',[req.params.id]);
  res.json({ok:true});
});

router.get('/export/json', async(req,res)=>{
  const profs=await getAll('SELECT * FROM professions ORDER BY code');
  res.json({professions:profs, count:profs.length, generated:new Date().toISOString()});
});

module.exports = router;
