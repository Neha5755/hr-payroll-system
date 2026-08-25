import { useState } from 'react';
import { runPayroll } from '../api/payrollApi';

const MONTH_NAMES = ['', 'January', 'February', 'March', 'April', 'May', 'June',
  'July', 'August', 'September', 'October', 'November', 'December'];

export default function PayrollRun() {
  const now = new Date();
  const [month, setMonth] = useState(now.getMonth() + 1);
  const [year, setYear] = useState(now.getFullYear());
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleRun = async () => {
    setLoading(true); setError(''); setResult(null);
    try {
      const { data } = await runPayroll(Number(month), Number(year));
      setResult(data);
    } catch (e) {
      setError(e.response?.data?.message || 'Payroll run failed.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="page">
      <h1>Run Payroll</h1>
      <div className="card">
        <p>Generates payslip PDFs and emails them to every active employee for the selected period.</p>
        <div className="form-grid">
          <label>Month
            <select value={month} onChange={(e) => setMonth(e.target.value)}>
              {MONTH_NAMES.slice(1).map((m, i) => <option key={i + 1} value={i + 1}>{m}</option>)}
            </select>
          </label>
          <label>Year
            <input type="number" value={year} onChange={(e) => setYear(e.target.value)} />
          </label>
        </div>
        <button onClick={handleRun} disabled={loading}>{loading ? 'Running…' : 'Run Payroll Now'}</button>

        {error && <div className="alert alert-error">{error}</div>}
        {result && (
          <div className="alert alert-success" style={{ marginTop: 16 }}>
            <strong>Run complete — status: {result.status}</strong>
            <ul>
              <li>Total employees: {result.totalEmployees}</li>
              <li>Processed: {result.processedCount}</li>
              <li>Emails sent successfully: {result.emailSuccessCount}</li>
              <li>Emails failed: {result.emailFailedCount}</li>
            </ul>
          </div>
        )}
      </div>
    </div>
  );
}
