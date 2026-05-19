package edu.umich.lib.normalize.callnumber;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

public class AnyCallNumberSimple extends AbstractCallNumber {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public String eitherValidKey;
  public LCCallNumberSimple lc;
  public DeweySimple dewey;


  public AnyCallNumberSimple(String str) {
    trimmedOriginal = str;
    lc               = new LCCallNumberSimple(str);
    dewey            = new DeweySimple(str);
    eitherValidKey = chooseLCOrDeweyValidKey(trimmedOriginal);
    isValid          = !(eitherValidKey == null);
  }


  @Override
  public Boolean hasValidKey() {
    return isValid;
  }

  @Override
  public String validKey() {
    return eitherValidKey;
  }


  @Override
  public Boolean hasAcceptableTruncatedKey() {
    return (lc.hasAcceptableTruncatedKey() || dewey.hasAcceptableTruncatedKey());
  }

  @Override
  public String acceptableTruncatedKey() {
    if (lc.hasAcceptableTruncatedKey()) return lc.acceptableTruncatedKey();
    if (dewey.hasAcceptableTruncatedKey()) return dewey.acceptableTruncatedKey();
    return null;
  }


  @Override
  public String invalidKey() {
    return lc.invalidKey();
  }


  private String chooseLCOrDeweyValidKey(String str) {
    if (lc.isValid) return lc.collationKey();
    if (dewey.isValid) return dewey.collationKey();
    return null;
  }


}
