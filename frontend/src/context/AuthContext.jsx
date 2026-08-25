import { createContext, useContext, useState, useCallback } from 'react';
import { login as loginApi } from '../api/authApi';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const stored = localStorage.getItem('hrms_user');
    return stored ? JSON.parse(stored) : null;
  });

  const login = useCallback(async (email, password) => {
    const { data } = await loginApi(email, password);
    localStorage.setItem('hrms_token', data.token);
    localStorage.setItem('hrms_user', JSON.stringify(data));
    setUser(data);
    return data;
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem('hrms_token');
    localStorage.removeItem('hrms_user');
    setUser(null);
  }, []);

  const isHrOrAdmin = user && (user.role === 'HR' || user.role === 'ADMIN');

  return (
    <AuthContext.Provider value={{ user, login, logout, isHrOrAdmin }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
