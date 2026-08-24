import { FormEvent, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

const inputCls =
  'w-full rounded-md border border-gray-300 px-3 py-2.5 text-[15px] focus:outline-none focus:ring-2 focus:ring-blue-500';
const btnCls =
  'rounded-md bg-blue-600 px-4 py-2.5 text-[15px] text-white cursor-pointer hover:bg-blue-700';

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    try {
      await login(username, password);
      navigate('/');
    } catch {
      setError('Invalid username or password');
    }
  };

  return (
    <div className="mx-auto mt-10 w-full max-w-md">
      <div className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
        <h2 className="mb-4 text-xl font-semibold">Login</h2>
        <form onSubmit={submit} className="flex min-w-[280px] flex-col gap-3">
          <input
            placeholder="Username"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            className={inputCls}
          />
          <input
            type="password"
            placeholder="Password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className={inputCls}
          />
          {error && <p className="m-0 text-red-600">{error}</p>}
          <button type="submit" className={btnCls}>
            Login
          </button>
        </form>
        <p className="mt-3 text-sm text-gray-600">
          No account? <Link to="/register" className="text-blue-600 hover:underline">Register</Link>
        </p>
      </div>
    </div>
  );
}
