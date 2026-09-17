import { defineConfig } from 'vite';

// base './' keeps asset URLs relative so the build works on
// GitHub Pages project sites (user.github.io/SAVESX2/) and custom domains.
export default defineConfig({
  base: './',
  build: {
    outDir: 'dist',
    emptyOutDir: true,
  },
  server: {
    host: '0.0.0.0',
    port: 5173,
    // Allow sandboxed/proxied preview hosts (e.g. *.e2b.app live preview).
    allowedHosts: true,
  },
});
