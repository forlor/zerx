package com.zerx.component.oss;

import java.io.Serializable;

/**
 * 对象存储暂存（预上传）结果
 * <p>
 * 表示一次预上传（staging）操作的结果。客户端收到此结果后，使用其中的
 * {@link PresignedUrl} 直接上传文件到暂存路径。上传完成后，客户端调用
 * {@link OssStorageService#confirm(String)} 提交暂存，或调用
 * {@link OssStorageService#cancel(String)} 取消暂存。
 * </p>
 *
 * <h3>工作流程：</h3>
 * <ol>
 *   <li>客户端请求预签名暂存 URL → 服务端返回 {@code OssStageResult}</li>
 *   <li>客户端使用 {@code presignedUrl} 直接 PUT 上传文件到对象存储</li>
 *   <li>上传成功后，客户端调用 {@code confirm(stageToken)} 将文件从暂存区移至正式存储</li>
 * </ol>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * OssStageResult stageResult = ossStorageService.presignStage(stageRequest);
 * // 将 stageResult.presignedUrl() 返回给前端
 * // 前端上传完成后调用 confirm(stageResult.stageToken())
 * }</pre>
 *
 * @param stageToken        暂存令牌（stage token），UUID 格式字符串。
 *                          用于后续调用 {@link OssStorageService#confirm(String)}
 *                          或 {@link OssStorageService#cancel(String)} 时标识此次暂存操作
 * @param presignedUrl      用于客户端直接上传的预签名 URL，包含签名参数和必要的请求头
 * @param stagingObjectKey  暂存路径（staging path），文件在对象存储中的临时存放位置。
 *                          文件在正式确认前存放在此路径下
 * @author zerx
 * @see OssStorageService#presignStage(OssStageRequest)
 * @see OssStorageService#confirm(String)
 * @see OssStorageService#cancel(String)
 */
public record OssStageResult(
        String stageToken,
        PresignedUrl presignedUrl,
        String stagingObjectKey
) implements Serializable {

    private static final long serialVersionUID = 1L;
}
