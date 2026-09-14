'use client';

import { useState, useEffect, useMemo, useRef } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import {
  LayoutDashboard, CheckSquare, FileText, Calendar, Bot, User,
  LogOut, Plus, Search, Folder, X, AlertCircle, CheckCircle2,
  Trash2, Eye, Upload, FileCode, AlignLeft, HardDrive, Filter,
  Clock, RefreshCw, Loader2, Edit3, Download, Sparkles
} from 'lucide-react';
import { getCurrentUser, logout, type User as AuthUser } from '@/lib/auth';
import {
  getDocumentsApi,
  getDocumentByIdApi,
  getDocumentContentApi,
  extractDocumentTextApi,
  uploadDocumentFileApi,
  updateDocumentApi,
  deleteDocumentApi,
  getDocumentDownloadUrl,
  type DocumentDto,
  type DocumentContentDto
} from '@/lib/documentApi';
import {
  searchKnowledgeApi,
  askKnowledgeApi,
  type SemanticSearchResultDto,
  type RagAnswerResponseDto
} from '@/lib/knowledgeApi';

type FileTypeFilter = 'ALL' | 'PDF' | 'DOCX' | 'TXT';
type SortFilter = 'RECENT' | 'OLDEST' | 'NAME' | 'SIZE';

export default function DocumentsPage() {
  const router = useRouter();
  const [user, setUser] = useState<AuthUser | null>(null);

  // Document State (Connected to PostgreSQL Backend + Real File Storage + Extraction)
  const [documents, setDocuments] = useState<DocumentDto[]>([]);
  const [isLoadingDocuments, setIsLoadingDocuments] = useState(true);
  const [documentApiError, setDocumentApiError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  // Search and Filter State
  const [searchQuery, setSearchQuery] = useState('');
  const [typeFilter, setTypeFilter] = useState<FileTypeFilter>('ALL');
  const [sortFilter, setSortFilter] = useState<SortFilter>('RECENT');

  // Modals State
  const [isUploadModalOpen, setIsUploadModalOpen] = useState(false);
  const [isSearchModalOpen, setIsSearchModalOpen] = useState(false);
  const [selectedDocForDetails, setSelectedDocForDetails] = useState<DocumentDto | null>(null);
  const [selectedDocContent, setSelectedDocContent] = useState<DocumentContentDto | null>(null);
  const [isLoadingContent, setIsLoadingContent] = useState(false);
  const [isExtractingText, setIsExtractingText] = useState(false);
  const [deletingDoc, setDeletingDoc] = useState<DocumentDto | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Semantic Search & RAG Modal State (STEP 4.7 & 4.8)
  const [knowledgeMode, setKnowledgeMode] = useState<'RAG' | 'SEARCH'>('RAG');
  const [semanticQuery, setSemanticQuery] = useState('');
  const [semanticTopK, setSemanticTopK] = useState(5);
  const [semanticResults, setSemanticResults] = useState<SemanticSearchResultDto[]>([]);
  const [isSearchingSemantic, setIsSearchingSemantic] = useState(false);
  const [semanticSearchError, setSemanticSearchError] = useState<string | null>(null);
  const [hasSearchedSemantic, setHasSearchedSemantic] = useState(false);

  // RAG Q&A State (STEP 4.8)
  const [ragAnswer, setRagAnswer] = useState<RagAnswerResponseDto | null>(null);
  const [isAskingRag, setIsAskingRag] = useState(false);

  // Edit State in Details Modal
  const [isEditingDescription, setIsEditingDescription] = useState(false);
  const [editDescValue, setEditDescValue] = useState('');

  // Upload Form State
  const [uploadedFile, setUploadedFile] = useState<File | null>(null);
  const [uploadTitle, setUploadTitle] = useState('');
  const [uploadDesc, setUploadDesc] = useState('');
  const [uploadError, setUploadError] = useState<string | null>(null);
  const [isDragging, setIsDragging] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    const u = getCurrentUser();
    if (!u) {
      router.replace('/login');
      return;
    }
    setUser(u);
    loadDocumentsFromBackend();
  }, [router]);

  async function loadDocumentsFromBackend() {
    setIsLoadingDocuments(true);
    setDocumentApiError(null);
    try {
      const data = await getDocumentsApi();
      setDocuments(data);
    } catch (err: any) {
      console.error('Failed to load documents:', err);
      setDocumentApiError('Unable to load documents.');
    } finally {
      setIsLoadingDocuments(false);
    }
  }

  function handleLogout() {
    logout();
    router.replace('/login');
  }

  // Format Helpers
  function formatBytes(bytes: number): string {
    if (!bytes || bytes === 0) return '0 B';
    if (bytes >= 1048576) return (bytes / 1048576).toFixed(1) + ' MB';
    if (bytes >= 1024) return (bytes / 1024).toFixed(0) + ' KB';
    return bytes + ' B';
  }

  function formatDate(isoStr?: string): string {
    if (!isoStr) return 'Recently';
    try {
      const d = new Date(isoStr);
      return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
    } catch {
      return isoStr;
    }
  }

  // Summary Metrics
  const summary = useMemo(() => {
    const total = documents.length;
    const pdfs = documents.filter(d => (d.fileType || '').toUpperCase() === 'PDF').length;
    const others = documents.filter(d => (d.fileType || '').toUpperCase() !== 'PDF').length;
    const totalBytes = documents.reduce((sum, d) => sum + (d.fileSize || 0), 0);

    let storageFormatted = '0 KB';
    if (totalBytes >= 1048576) {
      storageFormatted = (totalBytes / 1048576).toFixed(1) + ' MB';
    } else if (totalBytes > 0) {
      storageFormatted = (totalBytes / 1024).toFixed(0) + ' KB';
    }

    return { total, pdfs, others, storageFormatted };
  }, [documents]);

  // Filtered and Sorted Documents
  const filteredDocuments = useMemo(() => {
    let result = [...documents];

    // 1. Search filter
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase();
      result = result.filter(doc =>
        (doc.fileName && doc.fileName.toLowerCase().includes(q)) ||
        (doc.title && doc.title.toLowerCase().includes(q)) ||
        (doc.description && doc.description.toLowerCase().includes(q)) ||
        (doc.fileType && doc.fileType.toLowerCase().includes(q))
      );
    }

    // 2. Type filter
    if (typeFilter !== 'ALL') {
      result = result.filter(doc => (doc.fileType || '').toUpperCase() === typeFilter);
    }

    // 3. Sorting
    result.sort((a, b) => {
      if (sortFilter === 'NAME') {
        return (a.title || a.fileName).localeCompare(b.title || b.fileName);
      }
      if (sortFilter === 'SIZE') {
        return (b.fileSize || 0) - (a.fileSize || 0);
      }
      if (sortFilter === 'OLDEST') {
        return String(a.id).localeCompare(String(b.id));
      }
      return String(b.id).localeCompare(String(a.id));
    });

    return result;
  }, [documents, searchQuery, typeFilter, sortFilter]);

  // Handle File Selection
  function handleFileSelected(file: File) {
    setUploadError(null);
    const validExtensions = ['pdf', 'docx', 'txt'];
    const ext = file.name.split('.').pop()?.toLowerCase() || '';

    if (!validExtensions.includes(ext)) {
      setUploadError('Invalid file type. Supported formats: PDF, DOCX, TXT.');
      return;
    }

    if (file.size > 10 * 1024 * 1024) {
      setUploadError('File size exceeds the 10 MB limit.');
      return;
    }

    setUploadedFile(file);
    const baseName = file.name.replace(/\.[^/.]+$/, "");
    setUploadTitle(baseName);
  }

  function handleDrop(e: React.DragEvent) {
    e.preventDefault();
    setIsDragging(false);
    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      handleFileSelected(e.dataTransfer.files[0]);
    }
  }

  function openUploadModal() {
    setUploadedFile(null);
    setUploadTitle('');
    setUploadDesc('');
    setUploadError(null);
    setIsDragging(false);
    setActionError(null);
    setIsUploadModalOpen(true);
  }

  function openSearchModal() {
    setSemanticQuery('');
    setSemanticResults([]);
    setSemanticSearchError(null);
    setHasSearchedSemantic(false);
    setRagAnswer(null);
    setIsSearchModalOpen(true);
  }

  async function handleExecuteSemanticSearch(e: React.FormEvent) {
    e.preventDefault();
    if (!semanticQuery.trim()) return;

    setIsSearchingSemantic(true);
    setSemanticSearchError(null);
    setHasSearchedSemantic(true);

    try {
      const resp = await searchKnowledgeApi(semanticQuery.trim(), semanticTopK);
      setSemanticResults(resp.results || []);
    } catch (err: any) {
      console.error('Semantic search failed:', err);
      setSemanticSearchError(err.message || 'Semantic search failed. Please verify embedding service.');
      setSemanticResults([]);
    } finally {
      setIsSearchingSemantic(false);
    }
  }

  async function handleExecuteRagAsk(e: React.FormEvent) {
    e.preventDefault();
    if (!semanticQuery.trim()) return;

    setIsAskingRag(true);
    setSemanticSearchError(null);
    setRagAnswer(null);

    try {
      const resp = await askKnowledgeApi(semanticQuery.trim(), semanticTopK);
      setRagAnswer(resp);
    } catch (err: any) {
      console.error('RAG pipeline failed:', err);
      setSemanticSearchError(err.message || 'AI Knowledge Assistant is temporarily unavailable.');
    } finally {
      setIsAskingRag(false);
    }
  }

  // Actual Multipart File Upload + Automatic Extraction (STEP 4.3 & 4.4)
  async function handleAddDocument(e: React.FormEvent) {
    e.preventDefault();
    if (!uploadedFile) {
      setUploadError('Please choose or drag a file to upload.');
      return;
    }

    setIsSubmitting(true);
    setUploadError(null);

    try {
      await uploadDocumentFileApi(uploadedFile, uploadTitle, uploadDesc);
      await loadDocumentsFromBackend();
      setIsUploadModalOpen(false);
    } catch (err: any) {
      console.error('Failed to upload and process document:', err);
      setUploadError(err.message || 'Unable to upload document file.');
    } finally {
      setIsSubmitting(false);
    }
  }

  async function handleOpenDetails(doc: DocumentDto) {
    setActionError(null);
    setIsEditingDescription(false);
    setEditDescValue(doc.description || '');
    setSelectedDocForDetails(doc);
    setSelectedDocContent(null);
    setIsLoadingContent(true);

    try {
      const [freshDoc, freshContent] = await Promise.all([
        getDocumentByIdApi(doc.id).catch(() => doc),
        getDocumentContentApi(doc.id).catch(() => null)
      ]);
      setSelectedDocForDetails(freshDoc);
      setSelectedDocContent(freshContent);
    } catch (err: any) {
      console.warn('Could not fetch fresh details/content:', err);
    } finally {
      setIsLoadingContent(false);
    }
  }

  async function handleRetryExtraction() {
    if (!selectedDocForDetails) return;
    setIsExtractingText(true);
    setActionError(null);

    try {
      const extracted = await extractDocumentTextApi(selectedDocForDetails.id);
      setSelectedDocContent(extracted);
      // Refresh document status to READY
      const updatedDoc = { ...selectedDocForDetails, status: 'READY' };
      setSelectedDocForDetails(updatedDoc);
      setDocuments(prev => prev.map(d => d.id === updatedDoc.id ? updatedDoc : d));
    } catch (err: any) {
      console.error('Failed to extract text:', err);
      setActionError(err.message || 'Text extraction failed.');
    } finally {
      setIsExtractingText(false);
    }
  }

  async function handleUpdateDescription() {
    if (!selectedDocForDetails) return;
    setIsSubmitting(true);
    try {
      const updated = await updateDocumentApi(selectedDocForDetails.id, {
        description: editDescValue.trim()
      });
      setSelectedDocForDetails(updated);
      setDocuments(prev => prev.map(d => d.id === updated.id ? updated : d));
      setIsEditingDescription(false);
    } catch (err: any) {
      console.error('Failed to update document description:', err);
      setActionError(err.message || 'Failed to update document.');
    } finally {
      setIsSubmitting(false);
    }
  }

  function confirmDelete(doc: DocumentDto, e?: React.MouseEvent) {
    if (e) e.stopPropagation();
    setActionError(null);
    setDeletingDoc(doc);
  }

  async function executeDelete() {
    if (!deletingDoc) return;
    setIsSubmitting(true);
    setActionError(null);
    try {
      await deleteDocumentApi(deletingDoc.id);
      setDocuments(prev => prev.filter(d => d.id !== deletingDoc.id));
      if (selectedDocForDetails && selectedDocForDetails.id === deletingDoc.id) {
        setSelectedDocForDetails(null);
      }
      setDeletingDoc(null);
    } catch (err: any) {
      console.error('Failed to delete document:', err);
      setActionError(err.message || 'Unable to delete document.');
    } finally {
      setIsSubmitting(false);
    }
  }

  function handleDownloadFile(docId: string | number, fileName: string, e?: React.MouseEvent) {
    if (e) e.stopPropagation();
    const downloadUrl = getDocumentDownloadUrl(docId);
    const link = document.createElement('a');
    link.href = downloadUrl;
    link.download = fileName;
    link.target = '_blank';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }

  function renderFileIcon(type?: string) {
    const t = (type || '').toUpperCase();
    if (t === 'PDF') {
      return (
        <div className="document-icon-box doc-icon-pdf">
          <FileText size={22} />
        </div>
      );
    }
    if (t === 'DOCX' || t === 'DOC') {
      return (
        <div className="document-icon-box doc-icon-docx">
          <FileCode size={22} />
        </div>
      );
    }
    return (
      <div className="document-icon-box doc-icon-txt">
        <AlignLeft size={22} />
      </div>
    );
  }

  function renderStatusBadge(status?: string) {
    const s = (status || 'READY').toUpperCase();
    if (s === 'READY' || s === 'PROCESSED') {
      return (
        <span className="doc-status-badge doc-status-ready">
          <CheckCircle2 size={12} /> Ready
        </span>
      );
    }
    if (s === 'PROCESSING' || s === 'UPLOADED') {
      return (
        <span className="doc-status-badge doc-status-processing">
          <Clock size={12} /> Processing
        </span>
      );
    }
    return (
      <span className="doc-status-badge doc-status-failed">
        <AlertCircle size={12} /> Failed
      </span>
    );
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
            <Link href="/documents" className="sidebar-link sidebar-link-active">
              <FileText size={18} />
              Knowledge Base
            </Link>
            <Link href="/dashboard" className="sidebar-link">
              <Calendar size={18} />
              Calendar
            </Link>
            <Link href="/knowledge" className="sidebar-link">
              <Bot size={18} />
              AI Assistant
            </Link>
            <Link href="/dashboard" className="sidebar-link">
              <User size={18} />
              Profile
            </Link>
          </nav>
        </aside>

        {/* Main Content Area */}
        <section className="dashboard-content">
          <div className="fade-in">

            {/* Action / General Error Banner */}
            {actionError && (
              <div className="form-error fade-in" style={{ marginBottom: 20 }}>
                {actionError}
              </div>
            )}

            {/* 1. Page Header */}
            <div className="task-header" style={{ marginBottom: 24 }}>
              <div className="task-header-left">
                <h1>Knowledge Base</h1>
                <p>Store documents, extract plain text, and build your personal knowledge space.</p>
              </div>
              <div style={{ display: 'flex', gap: 10 }}>
                <button
                  className="btn btn-soft"
                  onClick={loadDocumentsFromBackend}
                  title="Refresh documents"
                  disabled={isLoadingDocuments}
                >
                  <RefreshCw size={16} className={isLoadingDocuments ? 'animate-spin' : ''} />
                </button>
                <button className="btn btn-soft" onClick={openSearchModal} style={{ borderColor: 'var(--accent-purple)' }}>
                  <Sparkles size={16} style={{ color: 'var(--accent-purple)' }} />
                  Semantic Search
                </button>
                <button className="btn btn-primary" onClick={openUploadModal}>
                  <Plus size={18} />
                  Upload Document
                </button>
              </div>
            </div>

            {/* Error State */}
            {documentApiError && (
              <div className="empty-state fade-in" style={{ padding: 24, marginBottom: 24, borderColor: 'var(--pastel-pink-border)' }}>
                <AlertCircle size={32} color="var(--pastel-pink-text)" style={{ marginBottom: 8 }} />
                <h3 style={{ color: 'var(--pastel-pink-text)' }}>{documentApiError}</h3>
                <p style={{ marginBottom: 12 }}>Could not retrieve documents from PostgreSQL. Please check server connection.</p>
                <button className="btn btn-primary btn-sm" onClick={loadDocumentsFromBackend}>
                  <RefreshCw size={14} /> Retry
                </button>
              </div>
            )}

            {/* 2. Document Summary Statistics Cards */}
            <div className="task-stats-grid" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))', marginBottom: 24 }}>
              <div className="stat-card">
                <div className="stat-icon stat-icon-total"><FileText size={20} /></div>
                <div className="stat-info">
                  <span>Total Documents</span>
                  <strong>{summary.total}</strong>
                </div>
              </div>

              <div className="stat-card">
                <div className="stat-icon stat-icon-overdue"><FileText size={20} /></div>
                <div className="stat-info">
                  <span>PDF Files</span>
                  <strong>{summary.pdfs}</strong>
                </div>
              </div>

              <div className="stat-card">
                <div className="stat-icon stat-icon-progress"><FileCode size={20} /></div>
                <div className="stat-info">
                  <span>Text / Other</span>
                  <strong>{summary.others}</strong>
                </div>
              </div>

              <div className="stat-card">
                <div className="stat-icon stat-icon-today"><HardDrive size={20} /></div>
                <div className="stat-info">
                  <span>Storage Used</span>
                  <strong>{summary.storageFormatted}</strong>
                </div>
              </div>
            </div>

            {/* 3. Search and Filters Toolbar */}
            <div className="task-toolbar" style={{ marginBottom: 24 }}>
              <div className="task-filter-bar">
                {/* Search */}
                <div className="task-search-wrap">
                  <Search size={16} className="task-search-icon" />
                  <input
                    type="text"
                    className="task-search-input"
                    placeholder="Search documents by name, title, or type..."
                    value={searchQuery}
                    onChange={e => setSearchQuery(e.target.value)}
                  />
                  {searchQuery && (
                    <button
                      className="search-clear"
                      onClick={() => setSearchQuery('')}
                      style={{ position: 'absolute', right: 12, top: '50%', transform: 'translateY(-50%)', background: 'none', border: 'none', color: 'var(--text-muted)', cursor: 'pointer' }}
                    >
                      <X size={14} />
                    </button>
                  )}
                </div>

                {/* Type Filter Buttons */}
                <div className="filter-group">
                  {(['ALL', 'PDF', 'DOCX', 'TXT'] as FileTypeFilter[]).map(t => (
                    <button
                      key={t}
                      className={`task-tab ${typeFilter === t ? 'task-tab-active' : ''}`}
                      onClick={() => setTypeFilter(t)}
                      style={{ padding: '6px 14px', fontSize: '0.82rem' }}
                    >
                      {t === 'ALL' ? 'All Files' : t}
                    </button>
                  ))}

                  {/* Sort Filter Dropdown */}
                  <select
                    className="filter-select"
                    value={sortFilter}
                    onChange={e => setSortFilter(e.target.value as SortFilter)}
                  >
                    <option value="RECENT">Sort: Recently Added</option>
                    <option value="OLDEST">Sort: Oldest</option>
                    <option value="NAME">Sort: Name (A-Z)</option>
                    <option value="SIZE">Sort: Size (Largest)</option>
                  </select>
                </div>
              </div>
            </div>

            {/* 4. Loading State */}
            {isLoadingDocuments && (
              <div className="empty-state fade-in" style={{ padding: 48 }}>
                <Loader2 size={36} className="animate-spin" style={{ color: 'var(--accent-purple)', marginBottom: 12 }} />
                <h3>Loading documents from PostgreSQL...</h3>
                <p>Retrieving your personal knowledge base.</p>
              </div>
            )}

            {/* 5. Documents Grid / Empty State */}
            {!isLoadingDocuments && !documentApiError && (
              filteredDocuments.length === 0 ? (
                <div className="empty-state fade-in" style={{ padding: 48 }}>
                  <div className="empty-icon-wrap"><FileText size={36} /></div>
                  <h3>{searchQuery || typeFilter !== 'ALL' ? 'No matching documents' : 'No documents yet'}</h3>
                  <p>
                    {searchQuery || typeFilter !== 'ALL'
                      ? 'Try adjusting your search terms or filter selection.'
                      : 'Upload your first PDF, DOCX, or TXT document to extract text and build your knowledge base.'}
                  </p>
                  <button className="btn btn-primary" onClick={openUploadModal} style={{ marginTop: 12 }}>
                    <Plus size={16} /> Upload Document
                  </button>
                </div>
              ) : (
                <div className="documents-grid fade-in">
                  {filteredDocuments.map(doc => (
                    <div
                      key={doc.id}
                      className="document-card"
                      onClick={() => handleOpenDetails(doc)}
                      style={{ cursor: 'pointer' }}
                    >
                      <div>
                        <div className="document-card-top">
                          {renderFileIcon(doc.fileType)}
                          <div className="document-header-info">
                            <h3 className="document-title" title={doc.title || doc.fileName}>{doc.title || doc.fileName}</h3>
                            <span className="document-meta-text">
                              {(doc.fileType || 'DOC').toUpperCase()} · {formatBytes(doc.fileSize)} · {formatDate(doc.uploadedAt)}
                            </span>
                          </div>
                        </div>

                        {doc.description ? (
                          <p className="document-desc">{doc.description}</p>
                        ) : (
                          <p className="document-desc" style={{ color: 'var(--text-muted)', fontStyle: 'italic' }}>
                            {doc.fileName}
                          </p>
                        )}
                      </div>

                      <div className="document-card-footer">
                        {renderStatusBadge(doc.status)}

                        <div style={{ display: 'flex', gap: 6 }}>
                          {doc.storageUrl && (
                            <button
                              className="btn btn-soft"
                              style={{ padding: '6px 10px', fontSize: '0.82rem' }}
                              onClick={(e) => handleDownloadFile(doc.id, doc.fileName, e)}
                              title="Download File"
                            >
                              <Download size={13} />
                            </button>
                          )}
                          <button
                            className="btn btn-soft"
                            style={{ padding: '6px 12px', fontSize: '0.82rem' }}
                            onClick={(e) => {
                              e.stopPropagation();
                              handleOpenDetails(doc);
                            }}
                            title="View Details & Extracted Text"
                          >
                            <Eye size={13} /> View
                          </button>
                          <button
                            className="btn btn-danger"
                            style={{ padding: '6px 10px', fontSize: '0.82rem' }}
                            onClick={(e) => confirmDelete(doc, e)}
                            title="Delete Document"
                          >
                            <Trash2 size={13} />
                          </button>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              )
            )}
          </div>
        </section>
      </div>

      {/* UPLOAD DOCUMENT MODAL (STEP 4.3 & 4.4 REAL FILE UPLOAD + EXTRACTION) */}
      {isUploadModalOpen && (
        <div className="modal-overlay" onClick={() => !isSubmitting && setIsUploadModalOpen(false)}>
          <div className="modal-card" onClick={e => e.stopPropagation()} style={{ maxWidth: 520 }}>
            <div className="modal-header">
              <div>
                <h2>Upload Document</h2>
                <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>Upload file, extract text content &amp; save to PostgreSQL.</p>
              </div>
              <button className="close-btn" onClick={() => !isSubmitting && setIsUploadModalOpen(false)} disabled={isSubmitting}>
                <X size={18} />
              </button>
            </div>

            <form onSubmit={handleAddDocument}>
              <div className="modal-body">
                {uploadError && (
                  <div className="form-error" style={{ marginBottom: 14 }}>
                    {uploadError}
                  </div>
                )}

                {/* Drag and Drop Zone */}
                <div
                  className={`upload-dropzone ${isDragging ? 'drag-active' : ''}`}
                  onDragOver={(e) => { e.preventDefault(); setIsDragging(true); }}
                  onDragLeave={() => setIsDragging(false)}
                  onDrop={handleDrop}
                  onClick={() => fileInputRef.current?.click()}
                >
                  <input
                    ref={fileInputRef}
                    type="file"
                    accept=".pdf,.docx,.txt"
                    style={{ display: 'none' }}
                    onChange={(e) => {
                      if (e.target.files && e.target.files[0]) {
                        handleFileSelected(e.target.files[0]);
                      }
                    }}
                  />
                  <div className="dropzone-icon">
                    <Upload size={28} />
                  </div>
                  <span className="dropzone-title">Drag and drop your file here</span>
                  <span className="dropzone-hint">or <strong style={{ color: 'var(--accent-purple)' }}>Choose a file</strong> from your computer</span>
                  <span style={{ fontSize: '0.78rem', color: 'var(--text-muted)', marginTop: 8 }}>
                    Supported formats: PDF, DOCX, TXT (Maximum file size: 10 MB)
                  </span>
                </div>

                {/* Selected File Preview Box */}
                {uploadedFile && (
                  <div className="selected-file-preview">
                    {renderFileIcon(
                      uploadedFile.name.endsWith('.pdf') ? 'PDF' :
                      uploadedFile.name.endsWith('.docx') ? 'DOCX' : 'TXT'
                    )}
                    <div className="preview-file-info">
                      <p className="preview-file-name">{uploadedFile.name}</p>
                      <span className="preview-file-meta">{formatBytes(uploadedFile.size)}</span>
                    </div>
                    <button
                      type="button"
                      className="btn btn-ghost"
                      style={{ padding: 4 }}
                      onClick={() => setUploadedFile(null)}
                      title="Remove selected file"
                    >
                      <X size={16} />
                    </button>
                  </div>
                )}

                {/* Document Title */}
                <div className="form-group" style={{ marginTop: 16 }}>
                  <label>Document Title</label>
                  <input
                    type="text"
                    className="form-input"
                    placeholder="e.g., DBMS Notes & Normalization"
                    value={uploadTitle}
                    onChange={e => setUploadTitle(e.target.value)}
                    disabled={isSubmitting}
                  />
                </div>

                {/* Document Description */}
                <div className="form-group">
                  <label>Description (Optional)</label>
                  <textarea
                    className="form-input"
                    rows={3}
                    placeholder="Add brief notes or summary of this document..."
                    value={uploadDesc}
                    onChange={e => setUploadDesc(e.target.value)}
                    style={{ resize: 'vertical' }}
                    disabled={isSubmitting}
                  />
                </div>
              </div>

              <div className="modal-footer">
                <button type="button" className="btn btn-soft" onClick={() => setIsUploadModalOpen(false)} disabled={isSubmitting}>
                  Cancel
                </button>
                <button type="submit" className="btn btn-primary" disabled={!uploadedFile || isSubmitting}>
                  {isSubmitting ? (
                    <><Loader2 size={16} className="animate-spin" /> Uploading &amp; Extracting Text...</>
                  ) : (
                    'Upload & Extract'
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* DOCUMENT DETAILS MODAL WITH PLAIN TEXT PREVIEW */}
      {selectedDocForDetails && (
        <div className="modal-overlay" onClick={() => setSelectedDocForDetails(null)}>
          <div className="modal-card" onClick={e => e.stopPropagation()} style={{ maxWidth: 620 }}>
            <div className="modal-header">
              <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                {renderFileIcon(selectedDocForDetails.fileType)}
                <div>
                  <h2 style={{ fontSize: '1.2rem', marginBottom: 2 }}>{selectedDocForDetails.title || selectedDocForDetails.fileName}</h2>
                  <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>{selectedDocForDetails.fileName}</span>
                </div>
              </div>
              <button className="close-btn" onClick={() => setSelectedDocForDetails(null)}>
                <X size={18} />
              </button>
            </div>

            <div className="modal-body" style={{ maxHeight: '70vh', overflowY: 'auto' }}>
              {/* Status and Details Grid */}
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 10, marginBottom: 16 }}>
                <div className="profile-detail" style={{ padding: '8px 12px' }}>
                  <span style={{ fontSize: '0.72rem' }}>File Type</span>
                  <strong style={{ fontSize: '0.88rem' }}>{(selectedDocForDetails.fileType || 'DOC').toUpperCase()}</strong>
                </div>
                <div className="profile-detail" style={{ padding: '8px 12px' }}>
                  <span style={{ fontSize: '0.72rem' }}>File Size</span>
                  <strong style={{ fontSize: '0.88rem' }}>{formatBytes(selectedDocForDetails.fileSize)}</strong>
                </div>
                <div className="profile-detail" style={{ padding: '8px 12px' }}>
                  <span style={{ fontSize: '0.72rem' }}>Status</span>
                  <div>{renderStatusBadge(selectedDocForDetails.status)}</div>
                </div>
                <div className="profile-detail" style={{ padding: '8px 12px' }}>
                  <span style={{ fontSize: '0.72rem' }}>Text Extracted</span>
                  <strong style={{ fontSize: '0.88rem', color: selectedDocContent?.hasText ? 'var(--pastel-green-text)' : 'var(--text-muted)' }}>
                    {selectedDocContent?.hasText
                      ? `Yes (${selectedDocContent.wordCount} words · ${selectedDocContent.chunkCount || 1} chunks · ${selectedDocContent.embeddingCount ?? (selectedDocContent.chunkCount || 1)} vectors)`
                      : 'None'}
                  </strong>
                </div>
              </div>

              {/* Description & Editable Description */}
              <div style={{ marginBottom: 16 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 4 }}>
                  <label style={{ fontSize: '0.82rem', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase' }}>
                    Description
                  </label>
                  {!isEditingDescription && (
                    <button
                      className="link-btn"
                      style={{ fontSize: '0.8rem', display: 'flex', alignItems: 'center', gap: 4 }}
                      onClick={() => {
                        setEditDescValue(selectedDocForDetails.description || '');
                        setIsEditingDescription(true);
                      }}
                    >
                      <Edit3 size={12} /> Edit
                    </button>
                  )}
                </div>

                {isEditingDescription ? (
                  <div style={{ marginTop: 6 }}>
                    <textarea
                      className="form-input"
                      rows={3}
                      value={editDescValue}
                      onChange={e => setEditDescValue(e.target.value)}
                      placeholder="Enter updated description..."
                    />
                    <div style={{ display: 'flex', gap: 8, marginTop: 8, justifyContent: 'flex-end' }}>
                      <button
                        type="button"
                        className="btn btn-soft btn-sm"
                        onClick={() => setIsEditingDescription(false)}
                        disabled={isSubmitting}
                      >
                        Cancel
                      </button>
                      <button
                        type="button"
                        className="btn btn-primary btn-sm"
                        onClick={handleUpdateDescription}
                        disabled={isSubmitting}
                      >
                        {isSubmitting ? <Loader2 size={13} className="animate-spin" /> : 'Save'}
                      </button>
                    </div>
                  </div>
                ) : (
                  <p style={{ marginTop: 4, fontSize: '0.92rem', color: selectedDocForDetails.description ? 'var(--text-secondary)' : 'var(--text-muted)', lineHeight: 1.6 }}>
                    {selectedDocForDetails.description || 'No description provided.'}
                  </p>
                )}
              </div>

              {/* PLAIN-TEXT EXTRACTION PREVIEW (STEP 4.4) */}
              <div style={{ marginBottom: 16 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 6 }}>
                  <label style={{ fontSize: '0.82rem', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase' }}>
                    Extracted Text Content
                  </label>
                  <button
                    className="link-btn"
                    style={{ fontSize: '0.8rem', display: 'flex', alignItems: 'center', gap: 4 }}
                    onClick={handleRetryExtraction}
                    disabled={isExtractingText}
                  >
                    <RefreshCw size={12} className={isExtractingText ? 'animate-spin' : ''} />
                    {isExtractingText ? 'Extracting...' : 'Re-extract Text'}
                  </button>
                </div>

                {isLoadingContent ? (
                  <div style={{ padding: 24, textAlign: 'center', background: 'var(--pastel-bg)', borderRadius: 'var(--radius-md)' }}>
                    <Loader2 size={24} className="animate-spin" style={{ margin: '0 auto 8px', color: 'var(--accent-purple)' }} />
                    <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>Loading extracted text...</span>
                  </div>
                ) : selectedDocContent && selectedDocContent.hasText ? (
                  <div style={{
                    padding: '12px 14px',
                    background: 'var(--input-bg, rgba(255,255,255,0.03))',
                    border: '1px solid var(--border-color)',
                    borderRadius: 'var(--radius-md)',
                    maxHeight: 220,
                    overflowY: 'auto',
                    fontFamily: 'inherit',
                    fontSize: '0.88rem',
                    lineHeight: 1.6,
                    color: 'var(--text-primary)',
                    whiteSpace: 'pre-wrap',
                    wordBreak: 'break-word'
                  }}>
                    {selectedDocContent.extractedText}
                  </div>
                ) : (
                  <div style={{
                    padding: '14px 16px',
                    background: 'var(--pastel-bg)',
                    border: '1px dashed var(--border-color)',
                    borderRadius: 'var(--radius-md)',
                    textAlign: 'center'
                  }}>
                    <p style={{ fontSize: '0.86rem', color: 'var(--text-secondary)', marginBottom: 6 }}>
                      No extractable plain text available.
                    </p>
                    <span style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>
                      Scanned images without selectable text are not supported yet (OCR is not implemented).
                    </span>
                  </div>
                )}
              </div>

              {/* Download Stored File */}
              {selectedDocForDetails.storageUrl && (
                <div style={{ padding: '10px 14px', background: 'var(--pastel-bg)', borderRadius: 'var(--radius-md)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <div>
                    <span style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-primary)' }}>Stored File Reference</span>
                    <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>{selectedDocForDetails.fileName} ({formatBytes(selectedDocForDetails.fileSize)})</p>
                  </div>
                  <button
                    className="btn btn-primary btn-sm"
                    onClick={() => handleDownloadFile(selectedDocForDetails.id, selectedDocForDetails.fileName)}
                  >
                    <Download size={13} /> Download
                  </button>
                </div>
              )}
            </div>

            <div className="modal-footer" style={{ justifyContent: 'space-between' }}>
              <button
                type="button"
                className="btn btn-danger"
                onClick={() => confirmDelete(selectedDocForDetails)}
              >
                <Trash2 size={14} /> Delete Document
              </button>
              <button
                type="button"
                className="btn btn-soft"
                onClick={() => setSelectedDocForDetails(null)}
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}

      {/* DELETE CONFIRMATION MODAL */}
      {deletingDoc && (
        <div className="modal-overlay" onClick={() => !isSubmitting && setDeletingDoc(null)}>
          <div className="modal-card" onClick={e => e.stopPropagation()} style={{ maxWidth: 420 }}>
            <div className="modal-header">
              <h2>Remove Document</h2>
              <button className="close-btn" onClick={() => !isSubmitting && setDeletingDoc(null)} disabled={isSubmitting}>
                <X size={18} />
              </button>
            </div>
            <div className="modal-body">
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.95rem' }}>
                Are you sure you want to remove <strong>{deletingDoc.title || deletingDoc.fileName}</strong>? This will delete the document metadata, extracted text content, and stored file.
              </p>
            </div>
            <div className="modal-footer">
              <button className="btn btn-soft" onClick={() => setDeletingDoc(null)} disabled={isSubmitting}>
                Cancel
              </button>
              <button className="btn btn-danger" onClick={executeDelete} disabled={isSubmitting}>
                {isSubmitting ? <Loader2 size={16} className="animate-spin" /> : 'Delete'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* KNOWLEDGE SEARCH & RAG Q&A MODAL (STEP 4.7 & 4.8) */}
      {isSearchModalOpen && (
        <div className="modal-overlay" onClick={() => !isSearchingSemantic && !isAskingRag && setIsSearchModalOpen(false)}>
          <div className="modal-card" onClick={e => e.stopPropagation()} style={{ maxWidth: 660 }}>
            <div className="modal-header">
              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <Sparkles size={20} style={{ color: 'var(--accent-purple)' }} />
                <h2>Knowledge Assistant & Semantic Retrieval</h2>
              </div>
              <button className="close-btn" onClick={() => setIsSearchModalOpen(false)} disabled={isSearchingSemantic || isAskingRag}>
                <X size={18} />
              </button>
            </div>

            {/* Mode Switcher Tabs */}
            <div style={{ display: 'flex', gap: 8, padding: '0 24px', borderBottom: '1px solid var(--border-color)', marginBottom: 16 }}>
              <button
                type="button"
                className={`task-tab ${knowledgeMode === 'RAG' ? 'task-tab-active' : ''}`}
                onClick={() => setKnowledgeMode('RAG')}
                style={{ padding: '8px 16px', fontSize: '0.86rem' }}
              >
                <Sparkles size={14} style={{ marginRight: 6 }} /> Grounded Q&A (RAG)
              </button>
              <button
                type="button"
                className={`task-tab ${knowledgeMode === 'SEARCH' ? 'task-tab-active' : ''}`}
                onClick={() => setKnowledgeMode('SEARCH')}
                style={{ padding: '8px 16px', fontSize: '0.86rem' }}
              >
                <Search size={14} style={{ marginRight: 6 }} /> Semantic Chunks Search
              </button>
            </div>

            <form onSubmit={knowledgeMode === 'RAG' ? handleExecuteRagAsk : handleExecuteSemanticSearch}>
              <div className="modal-body" style={{ paddingTop: 0 }}>
                <p style={{ color: 'var(--text-secondary)', fontSize: '0.86rem', marginBottom: 14 }}>
                  {knowledgeMode === 'RAG'
                    ? 'Ask questions to your knowledge base. Answers are generated by the LLM strictly grounded in retrieved document chunks.'
                    : 'Search your knowledge base by meaning and inspect ranked vector similarity chunks.'}
                </p>

                {/* Query Input */}
                <div className="form-group" style={{ marginBottom: 14 }}>
                  <label className="form-label" htmlFor="semantic-search-input">
                    {knowledgeMode === 'RAG' ? 'Ask a Question *' : 'Search Concept / Query *'}
                  </label>
                  <div style={{ display: 'flex', gap: 8 }}>
                    <input
                      id="semantic-search-input"
                      type="text"
                      className="form-input"
                      placeholder={knowledgeMode === 'RAG' ? 'e.g. What is normalization and why is it useful?' : 'e.g. How can I reduce duplicate data?'}
                      value={semanticQuery}
                      onChange={e => setSemanticQuery(e.target.value)}
                      required
                      autoFocus
                    />
                    <select
                      className="form-input"
                      style={{ width: 110 }}
                      value={semanticTopK}
                      onChange={e => setSemanticTopK(Number(e.target.value))}
                      title="Top-K Context Chunks"
                    >
                      <option value={3}>Top 3</option>
                      <option value={5}>Top 5</option>
                      <option value={8}>Top 8</option>
                      <option value={10}>Top 10</option>
                    </select>
                  </div>
                </div>

                <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: 16 }}>
                  <button
                    type="submit"
                    className="btn btn-primary btn-sm"
                    disabled={isSearchingSemantic || isAskingRag || !semanticQuery.trim()}
                  >
                    {isAskingRag ? (
                      <>
                        <Loader2 size={14} className="animate-spin" /> Synthesizing Answer...
                      </>
                    ) : isSearchingSemantic ? (
                      <>
                        <Loader2 size={14} className="animate-spin" /> Searching Vectors...
                      </>
                    ) : knowledgeMode === 'RAG' ? (
                      <>
                        <Sparkles size={14} /> Ask Knowledge Base
                      </>
                    ) : (
                      <>
                        <Search size={14} /> Search Knowledge
                      </>
                    )}
                  </button>
                </div>

                {/* Error Banner */}
                {semanticSearchError && (
                  <div className="form-error fade-in" style={{ marginBottom: 16 }}>
                    {semanticSearchError}
                  </div>
                )}

                {/* RAG GROUNDED ANSWER DISPLAY (STEP 4.8) */}
                {knowledgeMode === 'RAG' && ragAnswer && (
                  <div className="fade-in">
                    <div style={{
                      padding: '16px 18px',
                      background: 'var(--input-bg, rgba(255,255,255,0.03))',
                      border: '1px solid var(--border-color)',
                      borderRadius: 'var(--radius-md)',
                      marginBottom: 16
                    }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 10 }}>
                        <span style={{ fontSize: '0.82rem', fontWeight: 700, color: 'var(--accent-purple)', textTransform: 'uppercase' }}>
                          Grounded Answer
                        </span>
                        <span style={{ fontSize: '0.74rem', color: 'var(--text-muted)' }}>
                          Model: {ragAnswer.modelName}
                        </span>
                      </div>

                      <div style={{
                        fontSize: '0.92rem',
                        lineHeight: 1.65,
                        color: 'var(--text-primary)',
                        whiteSpace: 'pre-wrap'
                      }}>
                        {ragAnswer.answer}
                      </div>

                      {/* Source Citations */}
                      {ragAnswer.sources && ragAnswer.sources.length > 0 && (
                        <div style={{ marginTop: 14, paddingTop: 12, borderTop: '1px solid var(--border-color)' }}>
                          <span style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase', display: 'block', marginBottom: 6 }}>
                            Cited Sources ({ragAnswer.sources.length})
                          </span>
                          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
                            {ragAnswer.sources.map((src, i) => (
                              <span
                                key={src.chunkId || i}
                                style={{
                                  fontSize: '0.76rem',
                                  padding: '3px 9px',
                                  borderRadius: 12,
                                  background: 'rgba(99, 102, 241, 0.12)',
                                  border: '1px solid rgba(99, 102, 241, 0.25)',
                                  color: 'var(--text-secondary)'
                                }}
                              >
                                📄 {src.documentName} (Section {src.chunkIndex + 1}) · {(src.similarity * 100).toFixed(1)}% match
                              </span>
                            ))}
                          </div>
                        </div>
                      )}
                    </div>
                  </div>
                )}

                {/* SEMANTIC CHUNKS LIST (STEP 4.7) */}
                {knowledgeMode === 'SEARCH' && hasSearchedSemantic && !isSearchingSemantic && (
                  <div className="fade-in">
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
                      <span style={{ fontSize: '0.82rem', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase' }}>
                        Ranked Results ({semanticResults.length})
                      </span>
                    </div>

                    {semanticResults.length === 0 ? (
                      <div style={{
                        padding: 24,
                        textAlign: 'center',
                        background: 'var(--pastel-bg)',
                        border: '1px dashed var(--border-color)',
                        borderRadius: 'var(--radius-md)'
                      }}>
                        <p style={{ fontSize: '0.9rem', color: 'var(--text-secondary)', marginBottom: 4 }}>
                          No relevant information found in your knowledge base.
                        </p>
                        <span style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>
                          Try rephrasing your search or uploading more documents.
                        </span>
                      </div>
                    ) : (
                      <div style={{ display: 'flex', flexDirection: 'column', gap: 10, maxHeight: 320, overflowY: 'auto' }}>
                        {semanticResults.map((result, idx) => (
                          <div
                            key={result.chunkId || idx}
                            style={{
                              padding: '12px 14px',
                              background: 'var(--input-bg, rgba(255,255,255,0.03))',
                              border: '1px solid var(--border-color)',
                              borderRadius: 'var(--radius-md)'
                            }}
                          >
                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 6 }}>
                              <span style={{ fontSize: '0.82rem', fontWeight: 600, color: 'var(--text-primary)' }}>
                                #{idx + 1} · {result.documentName} (Section {result.chunkIndex + 1})
                              </span>
                              <span
                                style={{
                                  fontSize: '0.75rem',
                                  fontWeight: 700,
                                  padding: '2px 8px',
                                  borderRadius: 12,
                                  background: 'rgba(99, 102, 241, 0.15)',
                                  color: 'var(--accent-purple)'
                                }}
                              >
                                {(result.similarity * 100).toFixed(1)}% match
                              </span>
                            </div>
                            <p style={{
                              fontSize: '0.85rem',
                              color: 'var(--text-secondary)',
                              lineHeight: 1.55,
                              whiteSpace: 'pre-wrap',
                              wordBreak: 'break-word',
                              margin: 0
                            }}>
                              {result.content}
                            </p>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                )}
              </div>

              <div className="modal-footer">
                <button
                  type="button"
                  className="btn btn-soft"
                  onClick={() => setIsSearchModalOpen(false)}
                >
                  Close
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}


