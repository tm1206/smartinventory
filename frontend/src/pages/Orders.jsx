import { useState, useEffect, useCallback } from 'react';
import { Plus, X, ChevronDown, ChevronUp } from 'lucide-react';
import toast from 'react-hot-toast';
import { getOrders, getMyOrders, createOrder, updateOrderStatus } from '../api/orders';
import { getProducts } from '../api/products';
import { useAuth } from '../context/AuthContext';
import LoadingSpinner from '../components/LoadingSpinner';

const STATUS_OPTIONS = ['PENDING', 'PROCESSING', 'COMPLETED', 'CANCELLED'];

const statusColor = {
  PENDING: 'bg-yellow-100 text-yellow-800',
  PROCESSING: 'bg-blue-100 text-blue-800',
  COMPLETED: 'bg-green-100 text-green-800',
  CANCELLED: 'bg-red-100 text-red-800',
};

function OrderModal({ open, onClose, onSave }) {
  const [items, setItems] = useState([{ productId: '', quantity: 1 }]);
  const [shippingAddress, setShippingAddress] = useState('');
  const [notes, setNotes] = useState('');
  const [products, setProducts] = useState([]);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (open) {
      setItems([{ productId: '', quantity: 1 }]);
      setShippingAddress('');
      setNotes('');
      getProducts({ size: 100 })
        .then((r) => setProducts(r.data.data?.content || []))
        .catch(() => toast.error('Failed to load products'));
    }
  }, [open]);

  if (!open) return null;

  const addItem = () => setItems([...items, { productId: '', quantity: 1 }]);
  const removeItem = (i) => setItems(items.filter((_, idx) => idx !== i));
  const updateItem = (i, key, val) =>
    setItems(items.map((item, idx) => (idx === i ? { ...item, [key]: val } : item)));

  const handleSubmit = async (e) => {
    e.preventDefault();
    const validItems = items.filter((it) => it.productId && it.quantity > 0);
    if (!validItems.length) {
      toast.error('Add at least one item');
      return;
    }
    setSaving(true);
    try {
      await createOrder({
        items: validItems.map((it) => ({
          productId: Number(it.productId),
          quantity: Number(it.quantity),
        })),
        shippingAddress,
        notes,
      });
      toast.success('Order placed successfully');
      onSave();
      onClose();
    } catch (err) {
      const msg = err.response?.data?.message || 'Failed to place order';
      toast.error(msg);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4">
      <div className="bg-white rounded-xl shadow-xl w-full max-w-lg max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between p-6 border-b">
          <h2 className="text-lg font-semibold">Place New Order</h2>
          <button onClick={onClose} className="p-1 rounded hover:bg-gray-100">
            <X className="h-5 w-5" />
          </button>
        </div>
        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Order Items</label>
            {items.map((item, i) => (
              <div key={i} className="flex gap-2 mb-2">
                <select
                  required
                  value={item.productId}
                  onChange={(e) => updateItem(i, 'productId', e.target.value)}
                  className="flex-1 border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
                >
                  <option value="">Select product</option>
                  {products.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.name} (Stock: {p.quantity})
                    </option>
                  ))}
                </select>
                <input
                  type="number"
                  min="1"
                  required
                  value={item.quantity}
                  onChange={(e) => updateItem(i, 'quantity', e.target.value)}
                  className="w-20 border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  placeholder="Qty"
                />
                {items.length > 1 && (
                  <button
                    type="button"
                    onClick={() => removeItem(i)}
                    className="p-2 text-red-500 hover:bg-red-50 rounded-lg"
                  >
                    <X className="h-4 w-4" />
                  </button>
                )}
              </div>
            ))}
            <button
              type="button"
              onClick={addItem}
              className="text-indigo-600 text-sm hover:underline flex items-center gap-1"
            >
              <Plus className="h-3.5 w-3.5" /> Add item
            </button>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Shipping Address</label>
            <input
              type="text"
              value={shippingAddress}
              onChange={(e) => setShippingAddress(e.target.value)}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
              placeholder="Optional"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Notes</label>
            <textarea
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              rows={2}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 resize-none"
              placeholder="Optional"
            />
          </div>

          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={onClose} className="px-4 py-2 text-sm border border-gray-300 rounded-lg hover:bg-gray-50">
              Cancel
            </button>
            <button
              type="submit"
              disabled={saving}
              className="px-4 py-2 text-sm bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 disabled:opacity-60 flex items-center gap-2"
            >
              {saving && <LoadingSpinner size="sm" />}
              Place Order
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

