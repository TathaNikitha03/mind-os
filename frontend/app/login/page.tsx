'use client';

import { Suspense } from 'react';
import { useState, useEffect } from 'react';
import Link from 'next/link';
import { useRouter, useSearchParams } from 'next/navigation';
import { Eye, EyeOff, ArrowLeft } from 'lucide-react';
import { login, register, isLoggedIn } from '@/lib/auth';

function LoginForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [activeTab, setActiveTab] = useState<'login' | 'register'>('login');

  // Login form state
  const [loginMobile, setLoginMobile] = useState('');
  const [loginPassword, setLoginPassword] = useState('');
  const [showLoginPw, setShowLoginPw] = useState(false);
  const [loginError, setLoginError] = useState('');

  const [isSubmitting, setIsSubmitting] = useState(false);

  // Register form state
  const [regName, setRegName] = useState('');
  const [regMobile, setRegMobile] = useState('');
  const [regPassword, setRegPassword] = useState('');
  const [regConfirm, setRegConfirm] = useState('');
  const [showRegPw, setShowRegPw] = useState(false);
  const [regError, setRegError] = useState('');
  const [regSuccess, setRegSuccess] = useState('');

  useEffect(() => {
    if (isLoggedIn()) { router.replace('/dashboard'); return; }
    if (searchParams.get('mode') === 'register') setActiveTab('register');
  }, [router, searchParams]);

  async function handleLogin(e: React.FormEvent) {
    e.preventDefault();
    setLoginError('');
    setIsSubmitting(true);
    try {
      const result = await login(loginMobile, loginPassword);
      if (result.success) {
        router.replace('/dashboard');
      } else {
        setLoginError(result.message || 'Login failed. Please check your credentials.');
      }
    } catch (err: any) {
      setLoginError(err.message || 'An error occurred during login.');
    } finally {
      setIsSubmitting(false);
    }
  }

  async function handleRegister(e: React.FormEvent) {
    e.preventDefault();
    setRegError(''); setRegSuccess('');
    if (regPassword !== regConfirm) { setRegError('Passwords do not match.'); return; }
    setIsSubmitting(true);
    try {
      const result = await register(regName, regMobile, regPassword);
      if (result.success) {
        setRegSuccess(result.message);
        setRegName(''); setRegMobile(''); setRegPassword(''); setRegConfirm('');
        setTimeout(() => { setActiveTab('login'); setRegSuccess(''); }, 1500);
      } else {
        setRegError(result.message || 'Registration failed.');
      }
    } catch (err: any) {
      setRegError(err.message || 'An error occurred during registration.');
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className="auth-page">
      {/* Top Navbar */}
      <header className="navbar">
        <div className="container nav-inner">
          <Link href="/" className="logo">
            <span className="logo-mark">M</span>
            MIND OS
          </Link>
          <nav className="nav-links">
            <Link href="/#features">Features</Link>
            <Link href="/#how-it-works">How it works</Link>
            <Link href="/#testimonials">Reviews</Link>
          </nav>
          <div className="nav-actions">
            <Link href="/" className="btn btn-soft">
              <ArrowLeft size={16} />
              Back to Landing Page
            </Link>
          </div>
        </div>
      </header>

      <div className="auth-layout">
        {/* Left Brand Panel */}
        <aside className="auth-brand">
          <div>
            <Link href="/" className="logo">
              <span className="logo-mark">M</span>
              MIND OS
            </Link>
            <div className="auth-brand-content">
              <h2>Welcome back to your calm workspace.</h2>
              <p>Sign in to pick up where you left off — tasks, habits, and focus sessions waiting for you.</p>
              <ul className="auth-perks">
                <li>Track daily tasks effortlessly</li>
                <li>Build meaningful daily habits</li>
                <li>AI-powered schedule insights</li>
              </ul>
            </div>
          </div>
          <Link href="/" className="back-home-link">
            <ArrowLeft size={16} />
            Back to landing page
          </Link>
        </aside>

        {/* Right Auth Card */}
        <div>
          <div className="auth-card">
            <div className="auth-card-top-nav">
              <span>MIND OS AUTH</span>
              <Link href="/" className="auth-card-back-btn">
                <ArrowLeft size={14} />
                Landing Page
              </Link>
            </div>

            {/* Tab Switcher */}
            <div className="auth-tabs">
              <button
                className={`auth-tab ${activeTab === 'login' ? 'auth-tab-active' : ''}`}
                onClick={() => { setActiveTab('login'); setLoginError(''); }}
              >Log in</button>
              <button
                className={`auth-tab ${activeTab === 'register' ? 'auth-tab-active' : ''}`}
                onClick={() => { setActiveTab('register'); setRegError(''); setRegSuccess(''); }}
              >Register</button>
            </div>

            {/* Login Form */}
            {activeTab === 'login' && (
              <form className="auth-form fade-in" onSubmit={handleLogin}>
                <h2>Welcome back</h2>
                <p className="auth-subtitle">Enter your registered mobile number and password.</p>

                <div className="form-group">
                  <label htmlFor="login-mobile">Mobile number</label>
                  <input id="login-mobile" type="tel" className="form-input" placeholder="e.g. 9876543210"
                    value={loginMobile} onChange={e => setLoginMobile(e.target.value)} required />
                </div>

                <div className="form-group">
                  <label htmlFor="login-password">Password</label>
                  <div className="password-field">
                    <input id="login-password" type={showLoginPw ? 'text' : 'password'} className="form-input"
                      placeholder="Enter your password" value={loginPassword}
                      onChange={e => setLoginPassword(e.target.value)} required />
                    <button type="button" className="toggle-pw" onClick={() => setShowLoginPw(p => !p)}>
                      {showLoginPw ? <EyeOff size={16} /> : <Eye size={16} />}
                    </button>
                  </div>
                </div>

                {loginError && <div className="form-error">{loginError}</div>}
                <button type="submit" className="btn btn-primary btn-full" disabled={isSubmitting}>
                  {isSubmitting ? 'Logging in...' : 'Log in to MIND OS'}
                </button>

                <p className="auth-switch">
                  Don&apos;t have an account?{' '}
                  <button type="button" className="link-btn" onClick={() => setActiveTab('register')}>Register here</button>
                </p>
              </form>
            )}

            {/* Register Form */}
            {activeTab === 'register' && (
              <form className="auth-form fade-in" onSubmit={handleRegister}>
                <h2>Create account</h2>
                <p className="auth-subtitle">Register with your name and mobile number in 30 seconds.</p>

                <div className="form-group">
                  <label htmlFor="reg-name">Full name</label>
                  <input id="reg-name" type="text" className="form-input" placeholder="Enter your full name"
                    value={regName} onChange={e => setRegName(e.target.value)} required />
                </div>

                <div className="form-group">
                  <label htmlFor="reg-mobile">Mobile number</label>
                  <input id="reg-mobile" type="tel" className="form-input" placeholder="Enter mobile number (10–15 digits)"
                    value={regMobile} onChange={e => setRegMobile(e.target.value)} required />
                </div>

                <div className="form-group">
                  <label htmlFor="reg-password">Create password</label>
                  <div className="password-field">
                    <input id="reg-password" type={showRegPw ? 'text' : 'password'} className="form-input"
                      placeholder="Minimum 6 characters" value={regPassword}
                      onChange={e => setRegPassword(e.target.value)} minLength={6} required />
                    <button type="button" className="toggle-pw" onClick={() => setShowRegPw(p => !p)}>
                      {showRegPw ? <EyeOff size={16} /> : <Eye size={16} />}
                    </button>
                  </div>
                </div>

                <div className="form-group">
                  <label htmlFor="reg-confirm">Confirm password</label>
                  <input id="reg-confirm" type="password" className="form-input"
                    placeholder="Re-enter your password" value={regConfirm}
                    onChange={e => setRegConfirm(e.target.value)} required />
                </div>

                {regError && <div className="form-error">{regError}</div>}
                {regSuccess && <div className="form-success">{regSuccess}</div>}
                <button type="submit" className="btn btn-primary btn-full" disabled={isSubmitting}>
                  {isSubmitting ? 'Creating account...' : 'Create Free Account'}
                </button>

                <p className="auth-switch">
                  Already have an account?{' '}
                  <button type="button" className="link-btn" onClick={() => setActiveTab('login')}>Log in here</button>
                </p>
              </form>
            )}

            <Link href="/" className="auth-home-footer">Return to MIND OS Landing Page</Link>
          </div>
        </div>
      </div>
    </div>
  );
}

export default function LoginPage() {
  return (
    <Suspense fallback={<div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>Loading...</div>}>
      <LoginForm />
    </Suspense>
  );
}
