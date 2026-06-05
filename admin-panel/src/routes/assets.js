const router = require('express').Router();
const { getAll, getOne, query } = require('../services/db');
const multer = require('multer');
const path   = require('path');
const fs     = require('fs');

const storage = multer.diskStorage({
  destination(req,file,cb){
    const dir = path.join(process.env.UPLOAD_DIR||'./src/public/uploads', 'assets');
    fs.mkdirSync(dir,{recursive:true}); cb(null,dir);
  },
  filename(req,file,cb){ cb(null, Date.now()+'-'+file.originalname.replace(/\s+/g,'_')); }
});
const upload = multer({storage, limits:{fileSize:50*1024*1024}});

router.get('/', async(req,res)=>{
  const {type,category,search} = req.query;
  let sql=`SELECT a.*,au.username as uploader FROM assets a LEFT JOIN admin_users au ON a.uploaded_by=au.id WHERE 1=1`;
  const p=[];
  if(type){sql+=` AND a.type=$${p.length+1}`;p.push(type);}
  if(category){sql+=` AND a.category=$${p.length+1}`;p.push(category);}
  if(search){sql+=` AND a.name ILIKE $${p.length+1}`;p.push('%'+search+'%');}
  sql+=' ORDER BY a.created_at DESC LIMIT 200';
  const assets = await getAll(sql,p);
  const stats = await getOne(`SELECT
    COUNT(*) FILTER (WHERE type='sprite') as sprites,
    COUNT(*) FILTER (WHERE type='animation') as animations,
    COUNT(*) FILTER (WHERE type='tile') as tiles,
    COUNT(*) FILTER (WHERE type='audio') as audio,
    COUNT(*) total,
    SUM(file_size) total_size FROM assets`);
  res.render('pages/assets/index',{title:'Asset Manager',admin:req.session.admin,
    assets,stats,type:type||'',category:category||'',search:search||'',activePage:'assets'});
});

router.post('/upload', upload.array('files',20), async(req,res)=>{
  const {type,category,tags,server_id} = req.body;
  try{
    for(const file of req.files){
      await query(`INSERT INTO assets(name,type,category,file_path,file_size,mime_type,tags,server_id,uploaded_by)
        VALUES($1,$2,$3,$4,$5,$6,$7,$8,$9)`,
        [file.originalname, type||'sprite', category||'misc',
         '/uploads/assets/'+file.filename, file.size, file.mimetype,
         tags||null, server_id||null, req.session.admin.id]);
    }
    res.json({ok:true, count:req.files.length});
  }catch(e){res.json({ok:false,error:e.message});}
});

router.delete('/:id', async(req,res)=>{
  const asset = await getOne('SELECT * FROM assets WHERE id=$1',[req.params.id]);
  if(asset?.file_path){
    const full = path.join('./src/public', asset.file_path);
    fs.unlink(full,()=>{});
  }
  await query('DELETE FROM assets WHERE id=$1',[req.params.id]);
  res.json({ok:true});
});

// List for select dropdowns
router.get('/list', async(req,res)=>{
  const {type} = req.query;
  const assets = await getAll(`SELECT id,name,file_path,type,category FROM assets ${type?'WHERE type=$1':''} ORDER BY name LIMIT 500`,
    type?[type]:[]);
  res.json({assets});
});

module.exports = router;
