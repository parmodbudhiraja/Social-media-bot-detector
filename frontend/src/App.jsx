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
    <div className="min-h-screen bg-neutral-900 text-white font-sans flex items-center justify-center p-6 bg-[radial-gradient(ellipse_at_top_right,_var(--tw-gradient-stops))] from-indigo-950 via-neutral-900 to-black">
      <div className="w-full max-w-3xl bg-white/5 backdrop-blur-2xl border border-white/10 rounded-[2.5rem] p-12 shadow-[0_0_80px_rgba(79,70,229,0.1)] transition-all">
        
        {/* Hero Header */}
        <div className="text-center mb-12">
          <div className="inline-block px-4 py-1.5 rounded-full bg-indigo-500/10 border border-indigo-500/20 text-indigo-400 text-[10px] font-bold uppercase tracking-widest mb-6">
            AI-Powered Analysis
          </div>
          <h1 className="text-5xl md:text-6xl font-black tracking-tight mb-4 bg-clip-text text-transparent bg-gradient-to-b from-white to-neutral-500">
            Bot Detector
          </h1>
          <p className="text-neutral-400 text-lg max-w-md mx-auto leading-relaxed">
            Unmask fake engagement using advanced deep learning orchestration and behavioral analysis.
          </p>
        </div>

        {/* Input Form */}
        <form onSubmit={handleSubmit} className="relative group mb-10">
          <div className="absolute -inset-1 bg-gradient-to-r from-indigo-500 to-purple-600 rounded-[2rem] blur opacity-25 group-hover:opacity-40 transition duration-1000"></div>
          <div className="relative">
            <input 
              type="url" 
              placeholder="Paste Instagram Post URL here..." 
              value={url}
              onChange={(e) => setUrl(e.target.value)}
              disabled={loading || (jobId && status !== 'COMPLETED' && status !== 'FAILED')}
              className="w-full bg-black/60 border border-white/10 rounded-3xl py-6 px-8 text-xl text-white placeholder-neutral-600 focus:outline-none focus:ring-2 focus:ring-indigo-500 transition disabled:opacity-50"
              required
            />
            <button 
              type="submit" 
              disabled={loading || (jobId && status !== 'COMPLETED' && status !== 'FAILED')}
              className="absolute right-3 top-3 bottom-3 bg-indigo-500 hover:bg-indigo-400 text-white font-black px-10 rounded-2xl transition duration-300 shadow-xl shadow-indigo-500/20 disabled:opacity-50 active:scale-95"
            >
              {loading ? 'Starting...' : 'Analyze'}
            </button>
          </div>
        </form>

        {error && (
          <div className="bg-red-500/10 border border-red-500/20 text-red-400 rounded-2xl p-4 text-center mb-8 animate-fade-in font-medium">
            {error}
          </div>
        )}

        {/* Progress & Results */}
        {(jobId || loading) && (
          <div className="space-y-8">
            <div className={`bg-black/40 rounded-[2rem] p-10 border border-white/5 relative overflow-hidden transition-all duration-500 ${status === 'COMPLETED' ? 'opacity-50 grayscale scale-95' : ''}`}>
               <div className="text-center mb-10">
                  <span className="text-[10px] uppercase tracking-[0.3em] text-indigo-400 font-black mb-3 block opacity-70">Live Pipeline Status</span>
                  <p className={`text-3xl font-light tracking-tight ${status === 'FAILED' ? 'text-red-400' : 'text-white'}`}>
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
