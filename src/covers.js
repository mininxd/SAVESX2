/**
 * SAVESX2 Cover Studio & Social Media Generator
 * High-resolution canvas rendering engine (1080p minimum up to 4K)
 * Safe-zone aware layouts for Facebook mobile profile covers, Twitter, YouTube & GitHub
 */

import './style.css';
import './covers.css';

/* ============================================================
   TEMPLATES DEFINITIONS
   ============================================================ */
export const TEMPLATES = [
  {
    id: 'cobalt-safezone',
    name: 'Cobalt SafeZone (Exact Match)',
    category: 'profile',
    badge: 'EXACT 1000399422h',
    desc: 'Exact recreation of 1000399422h.png. Centered cobalt card with avatar-safe top placement for mobile Facebook profiles.',
    bestFor: 'Facebook Mobile Profile, Instagram Story',
    defaultColor: '#00439c',
  },
  {
    id: 'cobalt-full',
    name: 'Cobalt Edge Full-Bleed',
    category: 'banner',
    badge: 'MODERN BANNER',
    desc: 'Edge-to-edge cobalt blue with PlayStation controller glyphs (✕ ○ △ ▢) and tech chip clusters.',
    bestFor: 'Facebook Page Cover, Twitter / X Header',
    defaultColor: '#00439c',
  },
  {
    id: 'bios-matrix',
    name: 'PS2 BIOS Crystal Towers',
    category: 'retro',
    badge: 'RETRO HERITAGE',
    desc: 'Dark OLED space background with the iconic 7 crystal towers from the PS2 boot sequence and neon glow.',
    bestFor: 'YouTube Banner, Desktop Wallpaper',
    defaultColor: '#00b4d8',
  },
  {
    id: 'vintage-card',
    name: 'Vintage 8MB MagicGate Card',
    category: 'retro',
    badge: 'HARDWARE REPLICA',
    desc: 'Styled directly after the physical PS2 8MB Memory Card with textured grip ridges, red LED and SCPH-10020 badge.',
    bestFor: 'Twitter Header, GitHub Social Preview',
    defaultColor: '#00439c',
  },
  {
    id: 'stealth-oled',
    name: 'Stealth Dev Social Preview',
    category: 'dev',
    badge: 'CLEAN DEVELOPER',
    desc: 'Deep obsidian theme with titanium borders, feature chips (PSU, MAX, CBS, XPS) and high-contrast typography.',
    bestFor: 'GitHub Social Preview, Discord Header',
    defaultColor: '#00b4d8',
  },
  {
    id: 'emerald-sector',
    name: 'Emerald Memory Sector',
    category: 'dev',
    badge: 'HEX & CLUSTERS',
    desc: 'Material 3 secondary green theme with PS2 memory sector allocation maps, SuperBlock hex telemetry and circuit traces.',
    bestFor: 'Technical Blogs, Twitter Headers',
    defaultColor: '#06d6a0',
  },
  {
    id: 'solar-amber',
    name: 'Solar Amber Diagnostic',
    category: 'dev',
    badge: 'INDUSTRIAL TELEMETRY',
    desc: 'High-visibility industrial amber diagnostics styling with memory card benchmark readouts and warning accents.',
    bestFor: 'Changelogs, Release Announcements',
    defaultColor: '#ffb703',
  },
  {
    id: 'midnight-glow',
    name: 'Midnight Cyber Glow',
    category: 'banner',
    badge: 'NEON CYBER',
    desc: 'Deep midnight blue with glowing controller glyphs (✕ ○ △ ▢) and frosted tech glass card.',
    bestFor: 'Twitter / X Header, YouTube Banner',
    defaultColor: '#00b4d8',
  },
  {
    id: 'ps2-ocean',
    name: 'PS2 Deep Ocean Boot',
    category: 'retro',
    badge: 'COSMIC MEMORY',
    desc: 'Deep blue cosmic ocean with orbiting colored memory cubes inspired by the legendary PS2 startup sequence.',
    bestFor: 'Desktop Cover, YouTube Banner',
    defaultColor: '#00439c',
  },
  {
    id: 'hex-editor',
    name: 'Raw SuperBlock Hex Dump',
    category: 'dev',
    badge: 'REVERSE ENGINEERING',
    desc: 'CRT phosphor green and cyan terminal with authentic PlayStation 2 memory card superblock byte hex dump.',
    bestFor: 'GitHub Social Preview, Developer Cover',
    defaultColor: '#00ff66',
  },
  {
    id: 'box-art',
    name: 'Classic PS2 DVD Slipcover',
    category: 'retro',
    badge: 'PHYSICAL MEDIA',
    desc: 'Styled after vintage PlayStation 2 game DVD case artwork with silver PlayStation 2 header band and SLUS serial.',
    bestFor: 'Facebook Profile, Twitter Header',
    defaultColor: '#00439c',
  },
  {
    id: 'synthwave-neon',
    name: 'Synthwave 80s Grid',
    category: 'banner',
    badge: 'OUTRUN RETRO',
    desc: 'Vibrant 80s synthwave perspective grid receding to a magenta sunset with glowing chrome typography.',
    bestFor: 'Social Media Banners, Wallpapers',
    defaultColor: '#ff007f',
  },
  {
    id: 'pocket-lcd',
    name: 'PS2 Memory Sector Inspector',
    category: 'dev',
    badge: 'SECTOR TELEMETRY',
    desc: 'Authentic PS2 memory card telemetry readout showing cluster allocation, superblock FAT mapping and 8MB-128MB capacity status.',
    bestFor: 'Developer Previews, Technical Changelogs',
    defaultColor: '#ffb703',
  },
  {
    id: 'crimson-chaos',
    name: 'Crimson Chaos Edition',
    category: 'profile',
    badge: 'HIGH OCTANE',
    desc: 'Aggressive carbon black and blood-red livery with racing speed cuts, high contrast and format pills.',
    bestFor: 'Gaming Channels, Facebook Profile',
    defaultColor: '#e63946',
  },
];

/* ============================================================
   STATE
   ============================================================ */
const state = {
  templateId: 'cobalt-safezone',
  width: 1500,
  height: 1240,
  scale: 1, // 1x = 1080p minimum, 2x = 2K crisp, 3x = 4K master
  platformName: 'Facebook Mobile Cover',
  badgeText: 'LATEST STABLE',
  titleText: 'SAVESX2',
  versionText: 'v1.7.2',
  platformText: 'ANDROID APK • MINSDK',
  minSdkText: '26+',
  dateText: 'RELEASED 18 SEPT 2026',
  repoText: 'git:mininxd/SAVESX2',
  color: '#00439c',
  userSelectedColor: null, // Tracks active color theme across all templates
  activeFilter: 'all',
};

/* ============================================================
   IMAGE ASSET LOADER (HIGH RES ICON)
   ============================================================ */
let iconImage = null;
let iconLoaded = false;

function loadIconImage() {
  return new Promise((resolve) => {
    if (iconImage && iconLoaded) return resolve(iconImage);
    const img = new Image();
    img.crossOrigin = 'anonymous';
    // Test multiple potential relative paths in dev / build
    const paths = ['../icon.png', '/icon.png', './icon.png', 'icon.png'];
    let idx = 0;

    function tryNext() {
      if (idx >= paths.length) {
        console.warn('Could not load icon.png, continuing with fallback');
        resolve(null);
        return;
      }
      img.src = paths[idx++];
    }

    img.onload = () => {
      iconImage = img;
      iconLoaded = true;
      resolve(img);
    };
    img.onerror = () => tryNext();
    tryNext();
  });
}

/* ============================================================
   CANVAS RENDERING ENGINE (HIGH DPI / 1080P MINIMUM)
   ============================================================ */

/**
 * Ensures minimum 1080p export:
 * If the user's selected dimensions are smaller than 1080p,
 * scale factor is boosted so minimum dimension or major dimension is at least 1080p.
 */
function getEffectiveScale(baseScale, w, h) {
  const minDim = Math.min(w, h);
  let multiplier = baseScale;
  if (minDim * multiplier < 600 && Math.max(w, h) * multiplier < 1080) {
    multiplier = Math.max(multiplier, 1080 / Math.max(w, h));
  }
  return multiplier;
}

/**
 * Draw pill badge helper
 */
function drawPill(ctx, x, y, width, height, radius, bgColor) {
  ctx.save();
  ctx.beginPath();
  ctx.fillStyle = bgColor;
  ctx.roundRect(x, y, width, height, radius);
  ctx.fill();
  ctx.restore();
}

/**
 * Hex color parsing and palette generator
 * Ensures all 14 templates dynamically adapt to any selected color accent
 */
function normalizeHexColor(c) {
  if (!c || typeof c !== 'string') return '#00439c';
  let str = c.trim().toLowerCase();
  if (!str.startsWith('#')) str = '#' + str;
  if (str.length === 4) {
    return `#${str[1]}${str[1]}${str[2]}${str[2]}${str[3]}${str[3]}`;
  }
  if (str.length === 7 && /^#[0-9a-f]{6}$/.test(str)) {
    return str;
  }
  return '#00439c';
}

function parseHexToRgb(hex) {
  let c = (hex || '#00439c').replace('#', '').trim();
  if (c.length === 3) {
    c = c[0] + c[0] + c[1] + c[1] + c[2] + c[2];
  }
  if (c.length !== 6 || !/^[0-9a-fA-F]{6}$/.test(c)) {
    c = '00439c';
  }
  const num = parseInt(c, 16);
  return {
    r: (num >> 16) & 255,
    g: (num >> 8) & 255,
    b: num & 255,
  };
}

function hexToRgba(hex, alpha) {
  const { r, g, b } = parseHexToRgb(hex);
  return `rgba(${r}, ${g}, ${b}, ${alpha})`;
}

function adjustHex(hex, amount) {
  const { r, g, b } = parseHexToRgb(hex);
  const target = amount > 0 ? 255 : 0;
  const t = Math.abs(amount);
  const newR = Math.min(255, Math.max(0, Math.round(r + (target - r) * t)));
  const newG = Math.min(255, Math.max(0, Math.round(g + (target - g) * t)));
  const newB = Math.min(255, Math.max(0, Math.round(b + (target - b) * t)));
  return `#${((1 << 24) + (newR << 16) + (newG << 8) + newB).toString(16).slice(1)}`;
}

function isLightColor(hex) {
  const { r, g, b } = parseHexToRgb(hex);
  // ITU-R BT.709 perceived luminance
  return (0.2126 * r + 0.7152 * g + 0.0722 * b) > 165;
}

function getColorPalette(baseColor, fallbackHex = '#00439c') {
  const primary = normalizeHexColor(baseColor || fallbackHex);
  const isLight = isLightColor(primary);
  return {
    primary,
    isLight,
    onPrimary: isLight ? '#090d14' : '#ffffff',
    rgba: (a) => hexToRgba(primary, a),
    light: adjustHex(primary, 0.55),
    lighter: adjustHex(primary, 0.82),
    dark: adjustHex(primary, -0.45),
    darker: adjustHex(primary, -0.72),
  };
}

/**
 * Render template to canvas
 */
