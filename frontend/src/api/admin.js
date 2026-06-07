import api from './axios';

export const getUsers = (params) =>
  api.get('/api/admin/users', { params });

export const getUser = (id) =>
  api.get(`/api/admin/users/${id}`);

export const updateUserRole = (id, role) =>
  api.put(`/api/admin/users/${id}/role`, null, { params: { role } });

export const updateUserStatus = (id, active) =>
  api.put(`/api/admin/users/${id}/status`, null, { params: { active } });

export const getAuditLogs = (params) =>
  api.get('/api/admin/audit-logs', { params });
