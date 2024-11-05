package com.example.demo.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FirebaseAuth firebaseAuth;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        try {
            // Obtener el usuario de Firebase
            UserRecord userRecord = firebaseAuth.getUserByEmail(email);

            // Obtener información adicional del usuario desde tu base de datos
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            user.getRoles().forEach(role -> {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toString().toUpperCase()));
            });

            return new org.springframework.security.core.userdetails.User(
                    userRecord.getEmail(),
                    userRecord.getUid(), // Usamos el UID de Firebase como "contraseña"
                    userRecord.isEmailVerified(),
                    true,
                    true,
                    !userRecord.isDisabled(),
                    authorities);
        } catch (Exception e) {
            throw new UsernameNotFoundException("User not found with email: " + email, e);
        }
    }

    public UserDetails loadUserByToken(String idToken) throws UsernameNotFoundException {
        try {
            // Verificar el token ID de Firebase
            FirebaseToken decodedToken = firebaseAuth.verifyIdToken(idToken);
            String uid = decodedToken.getUid();

            // Obtener el usuario de Firebase
            UserRecord userRecord = firebaseAuth.getUser(uid);

            // Obtener información adicional del usuario desde tu base de datos
            User user = userRepository.findByEmail(userRecord.getEmail())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + userRecord.getEmail()));

            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            user.getRoles().forEach(role -> {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toString().toUpperCase()));
            });

            return new org.springframework.security.core.userdetails.User(
                    userRecord.getEmail(),
                    userRecord.getUid(),
                    userRecord.isEmailVerified(),
                    true,
                    true,
                    !userRecord.isDisabled(),
                    authorities);
        } catch (Exception e) {
            throw new UsernameNotFoundException("Failed to authenticate token", e);
        }
    }
}