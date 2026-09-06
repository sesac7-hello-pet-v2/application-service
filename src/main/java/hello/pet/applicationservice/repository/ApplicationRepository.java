package hello.pet.applicationservice.repository;

import hello.pet.applicationservice.entity.Application;
import hello.pet.applicationservice.entity.ApplicationStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {
    long countByIdInAndStatus(List<Long> ids, ApplicationStatus status);
    Page<Application> findByUserId(Long userId, Pageable pageable);

    Page<Application> findByAnnouncementId(Long announcementId, Pageable pageable);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("""
                UPDATE Application a
                SET a.status = 'REJECTED'
                WHERE a.announcementId = :announcementId
                AND a.id != :applicationId
                AND a.status IN ('SUBMITTED', 'UNDER_REVIEW')
            """)
    int bulkRejectApplications(@Param("announcementId") Long announcementId,
                               @Param("applicationId") Long applicationId);

    Optional<Application> findByUserIdAndAnnouncementId(Long userId, Long announcementId);

    // 특정 공고의 신청서 상태 일괄 변경
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE Application a
            SET a.status = :newStatus
            WHERE a.announcementId = :announcementId
            AND a.status = :currentStatus
            """)
    int bulkUpdateStatus(@Param("announcementId") Long announcementId,
                         @Param("currentStatus") ApplicationStatus currentStatus,
                         @Param("newStatus") ApplicationStatus newStatus);

    /**
     * Saga에서 사용하는 메서드
     * 특정 공고에서 주어진 상태의 신청서 ID 목록을 조회하되, 지정한 ID는 제외
     */
    @Query("""
            SELECT a.id FROM Application a
            WHERE a.announcementId = :announcementId
            AND a.status = :status
            AND a.id != :excludeId
            """)
    List<Long> findOtherApplicationIds(
            @Param("announcementId") Long announcementId,
            @Param("status") ApplicationStatus status,
            @Param("excludeId") Long excludeId);

    /** Saga 변경 대상을 ID로 제한한다. 벌크 쿼리 전에 승인 엔티티의 변경을 반영한다. */
    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE Application a
            SET a.status = :newStatus
            WHERE a.id IN :ids
            AND a.status = :currentStatus
            """)
    int bulkUpdateStatus(@Param("ids") List<Long> ids,
                         @Param("currentStatus") ApplicationStatus currentStatus,
                         @Param("newStatus") ApplicationStatus newStatus);
}
