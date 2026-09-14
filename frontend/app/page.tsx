'use client';
import Link from 'next/link';
import { useState } from 'react';
import { Target, CheckSquare, RefreshCw, BarChart2, Lock, Clock } from 'lucide-react';

const FEATURES = [
  { icon: <Target size={24} />, title: 'Focus Engine', desc: 'Timed focus blocks with soft ambient cues to keep you in the flow zone effortlessly.', color: 'lavender' },
  { icon: <CheckSquare size={24} />, title: 'Smart Tasks', desc: 'Organize your day with colorful pastel priority tags, due dates, and clean checklists.', color: 'mint' },
  { icon: <RefreshCw size={24} />, title: 'Habit Streaks', desc: 'Build healthy daily routines with gentle visual streaks that celebrate small daily wins.', color: 'peach' },
  { icon: <BarChart2 size={24} />, title: 'Clarity Insights', desc: 'Understand your peak focus hours through beautifully clean weekly progress charts.', color: 'sky' },
  { icon: <Lock size={24} />, title: 'Private Vault', desc: 'Keep your thoughts, reflections, and personal journals 100% private and protected.', color: 'pink' },
  { icon: <Clock size={24} />, title: 'MindFlow AI', desc: 'Get helpful productivity suggestions and schedule advice whenever you need a boost.', color: 'yellow' },
];

const STEPS = [
  { num: 'Step 01', title: 'Quick Account Signup', desc: 'Enter your name and mobile number. No email or credit card required.' },
  { num: 'Step 02', title: 'Personalize Your Dashboard', desc: 'Choose your favorite pastel color themes and set up your initial daily targets.' },
  { num: 'Step 03', title: 'Enjoy Distraction-Free Work', desc: 'Launch your first focus session and watch your productivity improve steadily.' },
];

const TESTIMONIALS = [
  { name: 'Ananya Rao', role: 'Design Student', initials: 'AR', quote: 'The pastel aesthetics are so soothing. Unlike other noisy task apps, MIND OS makes me feel relaxed whenever I open my dashboard.' },
  { name: 'Siddharth Kumar', role: 'Software Developer', initials: 'SK', quote: 'Having the habit tracker and focus timer in one simple pastel view has completely transformed my daily study routine.' },
  { name: 'Pooja Mehta', role: 'Content Creator', initials: 'PM', quote: 'Super easy to log in with mobile number. The MindFlow AI suggestions help me schedule tasks during my highest energy hours.' },
];

const INITIAL_TAGS = [
  { text: 'Finish project presentation', bg: 'var(--pastel-lavender-bg)', color: 'var(--pastel-lavender-text)', border: 'var(--pastel-lavender-border)' },
  { text: '20-min evening walk', bg: 'var(--pastel-mint-bg)', color: 'var(--pastel-mint-text)', border: 'var(--pastel-mint-border)' },
  { text: 'Read 15 pages', bg: 'var(--pastel-peach-bg)', color: 'var(--pastel-peach-text)', border: 'var(--pastel-peach-border)' },
];

const PALETTE = [
  { bg: 'var(--pastel-lavender-bg)', color: 'var(--pastel-lavender-text)', border: 'var(--pastel-lavender-border)' },
  { bg: 'var(--pastel-mint-bg)', color: 'var(--pastel-mint-text)', border: 'var(--pastel-mint-border)' },
  { bg: 'var(--pastel-sky-bg)', color: 'var(--pastel-sky-text)', border: 'var(--pastel-sky-border)' },
  { bg: 'var(--pastel-peach-bg)', color: 'var(--pastel-peach-text)', border: 'var(--pastel-peach-border)' },
  { bg: 'var(--pastel-pink-bg)', color: 'var(--pastel-pink-text)', border: 'var(--pastel-pink-border)' },
];

