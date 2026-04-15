export const metadata = {
  title: "Job Alerts & Tech Radar",
  description: "GitOps-based job alerts and platform radar dashboard"
};

export default function RootLayout({ children }) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}

