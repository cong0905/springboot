package vn.techshop.cart;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

public final class Pricing {
    private Pricing() {}
    public record Line(Long productId,int quantity,BigDecimal unitPrice) {}
    public static BigDecimal fee(BigDecimal subtotal) { return subtotal.compareTo(new BigDecimal("1000000"))>=0?BigDecimal.ZERO:new BigDecimal("30000"); }
    public static String hash(List<Line> lines,BigDecimal fee) {
        StringBuilder json=new StringBuilder("{\"currency\":\"VND\",\"lines\":[");
        var sorted=lines.stream().sorted(Comparator.comparing(Line::productId)).toList();
        for(int i=0;i<sorted.size();i++) { var l=sorted.get(i);if(i>0)json.append(',');json.append("{\"productId\":").append(l.productId()).append(",\"quantity\":").append(l.quantity()).append(",\"unitPrice\":\"").append(l.unitPrice().setScale(0).toPlainString()).append("\"}"); }
        return sha256(json.append("],\"shippingFee\":\"").append(fee.setScale(0).toPlainString()).append("\"}").toString());
    }
    public static String sha256(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch(NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
}
