import request from './request';
export const getComments = (photoId: string) => request.get(`/api/album/comments/photo/${photoId}`);
