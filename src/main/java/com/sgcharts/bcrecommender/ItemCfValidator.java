package com.sgcharts.bcrecommender;

import static com.sgcharts.bcrecommender.ItemCf.pairKey;
import static com.sgcharts.bcrecommender.ItemCf.predict;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sgcharts.bcrecommender.FileUtil;
import com.sgcharts.bcrecommender.MathUtil;

import com.google.common.base.CharMatcher;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Sets;
import com.google.common.primitives.Doubles;

/**
 * k-fold validation for item-based collaborative filtering on the book crossing
 * dataset. Only explicit ratings are considered (1 - 10 on the rating scale,
 * higher values denoting higher appreciation). Implicit ratings are removed.
 * 
 */
public final class ItemCfValidator {
	private static final Logger log = LoggerFactory.getLogger(ItemCfValidator.class);
	/**
	 * Ratings input file path
	 */
	private static final String RATINGS_INPUT_FILE_PATH = System.getProperty("toy.ratings");
	/**
	 * Number of folds
	 */
	private static final int K_FOLDS = Integer.parseInt(System.getProperty("toy.folds"));
	/**
	 * Minimum number of ratings that user must make in order to make a
	 * prediction. The higher this threshold, the fewer predictions made.
	 */
	private static final int MIN_RATINGS_COUNT = Integer.parseInt(System.getProperty("toy.min-ratings"));
	private static List<List<List<String>>> folds = new ArrayList<>(K_FOLDS);
	/**
	 * Remaining number of ratings after preprocessing. These ratings will be
	 * divided into k folds.
	 */
	private static int ratingCount = 0;

	private ItemCfValidator() {
		// Not meant to be instantiated
	}

	public static void main(String[] args) throws IOException {
		long startTime = System.currentTimeMillis();
		log.info("Main: started...");
		extract();
		validate();
		long elapsedTime = System.currentTimeMillis() - startTime;
		log.info("Main: completed ({}s)", elapsedTime / 1000);
	}

	/**
	 * Put ratings in a table so that it can be looked up by either books (rows)
	 * or users (columns) in O(1) time.
	 * 
	 * @param data
	 *            list of lines representing the training set
	 * @return table of ratings where rows are books and columns are users
	 */
	private static ImmutableTable<String, String, Integer> ratingTable(
			List<List<String>> data) {
		long startTime = System.currentTimeMillis();
		log.info("ratingTable: started...");
		ImmutableTable.Builder<String, String, Integer> ratingTableBuilder = ImmutableTable.builder();
		String isbn;
		String uid;
		Integer rating;
		for (List<String> tokens : data) {
			uid = tokens.get(0)
				.toLowerCase();
			isbn = tokens.get(1)
				.toLowerCase();
			rating = Integer.parseInt(tokens.get(2));
			ratingTableBuilder.put(isbn, uid, rating);
		}
		long elapsedTime = System.currentTimeMillis() - startTime;
		log.info("ratingTable: completed ({}s)", elapsedTime / 1000);
		return ratingTableBuilder.build();
	}

	/**
	 * Run k-fold validation, testing each fold and report the results.
	 */
	private static void validate() {
		List<List<String>> trainSet;
		List<List<String>> testSet;
		Result r;
		double sumMae = 0;
		double sumRmse = 0;
		int predictionCount = 0;
		int skippedCount = 0;
		for (int k = 0; k < K_FOLDS; k++) {
			trainSet = new ArrayList<>(ratingCount);
			testSet = folds.get(k);
			for (int i = 0; i < K_FOLDS; i++) {
				if (i == k) {
					continue;
				}
				trainSet.addAll(folds.get(i));
			}
			r = validate(trainSet, testSet);
			sumMae += r.meanAbsoluteError;
			sumRmse += r.rootMeanSquaredError;
			predictionCount += r.predictionCount;
			skippedCount += r.skippedCount;
			log.info(
					"=====\nResults for k={}:\nmeanAbsoluteError={}\nrootMeanSquaredError={}\n#predictions={}\n#skipped={}\n=====",
					k + 1, r.meanAbsoluteError, r.rootMeanSquaredError,
					r.predictionCount, r.skippedCount);
		}
		log.info(
				"=====\n{}-fold validation results:\naverage meanAbsoluteError={}\naverage rootMeanSquaredError={}\ntotal #predictions={}\ntotal #skipped={}\n=====",
				K_FOLDS, sumMae / K_FOLDS, sumRmse / K_FOLDS, predictionCount,
				skippedCount);
	}

