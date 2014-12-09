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

	private static final String COMMAND_LOG_LOGCAT = "logcat -v time";
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
				long lastTimestamp = 0;
				Process process;
				try {
					process = Runtime.getRuntime().exec(COMMAND_LOG_LOGCAT);
				} catch (IOException e) {
					return;
				}
				BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

				StringBuilder logBuilder=new StringBuilder();

				HashSet<String> logsForTimestamp = new HashSet<String>();
				while (mStarted.get()) {
					String lineRead;
					try {
						lineRead = bufferedReader.readLine();
					} catch (IOException e) {
						continue;
					}
					
					logBuilder.append("\n"+lineRead);
					String toPrint = logBuilder.toString().trim();

					if (toPrint.length() > 0) {
						String[] lines = toPrint.split("\\n");

						for (String line: lines) {
							for (String tag: mTags) {
								String patternToMatch = LOGCAT_TIME_PATTERN+" (.)/"+tag+" *\\(.+\\): (.*)$";
								Pattern pattern = Pattern.compile(patternToMatch);
								Matcher matcher = pattern.matcher(line);
								if (matcher.find()) {
									String time = Calendar.getInstance().get(Calendar.YEAR)+"-"+matcher.group(1);
									String logType = matcher.group(2);
									String log = matcher.group(3);
									
									if (log.equals("Manager initialized")) {
										"a".charAt(0);
									}

									Date d;
									try {
										d = mDateFormatter.parse(time);
									} catch (ParseException e) {
										d = new Date();
									}
									long timestamp = d.getTime();
									line = line.split(".*"+tag+" *\\(.+\\): ")[1];
									boolean takeLog = false;

									if (lastTimestamp < timestamp) {
										lastTimestamp = timestamp;
										logsForTimestamp.clear();
										logsForTimestamp.add(log);
										takeLog = true;
									} else if (lastTimestamp == timestamp && !logsForTimestamp.contains(log)) {
										logsForTimestamp.add(log);
										takeLog = true;
									}

									if (takeLog && mLogListener != null) {
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
