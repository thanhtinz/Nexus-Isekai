const router = require('express').Router();
router.get('/', (req, res) => {
  res.render('pages/download', { title: 'Tải Game' });
});
module.exports = router;
