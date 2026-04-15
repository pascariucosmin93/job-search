import "./globals.css";

export const metadata = {
  title: "Engineering Jobs Board",
  description: "Search engineering jobs via a live free API with filters for location and remote roles."
};

export default function RootLayout({ children }) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
