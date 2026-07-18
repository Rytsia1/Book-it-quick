package com.DTMK.Online.Bookkeeping.Website.Project.mapper;

import com.DTMK.Online.Bookkeeping.Website.Project.entity.RecurringBill;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * MyBatis mapper for {@code t_recurring_bill}.
 * <p>
 * Soft-delete convention: every read query filters by {@code is_deleted = 0},
 * and the only delete-style method ({@link #softDelete}) is a soft delete
 * that sets {@code is_deleted = 1}.
 * <p>
 * <b>Critically:</b> the scheduler's hot query
 * {@link #findDueTemplates} also filters by {@code is_deleted = 0} so
 * a deleted template never fires again. This is the single most
 * important read in the whole soft-delete layer — without this guard
 * the cron job would happily post bills for "deleted" templates every
 * month, which would be the worst possible data-integrity bug.
 */
@Mapper
public interface RecurringBillMapper {

    /**
     * Insert a new recurring-bill template. The service layer is expected
     * to have already validated the payload (clamped day-of-month to 1-28,
     * set startYearMonth to the current YYYY-MM, etc.). is_deleted
     * defaults to 0 (live) at the DB level.
     */
    @Insert("INSERT INTO t_recurring_bill(user_id, amount, type, category, description, " +
            "day_of_month, start_year_month, last_run_year_month, active, created_at) " +
            "VALUES(#{userId}, #{amount}, #{type}, #{category}, #{description}, " +
            "#{dayOfMonth}, #{startYearMonth}, #{lastRunYearMonth}, #{active}, #{createdAt})")
    void insert(RecurringBill recurring);

    // Read: retrieves a single live template by id.
    @Select("SELECT * FROM t_recurring_bill WHERE id = #{id} AND is_deleted = 0")
    RecurringBill findById(Integer id);

    // Read: user-scoped lookup so the controller can verify ownership.
    @Select("SELECT * FROM t_recurring_bill " +
            "WHERE id = #{id} AND user_id = #{userId} AND is_deleted = 0")
    RecurringBill findByIdAndUserId(@Param("id") Integer id, @Param("userId") Integer userId);

    // Read: lists every live template for a user.
    @Select("SELECT * FROM t_recurring_bill WHERE user_id = #{userId} AND is_deleted = 0 ORDER BY id DESC")
    List<RecurringBill> findByUserId(Integer userId);

    /**
     * The hot query. Returns every live, active template that:
     * <ul>
     *   <li>is active (active = 1),</li>
     *   <li>has a day-of-month that has been reached today,</li>
     *   <li>has not already run this month,</li>
     *   <li>and whose start_month has been reached.</li>
     * </ul>
     * The {@code is_deleted = 0} guard is critical: a deleted template
     * MUST NOT fire again.
     */
    @Select("SELECT * FROM t_recurring_bill " +
            "WHERE active = 1 AND is_deleted = 0 " +
            "AND day_of_month <= #{todayDay} " +
            "AND (last_run_year_month IS NULL OR last_run_year_month < #{currentYearMonth}) " +
            "AND start_year_month <= #{currentYearMonth}")
    List<RecurringBill> findDueTemplates(@Param("todayDay") int todayDay,
                                          @Param("currentYearMonth") String currentYearMonth);

    /**
     * Conditional update so two scheduler instances can't both "win" the
     * same row. The is_deleted = 0 guard ensures we only stamp
     * live templates.
     */
    @Update("UPDATE t_recurring_bill " +
            "SET last_run_year_month = #{yearMonth} " +
            "WHERE id = #{id} AND is_deleted = 0 " +
            "AND (last_run_year_month IS NULL OR last_run_year_month < #{yearMonth})")
    int updateLastRun(@Param("id") Integer id, @Param("yearMonth") String yearMonth);

    // Pause / resume. The is_deleted = 0 guard prevents a user from
    // re-activating a soft-deleted template via the PATCH endpoint.
    @Update("UPDATE t_recurring_bill SET active = #{active} " +
            "WHERE id = #{id} AND user_id = #{userId} AND is_deleted = 0")
    int updateActive(@Param("id") Integer id, @Param("userId") Integer userId, @Param("active") int active);

    // Soft-delete: marks the template as trashed. The row is never
    // actually removed. The next scheduler tick simply skips it.
    @Update("UPDATE t_recurring_bill SET is_deleted = 1 " +
            "WHERE id = #{id} AND user_id = #{userId} AND is_deleted = 0")
    int softDelete(@Param("id") Integer id, @Param("userId") Integer userId);
}