function OrderRow({ order, isManager, onStatusUpdate }) {
  const [expanded, setExpanded] = useState(false);
  const [updatingStatus, setUpdatingStatus] = useState(false);

  const handleStatusChange = async (status) => {
    setUpdatingStatus(true);
    try {
      await updateOrderStatus(order.id, status);
      toast.success(`Status updated to ${status}`);
      onStatusUpdate();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to update status');
    } finally {
      setUpdatingStatus(false);
    }
  };

  return (
    <>
      <tr
        className="border-b border-gray-50 hover:bg-gray-50 cursor-pointer transition-colors"
        onClick={() => setExpanded(!expanded)}
      >
        <td className="px-4 py-3 font-mono text-indigo-600 text-sm">{order.orderNumber}</td>
        <td className="px-4 py-3 text-sm text-gray-600">
          {order.username || order.createdBy}
        </td>
        <td className="px-4 py-3">
          {isManager ? (
            <select
              value={order.status}
              onChange={(e) => { e.stopPropagation(); handleStatusChange(e.target.value); }}
              disabled={updatingStatus}
              onClick={(e) => e.stopPropagation()}
              className={`text-xs font-medium px-2 py-1 rounded-full border-0 cursor-pointer focus:outline-none focus:ring-2 focus:ring-indigo-500 ${statusColor[order.status] || 'bg-gray-100'}`}
            >
              {STATUS_OPTIONS.map((s) => <option key={s} value={s}>{s}</option>)}
            </select>
          ) : (
            <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${statusColor[order.status] || 'bg-gray-100 text-gray-800'}`}>
              {order.status}
            </span>
          )}
        </td>
        <td className="px-4 py-3 text-sm text-gray-600">{order.items?.length ?? 0} items</td>
        <td className="px-4 py-3 text-sm font-medium text-gray-900 text-right">
          ${Number(order.totalAmount || 0).toFixed(2)}
        </td>
        <td className="px-4 py-3 text-gray-400">
          {expanded ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
        </td>
      </tr>
      {expanded && (
        <tr className="border-b border-gray-100 bg-indigo-50/30">
          <td colSpan={6} className="px-4 py-3">
            <div className="space-y-1">
              {order.items?.map((item, i) => (
                <div key={i} className="flex justify-between text-sm text-gray-700">
                  <span>{item.productName || `Product #${item.productId}`} × {item.quantity}</span>
                  <span>${Number(item.totalPrice || (item.unitPrice || item.price || 0) * item.quantity).toFixed(2)}</span>
                </div>
              ))}
              {order.shippingAddress && (
                <div className="text-xs text-gray-500 mt-2">
                  <span className="font-medium">Ship to:</span> {order.shippingAddress}
                </div>
              )}
              {order.notes && (
                <div className="text-xs text-gray-500">
                  <span className="font-medium">Notes:</span> {order.notes}
                </div>
              )}
            </div>
          </td>
        </tr>
      )}
    </>
  );
}

export default function Orders() {
  const { isManager } = useAuth();
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [modalOpen, setModalOpen] = useState(false);

  const fetchOrders = useCallback(async () => {
    setLoading(true);
    try {
      const params = { page, size: 20, sort: 'createdAt,desc' };
      const fn = isManager ? getOrders : getMyOrders;
      const r = await fn(params);
      const data = r.data.data;
      setOrders(data?.content || []);
      setTotalPages(data?.totalPages || 0);
    } catch {
      toast.error('Failed to load orders');
    } finally {
      setLoading(false);
    }
  }, [page, isManager]);

  useEffect(() => {
    fetchOrders();
  }, [fetchOrders]);

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">
          {isManager ? 'All Orders' : 'My Orders'}
        </h1>
        <button
          onClick={() => setModalOpen(true)}
          className="flex items-center gap-2 bg-indigo-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-indigo-700"
        >
          <Plus className="h-4 w-4" />
          Place Order
        </button>
      </div>

      <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
        {loading ? (
          <LoadingSpinner className="py-12" />
        ) : orders.length === 0 ? (
          <p className="text-center text-gray-400 py-12">No orders found</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-gray-50 border-b border-gray-100">
                <tr>
                  <th className="text-left px-4 py-3 text-gray-500 font-medium">Order #</th>
                  <th className="text-left px-4 py-3 text-gray-500 font-medium">User</th>
                  <th className="text-left px-4 py-3 text-gray-500 font-medium">Status</th>
                  <th className="text-left px-4 py-3 text-gray-500 font-medium">Items</th>
                  <th className="text-right px-4 py-3 text-gray-500 font-medium">Total</th>
                  <th className="px-4 py-3" />
                </tr>
              </thead>
              <tbody>
                {orders.map((order) => (
                  <OrderRow
                    key={order.id}
                    order={order}
                    isManager={isManager}
                    onStatusUpdate={fetchOrders}
                  />
                ))}
              </tbody>
            </table>
          </div>
        )}

        {totalPages > 1 && (
          <div className="flex items-center justify-center gap-2 px-4 py-3 border-t border-gray-100">
            <button
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={page === 0}
              className="px-3 py-1 text-sm border border-gray-300 rounded-lg disabled:opacity-40 hover:bg-gray-50"
            >
              Previous
            </button>
            <span className="text-sm text-gray-600">Page {page + 1} of {totalPages}</span>
            <button
              onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
              disabled={page >= totalPages - 1}
              className="px-3 py-1 text-sm border border-gray-300 rounded-lg disabled:opacity-40 hover:bg-gray-50"
            >
              Next
            </button>
          </div>
        )}
      </div>

      <OrderModal
        open={modalOpen}
        onClose={() => setModalOpen(false)}
        onSave={fetchOrders}
      />
    </div>
  );
}
