package com.DTMK.Online.Bookkeeping.Website.Project.mapper;

import com.DTMK.Online.Bookkeeping.Website.Project.entity.Bill;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.util.List;

/**
 * MyBatis mapper for {@code t_bill}.
 * <p>
 * <b>Soft-delete convention:</b> every read query filters by
 * {@code is_deleted = 0}, and the only delete-style method
 * ({@link #softDeleteBill}) is a soft delete — it sets
 * {@code is_deleted = 1} rather than removing the row. This preserves
 * the audit trail required for financial systems and is the foundation
 * for a future "Trash" / restore feature.
 */
@Mapper
public interface BillMapper {

    // Read: retrieves live (non-trashed) bills by user ID.
    @Select("SELECT * FROM t_bill WHERE user_id = #{userId} AND is_deleted = 0 ORDER BY bill_date DESC")
    List<Bill> findBillsByUserId(Integer userId);

    // ── Pagination support ─────────────────────────────────────────────
    // These methods back GET /api/bills/page and GET /api/bills/counts.
    // The is_deleted = 0 guard is applied inside the <script> block so
    // it composes cleanly with the existing dynamic <if test='type != null'>.

    /**
     * Paginated read: returns one page of live bills for a user,
     * optionally filtered by type. Ordered by bill_date DESC then id DESC
     * so pages stay stable when many rows share the same date.
     */
    @Select({
            "<script>",
            "SELECT * FROM t_bill",
            "WHERE user_id = #{userId} AND is_deleted = 0",
            "<if test='type != null'> AND type = #{type} </if>",
            "ORDER BY bill_date DESC, id DESC",
            "LIMIT #{offset}, #{size}",
            "</script>"
    })
    List<Bill> findBillsByPage(
            @Param("userId") Integer userId,
            @Param("type")   Integer type,
            @Param("offset") int offset,
            @Param("size")   int size);

    /**
     * Total row count for the same filter as {@link #findBillsByPage},
     * used by {@code <el-pagination>} to render the page count.
     */
    @Select({
            "<script>",
            "SELECT COUNT(*) FROM t_bill",
            "WHERE user_id = #{userId} AND is_deleted = 0",
            "<if test='type != null'> AND type = #{type} </if>",
            "</script>"
    })
    long countBillsByPage(
            @Param("userId") Integer userId,
            @Param("type")   Integer type);

    /** Count of every live bill (income + expense) for a user. */
    @Select("SELECT COUNT(*) FROM t_bill WHERE user_id = #{userId} AND is_deleted = 0")
    long countAllBills(Integer userId);

    /** Count of live income bills ({@code type = 1}) for a user. */
    @Select("SELECT COUNT(*) FROM t_bill WHERE user_id = #{userId} AND type = 1 AND is_deleted = 0")
    long countIncomeBills(Integer userId);

    /** Count of live expense bills ({@code type = 0}) for a user. */
    @Select("SELECT COUNT(*) FROM t_bill WHERE user_id = #{userId} AND type = 0 AND is_deleted = 0")
    long countExpenseBills(Integer userId);

    // Read: retrieves a single live bill by its primary key.
    @Select("SELECT * FROM t_bill WHERE id = #{id} AND is_deleted = 0")
    Bill findById(Integer id);

    // Create: adds a new bill. is_deleted defaults to 0 (live) at the DB level.
    @Insert("INSERT INTO t_bill(user_id, amount, type, category, description, bill_date) " +
            "VALUES(#{userId}, #{amount}, #{type}, #{category}, #{description}, #{billDate})")
    void insertBill(Bill bill);

    // Soft-delete: marks the bill as trashed. The row is never actually
    // removed, so a future "Trash" UI can restore by setting is_deleted=0.
    // The is_deleted = 0 guard is a no-op (it'd be 1 already) but it
    // makes the intent explicit and protects against a racing update.
    /**
     * Soft-delete a single bill by id. Returns the number of affected
     * rows so the service layer can map "not found / already trashed"
     * to a 404 (instead of a misleading 200 with no body).
     */
    @Update("UPDATE t_bill SET is_deleted = 1 WHERE id = #{id} AND is_deleted = 0")
    int softDeleteBill(Integer id);

    // Calculates total monthly income (excludes soft-deleted rows so the
    // budget bar never includes a trashed bill).
    @Select("SELECT COALESCE(SUM(amount), 0) FROM t_bill " +
            "WHERE user_id = #{userId} AND type = 1 AND is_deleted = 0 " +
            "AND MONTH(bill_date) = #{month} AND YEAR(bill_date) = #{year}")
    BigDecimal calculateMonthlyIncome(Integer userId, int month, int year);

