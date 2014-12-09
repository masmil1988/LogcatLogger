package it.masmil.logger;

public interface LogListener {
	public enum LogType {
		V,
		D,
		I,
		W,
		E,
		WTF
	}
	
	public void onLog(String tag, LogType logType, long timestamp, String log);
}
