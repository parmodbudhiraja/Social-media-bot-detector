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
    <div className="min-h-screen bg-[#F8F9FA] text-[#202124] font-sans flex items-center justify-center p-6 selection:bg-[#1A73E8] selection:text-white">
      <div className="w-full max-w-4xl bg-white rounded-[24px] p-10 md:p-14 shadow-[0_4px_6px_rgba(60,64,67,0.1),_0_1px_3px_rgba(60,64,67,0.15)] border border-[#DADCE0]">
        
        {/* Google Workspace Style Header */}
        <div className="text-center mb-10">
          <svg className="w-12 h-12 mx-auto mb-6 text-[#1A73E8]" fill="currentColor" viewBox="0 0 24 24">
             <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-1 17.93c-3.95-.49-7-3.85-7-7.93 0-.62.08-1.21.21-1.79L9 15v1c0 1.1.9 2 2 2v1.93zm6.9-2.54c-.26-.81-1-1.39-1.9-1.39h-1v-3c0-.55-.45-1-1-1H8v-2h2c.55 0 1-.45 1-1V7h2c1.1 0 2-.9 2-2v-.41c2.93 1.19 5 4.06 5 7.41 0 2.08-.8 3.97-2.1 5.39z"/>
          </svg>
          <h1 className="text-3xl md:text-4xl font-normal tracking-tight mb-3 text-[#202124]">
            AI-based Fake engagement detector
          </h1>
          <p className="text-[#5F6368] text-base max-w-lg mx-auto leading-relaxed">
            Unmask fake engagement using advanced deep learning orchestration and behavioral analysis.
          </p>
        </div>

        {/* Search Input Bar (Google Style) */}
        <form onSubmit={handleSubmit} className="mb-10 w-full max-w-2xl mx-auto">
          <div className="relative flex items-center shadow-[0_1px_2px_0_rgba(60,64,67,0.3),_0_1px_3px_1px_rgba(60,64,67,0.15)] bg-white rounded-full transition-shadow hover:shadow-[0_1px_3px_0_rgba(60,64,67,0.3),_0_4px_8px_3px_rgba(60,64,67,0.15)] focus-within:shadow-[0_1px_3px_0_rgba(60,64,67,0.3),_0_4px_8px_3px_rgba(60,64,67,0.15)]">
            <div className="pl-6 text-[#9AA0A6]">
              <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1" />
              </svg>
            </div>
            <input 
              type="url" 
              placeholder="Paste Instagram Post URL here" 
              value={url}
              onChange={(e) => setUrl(e.target.value)}
              disabled={loading || (jobId && status !== 'COMPLETED' && status !== 'FAILED')}
              className="w-full bg-transparent py-4 px-4 text-base text-[#202124] placeholder-[#9AA0A6] focus:outline-none disabled:opacity-50 font-normal"
              required
            />
            <div className="pr-2 py-2">
              <button 
                type="submit" 
                disabled={loading || (jobId && status !== 'COMPLETED' && status !== 'FAILED')}
                className="bg-[#1A73E8] hover:bg-[#1557B0] text-white font-medium px-8 py-2.5 rounded-full transition duration-200 disabled:opacity-50 disabled:bg-[#DADCE0] disabled:text-[#80868B] active:bg-[#174EA6]"
              >
                {loading ? 'Starting...' : 'Analyze'}
              </button>
            </div>
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
          <div className="space-y-8 animate-fade-in">
            <div className={`bg-white rounded-2xl p-8 border border-[#DADCE0] shadow-sm transition-all duration-500 ${status === 'COMPLETED' ? 'opacity-0 h-0 p-0 overflow-hidden border-none m-0' : ''}`}>
               <div className="text-center mb-10">
                  <span className="text-xs uppercase tracking-[0.2em] text-[#5F6368] font-bold mb-2 block">Live Pipeline Status</span>
                  <p className={`text-2xl font-normal ${status === 'FAILED' ? 'text-[#C5221F]' : 'text-[#202124]'}`}>
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
    </div>
  );
}

export default App;