    // Calculates total monthly expenses (excludes soft-deleted rows).
    @Select("SELECT COALESCE(SUM(amount), 0) FROM t_bill " +
            "WHERE user_id = #{userId} AND type = 0 AND is_deleted = 0 " +
            "AND MONTH(bill_date) = #{month} AND YEAR(bill_date) = #{year}")
    BigDecimal calculateMonthlyExpense(Integer userId, int month, int year);

    // Update: edits a bill. The is_deleted = 0 guard prevents resurrecting
    // a soft-deleted row by sending a PUT to its URL.
    @Update("UPDATE t_bill SET amount=#{amount}, type=#{type}, category=#{category}, " +
            "description=#{description}, bill_date=#{billDate} " +
            "WHERE id=#{id} AND is_deleted = 0")
    void updateBill(Bill bill);

    // Retrieves expense statistics by category for the pie chart.
    // Excludes soft-deleted rows.
    @Select("SELECT category AS name, SUM(amount) AS value FROM t_bill " +
            "WHERE user_id = #{userId} AND type = 0 AND is_deleted = 0 " +
            "AND MONTH(bill_date) = #{month} AND YEAR(bill_date) = #{year} " +
            "GROUP BY category")
    java.util.List<com.DTMK.Online.Bookkeeping.Website.Project.dto.CategoryStatDTO> getExpenseByCategory(
            @Param("userId") Integer userId,
            @Param("month") int month,
            @Param("year") int year);

    // Retrieves income statistics by category for the pie chart.
    @Select("SELECT category AS name, SUM(amount) AS value FROM t_bill " +
            "WHERE user_id = #{userId} AND type = 1 AND is_deleted = 0 " +
            "AND MONTH(bill_date) = #{month} AND YEAR(bill_date) = #{year} " +
            "GROUP BY category")
    java.util.List<com.DTMK.Online.Bookkeeping.Website.Project.dto.CategoryStatDTO> getIncomeByCategory(
            @Param("userId") Integer userId,
            @Param("month") int month,
            @Param("year") int year);

    // Retrieves live bills by category, type, and month for chart details.
    @Select("SELECT * FROM t_bill " +
            "WHERE user_id = #{userId} AND category = #{category} " +
            "AND type = #{type} AND is_deleted = 0 " +
            "AND MONTH(bill_date) = #{month} AND YEAR(bill_date) = #{year} " +
            "ORDER BY bill_date DESC")
    List<Bill> findBillsByCategoryAndMonth(
            @Param("userId") Integer userId,
            @Param("category") String category,
            @Param("type") int type,
            @Param("month") int month,
            @Param("year") int year);

    // Retrieves live bills by type and month.
    @Select("SELECT * FROM t_bill " +
            "WHERE user_id = #{userId} AND type = #{type} AND is_deleted = 0 " +
            "AND MONTH(bill_date) = #{month} AND YEAR(bill_date) = #{year} " +
            "ORDER BY bill_date DESC")
    List<Bill> findBillsByTypeAndMonth(
            @Param("userId") Integer userId,
            @Param("type") int type,
            @Param("month") int month,
            @Param("year") int year);

    // Retrieves all live bills (income + expense) for a user in a month.
    @Select("SELECT * FROM t_bill " +
            "WHERE user_id = #{userId} AND is_deleted = 0 " +
            "AND MONTH(bill_date) = #{month} AND YEAR(bill_date) = #{year} " +
            "ORDER BY bill_date DESC")
    List<Bill> findAllBillsByMonth(
            @Param("userId") Integer userId,
            @Param("month") int month,
            @Param("year") int year);

    // Retrieves daily income/expense totals (line chart). Excludes soft-deleted.
    @Select("SELECT bill_date AS date, " +
            "SUM(CASE WHEN type = 1 THEN amount ELSE 0 END) AS income, " +
            "SUM(CASE WHEN type = 0 THEN amount ELSE 0 END) AS expense " +
            "FROM t_bill " +
            "WHERE user_id = #{userId} AND is_deleted = 0 " +
            "AND MONTH(bill_date) = #{month} AND YEAR(bill_date) = #{year} " +
            "GROUP BY bill_date " +
            "ORDER BY bill_date ASC")
    List<com.DTMK.Online.Bookkeeping.Website.Project.dto.DailyStatDTO> getDailyStats(
            @Param("userId") Integer userId,
            @Param("month") int month,
            @Param("year") int year);
}
