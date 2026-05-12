package com.zerx.common.constants;

/**
 * 全局通用常量
 * <p>
 * 定义项目中常用的通用常量值，避免魔法值散落在代码各处。
 * 所有常量均为 {@code public static final}，可直接通过类名引用。
 * </p>
 *
 * @author zerx
 */
public final class CommonConstant {

    /** 私有构造器，防止实例化 */
    private CommonConstant() {
        throw new UnsupportedOperationException("常量类不允许实例化");
    }

    // ======================== 布尔标识 ========================

    /** 是（字符串形式 "true"） */
    public static final String TRUE = "true";

    /** 否（字符串形式 "false"） */
    public static final String FALSE = "false";

    /** 数字是（1） */
    public static final Integer YES = 1;

    /** 数字否（0） */
    public static final Integer NO = 0;

    // ======================== 分隔符 ========================

    /** 逗号分隔符 */
    public static final String COMMA = ",";

    /** 点号分隔符 */
    public static final String DOT = ".";

    /** 冒号分隔符 */
    public static final String COLON = ":";

    /** 分号分隔符 */
    public static final String SEMICOLON = ";";

    /** 竖线分隔符 */
    public static final String PIPE = "|";

    /** 短横线 */
    public static final String HYPHEN = "-";

    /** 下划线 */
    public static final String UNDERSCORE = "_";

    /** 斜杠（正斜杠） */
    public static final String SLASH = "/";

    /** 反斜杠 */
    public static final String BACKSLASH = "\\";

    /** 空格 */
    public static final String SPACE = " ";

    /** 换行符 */
    public static final String NEWLINE = "\n";

    /** 回车换行符 */
    public static final String CRLF = "\r\n";

    // ======================== 默认值 ========================

    /** 默认字符集名称 */
    public static final String DEFAULT_CHARSET = "UTF-8";

    /** 默认日期格式 */
    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";

    /** 默认日期时间格式 */
    public static final String DEFAULT_DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /** 默认分页页码 */
    public static final int DEFAULT_PAGE = 1;

    /** 默认每页大小 */
    public static final int DEFAULT_PAGE_SIZE = 10;

    /** 每页最大记录数 */
    public static final int MAX_PAGE_SIZE = 200;

    /** 默认排序字段 */
    public static final String DEFAULT_SORT_FIELD = "createTime";

    /** 默认排序方向 */
    public static final String DEFAULT_SORT_DIRECTION = "desc";

    // ======================== 通用标识 ========================

    /** 根节点标识（如树形结构的根节点 ID） */
    public static final Long ROOT_ID = 0L;

    /** 根节点父级 ID（顶级节点的 parentId） */
    public static final Long ROOT_PARENT_ID = 0L;

    /** 默认排序序号 */
    public static final Integer DEFAULT_SORT_ORDER = 0;

    /** 逻辑删除标记：已删除 */
    public static final Integer DELETED = 1;

    /** 逻辑删除标记：未删除 */
    public static final Integer NOT_DELETED = 0;

    /** 启用状态 */
    public static final Integer STATUS_ENABLED = 1;

    /** 禁用状态 */
    public static final Integer STATUS_DISABLED = 0;

    /** 成功状态 */
    public static final Integer STATUS_SUCCESS = 1;

    /** 失败状态 */
    public static final Integer STATUS_FAIL = 0;

    /** 未知/全部标识（用于查询条件 -1 表示不限制） */
    public static final Integer ALL = -1;

    // ======================== HTTP 相关 ========================

    /** HTTP 请求头：Content-Type */
    public static final String HEADER_CONTENT_TYPE = "Content-Type";

    /** HTTP 请求头：Authorization */
    public static final String HEADER_AUTHORIZATION = "Authorization";

    /** HTTP 请求头：Accept */
    public static final String HEADER_ACCEPT = "Accept";

    /** HTTP 请求头：User-Agent */
    public static final String HEADER_USER_AGENT = "User-Agent";

    /** Content-Type: JSON */
    public static final String CONTENT_TYPE_JSON = "application/json";

    /** Content-Type: 表单 */
    public static final String CONTENT_TYPE_FORM = "application/x-www-form-urlencoded";

    /** Content-Type: 多部分表单 */
    public static final String CONTENT_TYPE_MULTIPART = "multipart/form-data";

    /** Bearer Token 前缀 */
    public static final String BEARER_PREFIX = "Bearer ";

    // ======================== 编码相关 ========================

    /** UTF-8 BOM 头 */
    public static final String UTF8_BOM = "\uFEFF";

    /** 星号（通配符） */
    public static final String ASTERISK = "*";

    /** 百分号 */
    public static final String PERCENT = "%";

    /** 井号 */
    public static final String HASH = "#";

    /** 艾特符号 */
    public static final String AT = "@";
}
