package cu.axel.smartdock.utils;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.PreferenceManager;
import android.view.accessibility.AccessibilityManager;
import android.widget.Toast;
import androidx.core.content.ContextCompat;
import cu.axel.smartdock.services.DockService;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.List;
import android.util.Log;
import android.Manifest;
import android.content.pm.PackageManager;
import android.view.Display;
import android.hardware.display.DisplayManager;
import android.util.DisplayMetrics;

public class DeviceUtils {

	public static boolean lockScreen(Context context) {

		DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
		try {
			dpm.lockNow();
		} catch (SecurityException e) {
			return false;
		}
		return true;

	}

	public static void sendKeyEvent(int keycode) {
		runAsRoot("input keyevent " + keycode);
	}

	public static Process getRootAccess() throws IOException {
		String[] paths = { "/sbin/su", "/system/sbin/su", "/system/bin/su", "/system/xbin/su", "/su/bin/su",
				"/magisk/.core/bin/su" };
		for (String path : paths) {
			if (new java.io.File(path).exists())
				return Runtime.getRuntime().exec(path);
		}
		return Runtime.getRuntime().exec("/system/bin/su");
	}

	public static String runAsRoot(String command) {
		String output = "";
		try {
			java.lang.Process proccess = getRootAccess();
			DataOutputStream os = new DataOutputStream(proccess.getOutputStream());
			os.writeBytes(command + "\n");
			os.flush();
			os.close();
			BufferedReader br = new BufferedReader(new InputStreamReader(proccess.getInputStream()));
			String line;
			while ((line = br.readLine()) != null) {
				output += line + "\n";
			}
			br.close();
		} catch (IOException e) {
			return "error";
		}
		return output;
	}

	public static void sotfReboot() {
		runAsRoot("setprop ctl.restart zygote");
	}

	public static void reboot() {
		runAsRoot("am start -a android.intent.action.REBOOT");
	}

	public static void shutdown() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
			runAsRoot("am start -a android.intent.action.ACTION_REQUEST_SHUTDOWN");
		else
			runAsRoot("am start -a com.android.internal.intent.action.REQUEST_SHUTDOWN");
	}

	public static void setDisplaySize(int size) {
		if (size > 0)
			runAsRoot("settings put secure display_density_forced " + size);
		else
			runAsRoot("settings delete secure display_density_forced");
	}

	public static void toggleVolume(Context context) {
		AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI);

	}

	public static boolean isAccessibilityServiceEnabled(Context context) {
		AccessibilityManager am = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
		List<AccessibilityServiceInfo> enabledServices = am
				.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);

		for (AccessibilityServiceInfo enabledService : enabledServices) {
			ServiceInfo serviceInfo = enabledService.getResolveInfo().serviceInfo;
			if (serviceInfo.packageName.equals(context.getPackageName())
					&& serviceInfo.name.equals(DockService.class.getName())) {
				return true;
			}
		}

		return false;
	}

	public static int getStatusBarHeight(Context context) {
		int result = 0;
		int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
		if (resourceId > 0) {
			result = context.getResources().getDimensionPixelSize(resourceId);
		}
		return result;
	}

	public static String getUserName(Context context) {
		UserManager um = (UserManager) context.getSystemService(Context.USER_SERVICE);
		try {
			return um.getUserName();
		} catch (Exception e) {

		}
		return null;
	}

	public static Bitmap getUserIcon(Context context) {
		UserManager um = (UserManager) context.getSystemService(Context.USER_SERVICE);
		Bitmap userIcon = null;
		try {
			Method getUserIcon = um.getClass().getMethod("getUserIcon", int.class);
			Method myUserId = UserHandle.class.getMethod("myUserId");
			int id = (int) myUserId.invoke(UserHandle.class);
			userIcon = (Bitmap) getUserIcon.invoke(um, id);
			if (userIcon != null)
				userIcon = Utils.getCircularBitmap(userIcon);
		} catch (Exception e) {
		}
		return userIcon;
	}

	public static boolean hasStoragePermission(Context context) {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q || ContextCompat.checkSelfPermission(context,
				Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
	}

	public static boolean hasLocationPermission(Context context) {
		return ContextCompat.checkSelfPermission(context,
				Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
	}

	public static void playEventSound(Context context, String event) {
		String soundUri = PreferenceManager.getDefaultSharedPreferences(context).getString(event, "default");
		if (soundUri.equals("default")) {
		} else {
			try {
				Uri sound = Uri.parse(soundUri);
				if (sound != null) {
					final MediaPlayer mp = MediaPlayer.create(context, sound);
					mp.start();
					mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

						@Override
						public void onCompletion(MediaPlayer p1) {
							mp.release();
						}
					});
				}
			} catch (Exception e) {
			}
		}
	}

	public static Display getSecondaryDisplay(Context context) {
		DisplayManager dm = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
		Display[] displays = dm.getDisplays();
		return dm.getDisplays()[displays.length - 1];
	}

	public static DisplayMetrics getDisplayMetrics(Context context, boolean secondary) {
		DisplayManager dm = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
		Display display = secondary ? getSecondaryDisplay(context) : dm.getDisplay(Display.DEFAULT_DISPLAY);
		DisplayMetrics metrics = new DisplayMetrics();
		display.getMetrics(metrics);
		return metrics;
	}

	public static Context getDisplayContext(Context context, boolean secondary) {
		return secondary ? context.createDisplayContext(getSecondaryDisplay(context)) : context;
	}
}
