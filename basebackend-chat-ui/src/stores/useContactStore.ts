import { create } from 'zustand';
import type { Friend, FriendGroup, FriendRequest, BlacklistUser } from '@/types';

interface ContactState {
  /** 好友列表 */
  friends: Friend[];
  /** 好友分组 */
  friendGroups: FriendGroup[];
  /** 好友申请列表 */
  friendRequests: FriendRequest[];
  /** 黑名单 */
  blacklist: BlacklistUser[];
  /** 未处理的好友申请数 */
  pendingRequestCount: number;

  setFriends: (list: Friend[]) => void;
  setFriendGroups: (list: FriendGroup[]) => void;
  setFriendRequests: (list: FriendRequest[]) => void;
  setBlacklist: (list: BlacklistUser[]) => void;
  setPendingRequestCount: (n: number) => void;
  /** 移除好友 */
  removeFriend: (userId: number) => void;
}

export const useContactStore = create<ContactState>((set) => ({
  friends: [],
  friendGroups: [],
  friendRequests: [],
  blacklist: [],
  pendingRequestCount: 0,

  setFriends: (list) => set({ friends: list }),
  setFriendGroups: (list) => set({ friendGroups: list }),
  setFriendRequests: (list) => set({ friendRequests: list }),
  setBlacklist: (list) => set({ blacklist: list }),
  setPendingRequestCount: (n) => set({ pendingRequestCount: n }),
  removeFriend: (userId) =>
    set((state) => ({
      friends: state.friends.filter((f) => f.userId !== userId),
    })),
}));
