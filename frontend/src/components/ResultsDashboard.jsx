import React, { useMemo } from 'react';

export function ResultsDashboard({ jobId, result }) {

  const { categories } = useMemo(() => {
    const defaultCategories = {
      fake_100: { 
        title: "100% fake", 
        desc: "Both models say fake", 
        data: [] 
      },
      maybe_fake: { 
        title: "maybe fake", 
        desc: "ML model says fake and AI model says real", 
        data: [] 
      },
      maybe_real: { 
        title: "maybe real", 
        desc: "ML model says real and AI model says fake", 
        data: [] 
      },
      real_100: { 
        title: "100% real", 
        desc: "Both models say real", 
        data: [] 
      }
    };

    if (!result?.result) return { categories: defaultCategories };

    try {
      const parsed = JSON.parse(result.result);

      for (let i = 0; i < parsed.usernames.length; i++) {
        const x = parsed.x_predictions[i]; // ML Model (0=Real, 1=Fake)
        const y = parsed.y_predictions[i]; // AI Model (0=Real, 1=Fake)
        const username = parsed.usernames[i];

        let category = '';
        if (x === 1 && y === 1) category = 'fake_100';
        else if (x === 1 && y === 0) category = 'maybe_fake';
        else if (x === 0 && y === 1) category = 'maybe_real';
        else if (x === 0 && y === 0) category = 'real_100';

        if (category) defaultCategories[category].data.push(username);
      }
      return { categories: defaultCategories };
    } catch (e) {
      console.error("Failed to parse results:", e);
      return { categories: defaultCategories };
    }
  }, [result]);

  const downloadTxtFile = (categoryKey, title) => {
    const usernames = categories[categoryKey].data.join('\n');
    const blob = new Blob([usernames], { type: 'text/plain' });
    const url = URL.createObjectURL(blob);
    
    const a = document.createElement('a');
    a.href = url;
    a.download = `${title.replace(/\s+/g, '_')}_users.txt`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  };

  const totalAnalyzed = Object.values(categories).reduce((acc, cat) => acc + cat.data.length, 0);

  if (totalAnalyzed === 0) return null;

  return (
    <div className="mt-8 animate-fade-in pb-4">
      <div className="flex flex-col md:flex-row justify-between items-start md:items-end mb-6 gap-4 border-b border-[#DADCE0] pb-4">
        <div>
          <h2 className="text-2xl font-normal text-[#202124] tracking-tight">Analysis Report</h2>
          <p className="text-[#5F6368] text-sm mt-1">Metrics extracted and categorized via AI modeling</p>
        </div>
        <div className="px-4 py-2 text-right">
          <p className="text-[11px] uppercase tracking-wider text-[#5F6368] mb-1 font-bold">Profiles Processed</p>
          <p className="text-3xl font-normal text-[#1A73E8] leading-none">{totalAnalyzed}</p>
        </div>
      </div>

      <div className="bg-white border border-[#DADCE0] rounded-lg overflow-hidden mb-8 shadow-sm">
        <table className="w-full border-collapse text-left">
          <thead className="bg-[#F8F9FA] border-b border-[#DADCE0]">
            <tr className="text-xs font-medium text-[#5F6368]">
              <th className="px-6 py-4">Category</th>
              <th className="px-6 py-4 text-center w-32 border-l border-[#DADCE0]">Value</th>
              <th className="px-6 py-4 text-center w-48 border-l border-[#DADCE0]">Export</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-[#DADCE0]">
            {Object.entries(categories).map(([key, cat]) => (
              <tr key={key} className="hover:bg-[#F8F9FA] transition-colors">
                <td className="px-6 py-5">
                  <div className="text-[15px] font-medium text-[#202124] capitalize">{cat.title}</div>
                </td>
                <td className="px-6 py-5 text-center border-l border-[#DADCE0]">
                  <span className="inline-block px-3 py-1 bg-[#F1F3F4] rounded-full text-sm font-medium text-[#3C4043]">
                    {cat.data.length}
                  </span>
                </td>
                <td className="px-6 py-5 text-center border-l border-[#DADCE0]">
                  <button 
                    onClick={() => downloadTxtFile(key, cat.title)}
                    disabled={cat.data.length === 0}
                    className="inline-flex items-center justify-center gap-2 text-[#1A73E8] hover:bg-[#F1F3F4] disabled:text-[#9AA0A6] disabled:hover:bg-transparent text-sm font-medium py-2 px-5 rounded-full transition w-full whitespace-nowrap"
                  >
                    <svg xmlns="http://www.w3.org/2000/svg" className="h-[18px] w-[18px]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
                    </svg>
                    Download .txt
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="flex justify-end pt-4">
        <button 
          onClick={() => window.location.reload()}
          className="text-[#1A73E8] hover:bg-[#F8F9FA] px-6 py-2.5 rounded-full transition border border-[#DADCE0] text-sm font-medium"
        >
          Analyze Another URL
        </button>
      </div>
    </div>
  );
}
