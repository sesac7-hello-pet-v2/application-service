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

    Optional<Application> findByIdAndAnnouncementIdAndStatus(Long applicationId,
                                                             Long announcementId,
                                                             ApplicationStatus status);

    Optional<Application> findByUserIdAndAnnouncementId(Long userId, Long announcementId);

    // 특정 공고의 특정 상태 신청서들 조회
    List<Application> findByAnnouncementIdAndStatus(Long announcementId, ApplicationStatus status);
}
