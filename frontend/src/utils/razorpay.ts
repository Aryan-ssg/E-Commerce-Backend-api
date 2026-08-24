// Minimal typings for the Razorpay checkout global.
interface RazorpayResponse {
  razorpay_payment_id: string;
  razorpay_order_id: string;
  razorpay_signature: string;
}

interface RazorpayOptions {
  key: string;
  amount: number;
  currency: string;
  name?: string;
  description?: string;
  order_id: string;
  prefill?: { name?: string; email?: string; contact?: string };
  theme?: { color?: string };
}

declare global {
  interface Window {
    Razorpay?: new (options: RazorpayOptions & { handler: (resp: RazorpayResponse) => void }) => {
      open: () => void;
      on: (event: string, cb: (err: unknown) => void) => void;
    };
  }
}

export function loadRazorpayScript(): Promise<void> {
  return new Promise((resolve, reject) => {
    if (window.Razorpay) return resolve();
    const script = document.createElement('script');
    script.src = 'https://checkout.razorpay.com/v1/checkout.js';
    script.onload = () => resolve();
    script.onerror = () => reject(new Error('Failed to load Razorpay SDK'));
    document.body.appendChild(script);
  });
}

export function openRazorpay(
  options: RazorpayOptions
): Promise<{ razorpayPaymentId: string; razorpayOrderId: string; razorpaySignature: string }> {
  return new Promise((resolve, reject) => {
    if (!window.Razorpay) {
      reject(new Error('Razorpay SDK not loaded'));
      return;
    }
    const rzp = new window.Razorpay({
      ...options,
      handler: (resp: RazorpayResponse) =>
        resolve({
          razorpayPaymentId: resp.razorpay_payment_id,
          razorpayOrderId: resp.razorpay_order_id,
          razorpaySignature: resp.razorpay_signature,
        }),
    });
    rzp.on('payment.failed', (err) => reject(err));
    rzp.open();
  });
}
