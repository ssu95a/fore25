package ru.inversion.utils.scheck;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * @author antonovdi
 */
public class JInvStringWorker {

    private static JInvStringWorker g_instance = new JInvStringWorker();
    private Map<String, IJInvSWItem> itemMap = new HashMap(); // = new HashMap<>();

    private JInvStringWorker() {
        initMap();
        int a = 0;
    }

    public void registerSWItem(IJInvSWItem... items) {
        for (IJInvSWItem item : items) {
            if (item != null) {
                itemMap.put(item.getId(), item);
            }
        }
    }

    public void registerSWItemList(List<IJInvSWItem> list) {
        for (IJInvSWItem item : list) {
            if (item != null) {
                itemMap.put(item.getId(), item);
            }
        }
    }

    /**
     *
     */
    public int check(String value, String... ids) throws JInvStringWorkerException {

        int index = 0;

        for (String s : ids) {
            try {
                if (check(s, value, false) == null) {
                    return index;
                }
            } catch (JInvStringWorkerValidationException ex) {

            }
            index++;
        }
        return -1;
    }

    public JInvStringWorkerValidationException check(String id, String source, boolean isThrowException) throws JInvStringWorkerException, JInvStringWorkerValidationException {

        IJInvSWItem item = itemMap.get(id);

        if (item == null) {
            throw new JInvStringWorkerException(ResourceBundle.getBundle("schecker").getString("checkerNotFound") + id);
        }

        Pattern pattern = itemMap.get(id).getRegExp();
        Matcher matcher = pattern.matcher(source);

        if (matcher.find() && matcher.group(item.getGroupRegExp()) != null) {
            return null;
        }

        if (isThrowException) {
            throw new JInvStringWorkerValidationException((item.getErrorDescription()));
        }
        return new JInvStringWorkerValidationException((item.getErrorDescription()));
    }

    /**
     *
     * @param regExp
     * @param groupRegExp
     * @param source
     * @return
     */
    public boolean checkRegExp(String regExp, int groupRegExp, String source) {
        JInvStringWorkerException i = null;
        Pattern pattern = null;
        pattern = Pattern.compile(regExp);
        Matcher matcher = pattern.matcher(source);

        if (matcher.find() && matcher.group(groupRegExp) != null) {
            return true;
        }
        return false;

    }

//    public boolean check(String id, String source) {
//
//        IJInvSWItem item = itemMap.get(id);
//        Pattern pattern = itemMap.get(id).getRegExp();
//        Matcher matcher = pattern.matcher(source);
//
//        if (matcher.find() && matcher.group(item.getGroupRegExp()) != null) {
//            return true;
//        }
//        return  false;
//    }

//    public boolean check(String id, String source) {
//
//        IJInvSWItem item = itemMap.get(id);
//        Pattern pattern = itemMap.get(id).getRegExp();
//        Matcher matcher = pattern.matcher(source);
//
//        if (matcher.find() && matcher.group(item.getGroupRegExp()) != null) {
//            return true;
//        }
//        return  false;
//    }

    public String replace(String id, String source, String patternReplace, boolean replaceAll) throws JInvStringWorkerException {
        String result = source;

        IJInvSWItem item = itemMap.get(id);

        if (item == null) {
            throw new JInvStringWorkerException(ResourceBundle.getBundle("schecker").getString("checkerNotFound") + id);
        }

        Pattern pattern = itemMap.get(id).getRegExp();
        Matcher matcher = pattern.matcher(source);

        if (matcher.find() && matcher.group(item.getGroupRegExp()) != null) {
            if (replaceAll) {
                result = result.replaceAll(item.getRegExp().pattern(), patternReplace);
            } else {
                result = result.replaceFirst(matcher.group(item.getGroupRegExp()), patternReplace);
            }
        } else {
            throw new JInvStringWorkerException(item.getErrorDescription());
        }

        return result;
    }

    public int countItems(String id, String source) throws JInvStringWorkerException {

        int result = 0;
        IJInvSWItem item = itemMap.get(id);

        if (item == null) {
            throw new JInvStringWorkerException(ResourceBundle.getBundle("schecker").getString("checkerNotFound") + id);
        }
        Pattern pattern = itemMap.get(id).getRegExp();
        Matcher matcher = pattern.matcher(source);

        while (matcher.find() && matcher.group(item.getGroupRegExp()) != null) {
            result++;
        }

        if (result == 0) {
            throw new JInvStringWorkerException(item.getErrorDescription());
        }
        return result;
    }

