/**
 * MIND OS — Authentication Module
 * Uses localStorage for demo user registration and login.
 */

const Auth = (() => {
  const USERS_KEY = 'mind_os_users';
  const SESSION_KEY = 'mind_os_session';

  function getUsers() {
    try {
      return JSON.parse(localStorage.getItem(USERS_KEY)) || [];
    } catch {
      return [];
    }
  }

  function saveUsers(users) {
    localStorage.setItem(USERS_KEY, JSON.stringify(users));
  }

  function normalizeMobile(mobile) {
    return mobile.replace(/\D/g, '');
  }

  function validateMobile(mobile) {
    const digits = normalizeMobile(mobile);
    return digits.length >= 10 && digits.length <= 15;
  }

  function register(name, mobile, password) {
    const trimmedName = name.trim();
    const normalizedMobile = normalizeMobile(mobile);

    if (!trimmedName || trimmedName.length < 2) {
      return { success: false, message: 'Please enter a valid name (at least 2 characters).' };
    }

    if (!validateMobile(mobile)) {
      return { success: false, message: 'Please enter a valid mobile number (10–15 digits).' };
    }

    if (password.length < 6) {
      return { success: false, message: 'Password must be at least 6 characters.' };
    }

    const users = getUsers();
    const exists = users.find((u) => u.mobile === normalizedMobile);

    if (exists) {
      return { success: false, message: 'This mobile number is already registered. Please log in.' };
    }

    users.push({
      name: trimmedName,
      mobile: normalizedMobile,
      password,
      createdAt: new Date().toISOString(),
    });

    saveUsers(users);
    return { success: true, message: 'Account created successfully! You can now log in.' };
  }

  function login(mobile, password) {
    const normalizedMobile = normalizeMobile(mobile);
    const users = getUsers();
    const user = users.find(
      (u) => u.mobile === normalizedMobile && u.password === password
    );

    if (!user) {
      return { success: false, message: 'Invalid mobile number or password.' };
    }

    const session = {
      name: user.name,
      mobile: user.mobile,
      loggedInAt: new Date().toISOString(),
    };

    localStorage.setItem(SESSION_KEY, JSON.stringify(session));
    return { success: true, user: session };
  }

  function logout() {
    localStorage.removeItem(SESSION_KEY);
  }

  function getCurrentUser() {
    try {
      return JSON.parse(localStorage.getItem(SESSION_KEY));
    } catch {
      return null;
    }
  }

  function isLoggedIn() {
    return getCurrentUser() !== null;
  }

  function requireAuth() {
    if (!isLoggedIn()) {
      window.location.href = 'login.html';
      return false;
    }
    return true;
  }

  return { register, login, logout, getCurrentUser, isLoggedIn, requireAuth };
})();

/* ===== Login Page UI Logic ===== */
document.addEventListener('DOMContentLoaded', () => {
  const loginForm = document.getElementById('login-form');
  const registerForm = document.getElementById('register-form');
  if (!loginForm || !registerForm) return;

  const tabs = document.querySelectorAll('.auth-tab');
  const switchBtns = document.querySelectorAll('[data-switch]');

  function switchTab(tabName) {
    tabs.forEach((t) => t.classList.toggle('active', t.dataset.tab === tabName));
    loginForm.classList.toggle('active', tabName === 'login');
    registerForm.classList.toggle('active', tabName === 'register');
    clearMessages();
  }

  function clearMessages() {
    ['login-error', 'register-error', 'register-success'].forEach((id) => {
      const el = document.getElementById(id);
      if (el) {
        el.hidden = true;
        el.textContent = '';
      }
    });
  }

  function showError(id, message) {
    const el = document.getElementById(id);
    if (el) {
      el.textContent = message;
      el.hidden = false;
    }
  }

  function showSuccess(id, message) {
    const el = document.getElementById(id);
    if (el) {
      el.textContent = message;
      el.hidden = false;
    }
  }

  tabs.forEach((tab) => {
    tab.addEventListener('click', () => switchTab(tab.dataset.tab));
  });

  switchBtns.forEach((btn) => {
    btn.addEventListener('click', () => switchTab(btn.dataset.switch));
  });

  document.querySelectorAll('.toggle-password').forEach((btn) => {
    btn.addEventListener('click', () => {
      const input = btn.previousElementSibling;
      const isPassword = input.type === 'password';
      input.type = isPassword ? 'text' : 'password';
      btn.textContent = isPassword ? 'Hide' : 'Show';
    });
  });

  const params = new URLSearchParams(window.location.search);
  if (params.get('mode') === 'register') {
    switchTab('register');
  }

  if (Auth.isLoggedIn()) {
    window.location.href = 'dashboard.html';
    return;
  }

  loginForm.addEventListener('submit', (e) => {
    e.preventDefault();
    clearMessages();

    const mobile = document.getElementById('login-mobile').value;
    const password = document.getElementById('login-password').value;

    const result = Auth.login(mobile, password);

    if (result.success) {
      window.location.href = 'dashboard.html';
    } else {
      showError('login-error', result.message);
    }
  });

  registerForm.addEventListener('submit', (e) => {
    e.preventDefault();
    clearMessages();

    const name = document.getElementById('register-name').value;
    const mobile = document.getElementById('register-mobile').value;
    const password = document.getElementById('register-password').value;
    const confirm = document.getElementById('register-confirm').value;

    if (password !== confirm) {
      showError('register-error', 'Passwords do not match.');
      return;
    }

    const result = Auth.register(name, mobile, password);

    if (result.success) {
      showSuccess('register-success', result.message);
      registerForm.reset();
      setTimeout(() => switchTab('login'), 1500);
    } else {
      showError('register-error', result.message);
    }
  });
});