export async function renderCoverToCanvas(canvas, templateId, options = {}) {
  await document.fonts.ready;
  const icon = await loadIconImage();

  const data = {
    badge: options.badgeText || state.badgeText,
    title: options.titleText || state.titleText,
    version: options.versionText || state.versionText,
    platform: options.platformText || state.platformText,
    minSdk: options.minSdkText || state.minSdkText,
    date: options.dateText || state.dateText,
    repo: options.repoText || state.repoText,
    color: options.color || state.color,
  };

  const baseW = options.width || state.width;
  const baseH = options.height || state.height;
  const scale = options.scale || 1;

  canvas.width = Math.round(baseW * scale);
  canvas.height = Math.round(baseH * scale);

  const ctx = canvas.getContext('2d', { alpha: false });
  ctx.imageSmoothingEnabled = true;
  ctx.imageSmoothingQuality = 'high';

  // Scale context so drawing uses baseW x baseH coordinate space
  ctx.save();
  ctx.scale(scale, scale);

  switch (templateId) {
    case 'cobalt-safezone':
      drawCobaltSafeZone(ctx, baseW, baseH, data, icon);
      break;
    case 'cobalt-full':
      drawCobaltFull(ctx, baseW, baseH, data, icon);
      break;
    case 'bios-matrix':
      drawBiosMatrix(ctx, baseW, baseH, data, icon);
      break;
    case 'vintage-card':
      drawVintageCard(ctx, baseW, baseH, data, icon);
      break;
    case 'stealth-oled':
      drawStealthOled(ctx, baseW, baseH, data, icon);
      break;
    case 'emerald-sector':
      drawEmeraldSector(ctx, baseW, baseH, data, icon);
      break;
    case 'solar-amber':
      drawSolarAmber(ctx, baseW, baseH, data, icon);
      break;
    case 'midnight-glow':
      drawMidnightGlow(ctx, baseW, baseH, data, icon);
      break;
    case 'ps2-ocean':
      drawPs2Ocean(ctx, baseW, baseH, data, icon);
      break;
    case 'hex-editor':
      drawHexEditor(ctx, baseW, baseH, data, icon);
      break;
    case 'box-art':
      drawBoxArt(ctx, baseW, baseH, data, icon);
      break;
    case 'synthwave-neon':
      drawSynthwaveNeon(ctx, baseW, baseH, data, icon);
      break;
    case 'pocket-lcd':
      drawPocketLcd(ctx, baseW, baseH, data, icon);
      break;
    case 'crimson-chaos':
      drawCrimsonChaos(ctx, baseW, baseH, data, icon);
      break;
    default:
      drawCobaltSafeZone(ctx, baseW, baseH, data, icon);
      break;
  }

  ctx.restore();
  return canvas;
}

/* ------------------------------------------------------------
   TEMPLATE 1: Cobalt SafeZone (Exact match to 1000399422h.png)
   ------------------------------------------------------------ */
function drawCobaltSafeZone(ctx, w, h, data, icon) {
  const pal = getColorPalette(data.color, '#00439c');

  // Outer canvas dark background: #0f141c
  ctx.fillStyle = '#0f141c';
  ctx.fillRect(0, 0, w, h);

  // Subtle dot grid on outer canvas
  drawSubtleDotGrid(ctx, 0, 0, w, h, 'rgba(255, 255, 255, 0.035)', 24);

  // In the original 1500x1240 reference image:
  // Card width = 954px, centered horizontally -> left = (1500 - 954)/2 = 273px
  // Top starts at y = 38px, extends to h
  // Radius top = 80px
  const isExactRatio = Math.abs(w / h - 1500 / 1240) < 0.15;
  const cardW = isExactRatio ? Math.round(w * (954 / 1500)) : Math.round(w * 0.75);
  const cardLeft = Math.round((w - cardW) / 2);
  const cardTop = Math.round(h * (38 / 1240));
  const cardH = h - cardTop;
  const radius = Math.round(cardW * 0.084); // ~80px for 954px width

  // Draw Main Accent Card
  ctx.save();
  ctx.beginPath();
  ctx.fillStyle = pal.primary;
  ctx.roundRect(cardLeft, cardTop, cardW, cardH + 100, [radius, radius, 0, 0]);
  ctx.fill();

  // Subtle inner highlight
  ctx.strokeStyle = pal.isLight ? 'rgba(0, 0, 0, 0.12)' : 'rgba(255, 255, 255, 0.14)';
  ctx.lineWidth = 2;
  ctx.stroke();
  ctx.restore();

  // Reference coordinates normalized to 1500x1240
  const factor = w / 1500;

  // 1. Icon: exact placement matching 1000399422h.png, perfectly square 1:1 aspect ratio
  const iconSize = Math.round(192 * factor);
  const iconX = cardLeft + Math.round((357 - 273) * factor);
  const iconY = cardTop + Math.round((121 - 38) * factor);

  if (icon) {
    ctx.drawImage(icon, iconX, iconY, iconSize, iconSize);
  } else {
    drawFallbackCartridgeIcon(ctx, iconX, iconY, iconSize, iconSize);
  }

  // 2. Channel/Top Badge (e.g. "LATEST STABLE"): x=590, y=165 in 1500x1240
  const titleLeft = cardLeft + Math.round((590 - 273) * factor);
  const tagY = cardTop + Math.round((168 - 38) * factor);

  ctx.save();
  ctx.font = `600 ${Math.round(28 * factor)}px "IBM Plex Mono", monospace`;
  ctx.fillStyle = pal.isLight ? pal.darker : pal.light;
  ctx.letterSpacing = `${Math.round(5 * factor)}px`;
  ctx.fillText(data.badge.toUpperCase(), titleLeft, tagY);

  // 3. Title & Version: x=590, y=250 in 1500x1240
  const titleY = cardTop + Math.round((250 - 38) * factor);
  ctx.font = `700 ${Math.round(58 * factor)}px "Chakra Petch", sans-serif`;
  ctx.fillStyle = pal.onPrimary;
  ctx.letterSpacing = `${Math.round(1 * factor)}px`;
  const fullTitle = `${data.title} ${data.version}`;
  ctx.fillText(fullTitle, titleLeft, titleY);
  ctx.restore();

  // 4. Specs Line 1 & Line 2
  const specsY1 = cardTop + Math.round((398 - 38) * factor);
  const specsX = cardLeft + Math.round((370 - 273) * factor);

  ctx.save();
  ctx.font = `500 ${Math.round(36 * factor)}px "IBM Plex Mono", monospace`;
  ctx.fillStyle = pal.onPrimary;
  ctx.letterSpacing = `${Math.round(4 * factor)}px`;

  // Draw "ANDROID APK • "
  const prefix1 = `${data.platform.replace(/MINSDK.*/i, '').trim()} • `;
  ctx.fillText(prefix1, specsX, specsY1);
  const prefix1W = ctx.measureText(prefix1).width;

  const pillBg = pal.isLight ? 'rgba(0, 0, 0, 0.18)' : pal.dark;
  const pillFg = pal.isLight ? '#0a1017' : '#ffffff';

  // Draw "MINSDK" badge pill
  const badge1X = specsX + prefix1W;
  const badge1W = Math.round(210 * factor);
  const badge1H = Math.round(48 * factor);
  const badgeRadius = Math.round(12 * factor);
  drawPill(ctx, badge1X, specsY1 - badge1H + Math.round(10 * factor), badge1W, badge1H, badgeRadius, pillBg);

  ctx.fillStyle = pillFg;
  ctx.textAlign = 'center';
  ctx.fillText('MINSDK', badge1X + badge1W / 2, specsY1);

  // Line 2: [26+] (badge) • RELEASED 18 SEPT 2026
  const specsY2 = cardTop + Math.round((472 - 38) * factor);
  ctx.textAlign = 'left';

  // Badge for SDK number (dynamically sized for 26+)
  const sdkTextW = ctx.measureText(data.minSdk).width;
  const badge2W = Math.max(Math.round(116 * factor), Math.round(sdkTextW + 34 * factor));
  const badge2H = Math.round(48 * factor);
  drawPill(ctx, specsX, specsY2 - badge2H + Math.round(10 * factor), badge2W, badge2H, badgeRadius, pillBg);

  ctx.fillStyle = pillFg;
  ctx.textAlign = 'center';
  ctx.fillText(data.minSdk, specsX + badge2W / 2, specsY2);

  // " • RELEASED 18 SEPT 2026"
  ctx.fillStyle = pal.onPrimary;
  ctx.textAlign = 'left';
  const restLine2 = ` • ${data.date.toUpperCase()}`;
  ctx.fillText(restLine2, specsX + badge2W + Math.round(14 * factor), specsY2);
  ctx.restore();

  // 5. Bottom Git Repo: x=center, y=1215 in 1500x1240
  const botY = h - Math.round(25 * factor);
  ctx.save();
  ctx.font = `500 ${Math.round(22 * factor)}px "IBM Plex Mono", monospace`;
  ctx.fillStyle = pal.light;
  ctx.textAlign = 'center';
  ctx.letterSpacing = `${Math.round(1 * factor)}px`;
  ctx.fillText(data.repo, w / 2, botY);
  ctx.restore();
}

/* ------------------------------------------------------------
   TEMPLATE 2: Cobalt Full-Bleed Modern
   ------------------------------------------------------------ */
function drawCobaltFull(ctx, w, h, data, icon) {
  const pal = getColorPalette(data.color, '#00439c');

  // Background in accent color
  ctx.fillStyle = pal.primary;
  ctx.fillRect(0, 0, w, h);

  // Dot matrix
  drawSubtleDotGrid(ctx, 0, 0, w, h, 'rgba(255, 255, 255, 0.08)', 30);

  // Dark header and footer framing
  ctx.fillStyle = 'rgba(11, 16, 23, 0.65)';
  ctx.fillRect(0, 0, w, Math.round(h * 0.12));
  ctx.fillRect(0, h - Math.round(h * 0.12), w, Math.round(h * 0.12));

  // Top header text
  ctx.save();
  ctx.font = `600 ${Math.round(20 * (w / 1500))}px "IBM Plex Mono", monospace`;
  ctx.fillStyle = pal.isLight ? pal.darker : pal.lighter;
  ctx.fillText('PLAYSTATION 2 MEMORY CARD ARCHITECTURE // ANDROID', Math.round(60 * (w / 1500)), Math.round(55 * (h / 800)));
  ctx.textAlign = 'right';
  ctx.fillText(data.badge, w - Math.round(60 * (w / 1500)), Math.round(55 * (h / 800)));
  ctx.restore();

  // Watermark PS2 Controller Glyphs
  ctx.save();
  ctx.font = `700 ${Math.round(160 * (w / 1500))}px sans-serif`;
  ctx.fillStyle = pal.isLight ? 'rgba(0, 0, 0, 0.05)' : 'rgba(255, 255, 255, 0.035)';
  ctx.textAlign = 'right';
  ctx.fillText('✕ ○ △ ▢', w - Math.round(80 * (w / 1500)), h / 2 + Math.round(60 * (h / 800)));
  ctx.restore();

  // Main Content Left
  const contentX = Math.round(80 * (w / 1500));
  const contentY = Math.round(h * 0.38);

  if (icon) {
    const iconSize = Math.round(170 * (w / 1500));
    ctx.drawImage(icon, contentX, contentY - Math.round(iconSize * 0.55), iconSize, iconSize);
  }

  ctx.save();
  const textLeft = contentX + Math.round(220 * (w / 1500));
  ctx.font = `700 ${Math.round(72 * (w / 1500))}px "Chakra Petch", sans-serif`;
  ctx.fillStyle = pal.onPrimary;
  ctx.fillText(`${data.title} ${data.version}`, textLeft, contentY - 10);

  ctx.font = `500 ${Math.round(30 * (w / 1500))}px "IBM Plex Mono", monospace`;
  ctx.fillStyle = pal.isLight ? pal.darker : pal.lighter;
  ctx.fillText(`${data.platform} ${data.minSdk} • ${data.date}`, textLeft, contentY + Math.round(50 * (w / 1500)));

  // Chips row with dynamic accent tone
  const chipY = contentY + Math.round(110 * (w / 1500));
  const chips = ['GPL-3.0', '8MB / 16MB / 32MB / 64MB / 128MB', 'PSU / MAX / CBS', '3D ICON VIEWER'];
  let curX = textLeft;
  const chipBg = pal.isLight ? 'rgba(0, 0, 0, 0.16)' : pal.dark;
  const chipFg = pal.onPrimary;

  chips.forEach((c) => {
    ctx.font = `600 ${Math.round(18 * (w / 1500))}px "IBM Plex Mono", monospace`;
    const cw = ctx.measureText(c).width + 24;
    drawPill(ctx, curX, chipY - 26, cw, 36, 8, chipBg);
    ctx.fillStyle = chipFg;
    ctx.fillText(c, curX + 12, chipY - 2);
    curX += cw + 14;
  });
  ctx.restore();

  // Footer bar text
  ctx.save();
  ctx.font = `500 ${Math.round(20 * (w / 1500))}px "IBM Plex Mono", monospace`;
  ctx.fillStyle = pal.isLight ? pal.darker : pal.lighter;
  ctx.fillText(data.repo, Math.round(60 * (w / 1500)), h - Math.round(40 * (h / 800)));
  ctx.textAlign = 'right';
  ctx.fillText('FREE & OPEN SOURCE PS2 CARD MANAGER FOR ANDROID', w - Math.round(60 * (w / 1500)), h - Math.round(40 * (h / 800)));
  ctx.restore();
}

