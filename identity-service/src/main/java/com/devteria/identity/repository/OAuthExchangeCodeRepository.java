package com.devteria.identity.repository;

import java.util.Date;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.devteria.identity.entity.OAuthExchangeCode;

@Repository
public interface OAuthExchangeCodeRepository extends JpaRepository<OAuthExchangeCode, String> {
    @Modifying
    @Query("DELETE FROM OAuthExchangeCode o WHERE o.expiryTime < :now")
    void deleteExpired(@Param("now") Date now);
}
