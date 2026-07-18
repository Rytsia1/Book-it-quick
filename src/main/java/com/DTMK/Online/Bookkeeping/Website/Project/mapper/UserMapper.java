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
}
