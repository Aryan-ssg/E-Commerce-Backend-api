import api from './client';
import type { GetOrderByIdResponse, PlaceOrderResponse } from '../types';

export interface PlaceOrderItem {
  productId: number;
  quantity: number;
}

export interface PlaceOrderBody {
  orderItems: PlaceOrderItem[];
  addressLine: string;
  pinCode: string;
  landmark?: string;
}

export async function placeOrder(body: PlaceOrderBody): Promise<PlaceOrderResponse> {
  const { data } = await api.post<PlaceOrderResponse>('/order/place', body);
  return data;
}

export async function getMyOrders(): Promise<GetOrderByIdResponse[]> {
  const { data } = await api.get<GetOrderByIdResponse[]>('/order/my-orders');
  return data;
}

export async function cancelOrder(orderId: number): Promise<void> {
  await api.put(`/order/${orderId}/cancel`);
}

export async function verifyPayment(
  orderId: number,
  payload: { razorpayOrderId: string; razorpayPaymentId: string; razorpaySignature: string }
): Promise<PlaceOrderResponse> {
  const { data } = await api.post<PlaceOrderResponse>(
    `/order/${orderId}/verify-payment`,
    payload
  );
  return data;
}
