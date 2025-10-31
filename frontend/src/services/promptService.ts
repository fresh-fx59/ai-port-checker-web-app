import { apiService } from './api';
import { PromptRequest, PromptResponse, RequestLog, UserQuota } from '../types';

class PromptService {
  async submitPrompt(prompt: string, fingerprint?: string): Promise<PromptResponse> {
    const promptData: PromptRequest = { prompt, fingerprint };
    return apiService.post<PromptResponse>('/prompt', promptData);
  }

  async getUserQuota(): Promise<UserQuota> {
    return apiService.get<UserQuota>('/user/quota');
  }

  async getUserHistory(): Promise<RequestLog[]> {
    return apiService.get<RequestLog[]>('/user/history');
  }
}

export const promptService = new PromptService();