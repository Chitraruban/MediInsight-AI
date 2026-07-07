import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { 
  FileText, Download, ShieldAlert, CheckSquare, Square, 
  HelpCircle, Apple, Dumbbell, Droplet, Moon, Sparkles, ChevronRight, AlertTriangle
} from 'lucide-react';

export default function ReportPage({ reportId, onNavigateToChat }) {
  const [report, setReport] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [activeCategory, setActiveCategory] = useState('FOOD');
  const [downloading, setDownloading] = useState(false);

  useEffect(() => {
    fetchReport();
  }, [reportId]);

  const fetchReport = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await axios.get(`/api/reports/${reportId}`);
      setReport(response.data);
    } catch (err) {
      setError(err.response?.data?.error || err.message || "Failed to load report detail.");
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleToggleChecklist = async (itemId) => {
    try {
      const response = await axios.patch(`/api/reports/${reportId}/checklist/${itemId}`);
      // Update local state
      setReport(prev => ({
        ...prev,
        checklist: prev.checklist.map(item => item.id === itemId ? response.data : item)
      }));
    } catch (err) {
      console.error("Failed to toggle checklist item:", err);
    }
  };

  const handleDownloadPdf = async () => {
    setDownloading(true);
    try {
      const response = await axios.get(`/api/reports/${reportId}/pdf`, {
        responseType: 'blob'
      });
      const blob = new Blob([response.data], { type: 'application/pdf' });
      const link = document.createElement('a');
      link.href = window.URL.createObjectURL(blob);
      
      const safeName = report.fileName.replace(/[^a-zA-Z0-9.-]/g, "_");
      link.download = `MediInsight_Summary_${safeName}.pdf`;
      link.click();
    } catch (err) {
      console.error("Failed to download PDF:", err);
      alert("Could not download report PDF. Please try again.");
    } finally {
      setDownloading(false);
    }
  };

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[50vh] space-y-4">
        <div className="w-12 h-12 border-4 border-slate-200 border-t-primary rounded-full animate-spin"></div>
        <p className="text-slate-600 dark:text-slate-400">Loading analysis results...</p>
      </div>
    );
  }

  if (error || !report) {
    return (
      <div className="max-w-md mx-auto my-12 p-6 bg-red-50 dark:bg-red-950/20 border border-red-200 dark:border-red-900/50 rounded-2xl text-center">
        <AlertTriangle className="w-12 h-12 text-red-600 mx-auto mb-4" />
        <h3 className="text-lg font-bold text-slate-900 dark:text-white">Failed to load report</h3>
        <p className="text-sm text-red-700 dark:text-red-300 mt-2">{error || "Report not found"}</p>
        <button 
          onClick={fetchReport}
          className="mt-6 px-4 py-2 bg-primary hover:bg-primary-light text-white font-semibold rounded-lg text-sm transition"
        >
          Retry
        </button>
      </div>
    );
  }

  // Group lifestyle suggestions by category
  const lifestyleByCategory = {
    FOOD: report.lifestyleSuggestions?.filter(s => s.category === 'FOOD') || [],
    EXERCISE: report.lifestyleSuggestions?.filter(s => s.category === 'EXERCISE') || [],
    HYDRATION: report.lifestyleSuggestions?.filter(s => s.category === 'HYDRATION') || [],
    SLEEP: report.lifestyleSuggestions?.filter(s => s.category === 'SLEEP') || [],
    STRESS: report.lifestyleSuggestions?.filter(s => s.category === 'STRESS') || []
  };

  const getScoreBg = (score) => {
    if (score >= 80) return 'text-emerald-500 border-emerald-500 bg-emerald-50 dark:bg-emerald-950/20';
    if (score >= 50) return 'text-amber-500 border-amber-500 bg-amber-50 dark:bg-amber-950/20';
    return 'text-rose-500 border-rose-500 bg-rose-50 dark:bg-rose-950/20';
  };

  const getStatusBadge = (status) => {
    const val = status.toUpperCase();
    if (val.includes('ABNORMAL')) {
      return 'bg-rose-100 text-rose-800 dark:bg-rose-900/30 dark:text-rose-300 border-rose-200 dark:border-rose-900';
    }
    if (val.includes('MILD') || val.includes('CONCERNS')) {
      return 'bg-amber-100 text-amber-800 dark:bg-amber-900/30 dark:text-amber-300 border-amber-200 dark:border-amber-900';
    }
    if (val.includes('NORMAL')) {
      return 'bg-emerald-100 text-emerald-800 dark:bg-emerald-900/30 dark:text-emerald-300 border-emerald-200 dark:border-emerald-900';
    }
    return 'bg-slate-100 text-slate-800 dark:bg-slate-800 dark:text-slate-300 border-slate-200 dark:border-slate-700';
  };

  const getValueStatusColor = (status) => {
    const val = status.toLowerCase();
    if (val === 'high') return 'text-rose-600 dark:text-rose-400 font-semibold bg-rose-50 dark:bg-rose-950/30 px-2 py-0.5 rounded';
    if (val === 'low') return 'text-amber-600 dark:text-amber-400 font-semibold bg-amber-50 dark:bg-amber-950/30 px-2 py-0.5 rounded';
    return 'text-slate-600 dark:text-slate-400';
  };

  const getRiskSeverityColor = (severity) => {
    const val = severity.toUpperCase();
    if (val === 'HIGH') return 'bg-rose-100 text-rose-800 dark:bg-rose-950/30 dark:text-rose-300';
    if (val === 'MEDIUM') return 'bg-amber-100 text-amber-800 dark:bg-amber-950/30 dark:text-amber-300';
    return 'bg-blue-100 text-blue-800 dark:bg-blue-950/30 dark:text-blue-300';
  };

  const categoryIcons = {
    FOOD: <Apple className="w-5 h-5" />,
    EXERCISE: <Dumbbell className="w-5 h-5" />,
    HYDRATION: <Droplet className="w-5 h-5" />,
    SLEEP: <Moon className="w-5 h-5" />,
    STRESS: <Sparkles className="w-5 h-5" />
  };

  return (
    <div className="max-w-6xl mx-auto px-4 py-8 space-y-8">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-b border-slate-200 dark:border-slate-800 pb-6">
        <div>
          <div className="flex items-center space-x-2 text-slate-500 dark:text-slate-400 text-sm mb-1">
            <FileText className="w-4 h-4" />
            <span>File Name: {report.fileName}</span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 dark:text-white">
            Report Analysis
          </h1>
        </div>
        <div className="flex flex-wrap items-center gap-3">
          <button
            onClick={() => onNavigateToChat(report.id)}
            className="flex items-center justify-center space-x-2 px-5 py-2.5 bg-primary hover:bg-primary-light text-white font-semibold rounded-xl text-sm transition shadow-sm"
          >
            <span>Ask AI Assistant</span>
            <ChevronRight className="w-4 h-4" />
          </button>
          
          <button
            onClick={handleDownloadPdf}
            disabled={downloading}
            className="flex items-center justify-center space-x-2 px-5 py-2.5 border border-slate-300 dark:border-slate-700 hover:bg-slate-50 dark:hover:bg-slate-800 text-slate-700 dark:text-slate-300 font-semibold rounded-xl text-sm transition shadow-sm disabled:opacity-50"
          >
            <Download className="w-4 h-4" />
            <span>{downloading ? "Downloading..." : "Download PDF"}</span>
          </button>
        </div>
      </div>

      {/* Main Grid: Overview & Score */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        
        {/* Left Side: Summary Card */}
        <div className="lg:col-span-2 bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm p-6 space-y-6">
          <div>
            <h2 className="text-xl font-bold text-slate-900 dark:text-white mb-3">Executive Summary</h2>
            <p className="text-slate-700 dark:text-slate-300 text-base leading-relaxed">
              {report.summary}
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-4 pt-4 border-t border-slate-100 dark:border-slate-800">
            <div className="flex flex-col">
              <span className="text-xs text-slate-400 uppercase tracking-wider font-semibold">Overall Health Rating</span>
              <span className={`inline-flex items-center px-3 py-1 rounded-full text-sm font-semibold border mt-1 w-max ${getStatusBadge(report.overallStatus)}`}>
                {report.overallStatus}
              </span>
            </div>
            
            <div className="flex flex-col">
              <span className="text-xs text-slate-400 uppercase tracking-wider font-semibold">General Risk Level</span>
              <span className="text-base font-bold text-slate-800 dark:text-slate-200 mt-1">
                {report.riskLevel} Risk
              </span>
            </div>
          </div>
        </div>

        {/* Right Side: Health Score Radial Circle */}
        <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm p-6 flex flex-col items-center justify-center text-center">
          <h3 className="text-sm font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider mb-4">
            MediInsight Score
          </h3>
          
          <div className="relative w-36 h-36 flex items-center justify-center">
            {/* Visual ring container */}
            <div className={`w-32 h-32 rounded-full border-8 flex flex-col items-center justify-center ${getScoreBg(report.healthScore)}`}>
              <span className="text-4xl font-extrabold tracking-tight">{report.healthScore}</span>
              <span className="text-xs font-semibold opacity-75">out of 100</span>
            </div>
          </div>

          <p className="text-xs text-slate-500 dark:text-slate-400 mt-4 leading-normal px-4">
            This score aggregates biomarker findings. Higher values signify normal values across panels.
          </p>
        </div>
      </div>

      {/* Lab Values Table Card */}
      <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm p-6">
        <h2 className="text-xl font-bold text-slate-900 dark:text-white mb-6">Biomarker & Lab Values</h2>
        
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="border-b border-slate-200 dark:border-slate-800 text-slate-500 dark:text-slate-400 text-sm font-semibold">
                <th className="py-3 px-4">Biomarker</th>
                <th className="py-3 px-4">Value</th>
                <th className="py-3 px-4">Reference Range</th>
                <th className="py-3 px-4">Status</th>
                <th className="py-3 px-4">Explanation</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 dark:divide-slate-800/60 text-sm">
              {report.values && report.values.length > 0 ? (
                report.values.map((v) => (
                  <tr key={v.id} className="hover:bg-slate-50/50 dark:hover:bg-slate-800/20">
                    <td className="py-4 px-4 font-bold text-slate-800 dark:text-slate-200">
                      {v.name}
                    </td>
                    <td className="py-4 px-4 font-medium text-slate-900 dark:text-white">
                      {v.value} <span className="text-slate-500 text-xs font-normal">{v.unit}</span>
                    </td>
                    <td className="py-4 px-4 text-slate-600 dark:text-slate-400 font-mono">
                      {v.normalRange || "N/A"}
                    </td>
                    <td className="py-4 px-4">
                      <span className={getValueStatusColor(v.status)}>
                        {v.status || "normal"}
                      </span>
                    </td>
                    <td className="py-4 px-4 text-slate-600 dark:text-slate-400 max-w-xs">
                      {v.termMeaning && <span className="font-semibold block mb-0.5 text-xs text-primary">{v.termMeaning}</span>}
                      {v.explanation}
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan="5" className="py-6 text-center text-slate-500 italic">No lab values extracted.</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Risks and Lifestyle Recommendation Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        
        {/* Left Side: Potential Health Risks */}
        <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm p-6 space-y-6">
          <h2 className="text-xl font-bold text-slate-900 dark:text-white flex items-center space-x-2">
            <ShieldAlert className="w-5 h-5 text-rose-500" />
            <span>Health Considerations</span>
          </h2>
          
          <div className="space-y-4">
            {report.risks && report.risks.length > 0 ? (
              report.risks.map((risk) => (
                <div key={risk.id} className="p-4 rounded-xl border border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-800/40 space-y-2">
                  <div className="flex items-center justify-between">
                    <h3 className="font-bold text-slate-800 dark:text-white">{risk.conditionName}</h3>
                    <span className={`px-2 py-0.5 rounded text-xs font-bold ${getRiskSeverityColor(risk.severity)}`}>
                      {risk.severity} Severity
                    </span>
                  </div>
                  {risk.relatedTo && (
                    <p className="text-xs text-primary font-medium">Related to: {risk.relatedTo}</p>
                  )}
                  <p className="text-xs text-slate-600 dark:text-slate-400">
                    {risk.explanation}
                  </p>
                </div>
              ))
            ) : (
              <div className="p-4 rounded-xl bg-slate-50 dark:bg-slate-800/30 border border-slate-100 dark:border-slate-800 text-center text-sm text-slate-500 italic">
                No health risks identified. Excellent!
              </div>
            )}
          </div>
        </div>

        {/* Right Side: Lifestyle Recommendations */}
        <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm p-6 flex flex-col">
          <h2 className="text-xl font-bold text-slate-900 dark:text-white mb-6">Lifestyle Optimization</h2>
          
          {/* Tab buttons */}
          <div className="flex items-center space-x-1 bg-slate-100 dark:bg-slate-800 p-1 rounded-xl mb-6 overflow-x-auto">
            {Object.keys(lifestyleByCategory).map((cat) => (
              <button
                key={cat}
                onClick={() => setActiveCategory(cat)}
                className={`flex-1 flex items-center justify-center space-x-1.5 px-3 py-2 text-xs font-semibold rounded-lg transition whitespace-nowrap ${
                  activeCategory === cat 
                    ? 'bg-white dark:bg-slate-900 text-primary shadow-sm' 
                    : 'text-slate-500 hover:text-slate-700 dark:hover:text-slate-300'
                }`}
              >
                {categoryIcons[cat]}
                <span className="hidden sm:inline capitalize">{cat.toLowerCase()}</span>
              </button>
            ))}
          </div>

          {/* Tab content */}
          <div className="flex-1 bg-slate-50 dark:bg-slate-800/40 border border-slate-200 dark:border-slate-800 rounded-xl p-4 min-h-[160px]">
            {lifestyleByCategory[activeCategory] && lifestyleByCategory[activeCategory].length > 0 ? (
              <ul className="space-y-2">
                {lifestyleByCategory[activeCategory].map((s) => (
                  <li key={s.id} className="text-sm text-slate-700 dark:text-slate-300 flex items-start space-x-2">
                    <span className="text-primary font-bold mt-0.5">•</span>
                    <span>{s.suggestionText}</span>
                  </li>
                ))}
              </ul>
            ) : (
              <p className="text-slate-500 italic text-sm text-center pt-8">No specific recommendations for this category.</p>
            )}
          </div>
        </div>
      </div>

      {/* Doctor Questions & Checklist Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        
        {/* Doctor Questions */}
        <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm p-6 space-y-4">
          <h2 className="text-xl font-bold text-slate-900 dark:text-white flex items-center space-x-2">
            <HelpCircle className="w-5 h-5 text-primary" />
            <span>Questions for Your Doctor</span>
          </h2>
          <p className="text-xs text-slate-500 dark:text-slate-400">
            Consider asking your physician these tailored questions about your results during your next clinic visit.
          </p>
          <ul className="space-y-3 pt-2">
            {report.doctorQuestions && report.doctorQuestions.length > 0 ? (
              report.doctorQuestions.map((q) => (
                <li key={q.id} className="text-sm text-slate-700 dark:text-slate-300 bg-slate-50 dark:bg-slate-800/20 p-3 rounded-xl border border-slate-100 dark:border-slate-800/55">
                  {q.questionText}
                </li>
              ))
            ) : (
              <li className="text-slate-500 italic text-sm text-center py-4">No questions compiled.</li>
            )}
          </ul>
        </div>

        {/* Interactive Checklist */}
        <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm p-6 space-y-4">
          <h2 className="text-xl font-bold text-slate-900 dark:text-white">Active Recommendations Checklist</h2>
          <p className="text-xs text-slate-500 dark:text-slate-400 font-normal">
            Tick items to mark your health optimization goals as accomplished. Action items are derived dynamically from report findings.
          </p>
          <div className="space-y-3 pt-2">
            {report.checklist && report.checklist.length > 0 ? (
              report.checklist.map((item) => (
                <div
                  key={item.id}
                  onClick={() => handleToggleChecklist(item.id)}
                  className={`flex items-center space-x-3 p-3 rounded-xl border cursor-pointer transition select-none ${
                    item.isChecked 
                      ? 'bg-emerald-50/40 dark:bg-emerald-950/10 border-emerald-200 dark:border-emerald-900/40 text-slate-500 dark:text-slate-400' 
                      : 'bg-slate-50 dark:bg-slate-800/30 border-slate-200 dark:border-slate-850 hover:bg-slate-100 dark:hover:bg-slate-800/50 text-slate-700 dark:text-slate-300'
                  }`}
                >
                  <button className="flex-shrink-0">
                    {item.isChecked ? (
                      <CheckSquare className="w-5 h-5 text-emerald-500" />
                    ) : (
                      <Square className="w-5 h-5 text-slate-400 hover:text-primary" />
                    )}
                  </button>
                  <span className={`text-sm ${item.isChecked ? 'line-through opacity-70' : 'font-medium'}`}>
                    {item.itemText}
                  </span>
                </div>
              ))
            ) : (
              <div className="text-slate-500 italic text-sm text-center py-4">No checklist items.</div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
