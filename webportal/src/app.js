require('dotenv').config();
const express = require('express');
const path = require('path');
const compression = require('compression');
const helmet = require('helmet');
const morgan = require('morgan');
const { rateLimit } = require('express-rate-limit');

const app = express();
app.use(compression());
app.use(helmet({ contentSecurityPolicy: false }));
app.use(morgan('dev'));
app.use(express.json());
app.use(express.urlencoded({ extended: true }));
app.use(express.static(path.join(__dirname, 'public')));
app.set('view engine', 'ejs');
app.set('views', path.join(__dirname, 'views'));

// Rate limit for API
const limiter = rateLimit({ windowMs: 60000, max: 60 });
app.use('/api', limiter);

app.locals.siteName = process.env.SITE_NAME || 'Fantasy Realm Online';
app.locals.siteDesc = process.env.SITE_DESC || 'MMORPG xã hội fantasy Việt Nam';

const homeRouter = require('./routes/home');
const newsRouter = require('./routes/news');
const lbRouter   = require('./routes/leaderboard');
const gcRouter   = require('./routes/giftcode');
const dlRouter   = require('./routes/download');

app.use('/', homeRouter);
app.use('/tin-tuc', newsRouter);
app.use('/bang-xep-hang', lbRouter);
app.use('/giftcode', gcRouter);
app.use('/tai-game', dlRouter);

app.use((req, res) => res.status(404).render('pages/404', { title: '404' }));

const PORT = process.env.PORTAL_PORT || 3001;
app.listen(PORT, () => console.log(`[Portal] http://localhost:${PORT}`));