/* ------------------------------------------------------------
   TEMPLATE 3: PS2 BIOS Crystal Towers
   ------------------------------------------------------------ */
function drawBiosMatrix(ctx, w, h, data, icon) {
  const pal = getColorPalette(data.color, '#00b4d8');

  // Dark OLED Space Background
  ctx.fillStyle = '#070a0f';
  ctx.fillRect(0, 0, w, h);

  // Background gradient glow
  const grad = ctx.createRadialGradient(w * 0.7, h * 0.4, 10, w * 0.7, h * 0.4, w * 0.8);
  grad.addColorStop(0, pal.rgba(0.2));
  grad.addColorStop(0.5, pal.rgba(0.08));
  grad.addColorStop(1, 'rgba(7, 10, 15, 0)');
  ctx.fillStyle = grad;
  ctx.fillRect(0, 0, w, h);

  // Draw 7 stylized 3D PS2 Crystal Towers on right side
  const towers = [
    { x: 0.58, h: 0.65, w: 32 },
    { x: 0.64, h: 0.82, w: 36 },
    { x: 0.71, h: 0.48, w: 28 },
    { x: 0.77, h: 0.90, w: 42 },
    { x: 0.84, h: 0.70, w: 34 },
    { x: 0.90, h: 0.55, w: 30 },
    { x: 0.95, h: 0.75, w: 32 },
  ];

  ctx.save();
  towers.forEach((t) => {
    const tx = w * t.x;
    const th = h * t.h;
    const ty = h - th;
    const tw = t.w * (w / 1500);

    // Front face
    ctx.fillStyle = pal.rgba(0.2);
    ctx.fillRect(tx, ty, tw, th);

    // Right illuminated edge
    ctx.fillStyle = pal.rgba(0.5);
    ctx.fillRect(tx + tw - 3, ty, 3, th);

    // Top cap
    ctx.fillStyle = pal.light;
    ctx.fillRect(tx, ty, tw, 4);
  });
  ctx.restore();

  // CRT Scanlines
  drawScanlines(ctx, w, h, 'rgba(0, 0, 0, 0.25)', 4);

  // Left Content
  const f = w / 1500;
  const leftX = Math.round(90 * f);
  const topY = Math.round(h * 0.32);

  if (icon) {
    const iconSize = Math.round(150 * f);
    ctx.drawImage(icon, leftX, topY - Math.round(iconSize * 0.75), iconSize, iconSize);
  }

  ctx.save();
  ctx.font = `600 ${Math.round(24 * f)}px "IBM Plex Mono", monospace`;
  ctx.fillStyle = pal.primary;
  ctx.letterSpacing = `${Math.round(4 * f)}px`;
  ctx.fillText('PS2 BIOS CARD BROWSER & TELEMETRY // ANDROID', leftX, topY + Math.round(30 * f));

  ctx.font = `700 ${Math.round(76 * f)}px "Chakra Petch", sans-serif`;
  ctx.fillStyle = '#ffffff';
  ctx.shadowColor = pal.primary;
  ctx.shadowBlur = 18;
  ctx.fillText(`${data.title} ${data.version}`, leftX, topY + Math.round(115 * f));
  ctx.shadowBlur = 0;

  ctx.font = `500 ${Math.round(32 * f)}px "IBM Plex Mono", monospace`;
  ctx.fillStyle = pal.lighter;
  ctx.fillText(`SYSTEM: ${data.platform} ${data.minSdk} • ${data.date}`, leftX, topY + Math.round(180 * f));

  ctx.font = `400 ${Math.round(22 * f)}px "IBM Plex Mono", monospace`;
  ctx.fillStyle = 'rgba(255, 255, 255, 0.75)';
  ctx.fillText(`CLUSTER: 1024 BYTES // SUPERBLOCK: 0x00004000 // ${data.repo}`, leftX, h - Math.round(45 * f));
  ctx.restore();
}

/* ------------------------------------------------------------
   TEMPLATE 4: Vintage 8MB MagicGate Memory Card
   ------------------------------------------------------------ */
function drawVintageCard(ctx, w, h, data, icon) {
  const pal = getColorPalette(data.color, '#00439c');

  // Background desk surface
  ctx.fillStyle = '#0b0f16';
  ctx.fillRect(0, 0, w, h);

  // Memory card body
  const f = w / 1500;
  const cardW = Math.round(w * 0.88);
  const cardH = Math.round(h * 0.85);
  const cardX = Math.round((w - cardW) / 2);
  const cardY = Math.round((h - cardH) / 2);

  // Card plastic body
  ctx.save();
  ctx.fillStyle = '#181e28';
  ctx.beginPath();
  ctx.roundRect(cardX, cardY, cardW, cardH, [24 * f, 24 * f, 16 * f, 16 * f]);
  ctx.fill();
  ctx.strokeStyle = '#2e3848';
  ctx.lineWidth = 3;
  ctx.stroke();

  // Grip ridges (etched lines at top right)
  ctx.fillStyle = '#10151d';
  for (let i = 0; i < 6; i++) {
    ctx.fillRect(cardX + cardW - Math.round(120 * f), cardY + Math.round((40 + i * 22) * f), Math.round(90 * f), Math.round(10 * f));
  }

  // LED indicator light using accent color
  const ledX = cardX + Math.round(80 * f);
  const ledY = cardY + Math.round(60 * f);
  ctx.beginPath();
  ctx.arc(ledX, ledY, Math.round(10 * f), 0, Math.PI * 2);
  ctx.fillStyle = pal.primary;
  ctx.fill();
  ctx.shadowColor = pal.primary;
  ctx.shadowBlur = 12;
  ctx.fill();
  ctx.shadowBlur = 0;

  // "MagicGate" logo font
  ctx.font = `italic 700 ${Math.round(36 * f)}px serif`;
  ctx.fillStyle = '#c5ccd6';
  ctx.fillText('MagicGate™', ledX + Math.round(30 * f), ledY + Math.round(10 * f));

  // "MEMORY CARD (8MB)"
  ctx.font = `700 ${Math.round(44 * f)}px "Chakra Petch", sans-serif`;
  ctx.fillStyle = '#e8ecf2';
  ctx.fillText('MEMORY CARD (8MB/EXTENDED)', cardX + Math.round(80 * f), cardY + Math.round(160 * f));

  // Center inset label area in accent color
  const labelX = cardX + Math.round(60 * f);
  const labelY = cardY + Math.round(200 * f);
  const labelW = cardW - Math.round(120 * f);
  const labelH = cardH - Math.round(280 * f);

  ctx.fillStyle = pal.primary;
  ctx.beginPath();
  ctx.roundRect(labelX, labelY, labelW, labelH, 14 * f);
  ctx.fill();

  if (icon) {
    const iconSize = Math.round(125 * f);
    ctx.drawImage(icon, labelX + Math.round(40 * f), labelY + Math.round(25 * f), iconSize, iconSize);
  }

  const textX = labelX + Math.round(210 * f);
  ctx.font = `700 ${Math.round(56 * f)}px "Chakra Petch", sans-serif`;
  ctx.fillStyle = pal.onPrimary;
  ctx.fillText(`${data.title} ${data.version}`, textX, labelY + Math.round(90 * f));

  ctx.font = `500 ${Math.round(26 * f)}px "IBM Plex Mono", monospace`;
  ctx.fillStyle = pal.isLight ? pal.darker : pal.lighter;
  ctx.fillText(`${data.platform} ${data.minSdk} • ${data.date}`, textX, labelY + Math.round(140 * f));

  // Bottom card serial stamp
  ctx.font = `600 ${Math.round(20 * f)}px "IBM Plex Mono", monospace`;
  ctx.fillStyle = '#8a93a2';
  ctx.fillText(`SCPH-10020 // ${data.repo}`, cardX + Math.round(80 * f), cardY + cardH - Math.round(30 * f));
  ctx.restore();
}

/* ------------------------------------------------------------
   TEMPLATE 5: Stealth Dev Social Preview (GitHub style)
   ------------------------------------------------------------ */
function drawStealthOled(ctx, w, h, data, icon) {
  const pal = getColorPalette(data.color, '#00b4d8');

  ctx.fillStyle = '#0a0d14';
  ctx.fillRect(0, 0, w, h);

  const f = w / 1500;
  drawSubtleDotGrid(ctx, 0, 0, w, h, 'rgba(255, 255, 255, 0.04)', 28);

  // Border frame with accent
  ctx.strokeStyle = pal.rgba(0.5);
  ctx.lineWidth = 2;
  ctx.strokeRect(30 * f, 30 * f, w - 60 * f, h - 60 * f);

  // Top header status bar
  ctx.save();
  ctx.fillStyle = '#141c28';
  ctx.fillRect(30 * f, 30 * f, w - 60 * f, 60 * f);

  ctx.font = `600 ${Math.round(20 * f)}px "IBM Plex Mono", monospace`;
  ctx.fillStyle = pal.primary;
  ctx.fillText('● SAVESX2 // OPEN-SOURCE RELEASE ARCHIVE', 60 * f, 68 * f);

  ctx.textAlign = 'right';
  ctx.fillStyle = '#8892b0';
  ctx.fillText('GPL-3.0 • C++ / KOTLIN CORE', w - 60 * f, 68 * f);
  ctx.restore();

  // Center Content
  const midY = h * 0.46;
  const leftX = 90 * f;

  if (icon) {
    const iconSize = Math.round(145 * f);
    ctx.drawImage(icon, leftX, midY - Math.round(iconSize * 0.55), iconSize, iconSize);
  }

  ctx.save();
  const tx = leftX + 210 * f;
  ctx.font = `700 ${Math.round(74 * f)}px "Chakra Petch", sans-serif`;
  ctx.fillStyle = '#f8fafc';
  ctx.fillText(`${data.title} ${data.version}`, tx, midY - 10 * f);

  ctx.font = `500 ${Math.round(30 * f)}px "IBM Plex Mono", monospace`;
  ctx.fillStyle = pal.light;
  ctx.fillText(`${data.platform} ${data.minSdk} • ${data.date}`, tx, midY + 45 * f);

  // Tag badges with subtle accent outline
  const tags = ['PS2 Card Explorer', 'Save Editor', 'Icon 3D Mesh', 'Formatting & Resize', 'FreeMCBoot'];
  let tagX = tx;
  const tagY = midY + 110 * f;
  tags.forEach((t) => {
    ctx.font = `600 ${Math.round(18 * f)}px "IBM Plex Mono", monospace`;
    const tw = ctx.measureText(t).width + 24;
    drawPill(ctx, tagX, tagY - 26, tw, 36, 6, pal.rgba(0.14));
    ctx.fillStyle = pal.lighter;
    ctx.fillText(t, tagX + 12, tagY - 2);
    tagX += tw + 12;
  });

  // Bottom footer
  ctx.font = `500 ${Math.round(22 * f)}px "IBM Plex Mono", monospace`;
  ctx.fillStyle = pal.light;
  ctx.fillText(data.repo, leftX, h - 60 * f);
  ctx.restore();
}

