import React from 'react';

const steps = [
  { key: 'PENDING', label: 'Queued' },
  { key: 'SCRAPING', label: 'Scraping' },
  { key: 'INFERENCE', label: 'ML Inference' },
  { key: 'COMPLETED', label: 'Done' }
];

export function StatusStepper({ status }) {
  const currentIndex = steps.findIndex(s => s.key === status);
  const progress = (Math.max(0, currentIndex) / (steps.length - 1)) * 100;

  return (
    <div className="w-full py-6">
      <div className="relative h-1 bg-white/10 rounded-full mb-8">
        <div 
          className="absolute h-full bg-indigo-500 rounded-full transition-all duration-700 shadow-[0_0_15px_rgba(79,70,229,0.5)]"
          style={{ width: `${progress}%` }}
        />
        <div className="absolute top-[-14px] w-full flex justify-between px-1">
          {steps.map((step, index) => {
            const isCompleted = currentIndex > index;
            const isActive = currentIndex === index;
            return (
              <div key={step.key} className="flex flex-col items-center">
                <div className={`w-8 h-8 rounded-full flex items-center justify-center text-xs font-bold transition-all duration-500 z-10 
                  ${isCompleted ? 'bg-indigo-500 text-white shadow-lg' : 
                    isActive ? 'bg-indigo-400 text-white ring-4 ring-indigo-500/20 scale-110' : 
                    'bg-neutral-800 text-neutral-500 border border-neutral-700'}`}>
                  {isCompleted ? '✓' : index + 1}
                </div>
                <span className={`text-[10px] uppercase tracking-tighter mt-3 font-semibold transition-colors duration-300
                  ${isCompleted ? 'text-indigo-300' : isActive ? 'text-indigo-400' : 'text-neutral-600'}`}>
                  {step.label}
                </span>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}
