'use client';

import React, { useState, useEffect, useRef } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import {
  Bot, Sparkles, Send, FileText, Loader2, Copy, Check,
  AlertCircle, HelpCircle, RefreshCw, LayoutDashboard,
  CheckSquare, Folder, Calendar, User, LogOut, ArrowRight,
  ExternalLink, CornerDownLeft
} from 'lucide-react';
import { getCurrentUser, logout, type User as AuthUser } from '@/lib/auth';
import { askKnowledgeApi, type RagAnswerResponseDto, type RagSourceDto } from '@/lib/knowledgeApi';

const SUGGESTED_QUESTIONS = [
  'What is normalization?',
  'Summarize my DBMS notes.',
  'What are the important concepts in this document?',
  'Explain this topic in simple terms.',
];

/**
 * Lightweight safe markdown formatter for paragraphs, lists, bold text, and code blocks
 * without adding heavy external dependencies.
 */
function FormattedAnswer({ content }: { content: string }) {
  if (!content) return null;

  const lines = content.split('\n');
  const elements: React.ReactNode[] = [];
  let currentList: { type: 'ul' | 'ol'; items: string[] } | null = null;
  let inCodeBlock = false;
  let codeBuffer: string[] = [];

  const flushList = () => {
    if (currentList) {
      if (currentList.type === 'ul') {
        elements.push(
          <ul key={`ul-${elements.length}`} style={{ paddingLeft: 22, margin: '8px 0 14px' }}>
            {currentList.items.map((item, idx) => (
              <li key={idx} style={{ marginBottom: 5 }}>
                {renderInlineMarkdown(item)}
              </li>
            ))}
          </ul>
        );
      } else {
        elements.push(
          <ol key={`ol-${elements.length}`} style={{ paddingLeft: 22, margin: '8px 0 14px' }}>
            {currentList.items.map((item, idx) => (
              <li key={idx} style={{ marginBottom: 5 }}>
                {renderInlineMarkdown(item)}
              </li>
            ))}
          </ol>
        );
      }
      currentList = null;
    }
  };

  const flushCode = () => {
    if (codeBuffer.length > 0) {
      elements.push(
        <pre key={`code-${elements.length}`}>
          <code>{codeBuffer.join('\n')}</code>
        </pre>
      );
      codeBuffer = [];
    }
  };

  for (let i = 0; i < lines.length; i++) {
    const rawLine = lines[i];
    const trimmed = rawLine.trim();

    if (trimmed.startsWith('```')) {
      if (inCodeBlock) {
        flushCode();
        inCodeBlock = false;
      } else {
        flushList();
        inCodeBlock = true;
      }
      continue;
    }

    if (inCodeBlock) {
      codeBuffer.push(rawLine);
      continue;
    }

    if (!trimmed) {
      flushList();
      continue;
    }

    // Bullet points
    if (/^[-*•]\s+/.test(trimmed)) {
      const text = trimmed.replace(/^[-*•]\s+/, '');
      if (!currentList || currentList.type !== 'ul') {
        flushList();
        currentList = { type: 'ul', items: [text] };
      } else {
        currentList.items.push(text);
      }
      continue;
    }

    // Numbered lists
    if (/^\d+\.\s+/.test(trimmed)) {
      const text = trimmed.replace(/^\d+\.\s+/, '');
      if (!currentList || currentList.type !== 'ol') {
        flushList();
        currentList = { type: 'ol', items: [text] };
      } else {
        currentList.items.push(text);
      }
      continue;
    }

    // Regular paragraph
    flushList();
    elements.push(
      <p key={`p-${elements.length}`} style={{ marginBottom: 14 }}>
        {renderInlineMarkdown(rawLine)}
      </p>
    );
  }

  flushList();
  flushCode();

  return <div className="knowledge-answer-body">{elements}</div>;
}

