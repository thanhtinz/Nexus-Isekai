const router = require('express').Router();
const {getAll,getOne,query} = require('../services/db');

router.get('/', async(req,res)=>{
  const {type,rarity,search,server_id}=req.query;
  const servers = await getAll('SELECT id,name FROM game_servers ORDER BY id');
  let sql=`SELECT i.*,a.file_path as icon_url FROM items i LEFT JOIN assets a ON i.icon_asset_id=a.id WHERE 1=1`;
  const p=[];
  if(type){sql+=` AND i.type=$${p.length+1}`;p.push(type);}
  if(rarity!==undefined&&rarity!==''){sql+=` AND i.rarity=$${p.length+1}`;p.push(parseInt(rarity));}
  if(search){sql+=` AND (i.name ILIKE $${p.length+1} OR i.name_vn ILIKE $${p.length+1})`;p.push('%'+search+'%');}
  if(server_id){sql+=` AND (i.server_id=$${p.length+1} OR i.server_id IS NULL)`;p.push(server_id);}
  sql+=' ORDER BY i.item_id LIMIT 500';
  const items=await getAll(sql,p);
  res.render('pages/items/index',{title:'Vật phẩm',admin:req.session.admin,
    items,servers,type:type||'',rarity:rarity??'',search:search||'',
    selectedServer:server_id||'',activePage:'items'});
});

router.get('/create', async(req,res)=>{
  const servers=await getAll('SELECT id,name FROM game_servers ORDER BY id');
  const icons=await getAll("SELECT id,name,file_path FROM assets WHERE type='sprite' AND category='item' ORDER BY name LIMIT 200");
  res.render('pages/items/form',{title:'Tạo vật phẩm',admin:req.session.admin,
    item:null,servers,icons,activePage:'items'});
});

router.post('/create', async(req,res)=>{
  const {item_id,name,name_vn,type,category,rarity,base_price,sell_price,
    stackable,tradeable,description,icon_asset_id,server_id,stats,drop_sources}=req.body;
  try{
    await query(`INSERT INTO items(item_id,name,name_vn,type,category,rarity,base_price,sell_price,
      stackable,tradeable,description,icon_asset_id,server_id,drop_sources)
      VALUES($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,$14)`,
      [item_id,name,name_vn||null,type,category||null,parseInt(rarity)||0,
       parseInt(base_price)||100,parseInt(sell_price)||50,
       !!stackable,!!tradeable,description||null,
       icon_asset_id||null,server_id||null,drop_sources||null]);
    res.redirect('/items?success=Đã+tạo+vật+phẩm');
  }catch(e){res.redirect('/items/create?error='+encodeURIComponent(e.message));}
});

router.get('/:id/edit', async(req,res)=>{
  const item=await getOne('SELECT * FROM items WHERE id=$1',[req.params.id]);
  if(!item)return res.redirect('/items');
  const servers=await getAll('SELECT id,name FROM game_servers ORDER BY id');
  const icons=await getAll("SELECT id,name,file_path FROM assets WHERE type='sprite' ORDER BY name LIMIT 200");
  res.render('pages/items/form',{title:'Sửa vật phẩm',admin:req.session.admin,
    item,servers,icons,activePage:'items'});
});

router.post('/:id/edit', async(req,res)=>{
  const {name,name_vn,type,category,rarity,base_price,sell_price,stackable,tradeable,
    description,icon_asset_id,server_id,drop_sources}=req.body;
  await query(`UPDATE items SET name=$1,name_vn=$2,type=$3,category=$4,rarity=$5,base_price=$6,
    sell_price=$7,stackable=$8,tradeable=$9,description=$10,icon_asset_id=$11,
    server_id=$12,drop_sources=$13,updated_at=NOW() WHERE id=$14`,
    [name,name_vn||null,type,category||null,parseInt(rarity)||0,
     parseInt(base_price)||100,parseInt(sell_price)||50,!!stackable,!!tradeable,
     description||null,icon_asset_id||null,server_id||null,drop_sources||null,req.params.id]);
  res.redirect('/items');
});

router.delete('/:id', async(req,res)=>{
  await query('DELETE FROM items WHERE id=$1',[req.params.id]);
  res.json({ok:true});
});

// Export for game server consumption
router.get('/export/json', async(req,res)=>{
  const items=await getAll('SELECT * FROM items ORDER BY item_id');
  res.json({items, generated: new Date().toISOString(), count: items.length});
});

module.exports = router;
