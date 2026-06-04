const router = require('express').Router();
const { getAll, getOne, query } = require('../services/db');
const multer = require('multer');
const path = require('path');
const fs = require('fs');

const storage = multer.diskStorage({
  destination: (req,file,cb) => {
    const dir = process.env.UPLOAD_DIR || './src/public/uploads';
    fs.mkdirSync(dir, {recursive:true});
    cb(null, dir);
  },
  filename: (req,file,cb) => cb(null, Date.now()+path.extname(file.originalname))
});
const upload = multer({ storage, limits: { fileSize: 10*1024*1024 } });

function slugify(text) {
  return text.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g,'')
    .replace(/[^a-z0-9]+/g,'-').replace(/^-|-$/g,'');
}

router.get('/', async(req,res) => {
  const { category, server_id } = req.query;
  const servers = await getAll('SELECT id,name,slug FROM game_servers ORDER BY id');
  let sql = `SELECT n.*,au.username as author_name,gs.name as server_name
    FROM news_articles n
    LEFT JOIN admin_users au ON n.author_id=au.id
    LEFT JOIN game_servers gs ON n.server_id=gs.id
    WHERE 1=1`;
  const params = [];
  if(category){ sql += ` AND n.category=$${params.length+1}`; params.push(category); }
  if(server_id){ sql += ` AND n.server_id=$${params.length+1}`; params.push(server_id); }
  sql += ' ORDER BY n.created_at DESC LIMIT 100';
  const articles = await getAll(sql, params);
  res.render('pages/news/index', {
    title: 'Tin tức & Thông báo', admin: req.session.admin,
    servers, articles, category:category||'', selectedServer: server_id||'', activePage: 'news'
  });
});

router.get('/create', async(req,res) => {
  const servers = await getAll('SELECT id,name,slug FROM game_servers ORDER BY id');
  res.render('pages/news/create', {
    title: 'Viết bài mới', admin: req.session.admin,
    servers, article: null, activePage: 'news'
  });
});

router.post('/create', upload.single('thumbnail'), async(req,res) => {
  const { title, category, content, excerpt, server_id, is_published, is_pinned } = req.body;
  const slug = slugify(title) + '-' + Date.now();
  const thumbnail = req.file ? '/uploads/' + req.file.filename : null;
  await query(`INSERT INTO news_articles(title,slug,category,content,excerpt,thumbnail,server_id,author_id,is_published,is_pinned,published_at)
    VALUES($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11)`,
    [title, slug, category||'general', content, excerpt||null, thumbnail,
     server_id||null, req.session.admin.id, !!is_published, !!is_pinned,
     is_published ? new Date() : null]);
  res.redirect('/news');
});

router.get('/:id/edit', async(req,res) => {
  const servers = await getAll('SELECT id,name,slug FROM game_servers ORDER BY id');
  const article = await getOne('SELECT * FROM news_articles WHERE id=$1',[req.params.id]);
  if(!article) return res.redirect('/news');
  res.render('pages/news/create', {
    title: 'Sửa bài viết', admin: req.session.admin,
    servers, article, activePage: 'news'
  });
});

router.put('/:id', upload.single('thumbnail'), async(req,res) => {
  const { title, category, content, excerpt, server_id, is_published, is_pinned } = req.body;
  const old = await getOne('SELECT * FROM news_articles WHERE id=$1',[req.params.id]);
  const thumbnail = req.file ? '/uploads/'+req.file.filename : old?.thumbnail;
  await query(`UPDATE news_articles SET title=$1,category=$2,content=$3,excerpt=$4,thumbnail=$5,
    server_id=$6,is_published=$7,is_pinned=$8,published_at=$9,updated_at=NOW() WHERE id=$10`,
    [title, category||'general', content, excerpt||null, thumbnail, server_id||null,
     !!is_published, !!is_pinned, is_published ? new Date() : null, req.params.id]);
  res.redirect('/news');
});

router.delete('/:id', async(req,res) => {
  await query('DELETE FROM news_articles WHERE id=$1',[req.params.id]);
  res.json({ok:true});
});

router.post('/:id/toggle-pin', async(req,res) => {
  await query('UPDATE news_articles SET is_pinned=NOT is_pinned WHERE id=$1',[req.params.id]);
  res.json({ok:true});
});

module.exports = router;
