package x.mvmn.util.amqpgui.util;

import java.io.PrintWriter;
import java.io.StringWriter;

public class StackTraceUtil {
	public static String toString(Throwable t) {
		StringWriter strw = new StringWriter();
		t.printStackTrace(new PrintWriter(strw));
		return strw.toString();
	}
}
