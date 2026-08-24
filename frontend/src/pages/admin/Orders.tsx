import { useState } from 'react';
import * as adminApi from '../../api/admin';
import type { GetOrderByIdResponse, OrderStatus } from '../../types';

const STATUSES: OrderStatus[] = [
  'PENDING',
  'PAID',
  'PROCESSING',
  'SHIPPED',
  'DELIVERED',
  'CANCELLED',
];

const inputCls =
  'rounded-md border border-gray-300 px-2 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500';
const btnCls =
  'cursor-pointer rounded-md border border-gray-300 bg-gray-50 px-3 py-1.5 text-sm hover:bg-gray-100';
const thCls = 'border-b-2 border-gray-200 px-2 py-2 text-left text-sm font-semibold';
const tdCls = 'border-b border-gray-100 px-2 py-2 text-sm';

export default function AdminOrders() {
  const [orderId, setOrderId] = useState('');
  const [order, setOrder] = useState<GetOrderByIdResponse | null>(null);
  const [newStatus, setNewStatus] = useState<OrderStatus | ''>('');
  const [error, setError] = useState('');

  async function handleSearch() {
    setError('');
    setOrder(null);
    const id = Number(orderId);
    if (!id) {
      setError('Enter a numeric order ID');
      return;
    }
    try {
      const o = await adminApi.getOrderById(id);
      setOrder(o);
      setNewStatus('');
    } catch (e: any) {
      setError(e?.response?.data ?? 'Order not found');
    }
  }

  async function handleUpdate() {
    if (!order || newStatus === '') return;
    setError('');
    try {
      const o = await adminApi.updateOrderStatus(order.orderId, newStatus);
      setOrder(o);
      setNewStatus('');
    } catch (e: any) {
      setError(e?.response?.data ?? 'Failed to update status');
    }
  }

  return (
    <div>
      <h2 className="text-xl font-semibold">Orders</h2>
      <div className="mb-3 flex gap-2">
        <input
          placeholder="Order ID"
          value={orderId}
          onChange={(e) => setOrderId(e.target.value)}
          className={inputCls}
        />
        <button onClick={handleSearch} className={btnCls}>
          Lookup
        </button>
      </div>

      {error && <p className="text-red-600">{error}</p>}

      {order && (
        <div className="rounded-lg border border-gray-200 p-4">
          <div className="flex items-center justify-between">
            <div>
              <strong>Order #{order.orderId}</strong> — {order.orderStatus}
            </div>
            <div className="flex items-center gap-2">
              <select
                value={newStatus}
                onChange={(e) => setNewStatus(e.target.value as OrderStatus)}
                className={inputCls}
              >
                <option value="">Change status…</option>
                {STATUSES.map((s) => (
                  <option key={s} value={s}>
                    {s}
                  </option>
                ))}
              </select>
              <button disabled={newStatus === ''} onClick={handleUpdate} className={btnCls}>
                Update
              </button>
            </div>
          </div>

          <p className="my-2">Total: ₹{order.totalPrice}</p>
          <p className="my-2">
            Ship to: {[order.addressLine, order.landmark, `PIN ${order.pinCode}`].filter(Boolean).join(', ')}
          </p>

          <div className="overflow-x-auto">
          <table className="w-full border-collapse">
            <thead>
              <tr>
                <th className={thCls}>Product</th>
                <th className={thCls}>Qty</th>
                <th className={thCls}>Price</th>
              </tr>
            </thead>
            <tbody>
              {order.orderItems.map((it, idx) => (
                <tr key={idx}>
                  <td className={tdCls}>{it.productId}</td>
                  <td className={tdCls}>{it.quantity}</td>
                  <td className={tdCls}>{it.priceAtCheckout}</td>
                </tr>
              ))}
            </tbody>
          </table>
          </div>
        </div>
      )}
    </div>
  );
}
