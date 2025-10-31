export interface User {
  id: string;
  email?: string;
  googleId?: string;
  isAnonymous: boolean;
  fingerprint?: string;
  createdAt: string;
  updatedAt: string;
}

export interface AuthResponse {
  success: boolean;
  message?: string;
  user?: User;
  token?: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  confirmPassword: string;
}

export interface PromptRequest {
  prompt: string;
  fingerprint?: string;
}

export interface PromptResponse {
  success: boolean;
  report?: string;
  error?: string;
  remainingRequests: number;
}

export interface RequestLog {
  id: string;
  userPrompt: string;
  systemPrompt: string;
  aiResponse?: string;
  status: 'SUCCESS' | 'ERROR' | 'PENDING';
  errorMessage?: string;
  createdAt: string;
}

export interface UserQuota {
  remainingRequests: number;
  totalRequests: number;
  anonymous: boolean;
}

export interface ApiError {
  success: false;
  error: {
    code: string;
    message: string;
    details?: any;
  };
}