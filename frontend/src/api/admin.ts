import api from './client';
import type {
  PagedResponse,
  AdminUser,
  CategoryRequest,
  CategoryResponse,
  ProductAdminResponse,
  ProductRequest,
  UpdateProductRequest,
  OrderStatus,
  GetOrderByIdResponse,
} from '../types';

export async function getUsers(params: {
  username?: string;
  role?: string;
  page?: number;
  size?: number;
}): Promise<PagedResponse<AdminUser>> {
  const { data } = await api.get<PagedResponse<AdminUser>>('/admin/users', { params });
  return data;
}

export async function createCategory(body: CategoryRequest): Promise<CategoryResponse> {
  const { data } = await api.post<CategoryResponse>('/admin/categories', body);
  return data;
}

export async function updateCategory(id: number, body: CategoryRequest): Promise<CategoryResponse> {
  const { data } = await api.put<CategoryResponse>(`/admin/categories/${id}`, body);
  return data;
}

export async function deleteCategory(id: number): Promise<string> {
  const { data } = await api.delete<string>(`/admin/categories/${id}`);
  return data;
}

export async function createProduct(
  categoryId: number,
  body: ProductRequest,
): Promise<ProductAdminResponse> {
  const { data } = await api.post<ProductAdminResponse>(
    `/admin/categories/${categoryId}/products`,
    body,
  );
  return data;
}

export async function updateProduct(
  id: number,
  body: UpdateProductRequest,
): Promise<ProductAdminResponse> {
  const { data } = await api.put<ProductAdminResponse>(`/admin/products/${id}`, body);
  return data;
}

export async function deleteProduct(id: number): Promise<string> {
  const { data } = await api.delete<string>(`/admin/products/${id}`);
  return data;
}

export async function getOrderById(id: number): Promise<GetOrderByIdResponse> {
  const { data } = await api.get<GetOrderByIdResponse>(`/order/${id}`);
  return data;
}

export async function updateOrderStatus(
  id: number,
  updatedStatus: OrderStatus,
): Promise<GetOrderByIdResponse> {
  const { data } = await api.put<GetOrderByIdResponse>(`/order/${id}/updateOrderStatus`, {
    updatedStatus,
  });
  return data;
}
