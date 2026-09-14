document.addEventListener('DOMContentLoaded', () => {
  if (!Auth.requireAuth()) return;

  const user = Auth.getCurrentUser();
  initNavbar(user);
  initSidebar();
  initGreeting(user);
  initProfile(user);
});

function initNavbar(user) {
  const greetingEl = document.getElementById('user-greeting');
  const logoutBtn = document.getElementById('logout-btn');

  if (greetingEl) {
    greetingEl.textContent = `Hi, ${user.name}`;
  }

  if (logoutBtn) {
    logoutBtn.addEventListener('click', () => {
      Auth.logout();
      window.location.href = 'login.html';
    });
  }
}

function initSidebar() {
  const links = document.querySelectorAll('.sidebar-link');
  const panels = document.querySelectorAll('.content-panel');

  links.forEach((link) => {
    link.addEventListener('click', () => {
      const panelId = link.dataset.panel;

      links.forEach((l) => l.classList.remove('active'));
      panels.forEach((p) => p.classList.remove('active'));

      link.classList.add('active');
      const panel = document.getElementById(`panel-${panelId}`);
      if (panel) panel.classList.add('active');
    });
  });
}

function initGreeting(user) {
  const greetingEl = document.getElementById('time-greeting');
  const subtitleEl = document.getElementById('greeting-subtitle');
  if (!greetingEl) return;

  const hour = new Date().getHours();
  let timeGreeting = 'Good Evening';

  if (hour < 12) timeGreeting = 'Good Morning';
  else if (hour < 17) timeGreeting = 'Good Afternoon';

  greetingEl.textContent = `${timeGreeting}, ${user.name}`;

  if (subtitleEl) {
    subtitleEl.textContent = "Here's what's on your schedule today.";
  }
}

function initProfile(user) {
  const initial = user.name.charAt(0).toUpperCase();
  const avatar = document.getElementById('profile-avatar');
  const name = document.getElementById('profile-name');
  const mobile = document.getElementById('profile-mobile');
  const mobileDetail = document.getElementById('profile-mobile-detail');
  const since = document.getElementById('profile-since');

  if (avatar) avatar.textContent = initial;
  if (name) name.textContent = user.name;
  if (mobile) mobile.textContent = user.mobile;
  if (mobileDetail) mobileDetail.textContent = user.mobile;

  if (since) {
    const users = JSON.parse(localStorage.getItem('mind_os_users') || '[]');
    const account = users.find((u) => u.mobile === user.mobile);
    if (account?.createdAt) {
      since.textContent = new Date(account.createdAt).toLocaleDateString('en-IN', {
        day: 'numeric',
        month: 'short',
        year: 'numeric',
      });
    } else {
      since.textContent = 'Today';
    }
  }
}
