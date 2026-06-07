import api from './axios';

export const getOrders = (params) =>
  api.get('/api/orders', { params });

export const getMyOrders = (params) =>
  api.get('/api/orders/my-orders', { params });

export const getOrder = (id) =>
  api.get(`/api/orders/${id}`);

export const getOrderByNumber = (orderNumber) =>
  api.get(`/api/orders/number/${orderNumber}`);

export const createOrder = (data) =>
  api.post('/api/orders', data);

export const updateOrderStatus = (id, status) =>
  api.put(`/api/orders/${id}/status`, null, { params: { status } });
