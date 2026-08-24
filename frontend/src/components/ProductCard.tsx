import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import type { ProductResponse } from '../types';
import { addToCart } from '../api/cart';

export default function ProductCard({ product }: { product: ProductResponse }) {
  const navigate = useNavigate();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [imgFailed, setImgFailed] = useState(false);

  const handleAdd = async () => {
    setBusy(true);
    setError('');
    try {
      await addToCart(product.productId, 1);
      navigate('/cart');
    } catch {
      setError('Could not add to cart');
      setBusy(false);
    }
  };

  return (
    <div className="flex flex-col gap-2 rounded-xl border border-gray-200 bg-white p-4 transition hover:-translate-y-0.5 hover:shadow-md">
      <div className="flex h-[140px] items-center justify-center overflow-hidden rounded-md bg-gray-100">
        {product.imageUrl && !imgFailed ? (
          <img
            src={product.imageUrl}
            alt={product.productName}
            onError={() => setImgFailed(true)}
            className="h-full w-full object-cover"
          />
        ) : (
          <span className="text-[13px] text-gray-400">No image</span>
        )}
      </div>
      <h3 className="m-0">{product.productName}</h3>
      <p className="m-0 font-semibold">₹{product.productPrice}</p>
      <p className="m-0 text-[13px] text-gray-500">{product.categoryName}</p>
      <button
        onClick={handleAdd}
        disabled={busy}
        className="cursor-pointer rounded-md bg-blue-600 px-3 py-2 text-sm text-white disabled:opacity-60"
      >
        {busy ? 'Adding…' : 'Add to cart'}
      </button>
      {error && <p className="m-0 text-[13px] text-red-600">{error}</p>}
    </div>
  );
}
