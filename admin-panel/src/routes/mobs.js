const router = require('express').Router();
const {getAll,getOne,query} = require('../services/db');

router.get('/', async(req,res)=>{
  const {type,search}=req.query;
  let sql=`SELECT m.*,a.file_path as sprite_url FROM mobs m LEFT JOIN assets a ON m.sprite_asset_id=a.id WHERE 1=1`;
  const p=[];
  if(type){sql+=` AND m.type=$${p.length+1}`;p.push(type);}
  if(search){sql+=` AND (m.name ILIKE $${p.length+1} OR m.name_vn ILIKE $${p.length+1})`;p.push('%'+search+'%');}
  sql+=' ORDER BY m.level_min,m.template_id LIMIT 300';
  const mobs=await getAll(sql,p);
  const stats=await getOne(`SELECT
    COUNT(*) FILTER (WHERE type='normal') as normal,
    COUNT(*) FILTER (WHERE type='elite') as elite,
    COUNT(*) FILTER (WHERE type='boss') as boss,
    COUNT(*) total FROM mobs`);
  res.render('pages/mobs/index',{title:'Quái vật',admin:req.session.admin,
    mobs,stats,type:type||'',search:search||'',activePage:'mobs'});
});

router.get('/create', async(req,res)=>{
  const sprites=await getAll("SELECT id,name,file_path FROM assets WHERE type IN ('sprite','animation') AND category LIKE '%mob%' ORDER BY name LIMIT 200");
  const itemList=await getAll('SELECT item_id,name,name_vn FROM items ORDER BY item_id LIMIT 200');
  res.render('pages/mobs/form',{title:'Tạo Mob',admin:req.session.admin,
    mob:null,sprites,itemList,activePage:'mobs'});
});

router.post('/create', async(req,res)=>{
  const {template_id,name,name_vn,type,zone_ids,level_min,level_max,hp,atk,def,
    speed,exp_reward,gold_min,gold_max,ai_type,respawn_secs,description,
    sprite_asset_id,server_id,drop_table}=req.body;
  try{
    const zones=zone_ids?zone_ids.split(',').map(z=>parseInt(z.trim())).filter(Boolean):[];
    let dropJson='[]';
    try{dropJson=JSON.parse(drop_table||'[]'); dropJson=JSON.stringify(dropJson);}catch(e){dropJson=drop_table||'[]';}
    await query(`INSERT INTO mobs(template_id,name,name_vn,type,zone_ids,level_min,level_max,
      hp,atk,def,speed,exp_reward,gold_reward_min,gold_reward_max,ai_type,respawn_secs,
      description,sprite_asset_id,server_id,drop_table)
      VALUES($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,$14,$15,$16,$17,$18,$19,$20)`,
      [template_id,name,name_vn||null,type||'normal',zones,
       parseInt(level_min)||1,parseInt(level_max)||5,
       parseInt(hp)||100,parseInt(atk)||10,parseInt(def)||5,
       parseFloat(speed)||3.0,parseInt(exp_reward)||50,
       parseInt(gold_min)||10,parseInt(gold_max)||50,
       ai_type||'passive',parseInt(respawn_secs)||300,
       description||null,sprite_asset_id||null,server_id||null,dropJson]);
    res.redirect('/mobs');
  }catch(e){res.redirect('/mobs/create?error='+encodeURIComponent(e.message));}
});

router.get('/:id/edit', async(req,res)=>{
  const mob=await getOne('SELECT * FROM mobs WHERE id=$1',[req.params.id]);
  if(!mob)return res.redirect('/mobs');
  const sprites=await getAll("SELECT id,name,file_path FROM assets WHERE type IN ('sprite','animation') ORDER BY name LIMIT 200");
  const itemList=await getAll('SELECT item_id,name,name_vn FROM items ORDER BY item_id LIMIT 200');
  res.render('pages/mobs/form',{title:'Sửa Mob: '+mob.name,admin:req.session.admin,
    mob,sprites,itemList,activePage:'mobs'});
});

router.post('/:id/edit', async(req,res)=>{
  const {name,name_vn,type,zone_ids,level_min,level_max,hp,atk,def,speed,
    exp_reward,gold_min,gold_max,ai_type,respawn_secs,description,
    sprite_asset_id,server_id,drop_table}=req.body;
  const zones=zone_ids?zone_ids.split(',').map(z=>parseInt(z.trim())).filter(Boolean):[];
  let dropJson='[]';
  try{dropJson=JSON.stringify(JSON.parse(drop_table||'[]'));}catch(e){dropJson=drop_table||'[]';}
  await query(`UPDATE mobs SET name=$1,name_vn=$2,type=$3,zone_ids=$4,level_min=$5,level_max=$6,
    hp=$7,atk=$8,def=$9,speed=$10,exp_reward=$11,gold_reward_min=$12,gold_reward_max=$13,
    ai_type=$14,respawn_secs=$15,description=$16,sprite_asset_id=$17,server_id=$18,
    drop_table=$19,updated_at=NOW() WHERE id=$20`,
    [name,name_vn||null,type||'normal',zones,
     parseInt(level_min)||1,parseInt(level_max)||5,
     parseInt(hp)||100,parseInt(atk)||10,parseInt(def)||5,
     parseFloat(speed)||3.0,parseInt(exp_reward)||50,
     parseInt(gold_min)||10,parseInt(gold_max)||50,
     ai_type||'passive',parseInt(respawn_secs)||300,
     description||null,sprite_asset_id||null,server_id||null,dropJson,req.params.id]);
  res.redirect('/mobs');
});

router.delete('/:id', async(req,res)=>{
  await query('DELETE FROM mobs WHERE id=$1',[req.params.id]);
  res.json({ok:true});
});

router.get('/export/json', async(req,res)=>{
  const mobs=await getAll('SELECT * FROM mobs ORDER BY template_id');
  res.json({mobs, count:mobs.length, generated:new Date().toISOString()});
});

module.exports = router;
