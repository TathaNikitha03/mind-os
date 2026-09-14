import { getCurrentUser } from '@/lib/auth';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8081';

export interface SemanticSearchResultDto {
  chunkId: number;
  documentId: number;
  documentName: string;
  chunkIndex: number;
  content: string;
  similarity: number;
}

export interface SemanticSearchResponseDto {
  query: string;
  results: SemanticSearchResultDto[];
  totalResults: number;
  modelName: string;
}

export interface SemanticSearchRequestDto {
  query: string;
  topK?: number;
  threshold?: number;
}

export interface RagSourceDto {
  documentId: number;
  documentName: string;
  chunkId: number;
  chunkIndex: number;
  similarity: number;
}

export interface RagAnswerResponseDto {
  question: string;
  answer: string;
  sources: RagSourceDto[];
  hasContext: boolean;
  modelName: string;
}

export interface RagQuestionRequestDto {
  question: string;
  topK?: number;
  threshold?: number;
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

/**
 * POST /api/knowledge/search
 * Search the user's personal knowledge base by meaning/semantic vectors (Step 4.7).
 */
export async function searchKnowledgeApi(
  query: string,
  topK: number = 5,
  threshold?: number
): Promise<SemanticSearchResponseDto> {
  if (!query || !query.trim()) {
    throw new Error('Search query is required.');
  }

  const payload: SemanticSearchRequestDto = {
    query: query.trim(),
    topK,
  };

  if (threshold !== undefined && threshold !== null) {
    payload.threshold = threshold;
  }

  const res = await fetch(`${API_BASE_URL}/api/knowledge/search`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify(payload),
  });

  if (!res.ok) {
    const errorData = await res.json().catch(() => ({}));
    throw new Error(errorData.error || `Knowledge search failed (HTTP ${res.status})`);
  }

  return await res.json();
}

/**
 * POST /api/knowledge/ask
 * Ask question to personal knowledge base using RAG grounded retrieval (Step 4.8).
 */
export async function askKnowledgeApi(
  question: string,
  topK: number = 5,
  threshold?: number
): Promise<RagAnswerResponseDto> {
  if (!question || !question.trim()) {
    throw new Error('Question cannot be blank.');
  }

  const payload: RagQuestionRequestDto = {
    question: question.trim(),
    topK,
  };

  if (threshold !== undefined && threshold !== null) {
    payload.threshold = threshold;
  }

  const res = await fetch(`${API_BASE_URL}/api/knowledge/ask`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify(payload),
  });

  if (!res.ok) {
    const errorData = await res.json().catch(() => ({}));
    throw new Error(errorData.error || `RAG service failed (HTTP ${res.status})`);
  }

  return await res.json();
}
