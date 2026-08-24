import api from './client';
import type { CreatePaymentOrderResponse } from '../types';

export async function createPaymentOrder(orderId: number, currency = 'INR'): Promise<CreatePaymentOrderResponse> {
  const { data } = await api.post<CreatePaymentOrderResponse>('/payment/create-order', {
    orderId,
    currency,
  });
  return data;
}
