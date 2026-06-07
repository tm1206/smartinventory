import api from './axios';

export const getProducts = (params) =>
  api.get('/api/products', { params });

export const getProduct = (id) =>
  api.get(`/api/products/${id}`);

export const getProductBySku = (sku) =>
  api.get(`/api/products/sku/${sku}`);

export const getCategories = () =>
  api.get('/api/products/categories');

export const getLowStockProducts = () =>
  api.get('/api/products/low-stock');

export const createProduct = (data) =>
  api.post('/api/products', data);

export const updateProduct = (id, data) =>
  api.put(`/api/products/${id}`, data);

export const deleteProduct = (id) =>
  api.delete(`/api/products/${id}`);