    public List<String> matchCases(String id, String source) throws JInvStringWorkerException {

        List<String> result = new ArrayList();
        IJInvSWItem item = itemMap.get(id);

        if (item == null) {
            throw new JInvStringWorkerException(ResourceBundle.getBundle("schecker").getString("checkerNotFound") + id);
        }

        Pattern pattern = itemMap.get(id).getRegExp();
        Matcher matcher = pattern.matcher(source);

        while (matcher.find() && matcher.group(item.getGroupRegExp()) != null) {
            result.add(matcher.group(item.getGroupRegExp()));
        }

        if (result.isEmpty()) {
            throw new JInvStringWorkerException(item.getErrorDescription());
        }
        return result;
    }

    public String matchCase(String id, String source, int caseNumber) throws JInvStringWorkerException {

        String result = "";
        IJInvSWItem item = itemMap.get(id);

        if (item == null) {
            throw new JInvStringWorkerException(ResourceBundle.getBundle("schecker").getString("checkerNotFound") + id);
        }

        Pattern pattern = itemMap.get(id).getRegExp();
        Matcher matcher = pattern.matcher(source);
        int countFounded = 0;

        while (matcher.find() && matcher.group(item.getGroupRegExp()) != null) {
            countFounded++;
            if (countFounded == caseNumber) {
                result = matcher.group(item.getGroupRegExp());
                break;
            }
        }

        if (result.isEmpty()) {
            throw new JInvStringWorkerException(item.getErrorDescription());
        }

        return result;
    }

    public String matchCase(String id, String source) throws JInvStringWorkerException {

        String result = "";
        IJInvSWItem item = itemMap.get(id);
        Pattern pattern = itemMap.get(id).getRegExp();

        if (item == null) {
            throw new JInvStringWorkerException(ResourceBundle.getBundle("schecker").getString("checkerNotFound") + id);
        }

        Matcher matcher = pattern.matcher(source);
        if (matcher.find() && matcher.group(item.getGroupRegExp()) != null) {
            result = matcher.group(item.getGroupRegExp());
        } else {
            throw new JInvStringWorkerException(item.getErrorDescription());
        }

        return result;
    }

    public JInvStringWorkerValidationException checkRegExp(String regExp, int groupRegExp, String source, boolean ThrowException) throws JInvStringWorkerException, JInvStringWorkerValidationException {

        JInvStringWorkerException i = null;
        Pattern pattern = null;

        try {
            pattern = Pattern.compile("^"+regExp+"$");

        } catch (PatternSyntaxException ex) {
            throw new JInvStringWorkerException(ResourceBundle.getBundle("schecker").getString("regExpError"), ex);
        }

        Matcher matcher = pattern.matcher(source);

        if (matcher.find() && matcher.group(groupRegExp) != null) {
            return null;
        }

        if (ThrowException) {
            throw new JInvStringWorkerValidationException(ResourceBundle.getBundle("schecker").getString("dataIsNotFound") + " Regex: " + regExp + " Row: " + source);
        }

        return new JInvStringWorkerValidationException((ResourceBundle.getBundle("schecker").getString("dataIsNotFound")) + " Regex: " + regExp + " Row: " + source);
    }

    public String replaceRegExp(String regExp, int groupRegExp, String source, String patternReplace, boolean replaceAll) throws JInvStringWorkerException {
        String result = source;
        Pattern pattern = null;

        try {
            pattern = Pattern.compile(regExp);
        } catch (PatternSyntaxException ex) {
            throw new JInvStringWorkerException(ResourceBundle.getBundle("schecker").getString("regExpError"));
        }

        Matcher matcher = pattern.matcher(source);

        if (matcher.find() && matcher.group(groupRegExp) != null) {
            if (replaceAll) {
                result = result.replaceAll(regExp, patternReplace);
            } else {

                String group = matcher.group(groupRegExp);
//                group = group.replaceAll("?", "\\?");

                result = result.replaceFirst(group, patternReplace);
            }
        } else {
            throw new JInvStringWorkerException(ResourceBundle.getBundle("schecker").getString("replaceError"));
        }

        return result;
    }

