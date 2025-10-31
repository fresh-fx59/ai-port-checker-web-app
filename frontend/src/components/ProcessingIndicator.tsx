import { useState, useEffect } from 'react';
import LoadingSpinner from './LoadingSpinner';

interface ProcessingIndicatorProps {
  isVisible: boolean;
}

function ProcessingIndicator({ isVisible }: ProcessingIndicatorProps) {
  const [currentStep, setCurrentStep] = useState(0);
  const [dots, setDots] = useState('');

  const steps = [
    'Validating your prompt...',
    'Sending to Gemini AI...',
    'Processing your request...',
    'Generating response...',
    'Almost done...'
  ];

  useEffect(() => {
    if (!isVisible) {
      setCurrentStep(0);
      setDots('');
      return;
    }

    // Animate dots
    const dotsInterval = setInterval(() => {
      setDots(prev => prev.length >= 3 ? '' : prev + '.');
    }, 500);

    // Progress through steps
    const stepInterval = setInterval(() => {
      setCurrentStep(prev => (prev + 1) % steps.length);
    }, 2000);

    return () => {
      clearInterval(dotsInterval);
      clearInterval(stepInterval);
    };
  }, [isVisible, steps.length]);

  if (!isVisible) return null;

  return (
    <div className="bg-blue-50 border border-blue-200 rounded-lg p-6 text-center">
      <LoadingSpinner size="lg" className="mx-auto mb-4" />
      <h3 className="text-lg font-semibold text-blue-900 mb-2">
        Processing Your Request
      </h3>
      <p className="text-blue-700">
        {steps[currentStep]}{dots}
      </p>
      <div className="mt-4 bg-blue-200 rounded-full h-2">
        <div 
          className="bg-blue-600 h-2 rounded-full transition-all duration-1000 ease-in-out"
          style={{ width: `${((currentStep + 1) / steps.length) * 100}%` }}
        ></div>
      </div>
      <p className="text-sm text-blue-600 mt-2">
        This may take a few moments...
      </p>
    </div>
  );
}

export default ProcessingIndicator;