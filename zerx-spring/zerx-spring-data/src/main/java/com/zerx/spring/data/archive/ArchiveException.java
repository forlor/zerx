package com.zerx.spring.data.archive;

/**
 * 归档操作异常。
 * <p>
 * 当归档写入失败时抛出此异常，阻止后续的物理删除操作，
 * 保证已删除的数据不会丢失。属于可恢复的业务异常。
 * </p>
 *
 * @author zerx
 */
public class ArchiveException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 归档的实体类型
     */
    private final Class<?> entityClass;

    /**
     * 归档的实体 ID
     */
    private final Object entityId;

    /**
     * 构造归档异常
     *
     * @param entityClass 实体类型
     * @param entityId    实体 ID
     * @param message     异常消息
     */
    public ArchiveException(Class<?> entityClass, Object entityId, String message) {
        super(String.format("Archive failed for %s[id=%s]: %s",
                entityClass.getSimpleName(), entityId, message));
        this.entityClass = entityClass;
        this.entityId = entityId;
    }

    /**
     * 构造归档异常（携带原始异常）
     *
     * @param entityClass 实体类型
     * @param entityId    实体 ID
     * @param message     异常消息
     * @param cause       原始异常
     */
    public ArchiveException(Class<?> entityClass, Object entityId, String message, Throwable cause) {
        super(String.format("Archive failed for %s[id=%s]: %s",
                entityClass.getSimpleName(), entityId, message), cause);
        this.entityClass = entityClass;
        this.entityId = entityId;
    }

    /**
     * 获取归档的实体类型
     *
     * @return 实体类型
     */
    public Class<?> getEntityClass() {
        return entityClass;
    }

    /**
     * 获取归档的实体 ID
     *
     * @return 实体 ID
     */
    public Object getEntityId() {
        return entityId;
    }
}
