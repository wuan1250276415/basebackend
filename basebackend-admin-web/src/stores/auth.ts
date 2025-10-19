import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import { UserInfo } from '@/types'

interface AuthState {
  token: string | null
  userInfo: UserInfo | null
  permissions: string[]
  roles: string[]
  setToken: (token: string) => void
  setUserInfo: (userInfo: UserInfo) => void
  setPermissions: (permissions: string[]) => void
  setRoles: (roles: string[]) => void
  logout: () => void
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      token: null,
      userInfo: null,
      permissions: [],
      roles: [],
      setToken: (token) => set({ token }),
      setUserInfo: (userInfo) => set({ userInfo }),
      setPermissions: (permissions) => set({ permissions }),
      setRoles: (roles) => set({ roles }),
      logout: () => {
        set({
          token: null,
          userInfo: null,
          permissions: [],
          roles: [],
        })
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
      },
    }),
    {
      name: 'auth-storage',
    }
  )
)
