import React, { useMemo } from 'react';

const API_BASE = "/api/v1";

export function ResultsDashboard({ jobId, result }) {
  const data = useMemo(() => {
    if (!result?.result) return [];
    try {
      const parsed = JSON.parse(result.result);
      const rows = [];
      for (let i = 0; i < parsed.usernames.length; i++) {
        const x = parsed.x_predictions[i];
        const y = parsed.y_predictions[i];
        
        let verdict = "Real Account";
        let rating = "REAL";
        
        if (x === 1 && y === 1) {
          verdict = "Certain Bot";
          rating = "BOT";
        } else if (x === 1 || y === 1) {
          verdict = "Probable Bot";
          rating = "SUSPICIOUS";
        }
        
        rows.push({
          username: parsed.usernames[i],
          profileScore: x === 0 ? "Authentic" : "Fake Metrics",
          behaviorScore: y === 0 ? "Human-like" : "Bot-like",
          verdict,
          rating
        });
      }
      return rows;
    } catch (e) {
      console.error("Failed to parse results:", e);
      return [];
    }
  }, [result]);

  const stats = useMemo(() => {
    const total = data.length;
    const bots = data.filter(d => d.rating === 'BOT').length;
    const suspicious = data.filter(d => d.rating === 'SUSPICIOUS').length;
    return { total, bots, suspicious };
  }, [data]);

  if (!data.length) return null;

  return (
    <div className="mt-10 animate-fade-in">
      <div className="grid grid-cols-3 gap-4 mb-8">
        <div className="bg-white/5 border border-white/10 rounded-2xl p-4 text-center">
          <p className="text-xs uppercase tracking-widest text-neutral-500 mb-1">Total Users</p>
          <p className="text-2xl font-bold">{stats.total}</p>
        </div>
        <div className="bg-red-500/5 border border-red-500/20 rounded-2xl p-4 text-center">
          <p className="text-xs uppercase tracking-widest text-red-400/60 mb-1">Bots Detected</p>
          <p className="text-2xl font-bold text-red-400">{stats.bots}</p>
        </div>
        <div className="bg-orange-500/5 border border-orange-500/20 rounded-2xl p-4 text-center">
          <p className="text-xs uppercase tracking-widest text-orange-400/60 mb-1">Suspicious</p>
          <p className="text-2xl font-bold text-orange-400">{stats.suspicious}</p>
        </div>
      </div>

      <div className="bg-black/40 border border-white/5 rounded-2xl overflow-hidden mb-8">
        <table className="w-full text-left border-collapse">
          <thead>
            <tr className="bg-white/5 text-[10px] uppercase tracking-widest text-neutral-400">
              <th className="px-6 py-4 font-semibold">User</th>
              <th className="px-6 py-4 font-semibold">Profile</th>
              <th className="px-6 py-4 font-semibold">Behavior</th>
              <th className="px-6 py-4 font-semibold text-right">Verdict</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-white/5">
            {data.slice(0, 10).map((row, i) => (
              <tr key={i} className="hover:bg-white/5 transition-colors group">
                <td className="px-6 py-4 text-sm font-medium text-white">{row.username}</td>
                <td className="px-6 py-4 text-xs text-neutral-400">{row.profileScore}</td>
                <td className="px-6 py-4 text-xs text-neutral-400">{row.behaviorScore}</td>
                <td className="px-6 py-4 text-right">
                  <span className={`px-3 py-1 rounded-full text-[10px] font-bold uppercase tracking-tight
                    ${row.rating === 'BOT' ? 'bg-red-500/20 text-red-400' : 
                      row.rating === 'SUSPICIOUS' ? 'bg-orange-500/20 text-orange-400' : 
                      'bg-emerald-500/20 text-emerald-400'}`}>
                    {row.verdict}
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {data.length > 10 && (
          <div className="p-4 text-center text-xs text-neutral-500 italic border-t border-white/5">
            + {data.length - 10} more users analyzed.
          </div>
        )}
      </div>

      <div className="flex gap-4">
        <a 
          href={`${API_BASE}/jobs/${jobId}/download`}
          className="flex-1 bg-indigo-500 hover:bg-indigo-400 text-white font-bold py-4 rounded-2xl transition shadow-lg shadow-indigo-500/20 text-center"
        >
          Download Full CSV Report
        </a>
        <button 
          onClick={() => window.location.reload()}
          className="bg-white/5 hover:bg-white/10 text-neutral-400 px-6 rounded-2xl transition border border-white/10"
        >
          Reset
        </button>
      </div>
    </div>
  );
}
