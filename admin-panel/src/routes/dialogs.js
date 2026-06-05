const router = require('express').Router();
const {getAll,getOne,query} = require('../services/db');

// All dialogs view
router.get('/', async(req,res)=>{
  const {npc_id,lang}=req.query;
  let sql=`SELECT d.*,n.name as npc_name,n.template_id,a.file_path as voice_url
    FROM npc_dialogs d JOIN npcs n ON d.npc_id=n.id LEFT JOIN assets a ON d.voice_asset_id=a.id WHERE 1=1`;
  const p=[];
  if(npc_id){sql+=` AND d.npc_id=$${p.length+1}`;p.push(npc_id);}
  if(lang){sql+=` AND d.lang=$${p.length+1}`;p.push(lang);}
  sql+=' ORDER BY n.name,d.node_key LIMIT 500';
  const dialogs=await getAll(sql,p);
  const npcList=await getAll('SELECT id,name,template_id FROM npcs ORDER BY name');
  const voices=await getAll("SELECT id,name FROM audio WHERE type='voice' ORDER BY name LIMIT 200");
  res.render('pages/dialogs/index',{title:'Lời thoại NPC',admin:req.session.admin,
    dialogs,npcList,voices,npc_id:npc_id||'',lang:lang||'',activePage:'dialogs'});
});

// NPC dialogs (from NPC detail page)
router.get('/npc/:npcId', async(req,res)=>{
  const npc=await getOne('SELECT * FROM npcs WHERE id=$1',[req.params.npcId]);
  const dialogs=await getAll('SELECT d.*,a.file_path as voice_url FROM npc_dialogs d LEFT JOIN assets a ON d.voice_asset_id=a.id WHERE d.npc_id=$1 ORDER BY d.lang,d.node_key',[req.params.npcId]);
  const voices=await getAll("SELECT id,name,file_path FROM audio WHERE type='voice' ORDER BY name LIMIT 300");
  res.render('pages/dialogs/npc',{title:`Lời thoại: ${npc?.name||''}`,admin:req.session.admin,
    npc,dialogs,voices,activePage:'dialogs'});
});

router.post('/npc/:npcId/save', async(req,res)=>{
  const {node_key,text,choices,lang,voice_asset_id,condition}=req.body;
  try{
    let choicesJson;
    try{choicesJson=typeof choices==='string'?choices:JSON.stringify(choices);}
    catch(e){choicesJson='[]';}
    await query(`INSERT INTO npc_dialogs(npc_id,node_key,text,choices,lang,voice_asset_id,condition)
      VALUES($1,$2,$3,$4,$5,$6,$7)
      ON CONFLICT(npc_id,node_key,lang) DO UPDATE SET
      text=EXCLUDED.text, choices=EXCLUDED.choices,
      voice_asset_id=EXCLUDED.voice_asset_id, condition=EXCLUDED.condition`,
      [req.params.npcId,node_key,text,choicesJson,lang||'vi',
       voice_asset_id||null,condition||null]);
    res.json({ok:true});
  }catch(e){res.json({ok:false,error:e.message});}
});

router.delete('/:id', async(req,res)=>{
  await query('DELETE FROM npc_dialogs WHERE id=$1',[req.params.id]);
  res.json({ok:true});
});

// Export for game server
router.get('/export/:lang', async(req,res)=>{
  const dialogs=await getAll(`SELECT d.*,n.template_id FROM npc_dialogs d
    JOIN npcs n ON d.npc_id=n.id WHERE d.lang=$1 ORDER BY n.template_id,d.node_key`,[req.params.lang]);
  // Group by template_id
  const byNpc={};
  dialogs.forEach(d=>{
    if(!byNpc[d.template_id]) byNpc[d.template_id]={};
    byNpc[d.template_id][d.node_key]={text:d.text,choices:d.choices,condition:d.condition,voice:d.voice_asset_id};
  });
  res.json({dialogs:byNpc, lang:req.params.lang, generated:new Date().toISOString()});
});

module.exports = router;
