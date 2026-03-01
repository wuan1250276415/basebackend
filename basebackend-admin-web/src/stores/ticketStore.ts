import { create } from 'zustand';
import { ticketApi } from '@/api/ticketApi';
import type {
  TicketListItem,
  TicketDetail,
  TicketOverview,
  TicketCategoryTree,
  TicketQueryDTO,
} from '@/api/ticketApi';

export interface TicketState {
  // 列表
  loading: boolean;
  list: TicketListItem[];
  total: number;
  query: TicketQueryDTO & { current: number; size: number };

  // 详情
  detail: TicketDetail | null;
  detailLoading: boolean;

  // 统计
  overview: TicketOverview | null;

  // 分类
  categories: TicketCategoryTree[];

  // Actions
  fetchPage: (params?: Partial<TicketQueryDTO & { current: number; size: number }>) => Promise<void>;
  fetchDetail: (id: number) => Promise<void>;
  fetchOverview: () => Promise<void>;
  fetchCategories: () => Promise<void>;
  setQuery: (query: Partial<TicketQueryDTO & { current: number; size: number }>) => void;
  resetQuery: () => void;
}

const defaultQuery: TicketQueryDTO & { current: number; size: number } = {
  current: 1,
  size: 10,
};

export const useTicketStore = create<TicketState>()((set, get) => ({
  loading: false,
  list: [],
  total: 0,
  query: { ...defaultQuery },
  detail: null,
  detailLoading: false,
  overview: null,
  categories: [],

  fetchPage: async (params) => {
    const query = { ...get().query, ...params };
    set({ loading: true, query });
    try {
      const res = await ticketApi.page(query);
      set({ list: res.records, total: res.total });
    } finally {
      set({ loading: false });
    }
  },

  fetchDetail: async (id) => {
    set({ detailLoading: true });
    try {
      const detail = await ticketApi.getDetail(id);
      set({ detail });
    } finally {
      set({ detailLoading: false });
    }
  },

  fetchOverview: async () => {
    const overview = await ticketApi.getOverview();
    set({ overview });
  },

  fetchCategories: async () => {
    const categories = await ticketApi.getCategoryTree();
    set({ categories });
  },

  setQuery: (query) => {
    set({ query: { ...get().query, ...query } });
  },

  resetQuery: () => {
    set({ query: { ...defaultQuery } });
  },
}));
