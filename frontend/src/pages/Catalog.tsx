import { useEffect, useState } from 'react';
import { getCategories, getProducts } from '../api/products';
import type { Category, ProductResponse } from '../types';
import ProductCard from '../components/ProductCard';

const PAGE_SIZE = 12;

export default function Catalog() {
  const [products, setProducts] = useState<ProductResponse[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [selectedCategory, setSelectedCategory] = useState<number | null>(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    getCategories()
      .then(setCategories)
      .catch(() => setError('Failed to load categories'));
  }, []);

  useEffect(() => {
    setLoading(true);
    setError('');
    getProducts({ categoryId: selectedCategory ?? undefined, page, size: PAGE_SIZE })
      .then((res) => {
        setProducts(res.content);
        setTotalPages(Math.max(1, res.totalPages));
      })
      .catch(() => setError('Failed to load products'))
      .finally(() => setLoading(false));
  }, [selectedCategory, page]);

  return (
    <div>
      <h2 className="text-2xl font-bold">Products</h2>
      <p className="mb-4 mt-1 text-sm text-gray-500">Browse the catalog and add items to your cart.</p>

      <div className="mb-4 flex flex-wrap gap-2">
        <button
          onClick={() => {
            setSelectedCategory(null);
            setPage(0);
          }}
          className={`cursor-pointer rounded-full border px-3.5 py-1.5 text-sm ${
            selectedCategory === null
              ? 'border-blue-600 bg-blue-600 text-white'
              : 'border-gray-300 bg-white text-gray-900'
          }`}
        >
          All
        </button>
        {categories.map((c) => (
          <button
            key={c.categoryId}
            onClick={() => {
              setSelectedCategory(c.categoryId);
              setPage(0);
            }}
            className={`cursor-pointer rounded-full border px-3.5 py-1.5 text-sm ${
              selectedCategory === c.categoryId
                ? 'border-blue-600 bg-blue-600 text-white'
                : 'border-gray-300 bg-white text-gray-900'
            }`}
          >
            {c.categoryName}
          </button>
        ))}
      </div>

      {loading && <p className="text-gray-600">Loading…</p>}
      {error && <p className="text-red-600">{error}</p>}

      <div className="grid grid-cols-[repeat(auto-fill,minmax(200px,1fr))] gap-4">
        {products.map((p) => (
          <ProductCard key={p.productId} product={p} />
        ))}
      </div>

      <div className="mt-6 flex items-center justify-center gap-4">
        <button
          disabled={page === 0}
          onClick={() => setPage((p) => p - 1)}
          className="cursor-pointer rounded-md border border-gray-300 bg-white px-4 py-2 disabled:opacity-50"
        >
          Previous
        </button>
        <span className="self-center">
          Page {page + 1} / {totalPages}
        </span>
        <button
          disabled={page + 1 >= totalPages}
          onClick={() => setPage((p) => p + 1)}
          className="cursor-pointer rounded-md border border-gray-300 bg-white px-4 py-2 disabled:opacity-50"
        >
          Next
        </button>
      </div>
    </div>
  );
}
