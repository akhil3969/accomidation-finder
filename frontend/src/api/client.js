import axios from 'axios';

/**
 * One axios instance for the whole app.
 * In development VITE_API_BASE_URL is empty and Vite proxies /api to Spring Boot,
 * so there is no CORS round trip. In production it points at the deployed API.
 */
const baseURL = import.meta.env.VITE_API_BASE_URL || '';

export const TOKEN_KEY = 'accomfinder.token';

const client = axios.create({
  baseURL,
  headers: { 'Content-Type': 'application/json' },
  timeout: 20000,
});

export function getToken() {
  try {
    return window.localStorage.getItem(TOKEN_KEY);
  } catch {
    return null;
  }
}

export function setToken(token) {
  try {
    if (token) window.localStorage.setItem(TOKEN_KEY, token);
    else window.localStorage.removeItem(TOKEN_KEY);
  } catch {
    /* private browsing - the session simply will not survive a reload */
  }
}

client.interceptors.request.use((config) => {
  const token = getToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

/** Unwraps the backend's ApiError shape into a plain readable message. */
export function toMessage(error) {
  const data = error?.response?.data;
  if (!data) {
    return error?.message === 'Network Error'
      ? 'Cannot reach the server. Is the backend running on port 8080?'
      : error?.message || 'Something went wrong';
  }
  if (data.fieldErrors && Object.keys(data.fieldErrors).length) {
    return Object.values(data.fieldErrors).join(' ');
  }
  return data.message || data.error || 'Something went wrong';
}

let onUnauthorized = null;
export function registerUnauthorizedHandler(handler) {
  onUnauthorized = handler;
}

client.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error?.response?.status;
    const url = error?.config?.url || '';
    // Ignore the probe we fire on boot - a 401 there just means "not signed in".
    if (status === 401 && !url.includes('/api/auth/')) {
      setToken(null);
      if (onUnauthorized) onUnauthorized();
    }
    return Promise.reject(error);
  },
);

export default client;
