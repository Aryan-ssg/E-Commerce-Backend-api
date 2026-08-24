import api from './client';
import { tokenStorage } from '../auth/token';
import type { LoginResponse } from '../types';

export async function login(username: string, password: string): Promise<LoginResponse> {
  const { data } = await api.post<LoginResponse>('/public/login', { username, password });
  tokenStorage.setTokens(data.accessToken, data.refreshToken);
  return data;
}

export async function register(username: string, password: string): Promise<string> {
  const { data } = await api.post<string>('/public/register', { username, password });
  return data;
}

export async function logout(): Promise<void> {
  try {
    await api.post('/logout');
  } finally {
    tokenStorage.clear();
  }
}
