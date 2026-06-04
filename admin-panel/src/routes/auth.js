const router = require('express').Router();
const bcrypt = require('bcryptjs');
const { getOne, query } = require('../services/db');

router.get('/', (req,res) => res.redirect(req.session.admin ? '/dashboard' : '/login'));
router.get('/login', (req,res) => {
  if(req.session.admin) return res.redirect('/dashboard');
  res.render('pages/auth/login', { title: 'Đăng nhập', error: null });
});
router.post('/login', async(req,res) => {
  const { username, password } = req.body;
  try {
    const admin = await getOne('SELECT * FROM admin_users WHERE username=$1', [username]);
    if(!admin || !(await bcrypt.compare(password, admin.password_hash))) {
      return res.render('pages/auth/login', { title: 'Đăng nhập', error: 'Sai tài khoản hoặc mật khẩu' });
    }
    await query('UPDATE admin_users SET last_login=NOW() WHERE id=$1', [admin.id]);
    req.session.admin = { id: admin.id, username: admin.username, role: admin.role };
    const returnTo = req.session.returnTo || '/dashboard';
    delete req.session.returnTo;
    res.redirect(returnTo);
  } catch(e) {
    res.render('pages/auth/login', { title: 'Đăng nhập', error: 'Lỗi server: ' + e.message });
  }
});
router.get('/logout', (req,res) => {
  req.session.destroy();
  res.redirect('/login');
});
module.exports = router;
