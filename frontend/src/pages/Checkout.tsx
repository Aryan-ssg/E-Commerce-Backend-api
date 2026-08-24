import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getCart } from '../api/cart';
import { placeOrder, verifyPayment } from '../api/orders';
import { createPaymentOrder } from '../api/payment';
import { loadRazorpayScript, openRazorpay } from '../utils/razorpay';
import type { CartResponse } from '../types';

const payBtnCls =
  'mt-4 w-full cursor-pointer rounded-md bg-green-600 px-4 py-3 text-[16px] text-white disabled:opacity-60';
const inputCls =
  'w-full rounded-md border border-gray-300 px-3 py-2.5 text-[15px] focus:outline-none focus:ring-2 focus:ring-blue-500';

interface FieldErrors {
  addressLine?: string;
  pinCode?: string;
}

const PIN_RE = /^[1-9][0-9]{5}$/;

export default function Checkout() {
  const navigate = useNavigate();
  const [cart, setCart] = useState<CartResponse | null>(null);
  const [addressLine, setAddressLine] = useState('');
  const [pinCode, setPinCode] = useState('');
  const [landmark, setLandmark] = useState('');
  const [errors, setErrors] = useState<FieldErrors>({});
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [done, setDone] = useState(false);

  useEffect(() => {
    getCart()
      .then(setCart)
      .catch(() => setError('Failed to load cart'));
  }, []);

  const validate = (): boolean => {
    const e: FieldErrors = {};
    if (!addressLine.trim()) e.addressLine = 'Address is required';
    if (!pinCode.trim()) e.pinCode = 'PIN code is required';
    else if (!PIN_RE.test(pinCode.trim())) e.pinCode = 'Enter a valid 6-digit PIN code';
    setErrors(e);
    return Object.keys(e).length === 0;
  };

  const handlePay = async () => {
    if (!cart || cart.items.length === 0) {
      setError('Your cart is empty');
      return;
    }
    setError('');
    if (!validate()) return;

    setBusy(true);
    try {
      const order = await placeOrder({
        orderItems: cart.items.map((i) => ({ productId: i.productId, quantity: i.quantity })),
        addressLine: addressLine.trim(),
        pinCode: pinCode.trim(),
        landmark: landmark.trim() || undefined,
      });

      const payment = await createPaymentOrder(order.orderId);

      await loadRazorpayScript();
      const result = await openRazorpay({
        key: payment.keyId,
        amount: payment.amount,
        currency: payment.currency,
        order_id: payment.razorpayOrderId,
        name: 'Ecommerce',
        theme: { color: '#2563eb' },
      });

      await verifyPayment(order.orderId, {
        razorpayOrderId: result.razorpayOrderId,
        razorpayPaymentId: result.razorpayPaymentId,
        razorpaySignature: result.razorpaySignature,
      });

      setDone(true);
      setTimeout(() => navigate('/orders'), 1200);
    } catch (e: any) {
      const msg = e?.response?.data?.message || e?.message || 'Payment could not be completed';
      setError(msg);
    } finally {
      setBusy(false);
    }
  };

  if (done) return <p className="text-green-600">Payment successful! Redirecting to your orders…</p>;
  if (!cart) {
    if (error) return <p className="text-red-600">{error}</p>;
    return <p>Loading…</p>;
  }
  if (cart.items.length === 0) return <p>Your cart is empty. Add items before checking out.</p>;

  return (
    <div className="mx-auto max-w-[560px]">
      <h2 className="mb-4 text-2xl font-bold">Checkout</h2>
      <div className="mb-4 rounded-xl border border-gray-200 bg-white p-4 shadow-sm">
        {cart.items.map((i) => (
          <div key={i.cartItemId} className="flex justify-between">
            <span>
              {i.productName} × {i.quantity}
            </span>
            <span>₹{i.lineTotal}</span>
          </div>
        ))}
        <div className="mt-2 flex justify-between font-bold">
          <span>Total</span>
          <span>₹{cart.totalPrice}</span>
        </div>
      </div>

      <div className="rounded-xl border border-gray-200 bg-white p-4 shadow-sm">
        <div className="mb-3">
          <label className="mb-1 block text-sm font-medium text-gray-700">Address</label>
          <input
            value={addressLine}
            onChange={(e) => {
              setAddressLine(e.target.value);
              if (errors.addressLine) setErrors({ ...errors, addressLine: undefined });
            }}
            placeholder="House/flat, street, area"
            className={inputCls}
          />
          {errors.addressLine && <p className="mt-1 text-sm text-red-600">{errors.addressLine}</p>}
        </div>

        <div className="mb-3">
          <label className="mb-1 block text-sm font-medium text-gray-700">PIN code</label>
          <input
            value={pinCode}
            inputMode="numeric"
            maxLength={6}
            onChange={(e) => {
              setPinCode(e.target.value);
              if (errors.pinCode) setErrors({ ...errors, pinCode: undefined });
            }}
            placeholder="6-digit PIN"
            className={inputCls}
          />
          {errors.pinCode && <p className="mt-1 text-sm text-red-600">{errors.pinCode}</p>}
        </div>

        <div>
          <label className="mb-1 block text-sm font-medium text-gray-700">
            Landmark <span className="text-gray-400">(optional)</span>
          </label>
          <input
            value={landmark}
            onChange={(e) => setLandmark(e.target.value)}
            placeholder="Nearby landmark"
            className={inputCls}
          />
        </div>
      </div>

      <button onClick={handlePay} disabled={busy} className={payBtnCls}>
        {busy ? 'Processing…' : `Pay ₹${cart.totalPrice}`}
      </button>
      {error && (
        <p className="mt-3 rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
          {error}
        </p>
      )}
    </div>
  );
}