/* ------------------------------------------------------------
   TEMPLATE 6: Emerald Memory Sector Map
   ------------------------------------------------------------ */
function drawEmeraldSector(ctx, w, h, data, icon) {
  const pal = getColorPalette(data.color, '#06d6a0');

  ctx.fillStyle = pal.darker;
  ctx.fillRect(0, 0, w, h);

  const f = w / 1500;

  // Draw matrix grid of memory clusters in accent color
  ctx.save();
  const rows = 12;
  const cols = 24;
  const startX = w * 0.45;
  const cellW = (w * 0.5) / cols;
  const cellH = (h * 0.7) / rows;

  for (let r = 0; r < rows; r++) {
    for (let c = 0; c < cols; c++) {
      const active = (r * 7 + c * 13) % 5 === 0;
      ctx.fillStyle = active ? pal.rgba(0.35) : pal.rgba(0.06);
      ctx.fillRect(startX + c * cellW, 60 * f + r * cellH, cellW - 3, cellH - 3);
    }
  }
  ctx.restore();

  const leftX = 80 * f;
  const topY = h * 0.38;

  if (icon) {
    const iconSize = Math.round(145 * f);
    ctx.drawImage(icon, leftX, topY - Math.round(iconSize * 0.65), iconSize, iconSize);
  }

  ctx.save();
  ctx.font = `600 ${Math.round(22 * f)}px "IBM Plex Mono", monospace`;
  ctx.fillStyle = pal.primary;
  ctx.letterSpacing = `${Math.round(4 * f)}px`;
  ctx.fillText('PS2 MEMORY CLUSTER ALLOCATION MAP', leftX, topY + 60 * f);

  ctx.font = `700 ${Math.round(72 * f)}px "Chakra Petch", sans-serif`;
  ctx.fillStyle = '#ffffff';
  ctx.fillText(`${data.title} ${data.version}`, leftX, topY + 140 * f);

  ctx.font = `500 ${Math.round(30 * f)}px "IBM Plex Mono", monospace`;
  ctx.fillStyle = pal.lighter;
  ctx.fillText(`${data.platform} ${data.minSdk} • ${data.date}`, leftX, topY + 200 * f);

  ctx.font = `400 ${Math.round(22 * f)}px "IBM Plex Mono", monospace`;
  ctx.fillStyle = pal.light;
  ctx.fillText(`SECTORS: 1024B CLUSTERS // ${data.repo}`, leftX, h - 45 * f);
  ctx.restore();
}

/* ------------------------------------------------------------
   TEMPLATE 7: Solar Amber Industrial Diagnostic
   ------------------------------------------------------------ */
function drawSolarAmber(ctx, w, h, data, icon) {
  const pal = getColorPalette(data.color, '#ffb703');

  ctx.fillStyle = '#14110b';
  ctx.fillRect(0, 0, w, h);

  const f = w / 1500;

  // Hazard diagonal stripe bar at top and bottom in accent color
  drawHazardStripes(ctx, 0, 0, w, 24 * f, pal.primary, '#14110b');
  drawHazardStripes(ctx, 0, h - 24 * f, w, 24 * f, pal.primary, '#14110b');

  const leftX = 80 * f;
  const topY = h * 0.4;

  if (icon) {
    const iconSize = Math.round(145 * f);
    ctx.drawImage(icon, leftX, topY - Math.round(iconSize * 0.65), iconSize, iconSize);
  }

  ctx.save();
  ctx.font = `700 ${Math.round(24 * f)}px "IBM Plex Mono", monospace`;
  ctx.fillStyle = pal.primary;
  ctx.fillText('DIAGNOSTIC & REPAIR ENGINE // TELEMETRY OK', leftX + 190 * f, topY - 40 * f);

  ctx.font = `700 ${Math.round(72 * f)}px "Chakra Petch", sans-serif`;
  ctx.fillStyle = '#ffffff';
  ctx.fillText(`${data.title} ${data.version}`, leftX + 190 * f, topY + 30 * f);

  ctx.font = `500 ${Math.round(32 * f)}px "IBM Plex Mono", monospace`;
  ctx.fillStyle = pal.light;
  ctx.fillText(`${data.platform} ${data.minSdk} • ${data.date}`, leftX, topY + 110 * f);

  ctx.font = `500 ${Math.round(22 * f)}px "IBM Plex Mono", monospace`;
  ctx.fillStyle = pal.light;
  ctx.fillText(`PS2 MEMORY CARD REPAIR & RECOVERY ENGINE // ${data.repo}`, leftX, h - 50 * f);
  ctx.restore();
}

/* ------------------------------------------------------------
   TEMPLATE 8: Midnight Cyber Glow
   ------------------------------------------------------------ */
function drawMidnightGlow(ctx, w, h, data, icon) {
  const pal = getColorPalette(data.color, '#00b4d8');
  const f = w / 1500;

  // Deep midnight navy radial background
  const bg = ctx.createRadialGradient(w * 0.5, h * 0.45, 20, w * 0.5, h * 0.45, w * 0.75);
  bg.addColorStop(0, pal.darker);
  bg.addColorStop(0.6, '#010f24');
  bg.addColorStop(1, '#000611');
  ctx.fillStyle = bg;
  ctx.fillRect(0, 0, w, h);

  drawSubtleDotGrid(ctx, 0, 0, w, h, pal.rgba(0.08), 26);

  // Controller glyphs in subtle neon
  ctx.save();
  ctx.font = `700 ${Math.round(42 * f)}px sans-serif`;
  ctx.fillStyle = pal.rgba(0.18);
  ctx.fillText('△', w * 0.88, h * 0.25);
  ctx.fillText('○', w * 0.92, h * 0.48);
  ctx.fillText('✕', w * 0.86, h * 0.72);
  ctx.fillText('▢', w * 0.80, h * 0.42);
  ctx.restore();

  // Frosted center glass card
  const cardW = Math.round(w * 0.86);
  const cardH = Math.round(h * 0.76);
  const cardX = Math.round((w - cardW) / 2);
  const cardY = Math.round((h - cardH) / 2);

  ctx.save();
  ctx.fillStyle = 'rgba(6, 18, 38, 0.78)';
  ctx.beginPath();
  ctx.roundRect(cardX, cardY, cardW, cardH, 20 * f);
  ctx.fill();
  ctx.strokeStyle = pal.rgba(0.5);
  ctx.lineWidth = 2;
  ctx.stroke();

  // Top card banner line in accent color
  ctx.fillStyle = pal.primary;
  ctx.fillRect(cardX, cardY, cardW, 4 * f);

  // Icon: strictly square 1:1 ratio
  if (icon) {
    const iconSize = Math.round(165 * f);
    ctx.drawImage(icon, cardX + 45 * f, cardY + (cardH - iconSize) / 2, iconSize, iconSize);
  }

  const tx = cardX + 235 * f;
  const ty = cardY + cardH * 0.32;

  ctx.font = `600 ${Math.round(22 * f)}px "IBM Plex Mono", monospace`;
  ctx.fillStyle = pal.light;
  ctx.letterSpacing = `${Math.round(3 * f)}px`;
  ctx.fillText(data.badge.toUpperCase(), tx, ty);

  ctx.font = `700 ${Math.round(68 * f)}px "Chakra Petch", sans-serif`;
  ctx.fillStyle = '#ffffff';
  ctx.shadowColor = pal.primary;
  ctx.shadowBlur = 14;
  ctx.fillText(`${data.title} ${data.version}`, tx, ty + 68 * f);
  ctx.shadowBlur = 0;

  ctx.font = `500 ${Math.round(28 * f)}px "IBM Plex Mono", monospace`;
  ctx.fillStyle = pal.lighter;
  ctx.fillText(`${data.platform} ${data.minSdk} • ${data.date}`, tx, ty + 125 * f);

  // Pill tags
  const tags = ['PS2 CORE', 'SAVE MANAGER', 'APK BUILD', 'GPL-3.0'];
  let curX = tx;
  const pillY = ty + 180 * f;
  tags.forEach((t) => {
    ctx.font = `600 ${Math.round(16 * f)}px "IBM Plex Mono", monospace`;
    const pw = ctx.measureText(t).width + 20 * f;
    drawPill(ctx, curX, pillY - 22 * f, pw, 30 * f, 6 * f, pal.rgba(0.25));
    ctx.fillStyle = pal.light;
    ctx.fillText(t, curX + 10 * f, pillY - 2 * f);
    curX += pw + 10 * f;
  });

  // Footer inside card
  ctx.font = `500 ${Math.round(19 * f)}px "IBM Plex Mono", monospace`;
  ctx.fillStyle = pal.rgba(0.75);
  ctx.fillText(data.repo, cardX + 45 * f, cardY + cardH - 22 * f);
  ctx.restore();
}

/* ------------------------------------------------------------
   TEMPLATE 9: PS2 Deep Ocean Boot
   ------------------------------------------------------------ */
function drawPs2Ocean(ctx, w, h, data, icon) {
  const pal = getColorPalette(data.color, '#00439c');
  const f = w / 1500;

  // Deep ocean gradient with accent base
  const bg = ctx.createLinearGradient(0, 0, 0, h);
  bg.addColorStop(0, '#00040e');
  bg.addColorStop(0.5, '#00112c');
  bg.addColorStop(1, pal.darker);
  ctx.fillStyle = bg;
  ctx.fillRect(0, 0, w, h);

  // Floating colored memory cubes
  const cubes = [
    { x: 0.12, y: 0.28, s: 28, c: pal.primary, a: 0.85 },
    { x: 0.22, y: 0.78, s: 36, c: '#00b4d8', a: 0.6 },
    { x: 0.78, y: 0.22, s: 42, c: pal.light, a: 0.8 },
    { x: 0.88, y: 0.65, s: 30, c: '#06d6a0', a: 0.75 },
    { x: 0.68, y: 0.82, s: 24, c: '#ffffff', a: 0.9 },
    { x: 0.48, y: 0.15, s: 20, c: '#7209b7', a: 0.5 },
  ];

  ctx.save();
  cubes.forEach((cb) => {
    const cx = w * cb.x;
    const cy = h * cb.y;
    const cs = cb.s * f;
    ctx.fillStyle = cb.c;
    ctx.globalAlpha = cb.a;
    ctx.shadowColor = cb.c;
    ctx.shadowBlur = 15;
    ctx.fillRect(cx, cy, cs, cs);
    ctx.strokeStyle = '#ffffff';
    ctx.lineWidth = 1;
    ctx.strokeRect(cx, cy, cs, cs);
  });
  ctx.restore();

  // Scanlines
  drawScanlines(ctx, w, h, 'rgba(0, 0, 0, 0.2)', 3);

  // Left Content
  const leftX = Math.round(90 * f);
  const midY = Math.round(h * 0.42);

  if (icon) {
    const iconSize = Math.round(160 * f);
    ctx.save();
    ctx.shadowColor = pal.primary;
    ctx.shadowBlur = 25;
    ctx.drawImage(icon, leftX, midY - Math.round(iconSize * 0.55), iconSize, iconSize);
    ctx.restore();
  }

  ctx.save();
  const tx = leftX + Math.round(200 * f);
  ctx.font = `600 ${Math.round(24 * f)}px "IBM Plex Mono", monospace`;
  ctx.fillStyle = pal.primary;
  ctx.letterSpacing = `${Math.round(4 * f)}px`;
  ctx.fillText('PS2 MEMORY CARD SYSTEM SOFTWARE // ANDROID', tx, midY - 30 * f);

  ctx.font = `700 ${Math.round(76 * f)}px "Chakra Petch", sans-serif`;
  ctx.fillStyle = '#ffffff';
  ctx.fillText(`${data.title} ${data.version}`, tx, midY + 45 * f);

  ctx.font = `500 ${Math.round(30 * f)}px "IBM Plex Mono", monospace`;
  ctx.fillStyle = pal.lighter;
  ctx.fillText(`${data.platform} ${data.minSdk} • ${data.date}`, tx, midY + 105 * f);

  // Footer
  ctx.font = `500 ${Math.round(20 * f)}px "IBM Plex Mono", monospace`;
  ctx.fillStyle = pal.light;
  ctx.fillText(`PS2 MEMORY CARD ARCHITECTURE & SAVE EDITOR // ${data.repo}`, leftX, h - Math.round(45 * f));
  ctx.restore();
}

