import './style.css';

const FALLBACK_VERSION = 'v1.7.0';
const FALLBACK_TAG = 'v1.7.0';
const RELEASES_URL = 'https://github.com/mininxd/SAVESX2/releases/latest';
const RELEASE_API = 'https://api.github.com/repos/mininxd/SAVESX2/releases/latest';

/* ---------- Theme toggle (dark default, persisted) ---------- */
const root = document.documentElement;
const btnLight = document.getElementById('theme-btn-light');
const btnDark = document.getElementById('theme-btn-dark');
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
  if (metaTheme) metaTheme.setAttribute('content', isDark ? '#0f141c' : '#f9f9fe');
  try {
    localStorage.setItem('savesx2-theme', theme);
  } catch {
    /* private mode — ignore */
  }
}

(function initTheme() {
  let theme = 'dark';
  try {
    theme = localStorage.getItem('savesx2-theme') || 'dark';
  } catch {
    /* ignore */
  }
  if (theme !== 'dark' && theme !== 'light') theme = 'dark';
  applyTheme(theme);
})();

if (btnLight) btnLight.addEventListener('click', () => applyTheme('light'));
if (btnDark) btnDark.addEventListener('click', () => applyTheme('dark'));

/* ---------- Mobile drawer ---------- */
const drawer = document.getElementById('drawer');
const scrim = document.getElementById('scrim');
const menuOpenBtn = document.getElementById('menu-open');

function setDrawer(open) {
  drawer.classList.toggle('open', open);
  scrim.classList.toggle('open', open);
  drawer.inert = !open;
  menuOpenBtn.setAttribute('aria-expanded', String(open));
  document.body.style.overflow = open ? 'hidden' : '';
  if (open) document.getElementById('menu-close').focus();
}

menuOpenBtn.addEventListener('click', () => setDrawer(true));
document.getElementById('menu-close').addEventListener('click', () => setDrawer(false));
scrim.addEventListener('click', () => setDrawer(false));
drawer.querySelectorAll('a').forEach((a) => a.addEventListener('click', () => setDrawer(false)));
window.addEventListener('keydown', (e) => {
  if (e.key === 'Escape') setDrawer(false);
});

/* ---------- Scrollspy ---------- */
const spyLinks = [...document.querySelectorAll('.nav-desktop a')];
const spySections = spyLinks
  .map((a) => document.querySelector(a.getAttribute('href')))
  .filter(Boolean);

if ('IntersectionObserver' in window && spySections.length) {
  const spy = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (!entry.isIntersecting) return;
        const id = `#${entry.target.id}`;
        spyLinks.forEach((a) => a.classList.toggle('active', a.getAttribute('href') === id));
      });
    },
    { rootMargin: '-40% 0px -55% 0px' },
  );
  spySections.forEach((s) => spy.observe(s));
}

/* ---------- Reveal on scroll ---------- */
const revealEls = document.querySelectorAll('.reveal');
if ('IntersectionObserver' in window && !window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
  const revealer = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          entry.target.classList.add('in');
          revealer.unobserve(entry.target);
        }
      });
    },
    { threshold: 0.08 },
  );
  revealEls.forEach((el) => revealer.observe(el));
} else {
  revealEls.forEach((el) => el.classList.add('in'));
}

/* ---------- Hero: capacity playground ----------
   Mirrors Ps2SuperBlock geometry: 1024-byte clusters, so
   clusters = capacityMB * 1024. Used space is a fixed demo
   save-set (~5.2 MB) so bigger cards visibly free up. */
const DEMO_USED_MB = 5.2;
const capChips = [...document.querySelectorAll('.cap-chip')];
const mockFilename = document.getElementById('mock-filename');
const mockUsed = document.getElementById('mock-used');
const mockFree = document.getElementById('mock-free');
const mockBar = document.getElementById('mock-bar');
const geoClusters = document.getElementById('geo-clusters');

function setCapacity(capMb) {
  const free = Math.max(capMb - DEMO_USED_MB, 0);
  const pct = Math.min((DEMO_USED_MB / capMb) * 100, 100);
  mockFilename.textContent = `mcd001.ps2 — ${capMb} MB`;
  mockUsed.textContent = `${DEMO_USED_MB.toFixed(1)} MB used`;
  mockFree.textContent = `${free.toFixed(1)} MB free • 3 saves`;
  mockBar.style.width = `${pct}%`;
  geoClusters.textContent = (capMb * 1024).toLocaleString('en-US');
  capChips.forEach((chip) =>
    chip.setAttribute('aria-pressed', String(Number(chip.dataset.cap) === capMb)),
  );
}

