import {create} from 'zustand';
export const usePhotoStore = create(() => ({ photos: [], selectedPhotos: [] }));