/* ------------------------------------------------------------
   TEMPLATE 10: Raw SuperBlock Hex Dump
   ------------------------------------------------------------ */
function drawHexEditor(ctx, w, h, data, icon) {
  const pal = getColorPalette(data.color, '#00ff66');
  const f = w / 1500;

  // Terminal background
  ctx.fillStyle = '#060a08';
  ctx.fillRect(0, 0, w, h);

  // Background authentic Hex dump watermark in accent color
  ctx.save();
  ctx.font = `400 ${Math.round(15 * f)}px "IBM Plex Mono", monospace`;
  ctx.fillStyle = pal.rgba(0.08);
  const hexLines = [
    '00004000: 53 6F 6E 79 20 50 53 32  20 4D 65 6D 6F 72 79 20  | Sony PS2 Memory  |',
    '00004010: 43 61 72 64 20 46 6F 72  6D 61 74 20 31 2E 32 00  | Card Format 1.2. |',
    '00004020: 02 00 00 00 00 04 00 00  00 02 00 00 00 00 00 00  | ................ |',
    '00004030: 00 08 00 00 00 20 00 00  00 00 02 00 00 00 00 00  | ..... .......... |',
    '00004040: 00 00 00 00 08 00 00 00  FF FF FF FF 00 00 00 00  | ................ |',
    '00004050: 2F 42 49 4F 53 5F 44 55  4D 50 2F 53 41 56 45 53  | /BIOS_DUMP/SAVES |',
    '00004060: 53 43 50 48 2D 31 30 30  32 30 20 45 4E 47 49 4E  | SCPH-10020 ENGIN |',
    '00004070: 43 4C 55 53 54 45 52 5F  31 30 32 34 5F 42 59 54  | CLUSTER_1024_BYT |',
  ];
  let hexY = Math.round(50 * f);
  for (let i = 0; i < 24; i++) {
    const l = hexLines[i % hexLines.length];
    ctx.fillText(l, w * 0.42, hexY);
    hexY += Math.round(26 * f);
  }
  ctx.restore();

  // Left Content
  const leftX = Math.round(70 * f);
  const midY = Math.round(h * 0.42);

  if (icon) {
    const iconSize = Math.round(155 * f);
    ctx.drawImage(icon, leftX, midY - Math.round(iconSize * 0.6), iconSize, iconSize);
  }

  ctx.save();
  const tx = leftX + Math.round(195 * f);
  ctx.font = `700 ${Math.round(20 * f)}px "IBM Plex Mono", monospace`;
  ctx.fillStyle = pal.primary;
  ctx.fillText(`root@savesx2:~# ./inspect_superblock --auto`, tx, midY - 35 * f);

  ctx.font = `700 ${Math.round(70 * f)}px "Chakra Petch", sans-serif`;
  ctx.fillStyle = '#ffffff';
  ctx.fillText(`${data.title} ${data.version}`, tx, midY + 40 * f);

  // Blinking cursor in accent color
  ctx.fillStyle = pal.primary;
  const tw = ctx.measureText(`${data.title} ${data.version}`).width;
  ctx.fillRect(tx + tw + 10 * f, midY - 20 * f, 14 * f, 56 * f);

  ctx.font = `500 ${Math.round(28 * f)}px "IBM Plex Mono", monospace`;
  ctx.fillStyle = pal.light;
  ctx.fillText(`${data.platform} ${data.minSdk} • ${data.date}`, tx, midY + 98 * f);

  // Hex chips
  const tags = ['OFFSET: 0x00004000', 'FAT: 1024B', 'ECC: CRC32', 'STATUS: SYNCED'];
  let curX = tx;
  const pillY = midY + 155 * f;
  tags.forEach((t) => {
    ctx.font = `600 ${Math.round(16 * f)}px "IBM Plex Mono", monospace`;
    const pw = ctx.measureText(t).width + 20 * f;
    drawPill(ctx, curX, pillY - 22 * f, pw, 30 * f, 4 * f, pal.darker);
    ctx.fillStyle = pal.primary;
    ctx.fillText(t, curX + 10 * f, pillY - 2 * f);
    curX += pw + 10 * f;
  });

  // Footer
  ctx.font = `500 ${Math.round(19 * f)}px "IBM Plex Mono", monospace`;
  ctx.fillStyle = pal.light;
  ctx.fillText(`PS2 MEMORY CARD FORENSICS // ${data.repo}`, leftX, h - Math.round(40 * f));
  ctx.restore();
}

/* ------------------------------------------------------------
   TEMPLATE 11: Classic PS2 DVD Slipcover
   ------------------------------------------------------------ */
function drawBoxArt(ctx, w, h, data, icon) {
  const pal = getColorPalette(data.color, '#00439c');
  const f = w / 1500;

  // Main navy background
  ctx.fillStyle = '#0c121e';
  ctx.fillRect(0, 0, w, h);

  // Top PS2 DVD Header Band
  const bandH = Math.round(85 * f);
  ctx.fillStyle = '#000000';
  ctx.fillRect(0, 0, w, bandH);

  // Silver line under band
  ctx.fillStyle = '#a6b4c9';
  ctx.fillRect(0, bandH, w, 3 * f);

  ctx.save();
  ctx.font = `italic 700 ${Math.round(42 * f)}px "Chakra Petch", sans-serif`;
  ctx.fillStyle = '#ffffff';
  ctx.fillText('PlayStation 2', 40 * f, 58 * f);

  ctx.font = `600 ${Math.round(18 * f)}px "IBM Plex Mono", monospace`;
  ctx.fillStyle = '#94a3b8';
  ctx.textAlign = 'right';
  ctx.fillText('ANDROID APK • GPL-3.0', w - 40 * f, 55 * f);
  ctx.restore();

  // Left spine bar in accent color
  const spineW = Math.round(50 * f);
  ctx.fillStyle = pal.primary;
  ctx.fillRect(0, bandH + 3 * f, spineW, h - bandH - 3 * f);

  // Main Cover Art Area
  const leftX = spineW + Math.round(60 * f);
  const midY = Math.round(h * 0.48);

  if (icon) {
    const iconSize = Math.round(175 * f);
    ctx.drawImage(icon, leftX, midY - Math.round(iconSize * 0.55), iconSize, iconSize);
  }

  ctx.save();
  const tx = leftX + Math.round(215 * f);
  ctx.font = `700 ${Math.round(22 * f)}px "IBM Plex Mono", monospace`;
  ctx.fillStyle = pal.primary;
  ctx.fillText('OFFICIAL MEMORY MANAGER EDITION', tx, midY - 35 * f);

  ctx.font = `700 ${Math.round(74 * f)}px "Chakra Petch", sans-serif`;
  ctx.fillStyle = '#ffffff';
  ctx.fillText(`${data.title} ${data.version}`, tx, midY + 45 * f);

  ctx.font = `500 ${Math.round(30 * f)}px "IBM Plex Mono", monospace`;
  ctx.fillStyle = pal.lighter;
  ctx.fillText(`${data.platform} ${data.minSdk} • ${data.date}`, tx, midY + 105 * f);

  // Features list
  ctx.font = `500 ${Math.round(18 * f)}px "IBM Plex Mono", monospace`;
  ctx.fillStyle = pal.light;
  ctx.fillText('★ 8MB-128MB EXTENDED CARDS  ★ 3D MESH VIEWER  ★ PSU/MAX/CBS/XPS', tx, midY + 155 * f);

  // Footer bar
  ctx.fillStyle = '#1e293b';
  ctx.fillRect(spineW, h - Math.round(65 * f), w - spineW, Math.round(65 * f));

  ctx.font = `600 ${Math.round(20 * f)}px "IBM Plex Mono", monospace`;
  ctx.fillStyle = '#ffffff';
  ctx.fillText(`[ PS2 CARD MANAGER ]  [ 8MB - 128MB EXTENDED ]  [ 3D ICON VIEWER ]  // ${data.repo}`, spineW + 30 * f, h - 25 * f);
  ctx.restore();
}

/* ------------------------------------------------------------
   TEMPLATE 12: Synthwave 80s Grid
   ------------------------------------------------------------ */
function drawSynthwaveNeon(ctx, w, h, data, icon) {
  const pal = getColorPalette(data.color, '#ff007f');
  const f = w / 1500;

  // Night sky
  ctx.fillStyle = '#0c021c';
  ctx.fillRect(0, 0, w, h);

  // Sunset glow gradient with accent color
  const sunset = ctx.createLinearGradient(0, h * 0.2, 0, h * 0.65);
  sunset.addColorStop(0, '#0c021c');
  sunset.addColorStop(0.5, pal.dark);
  sunset.addColorStop(0.85, pal.primary);
  sunset.addColorStop(1, '#ffaa00');
  ctx.fillStyle = sunset;
  ctx.fillRect(0, h * 0.2, w, h * 0.45);

  // Sun disc
  ctx.save();
  ctx.beginPath();
  ctx.arc(w * 0.72, h * 0.52, Math.round(120 * f), 0, Math.PI * 2);
  const sunGrad = ctx.createLinearGradient(0, h * 0.35, 0, h * 0.65);
  sunGrad.addColorStop(0, '#ffee55');
  sunGrad.addColorStop(1, pal.primary);
  ctx.fillStyle = sunGrad;
  ctx.fill();
  // Cutout lines through sun
  ctx.fillStyle = '#0c021c';
  for (let i = 0; i < 5; i++) {
    ctx.fillRect(w * 0.6, h * (0.45 + i * 0.035), w * 0.25, Math.round(4 * f));
  }
  ctx.restore();

  // Horizon line
  const horizonY = Math.round(h * 0.65);
  ctx.fillStyle = pal.light;
  ctx.fillRect(0, horizonY, w, Math.round(3 * f));

  // Ground perspective grid in accent color
  ctx.save();
  ctx.fillStyle = '#080112';
  ctx.fillRect(0, horizonY + 3 * f, w, h - horizonY);

  ctx.strokeStyle = pal.rgba(0.45);
  ctx.lineWidth = 1.5;

  // Horizontal grid lines getting wider
  let gy = horizonY + 3 * f;
  let gstep = 8 * f;
  while (gy < h) {
    ctx.beginPath();
    ctx.moveTo(0, gy);
    ctx.lineTo(w, gy);
    ctx.stroke();
    gstep *= 1.35;
    gy += gstep;
  }

  // Perspective vertical lines converging to horizon center
  const vanishX = w * 0.5;
  for (let vx = -w * 0.5; vx < w * 1.5; vx += 70 * f) {
    ctx.beginPath();
    ctx.moveTo(vanishX, horizonY);
    ctx.lineTo(vx, h);
    ctx.stroke();
  }
  ctx.restore();

  // Left Content
  const leftX = Math.round(75 * f);
  const midY = Math.round(h * 0.38);

  if (icon) {
    const iconSize = Math.round(165 * f);
    ctx.save();
    ctx.shadowColor = pal.primary;
    ctx.shadowBlur = 30;
    ctx.drawImage(icon, leftX, midY - Math.round(iconSize * 0.55), iconSize, iconSize);
    ctx.restore();
  }

  ctx.save();
  const tx = leftX + Math.round(205 * f);
  ctx.font = `700 ${Math.round(22 * f)}px "IBM Plex Mono", monospace`;
  ctx.fillStyle = pal.light;
  ctx.fillText('RETRO MEMORY WAVE // EDITION 2026', tx, midY - 30 * f);

  ctx.font = `italic 700 ${Math.round(78 * f)}px "Chakra Petch", sans-serif`;
  ctx.fillStyle = '#ffffff';
  ctx.shadowColor = pal.primary;
  ctx.shadowBlur = 20;
  ctx.fillText(`${data.title} ${data.version}`, tx, midY + 48 * f);
  ctx.shadowBlur = 0;

  ctx.font = `500 ${Math.round(30 * f)}px "IBM Plex Mono", monospace`;
  ctx.fillStyle = pal.lighter;
  ctx.fillText(`${data.platform} ${data.minSdk} • ${data.date}`, tx, midY + 110 * f);

  ctx.font = `600 ${Math.round(20 * f)}px "IBM Plex Mono", monospace`;
  ctx.fillStyle = pal.light;
  ctx.fillText(data.repo, leftX, h - Math.round(35 * f));
  ctx.restore();
}

