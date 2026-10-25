import { useEffect } from 'react';
import { Navigate, Route, Routes, useLocation } from 'react-router-dom';
import { AppShell } from './components/AppShell';
import { SkyBackground } from './components/SkyBackground';
import { LandingPage } from './pages/LandingPage';
import { HomePage } from './pages/HomePage';
import { CalendarPage } from './pages/CalendarPage';
import { PrayerTimesPage } from './pages/PrayerTimesPage';
import { NotFoundPage } from './pages/NotFoundPage';
import { CalendarGuidePage, DateConverterGuidePage } from './pages/SeoGuidePages';
import { CalendarMonthPage, CalendarYearPage } from './pages/CalendarArchivePages';
import { PrayerMonthPage } from './pages/PrayerMonthPage';
import { PrivacyPage } from './pages/PrivacyPage';
import { CityPrayerTimesPage, PrayerTimesIndexPage } from './pages/CityPrayerTimesPage';
import { syncPageHead } from './lib/syncPageHead';

/** Keeps the title and description right when navigating without a page load. */
function HeadSync() {
  const { pathname } = useLocation();
  useEffect(() => {
    syncPageHead(pathname);
  }, [pathname]);
  return null;
}

/** The route table, shared by the browser (BrowserRouter) and the pre-renderer (StaticRouter). */
export default function App() {
  return (
    <>
      <HeadSync />
      <SkyBackground />
      <Routes>
        <Route path="/" element={<LandingPage />} />
        <Route path="/calendar" element={<CalendarGuidePage />} />
        <Route path="/calendar/:year" element={<CalendarYearPage />} />
        <Route path="/calendar/:year/:month" element={<CalendarMonthPage />} />
        <Route path="/date-converter" element={<DateConverterGuidePage />} />
        <Route path="/privacy" element={<PrivacyPage />} />
        <Route path="/prayer-times" element={<PrayerTimesIndexPage />} />
        <Route path="/prayer-times/:city" element={<CityPrayerTimesPage />} />
        <Route path="/prayer-times/:city/:year/:month" element={<PrayerMonthPage />} />
        <Route path="/app" element={<AppShell />}>
          <Route index element={<Navigate to="calendar" replace />} />
          <Route path="home" element={<HomePage />} />
          <Route path="calendar" element={<CalendarPage />} />
          <Route path="prayer-times" element={<PrayerTimesPage />} />
        </Route>
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </>
  );
}
