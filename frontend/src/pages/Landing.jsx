import { Link } from 'react-router-dom';
import {
  Package, Cloud, Mail, Activity, Zap,
  ArrowRight, ExternalLink, Github, Database, Server,
} from 'lucide-react';

const API_DOCS_URL =
  'https://smartinventory-production-2890.up.railway.app/swagger-ui/index.html';
const GITHUB_URL = 'https://github.com/tm1206/smartinventory';

const features = [
  {
    icon: Cloud,
    title: 'AWS S3',
    subtitle: 'Product image storage',
    description:
      'Store and serve product images with secure presigned URLs for fast, reliable delivery.',
    gradient: 'from-orange-500 to-amber-400',
    ring: 'ring-orange-500/30',
    glow: 'group-hover:shadow-orange-500/20',
    iconBg: 'bg-orange-500/10',
    iconColor: 'text-orange-400',
  },
  {
    icon: Zap,
    title: 'AWS SQS',
    subtitle: 'Async order processing',
    description:
      'Decouple order placement from fulfilment with guaranteed message delivery and automatic retries.',
    gradient: 'from-purple-500 to-violet-400',
    ring: 'ring-purple-500/30',
    glow: 'group-hover:shadow-purple-500/20',
    iconBg: 'bg-purple-500/10',
    iconColor: 'text-purple-400',
  },
  {
    icon: Mail,
    title: 'AWS SES',
    subtitle: 'Email notifications',
    description:
      'Automated transactional emails for order confirmations, status updates, and low-stock alerts.',
    gradient: 'from-blue-500 to-cyan-400',
    ring: 'ring-blue-500/30',
    glow: 'group-hover:shadow-blue-500/20',
    iconBg: 'bg-blue-500/10',
    iconColor: 'text-blue-400',
  },
  {
    icon: Activity,
    title: 'CloudWatch',
    subtitle: 'Real-time monitoring',
    description:
      'Custom metrics and log aggregation give full visibility into app health and performance.',
    gradient: 'from-emerald-500 to-green-400',
    ring: 'ring-emerald-500/30',
    glow: 'group-hover:shadow-emerald-500/20',
    iconBg: 'bg-emerald-500/10',
    iconColor: 'text-emerald-400',
  },
];

const techBadges = [
  { label: 'Spring Boot 3.5', cls: 'bg-green-500/10 text-green-400 border-green-500/25' },
  { label: 'Java 21',         cls: 'bg-orange-500/10 text-orange-400 border-orange-500/25' },
  { label: 'PostgreSQL',      cls: 'bg-blue-500/10 text-blue-400 border-blue-500/25' },
  { label: 'React 18',        cls: 'bg-cyan-500/10 text-cyan-400 border-cyan-500/25' },
  { label: 'TailwindCSS',     cls: 'bg-sky-500/10 text-sky-400 border-sky-500/25' },
  { label: 'Vite',            cls: 'bg-purple-500/10 text-purple-400 border-purple-500/25' },
  { label: 'Railway',         cls: 'bg-violet-500/10 text-violet-400 border-violet-500/25' },
  { label: 'Vercel',          cls: 'bg-gray-500/10 text-gray-300 border-gray-500/25' },
  { label: 'AWS Cloud',       cls: 'bg-yellow-500/10 text-yellow-400 border-yellow-500/25' },
];

