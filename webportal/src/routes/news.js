const router = require('express').Router();
const { query } = require('../db');

router.get('/', async (req, res) => {
  const page = parseInt(req.query.page)||1;
  const cat  = req.query.category||'';
  const offset = (page-1)*12;
  const where = cat ? 'AND category=$3' : '';
  const params = cat ? [12, offset, cat] : [12, offset];
  const { rows } = await query(
    `SELECT id,title,slug,excerpt,category,thumbnail,views,created_at FROM news WHERE is_published=true ${where} ORDER BY is_pinned DESC,created_at DESC LIMIT $1 OFFSET $2`,
    params).catch(()=>({rows:[]}));
  const total = parseInt((await query(`SELECT COUNT(*) FROM news WHERE is_published=true ${cat?'AND category=$1':''}`,cat?[cat]:[]).catch(()=>({rows:[{count:0}]}))).rows[0].count);
  res.render('pages/news/index', { title: 'Tin tức', rows, total, page, pages: Math.ceil(total/12), category: cat });
});

router.get('/:slug', async (req, res) => {
  await query('UPDATE news SET views=views+1 WHERE slug=$1', [req.params.slug]).catch(()=>{});
  const { rows } = await query('SELECT * FROM news WHERE slug=$1 AND is_published=true', [req.params.slug]).catch(()=>({rows:[]}));
  if (!rows[0]) return res.redirect('/tin-tuc');
  const { rows: related } = await query(
    'SELECT id,title,slug,thumbnail,created_at FROM news WHERE category=$1 AND slug!=$2 AND is_published=true ORDER BY created_at DESC LIMIT 4',
    [rows[0].category, req.params.slug]).catch(()=>({rows:[]}));
  res.render('pages/news/detail', { title: rows[0].title, article: rows[0], related });
});
module.exports = router;
