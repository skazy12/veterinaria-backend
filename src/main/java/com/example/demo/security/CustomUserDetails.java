package com.example.demo.security;


import com.example.demo.model.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class CustomUserDetails implements UserDetails {

    private String email;
    private List<Role> roles;
    private boolean enabled;

    // Constructor, Getters y Setters

    public CustomUserDetails(String email, List<Role> roles, boolean enabled) {
        this.email = email;
        this.roles = roles;
        this.enabled = enabled;
    }

    public List<Role> getRoles() {
        return roles;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Si necesitas implementar roles en granted authorities
        return null;
    }

    @Override
    public String getPassword() {
        return null; // No usas contraseña, Firebase maneja esto
    }

    @Override
    public String getUsername() {
        return email;
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
        return enabled;
    }
}
