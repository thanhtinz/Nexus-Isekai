#!/usr/bin/env node
/**
 * Import sprite từ game-assets/ vào bảng `assets` + copy file vào public/uploads.
 * Tự phân loại type/category theo cấu trúc thư mục.
 *
 * Chạy:  node src/scripts/import-assets.js [đường-dẫn-game-assets]
 * Mặc định đọc ../../game-assets (so với admin-panel/)
 */
require('dotenv').config();
const fs   = require('fs');
const path = require('path');
const { pool, query, initAdminTables } = require('../services/db');

const SRC = process.argv[2] || path.resolve(__dirname, '../../../game-assets');
const UPLOAD_DIR = process.env.UPLOAD_DIR || path.resolve(__dirname, '../public/uploads');
const PUBLIC_PREFIX = '/uploads/game-assets';

// Map thư mục → (type, category)
function classify(relPath) {
  const p = relPath.toLowerCase();
  if (p.startsWith('characters/eyes'))      return { type: 'sprite', category: 'character', tag: 'eyes' };
  if (p.startsWith('characters/hairstyles'))return { type: 'sprite', category: 'character', tag: 'hair' };
  if (p.startsWith('characters/outfits'))   return { type: 'sprite', category: 'character', tag: 'outfit' };
  if (p.startsWith('characters/base'))      return { type: 'sprite', category: 'character', tag: 'base' };
  if (p.startsWith('combat/'))              return { type: 'animation', category: 'character', tag: 'combat' };
  if (p.startsWith('npcs/'))                return { type: 'sprite', category: 'npc', tag: 'npc' };
  if (p.startsWith('monsters/'))            return { type: 'sprite', category: 'mob', tag: 'mob' };
  if (p.startsWith('ai-generated'))         return { type: 'sprite', category: 'character', tag: 'ai-gen' };
  return null; // bỏ qua _licenses, _tools, README...
}

function walk(dir, base) {
  let out = [];
  for (const f of fs.readdirSync(dir)) {
    const full = path.join(dir, f);
    const rel  = path.relative(base, full);
    if (f.startsWith('_') || f === 'README.md') continue;          // bỏ _licenses, _tools
    if (fs.statSync(full).isDirectory()) out = out.concat(walk(full, base));
    else if (f.toLowerCase().endsWith('.png')) out.push({ full, rel });
  }
  return out;
}

(async () => {
  if (!fs.existsSync(SRC)) {
    console.error('✗ Không tìm thấy thư mục game-assets:', SRC);
    process.exit(1);
  }
  await initAdminTables();

  const files = walk(SRC, SRC);
  console.log(`Tìm thấy ${files.length} file PNG trong ${SRC}`);

  fs.mkdirSync(path.join(UPLOAD_DIR, 'game-assets'), { recursive: true });

  let imported = 0, skipped = 0;
  for (const { full, rel } of files) {
    const cls = classify(rel);
    if (!cls) { skipped++; continue; }

    // Copy file vào public/uploads/game-assets/<rel> (giữ cấu trúc)
    const destRel = path.join('game-assets', rel);
    const destAbs = path.join(UPLOAD_DIR, destRel);
    fs.mkdirSync(path.dirname(destAbs), { recursive: true });
    fs.copyFileSync(full, destAbs);

    const publicPath = (PUBLIC_PREFIX + '/' + rel).replace(/\\/g, '/');
    const name = path.basename(rel);
    const size = fs.statSync(full).size;

    // Insert nếu chưa có (dựa trên file_path)
    const exists = await query('SELECT id FROM assets WHERE file_path=$1', [publicPath]);
    if (exists.rows.length) { skipped++; continue; }

    await query(
      `INSERT INTO assets(name,type,category,file_path,file_size,mime_type,tags)
       VALUES($1,$2,$3,$4,$5,'image/png',$6)`,
      [name, cls.type, cls.category, publicPath, size, cls.tag]
    );
    imported++;
    if (imported % 200 === 0) console.log(`  ...đã import ${imported}`);
  }

  console.log(`✅ Xong: import ${imported} asset, bỏ qua ${skipped} (đã có hoặc không phân loại).`);
  await pool.end();
})().catch(e => { console.error('Lỗi:', e.message); process.exit(1); });
