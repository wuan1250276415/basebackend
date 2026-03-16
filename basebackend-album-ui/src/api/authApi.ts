import request from './request';
export const login = (data: any) => request.post('/api/user/login', data);
