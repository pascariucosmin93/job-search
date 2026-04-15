import JobSearch from './components/JobSearch';

const FALLBACK_JOBS = [
  { id: '1', title: 'Senior Platform Engineer',  location: 'Remote',    type: 'Full-time', tags: ['Kubernetes', 'GitOps', 'Terraform'] },
  { id: '2', title: 'Staff Backend Developer',   location: 'Berlin',    type: 'Hybrid',    tags: ['Java', 'Spring Boot', 'PostgreSQL'] },
  { id: '3', title: 'DevOps Engineer',           location: 'Remote',    type: 'Full-time', tags: ['CI/CD', 'Argo CD', 'Docker'] },
  { id: '4', title: 'Cloud Infrastructure Lead', location: 'Bucharest', type: 'On-site',   tags: ['AWS', 'Kubernetes', 'Terraform'] },
  { id: '5', title: 'Site Reliability Engineer', location: 'Amsterdam', type: 'Hybrid',    tags: ['Prometheus', 'Grafana', 'Kubernetes'] },
  { id: '6', title: 'Full Stack Developer',      location: 'Remote',    type: 'Full-time', tags: ['React', 'Node.js', 'PostgreSQL'] },
  { id: '7', title: 'Security Engineer',         location: 'Remote',    type: 'Full-time', tags: ['DevSecOps', 'Vault', 'Kubernetes'] },
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

const radar = {
  adopt:  { color: '#3fb950', items: ['Kubernetes', 'Argo CD', 'GitOps', 'Docker'] },
  trial:  { color: '#4f9cf9', items: ['Backstage', 'OpenTelemetry', 'eBPF'] },
  assess: { color: '#d29922', items: ['WebAssembly', 'WASM Workers'] },
  hold:   { color: '#7d8590', items: ['Manual Deployments', 'Helm v2'] },
};

const pipeline = [
  { step: 'git push',       icon: '⬆', desc: 'Developer pushes to main' },
  { step: 'GitHub Actions', icon: '⚙', desc: 'Tests + Docker build + GHCR push' },
  { step: 'GitOps Update',  icon: '📝', desc: 'Image tag bumped in gitops repo' },
  { step: 'Argo CD Sync',   icon: '🔄', desc: 'Auto-deploy to Kubernetes' },
];

export default async function HomePage() {
  const jobs = await fetchJobs();

  return (
    <>
      {/* NAV */}
      <nav style={{ borderBottom: '1px solid var(--border)', padding: '0 2rem' }}>
        <div style={{ maxWidth: 1100, margin: '0 auto', display: 'flex', alignItems: 'center', justifyContent: 'space-between', height: 56 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <span style={{ fontSize: 20 }}>🚀</span>
            <span style={{ fontWeight: 700, fontSize: 15, color: 'var(--text)' }}>cosmin-lab</span>
            <span style={{ color: 'var(--border)', margin: '0 6px' }}>/</span>
            <span style={{ fontSize: 14, color: 'var(--muted)' }}>job-platform</span>
          </div>
          <div style={{ display: 'flex', gap: 20, fontSize: 13, color: 'var(--muted)' }}>
            <span>⭐ {jobs.length} jobs</span>
            <span>📡 Radar</span>
            <span>⚙ Pipeline</span>
          </div>
        </div>
      </nav>

      <main style={{ maxWidth: 1100, margin: '0 auto', padding: '2rem' }}>

        {/* HERO */}
        <section style={{ textAlign: 'center', padding: '3rem 0 2.5rem' }}>
          <div style={{ display: 'inline-block', background: 'var(--accent-dim)', color: 'var(--accent)', fontSize: 12, fontWeight: 600, padding: '4px 12px', borderRadius: 20, marginBottom: 16, letterSpacing: 1 }}>
            PRODUCTION · GITOPS · KUBERNETES
          </div>
          <h1 style={{ fontSize: 'clamp(1.8rem, 4vw, 2.8rem)', fontWeight: 800, lineHeight: 1.2, marginBottom: 12 }}>
            Job Alerts & Tech Radar
          </h1>
          <p style={{ color: 'var(--muted)', fontSize: 16, maxWidth: 540, margin: '0 auto' }}>
            Microservices platform deployed via GitHub Actions → GHCR → Argo CD → Kubernetes.
            Zero manual{' '}
            <code style={{ background: 'var(--surface2)', padding: '1px 6px', borderRadius: 4, fontSize: 13 }}>kubectl apply</code>.
          </p>
        </section>

        {/* PIPELINE */}
        <section style={{ marginBottom: '2.5rem' }}>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: 1, background: 'var(--border)', borderRadius: 'var(--radius)', overflow: 'hidden' }}>
            {pipeline.map((p, i) => (
              <div key={i} style={{ background: 'var(--surface)', padding: '1rem 1.25rem', position: 'relative' }}>
                <div style={{ fontSize: 22, marginBottom: 6 }}>{p.icon}</div>
                <div style={{ fontWeight: 600, fontSize: 13, color: 'var(--accent)', marginBottom: 4 }}>{p.step}</div>
                <div style={{ fontSize: 12, color: 'var(--muted)' }}>{p.desc}</div>
                {i < pipeline.length - 1 && (
                  <div style={{ position: 'absolute', right: -10, top: '50%', transform: 'translateY(-50%)', color: 'var(--muted)', fontSize: 16, zIndex: 1 }}>›</div>
                )}
              </div>
            ))}
          </div>
        </section>

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1.5rem' }}>

          {/* JOB SEARCH */}
          <section>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1rem' }}>
              <h2 style={{ fontSize: 15, fontWeight: 700, color: 'var(--muted)', letterSpacing: 1, textTransform: 'uppercase' }}>
                📋 Job Alerts
              </h2>
              <span style={{ fontSize: 12, color: 'var(--green)', background: '#0d2118', padding: '2px 8px', borderRadius: 20 }}>
                live · job-service
              </span>
            </div>
            <JobSearch initialJobs={jobs} />
          </section>

          {/* TECH RADAR */}
          <section>
            <div style={{ marginBottom: '1rem' }}>
              <h2 style={{ fontSize: 15, fontWeight: 700, color: 'var(--muted)', letterSpacing: 1, textTransform: 'uppercase' }}>
                📡 Tech Radar
              </h2>
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
              {Object.entries(radar).map(([ring, { color, items }]) => (
                <div key={ring} style={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 'var(--radius)', padding: '1rem 1.25rem' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 10 }}>
                    <div style={{ width: 8, height: 8, borderRadius: '50%', background: color }} />
                    <span style={{ fontSize: 12, fontWeight: 700, textTransform: 'uppercase', letterSpacing: 1, color }}>{ring}</span>
                  </div>
                  <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
                    {items.map((item) => (
                      <span key={item} style={{ fontSize: 12, color: 'var(--text)', background: 'var(--surface2)', border: '1px solid var(--border)', padding: '3px 10px', borderRadius: 12 }}>
                        {item}
                      </span>
                    ))}
                  </div>
                </div>
              ))}
            </div>
          </section>

        </div>

        {/* SERVICES */}
        <section style={{ marginTop: '1.5rem', background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 'var(--radius)', padding: '1.25rem' }}>
          <h2 style={{ fontSize: 15, fontWeight: 700, color: 'var(--muted)', letterSpacing: 1, textTransform: 'uppercase', marginBottom: '1rem' }}>
            ⚡ Services
          </h2>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))', gap: 10 }}>
            {[
              { name: 'job-service',           tech: 'Spring Boot', port: 8080 },
              { name: 'auth-service',           tech: 'Spring Boot', port: 8080 },
              { name: 'notification-service',   tech: 'Node.js',     port: 8080 },
              { name: 'analytics-service',      tech: 'Node.js',     port: 8080 },
              { name: 'frontend',               tech: 'Next.js',     port: 3000 },
            ].map((svc) => (
              <div key={svc.name} style={{ background: 'var(--surface2)', borderRadius: 6, padding: '10px 12px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 4 }}>
                  <div style={{ width: 6, height: 6, borderRadius: '50%', background: 'var(--green)' }} />
                  <span style={{ fontSize: 12, fontWeight: 600 }}>{svc.name}</span>
                </div>
                <div style={{ fontSize: 11, color: 'var(--muted)' }}>{svc.tech} · :{svc.port}</div>
              </div>
            ))}
          </div>
        </section>

        <footer style={{ textAlign: 'center', padding: '2rem 0 1rem', color: 'var(--muted)', fontSize: 12 }}>
          Built with Spring Boot · Node.js · Next.js · Docker · Kubernetes · Argo CD · GitHub Actions
        </footer>

      </main>
    </>
  );
}
