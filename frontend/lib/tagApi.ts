import { getCurrentUser } from '@/lib/auth';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8081';

export interface TagDto {
  id: number | string;
  userId?: number;
  name: string;
  createdAt?: string;
  taskCount?: number;
}

export interface TagInput {
  name: string;
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

// 1. GET /api/tags - List tags for authenticated user
export async function getTagsApi(): Promise<TagDto[]> {
  const res = await fetch(`${API_BASE_URL}/api/tags`, {
    method: 'GET',
    headers: getAuthHeaders(),
  });

  if (!res.ok) {
    const errorData = await res.json().catch(() => ({}));
    throw new Error(errorData.error || `Failed to load tags: HTTP ${res.status}`);
  }

  return await res.json();
}

// 2. GET /api/tags/{id} - Get single tag
export async function getTagByIdApi(id: string | number): Promise<TagDto> {
  const res = await fetch(`${API_BASE_URL}/api/tags/${id}`, {
    method: 'GET',
    headers: getAuthHeaders(),
  });

  if (!res.ok) {
    const errorData = await res.json().catch(() => ({}));
    throw new Error(errorData.error || `Failed to load tag (HTTP ${res.status})`);
  }

  return await res.json();
}

// 3. POST /api/tags - Create a new tag
export async function createTagApi(input: TagInput): Promise<TagDto> {
  const res = await fetch(`${API_BASE_URL}/api/tags`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify(input),
  });

  if (!res.ok) {
    const errorData = await res.json().catch(() => ({}));
    throw new Error(errorData.error || `Failed to create tag (HTTP ${res.status})`);
  }

  return await res.json();
}

// 4. PUT /api/tags/{id} - Update a tag
export async function updateTagApi(id: string | number, input: TagInput): Promise<TagDto> {
  const res = await fetch(`${API_BASE_URL}/api/tags/${id}`, {
    method: 'PUT',
    headers: getAuthHeaders(),
    body: JSON.stringify(input),
  });

  if (!res.ok) {
    const errorData = await res.json().catch(() => ({}));
    throw new Error(errorData.error || `Failed to update tag (HTTP ${res.status})`);
  }

  return await res.json();
}

// 5. DELETE /api/tags/{id} - Delete tag
export async function deleteTagApi(id: string | number): Promise<void> {
  const res = await fetch(`${API_BASE_URL}/api/tags/${id}`, {
    method: 'DELETE',
    headers: getAuthHeaders(),
  });

  if (!res.ok) {
    const errorData = await res.json().catch(() => ({}));
    throw new Error(errorData.error || `Failed to delete tag (HTTP ${res.status})`);
  }
}

// 6. POST /api/tasks/{taskId}/tags/{tagId} - Add tag to task
export async function addTagToTaskApi(taskId: string | number, tagId: string | number): Promise<any> {
  const res = await fetch(`${API_BASE_URL}/api/tasks/${taskId}/tags/${tagId}`, {
    method: 'POST',
    headers: getAuthHeaders(),
  });

  if (!res.ok) {
    const errorData = await res.json().catch(() => ({}));
    throw new Error(errorData.error || `Failed to add tag to task (HTTP ${res.status})`);
  }

  return await res.json();
}

// 7. DELETE /api/tasks/{taskId}/tags/{tagId} - Remove tag from task
export async function removeTagFromTaskApi(taskId: string | number, tagId: string | number): Promise<any> {
  const res = await fetch(`${API_BASE_URL}/api/tasks/${taskId}/tags/${tagId}`, {
    method: 'DELETE',
    headers: getAuthHeaders(),
  });

  if (!res.ok) {
    const errorData = await res.json().catch(() => ({}));
    throw new Error(errorData.error || `Failed to remove tag from task (HTTP ${res.status})`);
  }

  return await res.json();
}
