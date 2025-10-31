import { useState, useEffect } from 'react';
import { useAuth } from '../hooks/useAuth';
import { promptService } from '../services/promptService';
import { RequestLog } from '../types';
import LoadingSpinner from '../components/LoadingSpinner';

function HistoryPage() {
  const { isAuthenticated } = useAuth();
  const [history, setHistory] = useState<RequestLog[]>([]);
  const [filteredHistory, setFilteredHistory] = useState<RequestLog[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'SUCCESS' | 'ERROR' | 'PENDING'>('ALL');
  const [expandedItems, setExpandedItems] = useState<Set<string>>(new Set());

  useEffect(() => {
    if (isAuthenticated) {
      loadHistory();
    } else {
      setIsLoading(false);
    }
  }, [isAuthenticated]);

  useEffect(() => {
    // Filter history based on search term and status
    let filtered = history;
    
    if (searchTerm) {
      filtered = filtered.filter(item => 
        item.userPrompt.toLowerCase().includes(searchTerm.toLowerCase()) ||
        (item.aiResponse && item.aiResponse.toLowerCase().includes(searchTerm.toLowerCase()))
      );
    }
    
    if (statusFilter !== 'ALL') {
      filtered = filtered.filter(item => item.status === statusFilter);
    }
    
    setFilteredHistory(filtered);
  }, [history, searchTerm, statusFilter]);

  const loadHistory = async () => {
    try {
      const historyData = await promptService.getUserHistory();
      setHistory(historyData);
    } catch (err) {
      setError('Failed to load request history');
    } finally {
      setIsLoading(false);
    }
  };

  const toggleExpanded = (id: string) => {
    const newExpanded = new Set(expandedItems);
    if (newExpanded.has(id)) {
      newExpanded.delete(id);
    } else {
      newExpanded.add(id);
    }
    setExpandedItems(newExpanded);
  };

  const exportHistory = () => {
    const csvContent = [
      ['Date', 'Status', 'Prompt', 'Response'].join(','),
      ...filteredHistory.map(item => [
        new Date(item.createdAt).toLocaleString(),
        item.status,
        `"${item.userPrompt.replace(/"/g, '""')}"`,
        `"${(item.aiResponse || '').replace(/"/g, '""')}"`
      ].join(','))
    ].join('\n');
    
    const blob = new Blob([csvContent], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `gemini-history-${new Date().toISOString().split('T')[0]}.csv`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  };

  const copyToClipboard = (text: string) => {
    navigator.clipboard.writeText(text);
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleString();
  };

  if (!isAuthenticated) {
    return (
      <div className="max-w-4xl mx-auto text-center">
        <h1 className="text-3xl font-bold text-gray-900 mb-4">Request History</h1>
        <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-6">
          <p className="text-yellow-800">
            Please <a href="/login" className="text-yellow-600 hover:text-yellow-800 underline">sign in</a> to view your request history.
          </p>
        </div>
      </div>
    );
  }

  if (isLoading) {
    return (
      <div className="max-w-4xl mx-auto text-center">
        <h1 className="text-3xl font-bold text-gray-900 mb-8">Request History</h1>
        <LoadingSpinner size="lg" className="mx-auto" />
        <p className="text-gray-600 mt-4">Loading your request history...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="max-w-4xl mx-auto text-center">
        <h1 className="text-3xl font-bold text-gray-900 mb-4">Request History</h1>
        <div className="bg-red-50 border border-red-200 rounded-lg p-6">
          <p className="text-red-800">{error}</p>
          <button 
            onClick={loadHistory}
            className="mt-4 bg-red-600 hover:bg-red-700 text-white font-medium py-2 px-4 rounded-md transition-colors"
          >
            Try Again
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto">
      <div className="flex justify-between items-center mb-8">
        <h1 className="text-3xl font-bold text-gray-900">Request History</h1>
        {history.length > 0 && (
          <button
            onClick={exportHistory}
            className="bg-blue-600 hover:bg-blue-700 text-white font-medium py-2 px-4 rounded-md transition-colors flex items-center"
          >
            <svg className="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
            Export CSV
          </button>
        )}
      </div>
      
      {history.length === 0 ? (
        <div className="bg-gray-50 border border-gray-200 rounded-lg p-8 text-center">
          <svg className="w-16 h-16 text-gray-400 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
          </svg>
          <p className="text-gray-600 text-lg mb-2">No requests found</p>
          <p className="text-gray-500">
            <a href="/" className="text-blue-600 hover:text-blue-800 underline">
              Submit your first prompt
            </a> to see it here!
          </p>
        </div>
      ) : (
        <>
          {/* Search and Filter Controls */}
          <div className="bg-white rounded-lg shadow-sm border p-4 mb-6">
            <div className="flex flex-col md:flex-row gap-4">
              <div className="flex-1">
                <label htmlFor="search" className="block text-sm font-medium text-gray-700 mb-1">
                  Search requests
                </label>
                <input
                  type="text"
                  id="search"
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  placeholder="Search prompts and responses..."
                />
              </div>
              
              <div>
                <label htmlFor="status" className="block text-sm font-medium text-gray-700 mb-1">
                  Filter by status
                </label>
                <select
                  id="status"
                  value={statusFilter}
                  onChange={(e) => setStatusFilter(e.target.value as any)}
                  className="px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                >
                  <option value="ALL">All Status</option>
                  <option value="SUCCESS">Success</option>
                  <option value="ERROR">Error</option>
                  <option value="PENDING">Pending</option>
                </select>
              </div>
            </div>
            
            <div className="mt-3 text-sm text-gray-500">
              Showing {filteredHistory.length} of {history.length} requests
            </div>
          </div>

          {/* History Items */}
          <div className="space-y-4">
            {filteredHistory.map((request) => (
              <div key={request.id} className="bg-white border border-gray-200 rounded-lg shadow-sm overflow-hidden">
                <div className="p-4 border-b border-gray-100">
                  <div className="flex justify-between items-start">
                    <div className="flex items-center space-x-3">
                      <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                        request.status === 'SUCCESS' 
                          ? 'bg-green-100 text-green-800' 
                          : request.status === 'ERROR'
                          ? 'bg-red-100 text-red-800'
                          : 'bg-yellow-100 text-yellow-800'
                      }`}>
                        {request.status}
                      </span>
                      <span className="text-sm text-gray-500">
                        {formatDate(request.createdAt)}
                      </span>
                    </div>
                    
                    <div className="flex items-center space-x-2">
                      {request.aiResponse && (
                        <button
                          onClick={() => copyToClipboard(request.aiResponse!)}
                          className="text-gray-400 hover:text-gray-600 transition-colors"
                          title="Copy response to clipboard"
                        >
                          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z" />
                          </svg>
                        </button>
                      )}
                      
                      <button
                        onClick={() => toggleExpanded(request.id)}
                        className="text-gray-400 hover:text-gray-600 transition-colors"
                        title={expandedItems.has(request.id) ? "Collapse" : "Expand"}
                      >
                        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d={expandedItems.has(request.id) ? "M5 15l7-7 7 7" : "M19 9l-7 7-7-7"} />
                        </svg>
                      </button>
                    </div>
                  </div>
                  
                  {/* Prompt Preview */}
                  <div className="mt-3">
                    <p className="text-sm text-gray-600 line-clamp-2">
                      {request.userPrompt.length > 100 
                        ? `${request.userPrompt.substring(0, 100)}...` 
                        : request.userPrompt
                      }
                    </p>
                  </div>
                </div>
                
                {expandedItems.has(request.id) && (
                  <div className="p-4 space-y-4">
                    <div>
                      <h3 className="text-sm font-medium text-gray-700 mb-2">Full Prompt:</h3>
                      <div className="bg-gray-50 rounded-md p-3 max-h-40 overflow-y-auto">
                        <p className="text-sm text-gray-800 whitespace-pre-wrap">{request.userPrompt}</p>
                      </div>
                    </div>
                    
                    {request.status === 'SUCCESS' && request.aiResponse && (
                      <div>
                        <h3 className="text-sm font-medium text-gray-700 mb-2">AI Response:</h3>
                        <div className="bg-blue-50 rounded-md p-3 max-h-60 overflow-y-auto">
                          <p className="text-sm text-gray-800 whitespace-pre-wrap">{request.aiResponse}</p>
                        </div>
                      </div>
                    )}
                    
                    {request.status === 'ERROR' && request.errorMessage && (
                      <div>
                        <h3 className="text-sm font-medium text-red-700 mb-2">Error:</h3>
                        <div className="bg-red-50 rounded-md p-3">
                          <p className="text-sm text-red-800">{request.errorMessage}</p>
                        </div>
                      </div>
                    )}
                  </div>
                )}
              </div>
          ))}
          </div>
          
          {filteredHistory.length === 0 && history.length > 0 && (
            <div className="bg-gray-50 border border-gray-200 rounded-lg p-8 text-center">
              <p className="text-gray-600">No requests match your search criteria</p>
              <button
                onClick={() => {
                  setSearchTerm('');
                  setStatusFilter('ALL');
                }}
                className="mt-2 text-blue-600 hover:text-blue-800 underline"
              >
                Clear filters
              </button>
            </div>
          )}
        </>
      )}
    </div>
  );
}

export default HistoryPage;