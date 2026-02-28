/**
 * 树形数据工具函数
 * 提供扁平列表与树结构之间的转换，以及树节点查找功能
 */

/** listToTree 的配置选项 */
interface ListToTreeOptions {
  /** 节点 ID 字段名，默认 'id' */
  idKey?: string;
  /** 父节点 ID 字段名，默认 'parentId' */
  parentIdKey?: string;
  /** 子节点数组字段名，默认 'children' */
  childrenKey?: string;
  /** 根节点的父 ID 值，默认 0 */
  rootParentId?: number | string | null;
}

/**
 * 将扁平列表转换为树形结构
 * @param list 扁平数据列表
 * @param options 配置选项（可选）
 * @returns 树形结构数组
 */
export function listToTree<T>(
  list: T[],
  options?: ListToTreeOptions,
): T[] {
  const {
    idKey = 'id',
    parentIdKey = 'parentId',
    childrenKey = 'children',
    rootParentId = 0,
  } = options ?? {};

  // 构建 id -> 节点映射（浅拷贝，避免修改原始数据）
  const map = new Map<unknown, T>();
  const cloned = list.map((item) => ({ ...item }));
  for (const item of cloned) {
    map.set((item as Record<string, unknown>)[idKey], item);
  }

  const tree: T[] = [];

  for (const item of cloned) {
    const parentId = (item as Record<string, unknown>)[parentIdKey];
    // 判断是否为根节点
    if (parentId === rootParentId || parentId === null || parentId === undefined) {
      tree.push(item);
    } else {
      const parent = map.get(parentId);
      if (parent) {
        // 初始化父节点的 children 数组
        const parentRec = parent as Record<string, unknown>;
        if (!Array.isArray(parentRec[childrenKey])) {
          parentRec[childrenKey] = [];
        }
        (parentRec[childrenKey] as T[]).push(item);
      } else {
        // 找不到父节点，作为根节点处理
        tree.push(item);
      }
    }
  }

  return tree;
}

/**
 * 将树形结构展平为扁平列表（深度优先遍历）
 * @param tree 树形结构数组
 * @param childrenKey 子节点数组字段名，默认 'children'
 * @returns 扁平列表
 */
export function flattenTree<T>(
  tree: T[],
  childrenKey: string = 'children',
): T[] {
  const result: T[] = [];

  const traverse = (nodes: T[]): void => {
    for (const node of nodes) {
      result.push(node);
      const children = (node as Record<string, unknown>)[childrenKey];
      if (Array.isArray(children) && children.length > 0) {
        traverse(children as T[]);
      }
    }
  };

  traverse(tree);
  return result;
}

/**
 * 在树形结构中查找满足条件的第一个节点（深度优先搜索）
 * @param tree 树形结构数组
 * @param predicate 匹配条件函数
 * @param childrenKey 子节点数组字段名，默认 'children'
 * @returns 匹配的节点，未找到返回 null
 */
export function findInTree<T>(
  tree: T[],
  predicate: (node: T) => boolean,
  childrenKey: string = 'children',
): T | null {
  for (const node of tree) {
    if (predicate(node)) {
      return node;
    }
    const children = (node as Record<string, unknown>)[childrenKey];
    if (Array.isArray(children) && children.length > 0) {
      const found = findInTree(children as T[], predicate, childrenKey);
      if (found) {
        return found;
      }
    }
  }
  return null;
}
