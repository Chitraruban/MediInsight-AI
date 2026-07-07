import React, { useState, useEffect, useRef } from 'react';
import axios from 'axios';
import { Send, Bot, User, Sparkles, FileText, AlertCircle, Trash2 } from 'lucide-react';

export default function ChatPage({ initialReportId }) {
  const [reportId, setReportId] = useState(initialReportId || null);
  const [reports, setReports] = useState([]);
  const [messages, setMessages] = useState([]);
  const [inputValue, setInputValue] = useState('');
  const [sending, setSending] = useState(false);
  const [loadingHistory, setLoadingHistory] = useState(false);
  const [error, setError] = useState(null);
  
  const messagesEndRef = useRef(null);

  useEffect(() => {
    fetchAvailableReports();
  }, []);

  useEffect(() => {
    fetchChatHistory();
  }, [reportId]);

  useEffect(() => {
    scrollToBottom();
  }, [messages, sending]);

  const fetchAvailableReports = async () => {
    try {
      const response = await axios.get('/api/reports');
      setReports(response.data);
    } catch (err) {
      console.error("Failed to load reports for chat context:", err);
    }
  };

  const fetchChatHistory = async () => {
    setLoadingHistory(true);
    setError(null);
    try {
      const url = reportId ? `/api/chat/history?reportId=${reportId}` : '/api/chat/history';
      const response = await axios.get(url);
      
      // Map API messages (USER/ASSISTANT) to frontend format
      const formatted = response.data.map(m => ({
        role: m.role.toLowerCase(), // 'user' or 'assistant'
        content: m.content
      }));
      
      if (formatted.length === 0) {
        // Add welcome message
        setMessages([
          {
            role: 'assistant',
            content: reportId 
              ? "Hi! I've loaded the medical report summary as context. Feel free to ask me questions like 'What do these results mean?', 'What are my abnormal markers?', or 'What changes should I make?'"
              : "Hi there! I'm your MediInsight AI assistant. Ask me general health questions or link a report from the selector above to discuss specific results."
          }
        ]);
      } else {
        setMessages(formatted);
      }
    } catch (err) {
      setError("Could not retrieve past chat logs. Starting fresh conversation.");
      setMessages([
        {
          role: 'assistant',
          content: "Hi there! I'm your MediInsight AI assistant. Ask me anything about health education."
        }
      ]);
    } finally {
      setLoadingHistory(false);
    }
  };

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  const handleSendMessage = async (e) => {
    e.preventDefault();
    if (!inputValue.trim() || sending) return;

    const userText = inputValue;
    setInputValue('');
    
    // Add user message to state
    const updatedMessages = [...messages, { role: 'user', content: userText }];
    setMessages(updatedMessages);
    setSending(true);

    // Build history for the Gemini call (exclude welcome message or failed logs if desired, but passing all is standard)
    // Map roles to exact USER / ASSISTANT values expected by backend
    const apiHistory = updatedMessages.slice(0, -1).map(m => ({
      role: m.role.toUpperCase(),
      content: m.content
    }));

    try {
      const response = await axios.post('/api/chat', {
        reportId: reportId,
        message: userText,
        history: apiHistory
      });

      setMessages(prev => [...prev, { role: 'assistant', content: response.data.reply }]);
    } catch (err) {
      console.error(err);
      setMessages(prev => [...prev, { 
        role: 'assistant', 
        content: "Sorry, I encountered an error while processing that message. Please try setting your GEMINI_API_KEY environment variable or verify connection." 
      }]);
    } finally {
      setSending(false);
    }
  };

  const selectedReport = reports.find(r => r.id === reportId);

  return (
    <div className="max-w-4xl mx-auto px-4 py-8 h-[calc(100vh-140px)] flex flex-col">
      {/* Context Selector Header */}
      <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-4 rounded-t-2xl flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="flex items-center space-x-2.5">
          <Bot className="w-6 h-6 text-primary" />
          <div>
            <h1 className="text-base font-bold text-slate-900 dark:text-white">AI Health Advisor</h1>
            <p className="text-xs text-slate-500">Ask questions, request summaries, and explore topics.</p>
          </div>
        </div>

        <div className="flex items-center space-x-2">
          <span className="text-xs font-semibold text-slate-400">Context:</span>
          <select
            value={reportId || ''}
            onChange={(e) => setReportId(e.target.value ? Number(e.target.value) : null)}
            className="text-xs bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 px-3 py-1.5 rounded-lg font-medium text-slate-700 dark:text-slate-200 focus:outline-none focus:ring-1 focus:ring-primary"
          >
            <option value="">General (No Context)</option>
            {reports.map((r) => (
              <option key={r.id} value={r.id}>
                Report: {r.fileName}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Message Area */}
      <div className="flex-1 bg-slate-50 dark:bg-slate-900/40 border-x border-slate-200 dark:border-slate-800 overflow-y-auto p-4 space-y-4">
        {loadingHistory ? (
          <div className="flex items-center justify-center h-full">
            <div className="w-8 h-8 border-2 border-slate-200 border-t-primary rounded-full animate-spin"></div>
          </div>
        ) : (
          messages.map((msg, index) => {
            const isBot = msg.role === 'assistant';
            return (
              <div
                key={index}
                className={`flex space-x-3 max-w-[85%] ${isBot ? 'mr-auto' : 'ml-auto flex-row-reverse space-x-reverse'}`}
              >
                <div className={`w-8 h-8 rounded-lg flex items-center justify-center flex-shrink-0 ${
                  isBot 
                    ? 'bg-primary-lightest dark:bg-slate-800 text-primary dark:text-primary-light' 
                    : 'bg-primary text-white'
                }`}>
                  {isBot ? <Bot className="w-4.5 h-4.5" /> : <User className="w-4.5 h-4.5" />}
                </div>
                
                <div className={`p-3.5 rounded-2xl shadow-sm text-sm leading-relaxed ${
                  isBot 
                    ? 'bg-white dark:bg-slate-800/80 border border-slate-100 dark:border-slate-750 text-slate-800 dark:text-slate-200 rounded-tl-none' 
                    : 'bg-primary-light text-white rounded-tr-none'
                }`}>
                  {msg.content}
                </div>
              </div>
            );
          })
        )}

        {sending && (
          <div className="flex space-x-3 mr-auto max-w-[80%]">
            <div className="w-8 h-8 rounded-lg bg-primary-lightest dark:bg-slate-800 text-primary dark:text-primary-light flex items-center justify-center flex-shrink-0">
              <Bot className="w-4.5 h-4.5 animate-pulse" />
            </div>
            <div className="bg-white dark:bg-slate-800/80 border border-slate-100 dark:border-slate-750 p-3.5 rounded-2xl rounded-tl-none flex items-center space-x-1.5 shadow-sm">
              <div className="w-2 h-2 bg-slate-400 dark:bg-slate-500 rounded-full animate-bounce" style={{ animationDelay: '0ms' }}></div>
              <div className="w-2 h-2 bg-slate-400 dark:bg-slate-500 rounded-full animate-bounce" style={{ animationDelay: '150ms' }}></div>
              <div className="w-2 h-2 bg-slate-400 dark:bg-slate-500 rounded-full animate-bounce" style={{ animationDelay: '300ms' }}></div>
            </div>
          </div>
        )}
        
        <div ref={messagesEndRef} />
      </div>

      {/* Input Tray Footer */}
      <form
        onSubmit={handleSendMessage}
        className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-4 rounded-b-2xl flex items-center space-x-3"
      >
        <input
          type="text"
          value={inputValue}
          onChange={(e) => setInputValue(e.target.value)}
          placeholder={reportId ? `Ask about "${selectedReport?.fileName || 'report'}"...` : "Type a health educational question..."}
          className="flex-1 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 px-4 py-3 rounded-xl text-sm text-slate-850 dark:text-slate-200 placeholder-slate-400 dark:placeholder-slate-500 focus:outline-none focus:ring-1 focus:ring-primary"
          disabled={sending}
        />
        <button
          type="submit"
          disabled={!inputValue.trim() || sending}
          className={`p-3 rounded-xl flex items-center justify-center transition ${
            inputValue.trim() && !sending
              ? 'bg-primary text-white hover:bg-primary-light'
              : 'bg-slate-100 text-slate-400 dark:bg-slate-850 dark:text-slate-700 cursor-not-allowed'
          }`}
        >
          <Send className="w-4 h-4" />
        </button>
      </form>
    </div>
  );
}
