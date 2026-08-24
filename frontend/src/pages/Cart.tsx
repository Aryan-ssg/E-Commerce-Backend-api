import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getCart, updateCartItem, removeCartItem, clearCart } from '../api/cart';
import type { CartResponse } from '../types';

const qtyBtnCls =
  'flex h-7 w-7 cursor-pointer items-center justify-center rounded border border-gray-300 bg-white';
const primaryBtnCls =
  'cursor-pointer rounded-md bg-blue-600 px-4 py-2.5 text-white hover:bg-blue-700';
const ghostBtnCls =
  'cursor-pointer rounded-md border border-gray-300 bg-white px-4 py-2.5 hover:bg-gray-50';

export default function Cart() {
  const navigate = useNavigate();
  const [cart, setCart] = useState<CartResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = () => {
    setLoading(true);
    getCart()
      .then(setCart)
      .catch(() => setError('Failed to load cart'))
      .finally(() => setLoading(false));
  };

  useEffect(load, []);

  const changeQty = async (productId: number, quantity: number) => {
    if (quantity < 1) return;
    const updated = await updateCartItem(productId, quantity);
    setCart(updated);
  };

  const remove = async (productId: number) => {
    const updated = await removeCartItem(productId);
    setCart(updated);
  };

  if (loading) return <p>Loading…</p>;
  if (error) return <p className="text-red-600">{error}</p>;
  if (!cart || cart.items.length === 0) return <p>Your cart is empty.</p>;

  return (
    <div>
      <h2 className="text-2xl font-bold">Cart</h2>
      <div className="flex flex-col gap-3">
        {cart.items.map((item) => (
          <div
            key={item.cartItemId}
            className="flex flex-col gap-3 rounded-xl border border-gray-200 bg-white p-3 sm:flex-row sm:items-center sm:justify-between"
          >
            <div>
              <strong>{item.productName}</strong>
              <div className="text-[13px] text-gray-500">₹{item.unitPrice} each</div>
            </div>
            <div className="flex flex-wrap items-center gap-2">
              <button onClick={() => changeQty(item.productId, item.quantity - 1)} className={qtyBtnCls}>
                −
              </button>
              <span>{item.quantity}</span>
              <button onClick={() => changeQty(item.productId, item.quantity + 1)} className={qtyBtnCls}>
                +
              </button>
              <span className="w-20 text-right">₹{item.lineTotal}</span>
              <button
                onClick={() => remove(item.productId)}
                className="cursor-pointer border-0 bg-transparent text-red-600"
              >
                Remove
              </button>
            </div>
          </div>
        ))}
      </div>

      <div className="mt-5 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <strong>Total: ₹{cart.totalPrice}</strong>
        <div className="flex gap-2">
          <button onClick={() => clearCart().then(load)} className={ghostBtnCls}>
            Clear
          </button>
          <button onClick={() => navigate('/checkout')} className={primaryBtnCls}>
            Checkout
          </button>
        </div>
      </div>
    </div>
  );
}
