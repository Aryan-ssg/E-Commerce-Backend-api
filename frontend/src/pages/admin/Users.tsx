import { useEffect, useState } from 'react';
import * as adminApi from '../../api/admin';
import type { AdminUser, PagedResponse } from '../../types';

const inputCls =
  'rounded-md border border-gray-300 px-2 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500';
const btnCls =
  'cursor-pointer rounded-md border border-gray-300 bg-gray-50 px-3 py-1.5 text-sm hover:bg-gray-100';
const thCls = 'border-b-2 border-gray-200 px-2 py-2 text-left text-sm font-semibold';
const tdCls = 'border-b border-gray-100 px-2 py-2 text-sm';

export default function AdminUsers() {
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [username, setUsername] = useState('');
  const [role, setRole] = useState('');
  const [error, setError] = useState('');

  async function load() {
    setError('');
    try {
      const res: PagedResponse<AdminUser> = await adminApi.getUsers({
        username: username || undefined,
        role: role || undefined,
        page,
        size: 20,
      });
      setUsers(res.content);
      setTotalPages(res.totalPages);
    } catch (e: any) {
      setError(e?.response?.data ?? 'Failed to load users');
    }
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page]);

  return (
    <div>
      <h2 className="text-xl font-semibold">Users</h2>
      <div className="mb-3 flex gap-2">
        <input
          placeholder="username"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          className={inputCls}
        />
        <select value={role} onChange={(e) => setRole(e.target.value)} className={inputCls}>
          <option value="">all roles</option>
          <option value="USER">USER</option>
          <option value="ADMIN">ADMIN</option>
        </select>
        <button onClick={() => { setPage(0); load(); }} className={btnCls}>
          Search
        </button>
      </div>

      {error && <p className="text-red-600">{error}</p>}

      <div className="overflow-x-auto">
      <table className="w-full border-collapse">
        <thead>
          <tr>
            <th className={thCls}>ID</th>
            <th className={thCls}>Username</th>
            <th className={thCls}>Role</th>
          </tr>
        </thead>
        <tbody>
          {users.map((u) => (
            <tr key={u.userId}>
              <td className={tdCls}>{u.userId}</td>
              <td className={tdCls}>{u.username}</td>
              <td className={tdCls}>{u.role}</td>
            </tr>
          ))}
          {users.length === 0 && (
            <tr>
              <td className={tdCls} colSpan={3}>
                No users
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
