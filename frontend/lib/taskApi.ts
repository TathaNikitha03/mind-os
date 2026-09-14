import { getCurrentUser } from '@/lib/auth';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8081';

export interface TaskDto {
  id: number | string;
  userId?: number;
  title: string;
  description: string;
  priority: 'HIGH' | 'MEDIUM' | 'LOW';
  status: 'TODO' | 'IN_PROGRESS' | 'COMPLETED';
  dueDate: string;
  dueTime?: string;
  estimatedMinutes?: number;
  category?: string;
  tags?: string[];
  dependencyCount?: number;
  uncompletedDependencyCount?: number;
  blocked?: boolean;
  createdAt?: string;
  updatedAt?: string;
  completedAt?: string;
}

export interface TaskInput {
  title: string;
  description?: string;
  priority: 'HIGH' | 'MEDIUM' | 'LOW';
  status?: 'TODO' | 'IN_PROGRESS' | 'COMPLETED';
  dueDate?: string;
  dueTime?: string;
  estimatedMinutes?: number;
  category?: string;
  tags?: string[];
}

export interface TaskStatisticsDto {
  totalTasks: number;
  completedTasks: number;
  inProgressTasks: number;
  todoTasks: number;
  overdueTasks: number;
  todayTasks: number;
  blockedTasks: number;
  completionPercentage: number;
  priorityBreakdown: {
    HIGH?: number;
    MEDIUM?: number;
    LOW?: number;
    high?: number;
    medium?: number;
    low?: number;
    [key: string]: number | undefined;
  };
  categoryBreakdown: Record<string, number>;
  todayTasksList?: TaskDto[];
  upcomingTasksList?: TaskDto[];
  overdueTasksList?: TaskDto[];
  blockedTasksList?: TaskDto[];
}

function getAuthHeaders(): Record<string, string> {
  const user = getCurrentUser();
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
  };

  if (user) {
    headers['Authorization'] = `Bearer ${user.mobile}`;
    headers['X-User-Mobile'] = user.mobile;
    headers['X-User-Name'] = user.name;
  }

  return headers;
}

// 0. GET /api/tasks/statistics - Get task statistics & dashboard intelligence
export async function getTaskStatisticsApi(): Promise<TaskStatisticsDto> {
  const res = await fetch(`${API_BASE_URL}/api/tasks/statistics`, {
    method: 'GET',
    headers: getAuthHeaders(),
  });

  if (!res.ok) {
    const errorData = await res.json().catch(() => ({}));
    throw new Error(errorData.error || `Failed to load task statistics: HTTP ${res.status}`);
  }

  return await res.json();
}

// 1. GET /api/tasks - List tasks for authenticated user
export async function getTasksApi(): Promise<TaskDto[]> {
  const res = await fetch(`${API_BASE_URL}/api/tasks`, {
    method: 'GET',
    headers: getAuthHeaders(),
  });

  if (!res.ok) {
    throw new Error(`Failed to load tasks: HTTP ${res.status}`);
  }

  return await res.json();
}

// 2. GET /api/tasks/{id} - Get single task
export async function getTaskByIdApi(id: string | number): Promise<TaskDto> {
  const res = await fetch(`${API_BASE_URL}/api/tasks/${id}`, {
    method: 'GET',
    headers: getAuthHeaders(),
  });

  if (!res.ok) {
    throw new Error(`Failed to fetch task details (HTTP ${res.status})`);
  }

  return await res.json();
}

// 3. POST /api/tasks - Create task
export async function createTaskApi(input: TaskInput): Promise<TaskDto> {
  const payload = {
    ...input,
    dueDate: input.dueDate ? `${input.dueDate}${input.dueTime ? 'T' + input.dueTime : 'T18:00'}` : undefined,
  };

  const res = await fetch(`${API_BASE_URL}/api/tasks`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify(payload),
  });

  if (!res.ok) {
    const errorData = await res.json().catch(() => ({}));
    throw new Error(errorData.error || `Failed to create task (HTTP ${res.status})`);
  }

  return await res.json();
}

// 4. PUT /api/tasks/{id} - Update task
export async function updateTaskApi(id: string | number, input: TaskInput): Promise<TaskDto> {
  const payload = {
    ...input,
    dueDate: input.dueDate ? `${input.dueDate}${input.dueTime ? 'T' + input.dueTime : 'T18:00'}` : undefined,
  };

  const res = await fetch(`${API_BASE_URL}/api/tasks/${id}`, {
    method: 'PUT',
    headers: getAuthHeaders(),
    body: JSON.stringify(payload),
  });

  if (!res.ok) {
    const errorData = await res.json().catch(() => ({}));
    throw new Error(errorData.error || `Failed to update task (HTTP ${res.status})`);
  }

  return await res.json();
}

// 5. PATCH /api/tasks/{id}/complete - Mark complete
export async function completeTaskApi(id: string | number): Promise<TaskDto> {
  const res = await fetch(`${API_BASE_URL}/api/tasks/${id}/complete`, {
    method: 'PATCH',
    headers: getAuthHeaders(),
  });

  if (!res.ok) {
    const errorData = await res.json().catch(() => ({}));
    throw new Error(errorData.error || `Failed to complete task (HTTP ${res.status})`);
  }

  return await res.json();
}

// 6. DELETE /api/tasks/{id} - Delete task
export async function deleteTaskApi(id: string | number): Promise<void> {
  const res = await fetch(`${API_BASE_URL}/api/tasks/${id}`, {
    method: 'DELETE',
    headers: getAuthHeaders(),
  });

  if (!res.ok) {
    const errorData = await res.json().catch(() => ({}));
    throw new Error(errorData.error || `Failed to delete task (HTTP ${res.status})`);
  }
}
// Dummy change 2 for GitHub Desktop test!
