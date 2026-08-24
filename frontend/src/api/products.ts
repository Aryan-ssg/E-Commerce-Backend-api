import api from './client';
import type { PagedResponse, ProductResponse, Category } from '../types';

export async function getProducts(params: {
  name?: string;
  categoryId?: number;
  minPrice?: number;
  maxPrice?: number;
  inStock?: boolean;
  page?: number;
  size?: number;
}): Promise<PagedResponse<ProductResponse>> {
  const { data } = await api.get<PagedResponse<ProductResponse>>('/public/products', { params });
  return data;
}

export async function getCategories(): Promise<Category[]> {
  const { data } = await api.get<Category[]>('/public/categories');
  return data;
}
