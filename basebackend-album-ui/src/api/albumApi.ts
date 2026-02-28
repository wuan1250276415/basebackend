import request from './request';
export const getAlbums = () => request.get('/api/album/albums');
