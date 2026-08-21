import { useEffect, useState } from 'react';
import { adminApi } from '../../api/endpoints';
import Loader from '../../components/Loader';
import StatTile from '../../components/StatTile';
import ChartCard from '../../components/charts/ChartCard';
import LineChart from '../../components/charts/LineChart';
import BarChart from '../../components/charts/BarChart';

const RANGES = [
  { days: 7, label: 'Last 7 days' },
  { days: 30, label: 'Last 30 days' },
  { days: 90, label: 'Last 90 days' },
];

export default function OverviewTab() {
  const [bundle, setBundle] = useState(null);
  const [days, setDays] = useState(30);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    adminApi.analytics(days).then(setBundle).catch(() => {}).finally(() => setLoading(false));
  }, [days]);

  if (loading && !bundle) return <Loader label="Loading analytics…" />;
  if (!bundle) return null;

  // Every field below is read straight out of the response. Defaulting them
  // here means a analytics payload that is missing a series renders an empty
  // chart instead of throwing and blanking the whole admin console.
  const stats = bundle.stats || {};
  const signupsPerDay = bundle.signupsPerDay || [];
  const listingsPerDay = bundle.listingsPerDay || [];
  const bookingsPerDay = bundle.bookingsPerDay || [];

  // All three are counts per day, so they share one axis honestly.
  const activitySeries = [
    { label: 'Signups', data: signupsPerDay },
    { label: 'Listings', data: listingsPerDay },
    { label: 'Bookings', data: bookingsPerDay },
  ];

  const activityRows = signupsPerDay.map((point, index) => [
    point.date,
    point.value,
    listingsPerDay[index]?.value ?? 0,
    bookingsPerDay[index]?.value ?? 0,
  ]);

  return (
    <>
      {/* Filters sit in one row above the charts, never between them. */}
      <div className="row" style={{ marginBottom: '1.25rem' }}>
        <div className="chip-row">
          {RANGES.map((range) => (
            <button
              key={range.days}
              type="button"
              className={`chip ${days === range.days ? 'is-active' : ''}`}
              onClick={() => setDays(range.days)}
            >
              {range.label}
            </button>
          ))}
        </div>
        {loading && <span className="small muted">Updating...</span>}
      </div>

      <div className="stat-row">
        <StatTile label="People" value={stats.totalUsers} tone="primary" />
        <StatTile label="Listings" value={stats.totalListings} />
        <StatTile label="Available now" value={stats.availableListings} tone="success" />
        <StatTile label="Bookings" value={stats.totalBookings} />
        <StatTile label="Open reports" value={stats.openReports} tone="accent" />
        <StatTile label="Awaiting verification" value={stats.pendingVerifications} />
      </div>

      <div className="stack" style={{ gap: '1rem' }}>
        <ChartCard
          title="Activity"
          subtitle={`Signups, new listings and booking requests over the last ${bundle.days} days`}
          tableHeaders={['Date', 'Signups', 'Listings', 'Bookings']}
          tableRows={activityRows}
        >
          <LineChart series={activitySeries} />
        </ChartCard>

        <div className="chart-grid">
          <ChartCard
            title="Where our users come from"
            subtitle="The reason this platform exists, in one chart"
            tableHeaders={['Country', 'People']}
            tableRows={(bundle.usersByNationality || []).map((row) => [row.label, row.value])}
          >
            <BarChart data={bundle.usersByNationality || []} colourIndex={0} />
          </ChartCard>

          <ChartCard
            title="Listings by city"
            tableHeaders={['City', 'Listings']}
            tableRows={(bundle.listingsByCity || []).map((row) => [row.label, row.value])}
          >
            <BarChart data={bundle.listingsByCity || []} colourIndex={1} />
          </ChartCard>

          <ChartCard
            title="Bookings by status"
            subtitle="How many requests actually turn into a confirmed stay"
            tableHeaders={['Status', 'Bookings']}
            tableRows={(bundle.bookingsByStatus || []).map((row) => [row.label, row.value])}
          >
            <BarChart data={bundle.bookingsByStatus || []} colourIndex={2} />
          </ChartCard>

          <ChartCard
            title="Listings by type"
            tableHeaders={['Type', 'Listings']}
            tableRows={(bundle.listingsByType || []).map((row) => [row.label, row.value])}
          >
            <BarChart data={bundle.listingsByType || []} colourIndex={3} />
          </ChartCard>
        </div>
      </div>

      <div className="stat-row" style={{ marginTop: '1.5rem' }}>
        <StatTile label="New people this week" value={stats.newUsersThisWeek} tone="success" />
        <StatTile label="New listings this week" value={stats.newListingsThisWeek} />
        <StatTile label="Bookings this week" value={stats.bookingsThisWeek} />
        <StatTile label="Verified listings" value={stats.verifiedListings} />
        <StatTile label="Hidden listings" value={stats.hiddenListings} />
        <StatTile label="High-risk listings" value={stats.highRiskListings} tone="accent" />
      </div>
    </>
  );
}
