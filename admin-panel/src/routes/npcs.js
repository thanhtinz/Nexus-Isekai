const router = require('express').Router();
const {getAll,getOne,query} = require('../services/db');

router.get('/', async(req,res)=>{
  const {type,zone_id,search}=req.query;
  const servers=await getAll('SELECT id,name FROM game_servers ORDER BY id');
  let sql=`SELECT n.*,a.file_path as sprite_url FROM npcs n LEFT JOIN assets a ON n.sprite_asset_id=a.id WHERE 1=1`;
  const p=[];
  if(type){sql+=` AND n.type=$${p.length+1}`;p.push(type);}
  if(zone_id){sql+=` AND n.zone_id=$${p.length+1}`;p.push(zone_id);}
  if(search){sql+=` AND (n.name ILIKE $${p.length+1} OR n.name_vn ILIKE $${p.length+1})`;p.push('%'+search+'%');}
  sql+=' ORDER BY n.template_id LIMIT 300';
  const npcs=await getAll(sql,p);
  res.render('pages/npcs/index',{title:'NPC Manager',admin:req.session.admin,
    npcs,servers,type:type||'',zone_id:zone_id||'',search:search||'',activePage:'npcs'});
});

router.get('/create', async(req,res)=>{
  const sprites=await getAll("SELECT id,name,file_path FROM assets WHERE (type='sprite' OR type='animation') AND category LIKE '%npc%' ORDER BY name LIMIT 200");
  res.render('pages/npcs/form',{title:'Tạo NPC',admin:req.session.admin,
    npc:null,sprites,activePage:'npcs'});
});

router.post('/create', async(req,res)=>{
  const {template_id,name,name_vn,type,faction_id,zone_id,default_x,default_y,
    has_shop,has_dialog,description,sprite_asset_id,server_id}=req.body;
  try{
    const r=await query(`INSERT INTO npcs(template_id,name,name_vn,type,faction_id,zone_id,
      default_x,default_y,has_shop,has_dialog,description,sprite_asset_id,server_id)
      VALUES($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13) RETURNING id`,
      [template_id,name,name_vn||null,type||'merchant',parseInt(faction_id)||0,
       parseInt(zone_id)||1,parseFloat(default_x)||100,parseFloat(default_y)||100,
       !!has_shop,!!has_dialog,description||null,sprite_asset_id||null,server_id||null]);
    res.redirect('/npcs/'+r.rows[0].id+'/dialogs');
  }catch(e){res.redirect('/npcs/create?error='+encodeURIComponent(e.message));}
});

router.get('/:id', async(req,res)=>{
  const npc=await getOne(`SELECT n.*,a.file_path as sprite_url FROM npcs n LEFT JOIN assets a ON n.sprite_asset_id=a.id WHERE n.id=$1`,[req.params.id]);
  if(!npc)return res.redirect('/npcs');
  const dialogs=await getAll('SELECT d.*,a.file_path as voice_url FROM npc_dialogs d LEFT JOIN assets a ON d.voice_asset_id=a.id WHERE d.npc_id=$1 ORDER BY d.lang,d.node_key',[npc.id]);
  const sprites=await getAll("SELECT id,name,file_path FROM assets WHERE type IN ('sprite','animation') ORDER BY name LIMIT 200");
  const voices=await getAll("SELECT id,name,file_path FROM audio WHERE type='voice' ORDER BY name LIMIT 200");
  res.render('pages/npcs/detail',{title:'NPC: '+npc.name,admin:req.session.admin,
    npc,dialogs,sprites,voices,activePage:'npcs'});
});

router.post('/:id/edit', async(req,res)=>{
  const {name,name_vn,type,faction_id,zone_id,default_x,default_y,
    has_shop,has_dialog,description,sprite_asset_id,shop_items,schedule}=req.body;
  let shopJson='[]',schedJson='[]';
  try{shopJson=JSON.parse(shop_items||'[]'); shopJson=JSON.stringify(shopJson);}catch(e){shopJson=shop_items||'[]';}
  try{schedJson=JSON.parse(schedule||'[]'); schedJson=JSON.stringify(schedJson);}catch(e){schedJson=schedule||'[]';}
  await query(`UPDATE npcs SET name=$1,name_vn=$2,type=$3,faction_id=$4,zone_id=$5,default_x=$6,
    default_y=$7,has_shop=$8,has_dialog=$9,description=$10,sprite_asset_id=$11,
    shop_items=$12,schedule=$13,updated_at=NOW() WHERE id=$14`,
    [name,name_vn||null,type,parseInt(faction_id)||0,parseInt(zone_id)||1,
     parseFloat(default_x)||100,parseFloat(default_y)||100,!!has_shop,!!has_dialog,
     description||null,sprite_asset_id||null,shopJson,schedJson,req.params.id]);
  res.json({ok:true});
});

router.delete('/:id', async(req,res)=>{
  await query('DELETE FROM npcs WHERE id=$1',[req.params.id]);
  res.json({ok:true});
});

router.get('/export/json', async(req,res)=>{
  const npcs=await getAll(`SELECT n.*,json_agg(d.*) as dialogs FROM npcs n
    LEFT JOIN npc_dialogs d ON d.npc_id=n.id GROUP BY n.id ORDER BY n.template_id`);
  res.json({npcs, count:npcs.length, generated:new Date().toISOString()});
});

module.exports = router;
