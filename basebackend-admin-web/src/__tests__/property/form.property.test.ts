// Feature: admin-web-rebuild, Property 11: Password confirmation validation
// **Validates: Requirements 19.4**

/**
 * 密码确认验证属性基测试
 * 使用 fast-check 生成随机密码对，验证密码修改表单的验证逻辑：
 * - newPassword === confirmPassword 且两者非空时允许提交
 * - 两者不匹配或为空时应阻止提交并显示验证错误
 */
import { describe, it, expect } from 'vitest';
import fc from 'fast-check';

/**
 * 密码验证规则（与 profile/index.tsx 中的表单规则一致）
 *
 * oldPassword: 必填
 * newPassword: 必填，最少 6 位
 * confirmPassword: 必填，必须与 newPassword 一致
 */

/** 验证旧密码：必填 */
function validateOldPassword(value: string): { valid: boolean; message?: string } {
  if (!value) {
    return { valid: false, message: '请输入旧密码' };
  }
  return { valid: true };
}

/** 验证新密码：必填，最少 6 位 */
function validateNewPassword(value: string): { valid: boolean; message?: string } {
  if (!value) {
    return { valid: false, message: '请输入新密码' };
  }
  if (value.length < 6) {
    return { valid: false, message: '密码长度不能少于6位' };
  }
  return { valid: true };
}

/** 验证确认密码：必填，必须与 newPassword 一致 */
function validateConfirmPassword(
  confirmPassword: string,
  newPassword: string,
): { valid: boolean; message?: string } {
  if (!confirmPassword) {
    return { valid: false, message: '请确认新密码' };
  }
  if (confirmPassword !== newPassword) {
    return { valid: false, message: '两次输入的密码不一致' };
  }
  return { valid: true };
}

/** 综合验证：所有字段都通过时才允许提交 */
function validatePasswordForm(
  oldPassword: string,
  newPassword: string,
  confirmPassword: string,
): { canSubmit: boolean; errors: string[] } {
  const errors: string[] = [];
  const oldResult = validateOldPassword(oldPassword);
  if (!oldResult.valid) errors.push(oldResult.message!);
  const newResult = validateNewPassword(newPassword);
  if (!newResult.valid) errors.push(newResult.message!);
  const confirmResult = validateConfirmPassword(confirmPassword, newPassword);
  if (!confirmResult.valid) errors.push(confirmResult.message!);
  return { canSubmit: errors.length === 0, errors };
}

// 生成非空密码字符串（至少 6 位，满足最小长度要求）
const validPasswordArb = fc.string({ minLength: 6, maxLength: 30 }).filter((s) => s.length >= 6);

// 生成任意字符串（可能为空、可能短于 6 位）
const anyStringArb = fc.string({ minLength: 0, maxLength: 30 });

describe('密码确认验证属性基测试', () => {
  // Feature: admin-web-rebuild, Property 11: Password confirmation validation
  describe('Property 11: 匹配的非空密码对允许提交', () => {
    it('当 newPassword === confirmPassword 且满足所有规则时，表单应允许提交', () => {
      fc.assert(
        fc.property(validPasswordArb, validPasswordArb, (oldPwd, newPwd) => {
          // 确认密码与新密码相同
          const result = validatePasswordForm(oldPwd, newPwd, newPwd);
          expect(result.canSubmit).toBe(true);
          expect(result.errors).toHaveLength(0);
        }),
        { numRuns: 100 },
      );
    });
  });

  // Feature: admin-web-rebuild, Property 11: Password confirmation validation
  describe('Property 11: 不匹配的密码对阻止提交', () => {
    it('当 newPassword !== confirmPassword 时，表单应阻止提交并显示错误', () => {
      fc.assert(
        fc.property(
          validPasswordArb,
          validPasswordArb,
          validPasswordArb,
          (oldPwd, newPwd, confirmPwd) => {
            // 仅在两个密码不同时测试
            fc.pre(newPwd !== confirmPwd);

            const result = validatePasswordForm(oldPwd, newPwd, confirmPwd);
            expect(result.canSubmit).toBe(false);
            expect(result.errors).toContain('两次输入的密码不一致');
          },
        ),
        { numRuns: 100 },
      );
    });
  });

  // Feature: admin-web-rebuild, Property 11: Password confirmation validation
  describe('Property 11: 空确认密码阻止提交', () => {
    it('当 confirmPassword 为空时，表单应阻止提交', () => {
      fc.assert(
        fc.property(validPasswordArb, validPasswordArb, (oldPwd, newPwd) => {
          const result = validatePasswordForm(oldPwd, newPwd, '');
          expect(result.canSubmit).toBe(false);
          expect(result.errors).toContain('请确认新密码');
        }),
        { numRuns: 100 },
      );
    });
  });

  // Feature: admin-web-rebuild, Property 11: Password confirmation validation
  describe('Property 11: 空新密码阻止提交', () => {
    it('当 newPassword 为空时，表单应阻止提交', () => {
      fc.assert(
        fc.property(validPasswordArb, anyStringArb, (oldPwd, confirmPwd) => {
          const result = validatePasswordForm(oldPwd, '', confirmPwd);
          expect(result.canSubmit).toBe(false);
          expect(result.errors).toContain('请输入新密码');
        }),
        { numRuns: 100 },
      );
    });
  });

  // Feature: admin-web-rebuild, Property 11: Password confirmation validation
  describe('Property 11: 新密码少于6位阻止提交', () => {
    it('当 newPassword 长度小于 6 时，即使确认密码匹配也应阻止提交', () => {
      // 生成 1-5 位的短密码
      const shortPasswordArb = fc.string({ minLength: 1, maxLength: 5 }).filter((s) => s.length >= 1 && s.length < 6);

      fc.assert(
        fc.property(validPasswordArb, shortPasswordArb, (oldPwd, shortPwd) => {
          // 确认密码与新密码相同，但新密码太短
          const result = validatePasswordForm(oldPwd, shortPwd, shortPwd);
          expect(result.canSubmit).toBe(false);
          expect(result.errors).toContain('密码长度不能少于6位');
        }),
        { numRuns: 100 },
      );
    });
  });

  // Feature: admin-web-rebuild, Property 11: Password confirmation validation
  describe('Property 11: 空旧密码阻止提交', () => {
    it('当 oldPassword 为空时，即使新密码和确认密码匹配也应阻止提交', () => {
      fc.assert(
        fc.property(validPasswordArb, (newPwd) => {
          const result = validatePasswordForm('', newPwd, newPwd);
          expect(result.canSubmit).toBe(false);
          expect(result.errors).toContain('请输入旧密码');
        }),
        { numRuns: 100 },
      );
    });
  });
});
