LogcatLogger v0.01
============

A class for intercepting the Logcat output in your app.

Just create a new instance of LogcatLogger:
```java
LogcatLogger logger = new LogcatLogger();
```

Add the tags for which you want to be notified:
```java
logger.addTag("MY_TAG");
```

Set the listener on which you want to receive logs:
```java
LogListener myListener = new LogListener() {
	@Override
	public void onLog(String tag, LogType logType, long timestamp, String log) {
		// "tag" is the tag of the log
		// "logType" is the log level following the Log class name convention (V, D, I, W, E, WTF)
		// "timestamp" is the timestamp of the log
		// "log" is the log's text
	}
};
logger.setLogListener(myListener);
```

Start logging:
```java
logger.startLogging();
```

When you want to stop receiving logs stop the logger!
```java
logger.stopLogging();
```
