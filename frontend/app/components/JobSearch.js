'use client';

import { useState } from 'react';

export default function JobSearch({ initialJobs }) {
  const [query, setQuery] = useState('');

  const jobs = initialJobs.filter((job) => {
    const q = query.toLowerCase();
    if (!q) return true;
    return (
      job.title.toLowerCase().includes(q) ||
      job.location.toLowerCase().includes(q) ||
      job.type.toLowerCase().includes(q) ||
      (job.tags || []).some((t) => t.toLowerCase().includes(q))
    );
  });

  return (
    <div>
      {/* SEARCH BAR */}
      <div style={{ position: 'relative', marginBottom: 16 }}>
        <span style={{ position: 'absolute', left: 12, top: '50%', transform: 'translateY(-50%)', color: 'var(--muted)', fontSize: 14 }}>🔍</span>
        <input
          type="text"
          placeholder="Search by title, location, tech..."
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          style={{
            width: '100%',
            background: 'var(--surface2)',
            border: '1px solid var(--border)',
            borderRadius: 'var(--radius)',
            color: 'var(--text)',
            fontSize: 14,
            padding: '10px 12px 10px 36px',
            outline: 'none',
          }}
        />
        {query && (
          <button
            onClick={() => setQuery('')}
            style={{ position: 'absolute', right: 10, top: '50%', transform: 'translateY(-50%)', background: 'none', border: 'none', color: 'var(--muted)', cursor: 'pointer', fontSize: 16 }}
          >
            ✕
          </button>
        )}
      </div>

      {/* RESULTS COUNT */}
      <div style={{ fontSize: 12, color: 'var(--muted)', marginBottom: 12 }}>
        {query
          ? `${jobs.length} result${jobs.length !== 1 ? 's' : ''} for "${query}"`
          : `${jobs.length} open positions`}
      </div>

      {/* JOB CARDS */}
      {jobs.length === 0 ? (
        <div style={{ textAlign: 'center', padding: '2rem', color: 'var(--muted)', background: 'var(--surface)', borderRadius: 'var(--radius)', border: '1px solid var(--border)' }}>
          No jobs match <strong style={{ color: 'var(--text)' }}>"{query}"</strong>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          {jobs.map((job) => (
            <div key={job.id} style={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 'var(--radius)', padding: '1rem 1.25rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 8 }}>
                <span style={{ fontWeight: 600, fontSize: 14 }}>{job.title}</span>
                <span style={{ fontSize: 11, color: 'var(--muted)', background: 'var(--surface2)', padding: '2px 8px', borderRadius: 12, whiteSpace: 'nowrap', marginLeft: 8 }}>
                  {job.type}
                </span>
              </div>
              <div style={{ fontSize: 12, color: 'var(--muted)', marginBottom: 10 }}>
                📍 {job.location}
              </div>
              <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
                {(job.tags || []).map((t) => (
                  <span
                    key={t}
                    onClick={() => setQuery(t)}
                    style={{ fontSize: 11, color: 'var(--accent)', background: 'var(--accent-dim)', padding: '2px 8px', borderRadius: 12, cursor: 'pointer' }}
                  >
                    {t}
                  </span>
                ))}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
