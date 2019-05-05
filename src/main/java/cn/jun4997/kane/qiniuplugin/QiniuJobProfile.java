package cn.jun4997.kane.qiniuplugin;

import org.kohsuke.stapler.DataBoundConstructor;
/**
 * 
 * 七牛工作配置
 * @author kane
 * @Date 2019年4月30日
 */
public class QiniuJobProfile {
	// 配置文件
	public String profileName,
			// 文件路径
			source,
			// 去掉上传文件前缀
			removePrefix,
			// 七牛空间
			bucket;
	// 如果已存在则是否覆盖
	public boolean isUploadOnExists,
			// 构建失败则是否上传
			isUploadOnFailure;

	public QiniuJobProfile() {
	}

	@DataBoundConstructor
	public QiniuJobProfile(String profileName, String source, String removePrefix, String bucket, boolean isUploadOnFailure, boolean isUploadOnExists) {
		this.profileName = profileName;
		this.source = source;
		this.removePrefix = removePrefix;
		this.bucket = bucket;
		this.isUploadOnExists = isUploadOnExists;
		this.isUploadOnFailure = isUploadOnFailure;
	}

}
