import { useEffect, useState } from 'react';
import * as adminApi from '../../api/admin';
import { getProducts, getCategories } from '../../api/products';
import type { ProductResponse, Category, PagedResponse } from '../../types';

const inputCls =
  'rounded-md border border-gray-300 px-2 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500';
const btnCls =
  'cursor-pointer rounded-md border border-gray-300 bg-gray-50 px-3 py-1.5 text-sm hover:bg-gray-100';
const btnDangerCls = 'cursor-pointer border-0 bg-transparent text-red-600';
const thCls = 'border-b-2 border-gray-200 px-2 py-2 text-left text-sm font-semibold';
const tdCls = 'border-b border-gray-100 px-2 py-2 text-sm';

export default function AdminProducts() {
  const [products, setProducts] = useState<ProductResponse[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [error, setError] = useState('');

  const [name, setName] = useState('');
  const [price, setPrice] = useState('');
  const [stock, setStock] = useState('');
  const [imageUrl, setImageUrl] = useState('');
  const [categoryId, setCategoryId] = useState<number | ''>('');

  const [editing, setEditing] = useState<ProductResponse | null>(null);
  const [editName, setEditName] = useState('');
  const [editPrice, setEditPrice] = useState('');
  const [editImageUrl, setEditImageUrl] = useState('');

  async function load() {
    setError('');
    try {
      const res: PagedResponse<ProductResponse> = await getProducts({ page, size: 20 });
      setProducts(res.content);
      setTotalPages(res.totalPages);
    } catch (e: any) {
      setError(e?.response?.data ?? 'Failed to load products');
    }
  }

  useEffect(() => {
    load();
    getCategories()
      .then(setCategories)
      .catch(() => undefined);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page]);

  async function handleCreate() {
    setError('');
    if (categoryId === '' || !name.trim() || !price || !stock) {
      setError('Name, price, stock and category are required');
      return;
    }
    try {
      await adminApi.createProduct(Number(categoryId), {
        productName: name.trim(),
        productPrice: Number(price),
        stock: Number(stock),
        imageUrl: imageUrl.trim() || undefined,
      });
      setName('');
      setPrice('');
      setStock('');
      setImageUrl('');
      setCategoryId('');
      await load();
    } catch (e: any) {
      setError(e?.response?.data ?? 'Failed to create product');
    }
  }

  async function handleUpdate() {
    if (!editing) return;
    setError('');
    try {
      await adminApi.updateProduct(editing.productId, {
        productName: editName.trim(),
        productPrice: Number(editPrice),
        imageUrl: editImageUrl.trim() || undefined,
      });
      setEditing(null);
      await load();
    } catch (e: any) {
      setError(e?.response?.data ?? 'Failed to update product');
    }
  }

  async function handleDelete(p: ProductResponse) {
    if (!confirm(`Delete product "${p.productName}"?`)) return;
    setError('');
    try {
      await adminApi.deleteProduct(p.productId);
      await load();
    } catch (e: any) {
      setError(e?.response?.data ?? 'Failed to delete product');
    }
  }

  return (
    <div>
      <h2 className="text-xl font-semibold">Products</h2>
      {error && <p className="text-red-600">{error}</p>}

      <fieldset className="mb-4 rounded-lg border border-gray-200 p-3">
        <legend className="px-1 text-sm text-gray-600">New product</legend>
        <div className="flex flex-wrap gap-2">
          <input placeholder="Name" value={name} onChange={(e) => setName(e.target.value)} className={inputCls} />
          <input
            placeholder="Price"
            type="number"
            value={price}
            onChange={(e) => setPrice(e.target.value)}
            className={inputCls}
          />
          <input
            placeholder="Stock"
            type="number"
            value={stock}
            onChange={(e) => setStock(e.target.value)}
            className={inputCls}
          />
          <select
            value={categoryId}
            onChange={(e) => setCategoryId(e.target.value === '' ? '' : Number(e.target.value))}
            className={inputCls}
          >
            <option value="">Select category</option>
            {categories.map((c) => (
              <option key={c.categoryId} value={c.categoryId}>
                {c.categoryName}
              </option>
            ))}
          </select>
          <input
            placeholder="Image URL (optional)"
            value={imageUrl}
            onChange={(e) => setImageUrl(e.target.value)}
            className={inputCls}
          />
          <button onClick={handleCreate} className={btnCls}>
            Create
          </button>
        </div>
      </fieldset>

      <div className="overflow-x-auto">
      <table className="w-full border-collapse">
        <thead>
          <tr>
            <th className={thCls}>ID</th>
            <th className={thCls}>Name</th>
            <th className={thCls}>Price</th>
            <th className={thCls}>Category</th>
            <th className={thCls}>Image</th>
            <th className={thCls}></th>
          </tr>
        </thead>
        <tbody>
          {products.map((p) => (
            <tr key={p.productId}>
              <td className={tdCls}>{p.productId}</td>
              <td className={tdCls}>
                {editing?.productId === p.productId ? (
                  <input value={editName} onChange={(e) => setEditName(e.target.value)} className={inputCls} />
                ) : (
                  p.productName
                )}
              </td>
              <td className={tdCls}>
                {editing?.productId === p.productId ? (
                  <input
                    type="number"
                    value={editPrice}
                    onChange={(e) => setEditPrice(e.target.value)}
                    className={inputCls}
                  />
                ) : (
                  p.productPrice
                )}
              </td>
              <td className={tdCls}>
                {editing?.productId === p.productId ? (
                  <input
                    placeholder="Image URL"
                    value={editImageUrl}
                    onChange={(e) => setEditImageUrl(e.target.value)}
                    className={inputCls}
                  />
                ) : (
                  p.imageUrl ?? ''
                )}
              </td>
              <td className={tdCls}>{p.categoryName}</td>
              <td className={`${tdCls} flex gap-2`}>
                {editing?.productId === p.productId ? (
                  <>
                    <button onClick={handleUpdate} className={btnCls}>
                      Save
                    </button>
                    <button onClick={() => setEditing(null)} className={btnCls}>
                      Cancel
                    </button>
                  </>
                ) : (
                  <>
                    <button
                      onClick={() => {
                        setEditing(p);
                        setEditName(p.productName);
                        setEditPrice(String(p.productPrice));
                        setEditImageUrl(p.imageUrl ?? '');
                      }}
                      className={btnCls}
                    >
                      Edit
                    </button>
                    <button onClick={() => handleDelete(p)} className={btnDangerCls}>
                      Delete
                    </button>
                  </>
                )}
              </td>
            </tr>
          ))}
          {products.length === 0 && (
            <tr>
              <td className={tdCls} colSpan={6}>
                No products
              </td>
            </tr>
          )}
        </tbody>
      </table>
      </div>

      <div className="mt-3 flex items-center gap-2">
        <button disabled={page === 0} onClick={() => setPage((p) => p - 1)} className={btnCls}>
          Prev
        </button>
        <span>
          Page {page + 1} / {totalPages}
        </span>
        <button
          disabled={page + 1 >= totalPages}
          onClick={() => setPage((p) => p + 1)}
          className={btnCls}
        >
          Next
        </button>
      </div>
    </div>
  );
}
