import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { 
  LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer 
} from 'recharts';
import { 
  TrendingUp, Activity, AlertCircle, FileText, ChevronRight, RefreshCw, Calendar, Trash2 
} from 'lucide-react';

export default function DashboardPage({ onNavigateToReport, onNavigateToUpload }) {
  const [summary, setSummary] = useState(null);
  const [reports, setReports] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetchDashboardData();
  }, []);

  const fetchDashboardData = async () => {
    setLoading(true);
    setError(null);
    try {
      const [summaryRes, reportsRes] = await Promise.all([
        axios.get('/api/dashboard/summary'),
        axios.get('/api/reports')
      ]);
      setSummary(summaryRes.data);
      setReports(reportsRes.data);
    } catch (err) {
      setError("Failed to load dashboard data. Please try again.");
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteReport = async (e, id) => {
    e.stopPropagation(); // Avoid navigating to report page
    if (!window.confirm("Are you sure you want to delete this report? This action cannot be undone.")) {
      return;
    }
    
    try {
      await axios.delete(`/api/reports/${id}`);
      // Refresh data
      fetchDashboardData();
    } catch (err) {
      alert("Failed to delete report: " + (err.response?.data?.error || err.message));
      console.error(err);
    }
  };

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[50vh] space-y-4">
        <div className="w-12 h-12 border-4 border-slate-200 border-t-primary rounded-full animate-spin"></div>
        <p className="text-slate-600 dark:text-slate-400">Loading dashboard...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="max-w-md mx-auto my-12 p-6 bg-red-50 dark:bg-red-950/20 border border-red-200 dark:border-red-900/50 rounded-2xl text-center">
        <AlertCircle className="w-12 h-12 text-red-600 mx-auto mb-4" />
        <h3 className="text-lg font-bold text-slate-900 dark:text-white">Dashboard Error</h3>
        <p className="text-sm text-red-700 dark:text-red-300 mt-2">{error}</p>
        <button 
          onClick={fetchDashboardData}
          className="mt-6 px-4 py-2 bg-primary hover:bg-primary-light text-white font-semibold rounded-lg text-sm transition"
        >
          Retry
        </button>
      </div>
    );
  }

  const hasReports = reports && reports.length > 0;

  const getScoreColorClass = (score) => {
    if (score >= 80) return 'text-emerald-500';
    if (score >= 50) return 'text-amber-500';
    return 'text-rose-500';
  };

  const getRiskColorClass = (risk) => {
    const val = risk ? risk.toUpperCase() : 'LOW';
    if (val === 'HIGH') return 'text-rose-500';
    if (val === 'MEDIUM') return 'text-amber-500';
    return 'text-emerald-500';
  };

  return (
    <div className="max-w-6xl mx-auto px-4 py-8 space-y-8">
      {/* Header */}
      <div className="flex items-center justify-between border-b border-slate-200 dark:border-slate-800 pb-6">
        <div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 dark:text-white">Health Dashboard</h1>
          <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">Track health metrics, score trends, and clinical history.</p>
        </div>
        <button
          onClick={fetchDashboardData}
          className="p-2 border border-slate-200 dark:border-slate-800 hover:bg-slate-50 dark:hover:bg-slate-800 rounded-xl transition text-slate-500 dark:text-slate-400"
          title="Refresh Data"
        >
          <RefreshCw className="w-5 h-5" />
        </button>
      </div>

      {!hasReports ? (
        <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm p-10 text-center space-y-4">
          <Activity className="w-16 h-16 text-slate-300 dark:text-slate-700 mx-auto" />
          <h2 className="text-xl font-bold text-slate-950 dark:text-white">No Medical Reports Yet</h2>
          <p className="text-slate-600 dark:text-slate-400 max-w-sm mx-auto text-sm">
            Upload your first health report to compile health scores, view clinical analysis, and visualize trends over time.
          </p>
          <div className="pt-2">
            <button
              onClick={onNavigateToUpload}
              className="px-6 py-3 bg-primary hover:bg-primary-light text-white font-semibold rounded-xl text-sm transition shadow-sm"
            >
              Upload Report
            </button>
          </div>
        </div>
      ) : (
        <>
          {/* Stat Cards */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-6">
            
            {/* Health Score Stat Card */}
            <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm p-6 flex items-center justify-between">
              <div className="space-y-1">
                <span className="text-xs font-bold text-slate-400 dark:text-slate-500 uppercase tracking-wider">Latest Health Score</span>
                <p className={`text-4xl font-extrabold ${getScoreColorClass(summary.latestHealthScore)}`}>
                  {summary.latestHealthScore}
                </p>
                <p className="text-xs text-slate-500 dark:text-slate-400">Scale of 0 to 100</p>
              </div>
              <div className="w-12 h-12 bg-blue-50 dark:bg-slate-800 rounded-xl flex items-center justify-center text-primary dark:text-primary-light">
                <TrendingUp className="w-6 h-6" />
              </div>
            </div>

            {/* Abnormal Values Stat Card */}
            <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm p-6 flex items-center justify-between">
              <div className="space-y-1">
                <span className="text-xs font-bold text-slate-400 dark:text-slate-500 uppercase tracking-wider">Abnormal Biomarkers</span>
                <p className="text-4xl font-extrabold text-slate-900 dark:text-white">
                  {summary.totalAbnormalValues}
                </p>
                <p className="text-xs text-slate-500 dark:text-slate-400">In latest uploaded report</p>
              </div>
              <div className="w-12 h-12 bg-rose-50 dark:bg-slate-800 rounded-xl flex items-center justify-center text-rose-500">
                <AlertCircle className="w-6 h-6" />
              </div>
            </div>

            {/* Risk Level Stat Card */}
            <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm p-6 flex items-center justify-between">
              <div className="space-y-1">
                <span className="text-xs font-bold text-slate-400 dark:text-slate-500 uppercase tracking-wider">General Risk Severity</span>
                <p className={`text-4xl font-extrabold ${getRiskColorClass(summary.latestRiskLevel)}`}>
                  {summary.latestRiskLevel}
                </p>
                <p className="text-xs text-slate-500 dark:text-slate-400">Determined via AI triage</p>
              </div>
              <div className="w-12 h-12 bg-emerald-50 dark:bg-slate-800 rounded-xl flex items-center justify-center text-emerald-500">
                <Activity className="w-6 h-6" />
              </div>
            </div>

          </div>

          {/* Trend Chart */}
          <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm p-6">
            <h2 className="text-lg font-bold text-slate-900 dark:text-white mb-6">Health Score Trend Line</h2>
            <div className="w-full h-80">
              <ResponsiveContainer width="100%" height="100%">
                <LineChart
                  data={summary.scoreTrend}
                  margin={{ top: 10, right: 30, left: 0, bottom: 0 }}
                >
                  <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#E2E8F0" className="dark:stroke-slate-800" />
                  <XAxis 
                    dataKey="date" 
                    stroke="#94A3B8" 
                    fontSize={12}
                    tickLine={false}
                    axisLine={false}
                    dy={10}
                  />
                  <YAxis 
                    stroke="#94A3B8" 
                    fontSize={12}
                    tickLine={false}
                    axisLine={false}
                    domain={[0, 100]}
                    dx={-10}
                  />
                  <Tooltip 
                    contentStyle={{ 
                      backgroundColor: 'rgba(255, 255, 255, 0.95)',
                      borderRadius: '8px',
                      border: '1px solid #CBD5E1',
                      color: '#1E293B',
                      fontSize: '12px',
                    }}
                    cursor={{ stroke: '#0B5FA5', strokeWidth: 1, strokeDasharray: '3 3' }}
                  />
                  <Line 
                    type="monotone" 
                    dataKey="score" 
                    stroke="#0B5FA5" 
                    strokeWidth={3} 
                    dot={{ fill: '#0B5FA5', stroke: '#FFF', strokeWidth: 2, r: 6 }}
                    activeDot={{ r: 8, strokeWidth: 0 }}
                  />
                </LineChart>
              </ResponsiveContainer>
            </div>
          </div>

          {/* Past Reports List */}
          <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm p-6">
            <h2 className="text-lg font-bold text-slate-900 dark:text-white mb-6">Clinical History Catalog</h2>
            
            <div className="divide-y divide-slate-100 dark:divide-slate-800">
              {reports.map((report) => (
                <div
                  key={report.id}
                  onClick={() => onNavigateToReport(report.id)}
                  className="py-4 flex items-center justify-between cursor-pointer hover:bg-slate-50/50 dark:hover:bg-slate-850 p-2 rounded-xl transition group"
                >
                  <div className="flex items-center space-x-4 min-w-0">
                    <div className="w-10 h-10 bg-primary-lightest dark:bg-slate-800 text-primary dark:text-primary-light rounded-xl flex items-center justify-center flex-shrink-0">
                      <FileText className="w-5 h-5" />
                    </div>
                    <div className="truncate">
                      <h3 className="text-sm font-bold text-slate-850 dark:text-slate-200 truncate group-hover:text-primary transition">
                        {report.fileName}
                      </h3>
                      <div className="flex items-center space-x-3 text-xs text-slate-500 dark:text-slate-400 mt-1">
                        <span className="flex items-center space-x-1">
                          <Calendar className="w-3.5 h-3.5" />
                          <span>{new Date(report.uploadedAt).toLocaleDateString()}</span>
                        </span>
                        <span>•</span>
                        <span className={`font-semibold ${getScoreColorClass(report.healthScore)}`}>
                          Score: {report.healthScore}
                        </span>
                        <span>•</span>
                        <span>Status: {report.overallStatus}</span>
                      </div>
                    </div>
                  </div>

                  <div className="flex items-center space-x-4 flex-shrink-0 pl-4">
                    <button
                      onClick={(e) => handleDeleteReport(e, report.id)}
                      className="p-2 text-slate-400 hover:text-rose-500 dark:hover:text-rose-400 hover:bg-rose-50 dark:hover:bg-rose-950/20 rounded-lg transition"
                      title="Delete Report"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                    <ChevronRight className="w-5 h-5 text-slate-400 group-hover:text-primary group-hover:translate-x-1 transition" />
                  </div>
                </div>
              ))}
            </div>
          </div>
        </>
      )}
    </div>
  );
}
