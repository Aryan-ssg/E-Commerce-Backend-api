import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';
import { tokenStorage, decodeJwtPayload } from './token';
import * as authApi from '../api/auth';

function decodeRoles(token?: string | null): string[] {
  const payload = decodeJwtPayload(token);
  const raw = payload?.role;
  const list = Array.isArray(raw) ? raw : raw ? [raw] : [];
  return (list as string[]).map((r) => r.replace(/^ROLE_/, ''));
}

interface AuthContextValue {
  isAuthenticated: boolean;
  roles: string[];
  isAdmin: boolean;
  login: (username: string, password: string) => Promise<void>;
  register: (username: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [isAuthenticated, setIsAuthenticated] = useState<boolean>(!!tokenStorage.getAccessToken());
  const [roles, setRoles] = useState<string[]>(() => decodeRoles(tokenStorage.getAccessToken()));

  const login = async (username: string, password: string) => {
    await authApi.login(username, password);
    setIsAuthenticated(true);
    setRoles(decodeRoles(tokenStorage.getAccessToken()));
  };

  const register = async (username: string, password: string) => {
    await authApi.register(username, password);
  };

  const logout = async () => {
    await authApi.logout();
    setIsAuthenticated(false);
    setRoles([]);
  };

  useEffect(() => {
    setIsAuthenticated(!!tokenStorage.getAccessToken());
    setRoles(decodeRoles(tokenStorage.getAccessToken()));
  }, []);

  return (
    <AuthContext.Provider
      value={{ isAuthenticated, roles, isAdmin: roles.includes('ADMIN'), login, register, logout }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within an AuthProvider');
  return ctx;
}
