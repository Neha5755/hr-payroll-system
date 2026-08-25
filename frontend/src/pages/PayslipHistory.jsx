import { useEffect, useState } from 'react';
import { getMyPayslips } from '../api/payrollApi';
import axiosClient from '../api/axiosClient';

const MONTH_NAMES = ['', 'January', 'February', 'March', 'April', 'May', 'June',
  'July', 'August', 'September', 'October', 'November', 'December'];

export default function PayslipHistory() {
  const [payslips, setPayslips] = useState([]);
  const [error, setError] = useState('');

  useEffect(() => {
    getMyPayslips().then((res) => setPayslips(res.data)).catch(() => setError('Could not load payslips.'));
  }, []);

  // The download endpoint requires the JWT auth header, so a plain <a href> won't
  // work — fetch the PDF as a blob (with the interceptor attaching the token) and
  // trigger a client-side download instead.
  const handleDownload = async (id, month, year) => {
    const res = await axiosClient.get(`/payroll/payslips/${id}/download`, { responseType: 'blob' });
    const url = window.URL.createObjectURL(new Blob([res.data], { type: 'application/pdf' }));
    const link = document.createElement('a');
    link.href = url;
    link.download = `payslip_${month}_${year}.pdf`;
    link.click();
    window.URL.revokeObjectURL(url);
  };

  return (
    <div className="page">
      <h1>My Payslips</h1>
      {error && <div className="alert alert-error">{error}</div>}

      <div className="card">
        <table className="data-table">
          <thead>
            <tr><th>Period</th><th>Gross</th><th>Deductions</th><th>Net Pay</th><th>Status</th><th></th></tr>
          </thead>
          <tbody>
            {payslips.map((p) => (
              <tr key={p.id}>
                <td>{MONTH_NAMES[p.payPeriodMonth]} {p.payPeriodYear}</td>
                <td>₹{p.grossSalary}</td>
                <td>₹{p.totalDeductions}</td>
                <td><strong>₹{p.netSalary}</strong></td>
                <td><span className={`badge badge-${p.status.toLowerCase()}`}>{p.status}</span></td>
                <td>
                  <button onClick={() => handleDownload(p.id, p.payPeriodMonth, p.payPeriodYear)}>Download PDF</button>
                </td>
              </tr>
            ))}
            {payslips.length === 0 && <tr><td colSpan="6">No payslips generated yet.</td></tr>}
          </tbody>
        </table>
      </div>
    </div>
  );
}
