/**
 * MIND OS — Application & Landing Page Interactions
 */
document.addEventListener('DOMContentLoaded', () => {
  initMobileMenu();
  initHeroTaskToggle();
  initSandboxTaskAdder();
});

/* ===== Mobile Navigation Drawer Toggle ===== */
function initMobileMenu() {
  const menuBtn = document.getElementById('mobile-menu-btn');
  const navLinks = document.getElementById('nav-links');
  const navActions = document.querySelector('.nav-actions');

  if (menuBtn) {
    menuBtn.addEventListener('click', () => {
      const isOpen = navLinks?.classList.toggle('mobile-open');
      navActions?.classList.toggle('mobile-open', isOpen);
    });
  }
}

/* ===== Hero Preview Card Task Toggle ===== */
function initHeroTaskToggle() {
  const taskList = document.getElementById('hero-task-list');
  const countEl = document.getElementById('demo-task-count');
  if (!taskList) return;

  const tasks = taskList.querySelectorAll('li');
  tasks.forEach((task) => {
    task.addEventListener('click', () => {
      task.classList.toggle('done');

      // Update count
      const total = tasks.length;
      const completed = taskList.querySelectorAll('li.done').length;
      if (countEl) {
        countEl.textContent = `${completed}/${total}`;
      }
    });
  });
}

/* ===== Live Task Sandbox Demo ===== */
function initSandboxTaskAdder() {
  const input = document.getElementById('sandbox-input');
  const btn = document.getElementById('sandbox-add-btn');
  const container = document.getElementById('sandbox-task-container');
  if (!input || !btn || !container) return;

  const pastelThemes = [
    { bg: 'var(--pastel-lavender-bg)', color: 'var(--pastel-lavender-text)', border: 'var(--pastel-lavender-border)' },
    { bg: 'var(--pastel-mint-bg)', color: 'var(--pastel-mint-text)', border: 'var(--pastel-mint-border)' },
    { bg: 'var(--pastel-sky-bg)', color: 'var(--pastel-sky-text)', border: 'var(--pastel-sky-border)' },
    { bg: 'var(--pastel-peach-bg)', color: 'var(--pastel-peach-text)', border: 'var(--pastel-peach-border)' },
    { bg: 'var(--pastel-pink-bg)', color: 'var(--pastel-pink-text)', border: 'var(--pastel-pink-border)' }
  ];

  let themeIndex = 0;

  function addTask() {
    const text = input.value.trim();
    if (!text) return;

    const theme = pastelThemes[themeIndex % pastelThemes.length];
    themeIndex++;

    const badge = document.createElement('span');
    badge.className = 'sandbox-tag-btn';
    badge.style.background = theme.bg;
    badge.style.color = theme.color;
    badge.style.borderColor = theme.border;
    badge.style.animation = 'fadeIn 0.3s ease';
    badge.textContent = text;

    // Click to remove or toggle
    badge.addEventListener('click', () => {
      badge.style.opacity = '0.5';
      badge.style.textDecoration = 'line-through';
      setTimeout(() => badge.remove(), 600);
    });

    container.appendChild(badge);
    input.value = '';
  }

  btn.addEventListener('click', addTask);
  input.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') addTask();
  });
}
