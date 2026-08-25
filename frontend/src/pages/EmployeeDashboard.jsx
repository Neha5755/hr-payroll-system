import { useEffect, useState } from 'react';
import { getMyProfile } from '../api/employeeApi';
import StatCard from '../components/StatCard';

export default function EmployeeDashboard() {
  const [profile, setProfile] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    getMyProfile()
      .then((res) => setProfile(res.data))
      .catch(() => setError('Could not load your profile.'));
  }, []);

  if (error) return <div className="page"><div className="alert alert-error">{error}</div></div>;
  if (!profile) return <div className="page">Loading…</div>;

  return (
    <div className="page">
      <h1>Welcome, {profile.fullName}</h1>
      <div className="card">
        <h2>Profile</h2>
        <div className="profile-grid">
          <div><strong>Employee ID:</strong> {profile.employeeCode}</div>
          <div><strong>Department:</strong> {profile.department || '-'}</div>
          <div><strong>Designation:</strong> {profile.designation || '-'}</div>
          <div><strong>Joined:</strong> {profile.dateOfJoining}</div>
          <div><strong>Email:</strong> {profile.email}</div>
          <div><strong>Status:</strong> {profile.active ? 'Active' : 'Inactive'}</div>
        </div>
      </div>

      <h2 className="section-title">Leave Balances</h2>
      <div className="stat-row">
        {profile.leaveBalances.map((b) => (
          <StatCard
            key={b.leaveTypeCode}
            label={`${b.leaveTypeName} (${b.leaveTypeCode})`}
            value={`${b.remaining} / ${b.allocated}`}
            tone={b.remaining === 0 ? 'danger' : 'default'}
          />
        ))}
      </div>
    </div>
  );
}
