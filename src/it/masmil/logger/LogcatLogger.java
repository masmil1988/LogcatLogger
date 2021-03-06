package it.masmil.logger;

import it.masmil.logger.LogListener.LogType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogcatLogger {
	private LogListener mLogListener = null;
	private AtomicBoolean mStarted = new AtomicBoolean(false);

	private static final String COMMAND_CLEAR_LOGCAT = "logcat -c";
	private static final String COMMAND_LOG_LOGCAT = "logcat -v time -n 1";
	private static final String LOGCAT_TIME_FORMAT = "MM-dd HH:mm:ss.SSS";
	private static final String LOGCAT_TIME_FORMAT_WITH_YEAR = "yyyy-"+LOGCAT_TIME_FORMAT;
	private static final String LOGCAT_TIME_PATTERN = "([0-9]{2}\\-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}\\.[0-9]{3})";

	private DateFormat mDateFormatter= new SimpleDateFormat(LOGCAT_TIME_FORMAT_WITH_YEAR, Locale.US);
	private Set<String> mTags = Collections.synchronizedSet(new HashSet<String>());

	public void setLogListener(LogListener listener) {
		mLogListener = listener;
	}

	public void addTag(String tag) {
		mTags.add(tag);
	}

	public void removeTag(String tag) {
		mTags.remove(tag);
	}

	public void startLogging() {
		if (mStarted.getAndSet(true))
			return;

		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Runtime.getRuntime().exec(COMMAND_CLEAR_LOGCAT).waitFor();
				} catch (InterruptedException e) {
					return;
				} catch (IOException e) {
					return;
				}

				long lastTimestamp = System.currentTimeMillis();
				Process process;
				try {
					process = Runtime.getRuntime().exec(COMMAND_LOG_LOGCAT);
				} catch (IOException e) {
					return;
				}
				BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

				while (mStarted.get()) {
					String line;
					try {
						line = bufferedReader.readLine();
					} catch (IOException e) {
						continue;
					}

					if (line.length() > 0) {
						for (String tag: mTags) {
							String patternToMatch = LOGCAT_TIME_PATTERN+" (.)/"+tag+" *\\(.+\\): (.*)$";
							Pattern pattern = Pattern.compile(patternToMatch);
							Matcher matcher = pattern.matcher(line);
							if (matcher.find()) {
								String time = Calendar.getInstance().get(Calendar.YEAR)+"-"+matcher.group(1);
								String logType = matcher.group(2);
								String log = matcher.group(3);

								Date d;
								try {
									d = mDateFormatter.parse(time);
								} catch (ParseException e) {
									d = new Date();
								}
								long timestamp = d.getTime();
								String[] splittedParts = line.split(".*"+tag+" *\\(.+\\): ");
								if (splittedParts.length > 0)
									line = splittedParts[1];
								else
									line = "";

								if (mLogListener != null) {
									try {
										mLogListener.onLog(tag, LogType.valueOf(logType), timestamp, log);
									} catch (IllegalArgumentException e) {
										// thrown if logType is not expected in the enum LogType
									}
								}
							}
						}
					}
				}

				process.destroy();
			}
		});
		thread.start();
	}

	public void stopLogging() {
		mStarted.set(false);
	}
}