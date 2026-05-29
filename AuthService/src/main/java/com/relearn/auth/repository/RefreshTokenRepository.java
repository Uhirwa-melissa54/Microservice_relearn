package com.relearn.auth.repository;

import com.relearn.auth.entity.RefreshToken;
import com.relearn.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Data access layer for RefreshToken entities.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /** Find a refresh token by its token string */
    Optional<RefreshToken> findByToken(String token);

    /** Delete all refresh tokens belonging to a specific user.
     *  Called on logout or when issuing a new refresh token. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    int deleteByUser(User user);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    int deleteByUser_Id(Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    int deleteByToken(String token);
}
