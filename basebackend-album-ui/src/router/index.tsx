import React from 'react';
import { createBrowserRouter } from 'react-router-dom';
import MainLayout from '../layouts/MainLayout';
import Home from '../pages/Home';
import Albums from '../pages/Albums';
import AlbumDetail from '../pages/AlbumDetail';
import Timeline from '../pages/Timeline';
import Family from '../pages/Family';
import Trash from '../pages/Trash';
import Login from '../pages/Login';

const router = createBrowserRouter([
  {
    path: '/login',
    element: <Login />,
  },
  {
    path: '/',
    element: <MainLayout />,
    children: [
      { index: true, element: <Home /> },
      { path: 'albums', element: <Albums /> },
      { path: 'albums/:id', element: <AlbumDetail /> },
      { path: 'timeline', element: <Timeline /> },
      { path: 'family', element: <Family /> },
      { path: 'trash', element: <Trash /> },
    ],
  },
]);

export default router;
