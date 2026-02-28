import {create} from 'zustand';
export const useAlbumStore = create(() => ({ albums: [], currentAlbum: null }));
