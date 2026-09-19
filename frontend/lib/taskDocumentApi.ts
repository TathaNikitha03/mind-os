import { getCurrentUser } from '@/lib/auth';
import type { DocumentDto } from '@/lib/documentApi';
import type { TaskDto } from '@/lib/taskApi';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8081';

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

/**
 * GET /api/tasks/{taskId}/documents
 * Fetch all documents attached to a specific task.
 */
export async function getTaskDocumentsApi(taskId: string | number): Promise<DocumentDto[]> {
  const res = await fetch(`${API_BASE_URL}/api/tasks/${taskId}/documents`, {
    method: 'GET',
    headers: getAuthHeaders(),
  });

  if (!res.ok) {
    const errorData = await res.json().catch(() => ({}));
    throw new Error(errorData.error || `Failed to load task documents (HTTP ${res.status})`);
  }

  return await res.json();
}

/**
 * POST /api/tasks/{taskId}/documents/{documentId}
 * Attach a document to a task.
 */
export async function attachDocumentToTaskApi(
  taskId: string | number,
  documentId: string | number
): Promise<DocumentDto> {
  const res = await fetch(`${API_BASE_URL}/api/tasks/${taskId}/documents/${documentId}`, {
    method: 'POST',
    headers: getAuthHeaders(),
  });

  if (!res.ok) {
    const errorData = await res.json().catch(() => ({}));
    throw new Error(errorData.error || `Failed to attach document to task (HTTP ${res.status})`);
  }

  return await res.json();
}

/**
 * DELETE /api/tasks/{taskId}/documents/{documentId}
 * Unlink a document from a task.
 */
export async function detachDocumentFromTaskApi(
  taskId: string | number,
  documentId: string | number
): Promise<void> {
  const res = await fetch(`${API_BASE_URL}/api/tasks/${taskId}/documents/${documentId}`, {
    method: 'DELETE',
    headers: getAuthHeaders(),
  });

  if (!res.ok) {
    const errorData = await res.json().catch(() => ({}));
    throw new Error(errorData.error || `Failed to detach document from task (HTTP ${res.status})`);
  }
}

/**
 * GET /api/documents/{documentId}/tasks
 * Fetch all tasks linked to a specific document.
 */
export async function getTasksForDocumentApi(documentId: string | number): Promise<TaskDto[]> {
  const res = await fetch(`${API_BASE_URL}/api/documents/${documentId}/tasks`, {
    method: 'GET',
    headers: getAuthHeaders(),
  });

  if (!res.ok) {
    const errorData = await res.json().catch(() => ({}));
    throw new Error(errorData.error || `Failed to load document tasks (HTTP ${res.status})`);
  }

  return await res.json();
}
