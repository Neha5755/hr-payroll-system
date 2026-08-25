import axiosClient from './axiosClient';

export const applyForLeave = (data) => axiosClient.post('/leave/apply', data);
export const getMyLeaveHistory = () => axiosClient.get('/leave/my-history');
export const getMyLeaveBalance = () => axiosClient.get('/leave/my-balance');
export const cancelLeave = (id) => axiosClient.post(`/leave/${id}/cancel`);
export const getPendingLeaveRequests = () => axiosClient.get('/leave/pending');
export const approveLeave = (id) => axiosClient.post(`/leave/approve/${id}`);
export const rejectLeave = (id) => axiosClient.post(`/leave/reject/${id}`);
export const getEmployeeLeaveBalance = (employeeId, year) =>
  axiosClient.get(`/leave/employee/${employeeId}/balance`, { params: { year } });
