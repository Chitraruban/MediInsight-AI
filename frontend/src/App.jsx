import React, { useState, useEffect } from 'react';
import UploadPage from './pages/UploadPage';
import ReportPage from './pages/ReportPage';
import DashboardPage from './pages/DashboardPage';
import ChatPage from './pages/ChatPage';
import { 
  Upload, LayoutDashboard, MessageSquare, Sun, Moon, AlertTriangle, ShieldAlert
} from 'lucide-react';

export default function App() {
  const [currentPage, setCurrentPage] = useState('upload');
  const [currentReportId, setCurrentReportId] = useState(null);
  const [darkMode, setDarkMode] = useState(false);

  // Initialize theme from localStorage
  useEffect(() => {
    const savedTheme = localStorage.getItem('theme');
    if (savedTheme === 'dark') {
      setDarkMode(true);
      document.documentElement.classList.add('dark');
    } else {
      setDarkMode(false);
      document.documentElement.classList.remove('dark');
    }
  }, []);

  const toggleDarkMode = () => {
    if (darkMode) {
      document.documentElement.classList.remove('dark');
      localStorage.setItem('theme', 'light');
      setDarkMode(false);
    } else {
      document.documentElement.classList.add('dark');
      localStorage.setItem('theme', 'dark');
      setDarkMode(true);
    }
  };

  const handleUploadSuccess = (reportId) => {
    setCurrentReportId(reportId);
    setCurrentPage('report');
  };

  const handleNavigateToReport = (reportId) => {
    setCurrentReportId(reportId);
    setCurrentPage('report');
  };

  const handleNavigateToChat = (reportId) => {
    setCurrentReportId(reportId);
    setCurrentPage('chat');
  };

  // Render current page component
  const renderPage = () => {
    switch (currentPage) {
      case 'upload':
        return <UploadPage onUploadSuccess={handleUploadSuccess} />;
      case 'report':
        return (
          <ReportPage 
            reportId={currentReportId} 
            onNavigateToChat={handleNavigateToChat} 
          />
        );
      case 'dashboard':
        return (
          <DashboardPage 
            onNavigateToReport={handleNavigateToReport} 
            onNavigateToUpload={() => setCurrentPage('upload')}
          />
        );
      case 'chat':
        return <ChatPage initialReportId={currentReportId} />;
      default:
        return <UploadPage onUploadSuccess={handleUploadSuccess} />;
    }
  };

  const isDisclaimerVisible = currentPage === 'report' || currentPage === 'chat';

  const navItems = [
    { id: 'upload', label: 'Upload Report', icon: <Upload className="w-5 h-5" /> },
    { id: 'dashboard', label: 'Dashboard', icon: <LayoutDashboard className="w-5 h-5" /> },
    { id: 'chat', label: 'AI Advisor', icon: <MessageSquare className="w-5 h-5" /> }
  ];

  return (
    <div className="min-h-screen bg-slate-50 dark:bg-slate-950 flex flex-col md:flex-row text-slate-800 dark:text-slate-100 transition-colors duration-200">
      
      {/* SIDEBAR NAVIGATION - Collapses to bottom nav on screens smaller than 880px (we target md: 768px/880px in custom classes) */}
      <aside className="w-full md:w-64 bg-white dark:bg-slate-900 border-b md:border-b-0 md:border-r border-slate-200 dark:border-slate-800 flex flex-row md:flex-col justify-between md:justify-start flex-shrink-0 z-10 md:h-screen sticky top-0 hidden md:flex">
        <div className="flex flex-col w-full h-full">
          {/* Logo / Header */}
          <div className="p-6 border-b border-slate-100 dark:border-slate-800 flex items-center space-x-3">
            <div className="w-9 h-9 bg-primary rounded-xl flex items-center justify-center text-white">
              🩺
            </div>
            <span className="font-extrabold text-lg tracking-tight text-primary dark:text-white">
              MediInsight AI
            </span>
          </div>

          {/* Navigation Links */}
          <nav className="flex-1 px-4 py-6 space-y-2">
            {navItems.map((item) => {
              const isActive = currentPage === item.id || (item.id === 'upload' && currentPage === 'report');
              return (
                <button
                  key={item.id}
                  onClick={() => {
                    setCurrentPage(item.id);
                    if (item.id !== 'chat') {
                      // Preserve linked report context only if entering chat, else clear it when switching context
                    }
                  }}
                  className={`w-full flex items-center space-x-3 px-4 py-3 rounded-xl text-sm font-semibold transition ${
                    isActive 
                      ? 'bg-primary text-white shadow-md shadow-primary/20' 
                      : 'text-slate-500 hover:text-slate-800 hover:bg-slate-100 dark:text-slate-400 dark:hover:text-white dark:hover:bg-slate-800'
                  }`}
                >
                  {item.icon}
                  <span>{item.label}</span>
                </button>
              );
            })}
          </nav>

          {/* Footer Controls */}
          <div className="p-4 border-t border-slate-100 dark:border-slate-800 flex items-center justify-between">
            <span className="text-xs text-slate-400 dark:text-slate-500 font-medium">Version 1.0</span>
            <button
              onClick={toggleDarkMode}
              className="p-2.5 rounded-xl border border-slate-200 dark:border-slate-800 hover:bg-slate-100 dark:hover:bg-slate-800 text-slate-500 dark:text-slate-400 transition"
              title={darkMode ? "Switch to Light Mode" : "Switch to Dark Mode"}
            >
              {darkMode ? <Sun className="w-4 h-4" /> : <Moon className="w-4 h-4" />}
            </button>
          </div>
        </div>
      </aside>

      {/* MOBILE HEADER & BOTTOM NAV (collapses for under 880px / medium screens) */}
      <div className="flex flex-col w-full md:hidden">
        {/* Header bar */}
        <header className="bg-white dark:bg-slate-900 border-b border-slate-200 dark:border-slate-800 px-6 py-4 flex items-center justify-between sticky top-0 z-20">
          <div className="flex items-center space-x-3">
            <div className="w-8 h-8 bg-primary rounded-lg flex items-center justify-center text-white text-sm">
              🩺
            </div>
            <span className="font-extrabold text-base tracking-tight text-primary dark:text-white">
              MediInsight AI
            </span>
          </div>
          <button
            onClick={toggleDarkMode}
            className="p-2 rounded-lg border border-slate-200 dark:border-slate-800 hover:bg-slate-100 dark:hover:bg-slate-800 text-slate-500 dark:text-slate-400 transition"
          >
            {darkMode ? <Sun className="w-4 h-4" /> : <Moon className="w-4 h-4" />}
          </button>
        </header>

        {/* Bottom Nav bar */}
        <nav className="fixed bottom-0 left-0 right-0 bg-white dark:bg-slate-900 border-t border-slate-200 dark:border-slate-800 flex items-center justify-around py-2.5 z-20 shadow-lg">
          {navItems.map((item) => {
            const isActive = currentPage === item.id || (item.id === 'upload' && currentPage === 'report');
            return (
              <button
                key={item.id}
                onClick={() => setCurrentPage(item.id)}
                className={`flex flex-col items-center space-y-1 py-1 px-3 rounded-xl transition ${
                  isActive ? 'text-primary dark:text-white font-bold' : 'text-slate-400 dark:text-slate-500'
                }`}
              >
                {item.icon}
                <span className="text-[10px]">{item.label}</span>
              </button>
            );
          })}
        </nav>
      </div>

      {/* MAIN CONTENT AREA */}
      <main className="flex-1 flex flex-col md:h-screen md:overflow-y-auto pb-20 md:pb-0">
        {/* Global Disclaimer Banner */}
        {isDisclaimerVisible && (
          <div className="bg-amber-500/10 dark:bg-amber-950/20 border-b border-amber-500/20 text-amber-800 dark:text-amber-300 px-6 py-3 text-xs sm:text-sm flex items-start sm:items-center space-x-2.5">
            <ShieldAlert className="w-4 h-4 sm:w-5 sm:h-5 text-amber-500 flex-shrink-0 mt-0.5 sm:mt-0" />
            <span>
              <strong>Disclaimer:</strong> This analysis is for educational purposes only and is not a substitute for professional medical consultation. Always consult a licensed healthcare provider.
            </span>
          </div>
        )}

        <div className="flex-1">
          {renderPage()}
        </div>
      </main>
    </div>
  );
}