capChips.forEach((chip) =>
  chip.addEventListener('click', () => setCapacity(Number(chip.dataset.cap))),
);
setCapacity(8);

/* ---------- FAQ smooth accordion (one open at a time) ---------- */
const faqItems = [...document.querySelectorAll('.faq-list details')];
const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

function expandFaq(details, content) {
  details.dataset.animating = 'true';
  details.setAttribute('open', '');
  const endHeight = content.scrollHeight;
  content.style.overflow = 'hidden';

  if (prefersReducedMotion) {
    content.style.overflow = '';
    delete details.dataset.animating;
    return;
  }

  const anim = content.animate(
    [
      { height: '0px', opacity: 0, paddingTop: '0px', paddingBottom: '0px' },
      { height: `${endHeight}px`, opacity: 1, paddingTop: '', paddingBottom: '' },
    ],
    { duration: 280, easing: 'cubic-bezier(0.2, 0, 0, 1)' },
  );

  anim.onfinish = () => {
    content.style.overflow = '';
    delete details.dataset.animating;
  };
}

function shrinkFaq(details, content) {
  details.dataset.animating = 'true';
  const startHeight = content.scrollHeight;
  content.style.overflow = 'hidden';

  if (prefersReducedMotion) {
    details.removeAttribute('open');
    content.style.overflow = '';
    delete details.dataset.animating;
    return;
  }

  const anim = content.animate(
    [
      { height: `${startHeight}px`, opacity: 1, paddingTop: '', paddingBottom: '' },
      { height: '0px', opacity: 0, paddingTop: '0px', paddingBottom: '0px' },
    ],
    { duration: 240, easing: 'cubic-bezier(0.2, 0, 0, 1)' },
  );

  anim.onfinish = () => {
    details.removeAttribute('open');
    content.style.overflow = '';
    delete details.dataset.animating;
  };
}

faqItems.forEach((details) => {
  const summary = details.querySelector('summary');
  const content = details.querySelector('.faq-answer');
  if (!summary || !content) return;

  summary.addEventListener('click', (e) => {
    e.preventDefault();
    if (details.dataset.animating === 'true') return;

    const isOpen = details.hasAttribute('open');
    if (isOpen) {
      shrinkFaq(details, content);
    } else {
      faqItems.forEach((other) => {
        if (other !== details && other.hasAttribute('open') && other.dataset.animating !== 'true') {
          const otherContent = other.querySelector('.faq-answer');
          if (otherContent) shrinkFaq(other, otherContent);
        }
      });
      expandFaq(details, content);
    }
  });
});

/* ---------- Live release info (graceful fallback) ---------- */
const heroVersion = document.getElementById('hero-version');
const dlVersion = document.getElementById('dl-version');
const dlDate = document.getElementById('dl-date');
const dlButton = document.getElementById('dl-button');

async function refreshRelease() {
  try {
    const res = await fetch(RELEASE_API, { headers: { Accept: 'application/vnd.github+json' } });
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    const data = await res.json();
    const tag = typeof data.tag_name === 'string' && data.tag_name ? data.tag_name : FALLBACK_TAG;
    heroVersion.textContent = tag;
    dlVersion.textContent = `SAVESX2 ${tag}`;
    if (data.published_at) {
      const date = new Date(data.published_at);
      if (!Number.isNaN(date.getTime())) {
        dlDate.textContent = `released ${date.toLocaleDateString('en-GB', {
          day: 'numeric',
          month: 'short',
          year: 'numeric',
        })}`;
      }
    }
    if (typeof data.html_url === 'string' && data.html_url) dlButton.href = data.html_url;
  } catch {
    heroVersion.textContent = FALLBACK_VERSION;
    dlVersion.textContent = `SAVESX2 ${FALLBACK_TAG}`;
    dlButton.href = RELEASES_URL;
  }
}

refreshRelease();

/* ---------- Footer year ---------- */
document.getElementById('year').textContent = String(new Date().getFullYear());

/* ---------- Format filtering ---------- */
const formatFilterBtns = document.querySelectorAll('.format-filter-btn');
const formatCards = document.querySelectorAll('.format-card');

formatFilterBtns.forEach((btn) => {
  btn.addEventListener('click', () => {
    const filter = btn.dataset.filter;
    formatFilterBtns.forEach((b) => {
      const isActive = b === btn;
      b.classList.toggle('active', isActive);
      b.setAttribute('aria-selected', String(isActive));
    });

    formatCards.forEach((card) => {
      const match = filter === 'all' || card.dataset.category === filter;
      if (match) {
        card.removeAttribute('hidden');
      } else {
        card.setAttribute('hidden', '');
      }
    });
  });
});

