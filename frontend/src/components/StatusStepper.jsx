import React, { useState, useEffect } from 'react';

const steps = [
  { key: 'PENDING', label: 'Queued' },
  { key: 'SCRAPING', label: 'Scraping' },
  { key: 'INFERENCE', label: 'ML Inference' },
  { key: 'COMPLETED', label: 'Done' }
];

export function StatusStepper({ status }) {
  const currentIndex = steps.findIndex(s => s.key === status);
  const totalProgress = (Math.max(0, currentIndex) / (steps.length - 1)) * 100;
  
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
    <div className="w-full py-6">
      <div className="relative h-1 bg-[#F1F3F4] rounded-full mb-12">
        <div 
          className="absolute h-full bg-[#1A73E8] rounded-full transition-all duration-700"
          style={{ width: `${totalProgress}%` }}
        />
        <div className="absolute top-[-14px] w-full flex justify-between px-1">
          {steps.map((step, index) => {
            const isCompleted = currentIndex > index;
            const isActive = currentIndex === index;
            return (
              <div key={step.key} className="flex flex-col items-center">
                <div className={`w-8 h-8 rounded-full flex items-center justify-center text-xs font-bold transition-all duration-500 z-10 
                  ${isCompleted ? 'bg-[#1A73E8] text-white shadow-sm' : 
                    isActive ? 'bg-[#1A73E8] text-white ring-4 ring-[#E8F0FE] scale-110' : 
                    'bg-[#F8F9FA] text-[#80868B] border border-[#DADCE0]'}`}>
                  {isCompleted ? '✓' : index + 1}
                </div>
                <div className="flex flex-col items-center mt-4">
                    <span className={`text-xs font-medium transition-colors duration-300
                    ${isCompleted ? 'text-[#1A73E8]' : isActive ? 'text-[#202124]' : 'text-[#80868B]'}`}>
                    {step.label}
                    </span>
                    {isActive && status !== 'COMPLETED' && (
                        <span className="text-[10px] text-[#5F6368] mt-1 font-medium">
                            {stageProgress}%
                        </span>
                    )}
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}
