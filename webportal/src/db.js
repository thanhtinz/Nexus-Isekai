const { Pool } = require('pg');
const pool = new Pool({
  host: process.env.DB_HOST||'localhost', port: process.env.DB_PORT||5432,
  database: process.env.DB_NAME||'fantasyrealm',
  user: process.env.DB_USER||'fro', password: process.env.DB_PASS||'fro123',
  max: 10
});
module.exports = { query: (t,p) => pool.query(t,p) };
