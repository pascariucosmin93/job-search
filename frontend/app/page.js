const jobs = [
  { title: "Senior Platform Engineer", location: "Remote" },
  { title: "Staff Backend Developer", location: "Berlin" }
];

const radar = {
  adopt: ["Kubernetes", "Argo CD"],
  trial: ["Backstage", "OpenTelemetry"]
};

export default function HomePage() {
  return (
    <main style={{ fontFamily: "Georgia, serif", padding: "3rem", background: "linear-gradient(135deg, #f4efe6, #d7e6f1)", minHeight: "100vh", color: "#102a43" }}>
      <h1>Job Alerts & Tech Radar</h1>
      <p>Microservices platform delivered through GitHub Actions, GitOps manifests, and Argo CD auto-sync.</p>
      <section>
        <h2>Job Alerts</h2>
        <ul>
          {jobs.map((job) => (
            <li key={job.title}>{job.title} · {job.location}</li>
          ))}
        </ul>
      </section>
      <section>
        <h2>Tech Radar</h2>
        <p>Adopt: {radar.adopt.join(", ")}</p>
        <p>Trial: {radar.trial.join(", ")}</p>
      </section>
    </main>
  );
}

