package hello.pet.applicationservice.service;

import hello.pet.applicationservice.entity.Application;
import hello.pet.applicationservice.entity.ApplicationScore;
import hello.pet.applicationservice.entity.info.care.AbsenceTime;
import hello.pet.applicationservice.entity.info.care.CareInfo;
import hello.pet.applicationservice.entity.info.care.CareTime;
import hello.pet.applicationservice.entity.info.experience.PetExperienceInfo;
import hello.pet.applicationservice.entity.info.family.FamilyAgreement;
import hello.pet.applicationservice.entity.info.family.FamilyInfo;
import hello.pet.applicationservice.entity.info.financial.FinancialInfo;
import hello.pet.applicationservice.entity.info.financial.MonthlyBudget;
import hello.pet.applicationservice.entity.info.housing.HouseSizeRange;
import hello.pet.applicationservice.entity.info.housing.HousingInfo;
import hello.pet.applicationservice.entity.info.housing.HousingType;
import hello.pet.applicationservice.entity.info.housing.PetLivingPlace;
import hello.pet.applicationservice.entity.info.housing.ResidenceType;
import hello.pet.applicationservice.repository.ApplicationScoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScoringService {

    private final ApplicationScoreRepository scoreRepository;

    @Transactional
    public ApplicationScore calculateAndSaveScore(Application application) {
        log.info("Calculating score for application ID: {}", application.getId());

        // 즉시 탈락 여부 확인
        DisqualifyResult disqualifyResult = checkDisqualifyingFactors(application);

        // 탈락 여부와 관계없이 항상 모든 카테고리별 점수 계산
        int housingScore = calculateHousingScore(application);
        int financialScore = calculateFinancialScore(application);
        int experienceScore = calculateExperienceScore(application);
        int careScore = calculateCareScore(application);
        int familyScore = calculateFamilyScore(application);

        ApplicationScore score = ApplicationScore.builder()
                                                 .application(application)
                                                 .housingScore(housingScore)
                                                 .financialScore(financialScore)
                                                 .experienceScore(experienceScore)
                                                 .careScore(careScore)
                                                 .familyScore(familyScore)
                                                 .hasDisqualifyingFactor(disqualifyResult.isDisqualified())
                                                 .disqualifyingReason(disqualifyResult.reason())
                                                 .build();

        return scoreRepository.save(score);
    }

    /**
     * 즉시 탈락 요인 확인
     */
    private DisqualifyResult checkDisqualifyingFactors(Application app) {
        // 1. 반려동물 허용 여부
        if (!app.getHousingInfo().isPetAllowed()) {
            return new DisqualifyResult(true, "해당 거주지는 반려동물을 허용하지 않습니다.");
        }

        // 2. 가족 동의 여부
        if (app.getFamilyInfo().getFamilyAgreement() == FamilyAgreement.SOME_DISAGREE) {
            return new DisqualifyResult(true, "가족 구성원 중 반대하는 사람이 있습니다");
        }

        // 3. 알레르기 여부
        if (app.getFamilyInfo().isHasPetAllergy()) {
            return new DisqualifyResult(true, "가족 구성원 중 동물 알레르기가 있습니다");
        }

        return new DisqualifyResult(false, null);
    }

    /**
     * 주거 환경 점수 계산 (0-25점)
     */
    private int calculateHousingScore(Application app) {
        int score = 0;
        HousingInfo housing = app.getHousingInfo();

        // 주거 형태 (0-8점)
        HousingType housingType = housing.getHousingType();
        if (housingType == HousingType.DETACHED_HOUSE) {
            score += 8;
        } else if (housingType == HousingType.APARTMENT) {
            score += 6;
        } else if (housingType == HousingType.VILLA) {
            score += 5;
        } else if (housingType == HousingType.OFFICETEL) {
            score += 4;
        } else if (housingType == HousingType.DORMITORY) {
            score += 2;
        } else if (housingType == HousingType.MOBILE_HOME) {
            score += 1;
        }

        // 소유 형태 (0-5점)
        ResidenceType residenceType = housing.getResidenceType();
        if (residenceType == ResidenceType.OWNED) {
            score += 5;
        } else if (residenceType == ResidenceType.JEONSE) {
            score += 3;
        } else if (residenceType == ResidenceType.MONTHLY_RENT) {
            score += 2;
        } else if (residenceType == ResidenceType.TEMPORARY) {
            score += 1;
        }

        // 반려동물 거주 공간 (0-7점)
        PetLivingPlace petLivingPlace = housing.getPetLivingPlace();
        if (petLivingPlace == PetLivingPlace.BOTH) {
            score += 7;  // 실내+실외
        } else if (petLivingPlace == PetLivingPlace.INDOOR) {
            score += 5;  // 실내
        } else if (petLivingPlace == PetLivingPlace.OUTDOOR) {
            score += 2;  // 실외만
        }

        // 주거 공간 크기 (0-5점)
        HouseSizeRange houseSize = housing.getHouseSizeRange();
        if (houseSize == HouseSizeRange.OVER_99) {
            score += 5;  // 30평 이상
        } else if (houseSize == HouseSizeRange.FROM_66_TO_99) {
            score += 4;  // 20-30평
        } else if (houseSize == HouseSizeRange.FROM_33_TO_66) {
            score += 2;  // 10-20평
        } else if (houseSize == HouseSizeRange.LESS_THAN_33) {
            score += 1;  // 10평 미만
        }

        return score;  // 최대 25점 (8+5+7+5)
    }

    /**
     * 경제력 점수 계산 (0-20점)
     */
    private int calculateFinancialScore(Application app) {
        int score = 0;
        FinancialInfo financial = app.getFinancialInfo();

        // 월 예산 (0-15점)
        MonthlyBudget budget = financial.getMonthlyBudget();
        if (budget == MonthlyBudget.OVER_200K) {
            score += 15;  // 20만원 이상
        } else if (budget == MonthlyBudget.FROM_100K_TO_200K) {
            score += 10;  // 10-20만원
        } else if (budget == MonthlyBudget.FROM_50K_TO_100K) {
            score += 5;   // 5-10만원
        } else if (budget == MonthlyBudget.UNDER_50K) {
            score += 2;   // 5만원 이하
        }

        // 비상 자금 보유 (0-5점)
        if (financial.isHasEmergencyFund()) {
            score += 5;
        }

        return score;  // 최대 20점 (15+5)
    }

    /**
     * 경험 점수 계산 (0-20점)
     * 경험 유무만 체크하고 상세 내용은 보호소가 직접 확인
     */
    private int calculateExperienceScore(Application app) {
        PetExperienceInfo experience = app.getPetExperienceInfo();

        if (experience.isHasPetExperience()) {
            return 20;  // 경험 있음 - 만점
        } else {
            return 10;   // 경험 없음 - 기본 점수 (첫 반려동물도 환영)
        }
    }

    /**
     * 돌봄 시간 및 환경 점수 계산 (0-20점)
     */
    private int calculateCareScore(Application app) {
        int score = 0;
        CareInfo care = app.getCareInfo();

        // 하루 돌봄 가능 시간 (0-10점)
        CareTime careTime = care.getCareTime();
        if (careTime == CareTime.FOUR_OR_MORE) {
            score += 10;  // 4시간 이상
        } else if (careTime == CareTime.TWO_TO_THREE) {
            score += 6;   // 2-3시간
        } else if (careTime == CareTime.ONE_OR_LESS) {
            score += 2;   // 1시간 이하
        }

        // 장시간 부재 시간 (0-10점) - 짧을수록 좋음
        AbsenceTime absenceTime = care.getAbsenceTime();
        if (absenceTime == AbsenceTime.ONE_TO_THREE) {
            score += 10;  // 1-3시간 (짧음)
        } else if (absenceTime == AbsenceTime.FOUR_TO_SIX) {
            score += 7;   // 4-6시간
        } else if (absenceTime == AbsenceTime.SEVEN_TO_NINE) {
            score += 4;   // 7-9시간
        } else if (absenceTime == AbsenceTime.TEN_OR_MORE) {
            score += 1;   // 10시간 이상 (너무 김)
        }

        return score;  // 최대 20점 (10+10)
    }

    /**
     * 가족 점수 계산 (0-15점)
     */
    private int calculateFamilyScore(Application app) {
        int score = 0;
        FamilyInfo family = app.getFamilyInfo();
        int householdSize = family.getNumberOfHousehold();

        // 가구원 수별 기본 점수 (0-10점)
        if (householdSize == 1) {
            score = 5;  // 1인 가구 - 혼자 돌봄 부담
        } else if (householdSize == 2) {
            score = 8;  // 2인 가구
        } else if (householdSize >= 3) {
            score = 10;  // 3인 이상 - 가족이 함께 돌봄 가능
        }

        // 13세 미만 아이 유무 (0-5점)
        if (!family.isHasChildUnder13()) {
            score += 5;  // 어린 아이 없음 (안전성)
        } else {
            score += 2;  // 어린 아이 있어도 기본 점수 (교육 효과)
        }

        return score;  // 최대 15점 (10+5)
    }

    /**
     * 즉시 탈락 결과를 담는 내부 레코드
     *
     * @param isDisqualified 즉시 탈락 여부
     * @param reason         탈락 사유 (탈락이 아닌 경우 null)
     */
    private record DisqualifyResult(boolean isDisqualified, String reason) {
    }
}
