/* Render the shared HMXHARNESS slide spec into:
 *   - editable PPTX (PptxGenJS)
 *   - matching PDF (pdfkit)
 *   - PNG previews (@napi-rs/canvas)
 * Run from this directory with NODE_PATH=/tmp/opencode/hmx_deck/node_modules
 */
const fs = require('fs');
const path = require('path');
const PptxGenJS = require('pptxgenjs');
const PDFDocument = require('pdfkit');
const { createCanvas, loadImage, GlobalFonts } = require('@napi-rs/canvas');
const { slides, C, SW, SH, ROOT } = require('./deck_spec');

const OUT = ROOT;
const PREVIEW = path.join(OUT, 'slides-preview');
fs.mkdirSync(PREVIEW, { recursive: true });
const FONTS = path.join(OUT, 'assets', 'fonts');
const INTER = path.join(FONTS, 'Inter-Regular.ttf');
const SPACE = path.join(FONTS, 'SpaceGrotesk.ttf');

const hex = h => h.replace('#', '');
function rgba(hexColor, alpha) {
  const h = hex(hexColor);
  return `rgba(${parseInt(h.slice(0,2),16)},${parseInt(h.slice(2,4),16)},${parseInt(h.slice(4,6),16)},${alpha == null ? 1 : alpha})`;
}

/* ---------------- PPTX ---------------- */
function buildPptx() {
  const pptx = new PptxGenJS();
  pptx.defineLayout({ name: 'HMX', width: SW, height: SH });
  pptx.layout = 'HMX';
  pptx.author = 'HMXHARNESS';
  pptx.subject = 'Project Expo 2026 Presentation';
  pptx.title = 'HMXHARNESS — CODE BEYOND LIMITS';

  slides.forEach((slide, si) => {
    const s = pptx.addSlide();
    s.background = { color: hex(C.bg) };
    slide.ops.forEach(o => {
      switch (o.t) {
        case 'rect': {
          const shape = o.radius ? pptx.ShapeType.roundRect : pptx.ShapeType.rect;
          const cfg = { x: o.x, y: o.y, w: o.w, h: o.h,
            fill: { color: hex(o.fill || C.navy), transparency: Math.round((1 - (o.alpha == null ? 1 : o.alpha)) * 100) } };
          if (o.stroke) cfg.line = { color: hex(o.stroke), width: o.sw || 1,
            transparency: Math.round((1 - (o.strokeAlpha == null ? 1 : o.strokeAlpha)) * 100) };
          s.addShape(shape, cfg);
          break;
        }
        case 'text': {
          const lh = o.size * o.spacing;
          const h = o.lines.length * lh / 72;
          s.addText(o.lines.join('\n'), {
            x: o.x, y: o.y, w: o.w, h: Math.max(h, o.size / 72),
            fontFace: o.family === 'Space' ? 'Space Grotesk' : 'Inter',
            fontSize: o.size, color: hex(o.color), align: o.align || 'left',
            valign: 'top', margin: 0, breakLine: false, shrinkText: false,
            lineSpacingMultiple: o.spacing, charSpacing: o.tracking || 0,
            bold: false, isTextBox: true,
          });
          break;
        }
        case 'line': {
          const x = Math.min(o.x1, o.x2), y = Math.min(o.y1, o.y2);
          s.addShape(pptx.ShapeType.line, {
            x, y, w: Math.abs(o.x2 - o.x1), h: Math.abs(o.y2 - o.y1),
            flipH: o.x2 < o.x1, flipV: o.y2 < o.y1,
            line: { color: hex(o.color || C.line), width: o.width || 1,
              transparency: Math.round((1 - (o.alpha == null ? 1 : o.alpha)) * 100) },
          });
          break;
        }
        case 'circle': {
          s.addShape(pptx.ShapeType.ellipse, {
            x: o.x - o.r, y: o.y - o.r, w: o.r * 2, h: o.r * 2,
            fill: { color: hex(o.fill || C.purple), transparency: Math.round((1 - (o.alpha == null ? 1 : o.alpha)) * 100) },
            line: o.stroke ? { color: hex(o.stroke), width: 1 } : undefined,
          });
          break;
        }
        case 'image': {
          const img = { path: o.path, x: o.x, y: o.y, w: o.w, h: o.h, sizing: 'crop' };
          s.addImage(img);
          break;
        }
      }
    });
    s.addNotes(slide.notes);
  });
  return pptx.writeFile({ fileName: path.join(OUT, 'HMXHARNESS_PROJECT_EXPO.pptx') });
}

