/**
 * 树形数据工具函数单元测试
 */
import { describe, it, expect } from 'vitest';
import { listToTree, flattenTree, findInTree } from '@/utils/tree';

// 测试用的节点类型
interface TestNode {
  id: number;
  parentId: number;
  name: string;
  children?: TestNode[];
}

describe('listToTree', () => {
  it('应将扁平列表转换为树形结构', () => {
    const list: TestNode[] = [
      { id: 1, parentId: 0, name: '根节点' },
      { id: 2, parentId: 1, name: '子节点A' },
      { id: 3, parentId: 1, name: '子节点B' },
      { id: 4, parentId: 2, name: '孙节点' },
    ];

    const tree = listToTree(list);
    expect(tree).toHaveLength(1);
    expect(tree[0].name).toBe('根节点');
    expect((tree[0].children as TestNode[])).toHaveLength(2);
    expect((tree[0].children as TestNode[])[0].name).toBe('子节点A');
    expect(((tree[0].children as TestNode[])[0].children as TestNode[])[0].name).toBe('孙节点');
  });

  it('空列表应返回空数组', () => {
    expect(listToTree([])).toEqual([]);
  });

  it('所有节点都是根节点时应返回扁平数组', () => {
    const list = [
      { id: 1, parentId: 0, name: 'A' },
      { id: 2, parentId: 0, name: 'B' },
    ];
    const tree = listToTree(list);
    expect(tree).toHaveLength(2);
  });

  it('应支持自定义字段名', () => {
    const list = [
      { uid: 1, pid: null, label: '根' },
      { uid: 2, pid: 1, label: '子' },
    ];
    const tree = listToTree(list, {
      idKey: 'uid',
      parentIdKey: 'pid',
      childrenKey: 'items',
      rootParentId: null,
    });
    expect(tree).toHaveLength(1);
    expect((tree[0] as Record<string, unknown>).items).toHaveLength(1);
  });

  it('父节点不存在时应将节点作为根节点', () => {
    const list = [
      { id: 1, parentId: 0, name: '根' },
      { id: 2, parentId: 999, name: '孤儿' },
    ];
    const tree = listToTree(list);
    // 根节点 + 孤儿节点都在顶层
    expect(tree).toHaveLength(2);
  });

  it('不应修改原始数据', () => {
    const list: TestNode[] = [
      { id: 1, parentId: 0, name: '根' },
      { id: 2, parentId: 1, name: '子' },
    ];
    const original = JSON.parse(JSON.stringify(list));
    listToTree(list);
    expect(list).toEqual(original);
  });
});

describe('flattenTree', () => {
  it('应将树形结构展平为扁平列表', () => {
    const tree: TestNode[] = [
      {
        id: 1, parentId: 0, name: '根',
        children: [
          { id: 2, parentId: 1, name: '子A' },
          {
            id: 3, parentId: 1, name: '子B',
            children: [{ id: 4, parentId: 3, name: '孙' }],
          },
        ],
      },
    ];

    const flat = flattenTree(tree);
    expect(flat).toHaveLength(4);
    // 深度优先顺序
    expect(flat.map((n) => n.id)).toEqual([1, 2, 3, 4]);
  });

  it('空树应返回空数组', () => {
    expect(flattenTree([])).toEqual([]);
  });

  it('无子节点的树应返回自身', () => {
    const tree = [{ id: 1, parentId: 0, name: '单节点' }];
    const flat = flattenTree(tree);
    expect(flat).toHaveLength(1);
  });

  it('应支持自定义 childrenKey', () => {
    const tree = [
      {
        id: 1, parentId: 0, name: '根',
        items: [{ id: 2, parentId: 1, name: '子' }],
      },
    ];
    const flat = flattenTree(tree, 'items');
    expect(flat).toHaveLength(2);
  });
});

describe('findInTree', () => {
  const tree: TestNode[] = [
    {
      id: 1, parentId: 0, name: '系统管理',
      children: [
        { id: 2, parentId: 1, name: '用户管理' },
        {
          id: 3, parentId: 1, name: '角色管理',
          children: [{ id: 4, parentId: 3, name: '权限分配' }],
        },
      ],
    },
    { id: 5, parentId: 0, name: '监控中心' },
  ];

  it('应找到根节点', () => {
    const found = findInTree(tree, (n) => n.id === 1);
    expect(found).not.toBeNull();
    expect(found!.name).toBe('系统管理');
  });

  it('应找到深层嵌套节点', () => {
    const found = findInTree(tree, (n) => n.id === 4);
    expect(found).not.toBeNull();
    expect(found!.name).toBe('权限分配');
  });

  it('应找到第二棵树的节点', () => {
    const found = findInTree(tree, (n) => n.name === '监控中心');
    expect(found).not.toBeNull();
    expect(found!.id).toBe(5);
  });

  it('未找到时应返回 null', () => {
    const found = findInTree(tree, (n) => n.id === 999);
    expect(found).toBeNull();
  });

  it('空树应返回 null', () => {
    const found = findInTree([], () => true);
    expect(found).toBeNull();
  });

  it('应支持自定义 childrenKey', () => {
    const customTree = [
      {
        id: 1, parentId: 0, name: '根',
        items: [{ id: 2, parentId: 1, name: '目标' }],
      },
    ];
    const found = findInTree(customTree, (n) => n.id === 2, 'items');
    expect(found).not.toBeNull();
    expect(found!.name).toBe('目标');
  });
});

describe('listToTree + flattenTree 往返一致性', () => {
  it('展平后的节点数量应与原始列表一致', () => {
    const list: TestNode[] = [
      { id: 1, parentId: 0, name: 'A' },
      { id: 2, parentId: 1, name: 'B' },
      { id: 3, parentId: 1, name: 'C' },
      { id: 4, parentId: 2, name: 'D' },
      { id: 5, parentId: 0, name: 'E' },
    ];

    const tree = listToTree(list);
    const flat = flattenTree(tree);
    expect(flat).toHaveLength(list.length);
    // 所有原始 id 都应存在
    const ids = flat.map((n) => n.id);
    for (const item of list) {
      expect(ids).toContain(item.id);
    }
  });
});
