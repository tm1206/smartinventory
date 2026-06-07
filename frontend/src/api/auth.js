import api from './axios';

export const login = (username, password) =>
  api.post('/api/auth/login', { username, password });

export const register = (data) =>
  api.post('/api/auth/register', data);

export const logout = () =>
  api.post('/api/auth/logout');

export const refresh = (refreshToken) =>
  api.post('/api/auth/refresh', { refreshToken });
