'use client';

import { useMemo, useState } from 'react';

function uniqueValues(items) {
  return [...new Set(items.filter(Boolean))].sort((a, b) => a.localeCompare(b));
}

function formatDate(value) {
  if (!value) return 'Date unavailable';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;

  return new Intl.DateTimeFormat('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric'
  }).format(date);
}

function scoreJob(job, query) {
  if (!query) return 0;
  const normalized = query.toLowerCase();

  const title = job.title.toLowerCase();
  const company = (job.company || '').toLowerCase();
  const location = (job.location || '').toLowerCase();
  const tags = (job.tags || []).join(' ').toLowerCase();

  let score = 0;
  if (title.includes(normalized)) score += 5;
  if (company.includes(normalized)) score += 3;
  if (tags.includes(normalized)) score += 2;
  if (location.includes(normalized)) score += 1;
  return score;
}

export default function JobSearch({ initialJobs }) {
  const [query, setQuery] = useState('');
  const [location, setLocation] = useState('');
  const [remoteOnly, setRemoteOnly] = useState(false);
  const [sortBy, setSortBy] = useState('relevance');

  const locations = useMemo(
    () => uniqueValues(initialJobs.map((job) => job.location)),
    [initialJobs]
  );

  const filteredJobs = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase();
    const normalizedLocation = location.trim().toLowerCase();

    const filtered = initialJobs.filter((job) => {
      if (remoteOnly && !job.remote) return false;
      if (normalizedLocation && !(job.location || '').toLowerCase().includes(normalizedLocation)) {
        return false;
      }
      if (!normalizedQuery) return true;

      const searchable = [
        job.title,
        job.company,
        job.location,
        job.type,
        ...(job.tags || [])
      ]
        .filter(Boolean)
        .join(' ')
        .toLowerCase();

      return searchable.includes(normalizedQuery);
    });

    return filtered.sort((a, b) => {
      if (sortBy === 'latest') {
        return (new Date(b.publishedAt).getTime() || 0) - (new Date(a.publishedAt).getTime() || 0);
      }

      const relevanceDiff = scoreJob(b, normalizedQuery) - scoreJob(a, normalizedQuery);
      if (relevanceDiff !== 0) return relevanceDiff;
      return a.title.localeCompare(b.title);
    });
  }, [initialJobs, query, location, remoteOnly, sortBy]);

  return (
    <section className="jobs-shell">
      <div className="filters-grid">
        <label className="field">
          <span>Keyword</span>
          <input
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="e.g. spring, devops, frontend"
          />
        </label>

        <label className="field">
          <span>Location</span>
          <input
            list="known-locations"
            value={location}
            onChange={(event) => setLocation(event.target.value)}
            placeholder="Any location"
          />
          <datalist id="known-locations">
            {locations.slice(0, 40).map((city) => (
              <option key={city} value={city} />
            ))}
          </datalist>
        </label>

        <label className="field">
          <span>Sort</span>
          <select value={sortBy} onChange={(event) => setSortBy(event.target.value)}>
            <option value="relevance">Best match</option>
            <option value="latest">Newest first</option>
          </select>
        </label>

        <label className="switch">
          <input
            type="checkbox"
            checked={remoteOnly}
            onChange={(event) => setRemoteOnly(event.target.checked)}
          />
          <span>Remote only</span>
        </label>
      </div>

      <div className="results-row">
        <p>
          {filteredJobs.length} job{filteredJobs.length !== 1 ? 's' : ''} found
        </p>
        {(query || location || remoteOnly) && (
          <button
            className="reset-btn"
            type="button"
            onClick={() => {
              setQuery('');
              setLocation('');
              setRemoteOnly(false);
              setSortBy('relevance');
            }}
          >
            Clear filters
          </button>
        )}
      </div>

      <div className="jobs-list">
        {filteredJobs.length === 0 ? (
          <div className="empty-state">
            <h3>No jobs match these filters</h3>
            <p>Try a broader keyword or remove location constraints.</p>
          </div>
        ) : (
          filteredJobs.map((job) => (
            <article key={job.id} className="job-card">
              <div className="job-main">
                <div className="company-row">
                  <p className="company-name">{job.company || 'Unknown company'}</p>
                  <span className={`pill ${job.remote ? 'pill-remote' : ''}`}>
                    {job.remote ? 'Remote' : 'On-site/Hybrid'}
                  </span>
                </div>

                <h3>{job.title}</h3>

                <div className="meta-row">
                  <span>{job.location || 'Location unavailable'}</span>
                  <span>{job.type || 'Type unavailable'}</span>
                  <span>Posted {formatDate(job.publishedAt)}</span>
                </div>

                <div className="tags-row">
                  {(job.tags || []).slice(0, 6).map((tag) => (
                    <button key={tag} type="button" onClick={() => setQuery(tag)}>
                      {tag}
                    </button>
                  ))}
                </div>
              </div>

              <div className="actions-col">
                <span className="source-chip">{job.source || 'Source unknown'}</span>
                {job.url ? (
                  <a href={job.url} target="_blank" rel="noreferrer">
                    Apply now
                  </a>
                ) : (
                  <span className="disabled-link">No external link</span>
                )}
              </div>
            </article>
          ))
        )}
      </div>
    </section>
  );
}
