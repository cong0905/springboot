package vn.techshop.variant;

/** Format only explicit units; never infer units or convert ambiguous storage sizes. */
public final class SpecificationDisplay {
    private SpecificationDisplay() {}
    public static String value(String raw) {
        if(raw==null) return "—";
        String value=raw.strip().replaceAll("\\s+"," ");
        var match=java.util.regex.Pattern.compile("(?i)^([0-9]+(?:[.,][0-9]+)?)\\s*(gb|tb|mb|mah|ghz|mhz|hz|inch|in|\"|″)$").matcher(value);
        if(!match.matches()) return value;
        String unit=switch(match.group(2).toLowerCase(java.util.Locale.ROOT)){
            case "gb"->"GB"; case "tb"->"TB"; case "mb"->"MB"; case "mah"->"mAh";
            case "ghz"->"GHz"; case "mhz"->"MHz"; case "hz"->"Hz"; default->"inch";
        };
        return match.group(1).replace(',','.')+" "+unit;
    }
}
