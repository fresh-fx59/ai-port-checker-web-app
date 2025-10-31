import { useState } from 'react';
import { PromptResponse } from '../types';
import { useAuth } from '../hooks/useAuth';
import { promptService } from '../services/promptService';
import { validatePrompt } from '../utils/validation';
import LoadingSpinner from './LoadingSpinner';
import CharacterCounter from './CharacterCounter';
import PromptTemplates from './PromptTemplates';

interface PromptFormProps {
  onSubmit: (response: PromptResponse) => void;
  isLoading: boolean;
  setIsLoading: (loading: boolean) => void;
}

function PromptForm({ onSubmit, isLoading, setIsLoading }: PromptFormProps) {
  const [prompt, setPrompt] = useState('');
  const [errors, setErrors] = useState<string[]>([]);
  const [showTemplates, setShowTemplates] = useState(false);
  const { userFingerprint, isAuthenticated } = useAuth();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrors([]);
    
    // Validate prompt
    const validation = validatePrompt(prompt);
    if (!validation.isValid) {
      setErrors(validation.errors);
      return;
    }

    setIsLoading(true);
    
    try {
      const response = await promptService.submitPrompt(
        prompt, 
        isAuthenticated ? undefined : userFingerprint
      );
      onSubmit(response);
      
      if (response.success) {
        setPrompt(''); // Clear form on success
      }
    } catch (error: any) {
      const errorResponse: PromptResponse = {
        success: false,
        error: error.response?.data?.error || 'Failed to process prompt. Please try again.',
        remainingRequests: 0
      };
      onSubmit(errorResponse);
    } finally {
      setIsLoading(false);
    }
  };

  const handleTemplateSelect = (template: string) => {
    setPrompt(template);
    setErrors([]);
  };

  return (
    <div className="bg-white rounded-lg shadow-md p-6">
      <h2 className="text-xl font-semibold text-gray-900 mb-4">
        Submit Your Prompt
      </h2>
      
      {errors.length > 0 && (
        <div className="bg-red-50 border border-red-200 rounded-md p-4 mb-4">
          <ul className="text-red-800 text-sm">
            {errors.map((error, index) => (
              <li key={index}>{error}</li>
            ))}
          </ul>
        </div>
      )}

      <div className="bg-blue-50 border border-blue-200 rounded-md p-4 mb-4">
        <h3 className="text-sm font-medium text-blue-800 mb-2">Prompt Guidelines:</h3>
        <ul className="text-blue-700 text-sm space-y-1">
          <li>• Be specific and detailed in your request</li>
          <li>• Include context and background information</li>
          <li>• Specify the format you want for the response</li>
          <li>• Minimum 10 characters, maximum 5000 characters</li>
        </ul>
      </div>
      
      <PromptTemplates
        onSelectTemplate={handleTemplateSelect}
        isVisible={showTemplates}
        onToggle={() => setShowTemplates(!showTemplates)}
      />

      <form onSubmit={handleSubmit}>
        <div className="mb-4">
          <label htmlFor="prompt" className="block text-sm font-medium text-gray-700 mb-2">
            Enter your prompt
          </label>
          <textarea
            id="prompt"
            value={prompt}
            onChange={(e) => setPrompt(e.target.value)}
            className={`w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent resize-none ${
              errors.length > 0 ? 'border-red-300' : 'border-gray-300'
            }`}
            rows={8}
            placeholder="Enter your detailed prompt here. Be specific about what you want the AI to generate..."
            disabled={isLoading}
          />
          <div className="mt-2">
            <CharacterCounter 
              current={prompt.length}
              max={5000}
              min={10}
            />
          </div>
        </div>
        
        <button
          type="submit"
          disabled={isLoading || prompt.length < 10 || prompt.length > 5000}
          className="w-full bg-blue-600 hover:bg-blue-700 disabled:bg-blue-400 text-white font-medium py-3 px-4 rounded-md transition-colors flex items-center justify-center"
        >
          {isLoading ? (
            <>
              <LoadingSpinner size="sm" className="mr-2" />
              Processing your request...
            </>
          ) : (
            'Generate AI Report'
          )}
        </button>
      </form>
    </div>
  );
}

export default PromptForm;