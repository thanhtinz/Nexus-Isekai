
// Leaderboard AJAX
async function loadLeaderboard(serverId, board) {
  const rows = document.getElementById('lb-rows');
  if(!rows) return;
  rows.innerHTML = '<tr><td colspan="4" style="text-align:center;padding:24px;color:#6b6b8a">Đang tải...</td></tr>';
  try {
    const r = await fetch('/api-proxy/leaderboard?server_id='+serverId+'&board='+board);
    const d = await r.json();
    const entries = d.entries || [];
    rows.innerHTML = entries.length
      ? entries.map((e,i)=>`<tr>
          <td>${i===0?'🥇':i===1?'🥈':i===2?'🥉':'#'+(i+1)}</td>
          <td><strong>${e.name}</strong></td>
          <td>${e.faction||'-'}</td>
          <td><strong style="color:#c4b5fd">${(e.score||0).toLocaleString()}</strong></td>
        </tr>`).join('')
      : '<tr><td colspan="4" style="text-align:center;padding:24px;color:#6b6b8a">Không có dữ liệu</td></tr>';
  } catch(e) {
    rows.innerHTML = '<tr><td colspan="4" style="text-align:center;color:#f87171;padding:24px">Lỗi kết nối</td></tr>';
  }
}

// Smooth scroll
document.querySelectorAll('a[href^="#"]').forEach(a => {
  a.addEventListener('click', e => {
    e.preventDefault();
    document.querySelector(a.getAttribute('href'))?.scrollIntoView({behavior:'smooth'});
  });
});
