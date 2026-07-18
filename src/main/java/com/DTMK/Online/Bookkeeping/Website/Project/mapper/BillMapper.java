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
    // Retrieves bills by category and month for chart details.
    @Select("SELECT * FROM t_bill " +
            "WHERE user_id = #{userId} AND category = #{category} " +
            "AND type = 0 " +
            "AND MONTH(bill_date) = #{month} AND YEAR(bill_date) = #{year} " +
            "ORDER BY bill_date DESC")
    List<Bill> findBillsByCategoryAndMonth(
            @org.apache.ibatis.annotations.Param("userId") Integer userId,
            @org.apache.ibatis.annotations.Param("category") String category,
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
}
