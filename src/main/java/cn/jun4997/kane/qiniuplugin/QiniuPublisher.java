package cn.jun4997.kane.qiniuplugin;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;

import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.model.DefaultPutRet;
import com.qiniu.util.Auth;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Builder;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.CopyOnWriteList;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;

/**
 * Sample {@link Builder}.
 * <p>
 * When the user configures the project and enables this builder,
 * {@link DescriptorImpl#newInstance(StaplerRequest)} is invoked and a new
 * {@link QiniuPublisher} is created. The created instance is persisted to the
 * project configuration XML by using XStream, so this allows you to use
 * instance fields (like {@link #name}) to remember the configuration.
 * <p>
 * When a build is performed, the
 * {@link #perform(AbstractBuild, Launcher, BuildListener)} method will be
 * invoked.
 * 
 * @author kane
 * @Date 2019年4月30日
 */
public class QiniuPublisher extends Recorder {

	private final List<QiniuJobProfile> entries = new ArrayList<QiniuJobProfile>();

	public QiniuPublisher() {
		super();
	}

	private Auth auth;
	private UploadManager uploadManager;
	// private OperationManager operater;
	// private CdnManager cndManager;

	private void initQiniu(QiniuProfile profile, PrintStream logger) {
		if (uploadManager == null) {
			if (auth == null) {
				auth = Auth.create(profile.getAccessKey(), profile.getSecretKey());
			}
			uploadManager = new UploadManager(new Configuration());
		}
		// if (operater == null) {
		// if (auth == null) {
		// auth = Auth.create(profile.getAccessKey(), profile.getSecretKey());
		// }
		// operater = new OperationManager(auth, new Configuration());
		// }
		// if (cndManager == null) {
		// if (auth == null) {
		// auth = Auth.create(profile.getAccessKey(), profile.getSecretKey());
		// }
		// cndManager = new CdnManager(auth);
		// }
	}

	@Override
	public boolean perform(@SuppressWarnings("rawtypes") AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
		// This is where you 'build' the project.
		// Since this is a dummy, we just say 'hello world' and call that a
		// build.
		if (null == listener) {
			return false;
		}

		// 日志
		PrintStream logger = listener.getLogger();
		// This also shows how you can consult the global configuration of the
		// builder
		if (null == build) {
			logger.println("找不基础参数...");
			return false;
		}

		// 工作空间
		FilePath ws = build.getWorkspace();
		if (null == ws || !ws.exists()) {
			logger.println("找不到工作空间...");
			return false;
		}
		// logger.println("======工作空间=====");
		// logger.println(ws.getName());
		// logger.println(ws.getRemote());
		// try {
		// logger.println(JSONObject.fromObject(ws).toString());
		// } catch (Exception e) {
		// }
		// logger.println("======工作空间=====");

		String wsPath = ws.getRemote() + File.separator;

		// 返回包含用于启动此生成过程的环境变量的映射。
		Map<String, String> envVars = build.getEnvironment(listener);
		// logger.println("环境变量");
		// logger.println(JSONObject.fromObject(envVars).toString());
		// 生成有一个致命错误
		final boolean buildFailed = build.getResult() == Result.FAILURE;

		logger.println("开始上传到七牛...");
		for (QiniuJobProfile entry : this.entries) {

			if (!entry.isUploadOnFailure && buildFailed) {
				logger.println("构建失败,跳过上传");
				return false;
			}

			// 配置项
			QiniuProfile profile = this.getDescriptor().getProfileByName(entry.profileName);
			if (profile == null) {
				logger.println("找不到配置项,跳过");
				return false;
			}
			// 初始化七牛
			initQiniu(profile, logger);
			// 创建对应空间的上传token
			String upToken = auth.uploadToken(entry.bucket);
			// logger.println(entry.source);
			String expanded = Util.replaceMacro(entry.source, envVars);
			// logger.println(expanded);
			FilePath[] paths = ws.list(expanded);
			for (FilePath path : paths) {
				// 删除工作空间路径
				String keyPath = path.getRemote().replace(wsPath, "");
				String key = keyPath.replace(File.separator, "/").replace(entry.removePrefix, "");
				try {
					// 是否覆盖上传token
					if (entry.isUploadOnExists) {
						upToken = auth.uploadToken(entry.bucket, key);
					}
					Response response = uploadManager.put(path.getRemote(), key, upToken);
					if (response.isOK()) {
						DefaultPutRet ret = (DefaultPutRet) JSONObject.toBean(JSONObject.fromObject(response.bodyString()), DefaultPutRet.class);
						logger.println("上传 " + keyPath + " 到 " + entry.bucket + " 成功: " + ret.key);
					} else {
						logger.println("上传 " + keyPath + " 到 " + entry.bucket + " 失败: ");
						String error = response.error;
						if (error != null) {
							logger.println(error);
						}
						logger.println();
					}
				} catch (Exception e) {
					e.printStackTrace();
					build.setResult(Result.UNSTABLE);
					return false;
				}
			}

		}
		logger.println("上传到七牛已完成...");
		return true;
	}

