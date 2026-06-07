import { useState, useEffect } from 'react';
import { Package, ShoppingCart, AlertTriangle, TrendingUp } from 'lucide-react';
import toast from 'react-hot-toast';
import { getInventoryReport } from '../api/reports';
import { getOrders, getMyOrders } from '../api/orders';
import { useAuth } from '../context/AuthContext';
import LoadingSpinner from '../components/LoadingSpinner';

function StatCard({ icon: Icon, label, value, color }) {
  return (
    <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6 flex items-center gap-4">
      <div className={`p-3 rounded-xl ${color}`}>
        <Icon className="h-6 w-6 text-white" />
      </div>
      <div>
        <p className="text-sm text-gray-500">{label}</p>
        <p className="text-2xl font-bold text-gray-900">{value ?? '—'}</p>
      </div>
    </div>
  );
}

export default function Dashboard() {
  const { isManager } = useAuth();
  const [report, setReport] = useState(null);
  const [orders, setOrders] = useState([]);
  const [loadingReport, setLoadingReport] = useState(true);
  const [loadingOrders, setLoadingOrders] = useState(true);

  useEffect(() => {
    if (isManager) {
      getInventoryReport()
        .then((r) => setReport(r.data.data))
        .catch(() => toast.error('Failed to load inventory report'))
        .finally(() => setLoadingReport(false));

      getOrders({ page: 0, size: 5, sort: 'createdAt,desc' })
        .then((r) => setOrders(r.data.data?.content || []))
        .catch(() => toast.error('Failed to load orders'))
        .finally(() => setLoadingOrders(false));
    } else {
      setLoadingReport(false);
      getMyOrders({ page: 0, size: 5, sort: 'createdAt,desc' })
        .then((r) => setOrders(r.data.data?.content || []))
        .catch(() => toast.error('Failed to load your orders'))
        .finally(() => setLoadingOrders(false));
    }
  }, [isManager]);

  const statusColor = {
    PENDING: 'bg-yellow-100 text-yellow-800',
    PROCESSING: 'bg-blue-100 text-blue-800',
    COMPLETED: 'bg-green-100 text-green-800',
    CANCELLED: 'bg-red-100 text-red-800',
  };

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-gray-900">Dashboard</h1>

      {isManager && (
        <>
          {loadingReport ? (
            <LoadingSpinner className="py-8" />
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
              <StatCard
                icon={Package}
                label="Total Products"
                value={report?.totalProducts}
                color="bg-indigo-500"
              />
              <StatCard
                icon={AlertTriangle}
                label="Low Stock"
                value={report?.lowStockCount}
                color="bg-orange-500"
              />
              <StatCard
                icon={TrendingUp}
                label="Total Value"
                value={report?.totalInventoryValue != null ? `$${Number(report.totalInventoryValue).toFixed(2)}` : null}
                color="bg-green-500"
              />
              <StatCard
                icon={ShoppingCart}
                label="Categories"
                value={report?.categories?.length}
                color="bg-purple-500"
              />
            </div>
          )}

          {/* Low Stock Products */}
          {report?.lowStockProducts?.length > 0 && (
            <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6">
              <h2 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2">
                <AlertTriangle className="h-5 w-5 text-orange-500" />
                Low Stock Products
              </h2>
              <div className="space-y-2">
                {report.lowStockProducts.map((p) => (
                  <div
                    key={p.id}
                    className="flex items-center justify-between py-2 border-b border-gray-50 last:border-0"
                  >
                    <div>
                      <span className="font-medium text-gray-800">{p.name}</span>
                      <span className="text-gray-400 text-xs ml-2">{p.sku}</span>
                    </div>
                    <span className="text-red-600 font-semibold text-sm">
                      {p.quantity} left
                    </span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </>
      )}

      {/* Recent Orders */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6">
        <h2 className="text-lg font-semibold text-gray-900 mb-4">
          {isManager ? 'Recent Orders' : 'My Recent Orders'}
        </h2>
        {loadingOrders ? (
          <LoadingSpinner className="py-4" />
        ) : orders.length === 0 ? (
          <p className="text-gray-400 text-sm text-center py-4">No orders found</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-100">
                  <th className="text-left py-2 text-gray-500 font-medium">Order #</th>
                  <th className="text-left py-2 text-gray-500 font-medium">Status</th>
                  <th className="text-left py-2 text-gray-500 font-medium">Items</th>
                  <th className="text-right py-2 text-gray-500 font-medium">Total</th>
                </tr>
              </thead>
              <tbody>
                {orders.map((order) => (
                  <tr key={order.id} className="border-b border-gray-50">
                    <td className="py-2 font-mono text-indigo-600">{order.orderNumber}</td>
                    <td className="py-2">
                      <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${statusColor[order.status] || 'bg-gray-100 text-gray-800'}`}>
                        {order.status}
                      </span>
                    </td>
                    <td className="py-2 text-gray-600">{order.items?.length ?? 0} items</td>
                    <td className="py-2 text-right font-medium">
                      ${Number(order.totalAmount || 0).toFixed(2)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
