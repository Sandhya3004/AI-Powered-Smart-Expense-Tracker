package com.expense.repository;

import com.expense.entity.Receipt;
import com.expense.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReceiptRepository extends JpaRepository<Receipt, Long> {

    List<Receipt> findByUserOrderByCreatedAtDesc(User user);
    
    @Query("SELECT r FROM Receipt r WHERE r.user = :user ORDER BY r.createdAt DESC")
    List<Receipt> findByUser(@Param("user") User user);
    
    @Query("SELECT r FROM Receipt r WHERE r.user = :user AND r.type = :type ORDER BY r.createdAt DESC")
    List<Receipt> findByUserAndType(@Param("user") User user, @Param("type") String type);
    
    @Query("SELECT r FROM Receipt r WHERE r.user = :user AND r.status = :status ORDER BY r.createdAt DESC")
    List<Receipt> findByUserAndStatus(@Param("user") User user, @Param("status") String status);
    
    @Query("SELECT r FROM Receipt r WHERE r.user = :user AND r.confidence >= :minConfidence ORDER BY r.createdAt DESC")
    List<Receipt> findByUserAndConfidenceGreaterThanEqual(@Param("user") User user, @Param("minConfidence") Double minConfidence);
    
    @Query("SELECT r FROM Receipt r WHERE r.user = :user AND r.source = :source ORDER BY r.createdAt DESC")
    List<Receipt> findByUserAndSource(@Param("user") User user, @Param("source") String source);
    
    @Query("SELECT COUNT(r) FROM Receipt r WHERE r.user = :user AND r.status = :status")
    long countByUserAndStatus(@Param("user") User user, @Param("status") String status);
    
    @Query("SELECT r FROM Receipt r WHERE r.user = :user AND r.category = :category ORDER BY r.createdAt DESC")
    List<Receipt> findByUserAndCategory(@Param("user") User user, @Param("category") String category);
    
    @Query("SELECT r FROM Receipt r WHERE r.user = :user AND (r.merchant LIKE CONCAT('%', :query, '%') OR r.notes LIKE CONCAT('%', :query, '%') OR r.category LIKE CONCAT('%', :query, '%'))")
    List<Receipt> findByUserAndSearchQuery(@Param("user") User user, @Param("query") String query);
    
    @Query("SELECT r FROM Receipt r WHERE r.user = :user AND r.confidence >= :minConfidence ORDER BY r.createdAt DESC")
    List<Receipt> findHighConfidenceReceipts(@Param("user") User user, @Param("minConfidence") Double minConfidence);
}
