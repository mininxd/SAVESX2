<p align="center">
  <img src="public/icon.png" width="96" alt="SAVESX2 logo" />
</p>

<h1 align="center">SAVESX2 — Website</h1>

<p align="center">
  <strong>Landing + Download + Guide site for SAVESX2, the PS2 Memory Card Reader &amp; Editor for Android.</strong><br />
  Retro gamer UI patched onto Material 3 — flat colors only, zero gradients.
</p>

---

## This branch

`arena/01a0b02a-savesx2` is a **website-only branch**, rebuilt from zero with
[Vite](https://vitejs.dev/) + vanilla HTML/CSS/JS. The Android app source lives
on `master` — this branch keeps just the site, the logo (`public/icon.png`),
and the GPL-3.0 license.

## Develop

Requirements: Node.js 20+.

```bash
npm install
npm run dev      # local dev server at http://localhost:5173
npm run build    # production build into dist/
npm run preview  # preview the production build
```

## Deploy

Pushing to this branch triggers `.github/workflows/pages.yml`, which builds
with Vite and deploys `dist/` to **GitHub Pages**. Asset URLs use a relative
`base ('./')`, so the build works on project pages
(`<user>.github.io/SAVESX2/`) and custom domains alike.

## Design rules

- **Material 3**: color roles (primary / container / surface / outline),
  shape scale (12/16/20/28), filled/tonal/outlined buttons, chips, data table.
- **Retro gamer**: mono labels, region badges (`US`/`EU`/`JP`/`ASIA`/`SYS` —
  same recipe as `Ps2Region` in the app), dot-grid backdrop, blinking cursor,
  capacity playground mirroring `Ps2SuperBlock` cluster geometry.
- **No gradients**: no `linear-gradient` / `radial-gradient` anywhere. Flat
  fills, 1px outline borders, and solid shadows only. Patterns use SVG data
  URIs. Themes: PS2 deep-blue dark (default) + light, persisted in
  `localStorage`, `prefers-reduced-motion` respected.

Key files:

| File            | Purpose                                              |
| :-------------- | :--------------------------------------------------- |
| `index.html`    | Single page: hero, features, formats, guide, FAQ, download |
| `src/style.css` | Full M3 + retro token system and components          |
| `src/main.js`   | Theme, drawer, scrollspy, reveal, playground, release fetch |
| `public/`       | Static assets (`icon.png`, `favicon.svg`)            |

## License

GNU General Public License v3.0 or later — see [COPYING.GPLv3](COPYING.GPLv3).
