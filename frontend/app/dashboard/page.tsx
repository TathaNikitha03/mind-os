'use client';

import { useState, useEffect, useMemo } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import {
  LayoutDashboard, CheckSquare, FileText, Calendar, Bot, User,
  LogOut, Plus, Search, Filter, CheckCircle2, Circle, Edit3, Trash2,
  Clock, Tag as TagIcon, Folder, X, AlertCircle, Eye, ListTodo,
  CheckCheck, AlertTriangle, Sparkles, RefreshCw, Loader2, Link as LinkIcon,
  ChevronLeft, ChevronRight, Paperclip, Download, BookOpen, ExternalLink, HelpCircle
} from 'lucide-react';
import { getCurrentUser, logout, getMemberSince, type User as AuthUser } from '@/lib/auth';
import {
  getTasksApi, createTaskApi, updateTaskApi, completeTaskApi, deleteTaskApi,
  getTaskStatisticsApi, type TaskDto, type TaskInput, type TaskStatisticsDto
} from '@/lib/taskApi';
import {
  getCategoriesApi, createCategoryApi, updateCategoryApi, deleteCategoryApi,
  type CategoryDto, type CategoryInput
} from '@/lib/categoryApi';
import {
  getTaskDocumentsApi, attachDocumentToTaskApi, detachDocumentFromTaskApi
} from '@/lib/taskDocumentApi';
import {
  getDocumentsApi, getDocumentContentApi, getDocumentDownloadUrl,
  type DocumentDto, type DocumentContentDto
} from '@/lib/documentApi';
import {
  askKnowledgeApi, type RagAnswerResponseDto, type RagSourceDto
} from '@/lib/knowledgeApi';
import AiTaskAssistantModal from '../components/AiTaskAssistantModal';

function formatTimeAMPM(time24?: string) {
  if (!time24) return '';
  const [h, m] = time24.split(':');
  if (!h || !m) return time24;
  const hNum = parseInt(h, 10);
  const ampm = hNum >= 12 ? 'PM' : 'AM';
  const h12 = hNum % 12 || 12;
  return `${h12}:${m} ${ampm}`;
}

function formatDuration(minutes?: number): string {
  if (!minutes || minutes <= 0) return '';
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  if (h > 0 && m > 0) return `${h}h ${m}m`;
  if (h > 0) return `${h}h`;
  return `${m}m`;
}

function formatFileSize(bytes?: number): string {
  if (!bytes) return '0 B';
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
}

const HOURLY_TIME_OPTIONS = [
  // Morning (AM)
  { value: '00:00', label: '12:00 AM (Midnight)', period: 'AM' },
  { value: '01:00', label: '1:00 AM', period: 'AM' },
  { value: '02:00', label: '2:00 AM', period: 'AM' },
  { value: '03:00', label: '3:00 AM', period: 'AM' },
  { value: '04:00', label: '4:00 AM', period: 'AM' },
  { value: '05:00', label: '5:00 AM', period: 'AM' },
  { value: '06:00', label: '6:00 AM', period: 'AM' },
  { value: '07:00', label: '7:00 AM', period: 'AM' },
  { value: '08:00', label: '8:00 AM', period: 'AM' },
  { value: '09:00', label: '9:00 AM', period: 'AM' },
  { value: '10:00', label: '10:00 AM', period: 'AM' },
  { value: '11:00', label: '11:00 AM', period: 'AM' },
  // Afternoon / Evening (PM)
  { value: '12:00', label: '12:00 PM (Noon)', period: 'PM' },
  { value: '13:00', label: '1:00 PM', period: 'PM' },
  { value: '14:00', label: '2:00 PM', period: 'PM' },
  { value: '15:00', label: '3:00 PM', period: 'PM' },
  { value: '16:00', label: '4:00 PM', period: 'PM' },
  { value: '17:00', label: '5:00 PM', period: 'PM' },
  { value: '18:00', label: '6:00 PM', period: 'PM' },
  { value: '19:00', label: '7:00 PM', period: 'PM' },
  { value: '20:00', label: '8:00 PM', period: 'PM' },
  { value: '21:00', label: '9:00 PM', period: 'PM' },
  { value: '22:00', label: '10:00 PM', period: 'PM' },
  { value: '23:00', label: '11:00 PM', period: 'PM' },
];

const DEFAULT_CATEGORIES = [
  'Academics & Studies',
  'Work & Projects',
  'Research & AI',
  'Personal & Life',
  'Health & Fitness',
  'General',
];
import {
  getTagsApi, type TagDto
} from '@/lib/tagApi';
import {
  getTaskDependenciesApi, getTaskDependentsApi, addTaskDependencyApi, removeTaskDependencyApi,
  type TaskDependencyDto
} from '@/lib/dependencyApi';

type Panel = 'dashboard' | 'tasks' | 'categories' | 'documents' | 'calendar' | 'assistant' | 'profile';
type Priority = 'HIGH' | 'MEDIUM' | 'LOW';
type Status = 'TODO' | 'IN_PROGRESS' | 'COMPLETED';
type TabType = 'all' | 'today' | 'upcoming' | 'completed' | 'overdue';


export interface TaskItem {
  id: string;
  title: string;
  description: string;
  priority: Priority;
  status: Status;
  dueDate: string; // YYYY-MM-DD
  dueTime?: string; // HH:MM
  estimatedMinutes?: number;
  category: string;
  tags: string[];
  dependencyCount?: number;
  uncompletedDependencyCount?: number;
  blocked?: boolean;
  documentCount?: number;
  createdAt: string;
}

const NAV_ITEMS: { id: Panel; label: string; icon: React.ReactNode; href?: string }[] = [
  { id: 'dashboard', label: 'Dashboard', icon: <LayoutDashboard size={18} /> },
  { id: 'tasks', label: 'Tasks', icon: <CheckSquare size={18} /> },
  { id: 'categories', label: 'Categories', icon: <Folder size={18} /> },
  { id: 'documents', label: 'Knowledge Base', icon: <FileText size={18} />, href: '/documents' },
  { id: 'calendar', label: 'Calendar', icon: <Calendar size={18} /> },
  { id: 'assistant', label: 'AI Assistant', icon: <Bot size={18} />, href: '/knowledge' },
  { id: 'profile', label: 'Profile', icon: <User size={18} /> },
];


