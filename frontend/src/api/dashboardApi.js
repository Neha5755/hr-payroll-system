import axiosClient from './axiosClient';

export const getDashboard = (month, year) =>
  axiosClient.get('/dashboard', { params: { month, year } });