export default function HomePage() {
  const [demoTasks, setDemoTasks] = useState([
    { id: 1, text: 'Morning mindfulness & journal', done: true },
    { id: 2, text: 'Review weekly priority goals', done: true },
    { id: 3, text: 'Deep focus study session (45m)', done: true },
    { id: 4, text: "Plan tomorrow's roadmap", done: false },
  ]);
  const [tags, setTags] = useState(INITIAL_TAGS);
  const [input, setInput] = useState('');
  const [palette, setPalette] = useState(0);

  const toggleTask = (id: number) => {
    setDemoTasks(tasks => tasks.map(t => t.id === id ? { ...t, done: !t.done } : t));
  };

  const addTag = () => {
    if (!input.trim()) return;
    const theme = PALETTE[palette % PALETTE.length];
    setTags(prev => [...prev, { text: input.trim(), ...theme }]);
    setPalette(p => p + 1);
    setInput('');
  };

  const removeTag = (i: number) => setTags(prev => prev.filter((_, idx) => idx !== i));
  const doneCount = demoTasks.filter(t => t.done).length;

  return (
    <>
      {/* Navbar */}
      <header className="navbar">
        <div className="container nav-inner">
          <Link href="/" className="logo">
            <span className="logo-mark">M</span>
            MIND OS
          </Link>
          <nav className="nav-links">
            <Link href="#features">Features</Link>
            <Link href="#demo">Live Demo</Link>
            <Link href="#how-it-works">How it works</Link>
            <Link href="#testimonials">Reviews</Link>
          </nav>
          <div className="nav-actions">
            <Link href="/login" className="btn btn-ghost">Log in</Link>
            <Link href="/login?mode=register" className="btn btn-primary">Get started free</Link>
          </div>
        </div>
      </header>

      <main>
        {/* Hero */}
        <section className="hero">
          <div className="hero-blobs" aria-hidden="true">
            <span className="blob blob-1" /><span className="blob blob-2" /><span className="blob blob-3" />
          </div>
          <div className="container hero-grid">
            <div>
              <div className="badge"><span className="badge-dot" /> Your space for daily clarity</div>
              <h1>A softer, serene way to<br /><span className="gradient-text">organize your mind.</span></h1>
              <p className="hero-subtitle">MIND OS helps you structure daily tasks, build daily habits, and enjoy calm focus sessions wrapped in a professional pastel interface.</p>
              <div className="hero-cta">
                <Link href="/login?mode=register" className="btn btn-primary btn-lg">Start free today →</Link>
                <Link href="#demo" className="btn btn-soft btn-lg">Try interactive demo</Link>
              </div>
              <div className="hero-trust">
                <div className="trust-avatars">
                  <span>AN</span><span>SK</span><span>PM</span><span>+</span>
                </div>
                <p>Trusted by 10,000+ students &amp; professionals</p>
              </div>
            </div>

            {/* Interactive Hero Card */}
            <div className="hero-visual">
              <div className="preview-stack">
                <div className="preview-card">
                  <div className="preview-top">
                    <span className="preview-label">Today&apos;s Focus</span>
                    <span className="preview-pill">Focus Mode Active</span>
                  </div>
                  <p className="preview-greeting">Good morning, Nikitha</p>
                  <div className="preview-stats">
                    <div className="preview-stat preview-stat-lavender"><span>Tasks Done</span><strong>{doneCount}/{demoTasks.length}</strong></div>
                    <div className="preview-stat preview-stat-mint"><span>Habit Streak</span><strong>7 Days</strong></div>
                    <div className="preview-stat preview-stat-peach"><span>Focus Score</span><strong>92%</strong></div>
                  </div>
                  <ul className="preview-tasks">
                    {demoTasks.map(task => (
                      <li key={task.id} className={task.done ? 'preview-task-done' : ''} onClick={() => toggleTask(task.id)} style={{ cursor: 'pointer' }}>
                        <span className={`task-circle ${task.done ? 'task-circle-done' : ''}`} />
                        {task.text}
                      </li>
                    ))}
                  </ul>
                </div>
                <div className="preview-float float-pink">Clarity mode active</div>
                <div className="preview-float float-sky">{doneCount}/{demoTasks.length} tasks completed</div>
              </div>
            </div>
          </div>
        </section>

        {/* Interactive Sandbox */}
        <section id="demo" className="sandbox-section">
          <div className="container">
            <div className="sandbox-box">
              <div className="sandbox-title">
                <span className="section-tag">Interactive Sandbox</span>
                <h3>Test MIND OS Task Creator</h3>
                <p>Experience how instant and calm adding tasks feels in our pastel workspace.</p>
              </div>
              <div className="sandbox-controls">
                <input
                  className="sandbox-input"
                  placeholder="Type a task and press Enter or click Add..."
                  value={input}
                  onChange={e => setInput(e.target.value)}
                  onKeyDown={e => e.key === 'Enter' && addTag()}
                />
                <button className="btn btn-primary" onClick={addTag}>+ Add Task</button>
              </div>
              <div className="sandbox-tags">
                {tags.map((tag, i) => (
                  <span
                    key={i}
                    className="sandbox-tag"
                    style={{ background: tag.bg, color: tag.color, borderColor: tag.border }}
                    onClick={() => removeTag(i)}
                    title="Click to remove"
                  >
                    {tag.text}
                  </span>
                ))}
              </div>
            </div>
          </div>
        </section>

        {/* Features */}
        <section id="features" className="features">
          <div className="container">
            <div className="section-header">
              <span className="section-tag">Features</span>
              <h2>Everything you need for peace of mind</h2>
              <p>Carefully crafted pastel tools designed for deep focus without digital clutter.</p>
            </div>
            <div className="features-grid">
              {FEATURES.map((f, i) => (
                <article key={i} className={`feature-card ${f.color}`}>
                  <div className="feature-icon-wrap">{f.icon}</div>
                  <h3>{f.title}</h3>
                  <p>{f.desc}</p>
                </article>
              ))}
            </div>
          </div>
        </section>

        {/* How It Works */}
        <section id="how-it-works" className="how-it-works">
          <div className="container">
            <div className="section-header">
              <span className="section-tag">Simple Setup</span>
              <h2>Up and running in 3 easy steps</h2>
              <p>No complicated setup — start organizing your day in under a minute.</p>
            </div>
            <div className="steps-grid">
              {STEPS.map((s, i) => (
                <article key={i} className="step-card">
                  <span className="step-num">{s.num}</span>
                  <h3>{s.title}</h3>
                  <p>{s.desc}</p>
                </article>
              ))}
            </div>
          </div>
        </section>

        {/* Testimonials */}
        <section id="testimonials" className="testimonials">
          <div className="container">
            <div className="section-header">
              <span className="section-tag">Community</span>
              <h2>Loved by students &amp; professionals</h2>
              <p>Here is how MIND OS is helping people find clarity and balance daily.</p>
            </div>
            <div className="testimonials-grid">
              {TESTIMONIALS.map((t, i) => (
                <article key={i} className="testimonial-card">
                  <span className="testimonial-badge">VERIFIED REVIEW</span>
                  <p className="testimonial-quote">&quot;{t.quote}&quot;</p>
                  <div className="testimonial-user">
                    <div className="user-avatar">{t.initials}</div>
                    <div className="user-info"><h4>{t.name}</h4><span>{t.role}</span></div>
                  </div>
                </article>
              ))}
            </div>
          </div>
        </section>

        {/* CTA */}
        <section className="cta-section">
          <div className="container">
            <div className="cta-box">
              <h2>Ready for a calmer, clearer mind?</h2>
              <p>Join MIND OS today — it&apos;s 100% free, gentle, and designed specifically for your focus.</p>
              <Link href="/login?mode=register" className="btn btn-primary btn-lg">Create your free account →</Link>
            </div>
          </div>
        </section>
      </main>

      {/* Footer */}
      <footer className="footer">
        <div className="container footer-inner">
          <div className="footer-brand">
            <div className="logo"><span className="logo-mark logo-mark-sm">M</span> MIND OS</div>
            <p>Your gentle mental operating system.</p>
          </div>
          <div className="footer-links">
            <Link href="#features">Features</Link>
            <Link href="/login">Login</Link>
            <Link href="/login?mode=register">Register</Link>
          </div>
          <p className="footer-copy">&copy; 2026 MIND OS. All rights reserved.</p>
        </div>
      </footer>
    </>
  );
}
