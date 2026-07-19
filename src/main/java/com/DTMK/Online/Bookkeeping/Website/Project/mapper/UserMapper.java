package com.DTMK.Online.Bookkeeping.Website.Project.mapper;

import com.DTMK.Online.Bookkeeping.Website.Project.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

@Mapper
public interface UserMapper {

    // Finds a user by username for login and duplicate checks during registration.
    @Select("SELECT * FROM t_user WHERE username = #{username}")
    User findByUsername(String username);

    // Saves a new user to the database during registration.
    @Insert("INSERT INTO t_user(username, password, avatar) VALUES(#{username}, #{password}, #{avatar})")
    void insertUser(User user);

    // Finds a user by ID.
    @Select("SELECT * FROM t_user WHERE id = #{id}")
    User findById(@Param("id") Integer id);

    // Updates the user's monthly budget target.
    @Update("UPDATE t_user SET monthly_budget = #{monthlyBudget} WHERE id = #{userId}")
    void updateMonthlyBudget(@Param("userId") Integer userId, @Param("monthlyBudget") BigDecimal monthlyBudget);

    /**
     * RBAC: update a user's role. Called by the admin-only
     * {@code POST /api/admin/users/:id/role} endpoint to promote
     * a regular {@code USER} to {@code ADMIN} (or demote back).
     * The caller is expected to have already been authorised by
     * the {@code @PreAuthorize("hasRole('ADMIN')")} annotation
     * on the controller.
     */
    @Update("UPDATE t_user SET role = #{role} WHERE id = #{id}")
    void updateRole(@Param("id") Integer id, @Param("role") String role);

    /**
     * Count users with the {@code ADMIN} role. Used by the
     * bootstrap seed (and by future "is there an admin yet?"
     * health checks). Cheap because of the
     * {@code idx_user_role} index added in the schema migration.
     */
    @Select("SELECT COUNT(*) FROM t_user WHERE role = 'ADMIN'")
    int countAdmins();
}
