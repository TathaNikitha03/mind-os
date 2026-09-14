'use client';

import { useState, useMemo } from 'react';
import { Plus, Search, ChevronDown, X, Calendar, Tag, CheckCircle2, Circle, Clock, AlertCircle, Minus } from 'lucide-react';

// ─────────────────────────────────────────────────────
// Types
// ─────────────────────────────────────────────────────
type Priority = 'HIGH' | 'MEDIUM' | 'LOW';
type Status   = 'TODO' | 'IN_PROGRESS' | 'COMPLETED';
type Tab      = 'ALL' | 'TODAY' | 'UPCOMING' | 'COMPLETED';

interface Task {
  id: number;
  title: string;
  description: string;
  priority: Priority;
  status: Status;
  dueDate: string; // ISO date string
  category: string;
  tags: string[];
}

// ─────────────────────────────────────────────────────
// Seed Data
// ─────────────────────────────────────────────────────
const today = new Date();
const fmt = (d: Date) => d.toISOString().split('T')[0];
const addDays = (n: number) => { const d = new Date(today); d.setDate(d.getDate() + n); return fmt(d); };

const SEED_TASKS: Task[] = [
  { id: 1, title: 'Complete project report',      description: 'Final year project documentation', priority: 'HIGH',   status: 'TODO',        dueDate: addDays(7),  category: 'Academic',  tags: ['project', 'docs'] },
  { id: 2, title: 'Prepare presentation',          description: 'Project presentation slides',      priority: 'MEDIUM', status: 'IN_PROGRESS', dueDate: addDays(10), category: 'Academic',  tags: ['slides'] },
  { id: 3, title: 'Complete database schema',      description: 'MindFlow project DB setup',        priority: 'LOW',    status: 'COMPLETED',   dueDate: fmt(today),  category: 'Dev',       tags: ['database'] },
  { id: 4, title: 'Morning focus session',         description: 'Deep work — 45 min block',         priority: 'MEDIUM', status: 'TODO',        dueDate: fmt(today),  category: 'Wellness',  tags: ['focus'] },
  { id: 5, title: 'Review meeting notes',          description: 'Summarize action items from call', priority: 'HIGH',   status: 'TODO',        dueDate: fmt(today),  category: 'Work',      tags: ['notes'] },
  { id: 6, title: 'Set weekly goals',              description: 'Plan next 7 days objectives',      priority: 'MEDIUM', status: 'COMPLETED',   dueDate: addDays(-1), category: 'Planning',  tags: ['goals'] },
  { id: 7, title: 'Read chapter 4',               description: 'Data structures textbook',          priority: 'LOW',    status: 'IN_PROGRESS', dueDate: addDays(2),  category: 'Academic',  tags: ['reading'] },
  { id: 8, title: 'Submit assignment',             description: 'Operating systems lab report',     priority: 'HIGH',   status: 'TODO',        dueDate: addDays(1),  category: 'Academic',  tags: ['assignment'] },
];

// ─────────────────────────────────────────────────────
// Priority / Status helpers
// ─────────────────────────────────────────────────────
const PRIORITY_STYLE: Record<Priority, { bg: string; color: string; border: string; icon: React.ReactNode }> = {
  HIGH:   { bg: 'var(--pastel-pink-bg)',   color: 'var(--pastel-pink-text)',   border: 'var(--pastel-pink-border)',   icon: <AlertCircle size={12} /> },
  MEDIUM: { bg: 'var(--pastel-peach-bg)',  color: 'var(--pastel-peach-text)',  border: 'var(--pastel-peach-border)',  icon: <Minus size={12} /> },
  LOW:    { bg: 'var(--pastel-mint-bg)',   color: 'var(--pastel-mint-text)',   border: 'var(--pastel-mint-border)',   icon: <ChevronDown size={12} /> },
};

const STATUS_STYLE: Record<Status, { bg: string; color: string; border: string; label: string }> = {
  TODO:        { bg: 'var(--pastel-lavender-bg)', color: 'var(--pastel-lavender-text)', border: 'var(--pastel-lavender-border)', label: 'To Do' },
  IN_PROGRESS: { bg: 'var(--pastel-sky-bg)',      color: 'var(--pastel-sky-text)',      border: 'var(--pastel-sky-border)',      label: 'In Progress' },
  COMPLETED:   { bg: 'var(--pastel-mint-bg)',     color: 'var(--pastel-mint-text)',     border: 'var(--pastel-mint-border)',     label: 'Done' },
};

