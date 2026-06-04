const router = require('express').Router();
const { getAll, getOne } = require('../services/db');
const { callServerAPI } = require('../services/ServerManager');

router.get('/', async(req,res) => {
  const { server_id, board } = req.query;
  const servers = await getAll('SELECT id,name,slug FROM game_servers ORDER BY id');
  const sv = server_id ? await getOne('SELECT * FROM game_servers WHERE id=$1',[server_id]) : servers[0];
  const boards = ['FASHION','FISHING','WEALTH','COOKING','FOLLOWERS'];
  const selectedBoard = (board||'WEALTH').toUpperCase();
  let entries = [], error = null;
  if(sv){
    try{
      const d = await callServerAPI(sv,'GET',`/api/admin/leaderboard/${selectedBoard}`);
      entries = d.entries || [];
    }catch(e){ error = e.message; }
  }
  res.render('pages/leaderboard/index', {
    title: 'Bảng xếp hạng', admin: req.session.admin,
    servers, selectedServer: sv, boards, selectedBoard, entries, error, activePage: 'leaderboard'
  });
});

module.exports = router;
