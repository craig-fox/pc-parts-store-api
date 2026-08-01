package nz.fox.craig.customer.security;


import lombok.Getter;
import lombok.RequiredArgsConstructor;
import nz.fox.craig.customer.model.Customer;
import nz.fox.craig.customer.model.CustomerStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Getter
@RequiredArgsConstructor
public class CustomerUserDetails implements UserDetails {

    private final Customer customer;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
    }

    @Override
    public String getPassword() {
        return customer.getPassword();
    }

    @Override
    public String getUsername() {
        return customer.getEmail();
    }

    public UUID getCustomerId() {
        return customer.getId();
    }

    public String getFirstName() {
        return customer.getFirstName();
    }

    public String getLastName() {
        return customer.getLastName();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return customer.getStatus() == CustomerStatus.ACTIVE;
    }
}