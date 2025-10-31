import { useState, useEffect } from 'react';
import { useAuth } from '../hooks/useAuth';
import { promptService } from '../services/promptService';
import { UserQuota } from '../types';

function QuotaDisplay() {
  const { isAuthenticated } = useAuth();
  const [quota, setQuota] = useState<UserQuota | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    loadQuota();
  }, [isAuthenticated]);

  const loadQuota = async () => {
    try {
      const quotaData = await promptService.getUserQuota();
      setQuota(quotaData);
    } catch (error) {
      console.error('Failed to load quota:', error);
    } finally {
      setIsLoading(false);
    }
  };

  if (isLoading) {
    return (
      <div className="bg-white rounded-lg shadow-md p-6">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">Request Quota</h3>
        <div className="animate-pulse">
          <div className="h-4 bg-gray-200 rounded w-3/4 mb-2"></div>
          <div className="h-4 bg-gray-200 rounded w-1/2"></div>
        </div>
      </div>
    );
  }

  if (!quota) {
    return (
      <div className="bg-white rounded-lg shadow-md p-6">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">Request Quota</h3>
        <p className="text-gray-600">Unable to load quota information</p>
      </div>
    );
  }

  const usedRequests = quota.totalRequests - quota.remainingRequests;
  const progressPercentage = (usedRequests / quota.totalRequests) * 100;

  return (
    <div className="bg-white rounded-lg shadow-md p-6">
      <h3 className="text-lg font-semibold text-gray-900 mb-4">Request Quota</h3>
      
      <div className="space-y-3">
        <div className="flex justify-between text-sm">
          <span className="text-gray-600">Remaining</span>
          <span className="font-medium text-gray-900">
            {quota.remainingRequests} of {quota.totalRequests}
          </span>
        </div>
        
        <div className="w-full bg-gray-200 rounded-full h-2">
          <div 
            className="bg-blue-600 h-2 rounded-full transition-all duration-300"
            style={{ width: `${progressPercentage}%` }}
          ></div>
        </div>
        
        <div className="text-xs text-gray-500">
          {quota.anonymous ? (
            <span>
              Anonymous user - <a href="/register" className="text-blue-600 hover:text-blue-800 underline">Register</a> for more requests
            </span>
          ) : (
            <span>Registered user quota</span>
          )}
        </div>
      </div>
    </div>
  );
}

export default QuotaDisplay;