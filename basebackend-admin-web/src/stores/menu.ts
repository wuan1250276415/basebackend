import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import { Menu } from '@/types'

interface MenuState {
  menuList: Menu[]
  collapsed: boolean
  setMenuList: (menuList: Menu[]) => void
  setCollapsed: (collapsed: boolean) => void
  clearMenu: () => void
}

export const useMenuStore = create<MenuState>()(
  persist(
    (set) => ({
      menuList: [],
      collapsed: false,
      setMenuList: (menuList) => set({ menuList }),
      setCollapsed: (collapsed) => set({ collapsed }),
      clearMenu: () => set({ menuList: [] }),
    }),
    {
      name: 'menu-storage',
    }
  )
)
