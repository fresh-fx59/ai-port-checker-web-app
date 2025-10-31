import { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { User, AuthResponse } from '../types';
import { authService } from '../services/authService';
import { getStoredFingerprint } from '../utils/fingerprint';

interface AuthContextType {
  user: User | null;
  isLoading: boolean;
  isAuthenticated: boolean;
  isAnonymous: boolean;
  userFingerprint: string;
  login: (email: string, password: string) => Promise<AuthResponse>;
  register: (email: string, password: string) => Promise<AuthResponse>;
  logout: () => Promise<void>;
  loginWithGoogle: () => void;
  refreshUser: () => Promise<void>;
  createAnonymousUser: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

interface AuthProviderProps {
  children: ReactNode;
}

export function AuthProvider({ children }: AuthProviderProps) {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [userFingerprint] = useState(() => getStoredFingerprint());

  const isAuthenticated = !!user && !user.isAnonymous;
  const isAnonymous = !!user && user.isAnonymous;

  useEffect(() => {
    // Check if user is already authenticated on app load
    checkAuthStatus();
  }, []);

  const checkAuthStatus = async () => {
    try {
      const token = localStorage.getItem('authToken');
      if (token) {
        const userData = await authService.getCurrentUser();
        setUser(userData);
      } else {
        // No token, create anonymous user for tracking
        createAnonymousUser();
      }
    } catch (error) {
      // Token might be expired or invalid
      localStorage.removeItem('authToken');
      createAnonymousUser();
    } finally {
      setIsLoading(false);
    }
  };

  const login = async (email: string, password: string): Promise<AuthResponse> => {
    try {
      const response = await authService.login(email, password);
      if (response.success && response.user && response.token) {
        setUser(response.user);
        localStorage.setItem('authToken', response.token);
      }
      return response;
    } catch (error) {
      return {
        success: false,
        message: 'Login failed. Please try again.'
      };
    }
  };

  const register = async (email: string, password: string): Promise<AuthResponse> => {
    try {
      const response = await authService.register(email, password);
      if (response.success && response.user && response.token) {
        setUser(response.user);
        localStorage.setItem('authToken', response.token);
      }
      return response;
    } catch (error) {
      return {
        success: false,
        message: 'Registration failed. Please try again.'
      };
    }
  };

  const logout = async () => {
    try {
      await authService.logout();
    } catch (error) {
      console.error('Logout error:', error);
    } finally {
      setUser(null);
      localStorage.removeItem('authToken');
    }
  };

  const loginWithGoogle = () => {
    // Redirect to Google OAuth endpoint
    window.location.href = '/api/auth/google';
  };

  const refreshUser = async () => {
    try {
      const userData = await authService.getCurrentUser();
      setUser(userData);
    } catch (error) {
      console.error('Failed to refresh user data:', error);
    }
  };

  const createAnonymousUser = () => {
    // Create a temporary anonymous user for tracking
    const anonymousUser: User = {
      id: userFingerprint,
      isAnonymous: true,
      fingerprint: userFingerprint,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };
    setUser(anonymousUser);
  };

  const value: AuthContextType = {
    user,
    isLoading,
    isAuthenticated,
    isAnonymous,
    userFingerprint,
    login,
    register,
    logout,
    loginWithGoogle,
    refreshUser,
    createAnonymousUser,
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextType {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}