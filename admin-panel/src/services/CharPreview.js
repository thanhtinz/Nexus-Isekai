/**
 * Ghép các layer paperdoll thành 1 frame preview.
 * Dùng 'sharp' nếu có; nếu không, trả về danh sách layer để client tự ghép.
 */
const fs = require('fs');
const path = require('path');

let sharp = null;
try { sharp = require('sharp'); } catch (e) { /* optional */ }

const FRAME = 64;          // mỗi frame 64x64
const COLS = 8;            // sheet 512x512 = 8x8
// Layer order dưới→trên (theo Seliel naming)
const LAYER_ORDER = ['skin', 'outfit', 'eyes', 'hair'];

function frameRect(index) {
  const col = index % COLS, row = Math.floor(index / COLS);
  return { left: col * FRAME, top: row * FRAME, width: FRAME, height: FRAME };
}

/**
 * @param layers [{slot, absPath}] đã sắp đúng thứ tự
 * @param frameIndex frame nào trên sheet (0 = idle south)
 * @returns Buffer PNG hoặc null nếu không có sharp
 */
async function compose(layers, frameIndex = 0) {
  if (!sharp) return null;
  const rect = frameRect(frameIndex);
  const base = sharp({ create: { width: FRAME, height: FRAME, channels: 4,
    background: { r: 0, g: 0, b: 0, alpha: 0 } } }).png();

  const composites = [];
  for (const l of layers) {
    if (!l.absPath || !fs.existsSync(l.absPath)) continue;
    try {
      const meta = await sharp(l.absPath).metadata();
      // Nếu là sheet lớn → crop frame; nếu nhỏ (eye sheet 160x64) → crop theo width frame
      let buf;
      if (meta.width >= rect.left + FRAME && meta.height >= rect.top + FRAME) {
        buf = await sharp(l.absPath).extract(rect).png().toBuffer();
      } else {
        // sheet nhỏ: lấy frame đầu theo chiều ngang
        const r2 = { left: 0, top: 0, width: Math.min(FRAME, meta.width), height: Math.min(FRAME, meta.height) };
        buf = await sharp(l.absPath).extract(r2).png().toBuffer();
      }
      composites.push({ input: buf, left: 0, top: 0 });
    } catch (e) { /* skip bad layer */ }
  }
  return base.composite(composites).png().toBuffer();
}

module.exports = { compose, LAYER_ORDER, FRAME };
