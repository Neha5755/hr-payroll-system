import axiosClient from './axiosClient';

export const runPayroll = (month, year) => axiosClient.post('/payroll/run', { month, year });
export const getMyPayslips = () => axiosClient.get('/payroll/payslips/my');
export const getEmployeePayslips = (employeeId) => axiosClient.get(`/payroll/payslips/employee/${employeeId}`);
export const getPayslip = (id) => axiosClient.get(`/payroll/payslips/${id}`);
export const retryEmail = (id) => axiosClient.post(`/payroll/payslips/${id}/retry-email`);
export const downloadPayslipUrl = (id) => `/api/payroll/payslips/${id}/download`;
