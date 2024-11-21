package com.mdtlabs.migration.util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mdtlabs.migration.model.Constants;


/**
 * <p>
 * String utils for string validation etc.
 * </p>
 *
 * @author Karthick Murugesan created on Jan 11, 2024
 */
public class StringUtil {

    private static final Logger logger = LoggerFactory.getLogger(StringUtil.class);

	/**
	 * Converts a Date object into a string representation.
	 * The format used for the conversion is specified in Constants.DATE_FORMAT_DD_MM_YYYY.
	 *
	 * @param date The Date object to be converted into a string.
	 * @return A string representation of the date. If the provided date is null, returns null.
	 */
	public static String convertToDateString(Date date) {
		String dateString = null;
		logger.debug("Before Conversion of date {}", date);
		if (date != null) {
			SimpleDateFormat format = new SimpleDateFormat(Constants.DATE_FORMAT_DD_MM_YYYY);
			dateString = format.format(date);
			logger.debug("After Conversion of date {}", dateString);
		}

		return dateString;
	}


	/**
	 * Constructs a string by appending the provided strings with a space in between each string.
	 * The method uses the StringJoiner class with a space as the delimiter.
	 *
	 * @param args The strings to be appended.
	 * @return A string representing the appended strings with a space in between each string.
	 */
	public static String constructString(String... args) {
		StringJoiner buildString = new StringJoiner(Constants.SPACE);
		for (String arg : args) {
			buildString.add(arg);
		}
		return buildString.toString();
	}

	/**
	 * Constructs a string by appending the provided strings without any delimiter.
	 * The method uses the StringJoiner class with an empty string as the delimiter.
	 *
	 * @param args The strings to be appended.
	 * @return A string representing the appended strings without any delimiter.
	 */
	public static String concatString(String... args) {
		StringJoiner buildString = new StringJoiner(Constants.EMPTY);
		for (String arg : args) {
			buildString.add(arg);
		}
		return buildString.toString();
	}


	/**
	 * Constructs a list from a group of strings.
	 * The method uses the Collections.addAll method to add each string to the list.
	 *
	 * @param args The strings to be added to the list.
	 * @return A list containing the provided strings.
	 */
	public static List<String> constructListFromGroupOfString(String... args) {
		List<String> buildList = new ArrayList<>();
		for (String arg : args) {
			Collections.addAll(buildList, arg);
		}
		return buildList;
	}

	/**
	 * Converts a time from double to string.
	 * The method replaces ".5" with ":30" and ".0" with ":00" in the string representation of the double.
	 * If the hour part of the time string is a single digit, it prepends a zero to it.
	 *
	 * @param time The time to be converted into a string.
	 * @return A string representing the time. If the provided time is null, returns null.
	 */
	public static String convertTimeDoubleToString(Double time) {
		String timeString = time.toString().replace(".5", ":30").replace(".0", ":00");
		if (timeString.split(":")[Constants.ZERO].length() == Constants.ONE)
			timeString = Constants.STRING_ZERO + timeString;
		return timeString;
	}

	/**
	 * Parses an email template by replacing placeholders with corresponding values from a map.
	 * The placeholders in the template are in the format "{{key}}", where "key" is a key in the provided map.
	 * The method replaces each placeholder with the value associated with the key in the map.
	 * If the provided map is null or empty, the method returns the original template without any modifications.
	 *
	 * @param htmlTemplate The email template containing placeholders to be replaced.
	 * @param data A map containing the keys and values to be used for replacing the placeholders in the template.
	 * @return A string representing the parsed email template with placeholders replaced by the corresponding values from the map.
	 *         If the provided map is null or empty, returns the original template.
	 */
	public static String parseEmailTemplate(String htmlTemplate, Map<String, String> data) {
		if (data != null && !data.isEmpty()) {
			String[] result = new String[Constants.ONE];
			result[Constants.ZERO] = htmlTemplate;
			data.forEach(
					(key, value) -> result[Constants.ZERO] = result[Constants.ZERO].replace("{{" + key + "}}", value));
			return result[Constants.ZERO];
		}
		return htmlTemplate;
	}

}
