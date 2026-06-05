require('dotenv').config();
const express    = require('express');
const session    = require('express-session');
const path       = require('path');
const morgan     = require('morgan');
const compression= require('compression');
const helmet     = require('helmet');
const http       = require('http');
const { Server } = require('socket.io');
const cron       = require('node-cron');
const moment     = require('moment');
const { initAdminTables } = require('./services/db');
const { getAllServersWithStatus, getServerStatus } = require('./services/ServerManager');

const app = express();
const server = http.createServer(app);
const io = new Server(server, { cors: { origin: '*' } });

app.use(compression());
app.use(helmet({ contentSecurityPolicy: false }));
app.use(morgan('tiny'));
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true, limit: '10mb' }));
app.use(express.static(path.join(__dirname, 'public')));
app.use(require('method-override')('_method'));
app.use(session({
  secret: process.env.SESSION_SECRET || 'fro_admin_secret_2024',
  resave: false, saveUninitialized: false,
  cookie: { secure: false, maxAge: 8 * 3600 * 1000 }
}));

app.set('view engine', 'ejs');
app.set('views', path.join(__dirname, 'views'));

// Custom render that wraps pages in main layout
app.use((req, res, next) => {
  const originalRender = res.render.bind(res);
  res.render = function(view, locals, cb) {
    if(view.startsWith('pages/auth') || view === 'pages/404') {
      return originalRender(view, locals, cb);
    }
    originalRender(view, { ...locals, layout: false }, (err, body) => {
      if(err) return next(err);
      originalRender('layouts/main', { ...locals, body }, cb);
    });
  };
  next();
});
app.locals.moment = moment;
app.locals.io = io;

// ── Routes
app.use('/',           require('./routes/auth'));
app.use('/dashboard',  require('./middleware/auth'), require('./routes/dashboard'));
app.use('/players',    require('./middleware/auth'), require('./routes/players'));
app.use('/servers',    require('./middleware/auth'), require('./routes/servers'));
app.use('/economy',    require('./middleware/auth'), require('./routes/economy'));
app.use('/events',     require('./middleware/auth'), require('./routes/events'));
app.use('/giftcode',   require('./middleware/auth'), require('./routes/giftcode'));
app.use('/news',       require('./middleware/auth'), require('./routes/news'));
app.use('/leaderboard',require('./middleware/auth'), require('./routes/leaderboard'));
app.use('/config',     require('./middleware/auth'), require('./routes/config'));
app.use('/logs',       require('./middleware/auth'), require('./routes/logs'));
app.use('/api/v1',     require('./routes/api'));

// ── Content Management
app.use('/assets',     require('./middleware/auth'), require('./routes/assets'));
app.use('/items',      require('./middleware/auth'), require('./routes/items'));
app.use('/npcs',       require('./middleware/auth'), require('./routes/npcs'));
app.use('/dialogs',    require('./middleware/auth'), require('./routes/dialogs'));
app.use('/mobs',       require('./middleware/auth'), require('./routes/mobs'));
app.use('/maps',       require('./middleware/auth'), require('./routes/maps'));
app.use('/professions',require('./middleware/auth'), require('./routes/professions'));
app.use('/audio',      require('./middleware/auth'), require('./routes/audio'));

// ── 404
app.use((req, res) => res.status(404).render('pages/404', { title: '404' }));

// ── Socket.IO: real-time server stats push every 15s
io.on('connection', socket => {
  socket.on('subscribe_server', async (serverId) => {
    socket.join(`server_${serverId}`);
  });
});

cron.schedule('*/15 * * * * *', async () => {
  try {
    const servers = await getAllServersWithStatus();
    servers.forEach(sv => {
      io.to(`server_${sv.id}`).emit('server_stats', {
        id: sv.id, ...sv.liveStatus
      });
    });
    io.emit('servers_summary', servers.map(sv => ({
      id: sv.id, name: sv.name, slug: sv.slug, type: sv.type,
      status: sv.liveStatus.status, online: sv.liveStatus.online||0
    })));
  } catch(e) {}
});

const PORT = process.env.PORT || 3000;
initAdminTables().then(() => {
  server.listen(PORT, () => console.log(`[FRO Admin] http://localhost:${PORT}`));
}).catch(err => {
  console.error('[DB] Init failed, starting anyway:', err.message);
  server.listen(PORT, () => console.log(`[FRO Admin] http://localhost:${PORT} (no DB)`));
});
