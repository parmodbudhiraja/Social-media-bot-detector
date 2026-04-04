import React, { useState, useEffect } from 'react';

const steps = [
  { key: 'PENDING', label: 'Queuing' },
  { key: 'SCRAPING', label: 'Scraping' },
  { key: 'INFERENCE', label: 'Analyzing' }
];

export function StatusStepper({ status }) {
  const currentIndex = steps.findIndex(s => s.key === status);
  const [stageProgress, setStageProgress] = useState(0);

  useEffect(() => {
    if (status === 'COMPLETED') {
      setStageProgress(100);
      return;
    }
    
    setStageProgress(0);
    const interval = setInterval(() => {
      setStageProgress(prev => {
        if (prev < 80) return prev + Math.floor(Math.random() * 10) + 2;
        if (prev < 99) return prev + 1;
        return prev;
      });
    }, 1000);
    
    return () => clearInterval(interval);
  }, [status]);
  
  return (
    <ul className="vertical-stepper">
      {steps.map((step, index) => {
        const isCompleted = currentIndex > index || status === 'COMPLETED';
        const isActive = currentIndex === index && status !== 'COMPLETED';

        return (
          <li key={step.key} style={{ marginBottom: '20px', listStyle: 'none' }}>
            <div style={{ fontWeight: 500, marginBottom: '6px', color: isCompleted ? '#386A20' : (isActive ? '#202124' : '#79747E'), fontSize: '0.95rem' }}>
              {step.label}
            </div>
            <div style={{ width: '100%', backgroundColor: '#E0E0E0', borderRadius: '6px', height: '10px', overflow: 'hidden' }}>
                <div 
                  style={{ 
                    width: isCompleted ? '100%' : (isActive ? `${stageProgress}%` : '0%'),
                    backgroundColor: '#386A20',
                    height: '100%',
                    borderRadius: '6px',
                    transition: 'width 0.5s ease'
                  }}
                ></div>
              </div>
          </li>
        );
      })}
    </ul>
  );
}
