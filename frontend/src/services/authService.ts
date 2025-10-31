import { apiService } from './api';
import { User, AuthResponse, LoginRequest, RegisterRequest } from '../types';

class AuthService {
  async login(email: string, password: string): Promise<AuthResponse> {
    const loginData: LoginRequest = { email, password };
    return apiService.post<AuthResponse>('/auth/login', loginData);
  }

  async register(email: string, password: string): Promise<AuthResponse> {
    const registerData: RegisterRequest = { 
      email, 
      password, 
      confirmPassword: password 
    };
    return apiService.post<AuthResponse>('/auth/email', registerData);
  }

  async logout(): Promise<void> {
    return apiService.post<void>('/auth/logout');
  }

  async getCurrentUser(): Promise<User> {
    return apiService.get<User>('/auth/me');
  }

  async googleCallback(code: string): Promise<AuthResponse> {
    return apiService.post<AuthResponse>('/auth/google', { code });
  }
}

export const authService = new AuthService();