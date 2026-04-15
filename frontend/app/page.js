import JobSearch from './components/JobSearch';

const FALLBACK_JOBS = [
  {
    id: 'fallback-1',
    title: 'Senior Platform Engineer',
    company: 'CloudScale Labs',
    location: 'Remote',
    type: 'Full-time',
    remote: true,
    tags: ['Kubernetes', 'Terraform', 'GitOps'],
    url: 'https://example.com/jobs/senior-platform-engineer',
    source: 'Fallback',
    publishedAt: '2026-04-01'
  },
  {
    id: 'fallback-2',
    title: 'Java Backend Engineer',
    company: 'Nimbus Systems',
    location: 'Berlin',
    type: 'Hybrid',
    remote: false,
    tags: ['Java', 'Spring Boot', 'PostgreSQL'],
    url: 'https://example.com/jobs/java-backend-engineer',
    source: 'Fallback',
    publishedAt: '2026-03-30'
  }
];

async function fetchJobs() {
  const url = `${process.env.JOB_SERVICE_URL || 'http://localhost:8080'}/api/jobs`;
  try {
    const res = await fetch(url, { next: { revalidate: 30 } });
    if (!res.ok) return FALLBACK_JOBS;
    return res.json();
  } catch {
    return FALLBACK_JOBS;
  }
}

const highlights = [
  { title: 'Live Listings', value: 'API-fed', detail: 'Jobs pulled from Arbeitnow endpoint' },
  { title: 'Focus', value: 'Engineering', detail: 'Backend, frontend, infra and product roles' },
  { title: 'Flow', value: '<2 min', detail: 'Search, filter, open application page instantly' }
];

export default async function HomePage() {
  const jobs = await fetchJobs();
  const remoteCount = jobs.filter((job) => job.remote).length;

  return (
    <div className="page-wrap">
      <header className="topbar">
        <div className="topbar-inner">
          <div className="brand-block">
            <p className="brand-kicker">Job platform</p>
            <h1>Find your next engineering role</h1>
          </div>
          <div className="headline-stats">
            <div>
              <strong>{jobs.length}</strong>
              <span>total jobs</span>
            </div>
            <div>
              <strong>{remoteCount}</strong>
              <span>remote roles</span>
            </div>
          </div>
        </div>
      </header>

      <main className="main-content">
        <section className="hero-card">
          <p className="hero-label">Fresh opportunities</p>
          <h2>Search jobs by skill, location and work model</h2>
          <p>
            Data comes from a free external jobs API and is normalized by your `job-service`.
            When provider calls fail, the platform automatically serves fallback postings.
          </p>
        </section>

        <section className="highlights-grid">
          {highlights.map((item) => (
            <article key={item.title} className="highlight-card">
              <p>{item.title}</p>
              <h3>{item.value}</h3>
              <span>{item.detail}</span>
            </article>
          ))}
        </section>

        <section>
          <JobSearch initialJobs={jobs} />
        </section>

        <footer className="footer-note">
          Powered by job-service + Arbeitnow API | UI optimized for desktop and mobile
        </footer>
      </main>
    </div>
  );
}
