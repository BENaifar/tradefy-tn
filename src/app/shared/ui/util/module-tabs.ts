import { ModuleTab } from '../components/tx-module-tabs/tx-module-tabs.component';

export const MARKET_TABS: ModuleTab[] = [
  { label: 'Markets', link: '/markets', exact: true },
  { label: 'Charts', link: '/chart' },
  { label: 'Watchlist', link: '/watchlist' }
];

export const PORTFOLIO_TABS: ModuleTab[] = [
  { label: 'Overview', link: '/portfolio', exact: true },
  { label: 'Positions', link: '/portfolio/positions' },
  { label: 'Activity', link: '/portfolio/activity' },
  { label: 'Allocation', link: '/portfolio/allocation' },
  { label: 'Performance', link: '/portfolio/performance' },
  { label: 'Leaderboard', link: '/leaderboard' }
];