	/**
	 * Given a training set, build a rating table and similarity matrix for
	 * predicting ratings in the testing set.
	 * 
	 * @param trainSet
	 *            list of lines representing the training set
	 * @param testSet
	 *            list of lines representing the testing set
	 * @return test results
	 */
	private static Result validate(List<List<String>> trainSet,
			List<List<String>> testSet) {
		long startTime = System.currentTimeMillis();
		log.info("validate: started...");
		ImmutableTable<String, String, Integer> ratingTable = ratingTable(trainSet);
		Map<String, Float> simMatrix = similarityMatrix(ratingTable);
		List<Double> predictions = new ArrayList<>(testSet.size());
		List<Double> actuals = new ArrayList<>(testSet.size());
		String isbn;
		String uid;
		Double a;
		Double p;
		Optional<Integer> op;
		int skipped = 0;
		for (List<String> tokens : testSet) {
			uid = tokens.get(0)
				.toLowerCase();
			isbn = tokens.get(1)
				.toLowerCase();
			a = Double.parseDouble(tokens.get(2));
			op = predict(uid, isbn, ratingTable, simMatrix, MIN_RATINGS_COUNT);
			if (!op.isPresent()) {
				skipped++;
				continue;
			}
			actuals.add(a);
			p = (double) op.get();
			predictions.add(p);
			log.debug("a={}, p={}", a, p);
		}
		double[] predictionsArr = Doubles.toArray(predictions);
		double[] actualsArr = Doubles.toArray(actuals);
		Result result = new Result();
		result.meanAbsoluteError = MathUtil.meanAbsoluteError(predictionsArr,
				actualsArr);
		result.rootMeanSquaredError = MathUtil.rootMeanSquaredError(
				predictionsArr, actualsArr);
		result.predictionCount = predictionsArr.length;
		result.skippedCount = skipped;
		long elapsedTime = System.currentTimeMillis() - startTime;
		log.info("validate: completed ({}s)", elapsedTime / 1000);
		return result;
	}

	/**
	 * Given a rating table, build an item-item similarity matrix.
	 * <p>
	 * Similarity scores are stored as Float type to save space.
	 * 
	 * @param ratingTable
	 * @return Map of item-pair key to its similarity score.
	 */
	private static Map<String, Float> similarityMatrix(
			ImmutableTable<String, String, Integer> ratingTable) {
		long startTime = System.currentTimeMillis();
		log.info("similarityMatrix: started...");
		final int simMatrixExpectedSize = 50_000_000;
		Map<String, Float> simMatrix = new HashMap<>(simMatrixExpectedSize,
				0.99F);
		ImmutableSet<String> itemsSet = ratingTable.rowKeySet();
		final String[] items = itemsSet.toArray(new String[itemsSet.size()]);
		ImmutableMap<String, Map<String, Integer>> itemMap = ratingTable.rowMap();
		Map<String, Integer> uidToRating;
		Map<String, Integer> otherUidToRating;
		Set<String> raters;
		Set<String> otherRaters;
		Set<String> commonRaters;
		Float sim;
		String isbn;
		String otherIsbn;
		String pair;
		int progress = 0;
		final int progressInterval = 1_000_000;
		// Loop only the upper triangle of the item-item matrix.
		// Skip the lower triangle as their similarity scores have already been
		// computed.
		// This still takes O(n^2) time but practically, this reduces run time
		// by two-thirds as compared to a full nested loop. Performance gains
		// from not having to check whether similarity score has already been
		// computed, despite the lookup taking O(1) time.
		// One possibe reason is that the similarity matrix hash table is huge,
		// thus increasing the chance of
		// key collision and the lookup time is greater than O(1) time in an
		// imperfect hash table.
		for (int i = 0; i < items.length; i++) {
			for (int j = i + 1; j < items.length; j++) {
				isbn = items[i];
				uidToRating = itemMap.get(isbn);
				// this item has at least 1 rating
				raters = uidToRating.keySet();
				otherIsbn = items[j];
				otherUidToRating = itemMap.get(otherIsbn);
				// other item has at least 1 rating
				otherRaters = otherUidToRating.keySet();
				// Intersect raters of this item vs. other item
				commonRaters = Sets.intersection(raters, otherRaters);
				if (commonRaters.isEmpty()) {
					// No raters in common; skip
					continue;
				}
				pair = pairKey(isbn, otherIsbn);
				sim = cosineSimilarity(commonRaters, uidToRating,
						otherUidToRating);
				log.debug("sim={} isbn={} otherIsbn={}", sim, isbn, otherIsbn);
				if (++progress % progressInterval == 0) {
					log.info("{}M sim computed", progress / progressInterval);
				}
				simMatrix.put(pair, sim);
			}
		}
		log.info("{} sim computed", progress);
		long elapsedTime = System.currentTimeMillis() - startTime;
		log.info("similarityMatrix: completed ({}s)", elapsedTime / 1000);
		return simMatrix;
	}

