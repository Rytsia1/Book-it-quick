package com.DTMK.Online.Bookkeeping.Website.Project.mapper;

import com.DTMK.Online.Bookkeeping.Website.Project.entity.Category;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * MyBatis mapper for {@code t_category}.
 * <p>
 * Soft-delete convention: every read query filters by {@code is_deleted = 0},
 * and the only delete-style method ({@link #softDeleteCategory}) is a soft
 * delete that sets {@code is_deleted = 1}. A future "Trash" UI can restore
 * a category by setting the flag back to 0.
 */
@Mapper
public interface CategoryMapper {

    // Read: retrieves live (non-trashed) categories for a user.
    @Select("SELECT * FROM t_category WHERE user_id = #{userId} AND is_deleted = 0 ORDER BY type DESC, name ASC")
    List<Category> findCategoriesByUserId(Integer userId);

    // Create: inserts a new category. is_deleted defaults to 0 (live) at the DB level.
    @Insert("INSERT INTO t_category(user_id, type, name, created_at, updated_at) " +
            "VALUES(#{userId}, #{type}, #{name}, #{createdAt}, #{updatedAt})")
    @org.apache.ibatis.annotations.Options(useGeneratedKeys = true, keyProperty = "id")
    void insertCategory(Category category);

    // Update: edits a category. The is_deleted = 0 guard prevents resurrecting
    // a soft-deleted row by sending a PUT to its URL.
    @Update("UPDATE t_category SET name = #{name}, type = #{type}, updated_at = #{updatedAt} " +
            "WHERE id = #{id} AND user_id = #{userId} AND is_deleted = 0")
    void updateCategory(Category category);

    // Soft-delete: marks the category as trashed. The row is never actually
    // removed. Same name can be re-created later because countByNameAndType
    // also filters by is_deleted = 0.
    @Update("UPDATE t_category SET is_deleted = 1 " +
            "WHERE id = #{id} AND user_id = #{userId} AND is_deleted = 0")
    void softDeleteCategory(@Param("id") Integer id, @Param("userId") Integer userId);

    // Read: retrieves a single live category by id (user-scoped).
    @Select("SELECT * FROM t_category WHERE id = #{id} AND user_id = #{userId} AND is_deleted = 0")
    Category findByIdAndUserId(@Param("id") Integer id, @Param("userId") Integer userId);

    // Used to reject duplicate names during create/update. Counts only
    // LIVE categories, so a soft-deleted name can be re-created.
    @Select("SELECT COUNT(1) FROM t_category " +
            "WHERE user_id = #{userId} AND name = #{name} AND type = #{type} AND is_deleted = 0")
    int countByNameAndType(@Param("userId") Integer userId, @Param("name") String name, @Param("type") Integer type);
}
