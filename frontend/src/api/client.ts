import axios, { AxiosError } from 'axios';
import { tokenStorage } from '../auth/token';

// Shared axios instance. All calls are relative to /api and are proxied to
// the Spring backend by the Vite dev server.
const api = axios.create({ baseURL: '/api' });

// Attach the access token to every outgoing request.
api.interceptors.request.use((config) => {
  const token = tokenStorage.getAccessToken();
  if (token) {
    config.headers = config.headers ?? {};
    (config.headers as any).Authorization = `Bearer ${token}`;
  }
  return config;
});

// On 401, attempt a single silent refresh, then retry the original request.
// Concurrent 401s are queued behind the in-flight refresh.
let isRefreshing = false;
let pendingQueue: ((token: string | null) => void)[] = [];

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const original: any = error.config;

    if (error.response?.status === 401 && original && !original._retry) {
      // Never try to refresh from the refresh endpoint itself.
      if (original.url?.includes('/public/refresh')) {
        tokenStorage.clear();
        window.location.assign('/login');
        return Promise.reject(error);
      }

      if (!isRefreshing) {
        isRefreshing = true;
        const refreshToken = tokenStorage.getRefreshToken();
        try {
          const { data } = await axios.post('/api/public/refresh', { refreshToken });
          tokenStorage.setTokens(data.accessToken, data.refreshToken);
          isRefreshing = false;
          pendingQueue.forEach((cb) => cb(data.accessToken));
          pendingQueue = [];
          original._retry = true;
          original.headers.Authorization = `Bearer ${data.accessToken}`;
          return api(original);
        } catch (refreshError) {
          isRefreshing = false;
          tokenStorage.clear();
          pendingQueue.forEach((cb) => cb(null));
          pendingQueue = [];
          window.location.assign('/login');
          return Promise.reject(refreshError);
        }
      }

      // A refresh is already in flight: queue this request.
      return new Promise((resolve, reject) => {
        pendingQueue.push((token) => {
          if (token) {
            original._retry = true;
            original.headers.Authorization = `Bearer ${token}`;
            resolve(api(original));
          } else {
            reject(error);
          }
        });
      });
    }

    return Promise.reject(error);
  }
);

export default api;
