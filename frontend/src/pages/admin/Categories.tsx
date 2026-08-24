import { useEffect, useState } from 'react';
import * as adminApi from '../../api/admin';
import { getCategories } from '../../api/products';
import type { Category } from '../../types';

const inputCls =
  'rounded-md border border-gray-300 px-2 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500';
const btnCls =
  'cursor-pointer rounded-md border border-gray-300 bg-gray-50 px-3 py-1.5 text-sm hover:bg-gray-100';
const btnDangerCls = 'cursor-pointer border-0 bg-transparent text-red-600';
const thCls = 'border-b-2 border-gray-200 px-2 py-2 text-left text-sm font-semibold';
const tdCls = 'border-b border-gray-100 px-2 py-2 text-sm';

export default function AdminCategories() {
  const [categories, setCategories] = useState<Category[]>([]);
  const [name, setName] = useState('');
  const [error, setError] = useState('');
  const [editing, setEditing] = useState<Category | null>(null);
  const [editName, setEditName] = useState('');

  async function load() {
    try {
      setCategories(await getCategories());
    } catch (e: any) {
      setError(e?.response?.data ?? 'Failed to load categories');
    }
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function handleCreate() {
    setError('');
    if (!name.trim()) return;
    try {
      await adminApi.createCategory({ categoryName: name.trim() });
      setName('');
      await load();
    } catch (e: any) {
      setError(e?.response?.data ?? 'Failed to create category');
    }
  }

  async function handleUpdate() {
    if (!editing) return;
    setError('');
    try {
      await adminApi.updateCategory(editing.categoryId, { categoryName: editName.trim() });
      setEditing(null);
      await load();
    } catch (e: any) {
      setError(e?.response?.data ?? 'Failed to update category');
    }
  }

  async function handleDelete(c: Category) {
    if (!confirm(`Delete category "${c.categoryName}"?`)) return;
    setError('');
    try {
      await adminApi.deleteCategory(c.categoryId);
      await load();
    } catch (e: any) {
      setError(e?.response?.data ?? 'Failed to delete category');
    }
  }

  return (
    <div>
      <h2 className="text-xl font-semibold">Categories</h2>
      {error && <p className="text-red-600">{error}</p>}

      <div className="mb-3 flex gap-2">
        <input
          placeholder="New category name"
          value={name}
          onChange={(e) => setName(e.target.value)}
          className={inputCls}
        />
        <button onClick={handleCreate} className={btnCls}>
          Add
        </button>
      </div>

      <div className="overflow-x-auto">
      <table className="w-full border-collapse">
        <thead>
          <tr>
            <th className={thCls}>ID</th>
            <th className={thCls}>Name</th>
            <th className={thCls}>Products</th>
            <th className={thCls}></th>
          </tr>
        </thead>
        <tbody>
          {categories.map((c) => (
            <tr key={c.categoryId}>
              <td className={tdCls}>{c.categoryId}</td>
              <td className={tdCls}>
                {editing?.categoryId === c.categoryId ? (
                  <input
                    value={editName}
                    onChange={(e) => setEditName(e.target.value)}
                    className={inputCls}
                  />
                ) : (
                  c.categoryName
                )}
              </td>
              <td className={tdCls}>{c.products?.length ?? 0}</td>
              <td className={`${tdCls} flex gap-2`}>
                {editing?.categoryId === c.categoryId ? (
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
                        setEditing(c);
                        setEditName(c.categoryName);
                      }}
                      className={btnCls}
                    >
                      Edit
                    </button>
                    <button onClick={() => handleDelete(c)} className={btnDangerCls}>
                      Delete
                    </button>
                  </>
                )}
              </td>
            </tr>
          ))}
          {categories.length === 0 && (
            <tr>
              <td className={tdCls} colSpan={4}>
                No categories
              </td>
            </tr>
          )}
        </tbody>
      </table>
      </div>
    </div>
  );
}
