'use client';

import React, { useState, useEffect, useMemo } from 'react';
import { Bot, Sparkles, Plus, Loader2, X, AlertTriangle, Calendar, Clock, Folder } from 'lucide-react';
import { analyzeTaskWithAi, type AiTaskSuggestionResponse, type TaskSuggestionItem } from '@/lib/taskApi';

interface AiTaskAssistantModalProps {
  isOpen: boolean;
  onClose: () => void;
  onTasksGenerated: (tasks: TaskSuggestionItem[], targetDate: string, category: string) => Promise<void> | void;
  initialDate?: string;
  initialPrompt?: string;
}

const QUICK_PROMPTS = [
  {
    icon: '💼',
    label: 'Java Interview Preparation',
    prompt: 'Break down my Java interview preparation for tomorrow'
  },
  {
    icon: '🧪',
    label: 'Machine Learning Lab Report',
    prompt: 'Prepare Machine Learning Lab Report and evaluation metrics'
  },
  {
    icon: '🚀',
    label: 'Launch Full-Stack Website',
    prompt: 'Plan tasks for launching a new full-stack website and API'
  },
  {
    icon: '📚',
    label: 'Final Exam Study Sprint',
    prompt: 'High-yield exam study plan with past papers and active recall'
  },
  {
    icon: '🏃',
    label: 'Fitness & Strength Routine',
    prompt: 'Weekly strength and conditioning fitness routine with recovery'
  },
  {
    icon: '✈️',
    label: 'Weekend Trip Planning',
    prompt: 'Estimate steps and logistics for planning a weekend trip'
  }
];

