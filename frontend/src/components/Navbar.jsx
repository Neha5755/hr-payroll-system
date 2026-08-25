import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function Navbar() {
  const { user, logout, isHrOrAdmin } = useAuth();
  const navigate = useNavigate();

  if (!user) return null;

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <nav className="navbar">
      <div className="navbar-brand">HR Payroll System</div>
      <div className="navbar-links">
        <Link to="/dashboard">My Dashboard</Link>
        <Link to="/leave">Leave</Link>
        <Link to="/payslips">Payslips</Link>
        {isHrOrAdmin && (
          <>
            <Link to="/hr/dashboard">HR Dashboard</Link>
            <Link to="/hr/employees">Employees</Link>
            <Link to="/hr/salary-structures">Salary Structures</Link>
            <Link to="/hr/leave-approvals">Leave Approvals</Link>
            <Link to="/hr/payroll-run">Run Payroll</Link>
          </>
        )}
      </div>
      <div className="navbar-user">
        <span>{user.fullName} ({user.role})</span>
        <button onClick={handleLogout}>Logout</button>
      </div>
    </nav>
  );
}