export default function Landing() {
  return (
    <div className="min-h-screen bg-gray-950 text-white flex flex-col">
      {/* ── Navbar ─────────────────────────────────────────────── */}
      <header className="sticky top-0 z-50 border-b border-white/5 bg-gray-950/80 backdrop-blur-md">
        <div className="mx-auto max-w-6xl px-4 sm:px-6 h-16 flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="rounded-lg bg-indigo-600 p-1.5">
              <Package className="h-5 w-5 text-white" />
            </div>
            <span className="font-semibold text-lg tracking-tight">SmartInventory</span>
          </div>
          <div className="flex items-center gap-3">
            <a
              href={GITHUB_URL}
              target="_blank"
              rel="noopener noreferrer"
              className="hidden sm:flex items-center gap-1.5 text-sm text-gray-400 hover:text-white transition-colors"
            >
              <Github className="h-4 w-4" />
              GitHub
            </a>
            <Link
              to="/login"
              className="rounded-lg bg-indigo-600 hover:bg-indigo-500 px-4 py-2 text-sm font-medium transition-colors"
            >
              Sign In
            </Link>
          </div>
        </div>
      </header>

      {/* ── Hero ───────────────────────────────────────────────── */}
      <section className="relative flex flex-1 flex-col items-center justify-center px-4 pt-24 pb-20 text-center overflow-hidden">
        {/* background glow blobs */}
        <div className="pointer-events-none absolute inset-0 overflow-hidden">
          <div className="absolute -top-40 left-1/2 -translate-x-1/2 h-[500px] w-[700px] rounded-full bg-indigo-600/10 blur-3xl" />
          <div className="absolute top-1/3 -left-20 h-72 w-72 rounded-full bg-purple-600/10 blur-3xl" />
          <div className="absolute top-1/3 -right-20 h-72 w-72 rounded-full bg-cyan-600/10 blur-3xl" />
        </div>

        <div className="relative z-10 max-w-3xl">
          {/* logo mark */}
          <div className="mb-6 inline-flex items-center justify-center rounded-2xl bg-indigo-600/15 ring-1 ring-indigo-500/30 p-4">
            <Package className="h-12 w-12 text-indigo-400" />
          </div>

          {/* badge */}
          <div className="mb-5 inline-flex items-center gap-2 rounded-full border border-white/10 bg-white/5 px-3 py-1 text-xs text-gray-400">
            <span className="inline-block h-1.5 w-1.5 rounded-full bg-green-400" />
            Production-ready · Full-stack · Open source
          </div>

          <h1 className="text-4xl sm:text-5xl lg:text-6xl font-bold tracking-tight leading-tight">
            Smart Inventory{' '}
            <span className="bg-gradient-to-r from-indigo-400 via-purple-400 to-cyan-400 bg-clip-text text-transparent">
              Management
            </span>
            <br />
            powered by{' '}
            <span className="bg-gradient-to-r from-orange-400 to-yellow-400 bg-clip-text text-transparent">
              AWS
            </span>
          </h1>

          <p className="mt-5 text-base sm:text-lg text-gray-400 max-w-xl mx-auto leading-relaxed">
            A production-grade full-stack app for managing products, orders, and users —
            with real-time monitoring, async processing, and cloud storage built in.
          </p>

          <div className="mt-8 flex flex-col sm:flex-row gap-3 justify-center">
            <Link
              to="/signup"
              className="inline-flex items-center justify-center gap-2 rounded-xl bg-indigo-600 hover:bg-indigo-500 px-6 py-3 text-sm font-semibold transition-all hover:scale-[1.02] active:scale-100 shadow-lg shadow-indigo-600/25"
            >
              Get Started
              <ArrowRight className="h-4 w-4" />
            </Link>
            <a
              href={API_DOCS_URL}
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center justify-center gap-2 rounded-xl border border-white/10 bg-white/5 hover:bg-white/10 px-6 py-3 text-sm font-semibold transition-all hover:scale-[1.02] active:scale-100"
            >
              View API Docs
              <ExternalLink className="h-4 w-4" />
            </a>
          </div>
        </div>
      </section>

      {/* ── Features ───────────────────────────────────────────── */}
      <section className="px-4 sm:px-6 py-20 bg-gray-900/50">
        <div className="mx-auto max-w-6xl">
          <div className="text-center mb-12">
            <h2 className="text-2xl sm:text-3xl font-bold">AWS Cloud Integration</h2>
            <p className="mt-3 text-gray-400 text-sm sm:text-base">
              Four AWS services working together to power production-grade features.
            </p>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
            {features.map(({ icon: Icon, title, subtitle, description, ring, glow, iconBg, iconColor }) => (
              <div
                key={title}
                className={`group relative rounded-2xl border border-white/8 bg-gray-900 p-6 transition-all duration-300 hover:-translate-y-1 hover:shadow-xl ${glow} hover:border-white/15 ring-1 ${ring}`}
              >
                <div className={`inline-flex rounded-xl ${iconBg} p-3 mb-4`}>
                  <Icon className={`h-6 w-6 ${iconColor}`} />
                </div>
                <h3 className="font-semibold text-base">{title}</h3>
                <p className={`text-xs font-medium mt-0.5 mb-3 bg-gradient-to-r ${features.find(f=>f.title===title)?.gradient || ''} bg-clip-text text-transparent`}>
                  {subtitle}
                </p>
                <p className="text-sm text-gray-400 leading-relaxed">{description}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ── Tech Stack ─────────────────────────────────────────── */}
      <section className="px-4 sm:px-6 py-20">
        <div className="mx-auto max-w-6xl">
          <div className="text-center mb-10">
            <h2 className="text-2xl sm:text-3xl font-bold">Tech Stack</h2>
            <p className="mt-3 text-gray-400 text-sm sm:text-base">
              Modern tools chosen for developer experience and production reliability.
            </p>
          </div>

          {/* 3 stack rows */}
          <div className="flex flex-col gap-6 items-center">
            {/* Backend */}
            <div className="flex flex-col sm:flex-row items-center gap-3">
              <div className="flex items-center gap-2 text-xs text-gray-500 w-20 justify-end shrink-0">
                <Server className="h-3.5 w-3.5" />
                Backend
              </div>
              <div className="flex flex-wrap gap-2 justify-center">
                {techBadges.slice(0, 3).map(({ label, cls }) => (
                  <span key={label} className={`rounded-full border px-3 py-1 text-xs font-medium ${cls}`}>
                    {label}
                  </span>
                ))}
              </div>
            </div>

            {/* Frontend */}
            <div className="flex flex-col sm:flex-row items-center gap-3">
              <div className="flex items-center gap-2 text-xs text-gray-500 w-20 justify-end shrink-0">
                <Package className="h-3.5 w-3.5" />
                Frontend
              </div>
              <div className="flex flex-wrap gap-2 justify-center">
                {techBadges.slice(3, 6).map(({ label, cls }) => (
                  <span key={label} className={`rounded-full border px-3 py-1 text-xs font-medium ${cls}`}>
                    {label}
                  </span>
                ))}
              </div>
            </div>

            {/* Cloud */}
            <div className="flex flex-col sm:flex-row items-center gap-3">
              <div className="flex items-center gap-2 text-xs text-gray-500 w-20 justify-end shrink-0">
                <Cloud className="h-3.5 w-3.5" />
                Cloud
              </div>
              <div className="flex flex-wrap gap-2 justify-center">
                {techBadges.slice(6).map(({ label, cls }) => (
                  <span key={label} className={`rounded-full border px-3 py-1 text-xs font-medium ${cls}`}>
                    {label}
                  </span>
                ))}
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* ── CTA strip ──────────────────────────────────────────── */}
      <section className="px-4 sm:px-6 py-16 bg-gradient-to-r from-indigo-600/20 via-purple-600/10 to-cyan-600/20 border-y border-white/5">
        <div className="mx-auto max-w-2xl text-center">
          <h2 className="text-2xl sm:text-3xl font-bold">Ready to explore?</h2>
          <p className="mt-3 text-gray-400 text-sm sm:text-base">
            Sign in to manage products, track orders, and monitor your inventory in real time.
          </p>
          <div className="mt-7 flex flex-col sm:flex-row gap-3 justify-center">
            <Link
              to="/signup"
              className="inline-flex items-center justify-center gap-2 rounded-xl bg-indigo-600 hover:bg-indigo-500 px-6 py-3 text-sm font-semibold transition-all hover:scale-[1.02] active:scale-100 shadow-lg shadow-indigo-600/25"
            >
              Get Started
              <ArrowRight className="h-4 w-4" />
            </Link>
            <a
              href={GITHUB_URL}
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center justify-center gap-2 rounded-xl border border-white/10 bg-white/5 hover:bg-white/10 px-6 py-3 text-sm font-semibold transition-all hover:scale-[1.02] active:scale-100"
            >
              <Github className="h-4 w-4" />
              View on GitHub
            </a>
          </div>
        </div>
      </section>

      {/* ── Footer ─────────────────────────────────────────────── */}
      <footer className="px-4 sm:px-6 py-8 border-t border-white/5">
        <div className="mx-auto max-w-6xl flex flex-col sm:flex-row items-center justify-between gap-4 text-sm text-gray-500">
          <div className="flex items-center gap-2">
            <div className="rounded-md bg-indigo-600/80 p-1">
              <Package className="h-3.5 w-3.5 text-white" />
            </div>
            <span>SmartInventory</span>
            <span className="text-gray-700">·</span>
            <span>Built by <span className="text-gray-300">Tarang Munje</span></span>
          </div>
          <div className="flex items-center gap-5">
            <a
              href={GITHUB_URL}
              target="_blank"
              rel="noopener noreferrer"
              className="flex items-center gap-1.5 hover:text-white transition-colors"
            >
              <Github className="h-4 w-4" />
              GitHub
            </a>
            <a
              href={API_DOCS_URL}
              target="_blank"
              rel="noopener noreferrer"
              className="flex items-center gap-1.5 hover:text-white transition-colors"
            >
              <Database className="h-4 w-4" />
              API Docs
            </a>
            <Link to="/login" className="hover:text-white transition-colors">
              Sign In
            </Link>
          </div>
        </div>
      </footer>
    </div>
  );
}
