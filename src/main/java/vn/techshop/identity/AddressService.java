package vn.techshop.identity;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.techshop.shared.*;
@Service @Transactional(readOnly=true)
public class AddressService {
    public record Input(@NotBlank @Size(min=2,max=100) String recipientName,@NotBlank @Pattern(regexp="0[0-9]{9}") String phone,
        @NotBlank @Size(min=10,max=500) String address,boolean defaultAddress,Long expectedVersion){}
    public record View(Long id,String recipientName,String phone,String address,Long version,boolean defaultAddress){}
    private final JdbcTemplate jdbc;private final UserRepository users;
    public AddressService(JdbcTemplate jdbc,UserRepository users){this.jdbc=jdbc;this.users=users;}
    public List<View> list(Long userId){
        return jdbc.query("select a.*,u.default_address_id from customer_addresses a join users u on u.id=a.user_id where a.user_id=? order by a.id",
            (r,n)->new View(r.getLong("id"),r.getString("recipient_name"),r.getString("phone"),r.getString("address_text"),r.getLong("version"),Objects.equals(r.getObject("default_address_id",Long.class),r.getLong("id"))),userId);
    }
    @Transactional public View save(Long userId,Long id,Input input){
        var owner=users.lockById(userId).orElseThrow(BusinessException::missing);var existing=list(userId);
        String name=Rules.text(input.recipientName(),2,100),phone=Rules.text(input.phone(),10,10),address=Rules.text(input.address(),10,500);
        if(!phone.matches("0[0-9]{9}"))throw BusinessException.invalid("Số điện thoại không hợp lệ.");
        if(id==null){
            if(existing.size()>=10)throw BusinessException.invalid("Sổ địa chỉ tối đa 10 địa chỉ.");
            var keys=new org.springframework.jdbc.support.GeneratedKeyHolder();
            jdbc.update(c->{var s=c.prepareStatement("insert into customer_addresses(user_id,recipient_name,phone,address_text) values(?,?,?,?)",java.sql.Statement.RETURN_GENERATED_KEYS);s.setLong(1,userId);s.setString(2,name);s.setString(3,phone);s.setString(4,address);return s;},keys);
            id=Objects.requireNonNull(keys.getKey()).longValue();
        }else{
            Long chosen=id;var old=existing.stream().filter(a->a.id().equals(chosen)).findFirst().orElseThrow(BusinessException::missing);
            Rules.version(old.version(),input.expectedVersion(),"ADDRESS_CHANGED");
            jdbc.update("update customer_addresses set recipient_name=?,phone=?,address_text=?,version=version+1 where id=? and user_id=?",name,phone,address,id,userId);
        }
        if(input.defaultAddress()||existing.isEmpty()){owner.setDefaultAddressId(id);users.saveAndFlush(owner);}
        Long chosen=id;return list(userId).stream().filter(a->a.id().equals(chosen)).findFirst().orElseThrow();
    }
    @Transactional public void remove(Long userId,Long id,Long version){
        var owner=users.lockById(userId).orElseThrow(BusinessException::missing);
        var old=list(userId).stream().filter(a->a.id().equals(id)).findFirst().orElseThrow(BusinessException::missing);
        Rules.version(old.version(),version,"ADDRESS_CHANGED");jdbc.update("delete from customer_addresses where id=? and user_id=?",id,userId);
        if(Objects.equals(owner.getDefaultAddressId(),id)){owner.setDefaultAddressId(list(userId).stream().map(View::id).findFirst().orElse(null));users.saveAndFlush(owner);}
    }
}