function formatDate(iso: string): string {
  const d   = new Date(iso);
  const now = new Date();
  now.setHours(0, 0, 0, 0);
  d.setHours(0, 0, 0, 0);
  const diff = Math.round((d.getTime() - now.getTime()) / 86400000);
  if (diff === 0) return 'Today';
  if (diff === 1) return 'Tomorrow';
  if (diff === -1) return 'Yesterday';
  if (diff < 0) return `${Math.abs(diff)} days ago`;
  return d.toLocaleDateString('en-IN', { day: 'numeric', month: 'short' });
}

function isToday(iso: string): boolean {
  return new Date(iso).toDateString() === new Date().toDateString();
}
function isUpcoming(iso: string): boolean {
  const d = new Date(iso); d.setHours(0,0,0,0);
  const t = new Date();    t.setHours(0,0,0,0);
  return d.getTime() > t.getTime();
}

// ─────────────────────────────────────────────────────
// Create Task Modal
// ─────────────────────────────────────────────────────
interface ModalProps {
  onClose: () => void;
  onSave:  (t: Omit<Task, 'id'>) => void;
}

function CreateTaskModal({ onClose, onSave }: ModalProps) {
  const [title,    setTitle]    = useState('');
  const [desc,     setDesc]     = useState('');
  const [priority, setPriority] = useState<Priority>('MEDIUM');
  const [status,   setStatus]   = useState<Status>('TODO');
  const [dueDate,  setDueDate]  = useState(fmt(today));
  const [category, setCategory] = useState('');
  const [tagInput, setTagInput] = useState('');
  const [tags,     setTags]     = useState<string[]>([]);

  function addTag() {
    const t = tagInput.trim().toLowerCase();
    if (t && !tags.includes(t)) setTags(p => [...p, t]);
    setTagInput('');
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!title.trim()) return;
    onSave({ title: title.trim(), description: desc, priority, status, dueDate, category, tags });
    onClose();
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-box" onClick={e => e.stopPropagation()}>
        <div className="modal-header">
          <div>
            <h2 className="modal-title">Create New Task</h2>
            <p className="modal-subtitle">Add a task to your workspace</p>
          </div>
          <button className="modal-close-btn" onClick={onClose}><X size={20} /></button>
        </div>

        <form className="modal-form" onSubmit={handleSubmit}>
          <div className="form-group">
            <label className="form-label">Task title <span style={{color:'var(--error)'}}>*</span></label>
            <input className="form-input" placeholder="What needs to be done?" value={title} onChange={e => setTitle(e.target.value)} required />
          </div>

          <div className="form-group">
            <label className="form-label">Description</label>
            <textarea className="form-input" rows={3} placeholder="Add more details..." value={desc} onChange={e => setDesc(e.target.value)} style={{resize:'vertical'}} />
          </div>

          <div className="modal-row">
            <div className="form-group" style={{flex:1}}>
              <label className="form-label">Priority</label>
              <select className="form-input form-select" value={priority} onChange={e => setPriority(e.target.value as Priority)}>
                <option value="HIGH">High</option>
                <option value="MEDIUM">Medium</option>
                <option value="LOW">Low</option>
              </select>
            </div>
            <div className="form-group" style={{flex:1}}>
              <label className="form-label">Status</label>
              <select className="form-input form-select" value={status} onChange={e => setStatus(e.target.value as Status)}>
                <option value="TODO">To Do</option>
                <option value="IN_PROGRESS">In Progress</option>
                <option value="COMPLETED">Completed</option>
              </select>
            </div>
          </div>

          <div className="modal-row">
            <div className="form-group" style={{flex:1}}>
              <label className="form-label">Due date</label>
              <input type="date" className="form-input" value={dueDate} onChange={e => setDueDate(e.target.value)} />
            </div>
            <div className="form-group" style={{flex:1}}>
              <label className="form-label">Category</label>
              <input className="form-input" placeholder="e.g. Academic, Work" value={category} onChange={e => setCategory(e.target.value)} />
            </div>
          </div>

          <div className="form-group">
            <label className="form-label">Tags</label>
            <div className="tag-input-row">
              <input className="form-input" placeholder="Type a tag and press Enter" value={tagInput}
                onChange={e => setTagInput(e.target.value)}
                onKeyDown={e => { if (e.key === 'Enter') { e.preventDefault(); addTag(); } }} />
              <button type="button" className="btn btn-soft" onClick={addTag} style={{whiteSpace:'nowrap'}}>Add tag</button>
            </div>
            {tags.length > 0 && (
              <div className="tags-list" style={{marginTop:'10px'}}>
                {tags.map(t => (
                  <span key={t} className="task-tag">
                    #{t}
                    <button type="button" onClick={() => setTags(p => p.filter(x => x !== t))} style={{marginLeft:4,background:'none',border:'none',cursor:'pointer',color:'inherit',padding:0}}><X size={11}/></button>
                  </span>
                ))}
              </div>
            )}
          </div>

          <div className="modal-footer">
            <button type="button" className="btn btn-ghost" onClick={onClose}>Cancel</button>
            <button type="submit" className="btn btn-primary">Create Task</button>
          </div>
        </form>
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────
// Task Card
// ─────────────────────────────────────────────────────
interface TaskCardProps {
  task:     Task;
  onToggle: (id: number) => void;
  onDelete: (id: number) => void;
}

function TaskCard({ task, onToggle, onDelete }: TaskCardProps) {
  const p = PRIORITY_STYLE[task.priority];
  const s = STATUS_STYLE[task.status];
  const isDone = task.status === 'COMPLETED';

  return (
    <div className={`task-item ${isDone ? 'task-item-done' : ''}`}>
      <button className="task-check-btn" onClick={() => onToggle(task.id)} title={isDone ? 'Mark incomplete' : 'Mark complete'}>
        {isDone
          ? <CheckCircle2 size={22} style={{color:'var(--accent-mint)'}} />
          : <Circle      size={22} style={{color:'var(--border-focus)'}} />}
      </button>

      <div className="task-body">
        <div className="task-top-row">
          <h3 className={`task-title ${isDone ? 'task-title-done' : ''}`}>{task.title}</h3>
          <div className="task-badges">
            <span className="priority-badge" style={{background:p.bg, color:p.color, border:`1px solid ${p.border}`}}>
              {p.icon}{task.priority}
            </span>
            <span className="status-badge" style={{background:s.bg, color:s.color, border:`1px solid ${s.border}`}}>
              {s.label}
            </span>
          </div>
        </div>

        {task.description && <p className="task-desc">{task.description}</p>}

        <div className="task-meta">
          {task.category && (
            <span className="task-category">{task.category}</span>
          )}
          {task.tags.map(t => (
            <span key={t} className="task-tag">#{t}</span>
          ))}
          <span className="task-due">
            <Calendar size={12} />
            {isDone ? 'Completed' : formatDate(task.dueDate)}
          </span>
        </div>
      </div>

      <button className="task-delete-btn" onClick={() => onDelete(task.id)} title="Delete task">
        <X size={15} />
      </button>
    </div>
  );
}

// ─────────────────────────────────────────────────────
// Dropdown helper
// ─────────────────────────────────────────────────────
interface DropdownProps {
  label:    string;
  options:  { value: string; label: string }[];
  value:    string;
  onChange: (v: string) => void;
}
function FilterDropdown({ label, options, value, onChange }: DropdownProps) {
  const [open, setOpen] = useState(false);
  const current = options.find(o => o.value === value);
  return (
    <div className="filter-dropdown" style={{position:'relative'}}>
      <button className="filter-btn" onClick={() => setOpen(p => !p)}>
        {current?.label ?? label} <ChevronDown size={14} />
      </button>
      {open && (
        <div className="filter-menu">
          {options.map(o => (
            <button key={o.value} className={`filter-option ${value === o.value ? 'filter-option-active' : ''}`}
              onClick={() => { onChange(o.value); setOpen(false); }}>
              {o.label}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

// ─────────────────────────────────────────────────────
// Main Tasks Panel
// ─────────────────────────────────────────────────────
export default function TasksPanel() {
  const [tasks,       setTasks]       = useState<Task[]>(SEED_TASKS);
  const [activeTab,   setActiveTab]   = useState<Tab>('ALL');
  const [search,      setSearch]      = useState('');
  const [priorityFilter, setPriorityFilter] = useState('ALL');
  const [statusFilter,   setStatusFilter]   = useState('ALL');
  const [showModal,   setShowModal]   = useState(false);

  function toggleTask(id: number) {
    setTasks(ts => ts.map(t => {
      if (t.id !== id) return t;
      const next: Status = t.status === 'COMPLETED' ? 'TODO' : 'COMPLETED';
      return { ...t, status: next };
    }));
  }

  function deleteTask(id: number) {
    setTasks(ts => ts.filter(t => t.id !== id));
  }

  function addTask(data: Omit<Task, 'id'>) {
    setTasks(ts => [...ts, { ...data, id: Date.now() }]);
  }

  const filtered = useMemo(() => {
    let result = [...tasks];
    // Tab filter
    if (activeTab === 'TODAY')     result = result.filter(t => isToday(t.dueDate) && t.status !== 'COMPLETED');
    if (activeTab === 'UPCOMING')  result = result.filter(t => isUpcoming(t.dueDate) && t.status !== 'COMPLETED');
    if (activeTab === 'COMPLETED') result = result.filter(t => t.status === 'COMPLETED');
    // Search
    if (search.trim()) {
      const q = search.toLowerCase();
      result = result.filter(t => t.title.toLowerCase().includes(q) || t.description.toLowerCase().includes(q) || t.category.toLowerCase().includes(q));
    }
    // Filters
    if (priorityFilter !== 'ALL') result = result.filter(t => t.priority === priorityFilter);
    if (statusFilter   !== 'ALL') result = result.filter(t => t.status   === statusFilter);
    return result;
  }, [tasks, activeTab, search, priorityFilter, statusFilter]);

  const counts = useMemo(() => ({
    all:       tasks.length,
    today:     tasks.filter(t => isToday(t.dueDate) && t.status !== 'COMPLETED').length,
    upcoming:  tasks.filter(t => isUpcoming(t.dueDate) && t.status !== 'COMPLETED').length,
    completed: tasks.filter(t => t.status === 'COMPLETED').length,
  }), [tasks]);

  return (
    <>
      {showModal && <CreateTaskModal onClose={() => setShowModal(false)} onSave={addTask} />}

      <div className="tasks-panel fade-in">
        {/* Header */}
        <div className="tasks-header">
          <div>
            <h1 className="tasks-title">Tasks</h1>
            <p className="tasks-subtitle">Manage everything you need to get done</p>
          </div>
          <button className="btn btn-primary" onClick={() => setShowModal(true)}>
            <Plus size={18} /> Create Task
          </button>
        </div>

        {/* Tab bar */}
        <div className="tasks-tabs">
          {([
            ['ALL', 'All', counts.all],
            ['TODAY', 'Today', counts.today],
            ['UPCOMING', 'Upcoming', counts.upcoming],
            ['COMPLETED', 'Completed', counts.completed],
          ] as [Tab, string, number][]).map(([val, label, count]) => (
            <button key={val} className={`tasks-tab ${activeTab === val ? 'tasks-tab-active' : ''}`}
              onClick={() => setActiveTab(val)}>
              {label}
              <span className={`tab-count ${activeTab === val ? 'tab-count-active' : ''}`}>{count}</span>
            </button>
          ))}
        </div>

        {/* Filters */}
        <div className="tasks-filters">
          <div className="search-box">
            <Search size={16} className="search-icon" />
            <input
              className="search-input"
              placeholder="Search tasks..."
              value={search}
              onChange={e => setSearch(e.target.value)}
            />
            {search && (
              <button className="search-clear" onClick={() => setSearch('')}><X size={14} /></button>
            )}
          </div>

          <FilterDropdown
            label="Priority"
            value={priorityFilter}
            onChange={setPriorityFilter}
            options={[
              { value: 'ALL',    label: 'All Priorities' },
              { value: 'HIGH',   label: 'High' },
              { value: 'MEDIUM', label: 'Medium' },
              { value: 'LOW',    label: 'Low' },
            ]}
          />
          <FilterDropdown
            label="Status"
            value={statusFilter}
            onChange={setStatusFilter}
            options={[
              { value: 'ALL',         label: 'All Status' },
              { value: 'TODO',        label: 'To Do' },
              { value: 'IN_PROGRESS', label: 'In Progress' },
              { value: 'COMPLETED',   label: 'Completed' },
            ]}
          />
        </div>

        {/* Task List */}
        <div className="tasks-list">
          {filtered.length === 0 ? (
            <div className="tasks-empty">
              <div className="tasks-empty-icon"><CheckCircle2 size={32} /></div>
              <h3>No tasks found</h3>
              <p>{search ? 'Try a different search term.' : 'Click "+ Create Task" to add your first task.'}</p>
            </div>
          ) : (
            filtered.map(task => (
              <TaskCard key={task.id} task={task} onToggle={toggleTask} onDelete={deleteTask} />
            ))
          )}
        </div>

        {/* Footer count */}
        {filtered.length > 0 && (
          <p className="tasks-count">{filtered.length} task{filtered.length !== 1 ? 's' : ''}</p>
        )}
      </div>
    </>
  );
}