	/**
	 * Use cosine similarity to measure the similarity between two items. For
	 * speed and effectiveness, cosine similarity is recommended for item-based
	 * collaborative filtering in Ekstrand et al (2010).
	 * 
	 * @param raters
	 *            Set of raters that are common to both items.
	 * @param uidToRating
	 *            Map of user id to rating for the first item
	 * @param otherUidToRating
	 *            Map of user id to rating for the second item
	 * @return cosine similarity between two books
	 */
	private static float cosineSimilarity(Set<String> raters,
			Map<String, Integer> uidToRating,
			Map<String, Integer> otherUidToRating) {
		int size = raters.size();
		double[] ratings = new double[size];
		double[] otherRatings = new double[size];
		int i = 0;
		for (String rater : raters) {
			ratings[i] = (double) uidToRating.get(rater);
			otherRatings[i] = (double) otherUidToRating.get(rater);
			i++;
		}
		log.debug("ratings={}\notherRatings={}", ratings, otherRatings);
		return (float) MathUtil.cosineSimilarity(ratings, otherRatings);
	}

	/**
	 * Import the ratings file and divide the data into k folds. Preprocessing:
	 * remove implicit ratings.
	 * 
	 * @throws IOException
	 */
	private static void extract() throws IOException {
		long startTime = System.currentTimeMillis();
		log.info("Extract: started...");
		final int nHeaderRows = 1;
		final CharMatcher separator = CharMatcher.anyOf("\";\\");
		final boolean omitEmptyStrings = true;
		List<List<String>> in = FileUtil.read(RATINGS_INPUT_FILE_PATH,
				separator, nHeaderRows, omitEmptyStrings);
		log.info("ratings size={}, before removing implicit ratings", in.size());
		in = removeImplicitRatings(in);
		ratingCount = in.size();
		log.info("ratings size={}, after removing implicit ratings",
				ratingCount);
		log.debug("{}", in);
		// Randomly reshuffle the dataset before splitting into folds
		Collections.shuffle(in);
		int size = ratingCount / K_FOLDS;
		List<List<String>> fold;
		int i = 0;
		int j = size;
		for (int k = 0; k < K_FOLDS; k++) {
			if (k == K_FOLDS - 1) {
				j = ratingCount;
			} else {
				j = i + size;
			}
			fold = in.subList(i, j);
			folds.add(fold);
			i = j;
		}
		long elapsedTime = System.currentTimeMillis() - startTime;
		log.info("Extract: completed ({}s)", elapsedTime / 1000);
	}

	/**
	 * Discard implicit ratings that are expressed by 0 on the rating scale.
	 * 
	 * @param ratings
	 * @return
	 */
	private static List<List<String>> removeImplicitRatings(
			List<List<String>> ratings) {
		List<List<String>> ret = new ArrayList<>(ratings.size());
		int rating;
		for (List<String> tokens : ratings) {
			rating = Integer.parseInt(tokens.get(2));
			if (rating != 0) {
				ret.add(tokens);
			}
		}
		return ret;
	}

	/**
	 * Store the results of each test.
	 * 
	 */
	private static class Result {
		private double meanAbsoluteError = 0;
		private double rootMeanSquaredError = 0;
		/**
		 * Number of predictions made in this test
		 */
		private int predictionCount = 0;
		/**
		 * Number of test items skipped because prediction could not be made
		 */
		private int skippedCount = 0;
	}

}
