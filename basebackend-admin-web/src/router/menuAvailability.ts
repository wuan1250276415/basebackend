import type { MenuItem } from '@/types';

interface UnsupportedMenuEntry {
  name: string;
  path: string;
  reason: string;
}

const FEATURE_TOGGLE_REASON = '仓库内未检出 Feature Toggle 后端接口';

const UNSUPPORTED_MENU_NAMES = new Map<string, string>([
  ['特性开关', FEATURE_TOGGLE_REASON],
]);

const UNSUPPORTED_PATH_KEYS = new Map<string, string>([
  ['featuretoggle', FEATURE_TOGGLE_REASON],
  ['featuretoggles', FEATURE_TOGGLE_REASON],
]);

function normalizeMenuPath(path?: string): string {
  return (path ?? '').replace(/[^a-zA-Z0-9]/g, '').toLowerCase();
}

export function getUnsupportedMenuReason(menu: Pick<MenuItem, 'name' | 'path'>): string | null {
  const reasonByName = UNSUPPORTED_MENU_NAMES.get(menu.name);
  if (reasonByName) {
    return reasonByName;
  }

  const normalizedPath = normalizeMenuPath(menu.path);
  if (!normalizedPath) {
    return null;
  }

  return UNSUPPORTED_PATH_KEYS.get(normalizedPath) ?? null;
}

export function collectUnsupportedMenus(menus: MenuItem[]): UnsupportedMenuEntry[] {
  return menus.flatMap((menu) => {
    const reason = getUnsupportedMenuReason(menu);
    const current = reason
      ? [{
          name: menu.name,
          path: menu.path,
          reason,
        }]
      : [];

    const children = menu.children ? collectUnsupportedMenus(menu.children) : [];
    return [...current, ...children];
  });
}

export function filterUnsupportedMenus(menus: MenuItem[]): MenuItem[] {
  return menus.flatMap((menu) => {
    if (getUnsupportedMenuReason(menu)) {
      return [];
    }

    const children = menu.children ? filterUnsupportedMenus(menu.children) : undefined;
    if (menu.type === 0 && menu.children && (children?.length ?? 0) === 0) {
      return [];
    }

    return [{ ...menu, children }];
  });
}
