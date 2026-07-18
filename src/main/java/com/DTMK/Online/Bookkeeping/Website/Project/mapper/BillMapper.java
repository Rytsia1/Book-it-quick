package com.DTMK.Online.Bookkeeping.Website.Project.mapper;

import com.DTMK.Online.Bookkeeping.Website.Project.entity.Bill;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface BillMapper {

    // Read: retrieves bills by user ID.
    @Select("SELECT * FROM t_bill WHERE user_id = #{userId} ORDER BY bill_date DESC")
    List<Bill> findBillsByUserId(Integer userId);

    // ── Pagination support ─────────────────────────────────────────────
    // These four methods back GET /api/bills/page and GET /api/bills/counts.
    // They keep the existing findBillsByUserId untouched so the dashboard
    // (which still loads the full list) keeps working unchanged.

    /**
     * Paginated read: returns one page of bills for a user, optionally
     * filtered by type. Ordered by bill_date DESC then id DESC so pages
     * stay stable when many rows share the same date.
     * <p>
     * The {@code <if>} block is MyBatis dynamic SQL — the {@code type}
     * filter is only added when the caller passes a non-null value
     * (i.e. the frontend filter tab is "all"). Setting type to 0 or 1
     * restricts the result to expenses or income respectively.
     * <p>
     * {@code offset} is zero-based; {@code size} is the page size.
     */
    @org.apache.ibatis.annotations.Select({
            "<script>",
            "SELECT * FROM t_bill",
            "WHERE user_id = #{userId}",
            "<if test='type != null'> AND type = #{type} </if>",
            "ORDER BY bill_date DESC, id DESC",
            "LIMIT #{offset}, #{size}",
            "</script>"
    })
    List<Bill> findBillsByPage(
            @org.apache.ibatis.annotations.Param("userId") Integer userId,
            @org.apache.ibatis.annotations.Param("type")   Integer type,
            @org.apache.ibatis.annotations.Param("offset") int offset,
            @org.apache.ibatis.annotations.Param("size")   int size);

    /**
     * Total row count for the same filter as {@link #findBillsByPage},
     * used by {@code <el-pagination>} to render the page count.
     */
    @org.apache.ibatis.annotations.Select({
            "<script>",
            "SELECT COUNT(*) FROM t_bill",
            "WHERE user_id = #{userId}",
            "<if test='type != null'> AND type = #{type} </if>",
            "</script>"
    })
    long countBillsByPage(
            @org.apache.ibatis.annotations.Param("userId") Integer userId,
            @org.apache.ibatis.annotations.Param("type")   Integer type);

    /**
     * Count of every bill (income + expense) for a user.
     * Backs the "ALL" filter tab in {@code Bills.vue}.
     */
    @Select("SELECT COUNT(*) FROM t_bill WHERE user_id = #{userId}")
    long countAllBills(Integer userId);

    /**
     * Count of income bills ({@code type = 1}) for a user.
     * Backs the "INCOME" filter tab.
     */
    @Select("SELECT COUNT(*) FROM t_bill WHERE user_id = #{userId} AND type = 1")
    long countIncomeBills(Integer userId);

    /**
     * Count of expense bills ({@code type = 0}) for a user.
     * Backs the "EXPENSE" filter tab.
     */
    @Select("SELECT COUNT(*) FROM t_bill WHERE user_id = #{userId} AND type = 0")
    long countExpenseBills(Integer userId);

    // Read: retrieves a single bill by its primary key.
    @Select("SELECT * FROM t_bill WHERE id = #{id}")
    Bill findById(Integer id);

    // Create: adds a new bill.
    @Insert("INSERT INTO t_bill(user_id, amount, type, category, description, bill_date) " +
            "VALUES(#{userId}, #{amount}, #{type}, #{category}, #{description}, #{billDate})")
    void insertBill(Bill bill);

    // Delete: removes a bill by bill ID.
    @Delete("DELETE FROM t_bill WHERE id = #{id}")
    void deleteBill(Integer id);

    // Calculates total monthly income.
    @Select("SELECT COALESCE(SUM(amount), 0) FROM t_bill WHERE user_id = #{userId} AND type = 1 AND MONTH(bill_date) = #{month} AND YEAR(bill_date) = #{year}")
    BigDecimal calculateMonthlyIncome(Integer userId, int month, int year);

    // Calculates total monthly expenses.
    @Select("SELECT COALESCE(SUM(amount), 0) FROM t_bill WHERE user_id = #{userId} AND type = 0 AND MONTH(bill_date) = #{month} AND YEAR(bill_date) = #{year}")
    BigDecimal calculateMonthlyExpense(Integer userId, int month, int year);

    // Update: edits a bill.
    @org.apache.ibatis.annotations.Update("UPDATE t_bill SET amount=#{amount}, type=#{type}, category=#{category}, description=#{description}, bill_date=#{billDate} WHERE id=#{id}")
    void updateBill(Bill bill);

    // Retrieves expense statistics by category for the pie chart (type = 0 only).
    @org.apache.ibatis.annotations.Select("SELECT category AS name, SUM(amount) AS value FROM t_bill " +
            "WHERE user_id = #{userId} AND type = 0 AND MONTH(bill_date) = #{month} AND YEAR(bill_date) = #{year} " +
            "GROUP BY category")
    java.util.List<com.DTMK.Online.Bookkeeping.Website.Project.dto.CategoryStatDTO> getExpenseByCategory(
            @org.apache.ibatis.annotations.Param("userId") Integer userId,
            @org.apache.ibatis.annotations.Param("month") int month,
            @org.apache.ibatis.annotations.Param("year") int year);

    // Retrieves income statistics by category for the pie chart (type = 1 only).
    @org.apache.ibatis.annotations.Select("SELECT category AS name, SUM(amount) AS value FROM t_bill " +
            "WHERE user_id = #{userId} AND type = 1 AND MONTH(bill_date) = #{month} AND YEAR(bill_date) = #{year} " +
            "GROUP BY category")
    java.util.List<com.DTMK.Online.Bookkeeping.Website.Project.dto.CategoryStatDTO> getIncomeByCategory(
            @org.apache.ibatis.annotations.Param("userId") Integer userId,
            @org.apache.ibatis.annotations.Param("month") int month,
            @org.apache.ibatis.annotations.Param("year") int year);
    // Retrieves bills by category, type, and month for chart details.
    @Select("SELECT * FROM t_bill " +
            "WHERE user_id = #{userId} AND category = #{category} " +
            "AND type = #{type} " +
            "AND MONTH(bill_date) = #{month} AND YEAR(bill_date) = #{year} " +
            "ORDER BY bill_date DESC")
    List<Bill> findBillsByCategoryAndMonth(
            @org.apache.ibatis.annotations.Param("userId") Integer userId,
            @org.apache.ibatis.annotations.Param("category") String category,
            @org.apache.ibatis.annotations.Param("type") int type,
            @org.apache.ibatis.annotations.Param("month") int month,
            @org.apache.ibatis.annotations.Param("year") int year);
    // Retrieves bills by type (0 = expense, 1 = income) and month.
    @Select("SELECT * FROM t_bill " +
            "WHERE user_id = #{userId} AND type = #{type} " +
            "AND MONTH(bill_date) = #{month} AND YEAR(bill_date) = #{year} " +
            "ORDER BY bill_date DESC")
    List<Bill> findBillsByTypeAndMonth(
            @org.apache.ibatis.annotations.Param("userId") Integer userId,
            @org.apache.ibatis.annotations.Param("type") int type,
            @org.apache.ibatis.annotations.Param("month") int month,
            @org.apache.ibatis.annotations.Param("year") int year);

    // Retrieves all bills (both income and expense) for a user within a month.
    @Select("SELECT * FROM t_bill " +
            "WHERE user_id = #{userId} " +
            "AND MONTH(bill_date) = #{month} AND YEAR(bill_date) = #{year} " +
            "ORDER BY bill_date DESC")
    List<Bill> findAllBillsByMonth(
            @org.apache.ibatis.annotations.Param("userId") Integer userId,
            @org.apache.ibatis.annotations.Param("month") int month,
            @org.apache.ibatis.annotations.Param("year") int year);

    // Retrieves daily income and expense totals for a month (for the line chart).
    @org.apache.ibatis.annotations.Select("SELECT bill_date AS date, " +
            "SUM(CASE WHEN type = 1 THEN amount ELSE 0 END) AS income, " +
            "SUM(CASE WHEN type = 0 THEN amount ELSE 0 END) AS expense " +
            "FROM t_bill " +
            "WHERE user_id = #{userId} " +
            "AND MONTH(bill_date) = #{month} AND YEAR(bill_date) = #{year} " +
            "GROUP BY bill_date " +
            "ORDER BY bill_date ASC")
    List<com.DTMK.Online.Bookkeeping.Website.Project.dto.DailyStatDTO> getDailyStats(
            @org.apache.ibatis.annotations.Param("userId") Integer userId,
            @org.apache.ibatis.annotations.Param("month") int month,
            @org.apache.ibatis.annotations.Param("year") int year);
}
