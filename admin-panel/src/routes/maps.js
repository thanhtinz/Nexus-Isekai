const router = require('express').Router();
const {getAll,getOne,query} = require('../services/db');

router.get('/', async(req,res)=>{
  const maps=await getAll(`SELECT m.*,
    t.file_path as tileset_url, t.name as tileset_name,
    bg.name as bg_music_name, th.file_path as thumb_url
    FROM maps m
    LEFT JOIN assets t  ON m.tileset_asset_id=t.id
    LEFT JOIN audio bg  ON m.bg_music_id=bg.id
    LEFT JOIN assets th ON m.thumbnail_asset_id=th.id
    ORDER BY m.zone_id`);
  res.render('pages/maps/index',{title:'Bản đồ',admin:req.session.admin,
    maps,activePage:'maps'});
});

router.get('/create', async(req,res)=>{
  const tilesets=await getAll("SELECT id,name,file_path FROM assets WHERE type='tile' ORDER BY name LIMIT 100");
  const musics=await getAll("SELECT id,name FROM audio WHERE type='bgm' ORDER BY name LIMIT 100");
  const ambients=await getAll("SELECT id,name FROM audio WHERE type='ambient' ORDER BY name LIMIT 100");
  const thumbs=await getAll("SELECT id,name,file_path FROM assets WHERE category='map_thumb' ORDER BY name LIMIT 100");
  const npcList=await getAll('SELECT template_id,name FROM npcs ORDER BY name LIMIT 200');
  const mobList=await getAll('SELECT template_id,name FROM mobs ORDER BY level_min LIMIT 200');
  res.render('pages/maps/form',{title:'Tạo bản đồ',admin:req.session.admin,
    map:null,tilesets,musics,ambients,thumbs,npcList,mobList,activePage:'maps'});
});

router.post('/create', async(req,res)=>{
  const {zone_id,name,name_vn,type,width,height,tileset_asset_id,
    bg_music_id,ambient_sound_id,thumbnail_asset_id,server_id,properties,
    spawn_points,portals,npc_spawns,mob_spawns}=req.body;
  const parseJ=(v)=>{try{return JSON.stringify(typeof v==='string'?JSON.parse(v):v);}catch(e){return v||'[]';}};
  try{
    await query(`INSERT INTO maps(zone_id,name,name_vn,type,width,height,
      tileset_asset_id,bg_music_id,ambient_sound_id,thumbnail_asset_id,
      server_id,properties,spawn_points,portals,npc_spawns,mob_spawns)
      VALUES($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,$14,$15,$16)`,
      [zone_id,name,name_vn||null,type||'overworld',
       parseInt(width)||300,parseInt(height)||300,
       tileset_asset_id||null,bg_music_id||null,ambient_sound_id||null,
       thumbnail_asset_id||null,server_id||null,
       parseJ(properties),parseJ(spawn_points),
       parseJ(portals),parseJ(npc_spawns),parseJ(mob_spawns)]);
    res.redirect('/maps');
  }catch(e){res.redirect('/maps/create?error='+encodeURIComponent(e.message));}
});

router.get('/:id', async(req,res)=>{
  const map=await getOne(`SELECT m.*,t.file_path as tileset_url,t.name as tileset_name,
    bg.name as bg_music_name,am.name as ambient_name, th.file_path as thumb_url
    FROM maps m LEFT JOIN assets t ON m.tileset_asset_id=t.id
    LEFT JOIN audio bg ON m.bg_music_id=bg.id LEFT JOIN audio am ON m.ambient_sound_id=am.id
    LEFT JOIN assets th ON m.thumbnail_asset_id=th.id WHERE m.id=$1`,[req.params.id]);
  if(!map)return res.redirect('/maps');
  const tilesets=await getAll("SELECT id,name FROM assets WHERE type='tile' ORDER BY name");
  const musics=await getAll("SELECT id,name FROM audio WHERE type='bgm' ORDER BY name");
  const ambients=await getAll("SELECT id,name FROM audio WHERE type='ambient' ORDER BY name");
  const npcList=await getAll('SELECT template_id,name FROM npcs ORDER BY name LIMIT 200');
  const mobList=await getAll('SELECT template_id,name FROM mobs ORDER BY level_min LIMIT 200');
  res.render('pages/maps/detail',{title:'Bản đồ: '+map.name,admin:req.session.admin,
    map,tilesets,musics,ambients,npcList,mobList,activePage:'maps'});
});

router.post('/:id/save', async(req,res)=>{
  const {name,name_vn,type,width,height,tileset_asset_id,bg_music_id,
    ambient_sound_id,properties,spawn_points,portals,npc_spawns,mob_spawns}=req.body;
  const parseJ=(v)=>{try{return JSON.stringify(typeof v==='string'?JSON.parse(v):v);}catch(e){return v||'[]';}};
  await query(`UPDATE maps SET name=$1,name_vn=$2,type=$3,width=$4,height=$5,
    tileset_asset_id=$6,bg_music_id=$7,ambient_sound_id=$8,
    properties=$9,spawn_points=$10,portals=$11,npc_spawns=$12,mob_spawns=$13,
    updated_at=NOW() WHERE id=$14`,
    [name,name_vn||null,type,parseInt(width)||300,parseInt(height)||300,
     tileset_asset_id||null,bg_music_id||null,ambient_sound_id||null,
     parseJ(properties),parseJ(spawn_points),parseJ(portals),
     parseJ(npc_spawns),parseJ(mob_spawns),req.params.id]);
  res.json({ok:true});
});

router.delete('/:id', async(req,res)=>{
  await query('DELETE FROM maps WHERE id=$1',[req.params.id]);
  res.json({ok:true});
});

router.get('/export/json', async(req,res)=>{
  const maps=await getAll('SELECT * FROM maps ORDER BY zone_id');
  res.json({maps, count:maps.length, generated:new Date().toISOString()});
});

module.exports = router;
