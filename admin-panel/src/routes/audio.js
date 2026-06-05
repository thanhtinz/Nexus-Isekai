const router = require('express').Router();
const {getAll,getOne,query} = require('../services/db');
const multer = require('multer');
const path   = require('path');
const fs     = require('fs');

const storage = multer.diskStorage({
  destination(req,file,cb){
    const dir = path.join(process.env.UPLOAD_DIR||'./src/public/uploads','audio');
    fs.mkdirSync(dir,{recursive:true}); cb(null,dir);
  },
  filename(req,file,cb){ cb(null,Date.now()+'-'+file.originalname.replace(/\s+/g,'_')); }
});
const upload = multer({storage, limits:{fileSize:100*1024*1024},
  fileFilter(req,file,cb){ cb(null, /audio/i.test(file.mimetype)||/\.(mp3|ogg|wav|aac|flac)$/i.test(file.originalname)); }
});

router.get('/', async(req,res)=>{
  const {type,category,search}=req.query;
  let sql=`SELECT a.*,au.username as uploader FROM audio a LEFT JOIN admin_users au ON a.uploaded_by=au.id WHERE 1=1`;
  const p=[];
  if(type){sql+=` AND a.type=$${p.length+1}`;p.push(type);}
  if(category){sql+=` AND a.category=$${p.length+1}`;p.push(category);}
  if(search){sql+=` AND a.name ILIKE $${p.length+1}`;p.push('%'+search+'%');}
  sql+=' ORDER BY a.type,a.name LIMIT 300';
  const tracks=await getAll(sql,p);
  const stats=await getOne(`SELECT
    COUNT(*) FILTER (WHERE type='bgm') as bgm,
    COUNT(*) FILTER (WHERE type='sfx') as sfx,
    COUNT(*) FILTER (WHERE type='voice') as voice,
    COUNT(*) FILTER (WHERE type='ambient') as ambient,
    COUNT(*) total, SUM(file_size) total_size FROM audio`);
  res.render('pages/audio/index',{title:'Âm thanh & Âm nhạc',admin:req.session.admin,
    tracks,stats,type:type||'',category:category||'',search:search||'',activePage:'audio'});
});

router.post('/upload', upload.array('files',20), async(req,res)=>{
  const {type,category,tags,zone_id,loop_track,server_id}=req.body;
  try{
    for(const file of req.files){
      await query(`INSERT INTO audio(name,type,category,file_path,file_size,format,tags,zone_id,loop,server_id,uploaded_by)
        VALUES($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11)`,
        [file.originalname, type||'sfx', category||'misc',
         '/uploads/audio/'+file.filename, file.size,
         path.extname(file.originalname).slice(1).toLowerCase(),
         tags||null, zone_id||null, !!loop_track,
         server_id||null, req.session.admin.id]);
    }
    res.json({ok:true, count:req.files.length});
  }catch(e){res.json({ok:false,error:e.message});}
});

router.post('/:id/edit', async(req,res)=>{
  const {name,type,category,tags,zone_id,loop,volume_default}=req.body;
  await query(`UPDATE audio SET name=$1,type=$2,category=$3,tags=$4,zone_id=$5,loop=$6,volume_default=$7 WHERE id=$8`,
    [name,type,category,tags||null,zone_id||null,!!loop,parseFloat(volume_default)||1.0,req.params.id]);
  res.json({ok:true});
});

router.delete('/:id', async(req,res)=>{
  const track=await getOne('SELECT * FROM audio WHERE id=$1',[req.params.id]);
  if(track?.file_path){
    const full=path.join('./src/public',track.file_path);
    fs.unlink(full,()=>{});
  }
  await query('DELETE FROM audio WHERE id=$1',[req.params.id]);
  res.json({ok:true});
});

// Assign BGM to zone
router.post('/assign-zone', async(req,res)=>{
  const {audio_id,zone_id}=req.body;
  await query('UPDATE audio SET zone_id=$1 WHERE id=$2',[zone_id,audio_id]);
  await query('UPDATE maps SET bg_music_id=$1 WHERE zone_id=$2',[audio_id,zone_id]);
  res.json({ok:true});
});

router.get('/list', async(req,res)=>{
  const {type}=req.query;
  const tracks=await getAll(`SELECT id,name,file_path,type,category,duration_secs FROM audio ${type?'WHERE type=$1':''} ORDER BY name LIMIT 500`,
    type?[type]:[]);
  res.json({tracks});
});

module.exports = router;
