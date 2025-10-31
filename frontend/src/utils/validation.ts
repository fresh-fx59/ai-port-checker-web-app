export interface ValidationResult {
  isValid: boolean;
  errors: string[];
}

export function validateEmail(email: string): ValidationResult {
  const errors: string[] = [];
  
  if (!email) {
    errors.push('Email is required');
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    errors.push('Please enter a valid email address');
  }
  
  return {
    isValid: errors.length === 0,
    errors,
  };
}

export function validatePassword(password: string): ValidationResult {
  const errors: string[] = [];
  
  if (!password) {
    errors.push('Password is required');
  } else if (password.length < 8) {
    errors.push('Password must be at least 8 characters long');
  } else if (!/(?=.*[a-z])(?=.*[A-Z])(?=.*\d)/.test(password)) {
    errors.push('Password must contain at least one uppercase letter, one lowercase letter, and one number');
  }
  
  return {
    isValid: errors.length === 0,
    errors,
  };
}

export function validatePrompt(prompt: string): ValidationResult {
  const errors: string[] = [];
  
  if (!prompt.trim()) {
    errors.push('Prompt is required');
  } else if (prompt.length < 10) {
    errors.push('Prompt must be at least 10 characters long');
  } else if (prompt.length > 5000) {
    errors.push('Prompt must be less than 5000 characters');
  }
  
  return {
    isValid: errors.length === 0,
    errors,
  };
}