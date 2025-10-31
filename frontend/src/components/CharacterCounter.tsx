interface CharacterCounterProps {
  current: number;
  max: number;
  min?: number;
  className?: string;
}

function CharacterCounter({ current, max, min = 0, className = '' }: CharacterCounterProps) {
  const getColor = () => {
    if (current < min) return 'text-red-500';
    if (current > max * 0.9) return 'text-yellow-500';
    if (current > max) return 'text-red-500';
    return 'text-gray-500';
  };

  const getStatus = () => {
    if (current < min) return `Need ${min - current} more`;
    if (current > max) return `${current - max} over limit`;
    if (current > max * 0.9) return `${max - current} remaining`;
    return 'Good';
  };

  const percentage = Math.min((current / max) * 100, 100);

  return (
    <div className={`space-y-2 ${className}`}>
      <div className="flex justify-between items-center">
        <span className={`text-sm ${getColor()}`}>
          {getStatus()}
        </span>
        <span className={`text-sm ${getColor()}`}>
          {current}/{max}
        </span>
      </div>
      
      <div className="w-full bg-gray-200 rounded-full h-1">
        <div 
          className={`h-1 rounded-full transition-all duration-300 ${
            current > max 
              ? 'bg-red-500' 
              : current > max * 0.9 
              ? 'bg-yellow-500' 
              : 'bg-blue-500'
          }`}
          style={{ width: `${percentage}%` }}
        ></div>
      </div>
    </div>
  );
}

export default CharacterCounter;