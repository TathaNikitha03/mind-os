'use client';

// ============================================================
// MIND OS — Auth Utility (localStorage-based)
// ============================================================

export interface User {
  name: string;
  mobile: string;
  loggedInAt: string;
}

interface StoredUser {
  name: string;
  mobile: string;
  password: string;
  createdAt: string;
}

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8081';

const USERS_KEY = 'mind_os_users';
const SESSION_KEY = 'mind_os_session';

function getUsers(): StoredUser[] {
  if (typeof window === 'undefined') return [];
  try {
    return JSON.parse(localStorage.getItem(USERS_KEY) || '[]');
  } catch {
    return [];
  }
}

function saveUsers(users: StoredUser[]) {
  localStorage.setItem(USERS_KEY, JSON.stringify(users));
}

function normalizeMobile(mobile: string) {
  return mobile.replace(/\D/g, '');
}

export async function register(name: string, mobile: string, password: string): Promise<{ success: boolean; message: string }> {
  const trimmedName = name.trim();
  const nm = normalizeMobile(mobile);

  if (!trimmedName || trimmedName.length < 2)
    return { success: false, message: 'Please enter a valid name (at least 2 characters).' };
  if (nm.length < 10 || nm.length > 15)
    return { success: false, message: 'Please enter a valid mobile number (10–15 digits).' };
  if (password.length < 6)
    return { success: false, message: 'Password must be at least 6 characters.' };

  // 1. Register with Spring Boot backend (PostgreSQL database)
  try {
    const res = await fetch(`${API_BASE_URL}/api/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name: trimmedName, mobile: nm, password }),
    });

    const data = await res.json().catch(() => ({}));
    if (res.ok && data.success) {
      // Also cache in browser for fast offline access
      const users = getUsers();
      if (!users.find(u => u.mobile === nm)) {
        users.push({ name: trimmedName, mobile: nm, password, createdAt: new Date().toISOString() });
        saveUsers(users);
      }
      return { success: true, message: data.message || 'Account created successfully! You can now log in.' };
    } else if (!res.ok) {
      return { success: false, message: data.message || 'Registration failed. Please check your details.' };
    }
  } catch (err) {
    console.warn('Backend registration unavailable, falling back to local cache:', err);
  }

  // Fallback to local storage
  const users = getUsers();
  if (users.find(u => u.mobile === nm))
    return { success: false, message: 'This mobile number is already registered. Please log in.' };

  users.push({ name: trimmedName, mobile: nm, password, createdAt: new Date().toISOString() });
  saveUsers(users);
  return { success: true, message: 'Account created successfully! You can now log in.' };
}

export async function login(mobile: string, password: string): Promise<{ success: boolean; message?: string; user?: User }> {
  const nm = normalizeMobile(mobile);

  // 1. Authenticate against Spring Boot backend (PostgreSQL database)
  try {
    const res = await fetch(`${API_BASE_URL}/api/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ mobile: nm, password }),
    });

    const data = await res.json().catch(() => ({}));
    if (res.ok && data.success) {
      const session: User = {
        name: data.name || 'User',
        mobile: data.mobile || nm,
        loggedInAt: new Date().toISOString(),
      };
      localStorage.setItem(SESSION_KEY, JSON.stringify(session));

      // Cache user details locally
      const users = getUsers();
      if (!users.find(u => u.mobile === nm)) {
        users.push({ name: session.name, mobile: nm, password, createdAt: new Date().toISOString() });
        saveUsers(users);
      }

      return { success: true, user: session };
    } else if (res.status === 400 || res.status === 401) {
      return { success: false, message: data.message || 'Invalid mobile number or password.' };
    }
  } catch (err) {
    console.warn('Backend login unavailable, attempting local cache fallback:', err);
  }

  // Fallback to local storage
  const users = getUsers();
  const user = users.find(u => u.mobile === nm && u.password === password);
  if (!user) return { success: false, message: 'Invalid mobile number or password.' };

  const session: User = { name: user.name, mobile: user.mobile, loggedInAt: new Date().toISOString() };
  localStorage.setItem(SESSION_KEY, JSON.stringify(session));
  return { success: true, user: session };
}

export function logout() {
  if (typeof window !== 'undefined') localStorage.removeItem(SESSION_KEY);
}

export function getCurrentUser(): User | null {
  if (typeof window === 'undefined') return null;
  try {
    return JSON.parse(localStorage.getItem(SESSION_KEY) || 'null');
  } catch {
    return null;
  }
}

export function isLoggedIn(): boolean {
  return getCurrentUser() !== null;
}

export function getMemberSince(mobile: string): string {
  const users = getUsers();
  const user = users.find(u => u.mobile === mobile);
  if (user?.createdAt) {
    return new Date(user.createdAt).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' });
  }
  return 'Today';
}
