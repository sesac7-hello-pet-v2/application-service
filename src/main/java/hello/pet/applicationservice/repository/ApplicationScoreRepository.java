package hello.pet.applicationservice.repository;

import hello.pet.applicationservice.entity.ApplicationScore;
import hello.pet.applicationservice.entity.ScoreStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApplicationScoreRepository extends JpaRepository<ApplicationScore, Long> {

    /**
     * 특정 공고의 모든 신청서 점수를 조회 (점수순 정렬)
     */
    @Query("SELECT s FROM ApplicationScore s " +
            "JOIN s.application a " +
            "WHERE a.announcementId = :announcementId " +
            "ORDER BY s.totalScore DESC")
    List<ApplicationScore> findByAnnouncementIdOrderByTotalScoreDesc(@Param("announcementId") Long announcementId);

    /**
     * 특정 공고의 신청서 점수를 최소 점수 이상만 조회 (탈락 제외)
     */
    @Query("SELECT s FROM ApplicationScore s " +
            "JOIN s.application a " +
            "WHERE a.announcementId = :announcementId " +
            "AND s.totalScore >= :minScore " +
            "AND s.status != hello.pet.applicationservice.entity.ScoreStatus.DISQUALIFIED " +
            "ORDER BY s.totalScore DESC")
    List<ApplicationScore> findByAnnouncementIdAndMinScore(
            @Param("announcementId") Long announcementId,
            @Param("minScore") Integer minScore);

    /**
     * 특정 공고의 상태별 신청서 점수 조회
     */
    @Query("SELECT s FROM ApplicationScore s " +
            "JOIN s.application a " +
            "WHERE a.announcementId = :announcementId " +
            "AND s.status = :status " +
            "ORDER BY s.totalScore DESC")
    List<ApplicationScore> findByAnnouncementIdAndStatus(
            @Param("announcementId") Long announcementId,
            @Param("status") ScoreStatus status);

    /**
     * 특정 공고의 정상(NORMAL) 상태 신청서만 조회
     */
    @Query("SELECT s FROM ApplicationScore s " +
            "JOIN s.application a " +
            "WHERE a.announcementId = :announcementId " +
            "AND s.status = hello.pet.applicationservice.entity.ScoreStatus.NORMAL " +
            "ORDER BY s.totalScore DESC")
    List<ApplicationScore> findNormalApplicationsByAnnouncementId(@Param("announcementId") Long announcementId);

    /**
     * 상태별 신청서 개수 통계
     */
    @Query("SELECT s.status, COUNT(s) FROM ApplicationScore s " +
            "JOIN s.application a " +
            "WHERE a.announcementId = :announcementId " +
            "GROUP BY s.status")
    List<Object[]> countByStatusForAnnouncement(@Param("announcementId") Long announcementId);
}
