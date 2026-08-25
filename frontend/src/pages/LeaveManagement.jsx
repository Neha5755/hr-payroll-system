import { useEffect, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import {
  applyForLeave, getMyLeaveHistory, getMyLeaveBalance, cancelLeave,
  getPendingLeaveRequests, approveLeave, rejectLeave,
} from '../api/leaveApi';

const LEAVE_TYPES = [
  { code: 'CL', name: 'Casual Leave' },
  { code: 'SL', name: 'Sick Leave' },
  { code: 'EL', name: 'Earned Leave' },
];

export default function LeaveManagement() {
  const { isHrOrAdmin } = useAuth();
  const [balance, setBalance] = useState([]);
  const [history, setHistory] = useState([]);
  const [pending, setPending] = useState([]);
  const [form, setForm] = useState({ leaveTypeCode: 'CL', startDate: '', endDate: '', reason: '' });
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  const loadAll = () => {
    getMyLeaveBalance().then((res) => setBalance(res.data)).catch(() => {});
    getMyLeaveHistory().then((res) => setHistory(res.data)).catch(() => {});
    if (isHrOrAdmin) {
      getPendingLeaveRequests().then((res) => setPending(res.data)).catch(() => {});
    }
  };

  useEffect(loadAll, [isHrOrAdmin]);

  const submitLeave = async (e) => {
    e.preventDefault();
    setMessage(''); setError('');
    try {
      await applyForLeave(form);
      setMessage('Leave request submitted successfully.');
      setForm({ leaveTypeCode: 'CL', startDate: '', endDate: '', reason: '' });
      loadAll();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to submit leave request.');
    }
  };

  const handleCancel = async (id) => {
    await cancelLeave(id);
    loadAll();
  };

  const handleApprove = async (id) => {
    await approveLeave(id);
    loadAll();
  };

  const handleReject = async (id) => {
    await rejectLeave(id);
    loadAll();
  };

  return (
    <div className="page">
      <h1>Leave Management</h1>

      <div className="stat-row">
        {balance.map((b) => (
          <div key={b.leaveTypeCode} className="stat-card">
            <div className="stat-card-value">{b.remaining} / {b.allocated}</div>
            <div className="stat-card-label">{b.leaveTypeName} ({b.leaveTypeCode})</div>
          </div>
        ))}
      </div>

      <div className="card">
        <h2>Apply for Leave</h2>
        {message && <div className="alert alert-success">{message}</div>}
        {error && <div className="alert alert-error">{error}</div>}
        <form onSubmit={submitLeave} className="form-grid">
          <label>
            Leave Type
            <select value={form.leaveTypeCode} onChange={(e) => setForm({ ...form, leaveTypeCode: e.target.value })}>
              {LEAVE_TYPES.map((t) => <option key={t.code} value={t.code}>{t.name}</option>)}
            </select>
          </label>
          <label>
            Start Date
            <input type="date" value={form.startDate} onChange={(e) => setForm({ ...form, startDate: e.target.value })} required />
          </label>
          <label>
            End Date
            <input type="date" value={form.endDate} onChange={(e) => setForm({ ...form, endDate: e.target.value })} required />
          </label>
          <label className="form-grid-full">
            Reason
            <input type="text" value={form.reason} onChange={(e) => setForm({ ...form, reason: e.target.value })} placeholder="Optional" />
          </label>
          <button type="submit" className="form-grid-full">Submit Request</button>
        </form>
      </div>

      <div className="card">
        <h2>My Leave History</h2>
        <table className="data-table">
          <thead>
            <tr><th>Type</th><th>From</th><th>To</th><th>Days</th><th>Status</th><th></th></tr>
          </thead>
          <tbody>
            {history.map((h) => (
              <tr key={h.id}>
                <td>{h.leaveTypeCode}</td>
                <td>{h.startDate}</td>
                <td>{h.endDate}</td>
                <td>{h.daysRequested}</td>
                <td><span className={`badge badge-${h.status.toLowerCase()}`}>{h.status}</span></td>
                <td>
                  {h.status === 'PENDING' && <button onClick={() => handleCancel(h.id)}>Cancel</button>}
                </td>
              </tr>
            ))}
            {history.length === 0 && <tr><td colSpan="6">No leave requests yet.</td></tr>}
          </tbody>
        </table>
      </div>

      {isHrOrAdmin && (
        <div className="card">
          <h2>Pending Approvals</h2>
          <table className="data-table">
            <thead>
              <tr><th>Employee</th><th>Type</th><th>From</th><th>To</th><th>Days</th><th>Reason</th><th>Actions</th></tr>
            </thead>
            <tbody>
              {pending.map((p) => (
                <tr key={p.id}>
                  <td>{p.employeeFullName} ({p.employeeCode})</td>
                  <td>{p.leaveTypeCode}</td>
                  <td>{p.startDate}</td>
                  <td>{p.endDate}</td>
                  <td>{p.daysRequested}</td>
                  <td>{p.reason || '-'}</td>
                  <td>
                    <button onClick={() => handleApprove(p.id)}>Approve</button>{' '}
                    <button onClick={() => handleReject(p.id)} className="btn-secondary">Reject</button>
                  </td>
                </tr>
              ))}
              {pending.length === 0 && <tr><td colSpan="7">No pending requests.</td></tr>}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