    public int countItemsRegExp(String regExp, int groupRegExp, String source) throws JInvStringWorkerException {

        int result = 0;
        Pattern pattern = null;

        try {
            pattern = Pattern.compile(regExp);
        } catch (PatternSyntaxException ex) {
            throw new JInvStringWorkerException(ResourceBundle.getBundle("schecker").getString("regExpError"));
        }

        Matcher matcher = pattern.matcher(source);

        while (matcher.find() && matcher.group(groupRegExp) != null) {
            result++;
        }

        if (result == 0) {
            throw new JInvStringWorkerException(ResourceBundle.getBundle("schecker").getString("countError"));
        }
        return result;
    }

    public List<String> matchCasesRegExp(String regExp, int groupRegExp, String source) throws JInvStringWorkerException {
        List<String> result = new ArrayList();
        Pattern pattern = null;

        try {
            pattern = Pattern.compile(regExp);
        } catch (PatternSyntaxException ex) {
            throw new JInvStringWorkerException(ResourceBundle.getBundle("schecker").getString("regExpError"));
        }

        Matcher matcher = pattern.matcher(source);

        while (matcher.find() && matcher.group(groupRegExp) != null) {
            result.add(matcher.group(groupRegExp));
        }

        if (result.isEmpty()) {
            throw new JInvStringWorkerException(ResourceBundle.getBundle("schecker").getString("dataIsNotFound"));
        }
        return result;
    }

    public String matchCaseRegExp(String regExp, int groupRegExp, String source, int caseNumber) throws JInvStringWorkerException {

        String result = "";
        Pattern pattern = null;

        try {
            pattern = Pattern.compile(regExp);
        } catch (PatternSyntaxException ex) {
            throw new JInvStringWorkerException(ResourceBundle.getBundle("schecker").getString("regExpError"));
        }

        Matcher matcher = pattern.matcher(source);
        int countFounded = 0;

        while (matcher.find() && matcher.group(groupRegExp) != null) {
            countFounded++;
            if (countFounded == caseNumber) {

                result = matcher.group(groupRegExp);
                break;
            }
        }

        if (result.isEmpty()) {
            throw new JInvStringWorkerException(ResourceBundle.getBundle("schecker").getString("dataIsNotFound"));
        }

        return result;
    }

    public String matchCaseRegExp(String regExp, int groupRegExp, String source) throws JInvStringWorkerException {
        String result = "";

        Pattern pattern = null;

        try {
            pattern = Pattern.compile(regExp);
        } catch (PatternSyntaxException ex) {
            throw new JInvStringWorkerException(ResourceBundle.getBundle("schecker").getString("regExpError"));
        }

        Matcher matcher = pattern.matcher(source);

        if (matcher.find() && matcher.group(groupRegExp) != null) {
            result = matcher.group(groupRegExp);
        } else {
            throw new JInvStringWorkerException(ResourceBundle.getBundle("schecker").getString("dataIsNotFound"));
        }
        return result;
    }

    public String matchCaseRegExpOrNull(String regExp, int groupRegExp, String source) throws JInvStringWorkerException {
        String result = "";

        Pattern pattern = null;

        try {
            pattern = Pattern.compile(regExp);
        } catch (PatternSyntaxException ex) {
            throw new JInvStringWorkerException(ResourceBundle.getBundle("schecker").getString("regExpError"));
        }

        Matcher matcher = pattern.matcher(source);

        if (matcher.find() && matcher.group(groupRegExp) != null) {
            result = matcher.group(groupRegExp);
        } else {
            return null;
        }
        return result;
    }

    private void createItem(String ID, String pattern, int groupNum, String name, String errorDescriptionKey) {

        String errDescription = null;
        try {
            errDescription = ResourceBundle.getBundle("schecker").getString(errorDescriptionKey == null ? ID : errorDescriptionKey);
        } catch (MissingResourceException mrex) {
            ;
        }

        itemMap.put(
                ID,
                new SWItem(
                        ID,
                        Pattern.compile(pattern),
                        groupNum,
                        name,
                        errDescription
                )
        );
    }

