const router = require('express').Router();
const { getAll, getOne, query } = require('../services/db');
const bcrypt = require('bcryptjs');

router.get('/', async(req,res) => {
  const admins = await getAll('SELECT id,username,role,created_at,last_login FROM admin_users ORDER BY id');
  const servers = await getAll('SELECT * FROM game_servers ORDER BY id');
  res.render('pages/config/index', {
    title: 'Cài đặt hệ thống', admin: req.session.admin,
    admins, servers, activePage: 'config'
  });
});

// Add admin account
router.post('/admin/add', async(req,res) => {
  if(req.session.admin.role !== 'superadmin') return res.json({ok:false,error:'Không có quyền'});
  const { username, password, role } = req.body;
  const hash = await bcrypt.hash(password, 12);
  try {
    await query('INSERT INTO admin_users(username,password_hash,role)VALUES($1,$2,$3)',[username,hash,role||'admin']);
    res.json({ok:true});
  } catch(e){ res.json({ok:false,error:e.message}); }
});

// Change password
router.post('/admin/change-password', async(req,res) => {
  const { old_password, new_password } = req.body;
  const admin = await getOne('SELECT * FROM admin_users WHERE id=$1',[req.session.admin.id]);
  if(!(await bcrypt.compare(old_password, admin.password_hash)))
    return res.json({ok:false,error:'Mật khẩu cũ không đúng'});
  const hash = await bcrypt.hash(new_password, 12);
  await query('UPDATE admin_users SET password_hash=$1 WHERE id=$2',[hash,req.session.admin.id]);
  res.json({ok:true});
});

router.delete('/admin/:id', async(req,res) => {
  if(req.session.admin.role !== 'superadmin') return res.json({ok:false,error:'Không có quyền'});
  if(parseInt(req.params.id) === req.session.admin.id) return res.json({ok:false,error:'Không thể xóa chính mình'});
  await query('DELETE FROM admin_users WHERE id=$1',[req.params.id]);
  res.json({ok:true});
});

// Update server config
router.put('/server/:id', async(req,res) => {
  const { name, description, max_players, api_user, api_pass, host, game_port, admin_port } = req.body;
  await query(`UPDATE game_servers SET name=$1,description=$2,max_players=$3,api_user=$4,api_pass=$5,host=$6,game_port=$7,admin_port=$8,updated_at=NOW() WHERE id=$9`,
    [name,description,parseInt(max_players)||500,api_user,api_pass,host,parseInt(game_port),parseInt(admin_port),req.params.id]);
  res.json({ok:true});
});

module.exports = router;
