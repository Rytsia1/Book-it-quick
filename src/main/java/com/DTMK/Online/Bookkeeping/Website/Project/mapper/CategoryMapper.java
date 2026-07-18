package com.DTMK.Online.Bookkeeping.Website.Project.mapper;

import com.DTMK.Online.Bookkeeping.Website.Project.entity.Category;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CategoryMapper {

    @Select("SELECT * FROM t_category WHERE user_id = #{userId} ORDER BY type DESC, name ASC")
    List<Category> findCategoriesByUserId(Integer userId);

    @Insert("INSERT INTO t_category(user_id, type, name, created_at, updated_at) " +
            "VALUES(#{userId}, #{type}, #{name}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertCategory(Category category);

    @Update("UPDATE t_category SET name = #{name}, type = #{type}, updated_at = #{updatedAt} WHERE id = #{id} AND user_id = #{userId}")
    void updateCategory(Category category);

    @Delete("DELETE FROM t_category WHERE id = #{id} AND user_id = #{userId}")
    void deleteCategory(@Param("id") Integer id, @Param("userId") Integer userId);

    @Select("SELECT * FROM t_category WHERE id = #{id} AND user_id = #{userId}")
    Category findByIdAndUserId(@Param("id") Integer id, @Param("userId") Integer userId);

    @Select("SELECT COUNT(1) FROM t_category WHERE user_id = #{userId} AND name = #{name} AND type = #{type}")
    int countByNameAndType(@Param("userId") Integer userId, @Param("name") String name, @Param("type") Integer type);
}
