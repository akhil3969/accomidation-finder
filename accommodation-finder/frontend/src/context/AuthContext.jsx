import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { authApi, userApi } from '../api/endpoints';
import { getToken, registerUnauthorizedHandler, setToken } from '../api/client';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  const signOut = useCallback(() => {
    setToken(null);
    setUser(null);
  }, []);

  // Restore the session on a hard refresh.
  useEffect(() => {
    let cancelled = false;
    async function restore() {
      if (!getToken()) {
        setLoading(false);
        return;
      }
      try {
        const me = await authApi.me();
        if (!cancelled) setUser(me);
      } catch {
        setToken(null);
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    restore();
    return () => {
      cancelled = true;
    };
  }, []);

  // A 401 from anywhere in the app drops us back to signed-out state.
  useEffect(() => {
    registerUnauthorizedHandler(() => setUser(null));
  }, []);

  const signIn = useCallback(async (credentials) => {
    const result = await authApi.login(credentials);
    setToken(result.token);
    setUser(result.user);
    return result.user;
  }, []);

  const register = useCallback(async (payload) => {
    const result = await authApi.register(payload);
    setToken(result.token);
    setUser(result.user);
    return result.user;
  }, []);

  const becomeLandlord = useCallback(async () => {
    const updated = await userApi.becomeLandlord();
    setUser(updated);
    return updated;
  }, []);

  const value = useMemo(
    () => ({
      user,
      loading,
      signIn,
      register,
      signOut,
      becomeLandlord,
      updateUser: setUser,
      isAuthenticated: Boolean(user),
      isLandlord: user?.role === 'LANDLORD' || user?.role === 'ADMIN',
      isAdmin: user?.role === 'ADMIN',
    }),
    [user, loading, signIn, register, signOut, becomeLandlord],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used inside <AuthProvider>');
  }
  return context;
}
