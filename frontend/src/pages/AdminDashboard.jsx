import { Suspense, lazy, useState } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import useDocumentTitle from '../hooks/useDocumentTitle';
import PageTransition from '../components/PageTransition';
import Loader from '../components/Loader';
import { DURATION, EASE } from '../motion/tokens';

// Each tab pulls its own data and, in the overview's case, the whole charting
// layer. Loading three of them to show one was most of this route's weight.
const OverviewTab = lazy(() => import('./admin/OverviewTab'));
const ModerationTab = lazy(() => import('./admin/ModerationTab'));
const DataTab = lazy(() => import('./admin/DataTab'));
const ContentTab = lazy(() => import('./admin/ContentTab'));

const TABS = [
  { key: 'overview', label: 'Overview', component: OverviewTab },
  { key: 'moderation', label: 'Moderation', component: ModerationTab },
  { key: 'data', label: 'People & listings', component: DataTab },
  { key: 'content', label: 'Content & figures', component: ContentTab },
];

export default function AdminDashboard() {
  useDocumentTitle('Admin');
  const [active, setActive] = useState('overview');

  const Active = TABS.find((tab) => tab.key === active)?.component ?? OverviewTab;

  return (
    <PageTransition className="page">
      <div className="page-header">
        <div>
          <h1>Admin</h1>
          <p>How the platform is doing, what needs a decision, and what people are being told.</p>
        </div>
      </div>

      <nav className="admin-tabs" role="tablist">
        {TABS.map((tab) => (
          <button
            key={tab.key}
            type="button"
            role="tab"
            aria-selected={active === tab.key}
            className={`admin-tab ${active === tab.key ? 'is-active' : ''}`}
            onClick={() => setActive(tab.key)}
          >
            {tab.label}
            {/* A shared layoutId lets the underline slide between tabs rather
                than blinking out of one and into the next. */}
            {active === tab.key && (
              <motion.span
                layoutId="admin-tab-underline"
                className="admin-tab__underline"
                transition={{ type: 'spring', stiffness: 420, damping: 34 }}
              />
            )}
          </button>
        ))}
      </nav>

      <Suspense fallback={<Loader label="Loading…" />}>
        <AnimatePresence mode="wait" initial={false}>
          <motion.div
            key={active}
            role="tabpanel"
            initial={{ opacity: 0, y: 8 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -6 }}
            transition={{ duration: DURATION.base, ease: EASE }}
          >
            <Active />
          </motion.div>
        </AnimatePresence>
      </Suspense>
    </PageTransition>
  );
}
