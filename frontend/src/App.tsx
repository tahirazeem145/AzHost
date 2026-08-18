import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { BackendStatusProvider } from './context/BackendStatusContext';
import { Dashboard } from './pages/Dashboard';
import { Projects } from './pages/Projects';
import { Deployments } from './pages/Deployments';
import { LiveSites } from './pages/LiveSites';
import { Settings } from './pages/Settings';

export const App: React.FC = () => {
  return (
    <BackendStatusProvider>
      <Router>
        <Routes>
          <Route path="/" element={<Dashboard />} />
          <Route path="/projects" element={<Projects />} />
          <Route path="/deployments" element={<Deployments />} />
          <Route path="/live-sites" element={<LiveSites />} />
          <Route path="/settings" element={<Settings />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </Router>
    </BackendStatusProvider>
  );
};

export default App;