function renderInlineMarkdown(text: string): React.ReactNode {
  const parts: React.ReactNode[] = [];
  // Match bold **text** or inline code `code`
  const regex = /(\*\*[^*]+\*\*|`[^`]+`)/g;
  let lastIndex = 0;
  let match: RegExpExecArray | null;

  while ((match = regex.exec(text)) !== null) {
    if (match.index > lastIndex) {
      parts.push(text.substring(lastIndex, match.index));
    }

    const token = match[0];
    if (token.startsWith('**') && token.endsWith('**')) {
      parts.push(
        <strong key={match.index} style={{ fontWeight: 700, color: 'var(--text-primary)' }}>
          {token.slice(2, -2)}
        </strong>
      );
    } else if (token.startsWith('`') && token.endsWith('`')) {
      parts.push(
        <code key={match.index}>
          {token.slice(1, -1)}
        </code>
      );
    }

    lastIndex = match.index + token.length;
  }

  if (lastIndex < text.length) {
    parts.push(text.substring(lastIndex));
  }

  return parts.length > 0 ? parts : text;
}

export default function KnowledgeAssistantPage() {
  const router = useRouter();
  const [user, setUser] = useState<AuthUser | null>(null);

  // Assistant State
  const [question, setQuestion] = useState('');
  const [lastAskedQuestion, setLastAskedQuestion] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [loadingPhase, setLoadingPhase] = useState<'searching' | 'generating'>('searching');
  const [ragResult, setRagResult] = useState<RagAnswerResponseDto | null>(null);
  const [hasContext, setHasContext] = useState<boolean | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);

  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const phaseTimerRef = useRef<NodeJS.Timeout | null>(null);

  // Authentication check
  useEffect(() => {
    const currentUser = getCurrentUser();
    if (!currentUser) {
      router.replace('/login');
      return;
    }
    setUser(currentUser);
  }, [router]);

  // Clean up loading timer on unmount
  useEffect(() => {
    return () => {
      if (phaseTimerRef.current) clearTimeout(phaseTimerRef.current);
    };
  }, []);

  function handleLogout() {
    logout();
    router.replace('/login');
  }

  function handleSelectSuggestion(suggestion: string) {
    setQuestion(suggestion);
    if (textareaRef.current) {
      textareaRef.current.focus();
    }
  }

  function handleKeyDown(e: React.KeyboardEvent<HTMLTextAreaElement>) {
    if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
      e.preventDefault();
      if (!isLoading && question.trim()) {
        handleSubmit();
      }
    }
  }

  async function handleSubmit(e?: React.FormEvent) {
    if (e) e.preventDefault();

    const trimmed = question.trim();
    if (!trimmed || isLoading) return;

    setIsLoading(true);
    setLoadingPhase('searching');
    setErrorMessage(null);
    setRagResult(null);
    setHasContext(null);
    setLastAskedQuestion(trimmed);

    // Dynamic progression of loading indicator
    if (phaseTimerRef.current) clearTimeout(phaseTimerRef.current);
    phaseTimerRef.current = setTimeout(() => {
      setLoadingPhase('generating');
    }, 1200);

    try {
      const response = await askKnowledgeApi(trimmed, 5);

      if (phaseTimerRef.current) clearTimeout(phaseTimerRef.current);

      // Safe normalization of response
      const normalizedSources: RagSourceDto[] = Array.isArray(response?.sources)
        ? response.sources
        : [];
      const normalizedAnswer: string = response?.answer || '';
      const normalizedModelName: string = response?.modelName || 'MindOS RAG';

      // Check if backend identified sufficient context
      const isInsufficient =
        response?.hasContext === false ||
        (normalizedSources.length === 0 &&
          normalizedAnswer.toLowerCase().includes("couldn't find enough information"));

      setRagResult({
        question: trimmed,
        answer: normalizedAnswer,
        sources: normalizedSources,
        hasContext: !isInsufficient,
        modelName: normalizedModelName,
      });

      setHasContext(!isInsufficient);
    } catch (err: any) {
      if (phaseTimerRef.current) clearTimeout(phaseTimerRef.current);
      console.error('AI Knowledge Assistant request failed:', err);

      let userFriendlyMsg = 'AI service is temporarily unavailable. Please try again in a moment.';
      if (err?.message) {
        if (err.message.includes('HTTP 401') || err.message.includes('Unauthorized') || err.message.includes('User must be authenticated')) {
          userFriendlyMsg = 'Your session has expired. Please log in again to ask the assistant.';
        } else if (err.message.includes('fetch') || err.message.includes('NetworkError') || err.message.includes('Failed to fetch')) {
          userFriendlyMsg = 'Unable to connect to the MindOS backend. Please ensure the backend server is running.';
        } else if (!err.message.includes('Exception') && !err.message.includes('at com.')) {
          userFriendlyMsg = err.message;
        }
      }
      setErrorMessage(userFriendlyMsg);
    } finally {
      setIsLoading(false);
    }
  }

  async function handleCopyAnswer() {
    if (!ragResult?.answer) return;
    try {
      await navigator.clipboard.writeText(ragResult.answer);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      // Fallback if clipboard API unavailable
    }
  }

  if (!user) return null;

  return (
    <div>
      {/* Top Navbar */}
      <header className="navbar">
        <div className="container-fluid nav-inner">
          <Link href="/dashboard" className="logo">
            <span className="logo-mark">M</span>
            MIND OS
          </Link>
          <div className="nav-actions">
            <span className="user-greeting">Hi, {user.name}</span>
            <button className="btn btn-ghost" onClick={handleLogout}>
              <LogOut size={16} />
              Log out
            </button>
          </div>
        </div>
      </header>

      <div className="dashboard-shell">
        {/* Sidebar Navigation */}
        <aside className="dashboard-sidebar">
          <nav className="sidebar-nav">
            <Link href="/dashboard" className="sidebar-link">
              <LayoutDashboard size={18} />
              Dashboard
            </Link>
            <Link href="/dashboard" className="sidebar-link">
              <CheckSquare size={18} />
              Tasks
            </Link>
            <Link href="/dashboard" className="sidebar-link">
              <Folder size={18} />
              Categories
            </Link>
            <Link href="/documents" className="sidebar-link">
              <FileText size={18} />
              Knowledge Base
            </Link>
            <Link href="/knowledge" className="sidebar-link sidebar-link-active">
              <Bot size={18} />
              AI Assistant
            </Link>
            <Link href="/dashboard" className="sidebar-link">
              <Calendar size={18} />
              Calendar
            </Link>
            <Link href="/dashboard" className="sidebar-link">
              <User size={18} />
              Profile
            </Link>
          </nav>
        </aside>

        {/* Main Content Area */}
        <section className="dashboard-content">
          <div className="fade-in knowledge-assistant-wrap">

            {/* 1. Page Header */}
            <div className="knowledge-header">
              <div className="knowledge-header-left">
                <h1>AI Knowledge Assistant</h1>
                <p>Ask questions about your personal knowledge base.</p>
              </div>

              <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                {/* Visual Indicator of Grounded Documents */}
                <div className="knowledge-indicator-badge">
                  <span className="knowledge-indicator-dot" />
                  Grounded in your uploaded documents
                </div>

                <Link href="/documents" className="btn btn-soft btn-sm" title="View uploaded documents">
                  <FileText size={14} />
                  Knowledge Base
                </Link>
              </div>
            </div>

            {/* 2. Question Input Section */}
            <div className="knowledge-card">
              <form onSubmit={handleSubmit} className="knowledge-input-box">
                <label htmlFor="knowledge-question-input" style={{ display: 'none' }}>
                  Ask a question
                </label>
                <textarea
                  id="knowledge-question-input"
                  ref={textareaRef}
                  className="knowledge-textarea"
                  placeholder="Ask something about your notes, documents, or knowledge..."
                  value={question}
                  onChange={(e) => setQuestion(e.target.value)}
                  onKeyDown={handleKeyDown}
                  disabled={isLoading}
                  rows={4}
                />

                <div className="knowledge-input-actions">
                  <div className="knowledge-input-hint">
                    <CornerDownLeft size={13} />
                    <span>Press <kbd>Ctrl</kbd> + <kbd>Enter</kbd> to submit</span>
                  </div>

                  <div style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
                    {question.length > 0 && !isLoading && (
                      <button
                        type="button"
                        className="btn btn-ghost btn-sm"
                        onClick={() => setQuestion('')}
                      >
                        Clear
                      </button>
                    )}

                    <button
                      type="submit"
                      className="btn btn-primary"
                      disabled={!question.trim() || isLoading}
                    >
                      {isLoading ? (
                        <>
                          <Loader2 size={16} className="animate-spin" />
                          Asking AI...
                        </>
                      ) : (
                        <>
                          <Sparkles size={16} />
                          Ask AI
                        </>
                      )}
                    </button>
                  </div>
                </div>
              </form>

              {/* 3. Suggested Questions */}
              <div className="suggested-section">
                <div className="suggested-label">
                  <Sparkles size={13} style={{ color: 'var(--accent-purple)' }} />
                  Suggested Questions
                </div>
                <div className="suggested-chips-grid">
                  {SUGGESTED_QUESTIONS.map((suggestion, idx) => (
                    <button
                      key={idx}
                      type="button"
                      className="suggested-chip-btn"
                      onClick={() => handleSelectSuggestion(suggestion)}
                      disabled={isLoading}
                    >
                      <span>{suggestion}</span>
                    </button>
                  ))}
                </div>
              </div>
            </div>

            {/* 9. Error Handling Card */}
            {errorMessage && (
              <div className="knowledge-error-card fade-in">
                <div className="knowledge-error-icon">
                  <AlertCircle size={22} />
                </div>
                <div className="knowledge-error-info">
                  <div className="knowledge-error-title">Unable to Generate Answer</div>
                  <div className="knowledge-error-message">{errorMessage}</div>
                  <button
                    type="button"
                    className="btn btn-soft btn-sm"
                    onClick={() => handleSubmit()}
                    disabled={isLoading}
                  >
                    <RefreshCw size={13} /> Try Again
                  </button>
                </div>
              </div>
            )}

            {/* 5. Loading State Indicator */}
            {isLoading && (
              <div className="knowledge-loading-card fade-in">
                <div className="knowledge-loading-spinner-wrap">
                  <div className="knowledge-loading-pulse" />
                  <Loader2 size={28} className="animate-spin" style={{ color: 'var(--accent-purple)' }} />
                </div>
                <div>
                  <div className="knowledge-loading-text-main">
                    {loadingPhase === 'searching'
                      ? 'Searching your knowledge...'
                      : 'Generating answer...'}
                  </div>
                  <div className="knowledge-loading-text-sub">
                    {loadingPhase === 'searching'
                      ? 'Retrieving relevant document chunks using semantic similarity'
                      : 'Synthesizing grounded response with source citations'}
                  </div>
                </div>
              </div>
            )}

            {/* 8. No Answer / Insufficient Context State */}
            {!isLoading && hasContext === false && (
              <div className="knowledge-insufficient-card fade-in">
                <div className="knowledge-insufficient-icon">
                  <HelpCircle size={28} />
                </div>
                <div className="knowledge-insufficient-title">
                  No Sufficient Information Found
                </div>
                <div className="knowledge-insufficient-desc">
                  I couldn't find enough information in your knowledge base to answer:
                  <strong style={{ display: 'block', margin: '6px 0', color: 'var(--text-primary)' }}>
                    &ldquo;{lastAskedQuestion}&rdquo;
                  </strong>
                  Please try rephrasing your question, or upload relevant notes and documents to your personal knowledge base.
                </div>
                <div className="knowledge-insufficient-actions">
                  <Link href="/documents" className="btn btn-primary btn-sm">
                    <FileText size={14} />
                    Upload Documents
                  </Link>
                  <button
                    type="button"
                    className="btn btn-soft btn-sm"
                    onClick={() => {
                      if (textareaRef.current) {
                        textareaRef.current.focus();
                        textareaRef.current.select();
                      }
                    }}
                  >
                    Rephrase Question
                  </button>
                </div>
              </div>
            )}

            {/* 6. AI Answer Section */}
            {!isLoading && hasContext === true && ragResult && (
              <div className="knowledge-answer-card fade-in">
                {/* Header */}
                <div className="knowledge-answer-header">
                  <div className="knowledge-answer-title-wrap">
                    <div className="knowledge-ai-avatar">
                      <Bot size={20} />
                    </div>
                    <div>
                      <div className="knowledge-answer-title">AI Answer</div>
                      <div style={{ fontSize: '0.82rem', color: 'var(--text-muted)' }}>
                        Q: {ragResult.question}
                      </div>
                    </div>
                  </div>

                  <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                    {ragResult.modelName && (
                      <span className="knowledge-model-tag">
                        {ragResult.modelName}
                      </span>
                    )}
                    <button
                      type="button"
                      className="btn btn-ghost btn-sm"
                      onClick={handleCopyAnswer}
                      title="Copy answer to clipboard"
                    >
                      {copied ? (
                        <>
                          <Check size={14} style={{ color: 'var(--success)' }} />
                          <span style={{ color: 'var(--success)' }}>Copied</span>
                        </>
                      ) : (
                        <>
                          <Copy size={14} />
                          <span>Copy</span>
                        </>
                      )}
                    </button>
                  </div>
                </div>

                {/* Generated Answer Content */}
                <FormattedAnswer content={ragResult.answer} />

                {/* 7. Sources Section */}
                {ragResult.sources && ragResult.sources.length > 0 && (
                  <div className="knowledge-sources-wrap">
                    <div className="knowledge-sources-heading">
                      <div className="knowledge-sources-title">
                        <FileText size={16} style={{ color: 'var(--accent-purple)' }} />
                        <span>Sources</span>
                        <span className="knowledge-sources-count">
                          {ragResult.sources.length}
                        </span>
                      </div>
                      <span style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>
                        Retrieved from your personal documents
                      </span>
                    </div>

                    <div className="knowledge-sources-grid">
                      {ragResult.sources.map((source, index) => {
                        const similarityPercent = Math.round(
                          (typeof source.similarity === 'number' ? source.similarity : 0) * 100
                        );
                        return (
                          <div
                            key={source.chunkId ?? index}
                            className="knowledge-source-card"
                          >
                            <div className="knowledge-source-top">
                              <div className="knowledge-source-icon">
                                <FileText size={16} />
                              </div>
                              <div className="knowledge-source-details">
                                <div
                                  className="knowledge-source-name"
                                  title={source.documentName}
                                >
                                  {source.documentName || 'Document'}
                                </div>
                                <div className="knowledge-source-chunk">
                                  Chunk {(source.chunkIndex ?? 0) + 1}
                                </div>
                              </div>
                            </div>

                            <div className="knowledge-source-bottom">
                              <span style={{ fontSize: '0.76rem', color: 'var(--text-muted)' }}>
                                Relevance
                              </span>
                              <span className="knowledge-similarity-badge">
                                Similarity: {similarityPercent}%
                              </span>
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  </div>
                )}
              </div>
            )}

          </div>
        </section>
      </div>
    </div>
  );
}
