package it.gov.pagopa.afm.calculator.util;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

@UtilityClass
public class CalculatorUtil {

    public static boolean getAllCCP(String allCcp) {
        return StringUtils.isBlank(allCcp) || Boolean.parseBoolean(allCcp);
    }
    public static boolean getOnUsFirst(String onUsFirst) {
        return StringUtils.isBlank(onUsFirst) || Boolean.parseBoolean(onUsFirst);
    }
    public static String getOrderBy(String orderBy) {
        return StringUtils.isBlank(orderBy) ? "random" : orderBy;
    }
}
