package edu.umich.lib.normalize.callnumber;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * For purposes of this simple normalizer, define a valid dewey number as follows:
 *   * Starts with three digits
 *   * Optional decimal places which can include IGNORED slashes or apostrophes
 *   * Whatever's left:
 *    * Lowercase
 *    * remove dots after a letter
 *    * Trim spaces
 *    * remove leading/trailing punctuation (which will take care of a dot before a cutter)
 *    * trim spaces again
 *    * compact spaces
 *    * add back in with a preceding space
 *
 */
public class DeweySimple extends AbstractCallNumber {

  protected static Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  public static Logger logger() {
    return LOGGER;
  }

  public String digits = "";
  public String decimals = "";
  public String rest = "";

  public static Pattern deweyPattern = Pattern.compile(
      "^\\s*(?<digits>\\d{3})" + "\\s*" +
      "(?:\\.(?<decimals>[\\d/']+))?"  +
      "(?:\\s+(?<rest>.*))?$");

  public static Pattern acceptableThreeDigitsPattern = Pattern.compile("^\\s*\\d{3}\\s*$");


  public DeweySimple(String str) {
    trimmedOriginal = trimPunctuation(str.trim().toLowerCase());
    Matcher m = deweyPattern.matcher(trimmedOriginal);
    if (m.matches()) {
      isValid  = true;
      digits   = m.group("digits");
      decimals = fixedDecimals(m.group("decimals"));
      rest     = cleanupFreetext(m.group("rest"));
    } else {
      logger().debug(trimmedOriginal + " is invalid");
      isValid = false;
    }

  }

  @Override
  public Boolean hasValidKey() {
    return isValid;
  }
  @Override
  public String validKey() {
    return collationKey();
  }

  public String collationKey() {
    if (isValid) {
      return trimPunctuation(digits + decimals + rest);
    } else {
      return null;
    }
  }

  @Override
  public Boolean hasAcceptableTruncatedKey() {
    return isValidTruncatedQuery(trimmedOriginal);
  }

  @Override
  public String acceptableTruncatedKey() {
    if (isValidTruncatedQuery(trimmedOriginal)) {
      return trimmedOriginal;
    } else {
      return null;
    }
  }

  @Override
  public String invalidKey() {
    return cleanupFreetext(trimmedOriginal);
  }


  private String fixedDecimals(String str) {
    if (str == null) return "";
    if (str.trim().equals("")) return "";
    return "." + str.trim().replaceAll("[/']+", "");
  }

  private String cleanupFreetext(String str) {
    if (str == null)  return "";
    String s = str.trim();
    if (s.equals("")) {
      return s;
    }
    s = removeDotsBetweenLetters(s);
    s = ditchDotsAfterLetters(s);
    s = trimPunctuation(s);
    s = collapseSpaces(s);
    s = " " + s;
    return s;
  }

  private String removeDotsBetweenLetters(String str) {
    return str.replaceAll("(\\p{L})\\.(\\p{L})", "$1$2");
  }

  public String ditchDotsAfterLetters(String str) {
    return str.replaceAll("(\\p{L})\\.", "$1 ");
  }

  private Boolean isValidTruncatedQuery(String str) {
    return acceptableThreeDigitsPattern.matcher(str).matches();
  }

}
