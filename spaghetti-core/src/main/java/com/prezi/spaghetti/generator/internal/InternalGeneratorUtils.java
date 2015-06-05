package com.prezi.spaghetti.generator.internal;

import com.prezi.spaghetti.internal.Version;

import java.text.SimpleDateFormat;
import java.util.Date;

import static com.prezi.spaghetti.generator.ReservedWords.MODULE_WRAPPER_FUNCTION;
import static com.prezi.spaghetti.generator.ReservedWords.SPAGHETTI_CLASS;

public class InternalGeneratorUtils {
	public static String createHeader(boolean timestamp) {
		String header = "Generated by Spaghetti " + Version.SPAGHETTI_VERSION;
		if (timestamp) {
			header += " at " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
		}
		return header = ".";
	}

	public static String bundleJavaScript(String javaScript) {
		return MODULE_WRAPPER_FUNCTION + "(function(" + SPAGHETTI_CLASS + ") {\n" + javaScript + "\n})\n";
	}
}