function getTodayString(): string {
  const d = new Date();
  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function getGreeting(): string {
  const h = new Date().getHours();
  if (h < 12) return 'Good Morning';
  if (h < 17) return 'Good Afternoon';
  return 'Good Evening';
}

function PriorityBadge({ priority }: { priority: Priority }) {
  const cls =
    priority === 'HIGH'
      ? 'task-priority-high'
      : priority === 'MEDIUM'
      ? 'task-priority-medium'
      : 'task-priority-low';
  const label = priority === 'HIGH' ? 'High' : priority === 'MEDIUM' ? 'Medium' : 'Low';
  return <span className={`task-priority ${cls}`}>{label} Priority</span>;
}

function StatusBadge({ status }: { status: Status }) {
  const cls =
    status === 'COMPLETED'
      ? 'status-completed'
      : status === 'IN_PROGRESS'
      ? 'status-in_progress'
      : 'status-todo';
  const label =
    status === 'COMPLETED'
      ? 'Completed'
      : status === 'IN_PROGRESS'
      ? 'In Progress'
      : 'To Do';
  return (
    <span className={`status-badge ${cls}`}>
      {status === 'COMPLETED' && <CheckCircle2 size={12} />}
      {status === 'IN_PROGRESS' && <Clock size={12} />}
      {status === 'TODO' && <Circle size={12} />}
      {label}
    </span>
  );
}

export default function DashboardPage() {
  const router = useRouter();
  const [user, setUser] = useState<AuthUser | null>(null);
  const [panel, setPanel] = useState<Panel>('dashboard');

  // Task State & Loading / Error States
  const [tasks, setTasks] = useState<TaskItem[]>([]);
  const [isLoadingTasks, setIsLoadingTasks] = useState(true);
  const [taskApiError, setTaskApiError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  // Task Statistics State (STEP 3.4 Backend Calculated)
  const [taskStats, setTaskStats] = useState<TaskStatisticsDto | null>(null);
  const [isLoadingStats, setIsLoadingStats] = useState(true);
  const [statsApiError, setStatsApiError] = useState<string | null>(null);

  // Category State & Loading / Error States
  const [categories, setCategories] = useState<CategoryDto[]>([]);
  const [isLoadingCategories, setIsLoadingCategories] = useState(true);
  const [categoryApiError, setCategoryApiError] = useState<string | null>(null);
  const [categoryActionError, setCategoryActionError] = useState<string | null>(null);

  // Tag State & Loading / Error States
  const [tags, setTags] = useState<TagDto[]>([]);
  const [isLoadingTags, setIsLoadingTags] = useState(true);
  const [tagApiError, setTagApiError] = useState<string | null>(null);

  // AI Task Assistant State
  const [isAiModalOpen, setIsAiModalOpen] = useState(false);
  const [aiModalInitialDate, setAiModalInitialDate] = useState<string>('2026-09-19');
  const [aiModalInitialPrompt, setAiModalInitialPrompt] = useState<string>('');
  const [aiSuccessMessage, setAiSuccessMessage] = useState<string | null>(null);

  function openAiAssistant(initialDate?: string, prompt?: string) {
    setAiModalInitialDate(initialDate || calendarSelectedDate || getTodayString());
    setAiModalInitialPrompt(prompt || '');
    setIsAiModalOpen(true);
  }

  // Category Modal State
  const [isCategoryModalOpen, setIsCategoryModalOpen] = useState(false);
  const [editingCategory, setEditingCategory] = useState<CategoryDto | null>(null);
  const [categoryFormName, setCategoryFormName] = useState('');
  const [categoryFormDesc, setCategoryFormDesc] = useState('');
  const [categoryFormError, setCategoryFormError] = useState<string | null>(null);
  const [deletingCategory, setDeletingCategory] = useState<CategoryDto | null>(null);
  const [categoryDeleteError, setCategoryDeleteError] = useState<string | null>(null);
  const [isSubmittingCategory, setIsSubmittingCategory] = useState(false);

  // Filters & Search State
  const [activeTab, setActiveTab] = useState<TabType>('all');
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('all');
  const [priorityFilter, setPriorityFilter] = useState<string>('all');
  const [categoryFilter, setCategoryFilter] = useState<string>('all');
  const [tagFilter, setTagFilter] = useState<string>('all');

  // Modal Controls
  const [isTaskModalOpen, setIsTaskModalOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [editingTask, setEditingTask] = useState<TaskItem | null>(null);
  const [detailTask, setDetailTask] = useState<TaskItem | null>(null);
  const [deletingTaskId, setDeletingTaskId] = useState<string | null>(null);

  // Task Dependency State for Detail View
  const [taskDependencies, setTaskDependencies] = useState<TaskDependencyDto[]>([]);
  const [taskDependents, setTaskDependents] = useState<TaskDependencyDto[]>([]);
  const [isLoadingDependencies, setIsLoadingDependencies] = useState(false);
  const [dependencyError, setDependencyError] = useState<string | null>(null);
  const [isAddingDependency, setIsAddingDependency] = useState(false);
  const [selectedPrerequisiteId, setSelectedPrerequisiteId] = useState<string>('');

  // Task Document & Knowledge Base State
  const [allUserDocuments, setAllUserDocuments] = useState<DocumentDto[]>([]);
  const [taskDocuments, setTaskDocuments] = useState<DocumentDto[]>([]);
  const [isLoadingTaskDocuments, setIsLoadingTaskDocuments] = useState(false);
  const [taskDocumentError, setTaskDocumentError] = useState<string | null>(null);
  const [isAddingTaskDocument, setIsAddingTaskDocument] = useState(false);
  const [selectedDocToAttachId, setSelectedDocToAttachId] = useState<string>('');
  const [previewDocId, setPreviewDocId] = useState<number | string | null>(null);
  const [previewDocContent, setPreviewDocContent] = useState<DocumentContentDto | null>(null);
  const [isLoadingDocPreview, setIsLoadingDocPreview] = useState(false);

  // Grounded Task AI Assistant (RAG)
  const [taskAiQuestion, setTaskAiQuestion] = useState('');
  const [isAskingTaskAi, setIsAskingTaskAi] = useState(false);
  const [taskAiAnswer, setTaskAiAnswer] = useState<RagAnswerResponseDto | null>(null);
  const [taskAiError, setTaskAiError] = useState<string | null>(null);

  // Form Field State
  const [formAttachDocId, setFormAttachDocId] = useState<string>('');
  const [formTitle, setFormTitle] = useState('');
  const [formDescription, setFormDescription] = useState('');
  const [formPriority, setFormPriority] = useState<Priority>('MEDIUM');
  const [formStatus, setFormStatus] = useState<Status>('TODO');
  const [formDueDate, setFormDueDate] = useState('');
  const [formDueTime, setFormDueTime] = useState('');
  const [formCategory, setFormCategory] = useState('');
  const [isCustomCategory, setIsCustomCategory] = useState(false);
  const [customCategoryInput, setCustomCategoryInput] = useState('');
  const [formTagsList, setFormTagsList] = useState<string[]>([]);
  const [tagInputValue, setTagInputValue] = useState('');
  const [showTagSuggestions, setShowTagSuggestions] = useState(false);
  const [formErrors, setFormErrors] = useState<{ title?: string; priority?: string }>({});

  // Interactive Schedule Calendar State
  const [calendarCurrentDate, setCalendarCurrentDate] = useState(() => new Date());
  const [calendarSelectedDate, setCalendarSelectedDate] = useState(() => getTodayString());

  function handlePrevMonth() {
    setCalendarCurrentDate(prev => new Date(prev.getFullYear(), prev.getMonth() - 1, 1));
  }

  function handleNextMonth() {
    setCalendarCurrentDate(prev => new Date(prev.getFullYear(), prev.getMonth() + 1, 1));
  }

  function handleJumpToToday() {
    const today = new Date();
    setCalendarCurrentDate(today);
    setCalendarSelectedDate(getTodayString());
  }

  const calendarDays = useMemo(() => {
    const year = calendarCurrentDate.getFullYear();
    const month = calendarCurrentDate.getMonth();

    const firstDayIndex = new Date(year, month, 1).getDay();
    const daysInMonth = new Date(year, month + 1, 0).getDate();
    const daysInPrevMonth = new Date(year, month, 0).getDate();

    const days: {
      dateString: string;
      dayNumber: number;
      isCurrentMonth: boolean;
      isToday: boolean;
    }[] = [];

    const todayStr = getTodayString();

    // 1. Previous month trailing days
    for (let i = firstDayIndex - 1; i >= 0; i--) {
      const d = daysInPrevMonth - i;
      const prevDate = new Date(year, month - 1, d);
      const yStr = prevDate.getFullYear();
      const mStr = String(prevDate.getMonth() + 1).padStart(2, '0');
      const dStr = String(d).padStart(2, '0');
      const dateString = `${yStr}-${mStr}-${dStr}`;
      days.push({
        dateString,
        dayNumber: d,
        isCurrentMonth: false,
        isToday: dateString === todayStr,
      });
    }

    // 2. Current month days
    for (let d = 1; d <= daysInMonth; d++) {
      const mStr = String(month + 1).padStart(2, '0');
      const dStr = String(d).padStart(2, '0');
      const dateString = `${year}-${mStr}-${dStr}`;
      days.push({
        dateString,
        dayNumber: d,
        isCurrentMonth: true,
        isToday: dateString === todayStr,
      });
    }

    // 3. Next month leading days (completing 35 or 42 grid cells)
    const totalCells = days.length <= 35 ? 35 : 42;
    const remainingDays = totalCells - days.length;
    for (let d = 1; d <= remainingDays; d++) {
      const nextDate = new Date(year, month + 1, d);
      const yStr = nextDate.getFullYear();
      const mStr = String(nextDate.getMonth() + 1).padStart(2, '0');
      const dStr = String(d).padStart(2, '0');
      const dateString = `${yStr}-${mStr}-${dStr}`;
      days.push({
        dateString,
        dayNumber: d,
        isCurrentMonth: false,
        isToday: dateString === todayStr,
      });
    }

    return days;
  }, [calendarCurrentDate]);

  const tasksByDate = useMemo(() => {
    const map: Record<string, TaskItem[]> = {};
    tasks.forEach(t => {
      const dateKey = t.dueDate ? t.dueDate.split('T')[0] : '';
      if (dateKey) {
        if (!map[dateKey]) map[dateKey] = [];
        map[dateKey].push(t);
      }
    });
    Object.keys(map).forEach(key => {
      map[key].sort((a, b) => (a.dueTime || '99:99').localeCompare(b.dueTime || '99:99'));
    });
    return map;
  }, [tasks]);

  const selectedDateTasks = useMemo(() => {
    return tasksByDate[calendarSelectedDate] || [];
  }, [tasksByDate, calendarSelectedDate]);

  const formattedMonthYear = useMemo(() => {
    return calendarCurrentDate.toLocaleDateString('en-US', { month: 'long', year: 'numeric' });
  }, [calendarCurrentDate]);

  const formattedSelectedDate = useMemo(() => {
    const [y, m, d] = calendarSelectedDate.split('-').map(Number);
    if (!y || !m || !d) return calendarSelectedDate;
    const dateObj = new Date(y, m - 1, d);
    return dateObj.toLocaleDateString('en-US', { weekday: 'long', month: 'short', day: 'numeric', year: 'numeric' });
  }, [calendarSelectedDate]);

  useEffect(() => {
    const u = getCurrentUser();
    if (!u) {
      router.replace('/login');
      return;
    }
    setUser(u);
    loadTasksFromBackend();
    loadTaskStatisticsFromBackend();
    loadCategoriesFromBackend();
    loadTagsFromBackend();
    loadUserDocuments();
  }, [router]);

  async function loadTaskStatisticsFromBackend() {
    setIsLoadingStats(true);
    setStatsApiError(null);
    try {
      const dto = await getTaskStatisticsApi();
      setTaskStats(dto);
    } catch (err: any) {
      console.warn('Failed to load task statistics:', err?.message || err);
      setStatsApiError(err?.message || 'Unable to load task statistics.');
    } finally {
      setIsLoadingStats(false);
    }
  }

  async function loadTagsFromBackend() {
    setIsLoadingTags(true);
    setTagApiError(null);
    try {
      const dtos = await getTagsApi();
      setTags(dtos);
    } catch (err: any) {
      console.warn('Failed to load tags:', err?.message || err);
      setTagApiError(err?.message || 'Unable to load tags.');
    } finally {
      setIsLoadingTags(false);
    }
  }

  async function loadCategoriesFromBackend() {
    setIsLoadingCategories(true);
    setCategoryApiError(null);
    try {
      const dtos = await getCategoriesApi();
      setCategories(dtos);
    } catch (err: any) {
      console.warn('Failed to load categories:', err?.message || err);
      setCategoryApiError(err?.message || 'Unable to load categories.');
    } finally {
      setIsLoadingCategories(false);
    }
  }

  async function loadTasksFromBackend() {
    setIsLoadingTasks(true);
    setTaskApiError(null);
    try {
      const dtos = await getTasksApi();
      const mappedTasks: TaskItem[] = dtos.map(dto => ({
        id: String(dto.id),
        title: dto.title,
        description: dto.description || '',
        priority: dto.priority || 'MEDIUM',
        status: dto.status || 'TODO',
        dueDate: dto.dueDate || getTodayString(),
        dueTime: dto.dueTime || '',
        estimatedMinutes: dto.estimatedMinutes,
        category: dto.category || 'General',
        tags: dto.tags || [],
        dependencyCount: dto.dependencyCount || 0,
        uncompletedDependencyCount: dto.uncompletedDependencyCount || 0,
        blocked: dto.blocked || false,
        documentCount: dto.documentCount || 0,
        createdAt: dto.createdAt ? dto.createdAt.split('T')[0] : getTodayString(),
      }));
      setTasks(mappedTasks);
    } catch (err: any) {
      console.warn('Failed to load tasks:', err?.message || err);
      setTaskApiError(err?.message || 'Unable to load tasks. Please ensure the backend is running.');
    } finally {
      setIsLoadingTasks(false);
    }
  }

  async function loadUserDocuments() {
    try {
      const docs = await getDocumentsApi();
      setAllUserDocuments(docs);
    } catch (err) {
      console.warn('Could not load user documents:', err);
    }
  }

  async function loadTaskDependencies(taskId: string) {
    setIsLoadingDependencies(true);
    setDependencyError(null);
    try {
      const [deps, dependents] = await Promise.all([
        getTaskDependenciesApi(taskId),
        getTaskDependentsApi(taskId),
      ]);
      setTaskDependencies(deps);
      setTaskDependents(dependents);
    } catch (err: any) {
      console.error('Failed to load dependencies:', err);
      setDependencyError('Unable to load task dependencies.');
    } finally {
      setIsLoadingDependencies(false);
    }
  }

  async function loadTaskDocuments(taskId: string) {
    setIsLoadingTaskDocuments(true);
    setTaskDocumentError(null);
    try {
      const docs = await getTaskDocumentsApi(taskId);
      setTaskDocuments(docs);
    } catch (err: any) {
      console.error('Failed to load task documents:', err);
      setTaskDocumentError('Unable to load attached documents.');
    } finally {
      setIsLoadingTaskDocuments(false);
    }
  }

  function openTaskDetails(task: TaskItem) {
    setDetailTask(task);
    setIsAddingDependency(false);
    setSelectedPrerequisiteId('');
    setDependencyError(null);
    loadTaskDependencies(task.id);

    // Connected Knowledge & Document State
    setIsAddingTaskDocument(false);
    setSelectedDocToAttachId('');
    setTaskDocumentError(null);
    setPreviewDocId(null);
    setPreviewDocContent(null);
    setTaskAiAnswer(null);
    setTaskAiError(null);
    setTaskAiQuestion(`What are the key points and requirements for "${task.title}"?`);
    loadTaskDocuments(task.id);
    loadUserDocuments();
  }

  async function handleAddDependency(e: React.FormEvent) {
    e.preventDefault();
    if (!detailTask || !selectedPrerequisiteId) return;

    setDependencyError(null);
    try {
      await addTaskDependencyApi(detailTask.id, selectedPrerequisiteId);
      await loadTaskDependencies(detailTask.id);
      await loadTasksFromBackend();
      setSelectedPrerequisiteId('');
      setIsAddingDependency(false);
    } catch (err: any) {
      console.error('Failed to add dependency:', err);
      setDependencyError(err.message || 'Unable to add task dependency.');
    }
  }

  async function handleRemoveDependency(dependencyId: string | number) {
    if (!detailTask) return;
    setDependencyError(null);
    try {
      await removeTaskDependencyApi(detailTask.id, dependencyId);
      await loadTaskDependencies(detailTask.id);
      await loadTasksFromBackend();
    } catch (err: any) {
      console.error('Failed to remove dependency:', err);
      setDependencyError(err.message || 'Unable to remove task dependency.');
    }
  }

  async function handleAttachTaskDocument() {
    if (!detailTask || !selectedDocToAttachId) return;
    setTaskDocumentError(null);
    try {
      await attachDocumentToTaskApi(detailTask.id, selectedDocToAttachId);
      await loadTaskDocuments(detailTask.id);
      await loadTasksFromBackend();
      setSelectedDocToAttachId('');
      setIsAddingTaskDocument(false);
    } catch (err: any) {
      console.error('Failed to attach document:', err);
      setTaskDocumentError(err.message || 'Unable to attach document.');
    }
  }

  async function handleDetachTaskDocument(documentId: string | number) {
    if (!detailTask) return;
    setTaskDocumentError(null);
    try {
      await detachDocumentFromTaskApi(detailTask.id, documentId);
      await loadTaskDocuments(detailTask.id);
      await loadTasksFromBackend();
      if (previewDocId === documentId) {
        setPreviewDocId(null);
        setPreviewDocContent(null);
      }
    } catch (err: any) {
      console.error('Failed to detach document:', err);
      setTaskDocumentError(err.message || 'Unable to detach document.');
    }
  }

  async function handleToggleDocPreview(docId: string | number) {
    if (previewDocId === docId) {
      setPreviewDocId(null);
      setPreviewDocContent(null);
      return;
    }

    setPreviewDocId(docId);
    setIsLoadingDocPreview(true);
    try {
      const content = await getDocumentContentApi(docId);
      setPreviewDocContent(content);
    } catch (err: any) {
      console.error('Failed to load document content:', err);
      setTaskDocumentError('Unable to preview document text.');
    } finally {
      setIsLoadingDocPreview(false);
    }
  }

  async function handleAskTaskAi(customQuery?: string) {
    const q = (customQuery || taskAiQuestion).trim();
    if (!q) return;
    setIsAskingTaskAi(true);
    setTaskAiError(null);
    try {
      const answer = await askKnowledgeApi(q, 3);
      setTaskAiAnswer(answer);
    } catch (err: any) {
      console.error('Task AI query failed:', err);
      setTaskAiError(err.message || 'AI assistant is currently unavailable.');
    } finally {
      setIsAskingTaskAi(false);
    }
  }

  function handleLogout() {
    logout();
    router.replace('/login');
  }

  // Category Operations
  async function seedStarterCategories() {
    setIsSubmittingCategory(true);
    setCategoryActionError(null);
    try {
      const starters = [
        { name: 'Academics & Studies', description: 'Lectures, coursework, exam preparation, and study notes.' },
        { name: 'Work & Projects', description: 'Coding tasks, team projects, meetings, and sprint deliverables.' },
        { name: 'Research & AI', description: 'Paper reading, model experiments, and AI knowledge engineering.' },
        { name: 'Personal & Life', description: 'Personal habits, daily routines, errands, and health.' },
        { name: 'Health & Fitness', description: 'Workouts, nutrition, sleep tracking, and wellness goals.' },
      ];
      for (const s of starters) {
        if (!categories.some(c => c.name.toLowerCase() === s.name.toLowerCase())) {
          await createCategoryApi(s);
        }
      }
      await loadCategoriesFromBackend();
    } catch (err: any) {
      console.error('Failed to seed starter categories:', err);
      setCategoryActionError('Failed to load starter categories: ' + (err.message || 'Error'));
    } finally {
      setIsSubmittingCategory(false);
    }
  }

  function openCreateCategoryModal() {
    setEditingCategory(null);
    setCategoryFormName('');
    setCategoryFormDesc('');
    setCategoryFormError(null);
    setCategoryActionError(null);
    setIsCategoryModalOpen(true);
  }

  function openEditCategoryModal(cat: CategoryDto, e?: React.MouseEvent) {
    if (e) e.stopPropagation();
    setEditingCategory(cat);
    setCategoryFormName(cat.name);
    setCategoryFormDesc(cat.description || '');
    setCategoryFormError(null);
    setCategoryActionError(null);
    setIsCategoryModalOpen(true);
  }

  async function handleSaveCategory(e: React.FormEvent) {
    e.preventDefault();
    setCategoryFormError(null);
    setCategoryActionError(null);

    const trimmedName = categoryFormName.trim();
    if (!trimmedName) {
      setCategoryFormError('Category name cannot be empty.');
      return;
    }

    setIsSubmittingCategory(true);
    try {
      if (editingCategory) {
        // PUT /api/categories/{id}
        await updateCategoryApi(editingCategory.id, {
          name: trimmedName,
          description: categoryFormDesc.trim(),
        });
      } else {
        // POST /api/categories
        await createCategoryApi({
          name: trimmedName,
          description: categoryFormDesc.trim(),
        });
      }

      await loadCategoriesFromBackend();
      await loadTasksFromBackend();
      setIsCategoryModalOpen(false);
    } catch (err: any) {
      console.error('Failed to save category:', err);
      setCategoryFormError(err.message || 'Unable to save category.');
    } finally {
      setIsSubmittingCategory(false);
    }
  }

  function confirmDeleteCategory(cat: CategoryDto, e?: React.MouseEvent) {
    if (e) e.stopPropagation();
    setDeletingCategory(cat);
    setCategoryDeleteError(null);
  }

  async function executeDeleteCategory() {
    if (!deletingCategory) return;
    setIsSubmittingCategory(true);
    setCategoryDeleteError(null);
    try {
      // DELETE /api/categories/{id}
      await deleteCategoryApi(deletingCategory.id);
      await loadCategoriesFromBackend();
      await loadTasksFromBackend();
      setDeletingCategory(null);
    } catch (err: any) {
      console.error('Failed to delete category:', err);
      setCategoryDeleteError(err.message || 'Unable to delete category.');
    } finally {
      setIsSubmittingCategory(false);
    }
  }

  // Calculate Available Dynamic Categories for dropdowns & filters
  const categoriesList = useMemo(() => {
    const list = [...DEFAULT_CATEGORIES];
    categories.forEach(c => {
      if (c.name && !list.includes(c.name)) {
        list.push(c.name);
      }
    });
    tasks.forEach(t => {
      if (t.category && !list.includes(t.category)) {
        list.push(t.category);
      }
    });
    return list;
  }, [categories, tasks]);

  // Calculate Available Dynamic Tags for dropdowns & filters
  const tagsList = useMemo(() => {
    const list = tags.map(t => t.name);
    tasks.forEach(t => {
      if (t.tags) {
        t.tags.forEach(tag => {
          if (tag && !list.includes(tag)) {
            list.push(tag);
          }
        });
      }
    });
    return list.sort();
  }, [tags, tasks]);

  // Autocomplete matching tag suggestions for input
  const matchingSuggestions = useMemo(() => {
    const query = tagInputValue.trim().toLowerCase().replace(/^#+/, '');
    if (!query) return tags.filter(t => !formTagsList.includes(t.name.toLowerCase())).slice(0, 5);
    return tags.filter(t => t.name.toLowerCase().includes(query) && !formTagsList.includes(t.name.toLowerCase())).slice(0, 6);
  }, [tags, tagInputValue, formTagsList]);

  function addFormTag(rawName: string) {
    const normalized = rawName.trim().replace(/^#+/, '').toLowerCase();
    if (!normalized) return;
    if (!formTagsList.includes(normalized)) {
      setFormTagsList(prev => [...prev, normalized]);
    }
    setTagInputValue('');
  }

  function removeFormTag(tagToRemove: string) {
    setFormTagsList(prev => prev.filter(t => t !== tagToRemove));
  }

  function handleTagInputKeyDown(e: React.KeyboardEvent<HTMLInputElement>) {
    if (e.key === 'Enter' || e.key === ',') {
      e.preventDefault();
      addFormTag(tagInputValue);
    } else if (e.key === 'Backspace' && !tagInputValue && formTagsList.length > 0) {
      setFormTagsList(prev => prev.slice(0, prev.length - 1));
    }
  }

  function selectTagSuggestion(name: string) {
    addFormTag(name);
    setShowTagSuggestions(false);
  }

  // Statistics Calculation
  const stats = useMemo(() => {
    const todayStr = getTodayString();
    const total = tasks.length;
    const today = tasks.filter(t => t.dueDate === todayStr).length;
    const inProgress = tasks.filter(t => t.status === 'IN_PROGRESS').length;
    const completed = tasks.filter(t => t.status === 'COMPLETED').length;
    const overdue = tasks.filter(t => t.status !== 'COMPLETED' && t.dueDate < todayStr).length;

    return { total, today, inProgress, completed, overdue };
  }, [tasks]);

  // --- COMPUTE STATS & GETTERS --- //
  const activeTasks = tasks.filter(t => t.status !== 'COMPLETED');
  const doneTasks = tasks.filter(t => t.status === 'COMPLETED');

  // --- AI HANDLERS --- //
  const handleTasksGenerated = async (suggestedTasks: any[], targetDate?: string, category?: string) => {
    setIsAiModalOpen(false);
    setActionError(null);
    const dateToUse = targetDate || getTodayString();
    const catToUse = category || 'Academics & Studies';
    try {
      for (const t of suggestedTasks) {
        await createTaskApi({
          title: t.title,
          description: t.description || '',
          priority: t.priority || 'MEDIUM',
          dueDate: dateToUse,
          category: t.suggestedCategory || catToUse,
          estimatedMinutes: t.estimatedMinutes,
          tags: []
        });
      }
      await loadTasksFromBackend();
      setAiSuccessMessage(`✨ Successfully scheduled ${suggestedTasks.length} task(s) for ${dateToUse === getTodayString() ? 'Today' : 'Tomorrow'} (${dateToUse})!`);
      setTimeout(() => setAiSuccessMessage(null), 6000);
    } catch (err: any) {
      console.error('Failed to add generated tasks:', err);
      setActionError(err.message || 'Failed to add generated tasks.');
    }
  };

  // Filtered Tasks List
  const filteredTasks = useMemo(() => {
    const todayStr = getTodayString();

    return tasks.filter(task => {
      // 1. Tab Filter
      if (activeTab === 'today' && task.dueDate !== todayStr) return false;
      if (activeTab === 'upcoming' && (task.dueDate <= todayStr || task.status === 'COMPLETED')) return false;
      if (activeTab === 'completed' && task.status !== 'COMPLETED') return false;
      if (activeTab === 'overdue' && (task.dueDate >= todayStr || task.status === 'COMPLETED')) return false;

      // 2. Search Filter
      if (searchQuery.trim()) {
        const q = searchQuery.toLowerCase();
        const matchesTitle = task.title.toLowerCase().includes(q);
        const matchesDesc = task.description.toLowerCase().includes(q);
        if (!matchesTitle && !matchesDesc) return false;
      }

      // 3. Dropdown Filters
      if (statusFilter !== 'all' && task.status !== statusFilter) return false;
      if (priorityFilter !== 'all' && task.priority !== priorityFilter) return false;
      if (categoryFilter !== 'all' && task.category !== categoryFilter) return false;
      if (tagFilter !== 'all' && (!task.tags || !task.tags.includes(tagFilter))) return false;

      return true;
    });
  }, [tasks, activeTab, searchQuery, statusFilter, priorityFilter, categoryFilter, tagFilter]);

  // Task Operations (API Integrated)
  async function toggleCompleteTask(id: string, e?: React.MouseEvent) {
    if (e) e.stopPropagation();
    setActionError(null);
    try {
      const task = tasks.find(t => t.id === id);
      if (!task) return;

      if (task.status === 'COMPLETED') {
        // Toggle to TODO via update API
        const updated = await updateTaskApi(id, {
          title: task.title,
          description: task.description,
          priority: task.priority,
          status: 'TODO',
          dueDate: task.dueDate,
          dueTime: task.dueTime,
          estimatedMinutes: task.estimatedMinutes,
          category: task.category,
          tags: task.tags,
        });
        setTasks(prev => prev.map(t => t.id === id ? { ...t, status: updated.status || 'TODO' } : t));
      } else {
        // Mark COMPLETED via complete API
        const updated = await completeTaskApi(id);
        setTasks(prev => prev.map(t => t.id === id ? { ...t, status: updated.status || 'COMPLETED' } : t));
      }

      if (detailTask && detailTask.id === id) {
        setDetailTask(prev => prev ? { ...prev, status: prev.status === 'COMPLETED' ? 'TODO' : 'COMPLETED' } : null);
      }

      await loadTaskStatisticsFromBackend();
    } catch (err: any) {
      console.error('Failed to toggle complete task:', err);
      setActionError(err.message || 'Failed to update task completion status.');
    }
  }

  function openCreateModal() {
    setEditingTask(null);
    setFormTitle('');
    setFormDescription('');
    setFormPriority('MEDIUM');
    setFormStatus('TODO');
    setFormDueDate(getTodayString());
    setFormDueTime('18:00');
    setIsCustomCategory(false);
    setCustomCategoryInput('');
    setFormCategory('Academics & Studies');
    setFormTagsList([]);
    setTagInputValue('');
    setShowTagSuggestions(false);
    setFormAttachDocId('');
    setFormErrors({});
    setActionError(null);
    loadUserDocuments();
    setIsTaskModalOpen(true);
  }

  function openEditModal(task: TaskItem, e?: React.MouseEvent) {
    if (e) e.stopPropagation();
    setEditingTask(task);
    setFormTitle(task.title);
    setFormDescription(task.description);
    setFormPriority(task.priority);
    setFormStatus(task.status);
    setFormDueDate(task.dueDate);
    setFormDueTime(task.dueTime || '');
    setIsCustomCategory(false);
    setCustomCategoryInput('');
    setFormCategory(task.category || 'General');
    setFormTagsList(task.tags ? [...task.tags] : []);
    setTagInputValue('');
    setShowTagSuggestions(false);
    setFormAttachDocId('');
    setFormErrors({});
    setActionError(null);
    loadUserDocuments();
    setIsTaskModalOpen(true);
    setDetailTask(null);
  }

  async function handleSaveTask(e: React.FormEvent) {
    e.preventDefault();
    setActionError(null);

    // Validation
    const errors: { title?: string; priority?: string } = {};
    if (!formTitle.trim()) errors.title = 'Task Title is required';
    if (!formPriority) errors.priority = 'Priority is required';

    if (Object.keys(errors).length > 0) {
      setFormErrors(errors);
      return;
    }

    // Include any trailing typed tag if not yet committed
    let finalTags = [...formTagsList];
    if (tagInputValue.trim()) {
      const pending = tagInputValue.trim().replace(/^#+/, '').toLowerCase();
      if (pending && !finalTags.includes(pending)) {
        finalTags.push(pending);
      }
    }

    const taskInput: TaskInput = {
      title: formTitle.trim(),
      description: formDescription.trim(),
      priority: formPriority,
      status: formStatus,
      dueDate: formDueDate || getTodayString(),
      dueTime: formDueTime || '18:00',
      category: formCategory || 'General',
      tags: finalTags,
    };

    setIsSubmitting(true);

    try {
      if (editingTask) {
        // PUT /api/tasks/{id}
        await updateTaskApi(editingTask.id, taskInput);
      } else {
        // POST /api/tasks
        const created = await createTaskApi(taskInput);
        if (formAttachDocId && created?.id) {
          try {
            await attachDocumentToTaskApi(created.id, formAttachDocId);
          } catch (docErr) {
            console.warn('Could not attach document on creation:', docErr);
          }
        }
      }

      await loadTasksFromBackend();
      await loadTaskStatisticsFromBackend();
      await loadTagsFromBackend();
      setIsTaskModalOpen(false);
    } catch (err: any) {
      console.error('Failed to save task:', err);
      setActionError(err.message || 'Unable to save task. Please try again.');
    } finally {
      setIsSubmitting(false);
    }
  }

  function confirmDeleteTask(id: string, e?: React.MouseEvent) {
    if (e) e.stopPropagation();
    setActionError(null);
    setDeletingTaskId(id);
  }

  async function executeDeleteTask() {
    if (!deletingTaskId) return;
    setIsSubmitting(true);
    setActionError(null);
    try {
      // DELETE /api/tasks/{id}
      await deleteTaskApi(deletingTaskId);
      setTasks(prev => prev.filter(t => t.id !== deletingTaskId));
      if (detailTask && detailTask.id === deletingTaskId) setDetailTask(null);
      setDeletingTaskId(null);
      await loadTaskStatisticsFromBackend();
      await loadTagsFromBackend();
    } catch (err: any) {
      console.error('Failed to delete task:', err);
      setActionError(err.message || 'Unable to delete task. Please try again.');
    } finally {
      setIsSubmitting(false);
    }
  }

  function resetFilters() {
    setActiveTab('all');
    setSearchQuery('');
    setStatusFilter('all');
    setPriorityFilter('all');
    setCategoryFilter('all');
    setTagFilter('all');
  }

  if (!user) return null;

  return (
    <div>
      {/* Navbar */}
      <header className="navbar">
        <div className="container-fluid nav-inner">
          <Link href="/dashboard" className="logo">
            <span className="logo-mark">M</span>
            MIND OS
          </Link>
          <div className="nav-actions">
            <span className="user-greeting">Hi, {user.name}</span>
            <button className="btn btn-ghost" onClick={handleLogout} style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <LogOut size={16} />
              Log out
            </button>
          </div>
        </div>
      </header>

      <div className="dashboard-shell">
        {/* Sidebar */}
        <aside className="dashboard-sidebar">
          <nav className="sidebar-nav">
            {NAV_ITEMS.map(item => (
              item.href ? (
                <Link
                  key={item.id}
                  href={item.href}
                  className={`sidebar-link ${panel === item.id ? 'sidebar-link-active' : ''}`}
                >
                  {item.icon}
                  {item.label}
                </Link>
              ) : (
                <button
                  key={item.id}
                  className={`sidebar-link ${panel === item.id ? 'sidebar-link-active' : ''}`}
                  onClick={() => setPanel(item.id)}
                >
                  {item.icon}
                  {item.label}
                </button>
              )
            ))}
          </nav>
        </aside>

        {/* Main Content Area */}
        <section className="dashboard-content">

          {actionError && (
            <div className="form-error fade-in" style={{ marginBottom: 20 }}>
              {actionError}
            </div>
          )}

          {/* DASHBOARD PANEL (STEP 3.4 REAL TASK STATISTICS & DASHBOARD INTELLIGENCE) */}
          {panel === 'dashboard' && (
            <div className="fade-in">
              <div className="content-greeting" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: 12 }}>
                <div>
                  <h1>{getGreeting()}, {user.name}</h1>
                  <p>Here&apos;s real-time intelligence and statistics calculated from your PostgreSQL database.</p>
                </div>
                <div style={{ display: 'flex', gap: 10 }}>
                  <button
                    className="btn btn-soft"
                    onClick={() => {
                      loadTaskStatisticsFromBackend();
                      loadTasksFromBackend();
                    }}
                    title="Refresh statistics"
                    disabled={isLoadingStats}
                  >
                    <RefreshCw size={16} className={isLoadingStats ? 'animate-spin' : ''} />
                    Refresh Stats
                  </button>
                  <button className="btn btn-primary" onClick={openCreateModal}>
                    <Plus size={18} /> Create Task
                  </button>
                </div>
              </div>

              {/* Error State */}
              {statsApiError && (
                <div className="empty-state fade-in" style={{ padding: 24, marginBottom: 24, borderColor: 'var(--pastel-pink-border)' }}>
                  <AlertCircle size={32} color="var(--pastel-pink-text)" style={{ marginBottom: 8 }} />
                  <h3 style={{ color: 'var(--pastel-pink-text)' }}>Unable to load task statistics.</h3>
                  <p style={{ marginBottom: 12 }}>Could not retrieve statistics from backend. Please check server connection.</p>
                  <button className="btn btn-primary btn-sm" onClick={loadTaskStatisticsFromBackend}>
                    <RefreshCw size={14} /> Retry
                  </button>
                </div>
              )}

              {/* Loading State */}
              {isLoadingStats && !taskStats && !statsApiError && (
                <div className="empty-state fade-in" style={{ padding: 48, marginBottom: 24 }}>
                  <Loader2 size={36} className="animate-spin" style={{ color: 'var(--accent-purple)', marginBottom: 12 }} />
                  <h3>Calculating task statistics...</h3>
                  <p>Fetching real analytics from your PostgreSQL tasks.</p>
                </div>
              )}

              {/* Real Statistics Content */}
              {taskStats && (
                <>
                  {/* Empty State for brand new users with 0 tasks */}
                  {taskStats.totalTasks === 0 ? (
                    <div className="empty-state fade-in" style={{ padding: 48, marginBottom: 24 }}>
                      <div className="empty-icon-wrap"><CheckSquare size={36} /></div>
                      <h3>No tasks yet</h3>
                      <p>Create your first task to get started with task tracking and dashboard intelligence.</p>
                      <button className="btn btn-primary" onClick={openCreateModal} style={{ marginTop: 12 }}>
                        <Plus size={16} /> Create your first task
                      </button>
                    </div>
                  ) : (
                    <>
                      {/* AI Success Toast Notification */}
                      {aiSuccessMessage && (
                        <div className="auth-error-banner fade-in" style={{
                          background: 'linear-gradient(135deg, #ecfdf5, #d1fae5)',
                          borderColor: '#a7f3d0',
                          color: '#065f46',
                          marginBottom: 16,
                          display: 'flex',
                          alignItems: 'center',
                          gap: 8,
                          fontSize: '0.9rem',
                          fontWeight: 600
                        }}>
                          <CheckCircle2 size={18} color="#059669" />
                          <span>{aiSuccessMessage}</span>
                        </div>
                      )}

                      {/* AI Productivity Copilot Hero Banner */}
                      <div className="ai-copilot-banner fade-in">
                        <div className="ai-copilot-banner-content">
                          <div className="ai-copilot-badge">
                            <Sparkles size={14} /> AI Productivity Copilot
                          </div>
                          <h3 className="ai-copilot-title">
                            Accelerate Your Goals with Smart AI Scheduling
                          </h3>
                          <p className="ai-copilot-subtitle">
                            Describe any goal, exam prep, or lab work. MindOS AI decomposes it into actionable, prioritized tasks directly synced with your PostgreSQL database.
                          </p>
                          <div className="ai-copilot-chips">
                            <button
                              type="button"
                              className="ai-copilot-chip"
                              onClick={() => openAiAssistant('2026-09-19', 'Break down my Java interview preparation for tomorrow')}
                            >
                              💼 Java Interview Prep
                            </button>
                            <button
                              type="button"
                              className="ai-copilot-chip"
                              onClick={() => openAiAssistant('2026-09-20', 'Prepare Machine Learning Lab Report and evaluation metrics')}
                            >
                              🧪 ML Lab Report
                            </button>
                            <button
                              type="button"
                              className="ai-copilot-chip"
                              onClick={() => openAiAssistant('2026-09-20', 'High-yield exam study plan with past papers and active recall')}
                            >
                              📚 Exam Study Sprint
                            </button>
                          </div>
                        </div>
                        <div className="ai-copilot-banner-action">
                          <button
                            type="button"
                            className="btn btn-primary"
                            onClick={() => openAiAssistant()}
                            style={{
                              background: 'linear-gradient(135deg, #7c3aed, #6d28d9)',
                              boxShadow: '0 4px 14px rgba(124, 58, 237, 0.35)',
                              padding: '10px 18px',
                              fontSize: '0.9rem',
                              fontWeight: 600,
                              whiteSpace: 'nowrap'
                            }}
                          >
                            <Bot size={16} /> Open AI Assistant
                          </button>
                        </div>
                      </div>

                      {/* 1. Six Primary Stat Cards */}
                      <div className="task-stats-grid" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(150px, 1fr))', marginBottom: 20 }}>
                        <div className="stat-card">
                          <div className="stat-icon stat-icon-total"><ListTodo size={20} /></div>
                          <div className="stat-info">
                            <span>Total Tasks</span>
                            <strong>{taskStats.totalTasks}</strong>
                          </div>
                        </div>

                        <div className="stat-card">
                          <div className="stat-icon stat-icon-completed"><CheckCheck size={20} /></div>
                          <div className="stat-info">
                            <span>Completed</span>
                            <strong>{taskStats.completedTasks}</strong>
                          </div>
                        </div>

                        <div className="stat-card">
                          <div className="stat-icon stat-icon-progress"><Clock size={20} /></div>
                          <div className="stat-info">
                            <span>In Progress</span>
                            <strong>{taskStats.inProgressTasks}</strong>
                          </div>
                        </div>

                        <div className="stat-card">
                          <div className="stat-icon stat-icon-todo"><Circle size={20} /></div>
                          <div className="stat-info">
                            <span>To Do</span>
                            <strong>{taskStats.todoTasks}</strong>
                          </div>
                        </div>

                        <div className="stat-card">
                          <div className="stat-icon stat-icon-overdue"><AlertTriangle size={20} /></div>
                          <div className="stat-info">
                            <span>Overdue</span>
                            <strong>{taskStats.overdueTasks}</strong>
                          </div>
                        </div>

                        <div className="stat-card">
                          <div className="stat-icon stat-icon-blocked"><LinkIcon size={20} /></div>
                          <div className="stat-info">
                            <span>Blocked</span>
                            <strong>{taskStats.blockedTasks}</strong>
                          </div>
                        </div>
                      </div>

                      {/* 2. Overall Progress Indicator */}
                      <div className="dashboard-progress-card fade-in">
                        <div className="progress-header">
                          <span className="progress-title">Overall Task Completion</span>
                          <span className="progress-pct">{taskStats.completionPercentage}%</span>
                        </div>
                        <div className="progress-bar-track">
                          <div
                            className="progress-bar-fill"
                            style={{ width: `${Math.min(100, Math.max(0, taskStats.completionPercentage))}%` }}
                          />
                        </div>
                        <span className="progress-footer-text">
                          {taskStats.completedTasks} of {taskStats.totalTasks} tasks completed ({taskStats.completionPercentage}%)
                        </span>
                      </div>

                      {/* 3. Priority & Category Breakdowns */}
                      <div className="dashboard-breakdowns-grid fade-in">
                        {/* Priority Breakdown */}
                        <div className="breakdown-card">
                          <h3>Priority Breakdown</h3>
                          <div className="priority-stat-row">
                            <span className="priority-stat-label">
                              <span className="task-priority task-priority-high" style={{ fontSize: '0.75rem', padding: '2px 8px' }}>High</span>
                            </span>
                            <span className="priority-stat-val">
                              {taskStats.priorityBreakdown?.HIGH || taskStats.priorityBreakdown?.high || 0}
                            </span>
                          </div>

                          <div className="priority-stat-row">
                            <span className="priority-stat-label">
                              <span className="task-priority task-priority-medium" style={{ fontSize: '0.75rem', padding: '2px 8px' }}>Medium</span>
                            </span>
                            <span className="priority-stat-val">
                              {taskStats.priorityBreakdown?.MEDIUM || taskStats.priorityBreakdown?.medium || 0}
                            </span>
                          </div>

                          <div className="priority-stat-row">
                            <span className="priority-stat-label">
                              <span className="task-priority task-priority-low" style={{ fontSize: '0.75rem', padding: '2px 8px' }}>Low</span>
                            </span>
                            <span className="priority-stat-val">
                              {taskStats.priorityBreakdown?.LOW || taskStats.priorityBreakdown?.low || 0}
                            </span>
                          </div>
                        </div>

                        {/* Category Distribution */}
                        <div className="breakdown-card">
                          <h3>Category Distribution</h3>
                          <div className="category-stat-list">
                            {Object.entries(taskStats.categoryBreakdown || {}).length === 0 ? (
                              <p style={{ color: 'var(--text-muted)', fontSize: '0.88rem' }}>No categorized tasks yet.</p>
                            ) : (
                              Object.entries(taskStats.categoryBreakdown || {}).map(([catName, count]) => (
                                <div key={catName} className="category-stat-row">
                                  <span className="category-stat-name">
                                    <Folder size={14} color="var(--accent-purple)" />
                                    {catName}
                                  </span>
                                  <span className="category-stat-count">{count} task{count === 1 ? '' : 's'}</span>
                                </div>
                              ))
                            )}
                          </div>
                        </div>
                      </div>

                      {/* 4. Overdue Tasks Section (Alert or Positive State) */}
                      <div className="content-section">
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 14 }}>
                          <h2 style={{ display: 'flex', alignItems: 'center', gap: 8, color: taskStats.overdueTasks > 0 ? '#BE185D' : 'inherit' }}>
                            {taskStats.overdueTasks > 0 ? (
                              <><AlertTriangle size={20} color="#BE185D" /> Overdue Tasks ({taskStats.overdueTasks})</>
                            ) : (
                              <><CheckCircle2 size={20} color="var(--success)" /> Overdue Status</>
                            )}
                          </h2>
                        </div>

                        {taskStats.overdueTasks > 0 ? (
                          <div className="task-cards">
                            {(taskStats.overdueTasksList || tasks.filter(t => t.status !== 'COMPLETED' && t.dueDate < getTodayString())).slice(0, 4).map(t => (
                              <article key={t.id} className="task-card overdue-item-card" onClick={() => { setPanel('tasks'); openTaskDetails(t as any); }} style={{ cursor: 'pointer' }}>
                                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 6 }}>
                                  <PriorityBadge priority={t.priority} />
                                  <span style={{ fontSize: '0.76rem', color: '#BE185D', fontWeight: 700, background: 'var(--pastel-pink-bg)', padding: '2px 8px', borderRadius: 'var(--radius-full)' }}>
                                    Due: {t.dueDate}
                                  </span>
                                </div>
                                <h3 style={{ marginTop: 8 }}>{t.title}</h3>
                                {t.description && <p className="task-desc">{t.description}</p>}
                                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 8, fontSize: '0.82rem', color: 'var(--text-muted)' }}>
                                  <span>{t.category || 'General'}</span>
                                  <button
                                    className="btn btn-soft btn-sm"
                                    onClick={(e) => {
                                      e.stopPropagation();
                                      toggleCompleteTask(String(t.id), e);
                                    }}
                                  >
                                    <CheckCircle2 size={14} /> Mark Complete
                                  </button>
                                </div>
                              </article>
                            ))}
                          </div>
                        ) : (
                          <div className="task-card" style={{ padding: '16px 20px', background: 'var(--pastel-mint-bg)', border: '1px solid var(--pastel-mint-border)' }}>
                            <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                              <CheckCircle2 size={20} color="var(--pastel-mint-text)" />
                              <span style={{ fontWeight: 600, color: 'var(--pastel-mint-text)' }}>
                                No overdue tasks. All deadlines are on track!
                              </span>
                            </div>
                          </div>
                        )}
                      </div>

                      {/* 5. Blocked Tasks Section (Alert or Positive State) */}
                      <div className="content-section">
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 14 }}>
                          <h2 style={{ display: 'flex', alignItems: 'center', gap: 8, color: taskStats.blockedTasks > 0 ? '#C2410C' : 'inherit' }}>
                            {taskStats.blockedTasks > 0 ? (
                              <><AlertTriangle size={20} color="#C2410C" /> Blocked Tasks ({taskStats.blockedTasks})</>
                            ) : (
                              <><CheckCircle2 size={20} color="var(--success)" /> Dependency Status</>
                            )}
                          </h2>
                        </div>

                        {taskStats.blockedTasks > 0 ? (
                          <div className="task-cards">
                            {(taskStats.blockedTasksList || tasks.filter(t => t.blocked && t.status !== 'COMPLETED')).slice(0, 4).map(t => (
                              <article key={t.id} className="task-card blocked-item-card" onClick={() => { setPanel('tasks'); openTaskDetails(t as any); }} style={{ cursor: 'pointer' }}>
                                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 6 }}>
                                  <PriorityBadge priority={t.priority} />
                                  <span className="dependency-badge-blocked">
                                    <AlertTriangle size={10} /> Waiting on prerequisites
                                  </span>
                                </div>
                                <h3 style={{ marginTop: 8 }}>{t.title}</h3>
                                {t.description && <p className="task-desc">{t.description}</p>}
                                <p style={{ fontSize: '0.82rem', color: 'var(--text-muted)', marginTop: 8 }}>
                                  Category: {t.category || 'General'} · Click to view prerequisite tasks
                                </p>
                              </article>
                            ))}
                          </div>
                        ) : (
                          <div className="task-card" style={{ padding: '16px 20px', background: 'var(--pastel-sky-bg)', border: '1px solid var(--pastel-sky-border)' }}>
                            <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                              <CheckCircle2 size={20} color="var(--pastel-sky-text)" />
                              <span style={{ fontWeight: 600, color: 'var(--pastel-sky-text)' }}>
                                No blocked tasks. Ready to make progress!
                              </span>
                            </div>
                          </div>
                        )}
                      </div>

                      {/* 6. Today's Priority Tasks */}
                      <div className="content-section">
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
                          <h2>Today&apos;s Tasks ({taskStats.todayTasks})</h2>
                          <button className="link-btn" onClick={() => { setPanel('tasks'); setActiveTab('today'); }}>View all today&apos;s tasks →</button>
                        </div>

                        {taskStats.todayTasks === 0 ? (
                          <div className="task-card" style={{ padding: 20, textAlign: 'center', color: 'var(--text-secondary)' }}>
                            <p>No tasks scheduled for today. Great job staying ahead!</p>
                          </div>
                        ) : (
                          <div className="task-cards">
                            {(taskStats.todayTasksList || tasks.filter(t => t.dueDate === getTodayString())).slice(0, 3).map(t => (
                              <article key={t.id} className="task-card" onClick={() => { setPanel('tasks'); openTaskDetails(t as any); }} style={{ cursor: 'pointer' }}>
                                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 6 }}>
                                  <PriorityBadge priority={t.priority} />
                                  <div style={{ display: 'flex', gap: 4, alignItems: 'center' }}>
                                    {t.blocked && (
                                      <span className="dependency-badge-blocked">
                                        <AlertTriangle size={10} /> Blocked
                                      </span>
                                    )}
                                    {t.category && <span className="category-badge" style={{ fontSize: '0.72rem' }}><Folder size={10} /> {t.category}</span>}
                                  </div>
                                </div>
                                <h3 style={{ marginTop: 8 }}>{t.title}</h3>
                                {t.tags && t.tags.length > 0 && (
                                  <div className="task-tags-list" style={{ marginTop: 6, marginBottom: 4 }}>
                                    {t.tags.slice(0, 2).map((tag, i) => (
                                      <span key={i} className="tag-pill">#{tag}</span>
                                    ))}
                                    {t.tags.length > 2 && (
                                      <span className="tag-pill-more">+{t.tags.length - 2}</span>
                                    )}
                                  </div>
                                )}
                                <p style={{ marginTop: 4, fontSize: '0.85rem', color: 'var(--text-muted)' }}>Due: Today {t.dueTime ? `· ${formatTimeAMPM(t.dueTime)}` : ''}</p>
                              </article>
                            ))}
                          </div>
                        )}
                      </div>

                      {/* 7. Upcoming Deadlines */}
                      <div className="content-section">
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 14 }}>
                          <h2>Upcoming Deadlines</h2>
                          <button className="link-btn" onClick={() => { setPanel('tasks'); setActiveTab('upcoming'); }}>View all upcoming →</button>
                        </div>

                        <div className="deadline-list">
                          {(taskStats.upcomingTasksList || tasks.filter(t => t.dueDate > getTodayString() && t.status !== 'COMPLETED')).slice(0, 4).map(d => (
                            <div key={d.id} className="deadline-item" onClick={() => { setPanel('tasks'); openTaskDetails(d as any); }} style={{ cursor: 'pointer' }}>
                              <div className="deadline-info">
                                <h4>{d.title}</h4>
                                <span>{d.category || 'General'} · Priority {d.priority.toLowerCase()}</span>
                              </div>
                              <span className="deadline-date">{d.dueDate}</span>
                            </div>
                          ))}
                          {(taskStats.upcomingTasksList || tasks.filter(t => t.dueDate > getTodayString() && t.status !== 'COMPLETED')).length === 0 && (
                            <div className="deadline-item">
                              <div className="deadline-info">
                                <h4>No upcoming deadlines</h4>
                                <span>You&apos;re all caught up on future tasks!</span>
                              </div>
                            </div>
                          )}
                        </div>
                      </div>
                    </>
                  )}
                </>
              )}
            </div>
          )}

          {/* TASKS PANEL (STEP 2 API & POSTGRESQL CONNECTED) */}
          {panel === 'tasks' && (
            <div className="fade-in">
              {/* 1. Page Header */}
              <div className="task-header">
                <div className="task-header-left">
                  <h1>Task Management</h1>
                  <p>Manage, prioritize, and track all your tasks stored in PostgreSQL.</p>
                </div>
                <div className="tasks-header-actions" style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
                  <button className="btn btn-soft" onClick={loadTasksFromBackend} title="Refresh tasks">
                    <RefreshCw size={16} />
                    Refresh Stats
                  </button>
                  <button className="btn btn-secondary" onClick={() => setIsAiModalOpen(true)} style={{ background: 'var(--brand-primary)', color: 'white', borderColor: 'var(--brand-primary)' }}>
                    <Sparkles size={18} /> Ask AI
                  </button>
                  <button className="btn btn-primary" onClick={openCreateModal}>
                    <Plus size={18} />
                    Create Task
                  </button>
                </div>
              </div>

              {/* 2. Task Summary Statistics (Real PostgreSQL Stats) */}
              <div className="task-stats-grid">
                <div className="stat-card">
                  <div className="stat-icon stat-icon-total"><ListTodo size={20} /></div>
                  <div className="stat-info">
                    <span>Total Tasks</span>
                    <strong>{taskStats ? taskStats.totalTasks : stats.total}</strong>
                  </div>
                </div>
                <div className="stat-card">
                  <div className="stat-icon stat-icon-today"><Calendar size={20} /></div>
                  <div className="stat-info">
                    <span>Today</span>
                    <strong>{taskStats ? taskStats.todayTasks : stats.today}</strong>
                  </div>
                </div>
                <div className="stat-card">
                  <div className="stat-icon stat-icon-progress"><Clock size={20} /></div>
                  <div className="stat-info">
                    <span>In Progress</span>
                    <strong>{taskStats ? taskStats.inProgressTasks : stats.inProgress}</strong>
                  </div>
                </div>
                <div className="stat-card">
                  <div className="stat-icon stat-icon-completed"><CheckCheck size={20} /></div>
                  <div className="stat-info">
                    <span>Completed</span>
                    <strong>{taskStats ? taskStats.completedTasks : stats.completed}</strong>
                  </div>
                </div>
                <div className="stat-card">
                  <div className="stat-icon stat-icon-overdue"><AlertTriangle size={20} /></div>
                  <div className="stat-info">
                    <span>Overdue</span>
                    <strong>{taskStats ? taskStats.overdueTasks : stats.overdue}</strong>
                  </div>
                </div>
                <div className="stat-card">
                  <div className="stat-icon stat-icon-blocked"><LinkIcon size={20} /></div>
                  <div className="stat-info">
                    <span>Blocked</span>
                    <strong>{taskStats ? taskStats.blockedTasks : 0}</strong>
                  </div>
                </div>
              </div>

              {/* 3 & 4 & 5. Toolbar: Tabs, Search & Filters */}
              <div className="task-toolbar">
                {/* Task Tabs */}
                <div className="task-tabs">
                  <button className={`task-tab ${activeTab === 'all' ? 'task-tab-active' : ''}`} onClick={() => setActiveTab('all')}>
                    All ({stats.total})
                  </button>
                  <button className={`task-tab ${activeTab === 'today' ? 'task-tab-active' : ''}`} onClick={() => setActiveTab('today')}>
                    Today ({stats.today})
                  </button>
                  <button className={`task-tab ${activeTab === 'upcoming' ? 'task-tab-active' : ''}`} onClick={() => setActiveTab('upcoming')}>
                    Upcoming
                  </button>
                  <button className={`task-tab ${activeTab === 'completed' ? 'task-tab-active' : ''}`} onClick={() => setActiveTab('completed')}>
                    Completed ({stats.completed})
                  </button>
                  <button className={`task-tab ${activeTab === 'overdue' ? 'task-tab-active' : ''}`} onClick={() => setActiveTab('overdue')}>
                    Overdue ({stats.overdue})
                  </button>
                </div>

                {/* Filter Bar & Search */}
                <div className="task-filter-bar">
                  <div className="task-search-wrap">
                    <Search size={16} className="task-search-icon" />
                    <input
                      type="text"
                      className="task-search-input"
                      placeholder="Search tasks by title or description..."
                      value={searchQuery}
                      onChange={e => setSearchQuery(e.target.value)}
                    />
                  </div>

                  <div className="filter-group">
                    {/* Status Filter */}
                    <select className="filter-select" value={statusFilter} onChange={e => setStatusFilter(e.target.value)}>
                      <option value="all">Status: All</option>
                      <option value="TODO">Status: To Do</option>
                      <option value="IN_PROGRESS">Status: In Progress</option>
                      <option value="COMPLETED">Status: Completed</option>
                    </select>

                    {/* Priority Filter */}
                    <select className="filter-select" value={priorityFilter} onChange={e => setPriorityFilter(e.target.value)}>
                      <option value="all">Priority: All</option>
                      <option value="HIGH">Priority: High</option>
                      <option value="MEDIUM">Priority: Medium</option>
                      <option value="LOW">Priority: Low</option>
                    </select>

                    {/* Category Filter */}
                    <select className="filter-select" value={categoryFilter} onChange={e => setCategoryFilter(e.target.value)}>
                      <option value="all">Category: All</option>
                      {categoriesList.map(cat => (
                        <option key={cat} value={cat}>Category: {cat}</option>
                      ))}
                    </select>

                    {/* Tag Filter */}
                    <select className="filter-select" value={tagFilter} onChange={e => setTagFilter(e.target.value)}>
                      <option value="all">Tag: All</option>
                      {tagsList.map(tag => (
                        <option key={tag} value={tag}>Tag: #{tag}</option>
                      ))}
                    </select>
                  </div>
                </div>
              </div>

              {/* Loading State */}
              {isLoadingTasks && (
                <div className="empty-state fade-in" style={{ padding: 48 }}>
                  <Loader2 size={36} className="animate-spin" style={{ color: 'var(--accent-purple)', marginBottom: 12 }} />
                  <h3>Loading tasks...</h3>
                  <p>Fetching your personal tasks from PostgreSQL database.</p>
                </div>
              )}

              {/* Error State */}
              {!isLoadingTasks && taskApiError && (
                <div className="empty-state fade-in" style={{ padding: 48, borderColor: 'var(--pastel-pink-border)' }}>
                  <AlertCircle size={36} color="var(--pastel-pink-text)" style={{ marginBottom: 12 }} />
                  <h3 style={{ color: 'var(--pastel-pink-text)' }}>{taskApiError}</h3>
                  <p>Check if the Spring Boot backend server on port 8081 and PostgreSQL database are running.</p>
                  <button className="btn btn-primary" onClick={loadTasksFromBackend} style={{ marginTop: 12 }}>
                    <RefreshCw size={16} /> Retry
                  </button>
                </div>
              )}

              {/* Empty / Task Cards Grid State */}
              {!isLoadingTasks && !taskApiError && (
                filteredTasks.length === 0 ? (
                  <div className="empty-state fade-in">
                    <div className="empty-icon-wrap"><CheckSquare size={32} /></div>
                    <h3>No tasks found</h3>
                    <p>No tasks match your selected search terms or filters. Try adjusting your filters or create a new task.</p>
                    <div style={{ display: 'flex', gap: 12 }}>
                      <button className="btn btn-primary" onClick={openCreateModal}>
                        <Plus size={16} /> Create Task
                      </button>
                      <button className="btn btn-soft" onClick={resetFilters}>Clear Filters</button>
                    </div>
                  </div>
                ) : (
                  <div className="tasks-grid">
                    {filteredTasks.map(task => {
                      const isCompleted = task.status === 'COMPLETED';
                      const isOverdue = !isCompleted && task.dueDate < getTodayString();

                      return (
                        <div
                          key={task.id}
                          className={`task-item-card ${isCompleted ? 'completed-card' : ''}`}
                        >
                          <div className="task-item-top">
                            <button
                              className={`task-checkbox-btn ${isCompleted ? 'checked' : ''}`}
                              onClick={e => toggleCompleteTask(task.id, e)}
                              title={isCompleted ? 'Mark incomplete' : 'Mark complete'}
                            >
                              {isCompleted ? <CheckCircle2 size={20} /> : <Circle size={20} />}
                            </button>

                            <div className="task-title-wrap" onClick={() => openTaskDetails(task)}>
                              <h3 className={`task-item-title ${isCompleted ? 'task-title-done' : ''}`}>
                                {task.title}
                              </h3>
                            </div>

                            <div className="task-card-actions">
                              <button
                                className="action-icon-btn"
                                onClick={e => openEditModal(task, e)}
                                title="Edit task"
                              >
                                <Edit3 size={15} />
                              </button>
                              <button
                                className="action-icon-btn delete-btn"
                                onClick={e => confirmDeleteTask(task.id, e)}
                                title="Delete task"
                              >
                                <Trash2 size={15} />
                              </button>
                            </div>
                          </div>

                          {task.description && (
                            <p className="task-item-desc" onClick={() => openTaskDetails(task)} style={{ cursor: 'pointer' }}>
                              {task.description}
                            </p>
                          )}

                          <div className="task-badges">
                            <PriorityBadge priority={task.priority} />
                            <StatusBadge status={task.status} />
                            {task.category && (
                              <span className="category-badge">
                                <Folder size={11} /> {task.category}
                              </span>
                            )}
                            {task.blocked ? (
                              <span className="dependency-badge-blocked" title={`Blocked: waiting for ${task.uncompletedDependencyCount || 1} prerequisite task(s)`}>
                                <AlertTriangle size={11} /> Blocked
                              </span>
                            ) : task.dependencyCount && task.dependencyCount > 0 ? (
                              <span className="dependency-badge" title={`${task.dependencyCount} prerequisite dependency(ies)`}>
                                <LinkIcon size={11} /> {task.dependencyCount} dep{task.dependencyCount === 1 ? '' : 's'}
                              </span>
                            ) : null}
                          </div>

                          {task.tags && task.tags.length > 0 && (
                            <div className="task-tags-list">
                              {task.tags.slice(0, 3).map((tag, i) => (
                                <span key={i} className="tag-pill">#{tag}</span>
                              ))}
                              {task.tags.length > 3 && (
                                <span className="tag-pill-more">+{task.tags.length - 3} more</span>
                              )}
                            </div>
                          )}

                          <div className="task-item-meta">
                            <span className={`meta-item ${isOverdue ? 'meta-overdue' : ''}`}>
                              <Calendar size={13} />
                              {task.dueDate} {task.dueTime ? `at ${formatTimeAMPM(task.dueTime)}` : ''}
                              {isOverdue && ' (Overdue)'}
                            </span>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                )
              )}

            </div>
          )}

          {/* CATEGORIES PANEL */}
          {panel === 'categories' && (
            <div className="fade-in">
              {/* Category Header */}
              <div className="task-header">
                <div className="task-header-left">
                  <h1>My Categories</h1>
                  <p>Organize, group, and structure your tasks with custom categories.</p>
                </div>
                <div style={{ display: 'flex', gap: 10 }}>
                  <button className="btn btn-soft" onClick={seedStarterCategories} disabled={isSubmittingCategory} title="Load starter categories">
                    <Sparkles size={16} /> Starter Categories
                  </button>
                  <button className="btn btn-soft" onClick={loadCategoriesFromBackend} title="Refresh categories">
                    <RefreshCw size={16} />
                  </button>
                  <button className="btn btn-primary" onClick={openCreateCategoryModal}>
                    <Plus size={18} />
                    Create Category
                  </button>
                </div>
              </div>

              {categoryActionError && (
                <div className="form-error fade-in" style={{ marginBottom: 20 }}>
                  {categoryActionError}
                </div>
              )}

              {/* Loading State */}
              {isLoadingCategories && (
                <div className="empty-state fade-in" style={{ padding: 48 }}>
                  <Loader2 size={36} className="animate-spin" style={{ color: 'var(--accent-purple)', marginBottom: 12 }} />
                  <h3>Loading categories...</h3>
                  <p>Fetching your workspace categories from PostgreSQL database.</p>
                </div>
              )}

              {/* Error State */}
              {!isLoadingCategories && categoryApiError && (
                <div className="empty-state fade-in" style={{ padding: 48, borderColor: 'var(--pastel-pink-border)' }}>
                  <AlertCircle size={36} color="var(--pastel-pink-text)" style={{ marginBottom: 12 }} />
                  <h3 style={{ color: 'var(--pastel-pink-text)' }}>{categoryApiError}</h3>
                  <p>Unable to load categories from the server. Check if Spring Boot backend is active.</p>
                  <button className="btn btn-primary" onClick={loadCategoriesFromBackend} style={{ marginTop: 12 }}>
                    <RefreshCw size={16} /> Retry
                  </button>
                </div>
              )}

              {/* Empty / Categories List */}
              {!isLoadingCategories && !categoryApiError && (
                categories.length === 0 ? (
                  <div className="empty-state fade-in">
                    <div className="empty-icon-wrap"><Folder size={32} /></div>
                    <h3>No categories yet.</h3>
                    <p>Create categories like Study, Project, Placement, or Personal to organize your tasks.</p>
                    <div style={{ display: 'flex', gap: 10, justifyContent: 'center', marginTop: 12 }}>
                      <button className="btn btn-primary" onClick={openCreateCategoryModal}>
                        <Plus size={16} /> Create Category
                      </button>
                      <button className="btn btn-soft" onClick={seedStarterCategories} disabled={isSubmittingCategory}>
                        <Sparkles size={16} /> Load Starter Categories
                      </button>
                    </div>
                  </div>
                ) : (
                  <div className="categories-grid">
                    {categories.map(cat => (
                      <div key={cat.id} className="category-card">
                        <div>
                          <div className="category-card-top">
                            <span className="category-card-badge">
                              <Folder size={15} /> {cat.name}
                            </span>
                          </div>
                          <p className="category-card-desc">
                            {cat.description ? cat.description : <span style={{ color: 'var(--text-muted)', fontStyle: 'italic' }}>No description</span>}
                          </p>
                        </div>

                        <div className="category-card-footer">
                          <span className="category-task-count">
                            {cat.taskCount !== undefined ? `${cat.taskCount} task${cat.taskCount === 1 ? '' : 's'}` : 'Category'}
                          </span>
                          <div className="category-actions">
                            <button
                              className="btn btn-soft"
                              style={{ padding: '6px 14px', fontSize: '0.84rem' }}
                              onClick={() => openEditCategoryModal(cat)}
                            >
                              <Edit3 size={13} /> Edit
                            </button>
                            <button
                              className="btn btn-danger"
                              style={{ padding: '6px 14px', fontSize: '0.84rem' }}
                              onClick={() => confirmDeleteCategory(cat)}
                            >
                              <Trash2 size={13} /> Delete
                            </button>
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                )
              )}
            </div>
          )}

          {/* OTHER PANELS (Preserved Placeholders) */}
          {panel === 'documents' && (
            <div className="fade-in panel-placeholder">
              <div className="placeholder-icon"><FileText size={32} /></div>
              <h2>Documents &amp; Notes</h2>
              <p>Store and access your study materials and personal notes here. Coming soon.</p>
            </div>
          )}


          {panel === 'calendar' && (
            <div className="fade-in">
              <div className="calendar-layout">
                {/* Main Calendar Card */}
                <div className="calendar-main-card">
                  {/* Calendar Top Controls Header */}
                  <div className="calendar-header">
                    <div className="calendar-title-group">
                      <div className="calendar-nav-btns">
                        <button
                          type="button"
                          className="calendar-nav-btn"
                          onClick={handlePrevMonth}
                          title="Previous Month"
                        >
                          <ChevronLeft size={18} />
                        </button>
                        <button
                          type="button"
                          className="calendar-nav-btn"
                          onClick={handleNextMonth}
                          title="Next Month"
                        >
                          <ChevronRight size={18} />
                        </button>
                      </div>
                      <h2 className="calendar-month-title">{formattedMonthYear}</h2>
                      <button
                        type="button"
                        className="calendar-today-btn"
                        onClick={handleJumpToToday}
                      >
                        Today
                      </button>
                    </div>

                    <div className="calendar-actions" style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                      <button
                        type="button"
                        className="btn btn-secondary btn-sm"
                        style={{
                          background: 'linear-gradient(135deg, #8b5cf6, #7c3aed)',
                          color: 'white',
                          border: 'none',
                          padding: '8px 14px',
                          fontSize: '0.85rem',
                          fontWeight: 600,
                          boxShadow: '0 2px 8px rgba(139, 92, 246, 0.25)'
                        }}
                        onClick={() => openAiAssistant(calendarSelectedDate)}
                        title="Plan tasks for this day with AI"
                      >
                        <Sparkles size={14} /> AI Plan Day
                      </button>
                      <button
                        className="btn btn-primary btn-sm"
                        style={{ padding: '8px 16px', fontSize: '0.85rem' }}
                        onClick={() => {
                          openCreateModal();
                          setFormDueDate(calendarSelectedDate);
                        }}
                      >
                        <Plus size={16} /> Add Task
                      </button>
                    </div>
                  </div>

                  {/* Day of Week Headers */}
                  <div className="calendar-weekdays-grid">
                    {['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'].map(day => (
                      <div key={day} className="calendar-weekday-header">
                        {day}
                      </div>
                    ))}
                  </div>

                  {/* Calendar Days Grid */}
                  <div className="calendar-days-grid">
                    {calendarDays.map(item => {
                      const dayTasks = tasksByDate[item.dateString] || [];
                      const isSelected = item.dateString === calendarSelectedDate;
                      const displayTasks = dayTasks.slice(0, 3);
                      const extraCount = dayTasks.length - 3;

                      return (
                        <div
                          key={item.dateString}
                          className={`calendar-day-cell ${
                            !item.isCurrentMonth ? 'calendar-day-cell-other-month' : ''
                          } ${item.isToday ? 'calendar-day-cell-today' : ''} ${
                            isSelected ? 'calendar-day-cell-selected' : ''
                          }`}
                          onClick={() => setCalendarSelectedDate(item.dateString)}
                        >
                          <div className="calendar-day-top">
                            <span
                              className={`calendar-day-number ${
                                item.isToday ? 'calendar-day-number-today' : ''
                              }`}
                            >
                              {item.dayNumber}
                            </span>
                            {dayTasks.length > 0 && (
                              <span className="calendar-day-count-badge">
                                {dayTasks.length}
                              </span>
                            )}
                          </div>

                          <div className="calendar-tasks-list">
                            {displayTasks.map(task => {
                              const isCompleted = task.status === 'COMPLETED';
                              const priorityClass =
                                task.priority === 'HIGH'
                                  ? 'calendar-task-pill-high'
                                  : task.priority === 'LOW'
                                  ? 'calendar-task-pill-low'
                                  : 'calendar-task-pill-medium';

                              return (
                                <div
                                  key={task.id}
                                  className={`calendar-task-pill ${
                                    isCompleted ? 'calendar-task-pill-completed' : priorityClass
                                  }`}
                                  title={`${task.title}${task.dueTime ? ` (${formatTimeAMPM(task.dueTime)})` : ''}`}
                                  onClick={e => {
                                    e.stopPropagation();
                                    openTaskDetails(task);
                                  }}
                                >
                                  {isCompleted ? <CheckCheck size={11} /> : <Circle size={10} />}
                                  <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', flex: 1 }}>
                                    {task.dueTime ? `${formatTimeAMPM(task.dueTime).split(' ')[0]} ` : ''}
                                    {task.title}
                                  </span>
                                  {task.documentCount !== undefined && task.documentCount > 0 && (
                                    <span title={`${task.documentCount} document(s) attached`} style={{ display: 'inline-flex', alignItems: 'center' }}>
                                      <FileText size={11} style={{ flexShrink: 0, opacity: 0.95 }} />
                                    </span>
                                  )}
                                </div>
                              );
                            })}

                            {extraCount > 0 && (
                              <div className="calendar-more-pill">
                                +{extraCount} more
                              </div>
                            )}
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </div>

                {/* Day Agenda Side Panel */}
                <div className="calendar-agenda-card">
                  <div className="calendar-agenda-header">
                    <div className="calendar-agenda-date">{formattedSelectedDate}</div>
                    <div className="calendar-agenda-subtitle">
                      {selectedDateTasks.length === 0
                        ? 'No tasks scheduled'
                        : `${selectedDateTasks.length} task${selectedDateTasks.length === 1 ? '' : 's'} scheduled`}
                    </div>
                  </div>

                  <div style={{ display: 'flex', gap: 8, marginBottom: 14 }}>
                    <button
                      className="btn btn-soft btn-sm"
                      style={{ flex: 1, padding: '9px 8px', fontSize: '0.82rem' }}
                      onClick={() => {
                        openCreateModal();
                        setFormDueDate(calendarSelectedDate);
                      }}
                    >
                      <Plus size={13} /> Add Task
                    </button>
                    <button
                      className="btn btn-soft btn-sm"
                      style={{
                        flex: 1,
                        padding: '9px 8px',
                        fontSize: '0.82rem',
                        color: '#7c3aed',
                        background: '#f5f3ff',
                        border: '1px solid #ddd6fe'
                      }}
                      onClick={() => openAiAssistant(calendarSelectedDate)}
                      title="Plan this day with AI"
                    >
                      <Sparkles size={13} /> AI Plan Day
                    </button>
                  </div>

                  <div className="calendar-agenda-list">
                    {selectedDateTasks.length === 0 ? (
                      <div
                        style={{
                          textAlign: 'center',
                          padding: '32px 16px',
                          color: 'var(--text-muted)',
                        }}
                      >
                        <Calendar size={28} style={{ opacity: 0.4, marginBottom: 8 }} />
                        <p style={{ fontSize: '0.86rem', margin: 0 }}>
                          Nothing scheduled for this day.
                        </p>
                      </div>
                    ) : (
                      selectedDateTasks.map(task => {
                        const isCompleted = task.status === 'COMPLETED';
                        return (
                          <div
                            key={task.id}
                            className={`calendar-agenda-item ${
                              isCompleted ? 'calendar-agenda-item-completed' : ''
                            }`}
                            onClick={() => openTaskDetails(task)}
                          >
                            <button
                              type="button"
                              className={`task-check ${isCompleted ? 'task-check-done' : ''}`}
                              style={{ marginTop: 2, flexShrink: 0 }}
                              onClick={e => {
                                e.stopPropagation();
                                toggleCompleteTask(task.id, e);
                              }}
                              title={isCompleted ? 'Mark as todo' : 'Mark as completed'}
                            >
                              {isCompleted && <CheckCircle2 size={16} />}
                            </button>

                            <div className="calendar-agenda-item-content">
                              <div className="calendar-agenda-item-title">{task.title}</div>
                              <div className="calendar-agenda-item-meta">
                                {task.dueTime && (
                                  <span>
                                    <Clock size={11} style={{ display: 'inline', verticalAlign: 'middle', marginRight: 2 }} />
                                    {formatTimeAMPM(task.dueTime)}
                                  </span>
                                )}
                                <span className={`priority-badge priority-${task.priority.toLowerCase()}`} style={{ fontSize: '0.68rem', padding: '1px 6px' }}>
                                  {task.priority}
                                </span>
                                {task.category && (
                                  <span style={{ fontSize: '0.74rem', color: 'var(--text-secondary)' }}>
                                    {task.category}
                                  </span>
                                )}
                                {task.documentCount !== undefined && task.documentCount > 0 && (
                                  <span className="calendar-agenda-doc-badge" title={`${task.documentCount} document(s) attached from Knowledge Base`}>
                                    <FileText size={11} style={{ display: 'inline', verticalAlign: 'middle', marginRight: 3 }} />
                                    {task.documentCount} {task.documentCount === 1 ? 'Doc' : 'Docs'}
                                  </span>
                                )}
                              </div>
                            </div>
                          </div>
                        );
                      })
                    )}
                  </div>
                </div>
              </div>
            </div>
          )}

          {panel === 'assistant' && (
            <div className="fade-in">
              <div className="content-greeting"><h1>Productivity Assistant</h1><p>Your AI-powered schedule &amp; focus companion.</p></div>
              <div className="insight-card">
                <span className="insight-label">MindFlow Assistant</span>
                <p>Welcome! I can help you prioritize tasks, plan your schedule, and maintain consistent focus blocks throughout your day. Feature coming soon.</p>
              </div>
            </div>
          )}

          {panel === 'profile' && (
            <div className="fade-in">
              <div className="content-greeting"><h1>Profile Settings</h1><p>Your account details and preferences.</p></div>
              <div className="profile-card">
                <div className="profile-avatar">{user.name.charAt(0).toUpperCase()}</div>
                <h2>{user.name}</h2>
                <p className="profile-mobile-text">{user.mobile}</p>
                <div className="profile-detail"><span>Account status</span><span>Active</span></div>
                <div className="profile-detail"><span>Member since</span><span>{getMemberSince(user.mobile)}</span></div>
                <div className="profile-detail"><span>Mobile</span><span>{user.mobile}</span></div>
              </div>
            </div>
          )}

        </section>
      </div>

      {/* CREATE / EDIT TASK MODAL */}
      {isTaskModalOpen && (
        <div className="modal-overlay" onClick={() => !isSubmitting && setIsTaskModalOpen(false)}>
          <div className="modal-card" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h2>{editingTask ? 'Edit Task' : 'Create New Task'}</h2>
              <button className="close-btn" onClick={() => !isSubmitting && setIsTaskModalOpen(false)} disabled={isSubmitting}>
                <X size={18} />
              </button>
            </div>

            <form onSubmit={handleSaveTask}>
              <div className="modal-body">
                {/* Title (Required) */}
                <div className="form-group">
                  <label>Task Title *</label>
                  <input
                    type="text"
                    className="form-input"
                    placeholder="e.g., Finalize project presentation"
                    value={formTitle}
                    onChange={e => {
                      setFormTitle(e.target.value);
                      if (formErrors.title) setFormErrors(prev => ({ ...prev, title: undefined }));
                    }}
                    disabled={isSubmitting}
                  />
                  {formErrors.title && <p style={{ color: 'var(--error)', fontSize: '0.82rem', marginTop: 4 }}>{formErrors.title}</p>}
                </div>

                {/* Description (Optional) */}
                <div className="form-group">
                  <label>Description (Optional)</label>
                  <textarea
                    className="form-input"
                    rows={3}
                    placeholder="Add details, notes, or contextual links..."
                    value={formDescription}
                    onChange={e => setFormDescription(e.target.value)}
                    style={{ resize: 'vertical' }}
                    disabled={isSubmitting}
                  />
                </div>

                {/* Priority & Status Row */}
                <div className="form-grid-2">
                  <div className="form-group">
                    <label>Priority *</label>
                    <select
                      className="form-input"
                      value={formPriority}
                      onChange={e => setFormPriority(e.target.value as Priority)}
                      disabled={isSubmitting}
                    >
                      <option value="LOW">Low</option>
                      <option value="MEDIUM">Medium</option>
                      <option value="HIGH">High</option>
                    </select>
                  </div>

                  <div className="form-group">
                    <label>Status</label>
                    <select
                      className="form-input"
                      value={formStatus}
                      onChange={e => setFormStatus(e.target.value as Status)}
                      disabled={isSubmitting}
                    >
                      <option value="TODO">To Do</option>
                      <option value="IN_PROGRESS">In Progress</option>
                      <option value="COMPLETED">Completed</option>
                    </select>
                  </div>
                </div>

                {/* Due Date & Time Row */}
                <div className="form-grid-2">
                  <div className="form-group">
                    <label>Due Date</label>
                    <input
                      type="date"
                      className="form-input"
                      value={formDueDate}
                      onChange={e => setFormDueDate(e.target.value)}
                      disabled={isSubmitting}
                    />
                  </div>

                  <div className="form-group">
                    <label>Due Time</label>
                    <select
                      className="form-input"
                      value={formDueTime || '18:00'}
                      onChange={e => setFormDueTime(e.target.value)}
                      disabled={isSubmitting}
                    >
                      <optgroup label="Morning (AM)">
                        {HOURLY_TIME_OPTIONS.filter(t => t.period === 'AM').map(opt => (
                          <option key={opt.value} value={opt.value}>
                            {opt.label}
                          </option>
                        ))}
                      </optgroup>
                      <optgroup label="Afternoon / Evening (PM)">
                        {HOURLY_TIME_OPTIONS.filter(t => t.period === 'PM').map(opt => (
                          <option key={opt.value} value={opt.value}>
                            {opt.label}
                          </option>
                        ))}
                      </optgroup>
                      {formDueTime && !HOURLY_TIME_OPTIONS.some(t => t.value === formDueTime) && (
                        <option value={formDueTime}>
                          {formatTimeAMPM(formDueTime)}
                        </option>
                      )}
                    </select>
                  </div>
                </div>

                {/* Category Field */}
                <div className="form-group">
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 4 }}>
                    <label style={{ margin: 0 }}>Category</label>
                    {!isCustomCategory ? (
                      <button
                        type="button"
                        className="btn-link"
                        style={{ fontSize: '0.78rem', color: 'var(--brand-primary)', cursor: 'pointer', background: 'none', border: 'none', padding: 0 }}
                        onClick={() => {
                          setIsCustomCategory(true);
                          setCustomCategoryInput('');
                          setFormCategory('');
                        }}
                      >
                        + Custom Category
                      </button>
                    ) : (
                      <button
                        type="button"
                        className="btn-link"
                        style={{ fontSize: '0.78rem', color: 'var(--brand-primary)', cursor: 'pointer', background: 'none', border: 'none', padding: 0 }}
                        onClick={() => {
                          setIsCustomCategory(false);
                          setFormCategory(categoriesList[0] || 'General');
                        }}
                      >
                        Select Existing
                      </button>
                    )}
                  </div>
                  {!isCustomCategory ? (
                    <select
                      className="form-input"
                      value={formCategory}
                      onChange={e => {
                        if (e.target.value === '__NEW__') {
                          setIsCustomCategory(true);
                          setCustomCategoryInput('');
                          setFormCategory('');
                        } else {
                          setFormCategory(e.target.value);
                        }
                      }}
                      disabled={isSubmitting}
                    >
                      <option value="">Select category</option>
                      {categoriesList.map(cat => (
                        <option key={cat} value={cat}>{cat}</option>
                      ))}
                      <option value="__NEW__">➕ + Add New Custom Category...</option>
                    </select>
                  ) : (
                    <input
                      type="text"
                      className="form-input"
                      placeholder="Type custom category name..."
                      value={customCategoryInput}
                      onChange={e => {
                        setCustomCategoryInput(e.target.value);
                        setFormCategory(e.target.value);
                      }}
                      autoFocus
                      disabled={isSubmitting}
                    />
                  )}
                </div>

                {/* Tags Field (Chip input with autocomplete) */}
                <div className="form-group" style={{ marginBottom: 0 }}>
                  <label>Tags</label>
                  <div className="tag-input-container">
                    {formTagsList.map(tag => (
                      <span key={tag} className="tag-chip">
                        #{tag}
                        <button
                          type="button"
                          className="tag-chip-remove"
                          onClick={() => removeFormTag(tag)}
                          title={`Remove #${tag}`}
                        >
                          <X size={11} />
                        </button>
                      </span>
                    ))}
                    <input
                      type="text"
                      className="tag-input-field"
                      placeholder={formTagsList.length === 0 ? "Type a tag (e.g. java, springboot) and press Enter..." : "Add another tag..."}
                      value={tagInputValue}
                      onChange={e => {
                        setTagInputValue(e.target.value);
                        setShowTagSuggestions(true);
                      }}
                      onKeyDown={handleTagInputKeyDown}
                      onFocus={() => setShowTagSuggestions(true)}
                      onBlur={() => setTimeout(() => setShowTagSuggestions(false), 200)}
                      disabled={isSubmitting}
                    />
                    {showTagSuggestions && matchingSuggestions.length > 0 && (
                      <div className="tag-suggestions-box">
                        {matchingSuggestions.map(st => (
                          <div
                            key={st.id}
                            className="tag-suggestion-item"
                            onMouseDown={(e) => {
                              e.preventDefault();
                              selectTagSuggestion(st.name);
                            }}
                          >
                            <span>#{st.name}</span>
                            {st.taskCount !== undefined && st.taskCount > 0 && (
                              <span className="tag-suggestion-count">{st.taskCount} task{st.taskCount === 1 ? '' : 's'}</span>
                            )}
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                  <span style={{ fontSize: '0.78rem', color: 'var(--text-muted)', marginTop: 4, display: 'block' }}>
                    Type a tag and press <strong>Enter</strong> or comma to add. Existing tags will be suggested.
                  </span>
                </div>

                {/* Link Document from Knowledge Base (Optional) */}
                {!editingTask && (
                  <div className="form-group" style={{ marginTop: 14 }}>
                    <label style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: '0.85rem' }}>
                      <Paperclip size={13} color="var(--accent-purple)" /> Link Document from Knowledge Base (Optional)
                    </label>
                    <select
                      className="form-input"
                      value={formAttachDocId}
                      onChange={e => setFormAttachDocId(e.target.value)}
                      disabled={isSubmitting}
                    >
                      <option value="">-- No document attached --</option>
                      {allUserDocuments.map(doc => (
                        <option key={doc.id} value={doc.id}>
                          {doc.title || doc.fileName} ({doc.fileType} • {(doc.fileSize / 1024).toFixed(1)} KB)
                        </option>
                      ))}
                    </select>
                    <span style={{ fontSize: '0.78rem', color: 'var(--text-muted)', marginTop: 4, display: 'block' }}>
                      Connects lecture notes, project specifications, or PDFs directly to this calendar task.
                    </span>
                  </div>
                )}
              </div>

              <div className="modal-footer">
                <button type="button" className="btn btn-soft" onClick={() => setIsTaskModalOpen(false)} disabled={isSubmitting}>
                  Cancel
                </button>
                <button type="submit" className="btn btn-primary" disabled={isSubmitting}>
                  {isSubmitting ? 'Saving...' : editingTask ? 'Save Changes' : 'Create Task'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* TASK DETAILS MODAL */}
      {detailTask && (
        <div className="modal-overlay" onClick={() => setDetailTask(null)}>
          <div className="modal-card" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                <PriorityBadge priority={detailTask.priority} />
                <StatusBadge status={detailTask.status} />
              </div>
              <button className="close-btn" onClick={() => setDetailTask(null)}><X size={18} /></button>
            </div>

            <div className="modal-body">
              {/* Blocked or Ready Status Banner */}
              {taskDependencies.length > 0 && (
                taskDependencies.some(d => !d.isCompleted) ? (
                  <div className="dependency-blocked-banner">
                    <AlertTriangle size={18} />
                    <span>
                      <strong>Blocked:</strong> Waiting for {taskDependencies.filter(d => !d.isCompleted).length} prerequisite task(s) to be completed.
                    </span>
                  </div>
                ) : (
                  <div className="dependency-ready-banner">
                    <CheckCircle2 size={16} />
                    <span>All prerequisite tasks are completed! Ready to proceed.</span>
                  </div>
                )
              )}

              <h2 style={{ fontSize: '1.4rem', fontWeight: 800, marginBottom: 12 }}>{detailTask.title}</h2>

              {detailTask.description ? (
                <p style={{ color: 'var(--text-secondary)', fontSize: '0.98rem', lineHeight: 1.6, marginBottom: 20 }}>
                  {detailTask.description}
                </p>
              ) : (
                <p style={{ color: 'var(--text-muted)', fontStyle: 'italic', marginBottom: 20 }}>No description provided.</p>
              )}

              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(130px, 1fr))', gap: 16, background: 'var(--pastel-bg)', padding: 16, borderRadius: 'var(--radius-md)', marginBottom: 20 }}>
                <div>
                  <span style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-muted)', display: 'block', marginBottom: 2 }}>DUE DATE</span>
                  <span style={{ fontSize: '0.92rem', fontWeight: 600 }}>{detailTask.dueDate} {detailTask.dueTime ? `at ${formatTimeAMPM(detailTask.dueTime)}` : ''}</span>
                </div>

                <div>
                  <span style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-muted)', display: 'block', marginBottom: 2 }}>CATEGORY</span>
                  <span style={{ fontSize: '0.92rem', fontWeight: 600 }}>{detailTask.category || 'General'}</span>
                </div>

                <div>
                  <span style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-muted)', display: 'block', marginBottom: 2 }}>CREATED DATE</span>
                  <span style={{ fontSize: '0.92rem', fontWeight: 600 }}>{detailTask.createdAt}</span>
                </div>
              </div>

              {detailTask.tags && detailTask.tags.length > 0 && (
                <div style={{ marginBottom: 20 }}>
                  <span style={{ fontSize: '0.8rem', fontWeight: 700, color: 'var(--text-muted)', display: 'block', marginBottom: 6 }}>TAGS</span>
                  <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                    {detailTask.tags.map((tag, i) => (
                      <span key={i} className="tag-pill" style={{ padding: '4px 10px', fontSize: '0.8rem' }}>#{tag}</span>
                    ))}
                  </div>
                </div>
              )}

              {/* Dependencies (Prerequisites) Section */}
              <div className="dependency-section">
                <div className="dependency-section-title">
                  <h4>Prerequisite Dependencies ({taskDependencies.length})</h4>
                  {!isAddingDependency && (
                    <button
                      className="btn btn-soft btn-sm"
                      style={{ padding: '4px 10px', fontSize: '0.78rem' }}
                      onClick={() => { setIsAddingDependency(true); setDependencyError(null); }}
                    >
                      <Plus size={13} /> Add Dependency
                    </button>
                  )}
                </div>

                {dependencyError && (
                  <div className="form-error fade-in" style={{ marginBottom: 10, fontSize: '0.82rem' }}>
                    {dependencyError}
                  </div>
                )}

                {isLoadingDependencies ? (
                  <div style={{ padding: 12, textAlign: 'center', color: 'var(--text-muted)', fontSize: '0.85rem' }}>
                    <Loader2 size={16} className="animate-spin" style={{ margin: '0 auto 4px' }} />
                    Loading dependencies...
                  </div>
                ) : taskDependencies.length === 0 && !isAddingDependency ? (
                  <p style={{ color: 'var(--text-muted)', fontSize: '0.86rem', fontStyle: 'italic', margin: 0 }}>
                    No prerequisite dependencies. This task can be started anytime.
                  </p>
                ) : (
                  <div className="dependency-list">
                    {taskDependencies.map(dep => (
                      <div
                        key={dep.id}
                        className={`dependency-item ${dep.isCompleted ? 'dependency-item-done' : ''}`}
                      >
                        <div className="dependency-item-info">
                          {dep.isCompleted ? (
                            <CheckCircle2 size={16} className="dependency-check-done" />
                          ) : (
                            <Clock size={16} className="dependency-check-waiting" />
                          )}
                          <span className="dependency-item-title">
                            {dep.dependsOnTaskTitle}
                          </span>
                          {dep.dependsOnTaskCategory && (
                            <span className="category-badge" style={{ fontSize: '0.7rem', padding: '1px 6px' }}>
                              {dep.dependsOnTaskCategory}
                            </span>
                          )}
                          <span style={{ fontSize: '0.72rem', color: dep.isCompleted ? 'var(--success)' : 'var(--text-muted)' }}>
                            {dep.isCompleted ? '✓ Completed' : 'Pending'}
                          </span>
                        </div>
                        <button
                          className="dependency-item-remove"
                          onClick={() => handleRemoveDependency(dep.id)}
                          title={`Remove dependency on ${dep.dependsOnTaskTitle}`}
                        >
                          <X size={14} />
                        </button>
                      </div>
                    ))}
                  </div>
                )}

                {/* Add Dependency Inline Form */}
                {isAddingDependency && (
                  <form onSubmit={handleAddDependency} className="add-dependency-box fade-in">
                    <label style={{ fontSize: '0.8rem', fontWeight: 700, color: 'var(--text-primary)', display: 'block', marginBottom: 6 }}>
                      Select prerequisite task:
                    </label>
                    <div style={{ display: 'flex', gap: 8 }}>
                      <select
                        className="form-input"
                        style={{ fontSize: '0.85rem', padding: '6px 10px' }}
                        value={selectedPrerequisiteId}
                        onChange={e => setSelectedPrerequisiteId(e.target.value)}
                        required
                      >
                        <option value="">-- Choose a prerequisite task --</option>
                        {tasks
                          .filter(t => t.id !== detailTask.id && !taskDependencies.some(d => String(d.dependsOnTaskId) === t.id))
                          .map(t => (
                            <option key={t.id} value={t.id}>
                              {t.title} ({t.status === 'COMPLETED' ? 'Completed' : t.priority + ' Priority'})
                            </option>
                          ))}
                      </select>
                      <button type="submit" className="btn btn-primary btn-sm" disabled={!selectedPrerequisiteId}>
                        Add
                      </button>
                      <button
                        type="button"
                        className="btn btn-soft btn-sm"
                        onClick={() => { setIsAddingDependency(false); setSelectedPrerequisiteId(''); setDependencyError(null); }}
                      >
                        Cancel
                      </button>
                    </div>
                  </form>
                )}
              </div>

              {/* Tasks waiting on this task (Dependents) Section */}
              {taskDependents.length > 0 && (
                <div className="dependency-section" style={{ background: 'var(--pastel-bg)' }}>
                  <div className="dependency-section-title">
                    <h4>Tasks waiting for this ({taskDependents.length})</h4>
                  </div>
                  <div className="dependency-list">
                    {taskDependents.map(dep => (
                      <div key={dep.id} className="dependency-item" style={{ background: '#fff' }}>
                        <div className="dependency-item-info">
                          <span style={{ color: 'var(--accent-purple)', fontWeight: 700 }}>→</span>
                          <span className="dependency-item-title">{dep.taskTitle}</span>
                        </div>
                        <StatusBadge status={dep.dependsOnTaskStatus || 'TODO'} />
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Connected Knowledge & Documents Section */}
              <div className="task-doc-section">
                <div className="task-doc-header">
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <BookOpen size={16} color="var(--accent-purple)" />
                    <h4>Connected Knowledge & Documents ({taskDocuments.length})</h4>
                  </div>
                  {!isAddingTaskDocument && (
                    <button
                      type="button"
                      className="btn btn-soft btn-xs"
                      onClick={() => setIsAddingTaskDocument(true)}
                    >
                      <Plus size={13} /> Attach Document
                    </button>
                  )}
                </div>

                {taskDocumentError && (
                  <div className="auth-error-banner" style={{ margin: '8px 0', fontSize: '0.8rem' }}>
                    <AlertCircle size={14} /> {taskDocumentError}
                  </div>
                )}

                {isLoadingTaskDocuments ? (
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '12px 0', color: 'var(--text-secondary)', fontSize: '0.85rem' }}>
                    <Loader2 size={16} className="spinning" /> Loading connected documents...
                  </div>
                ) : taskDocuments.length === 0 ? (
                  <p style={{ fontSize: '0.82rem', color: 'var(--text-secondary)', margin: '6px 0' }}>
                    No documents attached yet. Connect PDF, DOCX, or Notes to ground AI queries and access study materials directly with this task.
                  </p>
                ) : (
                  <div className="task-doc-list">
                    {taskDocuments.map(doc => (
                      <div key={doc.id} className="task-doc-card">
                        <div className="task-doc-info">
                          <div className="task-doc-icon">
                            <FileText size={18} color="var(--accent-purple)" />
                          </div>
                          <div className="task-doc-details">
                            <div className="task-doc-title-row">
                              <span className="task-doc-name" title={doc.fileName}>{doc.fileName}</span>
                              <span className="task-doc-badge">
                                {doc.status === 'READY' || doc.status === 'PROCESSED' ? '⚡ RAG Ready' : doc.status}
                              </span>
                            </div>
                            <div className="task-doc-meta">
                              <span>{formatFileSize(doc.fileSize)}</span>
                              <span>•</span>
                              <span>{doc.fileType}</span>
                            </div>
                          </div>
                        </div>

                        <div className="task-doc-actions">
                          <button
                            type="button"
                            className="btn btn-soft btn-xs"
                            title="Preview Extracted Content"
                            onClick={() => handleToggleDocPreview(doc.id)}
                          >
                            <Eye size={13} /> {previewDocId === doc.id ? 'Hide' : 'Preview'}
                          </button>
                          <a
                            href={getDocumentDownloadUrl(doc.id)}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="btn btn-soft btn-xs"
                            title="Download or View Original"
                          >
                            <Download size={13} />
                          </a>
                          <button
                            type="button"
                            className="btn btn-soft btn-xs text-danger"
                            title="Unlink Document from Task"
                            onClick={() => handleDetachTaskDocument(doc.id)}
                          >
                            <X size={13} />
                          </button>
                        </div>
                      </div>
                    ))}
                  </div>
                )}

                {/* Inline Document Preview Box */}
                {previewDocId && (
                  <div className="task-doc-preview-box fade-in">
                    <div className="task-doc-preview-header">
                      <span style={{ fontWeight: 600, fontSize: '0.82rem', color: 'var(--accent-purple)' }}>
                        Document Preview
                      </span>
                      <button
                        type="button"
                        className="close-btn"
                        style={{ padding: 2 }}
                        onClick={() => { setPreviewDocId(null); setPreviewDocContent(null); }}
                      >
                        <X size={14} />
                      </button>
                    </div>
                    {isLoadingDocPreview ? (
                      <div style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '16px 0', color: 'var(--text-secondary)', fontSize: '0.82rem' }}>
                        <Loader2 size={14} className="spinning" /> Loading document text...
                      </div>
                    ) : previewDocContent ? (
                      <div className="task-doc-preview-content">
                        {previewDocContent.extractedText ? (
                          <pre style={{ margin: 0, whiteSpace: 'pre-wrap', fontFamily: 'inherit', fontSize: '0.82rem', color: 'var(--text-primary)', lineHeight: 1.5 }}>
                            {previewDocContent.extractedText.slice(0, 3000)}
                            {previewDocContent.extractedText.length > 3000 && '... [Preview truncated for display]'}
                          </pre>
                        ) : (
                          <p style={{ margin: 0, fontSize: '0.82rem', color: 'var(--text-secondary)' }}>
                            No extracted text available for this document.
                          </p>
                        )}
                      </div>
                    ) : null}
                  </div>
                )}

                {/* Attach Document Form */}
                {isAddingTaskDocument && (
                  <div className="add-dependency-box fade-in" style={{ marginTop: 10 }}>
                    <label style={{ fontSize: '0.8rem', fontWeight: 700, color: 'var(--text-primary)', display: 'block', marginBottom: 6 }}>
                      Select Document from Knowledge Base:
                    </label>
                    <div style={{ display: 'flex', gap: 8 }}>
                      <select
                        className="form-input"
                        style={{ fontSize: '0.85rem', padding: '6px 10px' }}
                        value={selectedDocToAttachId}
                        onChange={e => setSelectedDocToAttachId(e.target.value)}
                      >
                        <option value="">-- Choose document to connect --</option>
                        {allUserDocuments
                          .filter(d => !taskDocuments.some(td => String(td.id) === String(d.id)))
                          .map(d => (
                            <option key={d.id} value={d.id}>
                              {d.fileName} ({formatFileSize(d.fileSize)})
                            </option>
                          ))}
                      </select>
                      <button
                        type="button"
                        className="btn btn-primary btn-sm"
                        disabled={!selectedDocToAttachId}
                        onClick={handleAttachTaskDocument}
                      >
                        Attach
                      </button>
                      <button
                        type="button"
                        className="btn btn-soft btn-sm"
                        onClick={() => { setIsAddingTaskDocument(false); setSelectedDocToAttachId(''); setTaskDocumentError(null); }}
                      >
                        Cancel
                      </button>
                    </div>
                  </div>
                )}
              </div>

              {/* Grounded AI Task Assistant (RAG) Section */}
              <div className="task-ai-section">
                <div className="task-ai-header">
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <Sparkles size={16} color="var(--accent-purple)" />
                    <h4>Grounded AI Assistant (Task RAG)</h4>
                  </div>
                  <span className="task-doc-badge" style={{ background: '#ede9fe', color: 'var(--accent-purple)' }}>
                    Vector Knowledge Base
                  </span>
                </div>
                <p style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', margin: '4px 0 10px 0' }}>
                  Ask questions grounded in your attached documents and knowledge base to solve or prepare for this task.
                </p>

                <div className="task-ai-input-group">
                  <input
                    type="text"
                    className="form-input"
                    style={{ fontSize: '0.85rem', padding: '8px 12px' }}
                    placeholder={`Ask anything about "${detailTask.title}"...`}
                    value={taskAiQuestion}
                    onChange={e => setTaskAiQuestion(e.target.value)}
                    onKeyDown={e => {
                      if (e.key === 'Enter') {
                        e.preventDefault();
                        handleAskTaskAi();
                      }
                    }}
                  />
                  <button
                    type="button"
                    className="btn btn-primary btn-sm"
                    disabled={isAskingTaskAi || !taskAiQuestion.trim()}
                    onClick={() => handleAskTaskAi()}
                  >
                    {isAskingTaskAi ? (
                      <>
                        <Loader2 size={14} className="spinning" /> Asking...
                      </>
                    ) : (
                      <>
                        <Sparkles size={14} /> Ask AI
                      </>
                    )}
                  </button>
                </div>

                {taskAiError && (
                  <div className="auth-error-banner" style={{ margin: '8px 0', fontSize: '0.8rem' }}>
                    <AlertCircle size={14} /> {taskAiError}
                  </div>
                )}

                {taskAiAnswer && (
                  <div className="task-ai-answer-card fade-in">
                    <div className="task-ai-answer-header">
                      <span style={{ fontWeight: 700, fontSize: '0.82rem', color: 'var(--text-primary)' }}>
                        AI Response
                      </span>
                      <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>
                        Model: {taskAiAnswer.modelName || 'Mistral-7B'}
                      </span>
                    </div>
                    <div className="task-ai-answer-body">
                      {taskAiAnswer.answer}
                    </div>

                    {taskAiAnswer.sources && taskAiAnswer.sources.length > 0 && (
                      <div className="task-ai-sources">
                        <span style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)' }}>
                          Sources Cited:
                        </span>
                        <div className="task-ai-source-badges">
                          {taskAiAnswer.sources.map((src, i) => (
                            <div key={i} className="task-ai-source-badge" title={`Chunk #${src.chunkIndex}`}>
                              <FileText size={11} />
                              <span>{src.documentName}</span>
                              <span style={{ opacity: 0.7 }}>({Math.round((src.similarity || 0) * 100)}% match)</span>
                            </div>
                          ))}
                        </div>
                      </div>
                    )}
                  </div>
                )}
              </div>
            </div>

            <div className="modal-footer">
              <button className="btn btn-soft" onClick={() => toggleCompleteTask(detailTask.id)}>
                {detailTask.status === 'COMPLETED' ? 'Mark Incomplete' : 'Mark Complete'}
              </button>
              <button className="btn btn-soft" onClick={() => openEditModal(detailTask)}>
                <Edit3 size={15} /> Edit
              </button>
              <button className="btn btn-danger" onClick={() => confirmDeleteTask(detailTask.id)}>
                <Trash2 size={15} /> Delete
              </button>
            </div>
          </div>
        </div>
      )}

      {/* DELETE CONFIRMATION MODAL */}
      {deletingTaskId && (
        <div className="modal-overlay" onClick={() => !isSubmitting && setDeletingTaskId(null)}>
          <div className="modal-card" style={{ maxWidth: 420 }} onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h2>Confirm Deletion</h2>
              <button className="close-btn" onClick={() => !isSubmitting && setDeletingTaskId(null)} disabled={isSubmitting}><X size={18} /></button>
            </div>
            <div className="modal-body">
              <div className="confirm-box">
                <AlertCircle size={40} color="var(--pastel-pink-text)" style={{ marginBottom: 12 }} />
                <h3>Delete Task?</h3>
                <p>Are you sure you want to delete this task? This action will remove it permanently from your PostgreSQL database.</p>
              </div>
            </div>
            <div className="modal-footer">
              <button className="btn btn-soft" onClick={() => setDeletingTaskId(null)} disabled={isSubmitting}>Cancel</button>
              <button className="btn btn-danger" onClick={executeDeleteTask} disabled={isSubmitting}>
                {isSubmitting ? 'Deleting...' : 'Delete Task'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* CREATE / EDIT CATEGORY MODAL */}
      {isCategoryModalOpen && (
        <div className="modal-overlay" onClick={() => !isSubmittingCategory && setIsCategoryModalOpen(false)}>
          <div className="modal-card" style={{ maxWidth: 480 }} onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h2>{editingCategory ? 'Edit Category' : 'Create Category'}</h2>
              <button className="close-btn" onClick={() => !isSubmittingCategory && setIsCategoryModalOpen(false)} disabled={isSubmittingCategory}>
                <X size={18} />
              </button>
            </div>

            <form onSubmit={handleSaveCategory}>
              <div className="modal-body">
                {categoryFormError && (
                  <div className="form-error fade-in" style={{ marginBottom: 16 }}>
                    {categoryFormError}
                  </div>
                )}

                <div className="form-group">
                  <label>Category Name *</label>
                  <input
                    type="text"
                    className="form-input"
                    placeholder="e.g., Project, Study, Placement"
                    value={categoryFormName}
                    onChange={e => {
                      setCategoryFormName(e.target.value);
                      if (categoryFormError) setCategoryFormError(null);
                    }}
                    disabled={isSubmittingCategory}
                    autoFocus
                  />
                </div>

                <div className="form-group" style={{ marginBottom: 0 }}>
                  <label>Description (Optional)</label>
                  <textarea
                    className="form-input"
                    rows={3}
                    placeholder="Brief description of what this category is for..."
                    value={categoryFormDesc}
                    onChange={e => setCategoryFormDesc(e.target.value)}
                    style={{ resize: 'vertical' }}
                    disabled={isSubmittingCategory}
                  />
                </div>
              </div>

              <div className="modal-footer">
                <button type="button" className="btn btn-soft" onClick={() => setIsCategoryModalOpen(false)} disabled={isSubmittingCategory}>
                  Cancel
                </button>
                <button type="submit" className="btn btn-primary" disabled={isSubmittingCategory}>
                  {isSubmittingCategory ? 'Saving...' : editingCategory ? 'Save Changes' : 'Create Category'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* DELETE CATEGORY CONFIRMATION MODAL */}
      {deletingCategory && (
        <div className="modal-overlay" onClick={() => !isSubmittingCategory && setDeletingCategory(null)}>
          <div className="modal-card" style={{ maxWidth: 440 }} onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h2>Delete Category</h2>
              <button className="close-btn" onClick={() => !isSubmittingCategory && setDeletingCategory(null)} disabled={isSubmittingCategory}>
                <X size={18} />
              </button>
            </div>

            <div className="modal-body">
              {categoryDeleteError ? (
                <div className="empty-state fade-in" style={{ padding: '24px 16px', borderColor: 'var(--pastel-pink-border)' }}>
                  <AlertCircle size={36} color="var(--pastel-pink-text)" style={{ marginBottom: 10 }} />
                  <h3 style={{ fontSize: '1.05rem', color: 'var(--pastel-pink-text)', marginBottom: 8 }}>Cannot Delete Category</h3>
                  <p style={{ fontSize: '0.88rem', color: 'var(--text-secondary)', marginBottom: 0 }}>
                    {categoryDeleteError}
                  </p>
                </div>
              ) : (
                <div className="confirm-box">
                  <AlertCircle size={40} color="var(--pastel-pink-text)" style={{ marginBottom: 12 }} />
                  <h3>Delete Category?</h3>
                  <p>Are you sure you want to delete the <strong>{deletingCategory.name}</strong> category?</p>
                </div>
              )}
            </div>

            <div className="modal-footer">
              <button className="btn btn-soft" onClick={() => setDeletingCategory(null)} disabled={isSubmittingCategory}>
                {categoryDeleteError ? 'Close' : 'Cancel'}
              </button>
              {!categoryDeleteError && (
                <button className="btn btn-danger" onClick={executeDeleteCategory} disabled={isSubmittingCategory}>
                  {isSubmittingCategory ? 'Deleting...' : 'Delete Category'}
                </button>
              )}
            </div>
          </div>
        </div>
      )}
      {/* AI TASK ASSISTANT MODAL */}
      <AiTaskAssistantModal 
        isOpen={isAiModalOpen} 
        onClose={() => setIsAiModalOpen(false)} 
        onTasksGenerated={handleTasksGenerated} 
        initialDate={aiModalInitialDate}
        initialPrompt={aiModalInitialPrompt}
      />

    </div>
  );
}

