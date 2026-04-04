import React, { useState } from 'react';
import axios from 'axios';
import { useJobPoller } from './hooks/useJobPoller';
import { StatusStepper } from './components/StatusStepper';
import { ResultsDashboard } from './components/ResultsDashboard';
import './index.css';

const API_BASE = "/api/v1";

const statusMessages = {
  IDLE: "Ready to scan.",
  PENDING: "Job queued successfully...",
  SCRAPING: "Extracting user comments and profiles...",
  INFERENCE: "Deep learning models analyzing patterns...",
  COMPLETED: "Analysis Complete!",
  FAILED: "Analysis failed. Please try again."
};

function App() {
  const [url, setUrl] = useState('');
  const [jobId, setJobId] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const { status, result, setStatus } = useJobPoller(jobId);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!url.includes("instagram.com")) {
      setError("Please enter a valid Instagram URL.");
      return;
    }
    setError(null);
    setLoading(true);

    try {
      const response = await axios.post(`${API_BASE}/jobs`, { url }, { timeout: 15000 });
      setJobId(response.data.jobId);
    } catch (err) {
      setError("Failed to initiate analysis. Check connection.");
      setStatus('FAILED');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="main-container m3-surface">
      <div className="text-center" style={{ marginBottom: '2.5rem' }}>
        <h1 className="text-3xl md:text-4xl font-normal tracking-tight mb-3 text-[#202124]">
          Fake Engagement Detector
        </h1>
        <p className="text-[#5F6368] text-base max-w-lg mx-auto leading-relaxed">
          Unmask the fake engagement in social media post using AI
        </p>
      </div>

      {/* Basic M3 Input Form */}
      <form onSubmit={handleSubmit} className="w-full max-w-2xl mx-auto" style={{ marginBottom: '3rem' }}>
        <div className="input-group">
          <input 
            type="url" 
            id="urlInput"
            placeholder="https://www.instagram.com/..." 
            value={url}
            onChange={(e) => setUrl(e.target.value)}
            disabled={loading || (jobId && status !== 'COMPLETED' && status !== 'FAILED')}
            className="m3-outlined-input"
            required
          />
          <button 
            type="submit" 
            disabled={loading || (jobId && status !== 'COMPLETED' && status !== 'FAILED')}
            className="m3-filled-button"
            style={{ width: '150px', flexShrink: 0 }}
          >
            {loading ? 'Starting...' : 'Analyze'}
          </button>
        </div>
      </form>

      {error && (
        <div className="bg-[#FCE8E6] text-[#C5221F] rounded-lg p-4 text-center mb-8 font-medium max-w-2xl mx-auto flex items-center justify-center gap-2">
          <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 24 24"><path d="M11 15h2v2h-2zm0-8h2v6h-2zm.99-5C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8z"/></svg>
          {error}
        </div>
      )}

      {/* Progress & Results */}
      {(jobId || loading) && (
        <div className="space-y-8 animate-fade-in text-center">
          <div className={`transition-all duration-500 ${status === 'COMPLETED' ? 'opacity-0 h-0 p-0 overflow-hidden m-0' : ''}`}>
             <div className="mb-8">
                <p className={`text-xl font-normal ${status === 'FAILED' ? 'text-[#C5221F]' : 'text-[#202124]'}`}>
                  {statusMessages[status]}
                </p>
             </div>

             {status !== 'IDLE' && status !== 'FAILED' && (
               <StatusStepper status={status} />
             )}
          </div>

          {status === 'COMPLETED' && result && (
             <ResultsDashboard jobId={jobId} result={result} />
          )}
        </div>
      )}

    </div>
  );
}

export default App;
