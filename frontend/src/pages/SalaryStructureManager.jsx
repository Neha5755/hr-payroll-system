import { useEffect, useState } from 'react';
import {
  getAllSalaryStructures, createSalaryStructure, deleteSalaryStructure, assignSalaryStructure,
} from '../api/salaryApi';
import { getAllEmployees } from '../api/employeeApi';

const EMPTY = { name: '', basicSalary: '', hra: '', specialAllowance: '', pfPercent: 12, esiPercent: 0.75, professionalTax: 200, otherDeductions: 0 };

export default function SalaryStructureManager() {
  const [structures, setStructures] = useState([]);
  const [employees, setEmployees] = useState([]);
  const [form, setForm] = useState(EMPTY);
  const [assignForm, setAssignForm] = useState({ employeeId: '', salaryStructureId: '', effectiveFrom: '' });
  const [msg, setMsg] = useState(''); const [err, setErr] = useState('');

  const load = () => {
    getAllSalaryStructures().then((r) => setStructures(r.data)).catch(() => {});
    getAllEmployees().then((r) => setEmployees(r.data)).catch(() => {});
  };
  useEffect(load, []);

  const gross = (Number(form.basicSalary) || 0) + (Number(form.hra) || 0) + (Number(form.specialAllowance) || 0);

  const submit = async (e) => {
    e.preventDefault(); setMsg(''); setErr('');
    try {
      await createSalaryStructure(form);
      setMsg('Salary structure created.'); setForm(EMPTY); load();
    } catch (e2) { setErr(e2.response?.data?.message || 'Failed to create structure.'); }
  };

  const remove = async (id) => {
    if (!window.confirm('Delete (deactivate) this structure?')) return;
    await deleteSalaryStructure(id); load();
  };

  const assign = async (e) => {
    e.preventDefault(); setMsg(''); setErr('');
    try {
      await assignSalaryStructure({
        employeeId: Number(assignForm.employeeId),
        salaryStructureId: Number(assignForm.salaryStructureId),
        effectiveFrom: assignForm.effectiveFrom,
      });
      setMsg('Salary structure assigned. History recorded.');
      setAssignForm({ employeeId: '', salaryStructureId: '', effectiveFrom: '' });
    } catch (e2) { setErr(e2.response?.data?.message || 'Failed to assign structure.'); }
  };

  return (
    <div className="page">
      <h1>Salary Structures</h1>
      {msg && <div className="alert alert-success">{msg}</div>}
      {err && <div className="alert alert-error">{err}</div>}

      <div className="card">
        <h2>Create Structure</h2>
        <form onSubmit={submit} className="form-grid">
          <label>Name<input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required /></label>
          <label>Basic Salary<input type="number" value={form.basicSalary} onChange={(e) => setForm({ ...form, basicSalary: e.target.value })} required /></label>
          <label>HRA<input type="number" value={form.hra} onChange={(e) => setForm({ ...form, hra: e.target.value })} required /></label>
          <label>Special Allowance<input type="number" value={form.specialAllowance} onChange={(e) => setForm({ ...form, specialAllowance: e.target.value })} required /></label>
          <label>PF %<input type="number" step="0.01" value={form.pfPercent} onChange={(e) => setForm({ ...form, pfPercent: e.target.value })} /></label>
          <label>ESI %<input type="number" step="0.01" value={form.esiPercent} onChange={(e) => setForm({ ...form, esiPercent: e.target.value })} /></label>
          <label>Professional Tax<input type="number" value={form.professionalTax} onChange={(e) => setForm({ ...form, professionalTax: e.target.value })} /></label>
          <label>Other Deductions<input type="number" value={form.otherDeductions} onChange={(e) => setForm({ ...form, otherDeductions: e.target.value })} /></label>
          <div className="form-grid-full"><strong>Gross Salary Preview: ₹{gross}</strong></div>
          <button type="submit" className="form-grid-full">Create Structure</button>
        </form>
      </div>

      <div className="card">
        <h2>Existing Structures</h2>
        <table className="data-table">
          <thead><tr><th>Name</th><th>Basic</th><th>HRA</th><th>Allowance</th><th>Gross</th><th>Active</th><th></th></tr></thead>
          <tbody>
            {structures.map((s) => (
              <tr key={s.id}>
                <td>{s.name}</td><td>₹{s.basicSalary}</td><td>₹{s.hra}</td><td>₹{s.specialAllowance}</td>
                <td><strong>₹{s.grossSalary}</strong></td><td>{s.active ? 'Yes' : 'No'}</td>
                <td>{s.active && <button className="btn-secondary" onClick={() => remove(s.id)}>Delete</button>}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="card">
        <h2>Assign Structure to Employee</h2>
        <form onSubmit={assign} className="form-grid">
          <label>Employee
            <select value={assignForm.employeeId} onChange={(e) => setAssignForm({ ...assignForm, employeeId: e.target.value })} required>
              <option value="">-- Select --</option>
              {employees.map((e) => <option key={e.id} value={e.id}>{e.fullName} ({e.employeeCode})</option>)}
            </select>
          </label>
          <label>Structure
            <select value={assignForm.salaryStructureId} onChange={(e) => setAssignForm({ ...assignForm, salaryStructureId: e.target.value })} required>
              <option value="">-- Select --</option>
              {structures.filter((s) => s.active).map((s) => <option key={s.id} value={s.id}>{s.name} (₹{s.grossSalary})</option>)}
            </select>
          </label>
          <label>Effective From<input type="date" value={assignForm.effectiveFrom} onChange={(e) => setAssignForm({ ...assignForm, effectiveFrom: e.target.value })} required /></label>
          <button type="submit" className="form-grid-full">Assign</button>
        </form>
      </div>
    </div>
  );
}
