package com.DTMK.Online.Bookkeeping.Website.Project.mapper;

import com.DTMK.Online.Bookkeeping.Website.Project.entity.TokenDenylist;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * MyBatis mapper for {@code t_token_denylist}. Only consulted at
 * {@code /api/auth/refresh} and {@code /api/auth/logout} (per the
 * performance decision documented in the schema), never on every
 * request, so the table stays cheap to maintain.
 */
@Mapper
public interface TokenDenylistMapper {

    /**
     * Insert a denylist row. We use {@code INSERT IGNORE} so a duplicate
     * jti (which can happen if a client retries the same logout) is a
     * no-op rather than an exception.
     */
    @Insert("INSERT IGNORE INTO t_token_denylist " +
            "(jti, user_id, expires_at, reason, created_at) " +
            "VALUES (#{jti}, #{userId}, #{expiresAt}, #{reason}, #{createdAt})")
    void insert(TokenDenylist entry);

    /**
     * Returns the count of denylist rows matching the given jti whose
     * natural expiry is still in the future. A denylist row that has
     * itself expired doesn't count &mdash; the underlying access token
     * is no longer valid anyway, and the row is about to be pruned by
     * the daily cleanup job.
     */
    @Select("SELECT COUNT(*) FROM t_token_denylist " +
            "WHERE jti = #{jti} AND expires_at > NOW()")
    int isRevoked(@Param("jti") String jti);

    /** Daily cleanup: drop denylist rows whose underlying token has naturally expired. */
    @Delete("DELETE FROM t_token_denylist WHERE expires_at < NOW()")
    int deleteExpired();
}
