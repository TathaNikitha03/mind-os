import { getCurrentUser } from '@/lib/auth';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8081';

export interface CategoryDto {
  id: number | string;
  userId?: number;
  name: string;
  description?: string;
  createdAt?: string;
  taskCount?: number;
}

export interface CategoryInput {
  name: string;
  description?: string;
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

// 1. GET /api/categories - List categories for authenticated user
export async function getCategoriesApi(): Promise<CategoryDto[]> {
  const res = await fetch(`${API_BASE_URL}/api/categories`, {
    method: 'GET',
    headers: getAuthHeaders(),
  });

  if (!res.ok) {
    const errorData = await res.json().catch(() => ({}));
    throw new Error(errorData.error || `Failed to load categories: HTTP ${res.status}`);
  }

  return await res.json();
}

// 2. GET /api/categories/{id} - Get single category
export async function getCategoryByIdApi(id: string | number): Promise<CategoryDto> {
  const res = await fetch(`${API_BASE_URL}/api/categories/${id}`, {
    method: 'GET',
    headers: getAuthHeaders(),
  });

  if (!res.ok) {
    const errorData = await res.json().catch(() => ({}));
    throw new Error(errorData.error || `Failed to fetch category details (HTTP ${res.status})`);
  }

  return await res.json();
}

// 3. POST /api/categories - Create category
export async function createCategoryApi(input: CategoryInput): Promise<CategoryDto> {
  const res = await fetch(`${API_BASE_URL}/api/categories`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify(input),
  });

  if (!res.ok) {
    const errorData = await res.json().catch(() => ({}));
    throw new Error(errorData.error || `Failed to create category (HTTP ${res.status})`);
  }

  return await res.json();
}

// 4. PUT /api/categories/{id} - Update category
export async function updateCategoryApi(id: string | number, input: CategoryInput): Promise<CategoryDto> {
  const res = await fetch(`${API_BASE_URL}/api/categories/${id}`, {
    method: 'PUT',
    headers: getAuthHeaders(),
    body: JSON.stringify(input),
  });

  if (!res.ok) {
    const errorData = await res.json().catch(() => ({}));
    throw new Error(errorData.error || `Failed to update category (HTTP ${res.status})`);
  }

  return await res.json();
}

// 5. DELETE /api/categories/{id} - Delete category
export async function deleteCategoryApi(id: string | number): Promise<void> {
  const res = await fetch(`${API_BASE_URL}/api/categories/${id}`, {
    method: 'DELETE',
    headers: getAuthHeaders(),
  });

  if (!res.ok) {
    const errorData = await res.json().catch(() => ({}));
    throw new Error(errorData.error || `Failed to delete category (HTTP ${res.status})`);
  }
}