	// Overridden for better type safety.
	// If your plugin doesn't really define any property on Descriptor,
	// you don't have to do this.
	@Override
	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl) super.getDescriptor();
	}

	/**
	 * Descriptor for {@link QiniuPublisher}. Used as a singleton. The class is
	 * marked as public so that it can be accessed from views.
	 * <p>
	 * See
	 * <tt>src/main/resources/cn/jun4997/kane/qiniuplugin/QiniuPublisher/*.jelly</tt>
	 * for the actual HTML fragment for the configuration screen.
	 */
	@Extension
	// This indicates to Jenkins that this is an implementation of an extension point.
	public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
		/**
		 * To persist global configuration information, simply store it in a
		 * field and call save().
		 * <p>
		 * If you don't want fields to be persisted, use <tt>transient</tt>.
		 */
		private final CopyOnWriteList<QiniuProfile> profiles = new CopyOnWriteList<QiniuProfile>();

		public List<QiniuProfile> getProfiles() {
			return Arrays.asList(profiles.toArray(new QiniuProfile[0]));
		}

		public QiniuProfile getProfileByName(String profileName) {
			List<QiniuProfile> profiles = this.getProfiles();
			for (QiniuProfile profile : profiles) {
				if (null != profile) {
					if (profileName.equals(profile.getName())) {
						return profile;
					}
				}
			}
			return null;
		}

		/**
		 * In order to load the persisted global configuration, you have to call
		 * load() in the constructor.
		 */
		public DescriptorImpl() {
			load();
		}

		/**
		 * Performs on-the-fly validation of the form field 'name'.
		 * 
		 * @param value
		 *              This parameter receives the value that the user has typed.
		 * @return Indicates the outcome of the validation. This is sent to the
		 *         browser.
		 *         <p>
		 *         Note that returning {@link FormValidation#error(String)} does
		 *         not prevent the form from being saved. It just means that a
		 *         message will be displayed to the user.
		 */
		public FormValidation doCheckAccessKey(@QueryParameter String value) throws IOException, ServletException {
			if (value.length() == 0)
				return FormValidation.error("Access Key 不能为空");
			return FormValidation.ok();
		}

		public FormValidation doCheckProfileName(@QueryParameter String value) throws IOException, ServletException {
			if (value.length() == 0)
				return FormValidation.error("配置项名称不能为空");
			return FormValidation.ok();
		}

		public boolean isApplicable(@SuppressWarnings("rawtypes") Class<? extends AbstractProject> aClass) {
			// Indicates that this builder can be used with all kinds of project types
			return true;
		}

		/**
		 * This human readable name is used in the configuration screen.
		 */
		public String getDisplayName() {
			return "上传到七牛";
		}

		@Override
		public QiniuPublisher newInstance(StaplerRequest req, JSONObject formData) throws FormException {
			if (null == req) {
				return null;
			}
			List<QiniuJobProfile> entries = req.bindJSONToList(QiniuJobProfile.class, formData.get("jobProfile"));
			QiniuPublisher pub = new QiniuPublisher();
			pub.getEntries().addAll(entries);
			return pub;
		}

		@Override
		public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
			profiles.replaceBy(req.bindJSONToList(QiniuProfile.class, formData.get("profile")));
			save();
			return true;
		}
	}

	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.STEP;
	}

	public List<QiniuJobProfile> getEntries() {
		return entries;
	}

}
