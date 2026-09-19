'use client';

import React, { useState } from 'react';
import { Bot, Sparkles, Plus, Loader2, X, AlertTriangle } from 'lucide-react';
import { analyzeTaskWithAi, AiTaskSuggestionResponse } from '@/lib/taskApi';

interface AiTaskAssistantModalProps {
  isOpen: boolean;
  onClose: () => void;
  onTasksGenerated: (tasks: any[]) => void;
}

export default function AiTaskAssistantModal({ isOpen, onClose, onTasksGenerated }: AiTaskAssistantModalProps) {
  const [prompt, setPrompt] = useState('');
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<AiTaskSuggestionResponse | null>(null);

  if (!isOpen) return null;

  const handleAnalyze = async () => {
    if (!prompt.trim()) return;
    setIsAnalyzing(true);
    setError(null);
    setResult(null);

    try {
      const data = await analyzeTaskWithAi(prompt);
      setResult(data);
    } catch (err: any) {
      setError(err.message || 'Failed to analyze task.');
    } finally {
      setIsAnalyzing(false);
    }
  };

  const handlePromptClick = (text: string) => {
    setPrompt(text);
  };

  const handleAddTasks = () => {
    if (result && result.suggestedTasks) {
      onTasksGenerated(result.suggestedTasks);
    }
  };

  return (
    <div className="modal-overlay">
      <div className="modal-content" style={{ maxWidth: 600 }}>
        <div className="modal-header">
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <div style={{ background: 'var(--brand-primary)', color: 'white', padding: 6, borderRadius: 6 }}>
              <Sparkles size={18} />
            </div>
            <h2 className="modal-title">AI Task Assistant</h2>
          </div>
          <button className="btn btn-ghost" onClick={onClose} style={{ padding: 4 }}>
            <X size={20} />
          </button>
        </div>

        <div className="modal-body">
          {!result ? (
            <>
              <p style={{ color: 'var(--text-muted)', marginBottom: 16 }}>
                Tell MindOS what you need to accomplish, and the AI will break it down into an actionable plan.
              </p>
              
              <div className="form-group">
                <textarea
                  className="form-input"
                  style={{ minHeight: 120, resize: 'vertical' }}
                  placeholder="I need to prepare for my Java interview next week..."
                  value={prompt}
                  onChange={(e) => setPrompt(e.target.value)}
                  disabled={isAnalyzing}
                />
              </div>

              {error && (
                <div className="error-alert" style={{ marginBottom: 16 }}>
                  <AlertTriangle size={16} />
                  <span>{error}</span>
                </div>
              )}

              <button 
                className="btn btn-primary" 
                style={{ width: '100%', justifyContent: 'center', marginBottom: 24 }}
                onClick={handleAnalyze}
                disabled={!prompt.trim() || isAnalyzing}
              >
                {isAnalyzing ? (
                  <>
                    <Loader2 size={16} className="spinner" />
                    Analyzing Task...
                  </>
                ) : (
                  <>
                    <Bot size={16} />
                    Analyze Task
                  </>
                )}
              </button>

              <div style={{ marginTop: 24 }}>
                <p style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: 12 }}>SUGGESTED PROMPTS:</p>
                <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                  <button className="btn btn-ghost" onClick={() => handlePromptClick('Break down my Java interview preparation')} style={{ justifyContent: 'flex-start', padding: '8px 12px' }}>
                    • Break down my Java interview preparation
                  </button>
                  <button className="btn btn-ghost" onClick={() => handlePromptClick('Help me prioritize my tasks for launching the new website')} style={{ justifyContent: 'flex-start', padding: '8px 12px' }}>
                    • Help me prioritize my tasks for launching the new website
                  </button>
                  <button className="btn btn-ghost" onClick={() => handlePromptClick('What should I work on next to improve my fitness?')} style={{ justifyContent: 'flex-start', padding: '8px 12px' }}>
                    • What should I work on next to improve my fitness?
                  </button>
                  <button className="btn btn-ghost" onClick={() => handlePromptClick('Estimate the time and steps for planning a weekend trip')} style={{ justifyContent: 'flex-start', padding: '8px 12px' }}>
                    • Estimate the time and steps for planning a weekend trip
                  </button>
                </div>
              </div>
            </>
          ) : (
            <>
              <div style={{ background: 'var(--pastel-bg)', padding: 16, borderRadius: 8, marginBottom: 20 }}>
                <h3 style={{ fontSize: '1rem', fontWeight: 600, marginBottom: 8, display: 'flex', alignItems: 'center', gap: 8 }}>
                  <Sparkles size={16} style={{ color: 'var(--brand-primary)' }} />
                  AI Analysis
                </h3>
                <p style={{ fontSize: '0.9rem', color: 'var(--text-muted)' }}>
                  {result.analysisSummary}
                </p>
              </div>

              <h4 style={{ fontSize: '0.9rem', fontWeight: 600, marginBottom: 12 }}>Suggested Tasks ({result.suggestedTasks.length})</h4>
              
              <div style={{ display: 'flex', flexDirection: 'column', gap: 12, maxHeight: 300, overflowY: 'auto', paddingRight: 8, marginBottom: 20 }}>
                {result.suggestedTasks.map((t, idx) => (
                  <div key={idx} style={{ border: '1px solid var(--border-color)', borderRadius: 8, padding: 12 }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 4 }}>
                      <h5 style={{ fontWeight: 600, fontSize: '0.95rem', margin: 0 }}>{t.title}</h5>
                      <span style={{ 
                        fontSize: '0.7rem', 
                        padding: '2px 6px', 
                        borderRadius: 4, 
                        fontWeight: 600,
                        background: t.priority === 'HIGH' ? '#fee2e2' : t.priority === 'MEDIUM' ? '#fef3c7' : '#e0e7ff',
                        color: t.priority === 'HIGH' ? '#dc2626' : t.priority === 'MEDIUM' ? '#d97706' : '#4f46e5'
                      }}>
                        {t.priority}
                      </span>
                    </div>
                    <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)', margin: 0 }}>
                      {t.description}
                    </p>
                  </div>
                ))}
              </div>

              <div className="modal-footer" style={{ borderTop: 'none', padding: 0, marginTop: 0 }}>
                <button className="btn btn-ghost" onClick={() => setResult(null)}>
                  Back
                </button>
                <button className="btn btn-primary" onClick={handleAddTasks}>
                  <Plus size={16} />
                  Add All to Dashboard
                </button>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
