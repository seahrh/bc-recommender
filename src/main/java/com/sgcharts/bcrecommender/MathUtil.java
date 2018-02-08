package com.sgcharts.bcrecommender;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MathUtil {
	private static final Logger log = LoggerFactory.getLogger(MathUtil.class);

	private MathUtil() {
		// not meant to be instantiated
	}
	
	public static double meanAbsoluteError(double[] expected, double[] actual) {
		if (expected == null || expected.length == 0) {
			log.error("Expected-values array must not be null or empty");
			throw new IllegalArgumentException();
		}
		if (actual == null || actual.length == 0) {
			log.error("Actual-values array must not be null or empty");
			throw new IllegalArgumentException();
		}
		int elen = expected.length;
		int alen = actual.length;
		if (elen != alen) {
			log.error("Both arrays must be of equal length. elen={} alen={}", elen, alen);
			throw new IllegalArgumentException();
		}
		double sum = 0;
		for (int i = 0; i < elen; i++) {
			sum += Math.abs(expected[i] - actual[i]);
		}
		return sum / elen;
	}
	
	public static double rootMeanSquaredError(double[] expected, double[] actual) {
		if (expected == null || expected.length == 0) {
			log.error("Expected-values array must not be null or empty");
			throw new IllegalArgumentException();
		}
		if (actual == null || actual.length == 0) {
			log.error("Actual-values array must not be null or empty");
			throw new IllegalArgumentException();
		}
		int elen = expected.length;
		int alen = actual.length;
		if (elen != alen) {
			log.error("Both arrays must be of equal length. elen={} alen={}", elen, alen);
			throw new IllegalArgumentException();
		}
		double sum = 0;
		for (int i = 0; i < elen; i++) {
			sum += Math.pow(expected[i] - actual[i], 2);
		}
		return Math.sqrt(sum / elen);
	}
	
	public static double cosineSimilarity(double[] first, double[] second) {
		double dotProduct = dotProduct(first, second);
		double firstMagnitude = magnitude(first);
		double secondMagnitude = magnitude(second);
		return dotProduct / (firstMagnitude * secondMagnitude);
	}
	
	public static double dotProduct(double[] first, double[] second) {
		if (first == null || first.length == 0) {
			log.error("First array must not be null or empty");
			throw new IllegalArgumentException();
		}
		if (second == null || second.length == 0) {
			log.error("Second array must not be null or empty");
			throw new IllegalArgumentException();
		}
		int flen = first.length;
		int slen = second.length;
		if (flen != slen) {
			log.error("Both arrays must be of equal length. flen={} slen={}", flen, slen);
			throw new IllegalArgumentException();
		}
		double ret = 0;
		for (int i = 0; i < flen; i++) {
			ret += first[i] * second[i];
		}
		return ret;
	}
	
	public static double magnitude(double[] arr) {
		if (arr == null || arr.length == 0) {
			log.error("array must not be null or empty");
			throw new IllegalArgumentException();
		}
		int sum = 0;
		for (int i = 0; i < arr.length; i++) {
			sum += arr[i] * arr[i];
		}
		return Math.sqrt(sum);
	}

}
