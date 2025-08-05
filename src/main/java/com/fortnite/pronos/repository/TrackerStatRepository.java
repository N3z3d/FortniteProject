package com.fortnite.pronos.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fortnite.pronos.model.TrackerStat;

@Repository
public interface TrackerStatRepository extends JpaRepository<TrackerStat, UUID> {
  List<TrackerStat> findByPlayer_IdAndSeason(UUID playerId, int season);
}
