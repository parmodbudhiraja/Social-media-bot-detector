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

  const getBadgeClass = (key) => {
    if (key === 'fake_100') return 'badge badge-red';
    if (key === 'maybe_fake') return 'badge badge-amber';
    if (key === 'maybe_real') return 'badge badge-yellow';
    return 'badge badge-green';
  };

  const getId = (key) => {
    if (key === 'fake_100') return 'count-fake';
    if (key === 'maybe_fake') return 'count-maybe-fake';
    if (key === 'maybe_real') return 'count-maybe-real';
    return 'count-real_100'; // or count-real
  };

  if (totalAnalyzed === 0) return null;

  return (
    <div className="results-card animate-fade-in">
      <h2>Analysis Report</h2>
      <p className="subtitle">Metrics extracted and categorized via AI modeling ({totalAnalyzed} Profiles)</p>

      <table className="m3-table">
        <thead>
          <tr>
             <th>Category</th>
             <th>Number of users</th>
             <th>Extract the user IDs</th>
          </tr>
        </thead>
        <tbody>
          {Object.entries(categories).map(([key, cat]) => (
            <tr key={key}>
              <td>
                <span className={getBadgeClass(key)}>{cat.title}</span> 
              </td>
              <td id={getId(key)}>{cat.data.length}</td>
              <td>
                <button 
                  className="m3-tonal-button"
                  onClick={() => downloadTxtFile(key, cat.title)}
                  disabled={cat.data.length === 0}
                  style={{ opacity: cat.data.length === 0 ? 0.5 : 1, cursor: cat.data.length === 0 ? 'not-allowed' : 'pointer', fontSize: '0.85rem', padding: '6px 16px' }}
                >
                  Download
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      
      <button 
        className="m3-tonal-button"
        onClick={() => window.location.reload()}
      >
        Analyze Another URL
      </button>
    </div>
  );
}
