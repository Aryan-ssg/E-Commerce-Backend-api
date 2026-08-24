import { useEffect, useState } from 'react';
import { getMyOrders, cancelOrder } from '../api/orders';
import type { GetOrderByIdResponse } from '../types';

const CANCELLABLE = new Set(['PENDING', 'PAID']);

function statusClass(status: string): string {
  if (status === 'CANCELLED') return 'text-[13px] font-semibold text-red-600';
  if (status === 'PAID' || status === 'DELIVERED') return 'text-[13px] font-semibold text-green-600';
  return 'text-[13px] font-semibold text-blue-600';
}

export default function Orders() {
  const [orders, setOrders] = useState<GetOrderByIdResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = () => {
    setLoading(true);
    getMyOrders()
      .then(setOrders)
      .catch(() => setError('Failed to load orders'))
      .finally(() => setLoading(false));
  };

  useEffect(load, []);

  const handleCancel = async (orderId: number) => {
    try {
      await cancelOrder(orderId);
      load();
    } catch {
      setError('Could not cancel order');
    }
  };

  if (loading) return <p>Loading…</p>;
  if (error) return <p className="text-red-600">{error}</p>;
  if (orders.length === 0) return <p>You have no orders yet.</p>;

  return (
    <div>
      <h2 className="text-2xl font-bold">My Orders</h2>
      <div className="flex flex-col gap-3">
        {orders.map((o) => (
          <div key={o.orderId} className="rounded-xl border border-gray-200 bg-white p-4 shadow-sm">
            <div className="flex justify-between">
              <strong>Order #{o.orderId}</strong>
              <span className={statusClass(o.orderStatus)}>{o.orderStatus}</span>
            </div>
            <div className="text-[13px] text-gray-500">
              {new Date(o.orderDateTime).toLocaleString()} · ₹{o.totalPrice}
            </div>
            <ul className="my-2">
              {o.orderItems.map((item, idx) => (
                <li key={idx}>
                  Product #{item.productId} × {item.quantity} @ ₹{item.priceAtCheckout}
                </li>
              ))}
            </ul>
            <div className="text-[13px] text-gray-600">
              Ship to: {[o.addressLine, o.landmark, `PIN ${o.pinCode}`].filter(Boolean).join(', ')}
            </div>
            {CANCELLABLE.has(o.orderStatus) && (
              <button
                onClick={() => handleCancel(o.orderId)}
                className="mt-2 cursor-pointer rounded-md border border-gray-300 bg-white px-3 py-1.5 hover:bg-gray-50"
              >
                Cancel order
              </button>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}
