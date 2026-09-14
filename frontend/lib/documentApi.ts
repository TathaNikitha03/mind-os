import { getCurrentUser } from '@/lib/auth';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8081';

export interface DocumentDto {
  id: number | string;
  userId?: number;
  categoryId?: number;
  categoryName?: string;
  fileName: string;
  title: string;
  fileType: 'PDF' | 'DOCX' | 'TXT' | string;
  fileSize: number;
  description?: string;
  storageUrl?: string;
  status: 'READY' | 'PROCESSING' | 'PROCESSED' | 'FAILED' | string;
  uploadedAt?: string;
  updatedAt?: string;
}

export interface DocumentContentDto {
  id?: number | string;
  documentId: number | string;
  fileName: string;
  title: string;
  fileType: string;
  extractedText: string;
  charCount: number;
  wordCount: number;
  chunkCount?: number;
  embeddingCount?: number;
  hasText: boolean;
  status: string;
  extractedAt?: string;
}

export interface DocumentChunkDto {
  id: number | string;
  documentId: number | string;
  chunkIndex: number;
  content: string;
  charCount?: number;
  wordCount?: number;
  createdAt?: string;
}

export interface DocumentInput {
  fileName: string;
  title?: string;
  fileType?: string;
  fileSize?: number;
  description?: string;
  storageUrl?: string;
  status?: string;
  categoryId?: number;
}

function getAuthHeaders(includeContentType = true): Record<string, string> {
  const user = getCurrentUser();
  const headers: Record<string, string> = {};

  if (includeContentType) {
    headers['Content-Type'] = 'application/json';
  }

  if (user) {
    headers['Authorization'] = `Bearer ${user.mobile}`;
    headers['X-User-Mobile'] = user.mobile;
    headers['X-User-Name'] = user.name;
  }

  return headers;
}

// 1. GET /api/documents - List documents for authenticated user
export async function getDocumentsApi(): Promise<DocumentDto[]> {
  const res = await fetch(`${API_BASE_URL}/api/documents`, {
    method: 'GET',
    headers: getAuthHeaders(true),
  });

  if (!res.ok) {
    const errorData = await res.json().catch(() => ({}));
    throw new Error(errorData.error || `Unable to load documents (HTTP ${res.status})`);
  }

  return await res.json();
}

// 2. GET /api/documents/{id} - Get single document details
export async function getDocumentByIdApi(id: string | number): Promise<DocumentDto> {
  const res = await fetch(`${API_BASE_URL}/api/documents/${id}`, {
    method: 'GET',
    headers: getAuthHeaders(true),
  });

  if (!res.ok) {
    const errorData = await res.json().catch(() => ({}));
    throw new Error(errorData.error || `Document not found (HTTP ${res.status})`);
  }

  return await res.json();
}

// 3. GET /api/documents/{id}/content - Get extracted plain text content preview (STEP 4.4)
export async function getDocumentContentApi(id: string | number): Promise<DocumentContentDto> {
  const res = await fetch(`${API_BASE_URL}/api/documents/${id}/content`, {
    method: 'GET',
    headers: getAuthHeaders(true),
  });

  if (!res.ok) {
    const errorData = await res.json().catch(() => ({}));
    throw new Error(errorData.error || `Unable to load document content (HTTP ${res.status})`);
  }

  return await res.json();
}

// 4. GET /api/documents/{id}/chunks - Get sequential overlapping chunks (STEP 4.5)
export async function getDocumentChunksApi(id: string | number): Promise<DocumentChunkDto[]> {
  const res = await fetch(`${API_BASE_URL}/api/documents/${id}/chunks`, {
    method: 'GET',
    headers: getAuthHeaders(true),
  });

  if (!res.ok) {
    const errorData = await res.json().catch(() => ({}));
    throw new Error(errorData.error || `Unable to load document chunks (HTTP ${res.status})`);
  }

  return await res.json();
}

// 5. POST /api/documents/{id}/extract - Trigger on-demand re-extraction & chunking (STEP 4.4 & 4.5)
export async function extractDocumentTextApi(id: string | number): Promise<DocumentContentDto> {
  const res = await fetch(`${API_BASE_URL}/api/documents/${id}/extract`, {
    method: 'POST',
    headers: getAuthHeaders(true),
  });

  if (!res.ok) {
    const errorData = await res.json().catch(() => ({}));
    throw new Error(errorData.error || `Failed to extract document text (HTTP ${res.status})`);
  }

  return await res.json();
}

// 6. POST /api/documents/upload - Multipart Actual File Upload + Automatic Extraction + Chunking
export async function uploadDocumentFileApi(
  file: File,
  title?: string,
  description?: string,
  categoryId?: number
): Promise<DocumentDto> {
  const formData = new FormData();
  formData.append('file', file);
  if (title && title.trim()) {
    formData.append('title', title.trim());
  }
  if (description && description.trim()) {
    formData.append('description', description.trim());
  }
  if (categoryId !== undefined && categoryId !== null) {
    formData.append('categoryId', String(categoryId));
  }

  const res = await fetch(`${API_BASE_URL}/api/documents/upload`, {
    method: 'POST',
    headers: getAuthHeaders(false),
    body: formData,
  });

  if (!res.ok) {
    const errorData = await res.json().catch(() => ({}));
    throw new Error(errorData.error || `Failed to upload document file (HTTP ${res.status})`);
  }

  return await res.json();
}

// 7. GET Download URL helper
export function getDocumentDownloadUrl(id: string | number): string {
  return `${API_BASE_URL}/api/documents/${id}/download`;
}

// 8. POST /api/documents - Create document metadata (JSON fallback)
export async function createDocumentApi(input: DocumentInput): Promise<DocumentDto> {
  const res = await fetch(`${API_BASE_URL}/api/documents`, {
    method: 'POST',
    headers: getAuthHeaders(true),
    body: JSON.stringify(input),
  });

  if (!res.ok) {
    const errorData = await res.json().catch(() => ({}));
    throw new Error(errorData.error || `Unable to add document (HTTP ${res.status})`);
  }

  return await res.json();
}

// 9. PUT /api/documents/{id} - Update document metadata
export async function updateDocumentApi(id: string | number, input: Partial<DocumentInput>): Promise<DocumentDto> {
  const res = await fetch(`${API_BASE_URL}/api/documents/${id}`, {
    method: 'PUT',
    headers: getAuthHeaders(true),
    body: JSON.stringify(input),
  });

  if (!res.ok) {
    const errorData = await res.json().catch(() => ({}));
    throw new Error(errorData.error || `Failed to update document (HTTP ${res.status})`);
  }

  return await res.json();
}

// 10. DELETE /api/documents/{id} - Delete document metadata and storage file
export async function deleteDocumentApi(id: string | number): Promise<void> {
  const res = await fetch(`${API_BASE_URL}/api/documents/${id}`, {
    method: 'DELETE',
    headers: getAuthHeaders(true),
  });

  if (!res.ok) {
    const errorData = await res.json().catch(() => ({}));
    throw new Error(errorData.error || `Unable to delete document (HTTP ${res.status})`);
  }
}