/* ------------------------------------------------------------
   TEMPLATE 13: PS2 Memory Sector Inspector
   ------------------------------------------------------------ */
function drawPocketLcd(ctx, w, h, data, icon) {
  const pal = getColorPalette(data.color, '#ffb703');
  const f = w / 1500;

  // PS2 Memory Card telemetry screen with backlight matching user accent color
  ctx.fillStyle = pal.darker;
  ctx.fillRect(0, 0, w, h);

  // Inset bezel
  const bX = 30 * f;
  const bY = 30 * f;
  const bW = w - 60 * f;
  const bH = h - 60 * f;

  ctx.fillStyle = pal.lighter;
  ctx.fillRect(bX, bY, bW, bH);

  // Pixel grid overlay
  ctx.save();
  ctx.fillStyle = pal.rgba(0.12);
  for (let px = bX; px < bX + bW; px += 4 * f) {
    ctx.fillRect(px, bY, 1, bH);
  }
  for (let py = bY; py < bY + bH; py += 4 * f) {
    ctx.fillRect(bX, py, bW, 1);
  }
  ctx.restore();

  // Dark LCD pixels
  const leftX = bX + Math.round(50 * f);
  const midY = Math.round(h * 0.44);

  if (icon) {
    const iconSize = Math.round(155 * f);
    ctx.drawImage(icon, leftX, midY - Math.round(iconSize * 0.55), iconSize, iconSize);
  }

  ctx.save();
  ctx.fillStyle = pal.darker;
  const tx = leftX + Math.round(195 * f);

  ctx.font = `700 ${Math.round(22 * f)}px "IBM Plex Mono", monospace`;
  ctx.fillText('[PS2 MEMORY CARD INSPECTOR // ANDROID]', tx, midY - 35 * f);

  ctx.font = `700 ${Math.round(72 * f)}px "Chakra Petch", sans-serif`;
  ctx.fillText(`${data.title} ${data.version}`, tx, midY + 45 * f);

  ctx.font = `600 ${Math.round(28 * f)}px "IBM Plex Mono", monospace`;
  ctx.fillText(`${data.platform} ${data.minSdk} • ${data.date}`, tx, midY + 105 * f);

  // Memory block status
  ctx.font = `700 ${Math.round(20 * f)}px "IBM Plex Mono", monospace`;
  ctx.fillText('■■■■■■■■■■■■■■■ [8192/8192 CLUSTERS SYNCED] 8MB-128MB', tx, midY + 155 * f);

  ctx.fillText(`SAVESX2 PS2 CARD ARCHITECTURE // ${data.repo}`, leftX, bY + bH - Math.round(30 * f));
  ctx.restore();
}

/* ------------------------------------------------------------
   TEMPLATE 14: Crimson Chaos Edition
   ------------------------------------------------------------ */
function drawCrimsonChaos(ctx, w, h, data, icon) {
  const pal = getColorPalette(data.color, '#e63946');
  const f = w / 1500;

  // Carbon black background
  ctx.fillStyle = '#0f0406';
  ctx.fillRect(0, 0, w, h);

  // Angular dynamic shards in user accent color
  ctx.save();
  ctx.fillStyle = pal.dark;
  ctx.beginPath();
  ctx.moveTo(w * 0.65, 0);
  ctx.lineTo(w, 0);
  ctx.lineTo(w, h * 0.55);
  ctx.closePath();
  ctx.fill();

  ctx.fillStyle = pal.primary;
  ctx.beginPath();
  ctx.moveTo(w * 0.75, 0);
  ctx.lineTo(w, 0);
  ctx.lineTo(w, h * 0.35);
  ctx.closePath();
  ctx.fill();
  ctx.restore();

  // Bottom hazard line in accent color
  drawHazardStripes(ctx, 0, h - 22 * f, w, 22 * f, pal.primary, '#0f0406');

  // Left Content
  const leftX = Math.round(80 * f);
  const midY = Math.round(h * 0.42);

  if (icon) {
    const iconSize = Math.round(165 * f);
    ctx.drawImage(icon, leftX, midY - Math.round(iconSize * 0.55), iconSize, iconSize);
  }

  ctx.save();
  const tx = leftX + Math.round(205 * f);

  ctx.font = `700 ${Math.round(22 * f)}px "IBM Plex Mono", monospace`;
  ctx.fillStyle = pal.light;
  ctx.fillText(`CHAOS ENGINE // ${data.badge.toUpperCase()}`, tx, midY - 35 * f);

  ctx.font = `700 ${Math.round(76 * f)}px "Chakra Petch", sans-serif`;
  ctx.fillStyle = '#ffffff';
  ctx.fillText(`${data.title} ${data.version}`, tx, midY + 45 * f);

  ctx.font = `500 ${Math.round(30 * f)}px "IBM Plex Mono", monospace`;
  ctx.fillStyle = pal.lighter;
  ctx.fillText(`${data.platform} ${data.minSdk} • ${data.date}`, tx, midY + 105 * f);

  // Format chips
  const tags = ['.PS2', '.PSU', '.MAX', '.CBS', '.XPS', '8MB-128MB'];
  let curX = tx;
  const pillY = midY + 160 * f;
  tags.forEach((t) => {
    ctx.font = `600 ${Math.round(16 * f)}px "IBM Plex Mono", monospace`;
    const pw = ctx.measureText(t).width + 18 * f;
    drawPill(ctx, curX, pillY - 22 * f, pw, 30 * f, 4 * f, pal.darker);
    ctx.fillStyle = pal.light;
    ctx.fillText(t, curX + 9 * f, pillY - 2 * f);
    curX += pw + 10 * f;
  });

  ctx.font = `500 ${Math.round(20 * f)}px "IBM Plex Mono", monospace`;
  ctx.fillStyle = pal.primary;
  ctx.fillText(data.repo, leftX, h - Math.round(45 * f));
  ctx.restore();
}

/* ------------------------------------------------------------
   UTILITY DRAWING FUNCTIONS
   ------------------------------------------------------------ */
function drawSubtleDotGrid(ctx, x, y, w, h, color, step) {
  ctx.save();
  ctx.fillStyle = color;
  for (let px = x + step; px < x + w; px += step) {
    for (let py = y + step; py < y + h; py += step) {
      ctx.beginPath();
      ctx.arc(px, py, 1.2, 0, Math.PI * 2);
      ctx.fill();
    }
  }
  ctx.restore();
}

function drawScanlines(ctx, w, h, color, spacing) {
  ctx.save();
  ctx.fillStyle = color;
  for (let y = 0; y < h; y += spacing) {
    ctx.fillRect(0, y, w, 1);
  }
  ctx.restore();
}

function drawHazardStripes(ctx, x, y, w, h, col1, col2) {
  ctx.save();
  ctx.fillStyle = col2;
  ctx.fillRect(x, y, w, h);
  ctx.fillStyle = col1;
  const stripeW = h;
  for (let sx = -h; sx < w + h; sx += stripeW * 2) {
    ctx.beginPath();
    ctx.moveTo(sx, y + h);
    ctx.lineTo(sx + stripeW, y);
    ctx.lineTo(sx + stripeW * 1.5, y);
    ctx.lineTo(sx + stripeW * 0.5, y + h);
    ctx.closePath();
    ctx.fill();
  }
  ctx.restore();
}

function drawFallbackCartridgeIcon(ctx, x, y, w, h) {
  ctx.save();
  ctx.fillStyle = '#263238';
  ctx.beginPath();
  ctx.roundRect(x, y, w, h, 14);
  ctx.fill();
  ctx.fillStyle = '#00b4d8';
  ctx.font = `700 ${Math.round(h * 0.35)}px "Chakra Petch", sans-serif`;
  ctx.textAlign = 'center';
  ctx.fillText('PS2', x + w / 2, y + h / 2 + 10);
  ctx.restore();
}

/* ============================================================
   UI CONTROLLER & EVENT LISTENERS
   ============================================================ */

let liveCanvas = null;

function initStudio() {
  const canvasContainer = document.getElementById('cover-canvas');
  if (!canvasContainer) return;

  // Create primary live canvas
  liveCanvas = document.createElement('canvas');
  liveCanvas.id = 'studio-live-canvas';
  canvasContainer.innerHTML = '';
  canvasContainer.appendChild(liveCanvas);

  // Initial render
  updateLiveStage();
  populateTemplateSelector();
  populateGalleryGrid();
  attachEventListeners();
  initTheme();
  refreshGitHubRelease();
}

/**
 * Updates the live stage preview with current inputs and dimensions
 */
export async function updateLiveStage() {
  if (!liveCanvas) return;

  const viewportFrame = document.getElementById('stage-canvas-frame');
  if (viewportFrame) {
    // Update container aspect ratio to match base dimensions
    viewportFrame.style.aspectRatio = `${state.width} / ${state.height}`;
  }

  // Update labels
  const titleEl = document.getElementById('stage-template-name');
  const dimsEl = document.getElementById('stage-dims-display');
  const qualityEl = document.getElementById('stage-quality-badge');

  const tpl = TEMPLATES.find((t) => t.id === state.templateId) || TEMPLATES[0];
  if (titleEl) titleEl.textContent = tpl.name;

  const effScale = getEffectiveScale(state.scale, state.width, state.height);
  const outW = Math.round(state.width * effScale);
  const outH = Math.round(state.height * effScale);

  if (dimsEl) dimsEl.textContent = `${state.width} × ${state.height} px (${state.platformName})`;
  if (qualityEl) qualityEl.textContent = `Export: ${outW} × ${outH} px (${state.scale}x Scale)`;

  // Render to canvas
  await renderCoverToCanvas(liveCanvas, state.templateId, {
    width: state.width,
    height: state.height,
    scale: 1, // Preview at 1x for silky responsiveness
    badgeText: state.badgeText,
    titleText: state.titleText,
    versionText: state.versionText,
    platformText: state.platformText,
    minSdkText: state.minSdkText,
    dateText: state.dateText,
    repoText: state.repoText,
    color: state.color,
  });
}

