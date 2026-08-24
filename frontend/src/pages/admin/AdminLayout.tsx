import { NavLink, Outlet } from 'react-router-dom';

const items = [
  { to: '/admin/users', label: 'Users' },
  { to: '/admin/categories', label: 'Categories' },
  { to: '/admin/products', label: 'Products' },
  { to: '/admin/orders', label: 'Orders' },
];

export default function AdminLayout() {
  return (
    <div className="flex flex-col gap-4 md:flex-row md:items-start md:gap-6">
      <aside className="w-full md:w-[200px] md:border-r md:border-gray-200 md:pr-2">
        <h3 className="my-2 text-base font-semibold">Admin</h3>
        <nav className="flex flex-row gap-1 overflow-x-auto md:flex-col">
          {items.map((i) => (
            <NavLink
              key={i.to}
              to={i.to}
              className={({ isActive }) =>
                `rounded-md px-2.5 py-2 no-underline ${
                  isActive ? 'bg-indigo-100 font-semibold text-indigo-800' : 'text-gray-900'
                }`
              }
            >
              {i.label}
            </NavLink>
          ))}
        </nav>
      </aside>
      <section className="flex-1">
        <Outlet />
      </section>
    </div>
  );
}
