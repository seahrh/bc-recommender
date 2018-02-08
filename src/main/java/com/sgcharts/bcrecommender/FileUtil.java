package com.sgcharts.bcrecommender;



import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;

import static com.sgcharts.bcrecommender.StringUtil.split;
import static com.sgcharts.bcrecommender.StringUtil.trim;

public final class FileUtil {
	private static final Logger log = LoggerFactory.getLogger(FileUtil.class);
	private static final char END_OF_LINE = '\n';

	private FileUtil() {
		// Private constructor, not meant to be instantiated
	}

	public static String path(String parentDirectory, String filename) {
		if (Strings.isNullOrEmpty(parentDirectory)) {
			log.error("Parent directory must not be null or empty string");
			throw new IllegalArgumentException();
		}
		if (Strings.isNullOrEmpty(filename)) {
			log.error("filename must not be null or empty string");
			throw new IllegalArgumentException();
		}
		StringBuilder sb = new StringBuilder(parentDirectory);
		if (!parentDirectory.endsWith(File.separator)) {
			sb.append(File.separator);
		}
		sb.append(filename);
		return sb.toString();
	}

	public static List<List<String>> read(String path, CharMatcher separator)
			throws IOException {
		int nHeaderRows = 0;
		// Preserve empty columns, by default
		boolean omitEmptyStrings = false;
		return read(path, separator, nHeaderRows, omitEmptyStrings);
	}

	public static List<List<String>> read(String path, CharMatcher separator,
			int nHeaderRows) throws IOException {
		boolean omitEmptyStrings = false;
		return read(path, separator, nHeaderRows, omitEmptyStrings);
	}
	
	public static List<List<String>> read(String path, CharMatcher separator,
			int nHeaderRows, boolean omitEmptyStrings) throws IOException {
		path = trim(path);
		if (path.isEmpty()) {
			log.error("path must not be null or empty string");
			throw new IllegalArgumentException();
		}
		File file = new File(path);
		return read(file, separator, nHeaderRows, omitEmptyStrings);
	}

	public static List<List<String>> read(File file, CharMatcher separator)
			throws IOException {
		int nHeaderRows = 0;
		boolean omitEmptyStrings = false;
		return read(file, separator, nHeaderRows, omitEmptyStrings);
	}
	
	public static List<List<String>> read(File file, CharMatcher separator,
			int nHeaderRows) throws IOException {
		boolean omitEmptyStrings = false;
		return read(file, separator, nHeaderRows, omitEmptyStrings);
	}

	public static List<List<String>> read(File file, CharMatcher separator,
			int nHeaderRows, boolean omitEmptyStrings) throws IOException {
		BufferedReader br = null;
		if (file == null) {
			log.error("file must not be null");
			throw new IllegalArgumentException();
		}
		if (separator == null) {
			separator = CharMatcher.whitespace();
		}
		String line;
		List<String> row;
		List<List<String>> ret = new ArrayList<>();
		try {
			br = new BufferedReader(new FileReader(file));
			// Skip header rows
			for (int i = 0; i < nHeaderRows; i++) {
				br.readLine();
			}
			// Read main content
			while ((line = br.readLine()) != null) {
				row = split(line, separator, omitEmptyStrings);
				ret.add(row);
			}
		} finally {
			if (br != null) {
				br.close();
			}
		}
		return ret;
	}
	
	public static String readFirstLine(File file) throws IOException {
		BufferedReader br = null;
		String ret = null;
		if (file == null) {
			log.error("file must not be null");
			throw new IllegalArgumentException();
		}
		try {
			br = new BufferedReader(new FileReader(file));
			ret = br.readLine();
		} finally {
			if (br != null) {
				br.close();
			}
		}
		return ret;
	}
	
	public static String readFirstLine(String path) throws IOException {
		path = trim(path);
		if (path.isEmpty()) {
			log.error("path must not be null or empty string");
			throw new IllegalArgumentException();
		}
		File file = new File(path);
		return readFirstLine(file);
	}

	public static File write(List<List<String>> data, String path)
			throws IOException {
		String separator = " ";
		return write(data, path, separator);
	}

	public static File write(List<List<String>> data, String path,
			String separator) throws IOException {
		if (data == null || data.isEmpty()) {
			log.error("data must not be null or empty collection");
			throw new IllegalArgumentException();
		}
		String val;
		int size;
		StringBuilder sb = new StringBuilder();
		for (List<String> row : data) {
			size = row.size();
			for (int i = 0; i < size; i++) {
				val = row.get(i);
				sb.append(val);
				if (i == size - 1) {
					sb.append(END_OF_LINE);
				} else {
					sb.append(separator);
				}
			}
		}
		return write(sb.toString(), path);
	}

	public static File write(String content, String path) throws IOException {
		path = trim(path);
		if (path.isEmpty()) {
			log.error("path must not be null or empty string");
			throw new IllegalArgumentException();
		}
		File file = new File(path);
		return write(content, file);
	}

	public static File write(String content, File file) throws IOException {
		if (content == null) {
			log.error("content must not be null");
			throw new IllegalArgumentException();
		}
		if (file == null) {
			log.error("file must not be null");
			throw new IllegalArgumentException();
		}
		BufferedWriter bw = null;
		try {
			bw = new BufferedWriter(new FileWriter(file));
			bw.write(content);
		} finally {
			if (bw != null) {
				bw.close();
			}
		}
		return file;
	}

	public static void write(byte[] bytes, String path) throws IOException {
		OutputStream out = null;
		try {
			out = new BufferedOutputStream(new FileOutputStream(path));
			out.write(bytes);
		} finally {
			if (out != null) {
				out.close();
			}
		}
	}

	public static int[] widthAndHeight(File image) throws IOException {
		int w = 0;
		int h = 0;
		ImageInputStream in = null;
		ImageReader reader = null;
		try {
			in = ImageIO.createImageInputStream(image);
			Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
			// Get the 1st decoder
			if (readers.hasNext()) {
				reader = readers.next();
				reader.setInput(in);
				w = reader.getWidth(0);
				h = reader.getHeight(0);
			}
		} finally {
			if (in != null) {
				in.close();
			}
			if (reader != null) {
				reader.dispose();
			}
		}
		if (w < 1) {
			log.error("width must be greater than zero");
			throw new IllegalArgumentException();
		}
		if (h < 1) {
			log.error("height must be greater than zero");
			throw new IllegalArgumentException();
		}
		return new int[] { w, h };
	}
}
