export interface ProductResponse {
  productId: number;
  productName: string;
  productPrice: number;
  categoryName: string;
  imageUrl?: string;
}

export interface PagedResponse<T> {
  content: T[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface Category {
  categoryId: number;
  categoryName: string;
  products: { productId: number; productName: string; productPrice: number }[];
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
}

export type OrderStatus =
  | 'PENDING'
  | 'PAID'
  | 'PROCESSING'
  | 'SHIPPED'
  | 'DELIVERED'
  | 'CANCELLED';

export interface CartItemResponse {
  cartItemId: number;
  productId: number;
  productName: string;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
}

export interface CartResponse {
  cartId: number;
  items: CartItemResponse[];
  totalPrice: number;
}

export interface OrderItemsResponse {
  quantity: number;
  productId: number;
  priceAtCheckout: number;
}

export interface GetOrderByIdResponse {
  orderId: number;
  totalPrice: number;
  addressLine: string;
  pinCode: string;
  landmark?: string;
  orderDateTime: string;
  orderStatus: OrderStatus;
  orderItems: OrderItemsResponse[];
}

export interface PlaceOrderResponse {
  orderId: number;
  totalPrice: number;
  addressLine: string;
  pinCode: string;
  landmark?: string;
  orderDateTime: string;
  orderStatus: OrderStatus;
  orderItems: OrderItemsResponse[];
  razorpayOrderId: string;
  razorpayKeyId: string;
}

export interface AdminUser {
  userId: number;
  username: string;
  role: string;
}

export interface CategoryRequest {
  categoryName: string;
}

export interface CategoryResponse {
  categoryId: number;
  categoryName: string;
}

export interface ProductAdminResponse {
  productId: number;
  productName: string;
  productPrice: number;
  categoryName: string;
  stock: number;
  imageUrl?: string;
}

export interface ProductRequest {
  productName: string;
  productPrice: number;
  stock: number;
  imageUrl?: string;
}

export interface UpdateProductRequest {
  productName: string;
  productPrice: number;
  imageUrl?: string;
}

export interface CreatePaymentOrderResponse {
  razorpayOrderId: string;
  amount: number;
  currency: string;
  keyId: string;
  receipt: string;
}
