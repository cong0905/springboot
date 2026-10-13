package vn.techshop.identity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class AddressServiceTest {
    @Autowired IdentityService identity;@Autowired AddressService addresses;@Autowired MockMvc mvc;
    Long owner(){return identity.register(new IdentityService.Registration("Address Owner",java.util.UUID.randomUUID()+"@test.local","AddressPass2026!")).id();}
    AddressService.Input input(boolean def,Long version){return new AddressService.Input("Recipient","0912345678","123 Hanoi Street, Hanoi",def,version);}
    @Test void defaultIsUniqueAndDeletingDefaultPromotesRemainingAddress() {
        Long a=owner(),b=owner();var first=addresses.save(a,null,input(false,null));var second=addresses.save(a,null,input(true,null));
        assertThat(addresses.list(a).stream().filter(AddressService.View::defaultAddress)).extracting(AddressService.View::id).containsExactly(second.id());
        assertThatThrownBy(()->addresses.save(b,first.id(),input(true,0L))).isInstanceOf(vn.techshop.shared.BusinessException.class);
        addresses.save(a,first.id(),input(true,0L));
        assertThatThrownBy(()->addresses.remove(a,first.id(),0L)).isInstanceOf(vn.techshop.shared.BusinessException.class);
        addresses.remove(a,first.id(),1L);assertThat(addresses.list(a)).hasSize(1);assertThat(addresses.list(a).getFirst().defaultAddress()).isTrue();
    }
    @Test void apiRequiresOwnerAndCsrfAndRendersAddressBook() throws Exception {
        Long a=owner(),b=owner();var address=addresses.save(a,null,input(false,null));
        var pa=identity.loadUserByUsername(identity.profile(a).email());var pb=identity.loadUserByUsername(identity.profile(b).email());
        mvc.perform(get("/addresses").with(user(pa))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/me/addresses")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/v1/me/addresses/"+address.id()).param("expectedVersion","0").with(user(pa))).andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/me/addresses/"+address.id()).param("expectedVersion","0").with(user(pb)).with(csrf())).andExpect(status().isNotFound());
    }
}
