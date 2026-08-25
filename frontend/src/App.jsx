import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import PrivateRoute from './routes/PrivateRoute';
import Navbar from './components/Navbar';

import Login from './pages/Login';
import EmployeeDashboard from './pages/EmployeeDashboard';
import LeaveManagement from './pages/LeaveManagement';
import PayslipHistory from './pages/PayslipHistory';
import HRDashboard from './pages/HRDashboard';
import EmployeeManagement from './pages/EmployeeManagement';
import SalaryStructureManager from './pages/SalaryStructureManager';
import PayrollRun from './pages/PayrollRun';

function Shell() {
  const { user } = useAuth();
  return (
    <>
      <Navbar />
      <Routes>
        <Route path="/login" element={user ? <Navigate to="/dashboard" /> : <Login />} />

        <Route element={<PrivateRoute />}>
          <Route path="/dashboard" element={<EmployeeDashboard />} />
          <Route path="/leave" element={<LeaveManagement />} />
          <Route path="/payslips" element={<PayslipHistory />} />
        </Route>

        <Route element={<PrivateRoute requireHr />}>
          <Route path="/hr/dashboard" element={<HRDashboard />} />
          <Route path="/hr/employees" element={<EmployeeManagement />} />
          <Route path="/hr/salary-structures" element={<SalaryStructureManager />} />
          <Route path="/hr/leave-approvals" element={<LeaveManagement />} />
          <Route path="/hr/payroll-run" element={<PayrollRun />} />
        </Route>

        <Route path="*" element={<Navigate to={user ? '/dashboard' : '/login'} />} />
      </Routes>
    </>
  );
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Shell />
      </AuthProvider>
    </BrowserRouter>
  );
}