/* ---------------- PDF ---------------- */
function buildPdf() {
  return new Promise((resolve, reject) => {
    const doc = new PDFDocument({ size: [SW * 72, SH * 72], margin: 0, info: {
      Title: 'HMXHARNESS — CODE BEYOND LIMITS', Author: 'HMXHARNESS', Subject: 'Project Expo 2026' } });
    const stream = fs.createWriteStream(path.join(OUT, 'HMXHARNESS_PROJECT_EXPO.pdf'));
    doc.pipe(stream);
    doc.registerFont('Inter', INTER);
    doc.registerFont('Space', SPACE);
    slides.forEach((slide, idx) => {
      if (idx > 0) doc.addPage();
      doc.save().rect(0, 0, SW*72, SH*72).fillColor(hex(C.bg)).fill().restore();
      slide.ops.forEach(o => {
        const u = 72;
        switch (o.t) {
          case 'rect': {
            doc.save();
            const r = (o.radius || 0) * u;
            const pathFn = r ? doc.roundedRect(o.x*u, o.y*u, o.w*u, o.h*u, r)
                              : doc.rect(o.x*u, o.y*u, o.w*u, o.h*u);
            pathFn.fillColor(hex(o.fill || C.navy)).fillOpacity(o.alpha == null ? 1 : o.alpha).fill();
            if (o.stroke) pathFn.strokeColor(hex(o.stroke)).lineWidth(o.sw || 1)
              .strokeOpacity(o.strokeAlpha == null ? 1 : o.strokeAlpha).stroke();
            doc.restore();
            break;
          }
          case 'text': {
            doc.save().font(o.family === 'Space' ? 'Space' : 'Inter')
              .fontSize(o.size).fillColor(hex(o.color));
            o.lines.forEach((ln, i) => {
              doc.text(ln, o.x*u, (o.y + i * o.size * o.spacing / 72) * u,
                { width: o.w*u, align: o.align || 'left', lineBreak: false, continued: false });
            });
            doc.restore();
            break;
          }
          case 'line':
            doc.save().moveTo(o.x1*u, o.y1*u).lineTo(o.x2*u, o.y2*u)
              .lineWidth(o.width || 1).strokeColor(hex(o.color || C.line))
              .strokeOpacity(o.alpha == null ? 1 : o.alpha).stroke().restore();
            break;
          case 'circle':
            doc.save().circle(o.x*u, o.y*u, o.r*u)
              .fillColor(hex(o.fill || C.purple)).fillOpacity(o.alpha == null ? 1 : o.alpha).fill().restore();
            break;
          case 'image': {
            doc.save();
            if (o.radius) doc.roundedRect(o.x*u, o.y*u, o.w*u, o.h*u, o.radius*u).clip();
            doc.image(o.path, o.x*u, o.y*u, { cover: [o.w*u, o.h*u] });
            doc.restore();
            break;
          }
        }
      });
    });
    doc.end();
    stream.on('finish', resolve);
    stream.on('error', reject);
  });
}

/* ---------------- PNG previews ---------------- */
async function buildPreviews() {
  const SCALE = 120; // px per inch => 1600x900
  GlobalFonts.registerFromPath(INTER, 'Inter');
  GlobalFonts.registerFromPath(SPACE, 'Space');
  const cache = new Map();
  for (let i = 0; i < slides.length; i++) {
    const canvas = createCanvas(SW * SCALE, SH * SCALE);
    const ctx = canvas.getContext('2d');
    ctx.fillStyle = C.bg; ctx.fillRect(0, 0, canvas.width, canvas.height);
    for (const o of slides[i].ops) {
      const px = v => v * SCALE;
      if (o.t === 'rect') {
        ctx.save();
        ctx.beginPath();
        ctx.roundRect(px(o.x), px(o.y), px(o.w), px(o.h), px(o.radius || 0));
        ctx.globalAlpha = o.alpha == null ? 1 : o.alpha;
        ctx.fillStyle = o.fill || C.navy; ctx.fill();
        if (o.stroke) {
          ctx.globalAlpha = o.strokeAlpha == null ? 1 : o.strokeAlpha;
          ctx.lineWidth = (o.sw || 1) * SCALE / 72;
          ctx.strokeStyle = o.stroke; ctx.stroke();
        }
        ctx.restore();
      } else if (o.t === 'text') {
        ctx.save();
        ctx.fillStyle = o.color;
        ctx.font = `${o.size * SCALE / 72}px ${o.family}`;
        ctx.textBaseline = 'top';
        ctx.textAlign = o.align === 'center' ? 'center' : o.align === 'right' ? 'right' : 'left';
        const tx = o.align === 'center' ? px(o.x + o.w/2) : o.align === 'right' ? px(o.x + o.w) : px(o.x);
        o.lines.forEach((ln, k) => ctx.fillText(ln, tx, px(o.y + k * o.size * o.spacing / 72)));
        ctx.restore();
      } else if (o.t === 'line') {
        ctx.save(); ctx.globalAlpha = o.alpha == null ? 1 : o.alpha;
        ctx.strokeStyle = o.color || C.line; ctx.lineWidth = (o.width || 1) * SCALE / 72;
        ctx.beginPath(); ctx.moveTo(px(o.x1), px(o.y1)); ctx.lineTo(px(o.x2), px(o.y2)); ctx.stroke(); ctx.restore();
      } else if (o.t === 'circle') {
        ctx.save(); ctx.globalAlpha = o.alpha == null ? 1 : o.alpha;
        ctx.fillStyle = o.fill || C.purple; ctx.beginPath();
        ctx.arc(px(o.x), px(o.y), px(o.r), 0, Math.PI*2); ctx.fill(); ctx.restore();
      } else if (o.t === 'image') {
        let img = cache.get(o.path);
        if (!img) { img = await loadImage(o.path); cache.set(o.path, img); }
        ctx.save();
        ctx.beginPath();
        ctx.roundRect(px(o.x), px(o.y), px(o.w), px(o.h), px(o.radius || 0)); ctx.clip();
        const ar = img.width / img.height, br = o.w / o.h;
        let sw = img.width, sh = img.height, sx = 0, sy = 0;
        if (ar > br) { sw = img.height * br; sx = (img.width - sw) / 2; }
        else { sh = img.width / br; sy = (img.height - sh) / 2; }
        ctx.globalAlpha = o.alpha == null ? 1 : o.alpha;
        ctx.drawImage(img, sx, sy, sw, sh, px(o.x), px(o.y), px(o.w), px(o.h));
        ctx.restore();
      }
    }
    fs.writeFileSync(path.join(PREVIEW, `slide-${String(i+1).padStart(2,'0')}.png`), canvas.toBuffer('image/png'));
  }
}

(async () => {
  await buildPptx(); console.log('PPTX written');
  await buildPdf(); console.log('PDF written');
  await buildPreviews(); console.log('Previews written:', slides.length);
})().catch(e => { console.error(e); process.exit(1); });
