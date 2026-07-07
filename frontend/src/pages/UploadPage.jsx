import React, { useState, useRef } from 'react';
import axios from 'axios';
import { Upload, FileText, AlertCircle, CheckCircle2 } from 'lucide-react';

export default function UploadPage({ onUploadSuccess }) {
  const [dragActive, setDragActive] = useState(false);
  const [file, setFile] = useState(null);
  const [loading, setLoading] = useState(false);
  const [uploadStep, setUploadStep] = useState(0);
  const [error, setError] = useState(null);
  const fileInputRef = useRef(null);

  const steps = [
    "Uploading lab report file...",
    "Extracting report text (running OCR fallback)...",
    "Analyzing clinical biomarkers with Gemini AI...",
    "Structuring findings and saving summary..."
  ];

  const handleDrag = (e) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === "dragenter" || e.type === "dragover") {
      setDragActive(true);
    } else if (e.type === "dragleave") {
      setDragActive(false);
    }
  };

  const handleDrop = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);
    
    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      validateAndSetFile(e.dataTransfer.files[0]);
    }
  };

  const handleChange = (e) => {
    e.preventDefault();
    if (e.target.files && e.target.files[0]) {
      validateAndSetFile(e.target.files[0]);
    }
  };

  const validateAndSetFile = (selectedFile) => {
    const validTypes = ['application/pdf', 'image/jpeg', 'image/jpg', 'image/png'];
    if (validTypes.includes(selectedFile.type)) {
      setFile(selectedFile);
      setError(null);
    } else {
      setError("Invalid file type. Please upload a PDF or an Image (JPG, PNG).");
      setFile(null);
    }
  };

  const handleUpload = async () => {
    if (!file) return;

    const formData = new FormData();
    formData.append('file', file);

    setLoading(true);
    setError(null);
    setUploadStep(0);

    // Simulate progress updates for a smoother visual feel
    const stepIntervals = [
      setTimeout(() => setUploadStep(1), 1800),
      setTimeout(() => setUploadStep(2), 3500),
      setTimeout(() => setUploadStep(3), 6000)
    ];

    try {
      const response = await axios.post('/api/reports/upload', formData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      });
      
      // Clear intervals
      stepIntervals.forEach(clearTimeout);
      setUploadStep(4);
      
      // Let the user see completion for half a second
      setTimeout(() => {
        onUploadSuccess(response.data.id);
      }, 500);

    } catch (err) {
      stepIntervals.forEach(clearTimeout);
      setLoading(false);
      const errMsg = err.response?.data?.error || err.message || "An error occurred during upload.";
      setError(errMsg);
      console.error("Upload error:", err);
    }
  };

  return (
    <div className="max-w-3xl mx-auto px-4 py-8">
      <div className="text-center mb-8">
        <h1 className="text-3xl font-extrabold tracking-tight text-slate-900 dark:text-white">
          Upload Medical Report
        </h1>
        <p className="mt-2 text-base text-slate-600 dark:text-slate-400">
          Upload a lab test or medical diagnostic report to extract key biomarkers, health scoring, and lifestyle pointers.
        </p>
      </div>

      {error && (
        <div className="mb-6 p-4 rounded-xl bg-red-50 dark:bg-red-950/30 border border-red-200 dark:border-red-900/50 flex items-start space-x-3 text-red-800 dark:text-red-300">
          <AlertCircle className="w-5 h-5 mt-0.5 flex-shrink-0" />
          <div>
            <h4 className="font-semibold">Analysis Failed</h4>
            <p className="text-sm mt-0.5">{error}</p>
          </div>
        </div>
      )}

      {!loading ? (
        <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm p-6 sm:p-10 transition duration-300">
          <div
            onDragEnter={handleDrag}
            onDragOver={handleDrag}
            onDragLeave={handleDrag}
            onDrop={handleDrop}
            className={`border-2 border-dashed rounded-xl p-8 text-center cursor-pointer transition ${
              dragActive 
                ? 'border-primary bg-primary-lightest/40 dark:bg-primary-dark/10' 
                : 'border-slate-300 dark:border-slate-700 hover:border-primary/60 hover:bg-slate-50 dark:hover:bg-slate-800/40'
            }`}
            onClick={() => fileInputRef.current.click()}
          >
            <input
              ref={fileInputRef}
              type="file"
              className="hidden"
              accept=".pdf,.jpg,.jpeg,.png"
              onChange={handleChange}
            />
            
            <div className="w-16 h-16 mx-auto bg-primary-lightest dark:bg-slate-800 rounded-full flex items-center justify-center text-primary dark:text-primary-light mb-4">
              <Upload className="w-8 h-8" />
            </div>
            
            <p className="text-base font-semibold text-slate-700 dark:text-slate-300">
              Drag & drop report here, or <span className="text-primary hover:underline">browse</span>
            </p>
            <p className="text-xs text-slate-500 dark:text-slate-400 mt-2">
              Supports PDF, JPG, JPEG, PNG up to 10MB
            </p>
          </div>

          {file && (
            <div className="mt-6 p-4 rounded-xl bg-slate-50 dark:bg-slate-800/60 border border-slate-200 dark:border-slate-700 flex items-center justify-between">
              <div className="flex items-center space-x-3 min-w-0">
                <FileText className="w-8 h-8 text-primary flex-shrink-0" />
                <div className="truncate">
                  <p className="text-sm font-medium text-slate-800 dark:text-slate-200 truncate">{file.name}</p>
                  <p className="text-xs text-slate-500 dark:text-slate-400">{(file.size / (1024 * 1024)).toFixed(2)} MB</p>
                </div>
              </div>
              <button
                onClick={(e) => { e.stopPropagation(); setFile(null); }}
                className="text-xs text-slate-500 hover:text-red-500 font-semibold dark:text-slate-400 dark:hover:text-red-400"
              >
                Clear
              </button>
            </div>
          )}

          <div className="mt-8 flex justify-end">
            <button
              onClick={handleUpload}
              disabled={!file}
              className={`w-full sm:w-auto px-6 py-3 rounded-xl font-semibold shadow-sm transition flex items-center justify-center space-x-2 ${
                file 
                  ? 'bg-primary text-white hover:bg-primary-light' 
                  : 'bg-slate-100 text-slate-400 dark:bg-slate-800 dark:text-slate-600 cursor-not-allowed'
              }`}
            >
              <span>Analyze Report</span>
            </button>
          </div>
        </div>
      ) : (
        <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm p-10 text-center">
          <div className="inline-flex items-center justify-center relative mb-6">
            <div className="w-16 h-16 border-4 border-slate-200 border-t-primary rounded-full animate-spin"></div>
            <div className="absolute font-semibold text-primary text-xs">AI</div>
          </div>
          
          <h2 className="text-xl font-bold text-slate-900 dark:text-white mb-2">Analyzing Lab Results</h2>
          <p className="text-slate-600 dark:text-slate-400 max-w-md mx-auto text-sm mb-8">
            Please wait while MediInsight AI processes your clinical data. This takes 10-15 seconds.
          </p>

          <div className="max-w-md mx-auto space-y-4 text-left">
            {steps.map((stepText, idx) => {
              const isDone = uploadStep > idx;
              const isActive = uploadStep === idx;
              
              return (
                <div key={idx} className="flex items-center space-x-3">
                  {isDone ? (
                    <CheckCircle2 className="w-5 h-5 text-emerald-500 flex-shrink-0" />
                  ) : isActive ? (
                    <div className="w-5 h-5 rounded-full border-2 border-primary border-t-transparent animate-spin flex-shrink-0"></div>
                  ) : (
                    <div className="w-5 h-5 rounded-full border-2 border-slate-300 dark:border-slate-700 flex-shrink-0"></div>
                  )}
                  <span className={`text-sm ${
                    isDone ? 'text-slate-700 dark:text-slate-300 font-medium' : 
                    isActive ? 'text-primary dark:text-primary-light font-semibold' : 'text-slate-400 dark:text-slate-600'
                  }`}>
                    {stepText}
                  </span>
                </div>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
}
