package com.fortnite.pronos.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fortnite.pronos.model.PrSnapshot;

@Repository
public interface PrSnapshotRepository extends JpaRepository<PrSnapshot, PrSnapshot.PrSnapshotId> {}
