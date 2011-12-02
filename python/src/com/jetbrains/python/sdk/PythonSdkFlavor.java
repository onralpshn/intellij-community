package com.jetbrains.python.sdk;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkAdditionalData;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.encoding.EncodingManager;
import com.jetbrains.python.psi.LanguageLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @author yole
 */
public abstract class PythonSdkFlavor {
  private static final Logger LOG = Logger.getInstance(PythonSdkFlavor.class);

  public static String appendSystemPythonPath(String pythonPath) {
    String syspath = System.getenv(PYTHONPATH);
    if (syspath != null) {
      pythonPath += File.pathSeparator + syspath;
    }
    return pythonPath;
  }

  public static void initPythonPath(Map<String, String> envs, boolean passParentEnvs, List<String> pythonPathList) {
    String pythonPath = StringUtil.join(pythonPathList, File.pathSeparator);
    if (passParentEnvs && !envs.containsKey(PYTHONPATH)) {
      pythonPath = appendSystemPythonPath(pythonPath);
    }
    addToPythonPath(envs, pythonPath);
  }

  public Collection<String> suggestHomePaths() {
    return Collections.emptyList();
  }

  public static List<PythonSdkFlavor> getApplicableFlavors() {
    List<PythonSdkFlavor> result = new ArrayList<PythonSdkFlavor>();
    if (SystemInfo.isWindows) {
      result.add(WinPythonSdkFlavor.INSTANCE);
    }
    else if (SystemInfo.isMac) {
      result.add(MacPythonSdkFlavor.INSTANCE);
    }
    else if (SystemInfo.isUnix) {
      result.add(UnixPythonSdkFlavor.INSTANCE);
    }
    result.add(JythonSdkFlavor.INSTANCE);
    result.add(IronPythonSdkFlavor.INSTANCE);
    result.add(PyPySdkFlavor.INSTANCE);
    return result;
  }

  @Nullable
  public static PythonSdkFlavor getFlavor(Sdk sdk) {
    final SdkAdditionalData data = sdk.getSdkAdditionalData();
    if (data instanceof PythonSdkAdditionalData) {
      PythonSdkFlavor flavor = ((PythonSdkAdditionalData)data).getFlavor();
      if (flavor != null) {
        return flavor;
      }
    }
    return getFlavor(sdk.getHomePath());
  }

  @Nullable
  public static PythonSdkFlavor getFlavor(String sdkPath) {
    for (PythonSdkFlavor flavor : getApplicableFlavors()) {
      if (flavor.isValidSdkHome(sdkPath)) {
        return flavor;
      }
    }
    return null;
  }

  /**
   * Checks if the path is the name of a Python interpreter of this flavor.
   *
   * @param path path to check.
   * @return true if paths points to a valid home.
   */
  public boolean isValidSdkHome(String path) {
    File file = new File(path);
    return file.isFile() && FileUtil.getNameWithoutExtension(file).toLowerCase().startsWith("python");
  }

  @Nullable
  public String getVersionString(String sdkHome) {
    return getVersionFromOutput(sdkHome, "-V", "(Python \\S+).*");
  }

  @Nullable
  protected static String getVersionFromOutput(String sdkHome, String version_opt, String version_regexp) {
    Pattern pattern = Pattern.compile(version_regexp);
    String run_dir = new File(sdkHome).getParent();
    final ProcessOutput process_output = SdkUtil.getProcessOutput(run_dir, new String[]{sdkHome, version_opt});
    if (process_output.getExitCode() != 0) {
      String err = process_output.getStderr();
      if (StringUtil.isEmpty(err)) {
        err = process_output.getStdout();
      }
      LOG.warn("Couldn't get interpreter version: process exited with code " + process_output.getExitCode() + "\n" + err
      );
      return null;
    }
    final String result = SdkUtil.getFirstMatch(process_output.getStderrLines(), pattern);
    if (result != null) {
      return result;
    }
    return SdkUtil.getFirstMatch(process_output.getStdoutLines(), pattern);
  }

  public Collection<String> getExtraDebugOptions() {
    return Collections.emptyList();
  }

  public void initPythonPath(GeneralCommandLine cmd, Collection<String> path) {
    addToEnv(cmd, PYTHONPATH, appendSystemPythonPath(StringUtil.join(path, File.pathSeparator)));
  }

  public static void addToEnv(GeneralCommandLine cmd, final String key, String value) {
    Map<String, String> envs = cmd.getEnvParams();
    if (envs == null) {
      envs = new HashMap<String, String>();
      cmd.setEnvParams(envs);
    }
    addToEnv(envs, key, value);
  }

  public static void addToPythonPath(Map<String, String> envs, String value) {
    addToEnv(envs, PYTHONPATH, value);
  }

  public static void addToEnv(Map<String, String> envs, String key, String value) {
    if (envs.containsKey(key)) {
      envs.put(key, value + File.pathSeparatorChar + envs.get(key));
    }
    else {
      envs.put(key, value);
    }
  }

  public static final String PYTHONPATH = "PYTHONPATH";

  @SuppressWarnings({"MethodMayBeStatic"})
  public void addPredefinedEnvironmentVariables(Map<String, String> envs) {
    Charset defaultCharset = EncodingManager.getInstance().getDefaultCharset();
    if (defaultCharset != null) {
      final String encoding = defaultCharset.name();
      PythonEnvUtil.setPythonIOEncoding(envs, encoding);
    }
  }

  @NotNull
  public abstract String getName();

  public LanguageLevel getLanguageLevel(Sdk sdk) {
    final String version = sdk.getVersionString();
    final String prefix = getName() + " ";
    if (version != null && version.startsWith(prefix)) {
      return LanguageLevel.fromPythonVersion(version.substring(prefix.length()));
    }
    return LanguageLevel.getDefault();
  }
}
