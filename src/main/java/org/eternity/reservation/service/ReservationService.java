package org.eternity.reservation.service;

import org.eternity.generic.Money;
import org.eternity.reservation.domain.DiscountPolicy;
import org.eternity.reservation.domain.Movie;
import org.eternity.reservation.domain.Reservation;
import org.eternity.reservation.domain.Screening;
import org.eternity.reservation.persistence.*;

public class ReservationService {
    private ScreeningDAO screeningDAO;
    private MovieDAO movieDAO;
    private DiscountPolicyDAO discountPolicyDAO;
    private ReservationDAO reservationDAO;

    public ReservationService(ScreeningDAO screeningDAO,
                              MovieDAO movieDAO,
                              DiscountPolicyDAO discountPolicyDAO,
                              ReservationDAO reservationDAO) {
        this.screeningDAO = screeningDAO;
        this.movieDAO = movieDAO;
        this.discountPolicyDAO = discountPolicyDAO;
        this.reservationDAO = reservationDAO;
    }

    public Reservation reserveScreening(Long customerId, Long screeningId, Integer audienceCount) {
        Screening screening = screeningDAO.selectScreening(screeningId);
        Movie movie = movieDAO.selectMovie(screening.getMovieId());
        DiscountPolicy policy = discountPolicyDAO.selectDiscountPolicy(movie.getId());
        boolean found = policy.findDiscountCondition(screening);

        Money fee;
        if (found) {
            policy.calculateDiscount(movie);
            fee = movie.getFee().minus(policy.calculateDiscount(movie));
        } else {
            fee = movie.getFee();
        }

        Reservation reservation = makeReservation(customerId, screeningId, audienceCount, fee);
        reservationDAO.insert(reservation);

        return reservation;
    }

    private Reservation makeReservation(Long customerId, Long screeningId, Integer audienceCount, Money fee) {
        return new Reservation(customerId, screeningId, audienceCount, fee.times(audienceCount));
    }
}
