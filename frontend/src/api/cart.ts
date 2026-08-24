import api from './client';
import type { CartResponse } from '../types';

export async function getCart(): Promise<CartResponse> {
  const { data } = await api.get<CartResponse>('/cart');
  return data;
}

export async function addToCart(productId: number, quantity: number): Promise<CartResponse> {
  const { data } = await api.post<CartResponse>('/cart/items', { productId, quantity });
  return data;
}

export async function updateCartItem(productId: number, quantity: number): Promise<CartResponse> {
  const { data } = await api.put<CartResponse>(`/cart/items/${productId}`, { quantity });
  return data;
}

export async function removeCartItem(productId: number): Promise<CartResponse> {
  const { data } = await api.delete<CartResponse>(`/cart/items/${productId}`);
  return data;
}

export async function clearCart(): Promise<void> {
  await api.delete('/cart');
}
