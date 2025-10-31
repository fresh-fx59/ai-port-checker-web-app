import { useState } from 'react';
import { useAuth } from '../hooks/useAuth';
import PromptForm from '../components/PromptForm';
import ReportDisplay from '../components/ReportDisplay';
import QuotaDisplay from '../components/QuotaDisplay';
import ProcessingIndicator from '../components/ProcessingIndicator';
import { PromptResponse } from '../types';

function HomePage() {
  const { isAuthenticated } = useAuth();
  const [currentReport, setCurrentReport] = useState<PromptResponse | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  const handlePromptSubmit = async (response: PromptResponse) => {
    setCurrentReport(response);
  };

  return (
    <div className="max-w-4xl mx-auto">
      <div className="text-center mb-8">
        <h1 className="text-4xl font-bold text-gray-900 mb-4">
          Gemini AI Report Generator
        </h1>
        <p className="text-lg text-gray-600 mb-6">
          Generate AI-powered reports using Google's Gemini AI
        </p>
        
        {!isAuthenticated && (
          <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 mb-6">
            <p className="text-blue-800">
              You have <strong>1 free request</strong> as an anonymous user. 
              <a href="/register" className="text-blue-600 hover:text-blue-800 underline ml-1">
                Register
              </a> to get 4 additional requests!
            </p>
          </div>
        )}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <div className="lg:col-span-2">
          <PromptForm 
            onSubmit={handlePromptSubmit}
            isLoading={isLoading}
            setIsLoading={setIsLoading}
          />
          
          <div className="mt-8">
            <ProcessingIndicator isVisible={isLoading} />
          </div>
          
          {currentReport && !isLoading && (
            <div className="mt-8">
              <ReportDisplay response={currentReport} />
            </div>
          )}
        </div>
        
        <div className="lg:col-span-1">
          <QuotaDisplay />
          
          {isAuthenticated && (
            <div className="mt-6">
              <a 
                href="/history" 
                className="block w-full text-center bg-gray-100 hover:bg-gray-200 text-gray-800 font-medium py-2 px-4 rounded-lg transition-colors"
              >
                View Request History
              </a>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

export default HomePage;