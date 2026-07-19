package com.DTMK.Online.Bookkeeping.Website.Project.mapper;

import com.DTMK.Online.Bookkeeping.Website.Project.entity.RefreshToken;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * MyBatis mapper for {@code t_refresh_token}. The style matches the
 * project's other mappers (annotation-based, no XML) — see
 * {@code UserMapper} for the convention.
 */
@Mapper
public interface RefreshTokenMapper {

    /** Persist a new refresh-token row. The caller has already hashed the token. */
    @Insert("INSERT INTO t_refresh_token " +
            "(user_id, token_hash, expires_at, revoked, replaced_by, created_at) " +
            "VALUES (#{userId}, #{tokenHash}, #{expiresAt}, #{revoked}, #{replacedBy}, #{createdAt})")
    @org.apache.ibatis.annotations.Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(RefreshToken token);

    /** Look up a refresh token by its SHA-256 hash. The hot path on /refresh. */
    @Select("SELECT * FROM t_refresh_token WHERE token_hash = #{tokenHash}")
    RefreshToken findByHash(@Param("tokenHash") String tokenHash);

    /**
     * Mark a single token as revoked. Used on logout and after a
     * successful /refresh rotation. The {@code replaced_by} pointer is
     * set by the caller before invoking this so the rotation chain is
     * preserved in a single statement.
     */
    @Update("UPDATE t_refresh_token SET revoked = 1, replaced_by = #{replacedBy} WHERE id = #{id}")
    void revoke(@Param("id") Integer id, @Param("replacedBy") Integer replacedBy);

    /**
     * Mark every active token for a user as revoked. Used when an
     * anomaly is detected and we want to forcibly log the user out of
     * every session (e.g. password reset, "log out everywhere").
     */
    @Update("UPDATE t_refresh_token SET revoked = 1 WHERE user_id = #{userId} AND revoked = 0")
    void revokeAllForUser(@Param("userId") Integer userId);

    /** Daily cleanup: drop rows that have already passed their expiry. */
    @Delete("DELETE FROM t_refresh_token WHERE expires_at < NOW()")
    int deleteExpired();
}
