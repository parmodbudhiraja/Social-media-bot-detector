import { useState, useEffect } from 'react';

const API_BASE = "/api/v1";

export function useJobPoller(jobId) {
  const [status, setStatus] = useState('IDLE');
  const [result, setResult] = useState(null);

  useEffect(() => {
    if (!jobId) return;

    const eventSource = new EventSource(`${API_BASE}/jobs/stream/${jobId}`);

    eventSource.addEventListener("STATUS_UPDATE", (e) => {
      setStatus(e.data);
      if (e.data === 'COMPLETED' || e.data === 'FAILED') {
        eventSource.close();
        if (e.data === 'COMPLETED') {
           fetchJobDetails(jobId);
        }
      }
    });

    eventSource.onerror = (err) => {
      console.error("SSE Error:", err);
      eventSource.close();
    };

    return () => eventSource.close();
  }, [jobId]);

  const fetchJobDetails = async (id) => {
    try {
      const res = await fetch(`${API_BASE}/jobs/${id}`);
      const data = await res.json();
      setResult(data);
    } catch (err) {
      console.error("Failed to fetch job details:", err);
    }
  };

  return { status, result, setStatus };
}