export default function AiTaskAssistantModal({
  isOpen,
  onClose,
  onTasksGenerated,
  initialDate,
  initialPrompt = ''
}: AiTaskAssistantModalProps) {
  const [prompt, setPrompt] = useState(initialPrompt);
  const [targetDate, setTargetDate] = useState(initialDate || '2026-09-19');
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [isAdding, setIsAdding] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<AiTaskSuggestionResponse | null>(null);
  const [selectedIndices, setSelectedIndices] = useState<number[]>([]);
  const [chosenCategory, setChosenCategory] = useState('Academics & Studies');

  useEffect(() => {
    if (isOpen) {
      if (initialDate) setTargetDate(initialDate);
      if (initialPrompt) setPrompt(initialPrompt);
      setError(null);
    }
  }, [isOpen, initialDate, initialPrompt]);

  const handleAnalyze = async (overridePrompt?: string) => {
    const q = (overridePrompt || prompt).trim();
    if (!q) return;
    setIsAnalyzing(true);
    setError(null);
    setResult(null);

    try {
      const data = await analyzeTaskWithAi(q);
      setResult(data);
      if (data && data.suggestedTasks) {
        // Select all by default
        setSelectedIndices(data.suggestedTasks.map((_, i) => i));
        // Auto-select category if suggested
        const firstCategory = data.suggestedTasks.find(t => t.suggestedCategory)?.suggestedCategory;
        if (firstCategory) {
          setChosenCategory(firstCategory);
        }
      }
    } catch (err: any) {
      setError(err.message || 'Failed to analyze task with AI.');
    } finally {
      setIsAnalyzing(false);
    }
  };

  const handlePromptClick = (p: string) => {
    setPrompt(p);
    handleAnalyze(p);
  };

  const toggleTaskSelection = (index: number) => {
    setSelectedIndices(prev =>
      prev.includes(index) ? prev.filter(i => i !== index) : [...prev, index]
    );
  };

  const handleToggleSelectAll = () => {
    if (!result || !result.suggestedTasks) return;
    if (selectedIndices.length === result.suggestedTasks.length) {
      setSelectedIndices([]);
    } else {
      setSelectedIndices(result.suggestedTasks.map((_, i) => i));
    }
  };

  const totalEstimatedMinutes = useMemo(() => {
    if (!result || !result.suggestedTasks) return 0;
    return selectedIndices.reduce((acc, idx) => {
      const t = result.suggestedTasks[idx];
      return acc + (t?.estimatedMinutes || 30);
    }, 0);
  }, [result, selectedIndices]);

  const formatTotalTime = (mins: number) => {
    const h = Math.floor(mins / 60);
    const m = mins % 60;
    if (h > 0 && m > 0) return `${h}h ${m}m`;
    if (h > 0) return `${h}h`;
    return `${m}m`;
  };

  const handleCommitTasks = async () => {
    if (!result || !result.suggestedTasks || selectedIndices.length === 0) return;
    setIsAdding(true);
    setError(null);

    try {
      const tasksToSubmit = selectedIndices.map(i => result.suggestedTasks[i]);
      await onTasksGenerated(tasksToSubmit, targetDate, chosenCategory);
      onClose();
      // Reset state
      setResult(null);
      setPrompt('');
    } catch (err: any) {
      setError(err.message || 'Failed to schedule generated tasks.');
    } finally {
      setIsAdding(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="modal-overlay" onClick={() => !isAnalyzing && !isAdding && onClose()}>
      <div className="modal-content" style={{ maxWidth: 640 }} onClick={e => e.stopPropagation()}>
        {/* Header */}
        <div className="modal-header" style={{ borderBottom: '1px solid #f1ece1' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <div style={{
              background: 'linear-gradient(135deg, #8b5cf6, #7c3aed)',
              color: 'white',
              padding: '6px 8px',
              borderRadius: 8,
              boxShadow: '0 2px 8px rgba(139, 92, 246, 0.25)',
              display: 'flex',
              alignItems: 'center'
            }}>
              <Sparkles size={18} />
            </div>
            <div>
              <h2 className="modal-title" style={{ fontSize: '1.15rem', fontWeight: 700 }}>
                AI Productivity Copilot
              </h2>
              <span style={{ fontSize: '0.78rem', color: 'var(--text-secondary)' }}>
                Natural Language Goal Decomposition & Smart Scheduling
              </span>
            </div>
          </div>
          <button
            className="close-btn"
            onClick={onClose}
            disabled={isAnalyzing || isAdding}
            style={{ padding: 4 }}
          >
            <X size={18} />
          </button>
        </div>

        <div className="modal-body" style={{ padding: '20px 24px' }}>
          {!result ? (
            <>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.88rem', lineHeight: 1.5, marginBottom: 14 }}>
                Describe any goal, study plan, or deliverable. The AI will decompose it into prioritized, actionable tasks ready for your calendar.
              </p>

              {/* Target Date Picker */}
              <div style={{ marginBottom: 14, background: '#fcfaf7', padding: '10px 14px', borderRadius: 10, border: '1px solid #f1ece1' }}>
                <span style={{ fontSize: '0.8rem', fontWeight: 700, color: 'var(--text-primary)', display: 'block', marginBottom: 6 }}>
                  Schedule for Date:
                </span>
                <div style={{ display: 'flex', gap: 8 }}>
                  <button
                    type="button"
                    className={`btn btn-sm ${targetDate === '2026-09-19' ? 'btn-primary' : 'btn-soft'}`}
                    onClick={() => setTargetDate('2026-09-19')}
                  >
                    <Calendar size={13} /> Today (Sep 19)
                  </button>
                  <button
                    type="button"
                    className={`btn btn-sm ${targetDate === '2026-09-20' ? 'btn-primary' : 'btn-soft'}`}
                    onClick={() => setTargetDate('2026-09-20')}
                  >
                    <Calendar size={13} /> Tomorrow (Sep 20)
                  </button>
                </div>
              </div>

              {/* Goal Input Textarea */}
              <div className="form-group" style={{ marginBottom: 14 }}>
                <label className="form-label" style={{ fontSize: '0.82rem', fontWeight: 700 }}>
                  What do you want to accomplish? *
                </label>
                <textarea
                  className="form-input"
                  style={{ minHeight: 100, resize: 'vertical', fontSize: '0.88rem', padding: '10px 12px' }}
                  placeholder="e.g. Prepare for my Java technical interview tomorrow covering OOP, DSA and Mock questions..."
                  value={prompt}
                  onChange={e => setPrompt(e.target.value)}
                  disabled={isAnalyzing}
                  autoFocus
                />
              </div>

              {error && (
                <div className="error-alert" style={{ marginBottom: 16 }}>
                  <AlertTriangle size={16} />
                  <span>{error}</span>
                </div>
              )}

              <button
                type="button"
                className="btn btn-primary"
                style={{ width: '100%', justifyContent: 'center', marginBottom: 20, padding: '10px 16px', fontSize: '0.9rem' }}
                onClick={() => handleAnalyze()}
                disabled={!prompt.trim() || isAnalyzing}
              >
                {isAnalyzing ? (
                  <>
                    <Loader2 size={16} className="spinning" />
                    Analyzing & Structuring Plan...
                  </>
                ) : (
                  <>
                    <Sparkles size={16} />
                    Generate Actionable Plan
                  </>
                )}
              </button>

              {/* Quick Prompt Pills */}
              <div>
                <p style={{ fontSize: '0.78rem', fontWeight: 700, color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '0.5px', marginBottom: 8 }}>
                  Or Choose a One-Click Scenario:
                </p>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))', gap: 8 }}>
                  {QUICK_PROMPTS.map((qp, idx) => (
                    <button
                      key={idx}
                      type="button"
                      className="btn btn-soft btn-sm"
                      onClick={() => handlePromptClick(qp.prompt)}
                      disabled={isAnalyzing}
                      style={{ justifyContent: 'flex-start', textAlign: 'left', padding: '8px 12px', borderRadius: 8, fontSize: '0.8rem' }}
                    >
                      <span style={{ marginRight: 6 }}>{qp.icon}</span>
                      <span style={{ fontWeight: 600 }}>{qp.label}</span>
                    </button>
                  ))}
                </div>
              </div>
            </>
          ) : (
            <>
              {/* Summary Banner */}
              <div style={{
                background: 'linear-gradient(135deg, rgba(245, 243, 255, 0.8), rgba(253, 244, 255, 0.8))',
                border: '1px solid rgba(192, 132, 252, 0.3)',
                padding: '14px 16px',
                borderRadius: 10,
                marginBottom: 16
              }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 4 }}>
                  <Sparkles size={16} color="var(--accent-purple)" />
                  <span style={{ fontSize: '0.88rem', fontWeight: 700, color: '#5b21b6' }}>
                    AI Plan Summary
                  </span>
                </div>
                <p style={{ fontSize: '0.85rem', color: 'var(--text-primary)', margin: 0, lineHeight: 1.5 }}>
                  {result.analysisSummary}
                </p>
              </div>

              {/* Schedule Controls Header */}
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 10, flexWrap: 'wrap', gap: 8 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                  <span style={{ fontSize: '0.86rem', fontWeight: 700, color: 'var(--text-primary)' }}>
                    Suggested Tasks ({selectedIndices.length} of {result.suggestedTasks.length} selected)
                  </span>
                  <button
                    type="button"
                    className="link-btn"
                    style={{ fontSize: '0.78rem' }}
                    onClick={handleToggleSelectAll}
                  >
                    {selectedIndices.length === result.suggestedTasks.length ? 'Deselect All' : 'Select All'}
                  </button>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: '0.78rem', color: '#7c3aed', fontWeight: 600 }}>
                  <Clock size={13} />
                  <span>Total Time: {formatTotalTime(totalEstimatedMinutes)}</span>
                </div>
              </div>

              {/* Tasks Checklist List */}
              <div style={{
                display: 'flex',
                flexDirection: 'column',
                gap: 8,
                maxHeight: 280,
                overflowY: 'auto',
                paddingRight: 4,
                marginBottom: 16
              }}>
                {result.suggestedTasks.map((t, idx) => {
                  const isChecked = selectedIndices.includes(idx);
                  return (
                    <div
                      key={idx}
                      onClick={() => toggleTaskSelection(idx)}
                      style={{
                        display: 'flex',
                        alignItems: 'flex-start',
                        gap: 10,
                        padding: '10px 12px',
                        borderRadius: 8,
                        border: isChecked ? '1px solid #c4b5fd' : '1px solid #e2e8f0',
                        background: isChecked ? '#ffffff' : '#f8fafc',
                        cursor: 'pointer',
                        transition: 'all 0.15s ease'
                      }}
                    >
                      <input
                        type="checkbox"
                        checked={isChecked}
                        onChange={() => {}} // handled by parent div click
                        style={{ marginTop: 3, cursor: 'pointer', accentColor: '#7c3aed' }}
                      />
                      <div style={{ flex: 1, minWidth: 0 }}>
                        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 8, marginBottom: 2 }}>
                          <span style={{
                            fontSize: '0.88rem',
                            fontWeight: 600,
                            color: isChecked ? 'var(--text-primary)' : 'var(--text-secondary)',
                            textDecoration: isChecked ? 'none' : 'line-through'
                          }}>
                            {t.title}
                          </span>
                          <div style={{ display: 'flex', alignItems: 'center', gap: 6, flexShrink: 0 }}>
                            {t.estimatedMinutes && (
                              <span style={{ fontSize: '0.7rem', color: 'var(--text-secondary)', background: '#f1f5f9', padding: '1px 5px', borderRadius: 4 }}>
                                {t.estimatedMinutes}m
                              </span>
                            )}
                            <span style={{
                              fontSize: '0.68rem',
                              fontWeight: 700,
                              padding: '1px 6px',
                              borderRadius: 4,
                              background: t.priority === 'HIGH' ? '#fee2e2' : t.priority === 'MEDIUM' ? '#fef3c7' : '#e0e7ff',
                              color: t.priority === 'HIGH' ? '#dc2626' : t.priority === 'MEDIUM' ? '#d97706' : '#4f46e5'
                            }}>
                              {t.priority}
                            </span>
                          </div>
                        </div>
                        {t.description && (
                          <p style={{ margin: 0, fontSize: '0.8rem', color: 'var(--text-secondary)', lineHeight: 1.4 }}>
                            {t.description}
                          </p>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>

              {/* Target Date & Category Configuration */}
              <div style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
                gap: 12,
                background: '#fafaf9',
                border: '1px solid #e7e5e4',
                padding: '10px 14px',
                borderRadius: 8,
                marginBottom: 16
              }}>
                <div>
                  <label style={{ fontSize: '0.78rem', fontWeight: 700, color: 'var(--text-primary)', display: 'block', marginBottom: 4 }}>
                    <Calendar size={12} style={{ display: 'inline', marginRight: 4 }} /> Target Date
                  </label>
                  <select
                    className="form-input"
                    style={{ fontSize: '0.82rem', padding: '5px 8px' }}
                    value={targetDate}
                    onChange={e => setTargetDate(e.target.value)}
                  >
                    <option value="2026-09-19">Today (Sep 19, 2026)</option>
                    <option value="2026-09-20">Tomorrow (Sep 20, 2026)</option>
                  </select>
                </div>

                <div>
                  <label style={{ fontSize: '0.78rem', fontWeight: 700, color: 'var(--text-primary)', display: 'block', marginBottom: 4 }}>
                    <Folder size={12} style={{ display: 'inline', marginRight: 4 }} /> Category
                  </label>
                  <select
                    className="form-input"
                    style={{ fontSize: '0.82rem', padding: '5px 8px' }}
                    value={chosenCategory}
                    onChange={e => setChosenCategory(e.target.value)}
                  >
                    <option value="Academics & Studies">Academics & Studies</option>
                    <option value="Work & Projects">Work & Projects</option>
                    <option value="Research & AI">Research & AI</option>
                    <option value="Personal & Life">Personal & Life</option>
                    <option value="Health & Fitness">Health & Fitness</option>
                  </select>
                </div>
              </div>

              {error && (
                <div className="error-alert" style={{ marginBottom: 14 }}>
                  <AlertTriangle size={16} />
                  <span>{error}</span>
                </div>
              )}

              {/* Action Buttons */}
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 10 }}>
                <button
                  type="button"
                  className="btn btn-soft btn-sm"
                  onClick={() => setResult(null)}
                  disabled={isAdding}
                >
                  ← Edit Goal Prompt
                </button>
                <button
                  type="button"
                  className="btn btn-primary btn-sm"
                  disabled={selectedIndices.length === 0 || isAdding}
                  onClick={handleCommitTasks}
                  style={{ background: 'linear-gradient(135deg, #8b5cf6, #7c3aed)' }}
                >
                  {isAdding ? (
                    <>
                      <Loader2 size={14} className="spinning" />
                      Adding to PostgreSQL...
                    </>
                  ) : (
                    <>
                      <Plus size={14} />
                      Add {selectedIndices.length} Task{selectedIndices.length === 1 ? '' : 's'} to {targetDate === '2026-09-19' ? 'Today' : 'Tomorrow'}
                    </>
                  )}
                </button>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
