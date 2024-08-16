package org.eternity.reservation.service;

import org.eternity.generic.Money;
import org.eternity.reservation.domain.*;
import org.eternity.reservation.persistence.*;

import java.util.List;

public class ReservationService {
    private ScreeningDAO screeningDAO;
    private MovieDAO movieDAO;
    private DiscountPolicyDAO discountPolicyDAO;
    private DiscountConditionDAO discountConditionDAO;
    private ReservationDAO reservationDAO;

    public ReservationService(ScreeningDAO screeningDAO,
                              MovieDAO movieDAO,
                              DiscountPolicyDAO discountPolicyDAO,
                              DiscountConditionDAO discountConditionDAO,
                              ReservationDAO reservationDAO) {
        this.screeningDAO = screeningDAO;
        this.movieDAO = movieDAO;
        this.discountConditionDAO = discountConditionDAO;
        this.discountPolicyDAO = discountPolicyDAO;
        this.reservationDAO = reservationDAO;
    }

    public Reservation reserveScreening(Long customerId, Long screeningId, Integer audienceCount) {
        Screening screening = screeningDAO.selectScreening(screeningId);
        Movie movie = movieDAO.selectMovie(screening.getMovieId());
        DiscountPolicy policy = discountPolicyDAO.selectDiscountPolicy(movie.getId());
        List<DiscountCondition> conditions = discountConditionDAO.selectDiscountConditions(policy.getId());

        DiscountCondition condition = findDiscountCondition(screening, conditions);

        Money fee;
        if (condition != null) {
            fee = movie.getFee().minus(calculateDiscount(policy, movie));
        } else {
            fee = movie.getFee();
        }

        Reservation reservation = makeReservation(customerId, screeningId, audienceCount, fee);
        reservationDAO.insert(reservation);

        return reservation;
    }

    /**
     * 여기에서 핵심은 DiscountCondition 의 타입이 무엇이고 타입에 따라 어떤 일을 해야하는지를 DiscountCondition 이 아니라 외부의 ReservationService 가 대신 판단하고 결정하고 있다는 것이다.
     * 객체의 타입을 판단하고 타입에 따라 어던 일을 할지를 외부에서 결정하고 있다면 이는 절차지향적인 방식으로 구현된 코드다.
     */
    private DiscountCondition findDiscountCondition(Screening screening, List<DiscountCondition> conditions) {
        for(DiscountCondition condition : conditions) {
            if (condition.isPeriodCondition()) {
                if (screening.isPlayedIn(condition.getDayOfWeek(),
                                         condition.getStartTime(),
                                         condition.getEndTime())) {
                    return condition;
                }
            } else if (condition.isSequenceCondition()) {
                if (condition.getSequence().equals(screening.getSequence())) {
                    return condition;
                }
            } else if (condition.isCombineCondition()) {
                if ((condition.getSequence().equals(screening.getSequence())) &&
                    (screening.isPlayedIn(condition.getDayOfWeek(),
                                          condition.getStartTime(),
                                          condition.getEndTime()))) {
                    return condition;
                }
            }
        }

        return null;
    }

    /**
     * DiscountPolicy 의 타입에 따라 할인 금액을 계산하는 로직이 ReservationService 에 위치하고 있다.
     * 이것도 절차지향적이다.
     * 위의 메서드와 같이 제3의 객체가 객체의 타입을 판단하고 수행할 일을 결정하는 방식은 절차지향적인 코드에서 나타나는 것.
     */
    private Money calculateDiscount(DiscountPolicy policy, Movie movie) {
        if (policy.isAmountPolicy()) {
            return policy.getAmount();
        } else if (policy.isPercentPolicy()) {
            return movie.getFee().times(policy.getPercent());
        }

        return Money.ZERO;
    }

    private Reservation makeReservation(Long customerId, Long screeningId, Integer audienceCount, Money fee) {
        return new Reservation(customerId, screeningId, audienceCount, fee.times(audienceCount));
    }
}
