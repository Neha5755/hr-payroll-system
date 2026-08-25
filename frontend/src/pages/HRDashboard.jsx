import { useEffect, useState } from 'react';
import { getDashboard } from '../api/dashboardApi';
import StatCard from '../components/StatCard';

export default function HRDashboard() {
  const [data, setData] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    getDashboard().then((r) => setData(r.data)).catch(() => setError('Could not load dashboard.'));
  }, []);

  if (error) return <div className="page"><div className="alert alert-error">{error}</div></div>;
  if (!data) return <div className="page">Loading…</div>;

  return (
    <div className="page">
      <h1>HR Dashboard</h1>
      <p className="muted">Period: {data.currentPeriodMonth}/{data.currentPeriodYear}</p>

      <div className="stat-row">
        <StatCard label="Total Employees" value={data.totalEmployees} />
        <StatCard label="Payroll Processed" value={data.totalEmployeesProcessed} />
        <StatCard label="Pending Payroll" value={data.pendingPayroll} tone={data.pendingPayroll > 0 ? 'warning' : 'default'} />
        <StatCard label="Payslips Sent" value={data.payslipsSentSuccessfully} tone="success" />
        <StatCard label="Failed Deliveries" value={data.failedDeliveries} tone={data.failedDeliveries > 0 ? 'danger' : 'default'} />
        <StatCard label="Pending Leave Approvals" value={data.pendingLeaveApprovals} />
      </div>

      <div className="card">
        <h2>Organization-wide Leave Balance Summary</h2>
        <table className="data-table">
          <thead><tr><th>Leave Type</th><th>Total Allocated</th><th>Total Used</th><th>Total Remaining</th></tr></thead>
          <tbody>
            {data.leaveBalanceSummary.map((row) => (
              <tr key={row.leaveTypeCode}>
                <td>{row.leaveTypeName} ({row.leaveTypeCode})</td>
                <td>{row.totalAllocated}</td>
                <td>{row.totalUsed}</td>
                <td>{row.totalRemaining}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