    /**
     *
     */
    private void initMap() {
        createItem("EMAIL", "([a-zA-Z.0-9]+@[a-zA-Z]+\\.[a-zA-Z]+)", 0, "email", "emailError");
        createItem("IP",
                "(%)|(25|2[0-4]|[01]?[0-9][0-9]?)(%|(\\.(%|((25|2[0-4]|[01]?[0-9]?[0-9]?))))(%|(\\.(%|((25|2[0-4]|[01]?[0-9][0-9]?))))(%|(\\.(%|((25%|2[0-4]%|[01]?[0-9][0-9%]?)))))))|(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)",
                0,
                "ip", "ipError");
        createItem("PHONE", "^\\+?\\d(\\s|-)?((\\d\\d\\d|\\d\\d)(\\s|-)?){3,}$", 0, "PHONE", "phoneError");
        createItem("PASSPORT", "^(\\d{2}\\s\\d{2}\\s\\d{6}|\\d{4}\\s\\d{6}|\\d{10})\\s\\d{3}-\\d{3}$", 0, "PASSPORT", "passportError");
        createItem("INDEX", "^\\d{6,}$", 0, "INDEX", "indexError");
        createItem("URL",
                "((https?|ftp|gopher|telnet|file):((//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)",
                0,
                "URL",
                "urlError");
        createItem("RUSSIAN", "^[А-Яа-яЁё\\s]+$", 0, "RUSSIAN", "russian");
        createItem("RUSSIAN_EXT", "^[-\\s,.:_А-Яа-яЁё]+$", 0, "RUSSIAN_EXT", "russian");
        createItem("DRIVER_CATEGORY", "^((A|B|C|D|E|BE|CE|DE)|(((A|B|C|D|E|BE|CE|DE)\\W+)+(A|B|C|D|E|BE|CE|DE)))$", 0, "category of driver license", "driver_category");
        createItem("INTEGER_WS", "^-?[0-9 ]*$", 0, "INTEGER_WS", null);
        createItem("INTEGER", "^-?[0-9]*$", 0, "INTEGER", null);
        createItem("DECIMAL_WS", "^-?((([0-9\\s])*([,.=])?([0-9\\s])*)|(([,.=])?([0-9\\s])*))$", 0, "DECIMAL_WS", null);
//        createItem("SENIORITY", "",0,"","");
        createItem("DATE_DD.MM.YY", "^(0\\d|1\\d|2\\d|3[01])\\.(0\\d|1[012])\\.(\\d\\d)$", 0, "DECIMAL_WS", null);
        createItem("DATE_DD.MM.YYYY", "^(0\\d|1\\d|2\\d|3[01])\\.(0\\d|1[012])\\.(19[1-9]\\d|20[0-4]\\d)$", 0, "DECIMAL_WS", null);
        createItem("TIME_HH:MM:SS", "^(0[0-9]|1[0-9]|2[0-3])(:[0-5][0-9]){0,2}$", 0, "TIME_HH:MM:SS", null);

        createItem("MONEY_MASK", "^(-?((\\d([MmTtМмТт])?)+)?([,.]\\d*)?)$", 0, "MONEY_MASK", null);
        createItem("MONEY_MILLION", "(-?([MmМмTtТт])?((\\d)+)[MmМм])", 3, "MONEY_MILLION", null);
        createItem("MONEY_THOUSAND", "(-?([MmМмTtТт])?((\\d)+)[TtТт])", 3, "MONEY_THOUSAND", null);
        createItem("MONEY_NUMBERS_5_GROUP", "^(-?((.*[MmМмTtТт](\\d+))|(\\d+))([.,]\\d*)?)$", 5, "MONEY_NUMBERS_5_GROUP", null);
        createItem("MONEY_NUMBERS_4_GROUP", "^(-?((.*[MmМмTtТт](\\d+))|(\\d+))([.,]\\d*)?)$", 4, "MONEY_NUMBERS_4_GROUP", null);
        createItem("MONEY_FRACTION", "[.,](\\d+)", 1, "MONEY_FRACTION", null);
        createItem("GUID_8_4_4_4_12", "^[0-9a-f]{8}-([0-9a-f]{4}\\-){3}[0-9a-f]{12}$", 0, "Guid 8-4-4-4-12", "guid_8_4_4_4_12");
    }

    /**
     *
     */
    public static JInvStringWorker INSTANCE() {
        return g_instance;
    }
}