let currentReelCategory = 'all';

/**
 * Select a template and synchronize all UI components (dropdown, grid, stage, colors)
 */
export function selectTemplate(tplId) {
  state.templateId = tplId;
  const tpl = TEMPLATES.find((x) => x.id === state.templateId);

  // Preserve user-selected color theme across all templates!
  // If user explicitly picked an accent color, it stays applied to any template selected.
  // Otherwise, load the template's default color.
  if (state.userSelectedColor) {
    state.color = state.userSelectedColor;
  } else if (tpl && tpl.defaultColor) {
    state.color = tpl.defaultColor;
  }
  updateActiveColorSwatch(state.color);

  // Synchronize quick-select dropdown
  const quickSelect = document.getElementById('template-quick-select');
  if (quickSelect && quickSelect.value !== tplId) {
    quickSelect.value = tplId;
  }

  // If currently filtered by category and active template is outside category, reset to 'all'
  if (currentReelCategory !== 'all' && tpl && tpl.category !== currentReelCategory) {
    currentReelCategory = 'all';
    document.querySelectorAll('#tpl-subfilters .tpl-subchip').forEach((chip) => {
      chip.classList.toggle('active', chip.dataset.cat === 'all');
    });
    renderTemplateGrid();
  } else {
    // Update active class in grid
    const container = document.getElementById('template-selector-list');
    if (container) {
      container.querySelectorAll('.tpl-card').forEach((card) => {
        const isActive = card.dataset.template === tplId;
        card.classList.toggle('active', isActive);
        if (isActive) {
          card.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
        }
      });
    }
  }

  updateLiveStage();
}

/**
 * Render the template cards grid in inspector
 */
function renderTemplateGrid() {
  const container = document.getElementById('template-selector-list');
  if (!container) return;

  const filtered = currentReelCategory === 'all'
    ? TEMPLATES
    : TEMPLATES.filter((t) => t.category === currentReelCategory);

  container.innerHTML = filtered
    .map(
      (t) => `
    <button type="button" class="tpl-card ${t.id === state.templateId ? 'active' : ''}" data-template="${t.id}" title="${t.name}">
      <span class="tpl-card-badge">${t.badge}</span>
      <span class="tpl-card-name">${t.name}</span>
      <span class="tpl-card-sub">${t.bestFor}</span>
    </button>
  `,
    )
    .join('');

  container.querySelectorAll('.tpl-card').forEach((btn) => {
    btn.addEventListener('click', () => {
      selectTemplate(btn.dataset.template);
    });
  });

  const activeCard = container.querySelector('.tpl-card.active');
  if (activeCard) {
    activeCard.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
  }
}

/**
 * Populate Template Selector (Quick Select + Grid)
 */
function populateTemplateSelector() {
  // Populate Quick Select dropdown
  const quickSelect = document.getElementById('template-quick-select');
  if (quickSelect) {
    const categories = [
      { id: 'profile', label: 'Profile Header & Social' },
      { id: 'banner', label: 'Banners & Widescreen' },
      { id: 'retro', label: 'Retro PS2 / Heritage' },
      { id: 'dev', label: 'Dev, Telemetry & Hex' },
    ];

    let selectHtml = '';
    categories.forEach((cat) => {
      const catTemplates = TEMPLATES.filter((t) => t.category === cat.id);
      if (catTemplates.length > 0) {
        selectHtml += `<optgroup label="${cat.label}">`;
        catTemplates.forEach((t) => {
          selectHtml += `<option value="${t.id}" ${t.id === state.templateId ? 'selected' : ''}>${t.badge} — ${t.name}</option>`;
        });
        selectHtml += `</optgroup>`;
      }
    });
    quickSelect.innerHTML = selectHtml;
    quickSelect.value = state.templateId;
  }

  // Populate grid
  renderTemplateGrid();
}

/**
 * Populate Collection Gallery Cards at bottom
 */
function populateGalleryGrid() {
  const grid = document.getElementById('gallery-cards-grid');
  if (!grid) return;

  const filtered = state.activeFilter === 'all'
    ? TEMPLATES
    : TEMPLATES.filter((t) => t.category === state.activeFilter);

  grid.innerHTML = filtered
    .map(
      (t) => `
    <div class="gallery-card" data-template="${t.id}">
      <div class="gallery-card-thumb" id="thumb-${t.id}">
        <!-- Canvas thumbnail injected dynamically -->
      </div>
      <div class="gallery-card-body">
        <div class="gallery-card-head">
          <span class="gallery-card-badge">${t.badge}</span>
          <span class="gallery-card-cat">${t.category.toUpperCase()}</span>
        </div>
        <h3 class="gallery-card-title">${t.name}</h3>
        <p class="gallery-card-desc">${t.desc}</p>
        <div class="gallery-card-meta">
          <span class="gallery-meta-item">${t.bestFor}</span>
          <span class="gallery-meta-item">High-Res 1080p+</span>
        </div>
        <div class="gallery-card-actions">
          <button type="button" class="btn btn-filled btn-sm btn-use-tpl" data-tpl="${t.id}">
            Customize in Studio
          </button>
          <button type="button" class="btn btn-outline btn-sm btn-quick-dl" data-tpl="${t.id}">
            Quick Download PNG
          </button>
        </div>
      </div>
    </div>
  `,
    )
    .join('');

  // Render thumbnails
  filtered.forEach(async (t) => {
    const thumbContainer = document.getElementById(`thumb-${t.id}`);
    if (thumbContainer) {
      const c = document.createElement('canvas');
      thumbContainer.appendChild(c);
      await renderCoverToCanvas(c, t.id, {
        width: 600,
        height: 400,
        scale: 0.5,
        color: t.defaultColor,
      });
    }
  });

  // Attach card buttons
  grid.querySelectorAll('.btn-use-tpl').forEach((btn) => {
    btn.addEventListener('click', () => {
      const tplId = btn.dataset.tpl;
      selectTemplate(tplId);
      const t = TEMPLATES.find((x) => x.id === tplId);
      document.getElementById('studio')?.scrollIntoView({ behavior: 'smooth' });
      showToast(`Loaded "${t?.name || tplId}" into Studio`);
    });
  });

  grid.querySelectorAll('.btn-quick-dl').forEach((btn) => {
    btn.addEventListener('click', async () => {
      const tplId = btn.dataset.tpl;
      await exportCoverPng(tplId);
    });
  });
}

const COLOR_NAMES = {
  '#00439c': 'Cobalt Classic (#00439c)',
  '#00b4d8': 'Cyber Cyan (#00b4d8)',
  '#06d6a0': 'Emerald Green (#06d6a0)',
  '#ffb703': 'Solar Amber (#ffb703)',
  '#e63946': 'Crimson Red (#e63946)',
  '#ff007f': 'Synthwave Magenta (#ff007f)',
  '#00ff66': 'Terminal Phosphor (#00ff66)',
  '#141a24': 'Dark Obsidian (#141a24)',
};

function updateActiveColorSwatch(color) {
  const norm = normalizeHexColor(color).toLowerCase();
  document.querySelectorAll('.color-swatch').forEach((s) => {
    const sColor = normalizeHexColor(s.dataset.color).toLowerCase();
    s.classList.toggle('active', sColor === norm);
  });
  const hexInput = document.getElementById('input-custom-hex');
  if (hexInput) hexInput.value = color;
  const colorInput = document.getElementById('input-custom-color');
  if (colorInput) {
    colorInput.value = norm;
  }
  const preview = document.getElementById('custom-color-preview');
  if (preview) preview.style.background = color;

  const labelEl = document.getElementById('active-color-name');
  if (labelEl) {
    labelEl.textContent = COLOR_NAMES[norm] || `Custom HEX (${color.toUpperCase()})`;
  }
}

/**
 * Toast Notification Helper
 */
function showToast(msg) {
  const toast = document.getElementById('studio-status-toast');
  if (!toast) return;
  toast.textContent = msg;
  toast.classList.add('show');
  setTimeout(() => toast.classList.remove('show'), 3500);
}

/**
 * Export High-Resolution PNG (Guaranteed minimum 1080p)
 */
export async function exportCoverPng(templateId = state.templateId) {
  showToast('Rendering high-resolution PNG (1080p+)...');

  const exportCanvas = document.createElement('canvas');
  const effScale = getEffectiveScale(state.scale, state.width, state.height);

  await renderCoverToCanvas(exportCanvas, templateId, {
    width: state.width,
    height: state.height,
    scale: effScale,
    badgeText: state.badgeText,
    titleText: state.titleText,
    versionText: state.versionText,
    platformText: state.platformText,
    minSdkText: state.minSdkText,
    dateText: state.dateText,
    repoText: state.repoText,
    color: state.color,
  });

  exportCanvas.toBlob((blob) => {
    if (!blob) {
      showToast('Error generating PNG image');
      return;
    }

    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    const filename = `savesx2-cover-${templateId}-${exportCanvas.width}x${exportCanvas.height}.png`;
    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);

    showToast(`Downloaded ${filename} (${exportCanvas.width}×${exportCanvas.height}px)`);
  }, 'image/png');
}

/**
 * Copy Image to Clipboard
 */
async function copyImageToClipboard() {
  if (!navigator.clipboard || !navigator.clipboard.write) {
    showToast('Clipboard API not supported on this browser');
    return;
  }

  showToast('Preparing image for clipboard...');
  const exportCanvas = document.createElement('canvas');
  const effScale = getEffectiveScale(state.scale, state.width, state.height);

  await renderCoverToCanvas(exportCanvas, state.templateId, {
    width: state.width,
    height: state.height,
    scale: effScale,
    badgeText: state.badgeText,
    titleText: state.titleText,
    versionText: state.versionText,
    platformText: state.platformText,
    minSdkText: state.minSdkText,
    dateText: state.dateText,
    repoText: state.repoText,
    color: state.color,
  });

  exportCanvas.toBlob(async (blob) => {
    if (!blob) return showToast('Error generating PNG');
    try {
      await navigator.clipboard.write([new ClipboardItem({ 'image/png': blob })]);
      showToast(`Copied ${exportCanvas.width}×${exportCanvas.height}px PNG to clipboard!`);
    } catch (err) {
      console.error(err);
      showToast('Could not copy image to clipboard');
    }
  }, 'image/png');
}

