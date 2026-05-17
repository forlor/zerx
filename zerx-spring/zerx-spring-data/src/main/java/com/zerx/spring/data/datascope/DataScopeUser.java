package com.zerx.spring.data.datascope;

import java.util.List;

/**
 * 数据权限用户上下文信息。
 * <p>
 * 提供当前用户的 ID、部门 ID 等信息，供 {@link DataScopeHandler} 生成权限过滤条件。
 * 业务系统需要实现 {@link DataScopeUserProvider} 接口，根据实际用户体系提供此数据。
 * </p>
 *
 * @param userId   当前用户 ID
 * @param deptIds  当前用户所属部门 ID 列表（包含直接部门和间接部门）
 * @param roles    当前用户角色编码列表
 * @author zerx
 * @see DataScopeUserProvider
 */
public record DataScopeUser(Long userId, List<Long> deptIds, List<String> roles) {
}
