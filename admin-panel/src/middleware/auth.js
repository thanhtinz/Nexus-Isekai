module.exports = (req, res, next) => {
  if (req.session && req.session.admin) return next();
  req.session.returnTo = req.originalUrl;
  return res.redirect('/login');
};
