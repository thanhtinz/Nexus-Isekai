require('dotenv').config();
const express = require('express');
const path    = require('path');
const axios   = require('axios');

const app = express();
app.use(require('compression')());
app.use(require('helmet')({ contentSecurityPolicy: false }));
app.use(require('morgan')('tiny'));
app.use(express.urlencoded({ extended: true }));
app.use(express.static(path.join(__dirname, 'public')));
app.set('view engine', 'ejs');
app.set('views', path.join(__dirname, 'views'));

const ADMIN_API = process.env.ADMIN_API_URL || 'http://localhost:3000/api/v1';

async function fetchAPI(ep, fallback) {
  try { const r = await axios.get(ADMIN_API+ep,{timeout:3000}); return r.data; }
  catch(e) { return fallback; }
}

app.get('/', async(req,res) => {
  const [nd,sd] = await Promise.all([fetchAPI('/news?limit=9',{articles:[]}),fetchAPI('/servers',{servers:[]})]);
  const news = nd.articles||[]; const servers = (sd.servers||[]).filter(s=>s.type!=='test');
  res.render('pages/home', {page:'home',news,servers});
});
app.get('/news', async(req,res) => {
  const {category} = req.query;
  const d = await fetchAPI(`/news?limit=20${category?'&category='+category:''}`,{articles:[]});
  res.render('pages/news',{page:'news',articles:d.articles||[],category:category||''});
});
app.get('/news/:slug', async(req,res) => {
  const article = await fetchAPI('/news/'+req.params.slug,null);
  if(!article) return res.status(404).render('pages/404',{page:''});
  const related = await fetchAPI('/news?category='+article.category+'&limit=4',{articles:[]});
  res.render('pages/news-detail',{page:'news',article,related:related.articles||[]});
});
app.get('/leaderboard', async(req,res) => {
  const sd = await fetchAPI('/servers',{servers:[]});
  res.render('pages/leaderboard',{page:'leaderboard',servers:sd.servers||[]});
});
app.get('/giftcode', (req,res) => res.render('pages/giftcode',{page:'giftcode',result:null}));
app.post('/giftcode', async(req,res) => {
  const {code,character_name,server_id} = req.body;
  try {
    const r = await axios.post(ADMIN_API+'/giftcode/redeem',{code,character_name,server_id},{timeout:5000});
    res.render('pages/giftcode',{page:'giftcode',result:r.data});
  } catch(e){ res.render('pages/giftcode',{page:'giftcode',result:{ok:false,error:'Lỗi kết nối'}}); }
});
app.get('/download', async(req,res) => {
  const sd = await fetchAPI('/servers',{servers:[]});
  res.render('pages/download',{page:'download',servers:sd.servers||[]});
});
app.get('/events', async(req,res) => {
  const d = await fetchAPI('/news?category=event&limit=12',{articles:[]});
  res.render('pages/events',{page:'events',events:d.articles||[]});
});
app.use((req,res) => res.status(404).render('pages/404',{page:''}));

const PORT = process.env.WEBSITE_PORT || 4000;
app.listen(PORT, () => console.log('[FRO Website] http://localhost:'+PORT));