function attachEventListeners() {
  // Quick-select template dropdown in topbar
  document.getElementById('template-quick-select')?.addEventListener('change', (e) => {
    selectTemplate(e.target.value);
  });

  // Inspector Tabs switching
  document.querySelectorAll('.inspector-tab').forEach((tab) => {
    tab.addEventListener('click', () => {
      document.querySelectorAll('.inspector-tab').forEach((t) => {
        t.classList.remove('active');
        t.setAttribute('aria-selected', 'false');
      });
      tab.classList.add('active');
      tab.setAttribute('aria-selected', 'true');

      const targetTab = tab.dataset.tab;
      document.querySelectorAll('.inspector-panel').forEach((p) => p.classList.remove('active'));
      document.getElementById(`panel-${targetTab}`)?.classList.add('active');
    });
  });

  // Stage Zoom buttons
  document.querySelectorAll('.btn-zoom').forEach((btn) => {
    btn.addEventListener('click', () => {
      document.querySelectorAll('.btn-zoom').forEach((b) => b.classList.remove('active'));
      btn.classList.add('active');
      const zoom = btn.dataset.zoom;
      const frame = document.getElementById('stage-canvas-frame');
      if (frame) {
        frame.classList.remove('zoom-fit', 'zoom-100', 'zoom-75', 'zoom-50');
        frame.classList.add(`zoom-${zoom}`);
      }
    });
  });

  // Template category subfilter chips
  const subChips = document.querySelectorAll('#tpl-subfilters .tpl-subchip');
  subChips.forEach((chip) => {
    chip.addEventListener('click', () => {
      subChips.forEach((c) => c.classList.remove('active'));
      chip.classList.add('active');
      currentReelCategory = chip.dataset.cat || 'all';
      renderTemplateGrid();
    });
  });

  // Badge suggestion chips
  document.querySelectorAll('#badge-suggestions .chip-mini').forEach((chip) => {
    chip.addEventListener('click', () => {
      const val = chip.dataset.val;
      state.badgeText = val;
      const badgeInput = document.getElementById('input-badge');
      if (badgeInput) badgeInput.value = val;
      updateLiveStage();
    });
  });

  // Input fields
  const bindInput = (id, key) => {
    const el = document.getElementById(id);
    if (!el) return;
    el.addEventListener('input', (e) => {
      state[key] = e.target.value;
      updateLiveStage();
    });
  };

  bindInput('input-badge', 'badgeText');
  bindInput('input-title', 'titleText');
  
  const verInput = document.getElementById('input-version');
  if (verInput) {
    verInput.addEventListener('input', (e) => {
      const prevVer = state.versionText;
      state.versionText = e.target.value;
      syncBadgeVersionSuggestion(e.target.value, prevVer);
      updateLiveStage();
    });
  }

  bindInput('input-platform', 'platformText');
  bindInput('input-minsdk', 'minSdkText');
  bindInput('input-date', 'dateText');
  bindInput('input-repo', 'repoText');

  // Initial sync of badge version recommendation chip
  syncBadgeVersionSuggestion(state.versionText);

  // Platform selection tiles
  const platformTiles = document.querySelectorAll('#platform-chips .platform-tile, #platform-chips .cap-chip');
  platformTiles.forEach((tile) => {
    tile.addEventListener('click', () => {
      platformTiles.forEach((c) => c.classList.remove('active'));
      tile.classList.add('active');
      state.width = Number(tile.dataset.w);
      state.height = Number(tile.dataset.h);
      state.platformName = tile.querySelector('.platform-name')?.textContent || tile.querySelector('span')?.textContent || 'Custom';
      updateLiveStage();
    });
  });

  // Quality multipliers
  const qualityChips = document.querySelectorAll('#quality-chips .quality-chip');
  qualityChips.forEach((chip) => {
    chip.addEventListener('click', () => {
      qualityChips.forEach((c) => c.classList.remove('active'));
      chip.classList.add('active');
      state.scale = Number(chip.dataset.scale);
      updateLiveStage();
    });
  });

  // Color Swatches
  const swatches = document.querySelectorAll('.color-swatch');
  swatches.forEach((s) => {
    s.addEventListener('click', () => {
      swatches.forEach((x) => x.classList.remove('active'));
      s.classList.add('active');
      state.color = s.dataset.color;
      state.userSelectedColor = s.dataset.color;
      updateActiveColorSwatch(state.color);
      updateLiveStage();
    });
  });

  // Custom Color Picker & Hex Input
  const colorPicker = document.getElementById('input-custom-color');
  const hexInput = document.getElementById('input-custom-hex');
  const customPreview = document.getElementById('custom-color-preview');

  if (colorPicker && hexInput) {
    colorPicker.addEventListener('input', (e) => {
      const val = e.target.value;
      state.color = val;
      state.userSelectedColor = val;
      hexInput.value = val;
      if (customPreview) customPreview.style.background = val;
      document.querySelectorAll('.color-swatch').forEach((s) => s.classList.remove('active'));
      updateActiveColorSwatch(state.color);
      updateLiveStage();
    });

    hexInput.addEventListener('input', (e) => {
      let val = e.target.value.trim();
      if (!val.startsWith('#')) val = '#' + val;
      if (/^#[0-9A-Fa-f]{6}$/.test(val)) {
        state.color = val;
        state.userSelectedColor = val;
        colorPicker.value = val;
        if (customPreview) customPreview.style.background = val;
        document.querySelectorAll('.color-swatch').forEach((s) => s.classList.remove('active'));
        updateActiveColorSwatch(state.color);
        updateLiveStage();
      }
    });
  }

  // Reset Default Color button
  document.getElementById('btn-reset-color')?.addEventListener('click', () => {
    state.userSelectedColor = null;
    const t = TEMPLATES.find((x) => x.id === state.templateId);
    if (t && t.defaultColor) {
      state.color = t.defaultColor;
      updateActiveColorSwatch(state.color);
      updateLiveStage();
      showToast(`Color reset to ${t.name} default (${state.color})`);
    }
  });

  // Export buttons
  document.getElementById('btn-export-png')?.addEventListener('click', () => exportCoverPng());
  document.getElementById('btn-top-export')?.addEventListener('click', () => exportCoverPng());
  document.getElementById('btn-copy-clipboard')?.addEventListener('click', copyImageToClipboard);
  document.getElementById('btn-top-copy')?.addEventListener('click', copyImageToClipboard);

  // Today button
  document.getElementById('btn-set-today')?.addEventListener('click', () => {
    const d = new Date();
    const months = ['JAN', 'FEB', 'MAR', 'APR', 'MAY', 'JUN', 'JUL', 'AUG', 'SEPT', 'OCT', 'NOV', 'DEC'];
    const formatted = `RELEASED ${d.getDate()} ${months[d.getMonth()]} ${d.getFullYear()}`;
    state.dateText = formatted;
    const dateInput = document.getElementById('input-date');
    if (dateInput) dateInput.value = formatted;
    updateLiveStage();
    showToast(`Date updated to ${formatted}`);
  });

  // Sync GitHub Release
  document.getElementById('btn-sync-github')?.addEventListener('click', refreshGitHubRelease);

  // Gallery filter chips
  document.querySelectorAll('.filter-chip').forEach((chip) => {
    chip.addEventListener('click', () => {
      document.querySelectorAll('.filter-chip').forEach((c) => c.classList.remove('active'));
      chip.classList.add('active');
      state.activeFilter = chip.dataset.filter;
      populateGalleryGrid();
    });
  });

  // Drawer
  const drawer = document.getElementById('drawer');
  const scrim = document.getElementById('scrim');
  const menuOpenBtn = document.getElementById('menu-open');
  const menuCloseBtn = document.getElementById('menu-close');

  if (menuOpenBtn && drawer && scrim) {
    menuOpenBtn.addEventListener('click', () => {
      drawer.classList.add('open');
      scrim.classList.add('open');
      drawer.inert = false;
    });
  }

  if (menuCloseBtn && drawer && scrim) {
    menuCloseBtn.addEventListener('click', () => {
      drawer.classList.remove('open');
      scrim.classList.remove('open');
      drawer.inert = true;
    });
    scrim.addEventListener('click', () => {
      drawer.classList.remove('open');
      scrim.classList.remove('open');
      drawer.inert = true;
    });
  }
}

/**
 * Format a version string into a recommendation badge label (e.g. "v1.7.2 RELEASE")
 */
function formatBadgeRelease(ver) {
  let raw = (ver || '').trim();
  if (!raw) return 'LATEST RELEASE';
  if (/^\d/.test(raw)) {
    raw = 'v' + raw;
  }
  if (/release$/i.test(raw)) {
    return raw;
  }
  return `${raw} RELEASE`;
}

/**
 * Sync the top badge recommendation chip and active badge with the active version string
 */
function syncBadgeVersionSuggestion(newVer, prevVer = null) {
  const chip = document.getElementById('badge-chip-version');
  const formatted = formatBadgeRelease(newVer);
  if (chip) {
    chip.dataset.val = formatted;
    chip.textContent = formatted;
  }

  // If top badge input was currently tracking an older version release string, keep it synced
  const badgeInput = document.getElementById('input-badge');
  if (badgeInput) {
    const currentBadge = badgeInput.value.trim();
    const prevFormatted = prevVer ? formatBadgeRelease(prevVer) : null;
    const isOldReleaseBadge =
      currentBadge === 'v1.7.1 RELEASE' ||
      (prevFormatted && currentBadge.toLowerCase() === prevFormatted.toLowerCase());
    if (isOldReleaseBadge) {
      badgeInput.value = formatted;
      state.badgeText = formatted;
    }
  }
}

/**
 * Fetch latest release from GitHub API
 */
async function refreshGitHubRelease() {
  const RELEASE_API = 'https://api.github.com/repos/mininxd/SAVESX2/releases/latest';
  try {
    const res = await fetch(RELEASE_API, { headers: { Accept: 'application/vnd.github+json' } });
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    const data = await res.json();
    if (data.tag_name) {
      const prevVer = state.versionText;
      state.versionText = data.tag_name;
      const verInput = document.getElementById('input-version');
      if (verInput) verInput.value = data.tag_name;
      syncBadgeVersionSuggestion(data.tag_name, prevVer);
    }
    if (data.published_at) {
      const d = new Date(data.published_at);
      const months = ['JAN', 'FEB', 'MAR', 'APR', 'MAY', 'JUN', 'JUL', 'AUG', 'SEPT', 'OCT', 'NOV', 'DEC'];
      const formatted = `RELEASED ${d.getDate()} ${months[d.getMonth()]} ${d.getFullYear()}`;
      state.dateText = formatted;
      const dateInput = document.getElementById('input-date');
      if (dateInput) dateInput.value = formatted;
    }
    updateLiveStage();
    showToast(`Synced latest release ${state.versionText} from GitHub`);
  } catch {
    showToast('Could not fetch latest release; kept local release information');
  }
}

/**
 * Theme management
 */
function initTheme() {
  const root = document.documentElement;
  const btnLight = document.getElementById('theme-btn-light');
  const btnDark = document.getElementById('theme-btn-dark');
  const btnToggle = document.getElementById('btn-theme-toggle');
  const metaTheme = document.querySelector('meta[name="theme-color"]');

  function applyTheme(theme) {
    root.setAttribute('data-theme', theme);
    const isDark = theme === 'dark';
    if (btnLight && btnDark) {
      btnLight.classList.toggle('active', !isDark);
      btnLight.setAttribute('aria-pressed', String(!isDark));
      btnDark.classList.toggle('active', isDark);
      btnDark.setAttribute('aria-pressed', String(isDark));
    }
    if (metaTheme) {
      metaTheme.setAttribute('content', isDark ? '#0f141c' : '#f9f9fe');
    }
    try {
      localStorage.setItem('savesx2-theme', theme);
    } catch {}
    updateLiveStage();
  }

  let savedTheme = 'dark';
  try {
    savedTheme = localStorage.getItem('savesx2-theme') || 'dark';
  } catch {}
  if (savedTheme !== 'dark' && savedTheme !== 'light') savedTheme = 'dark';
  applyTheme(savedTheme);

  if (btnLight) btnLight.addEventListener('click', () => applyTheme('light'));
  if (btnDark) btnDark.addEventListener('click', () => applyTheme('dark'));
  if (btnToggle) {
    btnToggle.addEventListener('click', () => {
      const current = root.getAttribute('data-theme') || 'dark';
      const next = current === 'dark' ? 'light' : 'dark';
      applyTheme(next);
      showToast(`Theme: ${next.toUpperCase()}`);
    });
  }
}

// Auto-boot when DOM is loaded
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', initStudio);
} else {
  initStudio();
}
