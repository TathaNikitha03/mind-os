import { getCurrentUser } from '@/lib/auth';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8081';

export interface TaskDependencyDto {
  id: number | string;
  taskId: number | string;
  taskTitle: string;
  dependsOnTaskId: number | string;
  dependsOnTaskTitle: string;
  dependsOnTaskPriority?: 'HIGH' | 'MEDIUM' | 'LOW';
  dependsOnTaskStatus?: 'TODO' | 'IN_PROGRESS' | 'COMPLETED';
  dependsOnTaskCategory?: string;
  isCompleted: boolean;
  createdAt?: string;
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

// 1. GET /api/tasks/{taskId}/dependencies - Get prerequisites of a task
export async function getTaskDependenciesApi(taskId: string | number): Promise<TaskDependencyDto[]> {
  const res = await fetch(`${API_BASE_URL}/api/tasks/${taskId}/dependencies`, {
    method: 'GET',
    headers: getAuthHeaders(),
  });

  if (!res.ok) {
    const errorData = await res.json().catch(() => ({}));
    throw new Error(errorData.error || `Failed to load dependencies (HTTP ${res.status})`);
  }

  return await res.json();
}

// 2. GET /api/tasks/{taskId}/dependencies/dependents - Get tasks waiting on this task
export async function getTaskDependentsApi(taskId: string | number): Promise<TaskDependencyDto[]> {
  const res = await fetch(`${API_BASE_URL}/api/tasks/${taskId}/dependencies/dependents`, {
    method: 'GET',
    headers: getAuthHeaders(),
  });

  if (!res.ok) {
    const errorData = await res.json().catch(() => ({}));
    throw new Error(errorData.error || `Failed to load dependent tasks (HTTP ${res.status})`);
  }

  return await res.json();
}

// 3. POST /api/tasks/{taskId}/dependencies - Add a prerequisite dependency
export async function addTaskDependencyApi(taskId: string | number, dependsOnTaskId: string | number): Promise<TaskDependencyDto> {
  const res = await fetch(`${API_BASE_URL}/api/tasks/${taskId}/dependencies`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify({ dependsOnTaskId: Number(dependsOnTaskId) }),
  });

  if (!res.ok) {
    const errorData = await res.json().catch(() => ({}));
    throw new Error(errorData.error || `Failed to add dependency (HTTP ${res.status})`);
  }

  return await res.json();
}

// 4. DELETE /api/tasks/{taskId}/dependencies/{dependencyId} - Remove a dependency
export async function removeTaskDependencyApi(taskId: string | number, dependencyId: string | number): Promise<void> {
  const res = await fetch(`${API_BASE_URL}/api/tasks/${taskId}/dependencies/${dependencyId}`, {
    method: 'DELETE',
    headers: getAuthHeaders(),
  });

  if (!res.ok) {
    const errorData = await res.json().catch(() => ({}));
    throw new Error(errorData.error || `Failed to remove dependency (HTTP ${res.status})`);
  }
}
