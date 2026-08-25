import { useEffect, useState } from 'react';
import { getAllEmployees, createEmployee, deactivateEmployee, getDepartments } from '../api/employeeApi';

const EMPTY_FORM = {
  firstName: '', lastName: '', email: '', password: '', role: 'EMPLOYEE',
  departmentId: '', designation: '', dateOfJoining: '',
};

export default function EmployeeManagement() {
  const [employees, setEmployees] = useState([]);
  const [departments, setDepartments] = useState([]);
  const [form, setForm] = useState(EMPTY_FORM);
  const [showForm, setShowForm] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  const load = () => {
    getAllEmployees().then((res) => setEmployees(res.data)).catch(() => {});
    getDepartments().then((res) => setDepartments(res.data)).catch(() => {});
  };

  useEffect(load, []);

  const submit = async (e) => {
    e.preventDefault();
    setMessage(''); setError('');
    try {
      await createEmployee({ ...form, departmentId: form.departmentId ? Number(form.departmentId) : null });
      setMessage('Employee created successfully. Default CL/SL/EL balances were allocated.');
      setForm(EMPTY_FORM);
      setShowForm(false);
      load();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to create employee.');
    }
  };

  const handleDeactivate = async (id) => {
    if (!window.confirm('Deactivate this employee?')) return;
    await deactivateEmployee(id);
    load();
  };

  return (
    <div className="page">
      <h1>Employees</h1>
      <button onClick={() => setShowForm((s) => !s)}>{showForm ? 'Cancel' : '+ Add Employee'}</button>

      {showForm && (
        <div className="card">
          {message && <div className="alert alert-success">{message}</div>}
          {error && <div className="alert alert-error">{error}</div>}
          <form onSubmit={submit} className="form-grid">
            <label>First Name<input value={form.firstName} onChange={(e) => setForm({ ...form, firstName: e.target.value })} required /></label>
            <label>Last Name<input value={form.lastName} onChange={(e) => setForm({ ...form, lastName: e.target.value })} required /></label>
            <label>Email<input type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} required /></label>
            <label>Temporary Password<input type="password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} required minLength={8} /></label>
            <label>Role
              <select value={form.role} onChange={(e) => setForm({ ...form, role: e.target.value })}>
                <option value="EMPLOYEE">Employee</option>
                <option value="HR">HR</option>
                <option value="ADMIN">Admin</option>
              </select>
            </label>
            <label>Department
              <select value={form.departmentId} onChange={(e) => setForm({ ...form, departmentId: e.target.value })}>
                <option value="">-- None --</option>
                {departments.map((d) => <option key={d.id} value={d.id}>{d.name}</option>)}
              </select>
            </label>
            <label>Designation<input value={form.designation} onChange={(e) => setForm({ ...form, designation: e.target.value })} /></label>
            <label>Date of Joining<input type="date" value={form.dateOfJoining} onChange={(e) => setForm({ ...form, dateOfJoining: e.target.value })} required /></label>
            <button type="submit" className="form-grid-full">Create Employee</button>
          </form>
        </div>
      )}

      <div className="card">
        <table className="data-table">
          <thead>
            <tr><th>ID</th><th>Name</th><th>Email</th><th>Department</th><th>Role</th><th>Status</th><th></th></tr>
          </thead>
          <tbody>
            {employees.map((emp) => (
              <tr key={emp.id}>
                <td>{emp.employeeCode}</td>
                <td>{emp.fullName}</td>
                <td>{emp.email}</td>
                <td>{emp.department || '-'}</td>
                <td>{emp.role}</td>
                <td>{emp.active ? 'Active' : 'Inactive'}</td>
                <td>
                  {emp.active && <button className="btn-secondary" onClick={() => handleDeactivate(emp.id)}>Deactivate</button>}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
