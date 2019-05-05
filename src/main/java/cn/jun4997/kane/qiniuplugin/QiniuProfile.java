package cn.jun4997.kane.qiniuplugin;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * 七牛密钥配置
 * 
 * @author kane
 * @Date 2019年4月30日
 */
public class QiniuProfile {
	private String name, accessKey, secretKey;

	@DataBoundConstructor
	public QiniuProfile(String name, String accessKey, String secretKey) {
		this.setName(name);
		this.setAccessKey(accessKey);
		this.setSecretKey(secretKey);
	}

	public QiniuProfile() {
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getAccessKey() {
		return accessKey;
	}

	public void setAccessKey(String accessKey) {
		this.accessKey = accessKey;
	}

	public String getSecretKey() {
		return secretKey;
	}

	public void setSecretKey(String secretKey) {
		this.secretKey = secretKey;
	}
}
