package vn.techshop.config;

import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Forwarded values are usable only across explicitly trusted immediate proxy hops. */
@Component
public class ClientIpResolver {
    private final Set<String> proxies;
    public ClientIpResolver(@Value("${app.security.trusted-proxies:}") String trusted) {
        proxies=Arrays.stream(trusted.split(",")).map(String::trim).filter(s->!s.isEmpty()).collect(Collectors.toUnmodifiableSet());
    }
    public String resolve(HttpServletRequest request) {
        String remote=request.getRemoteAddr();
        if(!proxies.contains(remote)) return remote;
        String forwarded=request.getHeader("X-Forwarded-For");
        if(forwarded==null || forwarded.length()>1024) return remote;
        String[] hops=forwarded.split(",");
        String current=remote;
        for(int i=hops.length-1;i>=0 && proxies.contains(current);i--) {
            String next=hops[i].trim();
            if(!literal(next))return remote;
            current=next;
        }
        return current;
    }
    static boolean literal(String value) {
        if(value==null || value.length()>45 || !value.matches("[0-9a-fA-F:.]+"))return false;
        try { InetAddress.getByName(value); return value.contains(":") || value.matches("[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+"); }
        catch(Exception ex){return false;}
    }
}
