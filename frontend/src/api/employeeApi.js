import axiosClient from './axiosClient';

export const getMyProfile = () => axiosClient.get('/employees/me');
export const getAllEmployees = () => axiosClient.get('/employees');
export const getEmployee = (id) => axiosClient.get(`/employees/${id}`);
export const createEmployee = (data) => axiosClient.post('/employees', data);
export const updateEmployee = (id, data) => axiosClient.put(`/employees/${id}`, data);
export const deactivateEmployee = (id) => axiosClient.delete(`/employees/${id}`);
export const getDepartments = () => axiosClient.get('/departments');
