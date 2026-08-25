import axiosClient from './axiosClient';

export const getAllSalaryStructures = () => axiosClient.get('/salary-structures');
export const createSalaryStructure = (data) => axiosClient.post('/salary-structures', data);
export const updateSalaryStructure = (id, data) => axiosClient.put(`/salary-structures/${id}`, data);
export const deleteSalaryStructure = (id) => axiosClient.delete(`/salary-structures/${id}`);
export const assignSalaryStructure = (data) => axiosClient.post('/salary-structures/assign', data);
export const getCurrentStructureForEmployee = (employeeId) =>
  axiosClient.get(`/salary-structures/employee/${employeeId}/current`);
export const getSalaryHistoryForEmployee = (employeeId) =>
  axiosClient.get(`/salary-structures/employee/${employeeId}/history`);
