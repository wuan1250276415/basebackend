import request from './request';
export const getPhotos = () => request.get('/api/album/photos');
