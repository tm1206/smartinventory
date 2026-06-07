import api from './axios';

export const getInventoryReport = () =>
  api.get('/api/reports/inventory');

export const getOrdersReport = (params) =>
  api.get('/api/reports/orders', { params });

export const exportInventory = () =>
  api.get('/api/reports/inventory/export');

export const exportOrders = (params) =>
  api.get('/api/reports/orders/export', { params });
